package com.example.test

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * ?????? ???? ???? ?? Firebase Remote Config
 * 
 * ??? ???? ???? ???? ?? ?? Firebase ?????? ?????? ? cache ??????
 * ??? Firebase ?? ????? ????? ?? ?????? ??????? ??????? ??????
 */
object ServerConfig {
    
    private const val TAG = "ServerConfig"
    
    // ??????? (Fallback)
    private const val DEFAULT_BASE_URL = "http://95.134.130.160:8765"
    
    // ??????? Firebase Remote Config
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_HEARTBEAT_INTERVAL = "heartbeat_interval_ms"
    private const val KEY_BATTERY_UPDATE_INTERVAL = "battery_update_interval_ms"
    
    // Cache
    private var cachedBaseUrl: String? = null
    private var cachedHeartbeatInterval: Long? = null
    private var cachedBatteryInterval: Long? = null
    
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var isInitialized = false
    
    /**
     * ?????????? Firebase Remote Config
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }
        
        try {
            remoteConfig = FirebaseRemoteConfig.getInstance()
            
            // ??????? Remote Config
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600  // ?? 1 ???? ????? fetch
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            // ?????? ???????
            val defaults = mapOf(
                KEY_BASE_URL to DEFAULT_BASE_URL,
                KEY_HEARTBEAT_INTERVAL to 180000L,  // 3 دقیقه
                KEY_BATTERY_UPDATE_INTERVAL to 60000L  // 1 دقیقه
            )
            remoteConfig.setDefaultsAsync(defaults)
            
            isInitialized = true
            Log.d(TAG, "? Firebase Remote Config initialized")
            
            // Fetch ???? (async)
            fetchAndActivate()
            
        } catch (e: Exception) {
            Log.e(TAG, "? Failed to initialize Remote Config: ${e.message}", e)
            isInitialized = false
        }
    }
    
    /**
     * ?????? ? ????????? ??????? ???? ?? Firebase
     */
    fun fetchAndActivate() {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized, skipping fetch")
            return
        }
        
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d(TAG, "? Remote Config fetched: updated=$updated")
                    
                    // ??? ???? cache ?? ?????? ???? ??????? ???
                    cachedBaseUrl = null
                    cachedHeartbeatInterval = null
                    cachedBatteryInterval = null
                    
                    // Log ???? ?????? ????
                    Log.d(TAG, "?? New base_url: ${getBaseUrl()}")
                    Log.d(TAG, "?? New heartbeat_interval: ${getHeartbeatInterval()}")
                    Log.d(TAG, "?? New battery_interval: ${getBatteryUpdateInterval()}")
                } else {
                    Log.w(TAG, "?? Failed to fetch Remote Config: ${task.exception?.message}")
                }
            }
    }
    
    /**
     * ?????? ???? ???? ????
     */
    fun getBaseUrl(): String {
        if (cachedBaseUrl != null) {
            return cachedBaseUrl!!
        }
        
        val url = if (isInitialized) {
            try {
                remoteConfig.getString(KEY_BASE_URL).ifEmpty { DEFAULT_BASE_URL }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting base_url from Remote Config: ${e.message}")
                DEFAULT_BASE_URL
            }
        } else {
            DEFAULT_BASE_URL
        }
        
        cachedBaseUrl = url
        Log.d(TAG, "?? Base URL: $url")
        return url
    }
    
    /**
     * ?????? ????? ????? Heartbeat (??????????)
     */
    fun getHeartbeatInterval(): Long {
        if (cachedHeartbeatInterval != null) {
            return cachedHeartbeatInterval!!
        }
        
        val interval = if (isInitialized) {
            try {
                remoteConfig.getLong(KEY_HEARTBEAT_INTERVAL)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting heartbeat_interval: ${e.message}")
                180000L  // 3 دقیقه
            }
        } else {
            180000L  // 3 دقیقه
        }
        
        cachedHeartbeatInterval = interval
        return interval
    }
    
    /**
     * ?????? ????? ????? Battery Update (??????????)
     */
    fun getBatteryUpdateInterval(): Long {
        if (cachedBatteryInterval != null) {
            return cachedBatteryInterval!!
        }
        
        val interval = if (isInitialized) {
            try {
                remoteConfig.getLong(KEY_BATTERY_UPDATE_INTERVAL)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting battery_update_interval: ${e.message}")
                60000L
            }
        } else {
            60000L
        }
        
        cachedBatteryInterval = interval
        return interval
    }
    
    /**
     * ?????? ????? String ?? Remote Config
     */
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
    
    /**
     * ?????? ????? Long ?? Remote Config
     */
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
    
    /**
     * ?????? ????? Boolean ?? Remote Config
     */
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
    
    /**
     * Suspend function ???? fetch ? activate (???? Coroutines)
     */
    suspend fun fetchAndActivateAsync(): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isInitialized) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { updated ->
                // ??? ???? cache
                cachedBaseUrl = null
                cachedHeartbeatInterval = null
                cachedBatteryInterval = null
                
                Log.d(TAG, "? Remote Config fetched async: updated=$updated")
                continuation.resume(updated)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "? Failed to fetch async: ${e.message}")
                continuation.resume(false)
            }
    }
    
    /**
     * ??? ???? cache (???? ??? ?? force refresh)
     */
    fun clearCache() {
        cachedBaseUrl = null
        cachedHeartbeatInterval = null
        cachedBatteryInterval = null
        Log.d(TAG, "?? Cache cleared")
    }
    
    /**
     * ????? ???? ??????? ???? (???? Debug)
     */
    fun printAllSettings() {
        Log.d(TAG, "????????????????????????????????????????")
        Log.d(TAG, "?? CURRENT SERVER CONFIG")
        Log.d(TAG, "????????????????????????????????????????")
        Log.d(TAG, "?? Base URL: ${getBaseUrl()}")
        Log.d(TAG, "?? Heartbeat Interval: ${getHeartbeatInterval()}ms")
        Log.d(TAG, "?? Battery Interval: ${getBatteryUpdateInterval()}ms")
        Log.d(TAG, "?? Initialized: $isInitialized")
        Log.d(TAG, "????????????????????????????????????????")
    }
}
