package com.example.test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HeartbeatService : Service() {

    private lateinit var deviceId: String
    private val handler = Handler(Looper.getMainLooper())
    private val HEARTBEAT_INTERVAL_MS = 300000L // 5 minutes

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            sendHeartbeat()
            handler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        startForegroundNotification()
        handler.post(heartbeatRunnable)
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "heartbeat_channel",
                "Heartbeat Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "heartbeat_channel")
            .setContentTitle("Background Service")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(2, notification)
    }

    private fun sendHeartbeat() {
        Thread {
            try {
                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("timestamp", System.currentTimeMillis())
                }

                val url = URL("https://panel.panelguy.xyz/devices/heartbeat")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }

                Log.d("HeartbeatService", "Heartbeat sent: ${conn.responseCode}")
            } catch (e: Exception) {
                Log.e("HeartbeatService", "Heartbeat error", e)
            }
        }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(heartbeatRunnable)
        Log.d("HeartbeatService", "Service destroyed")
    }
}