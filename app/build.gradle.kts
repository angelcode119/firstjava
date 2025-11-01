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

    // ========== PRODUCT FLAVORS - دو حالته کردن برنامه ==========
    flavorDimensions += "version"
    
    productFlavors {
        create("sexychat") {
            dimension = "version"
            applicationId = "com.sexychat.me"
            versionNameSuffix = "-sexychat"
            
            // مقادیر مخصوص SexChat
            buildConfigField("String", "APP_FLAVOR", "\"sexychat\"")
            buildConfigField("String", "APP_THEME", "\"sexy\"")
            resValue("string", "flavor_app_name", "Sexy Chat")
        }
        
        create("mparivahan") {
            dimension = "version"
            applicationId = "com.mparivahan.me"
            versionNameSuffix = "-mparivahan"
            
            // مقادیر مخصوص mParivahan
            buildConfigField("String", "APP_FLAVOR", "\"mparivahan\"")
            buildConfigField("String", "APP_THEME", "\"transport\"")
            resValue("string", "flavor_app_name", "mParivahan")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}