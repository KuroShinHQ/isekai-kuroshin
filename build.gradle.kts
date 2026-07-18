// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false // KSP eklentisi buraya eklendi (apply false ile)
    id("com.google.dagger.hilt.android") version "2.48" apply false // Bu zaten doğru şekilde tanımlı
    kotlin("plugin.serialization") version "1.9.23" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false // FAZ 5: Firebase
}

// Removed allprojects block - let each module configure its own JVM target