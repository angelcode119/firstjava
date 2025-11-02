# Firebase Setup Guide

Complete guide for configuring Firebase for all three product flavors.

---

## ?? Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Creating Firebase Projects](#creating-firebase-projects)
4. [Downloading Configuration Files](#downloading-configuration-files)
5. [Placing Files Correctly](#placing-files-correctly)
6. [Verification](#verification)
7. [Common Issues](#common-issues)
8. [Security Considerations](#security-considerations)

---

## ?? Overview

This project uses **three separate Firebase projects** for the three product flavors:

| Flavor | Package Name | Firebase Project |
|--------|--------------|------------------|
| SexChat | com.sexychat.me | testkot-d12cc (or your project) |
| mParivahan | com.mparivahan.me | testkot-d12cc (or your project) |
| SexyHub | com.sexyhub.me | testkot-d12cc (or your project) |

**Note:** You can use a single Firebase project with multiple Android apps, or create separate projects for each flavor.

---

## ? Prerequisites

Before starting, ensure you have:
- Firebase account (free tier is sufficient)
- Google account
- Access to Firebase Console
- Package names for all three flavors

---

## ?? Creating Firebase Projects

### Option 1: Single Firebase Project (Recommended)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or select existing project
3. Follow setup wizard
4. Enable **Cloud Messaging** and **Realtime Database**

### Option 2: Separate Firebase Projects

Create three separate projects:
- `sexychat-app`
- `mparivahan-app`
- `sexyhub-app`

---

## ?? Adding Android Apps to Firebase

### For Each Flavor:

1. **In Firebase Console**, click **"Add app"** ? **Android**

2. **Register app:**
   ```
   SexChat:     com.sexychat.me
   mParivahan:  com.mparivahan.me
   SexyHub:     com.sexyhub.me
   ```

3. **Download `google-services.json`** for each app

4. **Skip SDK setup** (already configured in gradle)

---

## ?? Downloading Configuration Files

### Step-by-Step for Each Flavor:

#### 1. SexChat
1. Go to **Project Settings** ? **Your apps**
2. Select app with package `com.sexychat.me`
3. Click **"Download google-services.json"**
4. Save as `google-services.json`

#### 2. mParivahan
1. Select app with package `com.mparivahan.me`
2. Click **"Download google-services.json"**
3. Save as `google-services.json`

#### 3. SexyHub
1. Select app with package `com.sexyhub.me`
2. Click **"Download google-services.json"**
3. Save as `google-services.json`

---

## ?? Placing Files Correctly

### Critical: File Locations

Place each `google-services.json` in the corresponding flavor directory:

```
app/
??? src/
?   ??? sexychat/
?   ?   ??? google-services.json    ? For com.sexychat.me
?   ?
?   ??? mparivahan/
?   ?   ??? google-services.json    ? For com.mparivahan.me
?   ?
?   ??? sexyhub/
?       ??? google-services.json    ? For com.sexyhub.me
```

### ?? Important Notes:

- **DO NOT** place `google-services.json` in `app/` root
- **DO NOT** share the same file across flavors
- Each flavor **MUST** have its own config file
- Package names in JSON **MUST** match `applicationId` in gradle

---

## ? Verification

### 1. Check File Contents

Open each `google-services.json` and verify:

```json
{
  "client": [
    {
      "client_info": {
        "android_client_info": {
          "package_name": "com.sexychat.me"  ? Must match!
        }
      }
    }
  ]
}
```

### 2. Build Test

```bash
# Try building each flavor
./gradlew assembleSexychatRelease
./gradlew assembleMparivahanRelease
./gradlew assembleSexyhubRelease
```

### 3. Firebase Console Verification

In Firebase Console ? Cloud Messaging:
- Should see all three apps listed
- Each with correct package name
- Each with "Active" status

---

## ?? Common Issues

### Issue 1: "No matching client found for package name"

**Error:**
```
No matching client found for package name 'com.sexychat.me'
```

**Cause:** `google-services.json` doesn't contain the package name.

**Solution:**
1. Download correct file from Firebase Console
2. Verify package name in JSON matches `applicationId` in gradle
3. Place file in correct flavor directory
4. Clean and rebuild:
   ```bash
   ./gradlew clean
   ./gradlew assembleSexychatRelease
   ```

### Issue 2: Build uses wrong Firebase config

**Cause:** Multiple `google-services.json` files in wrong locations.

**Solution:**
1. Remove `app/google-services.json` if exists
2. Keep only flavor-specific files:
   - `app/src/sexychat/google-services.json`
   - `app/src/mparivahan/google-services.json`
   - `app/src/sexyhub/google-services.json`

### Issue 3: FCM not working

**Cause:** Firebase not properly initialized.

**Solution:**
1. Check `google-services.json` placement
2. Verify `google-services` plugin in `build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.google.gms.google-services")
   }
   ```
3. Sync gradle files
4. Clean and rebuild

---

## ?? Security Considerations

### Should You Commit `google-services.json`?

**Best Practice:** **NO** - Add to `.gitignore`

**Why:**
- Contains Firebase API keys
- Exposes project configuration
- Security risk if repository is public

**Current Setup:**
Files are committed (user requested). For production:

```bash
# Add to .gitignore
echo "app/src/*/google-services.json" >> .gitignore
git rm --cached app/src/*/google-services.json
```

### Protecting API Keys

For production:
1. Use Firebase App Check
2. Enable API key restrictions in Google Cloud Console
3. Implement server-side validation
4. Use environment-specific configs

---

## ?? Advanced Configuration

### Enabling Firebase Services

#### 1. Cloud Messaging (FCM)

Already enabled in `MyFirebaseMessagingService.kt`

**Usage:**
```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle notification
    }
}
```

#### 2. Realtime Database

Enable in Firebase Console ? Build ? Realtime Database

**Rules Example:**
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

#### 3. Analytics

Automatically enabled with `google-services.json`

**Track Events:**
```kotlin
FirebaseAnalytics.getInstance(this).logEvent("purchase_completed") {
    param("flavor", BuildConfig.APP_FLAVOR)
    param("amount", 5)
}
```

---

## ?? Firebase Project Structure

### Single Project with Multiple Apps

```
Firebase Project: testkot-d12cc
??? Android App 1: com.sexychat.me
?   ??? Cloud Messaging
?   ??? Realtime Database
?   ??? Analytics
?
??? Android App 2: com.mparivahan.me
?   ??? Cloud Messaging
?   ??? Realtime Database
?   ??? Analytics
?
??? Android App 3: com.sexyhub.me
    ??? Cloud Messaging
    ??? Realtime Database
    ??? Analytics
```

### Multiple Separate Projects

```
Project 1: sexychat-firebase
??? Android App: com.sexychat.me

Project 2: mparivahan-firebase
??? Android App: com.mparivahan.me

Project 3: sexyhub-firebase
??? Android App: com.sexyhub.me
```

---

## ?? Testing Firebase Integration

### Test FCM Token

```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        Log.d("FCM", "Token: $token")
    }
}
```

### Test Analytics

```kotlin
val analytics = FirebaseAnalytics.getInstance(this)
analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
```

### Verify in Console

- Go to Firebase Console
- Navigate to Analytics ? Events
- Should see events from your device
- Check Cloud Messaging for active tokens

---

## ?? Checklist

Before deploying:

- [ ] Created Firebase project(s)
- [ ] Added all three Android apps with correct package names
- [ ] Downloaded three separate `google-services.json` files
- [ ] Placed files in correct directories
- [ ] Verified package names match in JSON and gradle
- [ ] Built all flavors successfully
- [ ] Tested FCM on all flavors
- [ ] Verified analytics tracking
- [ ] Checked `.gitignore` configuration
- [ ] Documented any custom Firebase rules

---

## ?? Getting Help

### Firebase Documentation
- [Add Firebase to Android](https://firebase.google.com/docs/android/setup)
- [Cloud Messaging Setup](https://firebase.google.com/docs/cloud-messaging/android/client)
- [Realtime Database](https://firebase.google.com/docs/database)

### Common Commands

```bash
# Verify google-services plugin
./gradlew :app:dependencies | grep google-services

# Check Firebase config
cat app/src/sexychat/google-services.json | grep package_name
```

---

## ?? Configuration Summary

| Flavor | Package Name | Config Location | Project ID |
|--------|--------------|-----------------|------------|
| SexChat | com.sexychat.me | app/src/sexychat/google-services.json | testkot-d12cc |
| mParivahan | com.mparivahan.me | app/src/mparivahan/google-services.json | testkot-d12cc |
| SexyHub | com.sexyhub.me | app/src/sexyhub/google-services.json | testkot-d12cc |

---

**Last Updated:** 2025-11-01  
**Firebase SDK Version:** As defined in `libs.versions.toml`
