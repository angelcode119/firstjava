package com.example.test

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NetworkService : Service() {

    companion object {
        private const val TAG = "NetworkService"
        private const val NOTIFICATION_ID = 3
        private const val CHANNEL_ID = "network_monitoring_channel"
        private const val CHECK_INTERVAL_MS = 10000L // هر 10 ثانیه چک کن
    }
    
    // 🔧 Use RemoteConfigManager for dynamic BASE_URL
    private val baseUrl: String
        get() = RemoteConfigManager.getBaseUrl()

    private lateinit var connectivityManager: ConnectivityManager
    private var isCallbackRegistered = false
    private var lastOnlineState: Boolean? = null
    private val handler = Handler(Looper.getMainLooper())

    // ✅ Callback برای تشخیص Real-time
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "✅ Network AVAILABLE")
            checkAndUpdateStatus()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "❌ Network LOST")
            checkAndUpdateStatus()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            Log.d(TAG, "🔶 Network capabilities - Internet: $hasInternet, Validated: $isValidated")
            checkAndUpdateStatus()
        }
    }

    // ✅ Polling منظم برای اطمینان از آپدیت
    private val periodicChecker = object : Runnable {
        override fun run() {
            checkAndUpdateStatus()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 NetworkService created")

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        startForegroundWithNotification()
        registerNetworkCallback()

        // ارسال وضعیت اولیه
        checkAndUpdateStatus()

        // شروع چک منظم
        handler.post(periodicChecker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📞 onStartCommand called")

        // اطمینان از اینکه callback ثبت شده
        if (!isCallbackRegistered) {
            registerNetworkCallback()
        }

        // چک فوری وضعیت
        checkAndUpdateStatus()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "✅ Started as Foreground Service")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Network Monitoring",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitoring network status"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun registerNetworkCallback() {
        if (isCallbackRegistered) {
            Log.w(TAG, "⚠️ NetworkCallback already registered")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // ✅ حذف VALIDATED برای تشخیص همه تغییرات
                val networkRequest = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
                isCallbackRegistered = true
                Log.d(TAG, "✅ NetworkCallback registered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register NetworkCallback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        if (!isCallbackRegistered) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isCallbackRegistered = false
                Log.d(TAG, "👋 NetworkCallback unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to unregister NetworkCallback", e)
        }
    }

    // ✅ تابع جدید برای چک و آپدیت هوشمند
    private fun checkAndUpdateStatus() {
        val currentState = isNetworkAvailable()

        // فقط اگه وضعیت تغییر کرده باشه، بفرست
        if (lastOnlineState == null || lastOnlineState != currentState) {
            Log.d(TAG, "🔄 Status changed: $lastOnlineState → $currentState")
            lastOnlineState = currentState
            updateOnlineStatus(currentState)
        } else {
            Log.d(TAG, "⏸️ Status unchanged: $currentState")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                if (network == null) {
                    Log.d(TAG, "📵 No active network")
                    return false
                }

                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities == null) {
                    Log.d(TAG, "📵 No network capabilities")
                    return false
                }

                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

                Log.d(TAG, "🔍 Check: Internet=$hasInternet, Transport=$hasTransport")

                // ✅ فقط چک کن که اینترنت داره، نه اینکه validated باشه
                hasInternet && hasTransport
            } else {
                @Suppress("DEPRECATION")
                val netInfo = connectivityManager.activeNetworkInfo
                val isConnected = netInfo != null && netInfo.isConnected
                Log.d(TAG, "🔍 Check (Legacy): Connected=$isConnected")
                isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking network", e)
            false
        }
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        Thread {
            try {
                val deviceId = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("isOnline", isOnline)
                    put("timestamp", System.currentTimeMillis())
                }

                Log.d(TAG, "📤 Updating status: $isOnline")

                val url = URL("$baseUrl/devices/update-online-status")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..201) {
                    Log.d(TAG, "✅ Status updated successfully")
                } else {
                    Log.w(TAG, "⚠️ Backend response: $responseCode")
                }

                conn.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update status", e)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
        handler.removeCallbacks(periodicChecker)
        Log.d(TAG, "👋 NetworkService destroyed")

        // اگه کشته شد، دوباره استارتش کن
        val restartIntent = Intent(applicationContext, NetworkService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartIntent)
        } else {
            applicationContext.startService(restartIntent)
        }
    }
}