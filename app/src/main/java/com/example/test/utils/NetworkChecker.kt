package com.example.test.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * ?? ???? ????? ???????
 */
object NetworkChecker {
    
    private const val TAG = "NetworkChecker"
    
    /**
     * ?? ?????? ?? ??????? ???? ??? ?? ??
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork ?: run {
                Log.w(TAG, "? No active network")
                return false
            }
            
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: run {
                Log.w(TAG, "? No network capabilities")
                return false
            }
            
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            val hasTransport = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            
            val isConnected = hasInternet && hasTransport
            
            if (isConnected) {
                Log.d(TAG, "? Internet is available")
            } else {
                Log.w(TAG, "?? Internet is not available (hasInternet=$hasInternet, hasTransport=$hasTransport)")
            }
            
            isConnected
            
        } catch (e: Exception) {
            Log.e(TAG, "? Error checking internet: ${e.message}", e)
            false
        }
    }
    
    /**
     * ?????? ??? ????? (WiFi, Mobile, ?? Disconnected)
     */
    fun getConnectionType(context: Context): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.NONE
            
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                else -> ConnectionType.NONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection type: ${e.message}")
            ConnectionType.NONE
        }
    }
    
    enum class ConnectionType {
        WIFI, MOBILE, ETHERNET, NONE
    }
}
