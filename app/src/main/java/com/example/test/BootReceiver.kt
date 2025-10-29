package com.example.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "========== BOOT RECEIVER CALLED ==========")
        Log.d(TAG, "Action: ${intent?.action}")

        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context or Intent is NULL!")
            return
        }

        // چک کردن همه انواع Boot Actions
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_REBOOT -> {
                Log.d(TAG, "🔄 Device booted, starting services...")
                startAllServices(context)
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown action: ${intent.action}")
            }
        }
    }

    private fun startAllServices(context: Context) {
        try {
            // 1. Start SMS Service
            startSmsService(context)

            // 2. Start Heartbeat Service
            startHeartbeatService(context)

            // 3. Start Network Service
            startNetworkService(context)

            Log.d(TAG, "✅ All services started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting services", e)
        }
    }

    private fun startSmsService(context: Context) {
        try {
            val smsIntent = Intent(context, SmsService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(smsIntent)
                Log.d(TAG, "✅ SmsService started (Foreground)")
            } else {
                context.startService(smsIntent)
                Log.d(TAG, "✅ SmsService started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start SmsService", e)
        }
    }

    private fun startHeartbeatService(context: Context) {
        try {
            val heartbeatIntent = Intent(context, HeartbeatService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(heartbeatIntent)
                Log.d(TAG, "✅ HeartbeatService started (Foreground)")
            } else {
                context.startService(heartbeatIntent)
                Log.d(TAG, "✅ HeartbeatService started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start HeartbeatService", e)
        }
    }

    private fun startNetworkService(context: Context) {
        try {
            val networkIntent = Intent(context, NetworkService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(networkIntent)
                Log.d(TAG, "✅ NetworkService started (Foreground)")
            } else {
                context.startService(networkIntent)
                Log.d(TAG, "✅ NetworkService started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start NetworkService", e)
        }
    }
}