# Build Flavors Guide

Complete guide for managing and building multiple product flavors in this Android project.

---

## ?? Table of Contents

1. [Overview](#overview)
2. [Flavor Configuration](#flavor-configuration)
3. [Directory Structure](#directory-structure)
4. [Building Flavors](#building-flavors)
5. [Customization](#customization)
6. [Asset Management](#asset-management)
7. [Troubleshooting](#troubleshooting)

---

## ?? Overview

This project uses **Android Product Flavors** to create three distinct applications from a single codebase:

| Flavor | Package Name | App Name | Theme |
|--------|--------------|----------|-------|
| **sexychat** | com.sexychat.me | Sexy Chat | Pink/Purple |
| **mparivahan** | com.mparivahan.me | mParivahan | Blue |
| **sexyhub** | com.sexyhub.me | Sexy Hub | Pink/Red |

---

## ?? Flavor Configuration

### Build Configuration (`app/build.gradle.kts`)

```kotlin
android {
    flavorDimensions += "version"
    
    productFlavors {
        create("sexychat") {
            dimension = "version"
            applicationId = "com.sexychat.me"
            versionNameSuffix = "-sexychat"
            buildConfigField("String", "APP_FLAVOR", "\"sexychat\"")
            buildConfigField("String", "APP_THEME", "\"sexy\"")
            resValue("string", "flavor_app_name", "Sexy Chat")
        }
        
        create("mparivahan") {
            dimension = "version"
            applicationId = "com.mparivahan.me"
            versionNameSuffix = "-mparivahan"
            buildConfigField("String", "APP_FLAVOR", "\"mparivahan\"")
            buildConfigField("String", "APP_THEME", "\"transport\"")
            resValue("string", "flavor_app_name", "mParivahan")
        }
        
        create("sexyhub") {
            dimension = "version"
            applicationId = "com.sexyhub.me"
            versionNameSuffix = "-sexyhub"
            buildConfigField("String", "APP_FLAVOR", "\"sexyhub\"")
            buildConfigField("String", "APP_THEME", "\"hub\"")
            resValue("string", "flavor_app_name", "Sexy Hub")
        }
    }
}
```

---

## ?? Directory Structure

### File Override Hierarchy

Android resolves resources in this order (first match wins):

1. `app/src/<flavorName>/` (Flavor-specific)
2. `app/src/main/` (Common/shared)

### Example Structure

```
app/src/
??? main/
?   ??? assets/
?   ?   ??? payment.html          # Shared payment UI
?   ?   ??? googlepay-splash.html # Shared splash
?   ?   ??? upi-pin.html          # Shared UPI PIN
?   ??? java/
?   ?   ??? com/example/test/
?   ?       ??? MainActivity.kt   # Main activity
?   ??? res/
?       ??? values/
?           ??? strings.xml       # Common strings
?
??? sexychat/
?   ??? assets/
?   ?   ??? index.html            # SexChat splash
?   ?   ??? register.html         # SexChat registration
?   ?   ??? [payment icons]       # Payment method icons
?   ??? google-services.json      # Firebase for com.sexychat.me
?   ??? res/values/
?       ??? strings.xml           # SexChat strings
?
??? mparivahan/
?   ??? assets/
?   ?   ??? index.html            # mParivahan splash
?   ?   ??? register.html         # Vehicle registration
?   ?   ??? [payment icons]       # Payment method icons
?   ??? google-services.json      # Firebase for com.mparivahan.me
?   ??? res/values/
?       ??? strings.xml           # mParivahan strings
?
??? sexyhub/
    ??? assets/
    ?   ??? index.html            # Video library (no splash)
    ?   ??? [video images]        # Thumbnail images
    ?   ??? [payment icons]       # Payment method icons
    ??? google-services.json      # Firebase for com.sexyhub.me
    ??? res/values/
        ??? strings.xml           # SexyHub strings
```

---

## ??? Building Flavors

### Command Line

**Build all flavors (Release only):**
```bash
./gradlew assembleSexychatRelease assembleMparivahanRelease assembleSexyhubRelease
```

**Build individual flavors:**
```bash
# SexChat
./gradlew assembleSexychatRelease

# mParivahan
./gradlew assembleMparivahanRelease

# SexyHub
./gradlew assembleSexyhubRelease
```

**Clean build:**
```bash
./gradlew clean
./gradlew assembleSexychatRelease
```

### Android Studio

1. Open **Build Variants** panel (left sidebar)
2. Select desired flavor + build type:
   - `sexychatRelease`
   - `mparivahanRelease`
   - `sexyhubRelease`
3. Click **Build ? Build Bundle(s) / APK(s) ? Build APK(s)**

### Output Location

```
app/build/outputs/apk/
??? sexychat/
?   ??? release/
?       ??? app-sexychat-release.apk
??? mparivahan/
?   ??? release/
?       ??? app-mparivahan-release.apk
??? sexyhub/
    ??? release/
        ??? app-sexyhub-release.apk
```

---

## ?? Customization

### 1. Accessing Flavor in Code

**Kotlin (MainActivity.kt):**
```kotlin
when (BuildConfig.APP_FLAVOR) {
    "sexychat" -> {
        // SexChat specific code
        appName = "SexyChat"
        gradientColors = listOf(Color(0xFFff6b9d), Color(0xFFff1493))
    }
    "mparivahan" -> {
        // mParivahan specific code
        appName = "mParivahan"
        gradientColors = listOf(Color(0xFF4fc3f7), Color(0xFF1976d2))
    }
    "sexyhub" -> {
        // SexyHub specific code - no splash
        showSplash = false
    }
}
```

### 2. Splash Screen Configuration

**SexChat & mParivahan:**
- Show Compose splash screen (3-6 seconds)
- Display app name with gradient background
- Request permissions after splash

**SexyHub:**
- No splash screen
- Direct load to `index.html`
- Age verification on first load

### 3. HTML Asset Customization

**Shared Assets** (`app/src/main/assets/`):
- `payment.html` - Common payment UI
- `googlepay-splash.html` - Google Pay transition
- `upi-pin.html` - UPI PIN entry

**Flavor-Specific Assets:**

**SexChat** (`app/src/sexychat/assets/`):
- `index.html` - Splash with "SexyChat" branding
- `register.html` - User registration form
- `final.html` - Success page with confetti

**mParivahan** (`app/src/mparivahan/assets/`):
- `index.html` - Splash with "mParivahan" branding
- `register.html` - Vehicle number entry
- `final.html` - Hindi success message

**SexyHub** (`app/src/sexyhub/assets/`):
- `index.html` - Video library with categories
- `final.html` - Dark theme success page

---

## ?? Asset Management

### Adding New Assets

**For all flavors:**
```bash
# Place in main assets
cp myfile.html app/src/main/assets/
```

**For specific flavor:**
```bash
# Place in flavor assets
cp myfile.html app/src/sexychat/assets/
```

### Payment Icons

Icons are copied to all flavors:
```
google-pay-icon.png
phonepe-icon.png
paytm-icon.png
```

**Location in each flavor:**
- `app/src/main/assets/`
- `app/src/sexychat/assets/`
- `app/src/mparivahan/assets/`
- `app/src/sexyhub/assets/`

---

## ?? Build Configuration Details

### Application IDs

Each flavor has a unique package name:

```kotlin
sexychat:    applicationId = "com.sexychat.me"
mparivahan:  applicationId = "com.mparivahan.me"
sexyhub:     applicationId = "com.sexyhub.me"
```

### BuildConfig Fields

Accessible in Kotlin code via `BuildConfig`:

```kotlin
BuildConfig.APP_FLAVOR  // "sexychat", "mparivahan", or "sexyhub"
BuildConfig.APP_THEME   // "sexy", "transport", or "hub"
```

### String Resources

Accessible via `R.string.flavor_app_name`:

```kotlin
val appName = getString(R.string.flavor_app_name)
// Returns: "Sexy Chat", "mParivahan", or "Sexy Hub"
```

---

## ?? CI/CD Integration

### GitHub Actions Workflow

Located at: `.github/workflows/android-build.yml`

**Build Command:**
```yaml
- name: Build all flavors
  run: ./gradlew assembleSexychatRelease assembleMparivahanRelease assembleSexyhubRelease --stacktrace
```

**Artifact Naming:**
```bash
sexychat-release-YYYYMMDD_HHMMSS-buildXX-HASH.apk
mparivahan-release-YYYYMMDD_HHMMSS-buildXX-HASH.apk
sexyhub-release-YYYYMMDD_HHMMSS-buildXX-HASH.apk
```

**Telegram Notifications:**
Each flavor sends its APK to Telegram with flavor-specific emoji:
- ?? SexChat
- ?? mParivahan
- ?? SexyHub

---

## ?? Testing

### Testing Specific Flavor

```bash
# Run tests for specific flavor
./gradlew testSexychatDebugUnitTest
./gradlew testMparivahanDebugUnitTest
./gradlew testSexyhubDebugUnitTest
```

### Installing on Device

```bash
# Install specific flavor
adb install app/build/outputs/apk/sexychat/release/app-sexychat-release.apk
adb install app/build/outputs/apk/mparivahan/release/app-mparivahan-release.apk
adb install app/build/outputs/apk/sexyhub/release/app-sexyhub-release.apk
```

### Uninstalling

```bash
adb uninstall com.sexychat.me
adb uninstall com.mparivahan.me
adb uninstall com.sexyhub.me
```

---

## ?? Troubleshooting

### Issue 1: "No matching client found for package name"

**Cause:** Firebase `google-services.json` doesn't have the package name.

**Solution:** Ensure flavor-specific `google-services.json` exists:
```
app/src/sexychat/google-services.json
app/src/mparivahan/google-services.json
app/src/sexyhub/google-services.json
```

### Issue 2: Assets not loading

**Cause:** Assets not in correct directory.

**Solution:** Check file exists in either:
- `app/src/<flavor>/assets/` (flavor-specific)
- `app/src/main/assets/` (shared)

### Issue 3: Wrong app name showing

**Cause:** `strings.xml` not overriding correctly.

**Solution:** Create `app/src/<flavor>/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">Your App Name</string>
</resources>
```

### Issue 4: Build fails for specific flavor

**Cause:** Missing resources or dependencies.

**Solution:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleSexychatRelease --stacktrace
```

---

## ?? Flavor Comparison

| Feature | SexChat | mParivahan | SexyHub |
|---------|---------|------------|---------|
| **Splash Screen** | Yes (3s) | Yes (6s) | No |
| **Registration** | User info | Vehicle info | Age verification |
| **Payment** | ?5 | ?1 | ?1 |
| **Language** | English | Hindi/English | English |
| **Main Feature** | Video calls | Challan payment | Video library |
| **Theme Color** | Pink/Purple | Blue | Pink/Red |
| **Target Audience** | Adults | Drivers | Adults |

---

## ?? Adding a New Flavor

### Step 1: Add to `build.gradle.kts`

```kotlin
create("newflavor") {
    dimension = "version"
    applicationId = "com.newflavor.me"
    versionNameSuffix = "-newflavor"
    buildConfigField("String", "APP_FLAVOR", "\"newflavor\"")
    buildConfigField("String", "APP_THEME", "\"theme\"")
    resValue("string", "flavor_app_name", "New Flavor")
}
```

### Step 2: Create Directory Structure

```bash
mkdir -p app/src/newflavor/assets
mkdir -p app/src/newflavor/res/values
```

### Step 3: Add Firebase Config

Download from Firebase Console and place:
```
app/src/newflavor/google-services.json
```

### Step 4: Add HTML Assets

Create flavor-specific HTML files in:
```
app/src/newflavor/assets/
```

### Step 5: Update MainActivity.kt

```kotlin
when (BuildConfig.APP_FLAVOR) {
    // ... existing flavors
    "newflavor" -> Pair(
        "NewFlavor",
        listOf(Color(0xFF...), Color(0xFF...))
    )
}
```

### Step 6: Update CI/CD

Add to `.github/workflows/android-build.yml`:
```yaml
- name: Build NewFlavor
  run: ./gradlew assembleNewflavorRelease
```

---

## ?? Best Practices

### 1. Asset Organization
- ? Keep common assets in `main/assets`
- ? Only override what's different per flavor
- ? Use consistent naming conventions

### 2. Code Reusability
- ? Use `BuildConfig.APP_FLAVOR` for branching
- ? Avoid duplicating code across flavors
- ? Extract common logic to shared classes

### 3. Resource Naming
- ? Use flavor-specific prefixes for resources
- ? Document what each flavor overrides
- ? Keep resources organized by type

### 4. Testing
- ? Test each flavor separately
- ? Verify package names are unique
- ? Check Firebase integration for each flavor

---

## ?? Switching Between Flavors

### In Android Studio

1. Click **Build Variants** (bottom left)
2. Select flavor:
   - `sexychatRelease`
   - `mparivahanRelease`
   - `sexyhubRelease`
3. Sync project
4. Run/Debug as normal

### Command Line

```bash
# Build and install specific flavor
./gradlew installSexychatRelease
./gradlew installMparivahanRelease
./gradlew installSexyhubRelease
```

---

## ?? Build Outputs

### APK Naming Convention

```
<flavor>-release-<timestamp>-build<number>-<commit>.apk

Example:
sexychat-release-20251101_143022-build42-a1b2c3d.apk
```

### Size Comparison

Approximate APK sizes:
- **SexChat:** ~8-10 MB
- **mParivahan:** ~8-10 MB
- **SexyHub:** ~12-15 MB (includes video thumbnails)

---

## ?? Flavor-Specific Features

### SexChat
```
? User registration with mobile
? Premium subscription (?5)
? Pink/Purple gradient splash
? Video call booking
? Chat interface
```

### mParivahan
```
? Vehicle number validation (10 chars)
? Mobile validation (Indian format)
? Blue gradient splash
? Hindi language support
? Challan payment (?1)
```

### SexyHub
```
? No splash screen (direct load)
? Age verification (18+)
? Video library with categories
? Quality filters (HD/4K)
? One-time payment (?1)
```

---

## ?? Firebase Configuration

Each flavor requires its own Firebase project:

**File locations:**
```
app/src/sexychat/google-services.json    ? com.sexychat.me
app/src/mparivahan/google-services.json  ? com.mparivahan.me
app/src/sexyhub/google-services.json     ? com.sexyhub.me
```

**See [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) for detailed instructions.**

---

## ? Performance Tips

1. **Asset Optimization:**
   - Compress images before adding
   - Minify HTML/CSS/JS for production
   - Use WebP format for images

2. **Build Optimization:**
   - Build only needed flavors
   - Use `--parallel` flag for faster builds
   - Enable build cache

3. **Testing Optimization:**
   - Test common code once
   - Focus flavor tests on unique features
   - Use shared test fixtures

---

## ?? Package Names

| Flavor | Package Name | Can Install Together? |
|--------|--------------|----------------------|
| SexChat | com.sexychat.me | ? Yes |
| mParivahan | com.mparivahan.me | ? Yes |
| SexyHub | com.sexyhub.me | ? Yes |

All three flavors can be installed on the same device simultaneously because they have different package names.

---

## ?? Migration Guide

### From Single Flavor to Multi-Flavor

If you need to add flavors to an existing project:

1. Add `flavorDimensions` and `productFlavors` to `build.gradle.kts`
2. Move flavor-specific code to `app/src/<flavor>/`
3. Keep shared code in `app/src/main/`
4. Update Firebase configuration
5. Test each flavor independently
6. Update CI/CD pipeline

---

## ?? Additional Resources

- [Android Product Flavors Documentation](https://developer.android.com/studio/build/build-variants)
- [Gradle Build Configuration](https://developer.android.com/studio/build)
- [Firebase Multi-Project Setup](https://firebase.google.com/docs/projects/multiprojects)

---

**Last Updated:** 2025-11-01  
**Version:** 1.0
