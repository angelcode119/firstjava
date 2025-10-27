package com.example.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val isOnline = isOnline(context)
        Log.d("NetworkReceiver", "Device online: $isOnline")

        updateOnlineStatusToBackend(context, isOnline)
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val netInfo = cm.activeNetworkInfo
            netInfo != null && netInfo.isConnected
        }
    }

    private fun updateOnlineStatusToBackend(context: Context, isOnline: Boolean) {
        Thread {
            try {
                val deviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("isOnline", isOnline)
                }

                val url = URL("https://panel.panelguy.xyz/devices/update-online-status")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }

                Log.d("NetworkReceiver", "Status update: ${conn.responseCode}")
            } catch (e: Exception) {
                Log.e("NetworkReceiver", "Failed to update status", e)
            }
        }.start()
    }
}