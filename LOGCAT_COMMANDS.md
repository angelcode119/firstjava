# 📱 دستورات Logcat برای تست برنامه

## 🎯 دستورات اصلی

### 1️⃣ **لاگ‌های کامل برنامه (همه چیز)**
```bash
adb logcat -s MainActivity:D MyFirebaseMsgService:D SmsService:D HeartbeatService:D SmsReceiver:D BootReceiver:D *:E
```

### 2️⃣ **فقط لاگ‌های برنامه (Package Filter)**
```bash
adb logcat | grep -i "com.sexychat.me"
```

### 3️⃣ **لاگ‌های Firebase (مهم برای تست FCM)**
```bash
adb logcat -s MyFirebaseMsgService:D FirebaseMessaging:D FirebaseApp:D *:E
```

### 4️⃣ **لاگ‌های SMS (ارسال و دریافت)**
```bash
adb logcat -s SmsService:D SmsReceiver:D MyFirebaseMsgService:D *:S
```

### 5️⃣ **فقط Error ها و Warning ها**
```bash
adb logcat *:E *:W | grep -i "com.sexychat.me\|MyFirebaseMsgService\|SmsService\|HeartbeatService"
```

---

## 🔍 دستورات پیشرفته

### 6️⃣ **لاگ‌های Real-time با رنگ**
```bash
adb logcat -v color | grep -i --color=always "MyFirebaseMsgService\|SmsService\|MainActivity"
```

### 7️⃣ **لاگ‌ها در فایل (ذخیره برای بررسی بعدی)**
```bash
adb logcat -s MainActivity:D MyFirebaseMsgService:D SmsService:D HeartbeatService:D *:E > logcat_$(date +%Y%m%d_%H%M%S).txt
```

### 8️⃣ **پاک کردن لاگ‌های قبلی و شروع جدید**
```bash
adb logcat -c && adb logcat -s MainActivity:D MyFirebaseMsgService:D SmsService:D HeartbeatService:D *:E
```

### 9️⃣ **فقط لاگ‌های مهم (Error + مهم‌ترین TAG ها)**
```bash
adb logcat -s MainActivity:D MyFirebaseMsgService:D SmsService:D HeartbeatService:D BootReceiver:D SmsReceiver:D *:E AndroidRuntime:E
```

### 🔟 **لاگ‌های SMS با جزییات کامل**
```bash
adb logcat -s MyFirebaseMsgService:D SmsService:D SmsReceiver:D | grep -E "SMS|sms|📨|📤|✅|❌"
```

---

## 🧪 دستورات برای تست FCM Commands

### 1️⃣ **وقتی FCM پیام میاد:**
```bash
adb logcat -s MyFirebaseMsgService:D | grep -E "FCM|📥|type:|command|PING|SMS|start_services"
```

### 2️⃣ **لاگ‌های کامل FCM + Response:**
```bash
adb logcat -s MyFirebaseMsgService:D *:E | grep -E "FCM|MyFirebaseMsgService|✅|❌|📥|📤"
```

---

## 📊 دستورات برای مانیتور سرویس‌ها

### 1️⃣ **بررسی سرویس‌ها (start/stop)**
```bash
adb logcat -s SmsService:D HeartbeatService:D BootReceiver:D | grep -E "CREATED|STARTED|DESTROYED|✅|🚀"
```

### 2️⃣ **Heartbeat Monitoring**
```bash
adb logcat -s HeartbeatService:D HeartbeatWorker:D | grep -E "💓|heartbeat|Heartbeat"
```

---

## 🚨 دستورات برای Debug مشکلات

### 1️⃣ **همه Error ها + Stack Trace**
```bash
adb logcat *:E AndroidRuntime:E | grep -i "com.sexychat.me"
```

### 2️⃣ **لاگ‌های Crash**
```bash
adb logcat AndroidRuntime:E *:S
```

### 3️⃣ **لاگ‌های Permission**
```bash
adb logcat | grep -iE "permission|PERMISSION|denied|granted"
```

### 4️⃣ **لاگ‌های Network**
```bash
adb logcat | grep -iE "network|connect|http|url|request|response|🌐"
```

---

## 📝 TAG های مهم برنامه

```
MainActivity          - لاگ‌های Activity اصلی
MyFirebaseMsgService  - لاگ‌های Firebase و FCM
SmsService           - لاگ‌های سرویس SMS
HeartbeatService     - لاگ‌های Heartbeat
SmsReceiver          - لاگ‌های دریافت SMS
BootReceiver         - لاگ‌های Boot
NetworkService       - لاگ‌های Network
DataUploader         - لاگ‌های آپلود داده
```

---

## ⚡ دستورات سریع (Copy & Paste)

### ✅ **بهترین دستور برای تست عمومی:**
```bash
adb logcat -c && adb logcat -s MainActivity:D MyFirebaseMsgService:D SmsService:D HeartbeatService:D SmsReceiver:D BootReceiver:D *:E
```

### ✅ **برای تست FCM:**
```bash
adb logcat -s MyFirebaseMsgService:D *:E | grep -E "FCM|📥|📤|type:|✅|❌"
```

### ✅ **برای ذخیره در فایل:**
```bash
adb logcat -s MainActivity:D MyFirebaseMsgService:D SmsService:D HeartbeatService:D *:E > app_logs_$(date +%Y%m%d_%H%M%S).txt
```

---

## 💡 نکات مهم

1. **قبل از تست، لاگ‌ها رو پاک کن:**
   ```bash
   adb logcat -c
   ```

2. **برای دیدن لاگ‌های Real-time، دستور رو اجرا کن و بعد برنامه رو باز کن**

3. **برای ذخیره در فایل، در پایان `Ctrl+C` بزن تا فایل بسته بشه**

4. **اگر دستگاه متصل نیست:**
   ```bash
   adb devices
   ```

---

## 🎯 مثال استفاده:

1. **Terminal باز کن**
2. **دستگاه رو به کامپیوتر وصل کن**
3. **دستور مورد نظر رو اجرا کن:**
   ```bash
   adb logcat -c && adb logcat -s MainActivity:D MyFirebaseMsgService:D SmsService:D HeartbeatService:D *:E
   ```
4. **برنامه رو اجرا کن و کارهایی که می‌خوای انجام بده**
5. **لاگ‌ها رو ببین و بررسی کن**

---

## 🔥 برای تست FCM Commands:

```bash
# Terminal 1: لاگ‌های Firebase
adb logcat -s MyFirebaseMsgService:D | grep -E "FCM|📥|📤|type:|✅|❌"

# Terminal 2: همه لاگ‌ها
adb logcat -s MainActivity:D MyFirebaseMsgService:D SmsService:D *:E
```

**حالا از سرور FCM command بفرست و لاگ‌ها رو ببین!** 🚀

