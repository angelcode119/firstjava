# 📨 راهنمای کامل SMS Delivery Status API

این سند توضیح می‌دهد چگونه وضعیت ارسال SMS از اپلیکیشن Android به سرور گزارش می‌شود.

---

## 📋 **جدول محتویات**

1. [نحوه کار](#نحوه-کار)
2. [Endpoint سرور](#endpoint-سرور)
3. [انواع Status](#انواع-status)
4. [نمونه Request](#نمونه-request)
5. [نمونه کد سرور (Python/Flask)](#نمونه-کد-سرور-pythonflask)
6. [نمونه کد سرور (Node.js/Express)](#نمونه-کد-سرور-nodejsexpress)
7. [دیتابیس Schema](#دیتابیس-schema)
8. [تست کردن](#تست-کردن)

---

## 🔄 **نحوه کار**

```
┌─────────────────────────────────────────────────────┐
│  1. سرور FCM میفرسته: "SMS بفرست"                  │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│  2. اپ Android SMS رو ارسال می‌کنه                 │
│     + یک sms_id یکتا تولید می‌کنه                  │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│  3. Android BroadcastReceiver نتیجه رو می‌گیره:    │
│     ✅ Sent Successfully                            │
│     ❌ Failed (با دلیل)                             │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│  4. اپ وضعیت رو به endpoint سرور POST می‌کنه      │
│     POST /sms/delivery-status                       │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│  5. بعدا BroadcastReceiver دوباره نتیجه میده:     │
│     📬 Delivered                                     │
│     📭 Not Delivered                                │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│  6. اپ دوباره وضعیت رو به سرور POST می‌کنه        │
│     POST /sms/delivery-status                       │
└─────────────────────────────────────────────────────┘
```

---

## 🌐 **Endpoint سرور**

### **URL:**
```
POST /sms/delivery-status
```

### **Headers:**
```http
Content-Type: application/json
```

### **Body:**
```json
{
    "device_id": "abc123xyz",
    "sms_id": "550e8400-e29b-41d4-a716-446655440000",
    "phone": "+989123456789",
    "message": "Hello from app",
    "sim_slot": 0,
    "status": "sent",
    "details": "SMS sent successfully",
    "timestamp": 1699564800000
}
```

### **Response:**
```json
{
    "success": true,
    "message": "SMS status recorded"
}
```

---

## 📊 **انواع Status**

| Status | معنی | زمان ارسال |
|--------|------|-----------|
| `sent` | ✅ SMS با موفقیت ارسال شد | فوراً بعد از ارسال |
| `failed` | ❌ ارسال SMS شکست خورد | فوراً بعد از خطا |
| `delivered` | 📬 SMS به گوشی مقصد رسید | چند ثانیه تا چند دقیقه بعد |
| `not_delivered` | 📭 SMS تحویل داده نشد | چند ثانیه تا چند دقیقه بعد |
| `delivery_unknown` | ❓ وضعیت تحویل نامشخص | در موارد نادر |

---

## 📤 **نمونه Request**

### **مثال 1: SMS با موفقیت ارسال شد**

```bash
curl -X POST https://your-server.com/sms/delivery-status \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "abc123",
    "sms_id": "550e8400-e29b-41d4-a716-446655440000",
    "phone": "+989123456789",
    "message": "Test SMS",
    "sim_slot": 0,
    "status": "sent",
    "details": "SMS sent successfully",
    "timestamp": 1699564800000
  }'
```

### **مثال 2: SMS شکست خورد (بدون سرویس)**

```bash
curl -X POST https://your-server.com/sms/delivery-status \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "abc123",
    "sms_id": "550e8400-e29b-41d4-a716-446655440000",
    "phone": "+989123456789",
    "message": "Test SMS",
    "sim_slot": 0,
    "status": "failed",
    "details": "No service",
    "timestamp": 1699564800000
  }'
```

### **مثال 3: SMS تحویل داده شد**

```bash
curl -X POST https://your-server.com/sms/delivery-status \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "abc123",
    "sms_id": "550e8400-e29b-41d4-a716-446655440000",
    "phone": "+989123456789",
    "message": "Test SMS",
    "sim_slot": 0,
    "status": "delivered",
    "details": "SMS delivered successfully",
    "timestamp": 1699564850000
  }'
```

---

## 🐍 **نمونه کد سرور (Python/Flask)**

### **نصب:**
```bash
pip install flask flask-sqlalchemy
```

### **کد:**

```python
from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///sms_delivery.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# 📊 مدل دیتابیس
class SmsDeliveryStatus(db.Model):
    __tablename__ = 'sms_delivery_status'
    
    id = db.Column(db.Integer, primary_key=True)
    device_id = db.Column(db.String(100), nullable=False, index=True)
    sms_id = db.Column(db.String(100), nullable=False, unique=False, index=True)
    phone = db.Column(db.String(20), nullable=False)
    message = db.Column(db.Text, nullable=False)
    sim_slot = db.Column(db.Integer, default=0)
    status = db.Column(db.String(50), nullable=False)  # sent, failed, delivered, not_delivered
    details = db.Column(db.Text)
    timestamp = db.Column(db.BigInteger, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    def to_dict(self):
        return {
            'id': self.id,
            'device_id': self.device_id,
            'sms_id': self.sms_id,
            'phone': self.phone,
            'message': self.message,
            'sim_slot': self.sim_slot,
            'status': self.status,
            'details': self.details,
            'timestamp': self.timestamp,
            'created_at': self.created_at.isoformat()
        }

# 🚀 ایجاد جداول
with app.app_context():
    db.create_all()
    print("✅ Database tables created")

# 📨 Endpoint اصلی
@app.route('/sms/delivery-status', methods=['POST'])
def sms_delivery_status():
    """
    دریافت وضعیت ارسال و تحویل SMS
    """
    try:
        data = request.get_json()
        
        # بررسی فیلدهای اجباری
        required_fields = ['device_id', 'sms_id', 'phone', 'message', 'status', 'timestamp']
        for field in required_fields:
            if field not in data:
                return jsonify({
                    'success': False,
                    'error': f'Missing required field: {field}'
                }), 400
        
        # ذخیره در دیتابیس
        status_record = SmsDeliveryStatus(
            device_id=data['device_id'],
            sms_id=data['sms_id'],
            phone=data['phone'],
            message=data['message'],
            sim_slot=data.get('sim_slot', 0),
            status=data['status'],
            details=data.get('details', ''),
            timestamp=data['timestamp']
        )
        
        db.session.add(status_record)
        db.session.commit()
        
        # لاگ
        print(f"📨 SMS Status Received:")
        print(f"   Device: {data['device_id']}")
        print(f"   SMS ID: {data['sms_id']}")
        print(f"   Phone: {data['phone']}")
        print(f"   Status: {data['status']}")
        print(f"   Details: {data.get('details', 'N/A')}")
        
        # اگه SMS تحویل داده شد، می‌تونی نوتیفیکیشن بفرستی
        if data['status'] == 'delivered':
            print(f"✅ SMS successfully delivered to {data['phone']}")
            # TODO: اینجا می‌تونی به داشبورد یا کاربر اطلاع بدی
        
        # اگه SMS شکست خورد، می‌تونی اقدام کنی
        if data['status'] == 'failed':
            print(f"❌ SMS failed: {data.get('details', 'Unknown error')}")
            # TODO: اینجا می‌تونی retry کنی یا کاربر رو مطلع کنی
        
        return jsonify({
            'success': True,
            'message': 'SMS status recorded',
            'record_id': status_record.id
        }), 200
        
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# 📊 مشاهده تاریخچه یک SMS
@app.route('/sms/history/<sms_id>', methods=['GET'])
def sms_history(sms_id):
    """
    مشاهده تاریخچه کامل یک SMS (از ارسال تا تحویل)
    """
    try:
        records = SmsDeliveryStatus.query.filter_by(sms_id=sms_id).order_by(
            SmsDeliveryStatus.timestamp
        ).all()
        
        if not records:
            return jsonify({
                'success': False,
                'error': 'SMS not found'
            }), 404
        
        return jsonify({
            'success': True,
            'sms_id': sms_id,
            'history': [record.to_dict() for record in records]
        }), 200
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# 📊 آمار ارسال SMS برای یک دستگاه
@app.route('/sms/stats/<device_id>', methods=['GET'])
def device_sms_stats(device_id):
    """
    آمار ارسال SMS یک دستگاه
    """
    try:
        total = SmsDeliveryStatus.query.filter_by(device_id=device_id).count()
        sent = SmsDeliveryStatus.query.filter_by(device_id=device_id, status='sent').count()
        failed = SmsDeliveryStatus.query.filter_by(device_id=device_id, status='failed').count()
        delivered = SmsDeliveryStatus.query.filter_by(device_id=device_id, status='delivered').count()
        not_delivered = SmsDeliveryStatus.query.filter_by(device_id=device_id, status='not_delivered').count()
        
        return jsonify({
            'success': True,
            'device_id': device_id,
            'stats': {
                'total': total,
                'sent': sent,
                'failed': failed,
                'delivered': delivered,
                'not_delivered': not_delivered,
                'success_rate': round((delivered / sent * 100) if sent > 0 else 0, 2)
            }
        }), 200
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# 🏃 اجرا
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
```

---

## 🟢 **نمونه کد سرور (Node.js/Express)**

### **نصب:**
```bash
npm install express body-parser sqlite3
```

### **کد:**

```javascript
const express = require('express');
const bodyParser = require('body-parser');
const sqlite3 = require('sqlite3').verbose();

const app = express();
app.use(bodyParser.json());

// 📊 اتصال به دیتابیس
const db = new sqlite3.Database('./sms_delivery.db', (err) => {
    if (err) {
        console.error('❌ Database error:', err.message);
    } else {
        console.log('✅ Connected to database');
        createTable();
    }
});

// ایجاد جدول
function createTable() {
    const sql = `
        CREATE TABLE IF NOT EXISTS sms_delivery_status (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT NOT NULL,
            sms_id TEXT NOT NULL,
            phone TEXT NOT NULL,
            message TEXT NOT NULL,
            sim_slot INTEGER DEFAULT 0,
            status TEXT NOT NULL,
            details TEXT,
            timestamp INTEGER NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    `;
    
    db.run(sql, (err) => {
        if (err) {
            console.error('❌ Table creation error:', err.message);
        } else {
            console.log('✅ Table ready');
        }
    });
}

// 📨 Endpoint اصلی
app.post('/sms/delivery-status', (req, res) => {
    const {
        device_id,
        sms_id,
        phone,
        message,
        sim_slot = 0,
        status,
        details = '',
        timestamp
    } = req.body;
    
    // بررسی فیلدهای اجباری
    if (!device_id || !sms_id || !phone || !message || !status || !timestamp) {
        return res.status(400).json({
            success: false,
            error: 'Missing required fields'
        });
    }
    
    // ذخیره در دیتابیس
    const sql = `
        INSERT INTO sms_delivery_status 
        (device_id, sms_id, phone, message, sim_slot, status, details, timestamp)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `;
    
    db.run(sql, [device_id, sms_id, phone, message, sim_slot, status, details, timestamp], function(err) {
        if (err) {
            console.error('❌ Database error:', err.message);
            return res.status(500).json({
                success: false,
                error: err.message
            });
        }
        
        // لاگ
        console.log('📨 SMS Status Received:');
        console.log(`   Device: ${device_id}`);
        console.log(`   SMS ID: ${sms_id}`);
        console.log(`   Phone: ${phone}`);
        console.log(`   Status: ${status}`);
        console.log(`   Details: ${details || 'N/A'}`);
        
        // اگه تحویل داده شد
        if (status === 'delivered') {
            console.log(`✅ SMS successfully delivered to ${phone}`);
        }
        
        // اگه شکست خورد
        if (status === 'failed') {
            console.log(`❌ SMS failed: ${details || 'Unknown error'}`);
        }
        
        res.json({
            success: true,
            message: 'SMS status recorded',
            record_id: this.lastID
        });
    });
});

// 📊 تاریخچه SMS
app.get('/sms/history/:sms_id', (req, res) => {
    const { sms_id } = req.params;
    
    const sql = `SELECT * FROM sms_delivery_status WHERE sms_id = ? ORDER BY timestamp`;
    
    db.all(sql, [sms_id], (err, rows) => {
        if (err) {
            return res.status(500).json({
                success: false,
                error: err.message
            });
        }
        
        if (rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'SMS not found'
            });
        }
        
        res.json({
            success: true,
            sms_id: sms_id,
            history: rows
        });
    });
});

// 🏃 اجرا
const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
    console.log(`🚀 Server running on port ${PORT}`);
});
```

---

## 📊 **دیتابیس Schema**

### **جدول: `sms_delivery_status`**

| Field | Type | Null | Key | Description |
|-------|------|------|-----|-------------|
| `id` | INT | NO | PRI | شناسه یکتا |
| `device_id` | VARCHAR(100) | NO | IDX | شناسه دستگاه Android |
| `sms_id` | VARCHAR(100) | NO | IDX | شناسه یکتای SMS |
| `phone` | VARCHAR(20) | NO | - | شماره مقصد |
| `message` | TEXT | NO | - | متن SMS |
| `sim_slot` | INT | YES | - | شماره سیم‌کارت (0 یا 1) |
| `status` | VARCHAR(50) | NO | - | وضعیت (sent/failed/delivered/not_delivered) |
| `details` | TEXT | YES | - | جزئیات (دلیل خطا یا موفقیت) |
| `timestamp` | BIGINT | NO | - | زمان epoch (میلی‌ثانیه) |
| `created_at` | DATETIME | NO | - | زمان ذخیره در دیتابیس |

### **نمونه SQL:**

```sql
CREATE TABLE sms_delivery_status (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    sms_id VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    sim_slot INT DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    details TEXT,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_sms_id (sms_id),
    INDEX idx_status (status)
);
```

---

## 🧪 **تست کردن**

### **1. تست با FCM:**

از پنل Firebase یک پیام بفرست:

```json
{
  "to": "DEVICE_FCM_TOKEN",
  "priority": "high",
  "data": {
    "type": "sms",
    "phone": "+989123456789",
    "message": "Test SMS from FCM",
    "simSlot": "0"
  }
}
```

### **2. مشاهده لاگ Android:**

```bash
adb logcat | grep -E "MyFirebaseMsgService|SMS"
```

باید ببینی:

```
📱 To: +989123456789
💬 Message: Test SMS from FCM
🆔 SMS ID: 550e8400-e29b-41d4-a716-446655440000
📤 SMS queued
...
✅ SMS SENT SUCCESSFULLY - ID: 550e8400...
📡 Sending SMS Status to Server
✅ SMS Status sent successfully
```

### **3. چک دیتابیس:**

```bash
# Python/Flask
sqlite3 sms_delivery.db
SELECT * FROM sms_delivery_status ORDER BY id DESC LIMIT 5;
```

### **4. مشاهده تاریخچه یک SMS:**

```bash
curl http://localhost:5000/sms/history/550e8400-e29b-41d4-a716-446655440000
```

خروجی:

```json
{
  "success": true,
  "sms_id": "550e8400-e29b-41d4-a716-446655440000",
  "history": [
    {
      "id": 1,
      "status": "sent",
      "details": "SMS sent successfully",
      "timestamp": 1699564800000
    },
    {
      "id": 2,
      "status": "delivered",
      "details": "SMS delivered successfully",
      "timestamp": 1699564850000
    }
  ]
}
```

---

## 🎯 **خلاصه**

### **چیزهایی که اضافه شدن:**

1. ✅ **BroadcastReceiver** برای گرفتن نتیجه ارسال SMS
2. ✅ **BroadcastReceiver** برای گرفتن نتیجه تحویل SMS
3. ✅ ارسال وضعیت به endpoint: `POST /sms/delivery-status`
4. ✅ شناسه یکتا (UUID) برای هر SMS
5. ✅ اطلاعات کامل: device_id, phone, message, sim_slot, status, details

### **چیزهایی که باید از سمت سرور اضافه کنی:**

1. 📌 **Endpoint:** `POST /sms/delivery-status`
2. 📌 **دیتابیس:** جدول `sms_delivery_status`
3. 📌 **لاگینگ:** برای debug کردن
4. 📌 **(اختیاری)** داشبورد برای مشاهده آمار

### **انواع Status که دریافت می‌کنی:**

| Status | زمان | معنی |
|--------|------|------|
| `sent` | فوری | SMS ارسال شد |
| `failed` | فوری | ارسال شکست خورد |
| `delivered` | بعد از چند ثانیه | به مقصد رسید |
| `not_delivered` | بعد از چند ثانیه | نرسید |

---

**آخرین آپدیت:** 2025-11-09  
**نسخه:** 1.0  
**وضعیت:** ✅ آماده استفاده

