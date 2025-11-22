package com.example.test

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "HeartbeatWorker"
        const val WORK_NAME = "HeartbeatWork"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "💓 HEARTBEAT WORKER STARTED")
            Log.d(TAG, "════════════════════════════════════════")

            sendHeartbeat()

            Log.d(TAG, "✅ Heartbeat sent successfully")
            Log.d(TAG, "════════════════════════════════════════")

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Heartbeat failed: ${e.message}", e)

            // ⭐ Retry تا 3 بار
            if (runAttemptCount < 3) {
                Log.w(TAG, "🔄 Retrying... Attempt: ${runAttemptCount + 1}")
                Result.retry()
            } else {
                Log.e(TAG, "💥 Max retries reached, giving up")
                Result.failure()
            }
        }
    }

    private fun sendHeartbeat() {
        val deviceId = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val body = JSONObject().apply {
            put("deviceId", deviceId)
            put("isOnline", true)
            put("timestamp", System.currentTimeMillis())
            put("source", "WorkManager")
        }

        val baseUrl = ServerConfig.getBaseUrl()
        val url = URL("$baseUrl/devices/heartbeat")
        val conn = url.openConnection() as HttpURLConnection

        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray())
                os.flush()
            }

            val responseCode = conn.responseCode
            Log.d(TAG, "📡 Server response: $responseCode")

            if (responseCode !in 200..299) {
                throw Exception("Server returned $responseCode")
            }

        } finally {
            conn.disconnect()
        }
    }
}
