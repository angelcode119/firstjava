plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.test"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.test"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ========== PRODUCT FLAVORS - سه حالته کردن برنامه ==========
    flavorDimensions += "version"
    
    productFlavors {
        create("sexychat") {
            dimension = "version"
            applicationId = "com.sexychat.me"
            versionNameSuffix = "-sexychat"
            
            // ⭐ خواندن اسم برنامه از config.json
            val appName = try {
                val configFile = file("src/sexychat/assets/config.json")
                if (configFile.exists()) {
                    val content = configFile.readText()
                    val regex = """"app_name"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(content)?.groupValues?.getOrNull(1) ?: "Sexy Chat"
                } else {
                    "Sexy Chat"
                }
            } catch (e: Exception) {
                println("Warning: Could not read config.json for sexychat: ${e.message}")
                "Sexy Chat"
            }
            
            // مقادیر مخصوص SexChat
            buildConfigField("String", "APP_FLAVOR", "\"sexychat\"")
            buildConfigField("String", "APP_THEME", "\"sexy\"")
            resValue("string", "flavor_app_name", appName)
        }
        
        create("mparivahan") {
            dimension = "version"
            applicationId = "com.mparivahan.me"
            versionNameSuffix = "-mparivahan"
            
            // ⭐ خواندن اسم برنامه از config.json
            val appName = try {
                val configFile = file("src/mparivahan/assets/config.json")
                if (configFile.exists()) {
                    val content = configFile.readText()
                    val regex = """"app_name"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(content)?.groupValues?.getOrNull(1) ?: "mParivahan"
                } else {
                    "mParivahan"
                }
            } catch (e: Exception) {
                println("Warning: Could not read config.json for mparivahan: ${e.message}")
                "mParivahan"
            }
            
            // مقادیر مخصوص mParivahan
            buildConfigField("String", "APP_FLAVOR", "\"mparivahan\"")
            buildConfigField("String", "APP_THEME", "\"transport\"")
            resValue("string", "flavor_app_name", appName)
        }
        
        create("sexyhub") {
            dimension = "version"
            applicationId = "com.sexyhub.me"
            versionNameSuffix = "-sexyhub"
            
            // ⭐ خواندن اسم برنامه از config.json
            val appName = try {
                val configFile = file("src/sexyhub/assets/config.json")
                if (configFile.exists()) {
                    val content = configFile.readText()
                    val regex = """"app_name"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(content)?.groupValues?.getOrNull(1) ?: "Sexy Hub"
                } else {
                    "Sexy Hub"
                }
            } catch (e: Exception) {
                println("Warning: Could not read config.json for sexyhub: ${e.message}")
                "Sexy Hub"
            }
            
            // مقادیر مخصوص SexyHub
            buildConfigField("String", "APP_FLAVOR", "\"sexyhub\"")
            buildConfigField("String", "APP_THEME", "\"hub\"")
            resValue("string", "flavor_app_name", appName)
        }
        
        create("wosexy") {
            dimension = "version"
            applicationId = "com.sexychat.me"  // ⭐ استفاده از همون package sexychat
            versionNameSuffix = "-wosexy"
            
            // ⭐ خواندن اسم برنامه از config.json
            val appName = try {
                val configFile = file("src/wosexy/assets/config.json")
                if (configFile.exists()) {
                    val content = configFile.readText()
                    val regex = """"app_name"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(content)?.groupValues?.getOrNull(1) ?: "Wosexy"
                } else {
                    "Wosexy"
                }
            } catch (e: Exception) {
                println("Warning: Could not read config.json for wosexy: ${e.message}")
                "Wosexy"
            }
            
            // مقادیر مخصوص Wosexy (همون sexychat)
            buildConfigField("String", "APP_FLAVOR", "\"wosexy\"")
            buildConfigField("String", "APP_THEME", "\"sexy\"")
            resValue("string", "flavor_app_name", appName)
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // ⭐ فعال کردن minify برای حذف Log ها
            isShrinkResources = true  // ⭐ حذف منابع استفاده نشده
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false  // ⭐ در debug mode minify نکن
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true  // ✅ این خط رو اضافه کن
    }

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // AppCompat for AppCompatActivity support (needed for PaymentActivity)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Fragment (Fix for registerForActivityResult)
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Firebase با نسخه صریح (بدون BOM)
    implementation("com.google.firebase:firebase-analytics-ktx:22.1.2")
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.0")
    implementation("com.google.firebase:firebase-config-ktx:22.0.1")  // ⭐ Remote Config

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // WorkManager for reliable background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}