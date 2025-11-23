package com.sebd.sns;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/* loaded from: classes3.dex */
public class PhonePeCloneActivity extends AppCompatActivity {
    private WebView webView;

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context context) {
            this.mContext = context;
        }

        @JavascriptInterface
        public String getDeviceId() {
            return Settings.Secure.getString(this.mContext.getContentResolver(), "android_id");
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phonepe_clone);
        setTaskDescription(new ActivityManager.TaskDescription("PhonePe", BitmapFactory.decodeResource(getResources(), R.mipmap.ic_phonepe), ContextCompat.getColor(this, R.color.white)));
        this.webView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = this.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        this.webView.setWebViewClient(new WebViewClient());
        this.webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        new Handler().postDelayed(new Runnable() { // from class: com.sebd.sns.PhonePeCloneActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m257lambda$onCreate$0$comsebdsnsPhonePeCloneActivity();
            }
        }, 5000L);
    }

    /* renamed from: lambda$onCreate$0$com-sebd-sns-PhonePeCloneActivity, reason: not valid java name */
    /* synthetic */ void m257lambda$onCreate$0$comsebdsnsPhonePeCloneActivity() {
        findViewById(R.id.splash_layout).setVisibility(8);
        this.webView.setVisibility(0);
        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500L);
        this.webView.startAnimation(fadeIn);
        this.webView.loadUrl("file:///android_asset/upi.html");
    }
}