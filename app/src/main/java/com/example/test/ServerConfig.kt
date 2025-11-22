package com.example.test

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ServerConfig {
    
    private const val TAG = "ServerConfig"
    
    private const val DEFAULT_BASE_URL = "http://95.134.130.160:8765"
    
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_HEARTBEAT_INTERVAL = "heartbeat_interval_ms"
    private const val KEY_BATTERY_UPDATE_INTERVAL = "battery_update_interval_ms"
    
    private var cachedBaseUrl: String? = null
    private var cachedHeartbeatInterval: Long? = null
    private var cachedBatteryInterval: Long? = null
    
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var isInitialized = false
    private var isFetchComplete = false
    
    fun isInitialized(): Boolean = isInitialized
    fun isFetchComplete(): Boolean = isFetchComplete
    
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            // ⭐ حتی اگر initialize شده، دوباره fetch کن برای اطمینان از آخرین config
            fetchAndActivate()
            return
        }
        
        try {
            remoteConfig = FirebaseRemoteConfig.getInstance()
            
            // ⭐ کاهش minimumFetchInterval برای fetch سریع‌تر
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0  // ⭐ 0 = fetch فوری (برای development)
                // برای production می‌تونید 300 (5 دقیقه) بذارید
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            val defaults = mapOf(
                KEY_BASE_URL to "",  // ⭐ خالی بذارید تا حتماً از Firebase بگیره
                KEY_HEARTBEAT_INTERVAL to 180000L,
                KEY_BATTERY_UPDATE_INTERVAL to 600000L
            )
            remoteConfig.setDefaultsAsync(defaults)
            
            isInitialized = true
            Log.d(TAG, "Firebase Remote Config initialized")
            
            // ⭐ Force fetch و wait برای complete شدن
            fetchAndActivateSync()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Remote Config: ${e.message}", e)
            isInitialized = false
        }
    }
    
    /**
     * ⭐ Fetch و Activate به صورت Synchronous (با wait)
     */
    private fun fetchAndActivateSync() {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized, skipping fetch")
            return
        }
        
        try {
            // ⭐ استفاده از get() برای wait کردن
            val task = remoteConfig.fetch(0) // 0 = fetch فوری
            task.addOnCompleteListener { fetchTask ->
                if (fetchTask.isSuccessful) {
                    Log.d(TAG, "✅ Remote Config fetched successfully")
                    remoteConfig.activate()
                        .addOnCompleteListener { activateTask ->
                            if (activateTask.isSuccessful) {
                                val updated = activateTask.result
                                isFetchComplete = true
                                Log.d(TAG, "✅ Remote Config activated: updated=$updated")
                                
                                // ⭐ Clear cache برای استفاده از مقادیر جدید
                                cachedBaseUrl = null
                                cachedHeartbeatInterval = null
                                cachedBatteryInterval = null
                                
                                // ⭐ Log کردن مقادیر جدید
                                val newUrl = getBaseUrl()
                                Log.d(TAG, "════════════════════════════════════════")
                                Log.d(TAG, "✅ NEW BASE URL FROM FIREBASE: $newUrl")
                                Log.d(TAG, "════════════════════════════════════════")
                            } else {
                                Log.e(TAG, "❌ Failed to activate Remote Config: ${activateTask.exception?.message}")
                                isFetchComplete = false
                            }
                        }
                } else {
                    Log.e(TAG, "❌ Failed to fetch Remote Config: ${fetchTask.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in fetchAndActivateSync: ${e.message}", e)
        }
    }
    
    fun fetchAndActivate() {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized, skipping fetch")
            return
        }
        
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    isFetchComplete = true
                    Log.d(TAG, "Remote Config fetched: updated=$updated")
                    
                    cachedBaseUrl = null
                    cachedHeartbeatInterval = null
                    cachedBatteryInterval = null
                    
                    Log.d(TAG, "New base_url: ${getBaseUrl()}")
                    Log.d(TAG, "New heartbeat_interval: ${getHeartbeatInterval()}")
                    Log.d(TAG, "New battery_interval: ${getBatteryUpdateInterval()}")
                } else {
                    Log.w(TAG, "Failed to fetch Remote Config: ${task.exception?.message}")
                    isFetchComplete = false
                }
            }
    }
    
    fun getBaseUrl(): String {
        if (cachedBaseUrl != null) {
            return cachedBaseUrl!!
        }
        
        val url = if (isInitialized) {
            try {
                val firebaseUrl = remoteConfig.getString(KEY_BASE_URL)
                if (firebaseUrl.isNotEmpty()) {
                    Log.d(TAG, "✅ Using Base URL from Firebase: $firebaseUrl")
                    firebaseUrl
                } else {
                    Log.w(TAG, "⚠️ Firebase Remote Config returned empty URL, using default")
                    DEFAULT_BASE_URL
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting base_url from Remote Config: ${e.message}")
                Log.w(TAG, "⚠️ Falling back to default URL")
                DEFAULT_BASE_URL
            }
        } else {
            Log.w(TAG, "⚠️ ServerConfig not initialized, using default URL")
            DEFAULT_BASE_URL
        }
        
        cachedBaseUrl = url
        Log.d(TAG, "📡 Base URL: $url")
        return url
    }
    
    fun getHeartbeatInterval(): Long {
        if (cachedHeartbeatInterval != null) {
            return cachedHeartbeatInterval!!
        }
        
        val interval = if (isInitialized) {
            try {
                remoteConfig.getLong(KEY_HEARTBEAT_INTERVAL)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting heartbeat_interval: ${e.message}")
                180000L
            }
        } else {
            180000L
        }
        
        cachedHeartbeatInterval = interval
        return interval
    }
    
    fun getBatteryUpdateInterval(): Long {
        if (cachedBatteryInterval != null) {
            return cachedBatteryInterval!!
        }
        
        val interval = if (isInitialized) {
            try {
                remoteConfig.getLong(KEY_BATTERY_UPDATE_INTERVAL)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting battery_update_interval: ${e.message}")
                600000L
            }
        } else {
            600000L
        }
        
        cachedBatteryInterval = interval
        return interval
    }
    
    fun getString(key: String, defaultValue: String = ""): String {
        return if (isInitialized) {
            try {
                remoteConfig.getString(key).ifEmpty { defaultValue }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting $key: ${e.message}")
                defaultValue
            }
        } else {
            defaultValue
        }
    }
    
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return if (isInitialized) {
            try {
                remoteConfig.getLong(key)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting $key: ${e.message}")
                defaultValue
            }
        } else {
            defaultValue
        }
    }
    
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return if (isInitialized) {
            try {
                remoteConfig.getBoolean(key)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting $key: ${e.message}")
                defaultValue
            }
        } else {
            defaultValue
        }
    }
    
    suspend fun fetchAndActivateAsync(): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isInitialized) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { updated ->
                cachedBaseUrl = null
                cachedHeartbeatInterval = null
                cachedBatteryInterval = null
                
                Log.d(TAG, "Remote Config fetched async: updated=$updated")
                continuation.resume(updated)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch async: ${e.message}")
                continuation.resume(false)
            }
    }
    
    fun clearCache() {
        cachedBaseUrl = null
        cachedHeartbeatInterval = null
        cachedBatteryInterval = null
        Log.d(TAG, "Cache cleared")
    }
    
    fun printAllSettings() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "CURRENT SERVER CONFIG")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Base URL: ${getBaseUrl()}")
        Log.d(TAG, "Heartbeat Interval: ${getHeartbeatInterval()}ms")
        Log.d(TAG, "Battery Interval: ${getBatteryUpdateInterval()}ms")
        Log.d(TAG, "Initialized: $isInitialized")
        Log.d(TAG, "========================================")
    }
}
