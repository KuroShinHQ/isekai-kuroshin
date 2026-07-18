# ═══════════════════════════════════════════════════════════════════════════
# G60b: ProGuard/R8 Optimization Rules for Isekai Kuroshin
# Target: <300MB RAM, 60 FPS, <45°C thermal on Samsung A34s
# ═══════════════════════════════════════════════════════════════════════════

# ───────────────────────────────────────────────────────────────────────────
# 🔧 GENERAL OPTIMIZATION
# ───────────────────────────────────────────────────────────────────────────

# Enable aggressive optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# Keep line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove logging in release builds (reduces APK size ~5-10%)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ───────────────────────────────────────────────────────────────────────────
# 📦 KOTLIN & COROUTINES
# ───────────────────────────────────────────────────────────────────────────

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ───────────────────────────────────────────────────────────────────────────
# 🎨 JETPACK COMPOSE
# ───────────────────────────────────────────────────────────────────────────

# Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keepclassmembers class androidx.compose.** {
    @androidx.compose.runtime.Composable *;
}

# Compose compiler
-dontwarn androidx.compose.compiler.**

# ───────────────────────────────────────────────────────────────────────────
# 💉 HILT / DAGGER
# ───────────────────────────────────────────────────────────────────────────

-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter
-keep class * extends dagger.internal.StaticInjection

-keepclasseswithmembers class * {
    @dagger.* <methods>;
}

-keep class **$$Factory { *; }
-keep class **$$MembersInjector { *; }

# Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltComponents { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# ───────────────────────────────────────────────────────────────────────────
# 🗄️ ROOM DATABASE
# ───────────────────────────────────────────────────────────────────────────

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract <methods>;
}

# ───────────────────────────────────────────────────────────────────────────
# 📝 KOTLINX SERIALIZATION
# ───────────────────────────────────────────────────────────────────────────

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.isekaikuroshin.**$$serializer { *; }
-keepclassmembers class com.example.isekaikuroshin.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.isekaikuroshin.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable classes
-keep @kotlinx.serialization.Serializable class * { *; }

# ───────────────────────────────────────────────────────────────────────────
# 🤖 ML KIT & MEDIAPIPE
# ───────────────────────────────────────────────────────────────────────────

# MediaPipe
-keep class com.google.mediapipe.** { *; }
-keepclassmembers class com.google.mediapipe.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

# ───────────────────────────────────────────────────────────────────────────
# 🔥 FIREBASE
# ───────────────────────────────────────────────────────────────────────────

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore models
-keep class com.example.isekaikuroshin.data.** { *; }
-keepclassmembers class com.example.isekaikuroshin.data.** { *; }

# ───────────────────────────────────────────────────────────────────────────
# 🎵 EXOPLAYER / MEDIA3
# ───────────────────────────────────────────────────────────────────────────

-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn androidx.media3.**

# ───────────────────────────────────────────────────────────────────────────
# 🔐 ENCRYPTED SHARED PREFERENCES
# ───────────────────────────────────────────────────────────────────────────

-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** { *; }

# ───────────────────────────────────────────────────────────────────────────
# 🎮 GAME-SPECIFIC OPTIMIZATIONS
# ───────────────────────────────────────────────────────────────────────────

# Keep all ViewModels (needed for Hilt injection)
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Composable functions (prevent crashes from R8)
-keep @androidx.compose.runtime.Composable class * { *; }

# Keep data classes with @Serializable
-keep class com.example.isekaikuroshin.data.** { *; }
-keepclassmembers class com.example.isekaikuroshin.data.** {
    <fields>;
    <methods>;
}

# Keep Engine classes (critical for game logic)
-keep class com.example.isekaikuroshin.engine.** { *; }
-keepclassmembers class com.example.isekaikuroshin.engine.** { *; }

# Keep sound manager
-keep class com.example.isekaikuroshin.sound.** { *; }

# ───────────────────────────────────────────────────────────────────────────
# ⚡ PERFORMANCE-CRITICAL CLASSES (G60)
# ───────────────────────────────────────────────────────────────────────────

# Keep MemoryMonitor (G58)
-keep class com.example.isekaikuroshin.utils.MemoryMonitor { *; }
-keepclassmembers class com.example.isekaikuroshin.utils.MemoryMonitor { *; }

# Keep PerformanceManager (G59)
-keep class com.example.isekaikuroshin.utils.PerformanceManager { *; }
-keepclassmembers class com.example.isekaikuroshin.utils.PerformanceManager { *; }

# Keep PersistentDataManager (core state management)
-keep class com.example.isekaikuroshin.data.PersistentDataManager { *; }
-keepclassmembers class com.example.isekaikuroshin.data.PersistentDataManager { *; }

# Keep GameStateManager
-keep class com.example.isekaikuroshin.game.GameStateManager { *; }

# ───────────────────────────────────────────────────────────────────────────
# 🚫 SUPPRESS WARNINGS
# ───────────────────────────────────────────────────────────────────────────

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ═══════════════════════════════════════════════════════════════════════════
# END OF PROGUARD RULES
# Expected APK size reduction: ~30-40%
# Expected memory reduction: ~15-20%
# ═══════════════════════════════════════════════════════════════════════════
