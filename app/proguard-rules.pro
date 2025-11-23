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