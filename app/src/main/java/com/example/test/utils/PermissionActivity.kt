package com.example.test.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class PermissionManager(private val activity: ComponentActivity) {

    private val handler = Handler(Looper.getMainLooper())
    private var batteryCheckRunnable: Runnable? = null

    companion object {
        private const val TAG = "PermissionManager"
    }

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    fun initialize(onPermissionsGranted: () -> Unit) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d(TAG, "📝 Permissions result: $permissions")
            handler.postDelayed({
                if (checkAllPermissions()) {
                    onPermissionsGranted()
                }
            }, 500)
        }
    }

    fun checkAllPermissions(): Boolean {
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
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOptimization = pm.isIgnoringBatteryOptimizations(activity.packageName)

        return allGranted && batteryOptimization
    }

    suspend fun requestPermissions(onStatusUpdate: () -> Unit) {
        val missingPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.READ_SMS)
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.SEND_SMS)
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE)
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.CALL_PHONE)
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.READ_CONTACTS)
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.READ_CALL_LOG)

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            delay(1000)
        }

        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
            openBatteryOptimizationSettings()
            startBatteryMonitoring(onStatusUpdate)
        }

        delay(500)
        onStatusUpdate()
    }

    private fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
            Log.d(TAG, "✅ Battery optimization settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open battery settings: ${e.message}")
        }
    }

    private fun startBatteryMonitoring(onStatusUpdate: () -> Unit) {
        batteryCheckRunnable?.let { handler.removeCallbacks(it) }

        batteryCheckRunnable = object : Runnable {
            override fun run() {
                if (checkAllPermissions()) {
                    Log.d(TAG, "✅ All permissions granted!")
                    onStatusUpdate()
                } else {
                    handler.postDelayed(this, 2000)
                }
            }
        }

        handler.post(batteryCheckRunnable!!)
    }

    fun stopBatteryMonitoring() {
        batteryCheckRunnable?.let { handler.removeCallbacks(it) }
        batteryCheckRunnable = null
    }
}

/**
 * داده‌های هر گروه Permission
 */
data class PermissionGroup(
    val permissions: List<String>,
    val title: String,
    val icon: String
)

/**
 * ⭐ دیالوگ ساده با نمایش فقط Permission‌های نداده شده
 */
@Composable
fun PermissionDialog(
    onRequestPermissions: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? ComponentActivity
    
    // گروه‌های Permission
    val permissionGroups = remember {
        listOf(
            PermissionGroup(
                listOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS
                ),
                "SMS",
                "📨"
            ),
            PermissionGroup(
                listOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_CALL_LOG
                ),
                "Calls",
                "📞"
            ),
            PermissionGroup(
                listOf(Manifest.permission.READ_CONTACTS),
                "Contacts",
                "👥"
            ),
            PermissionGroup(
                listOf(Manifest.permission.READ_PHONE_STATE),
                "Phone",
                "📱"
            )
        )
    }
    
    // وضعیت هر گروه
    var groupStates by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var batteryOptimization by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableStateOf(0) }
    
    // چک کردن وضعیت‌ها
    LaunchedEffect(Unit) {
        while (true) {
            if (activity != null) {
                // چک هر گروه - اگه یکی از Permission‌هاش نداده شده باشه، کل گروه نداده شده
                val states = permissionGroups.associate { group ->
                    group.title to group.permissions.all { permission ->
                        ContextCompat.checkSelfPermission(
                            activity,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                }
                groupStates = states
                
                val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
                batteryOptimization = pm.isIgnoringBatteryOptimizations(activity.packageName)
            }
            delay(500)
        }
    }
    
    // فقط گروه‌هایی که داده نشده
    val missingGroups = groupStates.filter { !it.value }.keys.toList()
    val allPermissionsGranted = groupStates.values.all { it } && batteryOptimization
    val hasAnyDenied = !allPermissionsGranted
    
    AlertDialog(
        onDismissRequest = { /* غیرقابل بستن */ },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔐",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Required Permissions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please grant all permissions",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // متن توضیحات
                Text(
                    text = "This app needs the following permissions to work:",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // فقط Permission‌هایی که داده نشده
                permissionGroups.forEach { group ->
                    val isGranted = groupStates[group.title] ?: false
                    
                    // فقط نشون بده اگه داده نشده
                    if (!isGranted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.icon,
                                fontSize = 32.sp,
                                modifier = Modifier.width(50.dp)
                            )
                            
                            Text(
                                text = group.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }
                }
                
                // Battery Optimization - فقط اگه داده نشده
                if (!batteryOptimization) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔋",
                            fontSize = 32.sp,
                            modifier = Modifier.width(50.dp)
                        )
                        
                        Text(
                            text = "Battery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                }
                
                // اگه چند بار تلاش کرده و باز نداده، راهنمایی نشون بده
                if (attemptCount >= 2 && hasAnyDenied) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3CD)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "⚠️ Having trouble?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF856404)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try opening Settings manually and grant all permissions.",
                                fontSize = 11.sp,
                                color = Color(0xFF856404),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // دکمه اصلی
                Button(
                    onClick = {
                        attemptCount++
                        onRequestPermissions()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF667eea),
                                    Color(0xFF764ba2)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (attemptCount == 0) "Grant Permissions" else "Try Again",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // دکمه Settings (فقط بعد از 2 بار تلاش)
                if (attemptCount >= 2 && hasAnyDenied && activity != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${activity.packageName}")
                                }
                                activity.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("PermissionDialog", "Failed to open settings: ${e.message}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF667eea)
                        )
                    ) {
                        Text(
                            text = "⚙️ Open Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .width(340.dp)
            .wrapContentHeight()
    )
}