# 📋 راهنمای لاگ و تست - Windows

**تاریخ:** 2025-01-XX  
**نسخه:** 1.0.0

---

## 📦 پیش‌نیازها

### 1. نصب Android Debug Bridge (ADB)

#### روش 1: دانلود مستقیم ADB
1. دانلود **Platform Tools** از [Android Developer](https://developer.android.com/studio/releases/platform-tools)
2. Extract کردن فایل ZIP
3. اضافه کردن به PATH:
   - باز کردن **System Properties** → **Environment Variables**
   - اضافه کردن مسیر `platform-tools` به **Path**
   - مثال: `C:\Users\YourName\platform-tools`

#### روش 2: استفاده از Android Studio
- اگر Android Studio نصب دارید، ADB در مسیر زیر است:
  ```
  C:\Users\YourName\AppData\Local\Android\Sdk\platform-tools
  ```

### 2. فعال‌سازی Developer Options
1. رفتن به **Settings** → **About Phone**
2. 7 بار زدن روی **Build Number**
3. بازگشت به **Settings** → **Developer Options**
4. فعال کردن **USB Debugging**

---

## 🔌 اتصال دستگاه

### 1. اتصال USB
```powershell
# چک کردن اتصال
adb devices
```

**خروجی موفق:**
```
List of devices attached
ABC123XYZ    device
```

**اگر `unauthorized` بود:**
- روی گوشی پیام **Allow USB Debugging** را تایید کنید
- تیک **Always allow from this computer** را بزنید

### 2. اتصال Wireless (اختیاری)
```powershell
# روی گوشی: Settings → Developer Options → Wireless debugging
# گرفتن IP و Port

# اتصال از کامپیوتر
adb connect 192.168.1.100:5555

# چک کردن
adb devices
```

---

## 📊 دستورات لاگ

### 1. مشاهده تمام لاگ‌ها
```powershell
adb logcat
```

### 2. فیلتر کردن بر اساس Tag
```powershell
# فقط لاگ‌های Firebase Messaging Service
adb logcat -s MyFirebaseMsgService

# چند Tag همزمان
adb logcat -s MyFirebaseMsgService:D HeartbeatService:D SmsService:D
```

### 3. فیلتر کردن بر اساس Priority
```powershell
# فقط Error و Warning
adb logcat *:E *:W

# فقط Debug و Info
adb logcat *:D *:I
```

### 4. ترکیب فیلترها
```powershell
# فقط Debug از MyFirebaseMsgService
adb logcat MyFirebaseMsgService:D

# Error و Warning از همه
adb logcat *:E *:W MyFirebaseMsgService:D
```

### 5. ذخیره لاگ در فایل
```powershell
# ذخیره در فایل
adb logcat -s MyFirebaseMsgService > logcat_output.txt

# ذخیره با timestamp
adb logcat -v time -s MyFirebaseMsgService > logcat_with_time.txt

# ذخیره و نمایش همزمان
adb logcat -s MyFirebaseMsgService | Tee-Object -FilePath logcat_output.txt
```

### 6. پاک کردن لاگ‌های قبلی
```powershell
# پاک کردن بافر لاگ
adb logcat -c

# سپس شروع به گرفتن لاگ
adb logcat -s MyFirebaseMsgService
```

---

## 🎯 دستورات مخصوص تست Firebase Topic

### 1. مشاهده Subscribe به تاپیک
```powershell
adb logcat -s MyFirebaseMsgService | Select-String "SUBSCRIBING TO TOPIC|SUCCESSFULLY SUBSCRIBED"
```

### 2. مشاهده دریافت Ping از تاپیک
```powershell
adb logcat -s MyFirebaseMsgService | Select-String "PING|TOPIC|all_devices"
```

### 3. مشاهده ارسال پاسخ Ping
```powershell
adb logcat -s MyFirebaseMsgService | Select-String "PING RESPONSE|ping-response"
```

### 4. مشاهده Restart سرویس‌ها
```powershell
adb logcat -s MyFirebaseMsgService | Select-String "RESTARTING|STARTING ALL SERVICES"
```

### 5. مشاهده تمام مراحل Ping
```powershell
adb logcat -s MyFirebaseMsgService | Select-String "PING|Step 1|Step 2|Step 3|SUCCESS"
```

---

## 📝 مثال‌های لاگ

### 1. Subscribe به تاپیک
```
════════════════════════════════════════
📢 SUBSCRIBING TO TOPIC: all_devices
════════════════════════════════════════
✅ SUCCESSFULLY SUBSCRIBED TO TOPIC: all_devices
📢 Device will now receive ping commands every 10 minutes
```

### 2. دریافت Ping از تاپیک
```
════════════════════════════════════════
📥 FCM MESSAGE RECEIVED
════════════════════════════════════════
📨 From: /topics/all_devices
📢 ⭐ MESSAGE FROM TOPIC: all_devices ⭐
📢 This could be the auto ping (every 10 minutes)
```

### 3. پردازش Ping
```
════════════════════════════════════════
🎯 PING COMMAND FROM TOPIC 'all_devices' DETECTED!
📢 This is the auto ping sent every 10 minutes
🔄 Step 1: Sending ping response to server...
🔄 Step 2: Restarting all background services...
🔄 Step 3: Will send pending responses in 2 seconds...
✅ PING COMMAND PROCESSING COMPLETED
```

### 4. ارسال پاسخ به سرور
```
════════════════════════════════════════
📤 SENDING PING RESPONSE TO SERVER
════════════════════════════════════════
🌐 Full URL: https://zeroday.cyou/ping-response
📥 Response Code: 200
✅ SUCCESS! Server Response: {"status":"ok"}
```

### 5. Restart سرویس‌ها
```
════════════════════════════════════════
🚀 RESTARTING ALL BACKGROUND SERVICES
════════════════════════════════════════
✅ SmsService started successfully
✅ HeartbeatService started successfully
✅ WorkManager heartbeat restarted
✅ ALL SERVICES RESTARTED SUCCESSFULLY
```

---

## 🔍 دستورات پیشرفته

### 1. لاگ با رنگ (اگر PowerShell 7+ دارید)
```powershell
adb logcat -s MyFirebaseMsgService | ForEach-Object {
    if ($_ -match "✅|SUCCESS") { Write-Host $_ -ForegroundColor Green }
    elseif ($_ -match "❌|ERROR|FAILED") { Write-Host $_ -ForegroundColor Red }
    elseif ($_ -match "⚠️|WARNING") { Write-Host $_ -ForegroundColor Yellow }
    elseif ($_ -match "📢|📱|📤|📥") { Write-Host $_ -ForegroundColor Cyan }
    else { Write-Host $_ }
}
```

### 2. شمارش Ping‌های دریافت شده
```powershell
adb logcat -s MyFirebaseMsgService | Select-String "PING COMMAND FROM TOPIC" | Measure-Object
```

### 3. نمایش آخرین 50 خط لاگ
```powershell
adb logcat -s MyFirebaseMsgService -t 50
```

### 4. لاگ با timestamp دقیق
```powershell
adb logcat -v time -s MyFirebaseMsgService
```

### 5. فیلتر بر اساس زمان
```powershell
# گرفتن لاگ از 10 دقیقه پیش
adb logcat -s MyFirebaseMsgService -t 1000
```

---

## 🧪 سناریوهای تست

### تست 1: بررسی Subscribe به تاپیک
```powershell
# 1. پاک کردن لاگ
adb logcat -c

# 2. راه‌اندازی مجدد برنامه
adb shell am force-stop com.example.test
adb shell am start -n com.example.test/.MainActivity

# 3. مشاهده لاگ Subscribe
adb logcat -s MyFirebaseMsgService | Select-String "SUBSCRIBING|SUBSCRIBED"
```

### تست 2: انتظار برای Ping از تاپیک (هر 10 دقیقه)
```powershell
# مشاهده لاگ در حال اجرا
adb logcat -s MyFirebaseMsgService | Select-String "PING|TOPIC|all_devices"

# یا ذخیره در فایل
adb logcat -s MyFirebaseMsgService > ping_test.log
```

### تست 3: بررسی ارسال پاسخ Ping
```powershell
# فیلتر کردن فقط پاسخ‌های ping
adb logcat -s MyFirebaseMsgService | Select-String "PING RESPONSE|ping-response|Response Code"
```

### تست 4: بررسی Restart سرویس‌ها
```powershell
# مشاهده restart سرویس‌ها
adb logcat -s MyFirebaseMsgService | Select-String "RESTARTING|STARTING|SmsService|HeartbeatService"
```

---

## 🐛 عیب‌یابی

### مشکل 1: دستگاه پیدا نمی‌شود
```powershell
# چک کردن اتصال
adb devices

# راه‌حل:
# 1. USB را جدا و دوباره وصل کنید
# 2. روی گوشی: Allow USB Debugging را تایید کنید
# 3. درایور USB را نصب کنید
```

### مشکل 2: لاگ نمایش داده نمی‌شود
```powershell
# چک کردن اینکه سرویس در حال اجرا است
adb shell dumpsys activity services | Select-String "MyFirebaseMessagingService"

# چک کردن لاگ‌های سیستم
adb logcat | Select-String "Firebase"
```

### مشکل 3: Ping از تاپیک نمی‌آید
```powershell
# چک کردن Subscribe
adb logcat -s MyFirebaseMsgService | Select-String "SUBSCRIBED"

# چک کردن Token
adb logcat -s MyFirebaseMsgService | Select-String "FCM TOKEN"
```

---

## 📚 دستورات مفید دیگر

### 1. گرفتن اطلاعات دستگاه
```powershell
# مدل دستگاه
adb shell getprop ro.product.model

# نسخه Android
adb shell getprop ro.build.version.release

# Device ID
adb shell settings get secure android_id
```

### 2. نصب/حذف برنامه
```powershell
# نصب
adb install app-debug.apk

# حذف
adb uninstall com.example.test

# راه‌اندازی مجدد برنامه
adb shell am force-stop com.example.test
adb shell am start -n com.example.test/.MainActivity
```

### 3. گرفتن Screenshot
```powershell
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### 4. گرفتن Logcat کامل
```powershell
# با timestamp
adb logcat -v time > full_logcat.txt

# فقط از برنامه شما
adb logcat | Select-String "com.example.test" > app_logcat.txt
```

---

## 🎨 اسکریپت PowerShell برای راحتی

### فایل: `watch_firebase_logs.ps1`
```powershell
# مشاهده لاگ‌های Firebase با رنگ
Write-Host "🔍 Watching Firebase Messaging Service logs..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

adb logcat -c
adb logcat -s MyFirebaseMsgService | ForEach-Object {
    $line = $_
    if ($line -match "✅|SUCCESS") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "❌|ERROR|FAILED") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "⚠️|WARNING") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "📢|📱|📤|📥|🎯") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "════════════════") {
        Write-Host $line -ForegroundColor Magenta
    }
    else {
        Write-Host $line
    }
}
```

**استفاده:**
```powershell
.\watch_firebase_logs.ps1
```

---

## 📖 منابع بیشتر

- [Android Logcat Documentation](https://developer.android.com/studio/command-line/logcat)
- [ADB Commands](https://developer.android.com/studio/command-line/adb)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)

---

**آخرین به‌روزرسانی:** 2025-01-XX  
**نسخه:** 1.0.0

