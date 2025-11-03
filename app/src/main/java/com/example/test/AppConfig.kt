package com.example.test

import android.content.Context
import android.graphics.Color
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ???? ?????? ??????? ?????? ?? ???? config.json ?? assets
 */
data class AppConfig(
    val appName: String,
    val userId: String,
    val appType: String,
    val theme: ThemeConfig
) {
    data class ThemeConfig(
        val primaryColor: String,
        val secondaryColor: String,
        val accentColor: String
    ) {
        fun getPrimaryColorInt(): Int = parseColor(primaryColor)
        fun getSecondaryColorInt(): Int = parseColor(secondaryColor)
        fun getAccentColorInt(): Int = parseColor(accentColor)
        
        private fun parseColor(colorHex: String): Int {
            return try {
                Color.parseColor(colorHex)
            } catch (e: Exception) {
                Color.parseColor("#6200EE") // Default color
            }
        }
    }

    companion object {
        private const val TAG = "AppConfig"
        private var instance: AppConfig? = null

        /**
         * ?????? ??????? ?? ???? config.json
         */
        fun load(context: Context): AppConfig {
            if (instance != null) {
                return instance!!
            }

            try {
                Log.d(TAG, "?? Reading config.json from assets...")
                
                val inputStream = context.assets.open("config.json")
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.use { it.readText() }
                
                Log.d(TAG, "? Config file read successfully")
                Log.d(TAG, "?? Content: $jsonString")
                
                val json = JSONObject(jsonString)
                
                val appName = json.getString("app_name")
                val userId = json.getString("user_id")
                val appType = json.getString("app_type")
                
                val themeJson = json.getJSONObject("theme")
                val theme = ThemeConfig(
                    primaryColor = themeJson.getString("primary_color"),
                    secondaryColor = themeJson.getString("secondary_color"),
                    accentColor = themeJson.getString("accent_color")
                )
                
                instance = AppConfig(appName, userId, appType, theme)
                
                Log.d(TAG, "????????????????????????????????????????")
                Log.d(TAG, "? CONFIG LOADED SUCCESSFULLY")
                Log.d(TAG, "?? App Name: $appName")
                Log.d(TAG, "?? User ID: $userId")
                Log.d(TAG, "?? App Type: $appType")
                Log.d(TAG, "?? Primary Color: ${theme.primaryColor}")
                Log.d(TAG, "?? Secondary Color: ${theme.secondaryColor}")
                Log.d(TAG, "?? Accent Color: ${theme.accentColor}")
                Log.d(TAG, "????????????????????????????????????????")
                
                return instance!!
                
            } catch (e: Exception) {
                Log.e(TAG, "? Failed to read config.json: ${e.message}", e)
                e.printStackTrace()
                
                // Default fallback config
                val defaultConfig = AppConfig(
                    appName = "App",
                    userId = "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
                    appType = "default",
                    theme = ThemeConfig(
                        primaryColor = "#6200EE",
                        secondaryColor = "#3700B3",
                        accentColor = "#03DAC5"
                    )
                )
                
                instance = defaultConfig
                return defaultConfig
            }
        }

        /**
         * ?????? instance ???? (???? ???? load ??? ????)
         */
        fun getInstance(): AppConfig {
            return instance ?: throw IllegalStateException("AppConfig not loaded! Call load() first.")
        }
    }
}
