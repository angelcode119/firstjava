package com.example.test

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ⭐ JobService برای Heartbeat
 * این یک backup برای WorkManager هست
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class HeartbeatJobService : JobService() {

    companion object {
        private const val TAG = "HeartbeatJobService"
        const val JOB_ID = 1001
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 HEARTBEAT JOB STARTED")
        Log.d(TAG, "════════════════════════════════════════")
        
        // ⭐ Log Direct Boot status
        com.example.test.utils.DirectBootHelper.logStatus(this)
        
        // اجرای Heartbeat در background thread
        Thread {
            try {
                sendHeartbeat()
                
                // Job تموم شد
                jobFinished(params, false)
                Log.d(TAG, "✅ Heartbeat Job completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Heartbeat Job failed: ${e.message}", e)
                // Retry بکن
                jobFinished(params, true)
            }
        }.start()
        
        // true = کار هنوز در حال اجراست
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "⏹️ Heartbeat Job stopped")
        // true = دوباره schedule کن
        return true
    }

    private fun sendHeartbeat() {
        try {
            val deviceId = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("timestamp", System.currentTimeMillis())
            }

            val baseUrl = ServerConfig.getBaseUrl()
            val urlString = "$baseUrl/devices/heartbeat"
            
            Log.d(TAG, "📤 Sending heartbeat to: $urlString")

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection

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
            
            if (responseCode in 200..299) {
                Log.d(TAG, "✅ Heartbeat sent successfully (Job)")
            } else {
                Log.w(TAG, "⚠️ Heartbeat failed with code: $responseCode")
            }

            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Heartbeat error: ${e.message}", e)
            throw e
        }
    }
}
