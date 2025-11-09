# 🔐 راهنمای کامل دیالوگ Permission

این دیالوگ برای درخواست و مدیریت Permission‌های اپلیکیشن طراحی شده.

---

## ✨ **ویژگی‌ها**

### **1️⃣ نمایش Real-time وضعیت:**
- ✅ نشون می‌ده کدوم Permission‌ها داده شده
- ❌ نشون می‌ده کدوم Permission‌ها داده نشده
- 🔄 هر 500ms وضعیت رو آپدیت می‌کنه

### **2️⃣ غیرقابل بستن:**
- کاربر نمی‌تونه دیالوگ رو ببنده
- باید تمام Permission‌ها رو بده

### **3️⃣ راهنمایی هوشمند:**
- بعد از 2 بار تلاش ناموفق، یک هشدار نشون میده
- یک دکمه "Open Settings" ظاهر میشه

### **4️⃣ دسترسی به Settings:**
- بعد از 2 بار تلاش، کاربر می‌تونه مستقیم به Settings بره
- باز می‌کنه صفحه تنظیمات اپ رو

---

## 📋 **لیست Permission‌ها**

| آیکون | عنوان | توضیحات | Permission |
|------|--------|---------|-----------|
| 📨 | Read SMS | Required to read messages | `READ_SMS` |
| 📩 | Receive SMS | Required to receive messages | `RECEIVE_SMS` |
| 📤 | Send SMS | Required to send messages | `SEND_SMS` |
| 📱 | Phone State | Required to read phone info | `READ_PHONE_STATE` |
| 📞 | Make Calls | Required for call features | `CALL_PHONE` |
| 👥 | Read Contacts | Required to access contacts | `READ_CONTACTS` |
| 📋 | Call History | Required to read call logs | `READ_CALL_LOG` |
| 🔋 | Battery Optimization | Disable to run in background | `BATTERY_OPTIMIZATION` |

---

## 🎨 **نمای دیالوگ**

### **حالت 1: همه داده نشده (اولین بار)**

```
┌─────────────────────────────────────┐
│              🔐                     │
│      Required Permissions          │
│   Please grant all permissions     │
├─────────────────────────────────────┤
│                                     │
│  📨  Read SMS                    ❌ │
│      Required to read messages      │
│                                     │
│  📩  Receive SMS                 ❌ │
│      Required to receive messages   │
│                                     │
│  📤  Send SMS                    ❌ │
│      Required to send messages      │
│                                     │
│  📱  Phone State                 ❌ │
│      Required to read phone info    │
│                                     │
│  📞  Make Calls                  ❌ │
│      Required for call features     │
│                                     │
│  👥  Read Contacts               ❌ │
│      Required to access contacts    │
│                                     │
│  📋  Call History                ❌ │
│      Required to read call logs     │
│                                     │
│  🔋  Battery Optimization        ❌ │
│      Disable to run in background   │
│                                     │
├─────────────────────────────────────┤
│   [ Grant Permissions ]             │
└─────────────────────────────────────┘
```

---

### **حالت 2: بعضی داده شده**

```
┌─────────────────────────────────────┐
│              🔐                     │
│      Required Permissions          │
│   Please grant all permissions     │
├─────────────────────────────────────┤
│                                     │
│  📨  Read SMS                    ✅ │
│  📩  Receive SMS                 ✅ │
│  📤  Send SMS                    ✅ │
│  📱  Phone State                 ✅ │
│  📞  Make Calls                  ✅ │
│  👥  Read Contacts               ❌ │  ← هنوز نداده
│  📋  Call History                ❌ │  ← هنوز نداده
│  🔋  Battery Optimization        ❌ │  ← هنوز نداده
│                                     │
├─────────────────────────────────────┤
│   [ Try Again ]                     │
└─────────────────────────────────────┘
```

---

### **حالت 3: بعد از 2 بار تلاش (با راهنمایی)**

```
┌─────────────────────────────────────┐
│              🔐                     │
│      Required Permissions          │
│   Please grant all permissions     │
├─────────────────────────────────────┤
│                                     │
│  📨  Read SMS                    ✅ │
│  📩  Receive SMS                 ✅ │
│  📤  Send SMS                    ✅ │
│  📱  Phone State                 ✅ │
│  📞  Make Calls                  ✅ │
│  👥  Read Contacts               ❌ │
│  📋  Call History                ❌ │
│  🔋  Battery Optimization        ❌ │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ⚠️ Having trouble?          │   │
│  │ Try opening Settings        │   │
│  │ manually and grant all      │   │
│  │ permissions.                │   │
│  └─────────────────────────────┘   │
│                                     │
├─────────────────────────────────────┤
│   [ Try Again ]                     │
│   [ ⚙️ Open Settings ]              │
└─────────────────────────────────────┘
```

---

### **حالت 4: همه داده شده ✅**

```
┌─────────────────────────────────────┐
│              🔐                     │
│      Required Permissions          │
│   Please grant all permissions     │
├─────────────────────────────────────┤
│                                     │
│  📨  Read SMS                    ✅ │
│  📩  Receive SMS                 ✅ │
│  📤  Send SMS                    ✅ │
│  📱  Phone State                 ✅ │
│  📞  Make Calls                  ✅ │
│  👥  Read Contacts               ✅ │
│  📋  Call History                ✅ │
│  🔋  Battery Optimization        ✅ │
│                                     │
│  → دیالوگ خودکار بسته میشه         │
└─────────────────────────────────────┘
```

---

## 🔄 **نحوه کار**

### **Flow Diagram:**

```
┌───────────────────┐
│  اپ باز میشه      │
└────────┬──────────┘
         │
         ▼
  ┌────────────────┐
  │ همه Permission  │
  │ داده شده؟      │
  └──┬─────────┬───┘
     │         │
    بله       خیر
     │         │
     ▼         ▼
  ┌─────┐  ┌──────────────┐
  │ ادامه│  │ دیالوگ نشون  │
  │ اپ  │  │ داده میشه    │
  └─────┘  └──────┬───────┘
                  │
                  ▼
           ┌─────────────────┐
           │ کاربر دکمه رو    │
           │ می‌زنه           │
           └─────┬───────────┘
                 │
                 ▼
         ┌──────────────────┐
         │ Android Dialog    │
         │ Permission        │
         └───┬──────────┬────┘
             │          │
           Allow      Deny
             │          │
             ▼          ▼
        ┌────────┐  ┌────────┐
        │ ✅     │  │ ❌      │
        │ داده   │  │ نداده  │
        └────────┘  └────┬───┘
                         │
                         ▼
                  ┌─────────────┐
                  │ تعداد تلاش  │
                  │ >= 2 ؟     │
                  └──┬──────┬───┘
                     │      │
                    بله    خیر
                     │      │
                     ▼      ▼
              ┌──────────┐  ┌─────┐
              │ دکمه     │  │ دوباره│
              │ Settings │  │ تلاش │
              │ ظاهر میشه│  └─────┘
              └──────────┘
```

---

## 💻 **کد استفاده**

### **در MainActivity:**

```kotlin
import com.example.test.utils.PermissionDialog
import com.example.test.utils.PermissionManager

class MainActivity : ComponentActivity() {
    
    private lateinit var permissionManager: PermissionManager
    private var showPermissionDialog by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // مقداردهی اولیه
        permissionManager = PermissionManager(this)
        permissionManager.initialize {
            // وقتی همه Permission‌ها داده شد
            showPermissionDialog = false
            // ادامه اپ
        }
        
        // چک کردن Permission‌ها
        if (!permissionManager.checkAllPermissions()) {
            showPermissionDialog = true
        }
        
        setContent {
            if (showPermissionDialog) {
                PermissionDialog(
                    onRequestPermissions = {
                        scope.launch {
                            permissionManager.requestPermissions {
                                if (permissionManager.checkAllPermissions()) {
                                    showPermissionDialog = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
```

---

## 🎯 **ویژگی‌های کلیدی**

### **1. Real-time Update (هر 500ms):**

```kotlin
LaunchedEffect(Unit) {
    while (true) {
        if (activity != null) {
            // چک کردن وضعیت همه Permission‌ها
            val states = permissions.associate { item ->
                item.permission to (ContextCompat.checkSelfPermission(
                    activity,
                    item.permission
                ) == PackageManager.PERMISSION_GRANTED)
            }
            permissionStates = states
            
            // چک Battery Optimization
            val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            batteryOptimization = pm.isIgnoringBatteryOptimizations(activity.packageName)
        }
        delay(500)
    }
}
```

---

### **2. Attempt Counter:**

```kotlin
var attemptCount by remember { mutableStateOf(0) }

Button(
    onClick = {
        attemptCount++  // شمارش تلاش‌ها
        onRequestPermissions()
    }
)
```

---

### **3. Conditional Settings Button:**

```kotlin
// فقط بعد از 2 بار تلاش و داشتن Permission نداده شده
if (attemptCount >= 2 && hasAnyDenied && activity != null) {
    OutlinedButton(
        onClick = {
            // باز کردن صفحه Settings اپ
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        }
    ) {
        Text("⚙️ Open Settings")
    }
}
```

---

### **4. Warning Card:**

```kotlin
// هشدار بعد از 2 بار تلاش ناموفق
if (attemptCount >= 2 && hasAnyDenied) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3CD)  // زرد
        )
    ) {
        Column {
            Text("⚠️ Having trouble?")
            Text("Try opening Settings manually...")
        }
    }
}
```

---

## 🧪 **تست کردن**

### **1. تست حالت عادی:**
```bash
# کاربر همه Permission‌ها رو میده
✅ دیالوگ بسته میشه
✅ اپ شروع میشه
```

### **2. تست رد کردن:**
```bash
# کاربر یک یا چند Permission رو رد می‌کنه
❌ دیالوگ باز می‌مونه
🔄 نشون میده کدوما داده نشده
🔄 دکمه عوض میشه به "Try Again"
```

### **3. تست چند بار رد:**
```bash
# کاربر 2 بار یا بیشتر رد می‌کنه
⚠️ هشدار نشون داده میشه
⚙️ دکمه "Open Settings" ظاهر میشه
```

### **4. تست Settings:**
```bash
# کاربر روی "Open Settings" می‌زنه
→ صفحه تنظیمات اپ باز میشه
→ کاربر می‌تونه دستی Permission بده
→ برمی‌گرده به اپ
→ دیالوگ وضعیت رو آپدیت می‌کنه
→ اگه همه رو داده باشه، دیالوگ بسته میشه
```

---

## 📊 **مقایسه با heartbeatra**

| ویژگی | heartbeatra | ما |
|-------|-------------|-----|
| نمایش لیست Permission‌ها | ✅ | ✅ |
| Real-time update | ✅ | ✅ |
| غیرقابل بستن | ✅ | ✅ |
| دکمه Settings بعد چند بار | ✅ | ✅ (بعد 2 بار) |
| هشدار راهنمایی | ❌ | ✅ |
| آیکون زیبا | ✅ | ✅ |
| Scrollable (برای صفحات کوچک) | ❌ | ✅ |

---

## 🎨 **رنگ‌ها و طراحی**

### **Gradient دکمه اصلی:**
```kotlin
Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF667eea),  // بنفش روشن
        Color(0xFF764ba2)   // بنفش تیره
    )
)
```

### **رنگ هشدار:**
```kotlin
containerColor = Color(0xFFFFF3CD)  // زرد روشن
textColor = Color(0xFF856404)        // قهوه‌ای تیره
```

---

## 💡 **نکات مهم**

### **1. Performance:**
- ✅ چک هر 500ms (نه خیلی سریع، نه خیلی کند)
- ✅ از LaunchedEffect استفاده میشه (Cancel میشه وقتی دیالوگ بسته بشه)

### **2. UX:**
- ✅ کاربر می‌بینه دقیقاً کدوم Permission نداده
- ✅ راهنمایی هوشمند بعد از 2 بار تلاش
- ✅ دسترسی مستقیم به Settings

### **3. Security:**
- ✅ نمیشه دیالوگ رو بست (`onDismissRequest = {}`)
- ✅ تا همه Permission‌ها داده نشه، اپ شروع نمیشه

---

## 🐛 **مشکلات احتمالی و راه حل**

### **مشکل 1: دیالوگ بسته میشه قبل از دادن همه Permission‌ها**
**راه حل:**
```kotlin
if (permissionManager.checkAllPermissions()) {
    showPermissionDialog = false  // فقط اینجا false کن
}
```

### **مشکل 2: وضعیت آپدیت نمیشه**
**راه حل:**
- چک کن که `LaunchedEffect` کنسل نشده باشه
- چک کن که `delay(500)` درست کار می‌کنه

### **مشکل 3: دکمه Settings کار نمی‌کنه**
**راه حل:**
```kotlin
// اطمینان حاصل کن که package name درسته
data = Uri.parse("package:${activity.packageName}")
```

---

## ✅ **خلاصه**

این دیالوگ:
- ✅ همه Permission‌ها رو نشون میده
- ✅ Real-time وضعیت رو آپدیت می‌کنه
- ✅ غیرقابل بستنه
- ✅ راهنمایی هوشمند داره
- ✅ دسترسی مستقیم به Settings می‌ده
- ✅ زیبا و user-friendly هست

---

**آخرین آپدیت:** 2025-11-09  
**نسخه:** 2.0  
**وضعیت:** ✅ تست شده و آماده استفاده

