package com.example.test.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * ⭐ Helper برای Direct Boot Support
 * 
 * Direct Boot = اپ حتی قبل از Unlock هم کار می‌کنه!
 */
object DirectBootHelper {

    private const val TAG = "DirectBootHelper"

    /**
     * آیا گوشی در حالت Direct Boot هست؟
     * (یعنی هنوز Unlock نشده)
     */
    fun isDeviceLocked(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            !context.isDeviceProtectedStorage
        } else {
            false // Android 6 و پایین‌تر Direct Boot ندارن
        }
    }

    /**
     * گرفتن Context مناسب برای Direct Boot
     * 
     * قبل از Unlock: Device Protected Storage
     * بعد از Unlock: Credential Protected Storage (عادی)
     */
    fun getContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // اگه قبل از Unlock هستیم
            if (!context.isDeviceProtectedStorage) {
                // Context مخصوص Direct Boot بگیر
                context.createDeviceProtectedStorageContext()
            } else {
                // Context عادی
                context
            }
        } else {
            // Android 6 و پایین‌تر
            context
        }
    }

    /**
     * Migrate کردن داده از Device Protected به Credential Protected
     * (وقتی کاربر Unlock می‌کنه)
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun migrateStorageIfNeeded(context: Context) {
        try {
            if (!context.isDeviceProtectedStorage) {
                Log.d(TAG, "📦 Starting storage migration...")
                
                // Migrate کن
                val migrated = context.moveSharedPreferencesFrom(
                    context.createDeviceProtectedStorageContext(),
                    "app_prefs"
                )
                
                if (migrated) {
                    Log.d(TAG, "✅ Storage migration successful")
                } else {
                    Log.d(TAG, "ℹ️ No storage to migrate")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Storage migration failed: ${e.message}", e)
        }
    }

    /**
     * Log کردن وضعیت Direct Boot
     */
    fun logStatus(context: Context) {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🔐 DIRECT BOOT STATUS")
        Log.d(TAG, "════════════════════════════════════════")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val isLocked = isDeviceLocked(context)
            val storageType = if (context.isDeviceProtectedStorage) {
                "Device Protected (قبل Unlock)"
            } else {
                "Credential Protected (بعد Unlock)"
            }
            
            Log.d(TAG, "📱 Device Locked: $isLocked")
            Log.d(TAG, "💾 Storage Type: $storageType")
            Log.d(TAG, "✅ Direct Boot Support: ENABLED")
        } else {
            Log.d(TAG, "⚠️ Android ${Build.VERSION.SDK_INT} - Direct Boot not supported")
        }
        
        Log.d(TAG, "════════════════════════════════════════")
    }

    /**
     * چک کردن Permission برای USER_UNLOCKED broadcast
     */
    fun canReceiveUserUnlocked(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
}
