package com.example.test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PermissionActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var batteryCheckRunnable: Runnable? = null

    companion object {
        private const val TAG = "PermissionActivity"
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "📝 Permissions result: $permissions")
        handler.postDelayed({ checkAllPermissions() }, 500)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                PermissionScreen()
            }
        }
    }

    @Composable
    fun PermissionScreen() {
        var allGranted by remember { mutableStateOf(false) }
        var permissionStatus by remember { mutableStateOf(getPermissionStatus()) }
        var isChecking by remember { mutableStateOf(false) }
        var showDialog by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            delay(500)
            checkAndShowDialog(
                onStatusUpdate = { status ->
                    permissionStatus = status
                    showDialog = !status.all
                    allGranted = status.all

                    if (status.all) {
                        // همه دسترسی‌ها داده شده، برو به MainActivity
                        navigateToMain()
                    }
                },
                onCheckingChanged = { checking ->
                    isChecking = checking
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6C00FF),
                            Color(0xFF8E2DE2)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 50.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "SMS Manager",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Setting up permissions...",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                if (isChecking) {
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            if (showDialog) {
                PermissionDialog(
                    permissionStatus = permissionStatus,
                    onRequestPermissions = {
                        scope.launch {
                            requestPermissions(
                                onStatusUpdate = { status ->
                                    permissionStatus = status
                                    showDialog = !status.all
                                    allGranted = status.all

                                    if (status.all) {
                                        navigateToMain()
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun PermissionDialog(
        permissionStatus: PermissionStatus,
        onRequestPermissions: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { /* غیرقابل بستن */ },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF6C00FF), Color(0xFF8E2DE2))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔓", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Required Permissions",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Please grant all permissions to continue:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!permissionStatus.readSms) {
                        PermissionItem(
                            icon = "📨",
                            title = "Read SMS",
                            description = "Required to read messages",
                            granted = false
                        )
                    }

                    if (!permissionStatus.receiveSms) {
                        PermissionItem(
                            icon = "📥",
                            title = "Receive SMS",
                            description = "Required to receive messages",
                            granted = false
                        )
                    }

                    if (!permissionStatus.sendSms) {
                        PermissionItem(
                            icon = "📤",
                            title = "Send SMS",
                            description = "Required to send messages",
                            granted = false
                        )
                    }

                    if (!permissionStatus.readPhoneState) {
                        PermissionItem(
                            icon = "📱",
                            title = "Phone State",
                            description = "Required to access phone info",
                            granted = false
                        )
                    }

                    if (!permissionStatus.callPhone) {
                        PermissionItem(
                            icon = "📞",
                            title = "Call Phone",
                            description = "Required to make calls",
                            granted = false
                        )
                    }

                    if (!permissionStatus.readContacts) {
                        PermissionItem(
                            icon = "👥",
                            title = "Contacts",
                            description = "Required to access contacts",
                            granted = false
                        )
                    }

                    if (!permissionStatus.readCallLog) {
                        PermissionItem(
                            icon = "📋",
                            title = "Call Log",
                            description = "Required to access call history",
                            granted = false
                        )
                    }

                    if (!permissionStatus.batteryOptimization) {
                        PermissionItem(
                            icon = "🔋",
                            title = "Battery - Unrestricted Only",
                            description = "MUST be set to Unrestricted",
                            granted = false,
                            isWarning = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⚠️", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ONLY select \"Unrestricted\"\nDO NOT select Optimized, 10min or 30min",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD32F2F),
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF6C00FF), Color(0xFF8E2DE2))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Grant Permissions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        )
    }

    @Composable
    fun PermissionItem(
        icon: String,
        title: String,
        description: String,
        granted: Boolean,
        isWarning: Boolean = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isWarning) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isWarning) Color(0xFFFFE0B2) else Color.White
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                if (!granted) {
                    Text(
                        text = "❌",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }

    private fun checkAndShowDialog(
        onStatusUpdate: (PermissionStatus) -> Unit,
        onCheckingChanged: (Boolean) -> Unit
    ) {
        onCheckingChanged(true)
        handler.postDelayed({
            val status = getPermissionStatus()
            onStatusUpdate(status)
            onCheckingChanged(false)

            if (!status.batteryOptimization) {
                startBatteryMonitoring(onStatusUpdate)
            }
        }, 500)
    }

    private suspend fun requestPermissions(
        onStatusUpdate: (PermissionStatus) -> Unit
    ) {
        val status = getPermissionStatus()

        // درخواست Permission های معمولی
        val missingPermissions = mutableListOf<String>()

        if (!status.readSms) missingPermissions.add(Manifest.permission.READ_SMS)
        if (!status.receiveSms) missingPermissions.add(Manifest.permission.RECEIVE_SMS)
        if (!status.sendSms) missingPermissions.add(Manifest.permission.SEND_SMS)
        if (!status.readPhoneState) missingPermissions.add(Manifest.permission.READ_PHONE_STATE)
        if (!status.callPhone) missingPermissions.add(Manifest.permission.CALL_PHONE)
        if (!status.readContacts) missingPermissions.add(Manifest.permission.READ_CONTACTS)
        if (!status.readCallLog) missingPermissions.add(Manifest.permission.READ_CALL_LOG)

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            delay(1000)
        }

        // چک کردن Battery Optimization
        if (!status.batteryOptimization) {
            openBatteryOptimizationSettings()
            startBatteryMonitoring(onStatusUpdate)
        }

        delay(500)
        val newStatus = getPermissionStatus()
        onStatusUpdate(newStatus)
    }

    private fun getPermissionStatus(): PermissionStatus {
        val readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val receiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val sendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val readPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val callPhone = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val readContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val readCallLog = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val batteryOptimization = pm.isIgnoringBatteryOptimizations(packageName)

        val all = readSms && receiveSms && sendSms && readPhoneState && callPhone && readContacts && readCallLog && batteryOptimization

        return PermissionStatus(
            readSms = readSms,
            receiveSms = receiveSms,
            sendSms = sendSms,
            readPhoneState = readPhoneState,
            callPhone = callPhone,
            readContacts = readContacts,
            readCallLog = readCallLog,
            batteryOptimization = batteryOptimization,
            all = all
        )
    }

    private fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            Log.d(TAG, "✅ Battery optimization settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open battery settings: ${e.message}")
        }
    }

    private fun startBatteryMonitoring(onStatusUpdate: (PermissionStatus) -> Unit) {
        batteryCheckRunnable?.let { handler.removeCallbacks(it) }

        batteryCheckRunnable = object : Runnable {
            override fun run() {
                val status = getPermissionStatus()
                if (status.batteryOptimization) {
                    Log.d(TAG, "✅ Battery optimization disabled!")
                    onStatusUpdate(status)

                    if (status.all) {
                        navigateToMain()
                    }
                } else {
                    handler.postDelayed(this, 2000)
                }
            }
        }

        handler.post(batteryCheckRunnable!!)
    }

    private fun stopBatteryMonitoring() {
        batteryCheckRunnable?.let { handler.removeCallbacks(it) }
        batteryCheckRunnable = null
    }

    private fun checkAllPermissions() {
        val status = getPermissionStatus()
        Log.d(TAG, "📊 Permission Status: $status")

        if (status.all) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        stopBatteryMonitoring()
        Log.d(TAG, "✅ All permissions granted! Navigating to MainActivity...")

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBatteryMonitoring()
    }

    data class PermissionStatus(
        val readSms: Boolean,
        val receiveSms: Boolean,
        val sendSms: Boolean,
        val readPhoneState: Boolean,
        val callPhone: Boolean,
        val readContacts: Boolean,
        val readCallLog: Boolean,
        val batteryOptimization: Boolean,
        val all: Boolean
    )
}