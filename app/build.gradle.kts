
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dagger.hilt)  // ✅ RE-ENABLED with KSP
    id("com.google.devtools.ksp") version "1.9.23-1.0.20"  // ✅ KSP instead of KAPT
    kotlin("plugin.serialization")
    id("com.google.gms.google-services") // GÖREV O FAZ 1: Firebase ENABLED
}

android {
    namespace = "com.example.isekaikuroshin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.isekaikuroshin"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // MediaPipe native library için gerekli
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // Disable APK splits to ensure native libraries are included
        splits.abi.isEnable = false
    }

    buildTypes {
        release {
            // G60a: Enable Proguard/R8 for code shrinking and obfuscation
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf(
                "**/*.so"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)  // ✅ KSP for Hilt
    implementation(libs.androidx.hilt.navigation.compose)

    // Media
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)  // ✅ KSP for Room

    // Security
    implementation(libs.androidx.security.crypto)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Startup Library - RACE CONDITION FIX
    implementation("androidx.startup:startup-runtime:1.1.1")

    // Health Connect
    implementation(libs.androidx.health.connect)

    // Compose Charts
    implementation(libs.compose.charts)

    // G89: Quarks kütüphanesi kaldırıldı - Basit parçacık sistemi kullanıyoruz
    // implementation("me.nikhilchaudhari:quarks:1.0.0-alpha02")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ExoPlayer
    implementation(libs.exoplayer)

    // ML Kit (MediaPipe for local AI)
    implementation(libs.mediapipe.tasks.genai)

    // Gemini Vision SDK - Health Hub Blood Analysis
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")

    // Lottie-Compose - Health Hub Animations
    implementation("com.airbnb.android:lottie-compose:6.0.0")

    // Coil - Image Loading for Health Hub
    implementation("io.coil-kt:coil-compose:2.4.0")

    // ML Kit Pose Detection
    implementation("com.google.mlkit:pose-detection:18.0.0-beta4")

    // MediaPipe Hands (Kadim Mühür Teknikleri için)
    // Using 0.10.29 - has additional architecture support and fixes JNI library loading issues
    implementation("com.google.mediapipe:tasks-vision:0.10.29")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // Accompanist Permissions (kamera izni için)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // HTTP Client (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for TypeConverters
    implementation("com.google.code.gson:gson:2.10.1")

    // TODO-D3: Retrofit for Weather API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // TODO-D3: Google Play Services Location for GPS
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // PDFBox for PDF processing
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // GÖREV O FAZ 1: Firebase - Google Play Billing + Community Features
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // Updated to latest
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // GÖREV O FAZ 3: Google Play Billing Library v7+
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// KSP configuration for better generated code location
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
