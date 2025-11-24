# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ========== حذف Log statements در Release Build ==========
# این قوانین تمام Log calls رو از bytecode حذف می‌کنن
# وقتی release build بگیرید، تمام Log ها از کد حذف می‌شن (نه فقط disable)

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static boolean isLoggable(java.lang.String, int);
}

# ========== حفظ کردن کلاس‌های ضروری ==========

# حفظ کردن WebView JavaScript Interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# حفظ کردن line number برای debugging (اگر crash کرد)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ========== Firebase ==========
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ========== Kotlin ==========
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ========== Coroutines ==========
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ========== حفظ کردن کلاس‌های اصلی برنامه ==========
-keep class com.example.test.** { *; }
-keepclassmembers class com.example.test.** { *; }

# ========== حفظ کردن Resource ها ==========
# جلوگیری از حذف resource ID ها
-keepclassmembers class **.R$* {
    public static <fields>;
}

# حفظ کردن تمام resource ها
-keep class **.R
-keep class **.R$* {
    *;
}

# ========== حفظ کردن Asset Files ==========
# جلوگیری از حذف asset files در shrinkResources
# (اگر shrinkResources فعال باشه، این کمک می‌کنه)

# ========== حفظ کردن Reflection ==========
# برای استفاده از reflection در کد
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ========== حفظ کردن JSON Classes ==========
# برای JSON parsing
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ========== حفظ کردن WebView ==========
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# ========== حفظ کردن Context و Activity ==========
-keep public class * extends android.content.Context
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver