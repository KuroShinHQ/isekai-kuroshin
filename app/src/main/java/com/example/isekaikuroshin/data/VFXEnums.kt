package com.example.isekaikuroshin.data

// VFX Atölyesi için gerekli enum'lar

enum class VFXLevel {
    SPRITE,      // Seviye 1: 2D Sprite Animasyonu
    PARTICLE,    // Seviye 2: Parçacık Efekti
    METABALL     // Seviye 3: 2D Sıvı Simülasyonu (Deneysel)
}

enum class VFXShape {
    CIRCLE,      // Daire
    TRIANGLE,    // Üçgen
    RECTANGLE,   // Dikdörtgen
    STAR         // Yıldız
}

enum class GestureType {
    CREATION,    // 1. Hareket: Yaratma (Açık El)
    DIRECTION    // 2. Hareket: Yönlendirme (Barış İşareti)
}

/**
 * ⚡⚡⚡ BÖLÜM D: VFX Efekt Türleri
 * Farklı görsel efekt stilleri
 */
enum class VFXEffectType {
    STANDARD,    // Standart efekt (mevcut - şekil + level kombinasyonu)
    SMOKE,       // Duman efekti - yukarı doğru yükselen, dağılan parçacıklar
    FIRE,        // Ateş efekti - titreşimli, sıcak renkli alevler
    EXPLOSION,   // Patlama efekti - dışa doğru genişleyen yıldız şekli
    TRAIL,       // İz efekti - hareket eden nesnenin arkasında iz bırakma
    GLOW,        // Parıltı efekti - parlayan aura, bloom effect
    CONFETTI,    // Konfeti efekti - renkli dönen parçacıklar
    DISSOLVE,    // Erime efekti - pikselleşerek kaybolma
    ENERGY       // Enerji efekti - elektrik çarpması benzeri
}

/**
 * 3D Vektör sınıfı (VFX konumlandırma için)
 * MediaPipe'tan gelen normalize edilmiş 3D koordinatlar için
 */
data class Vector3f(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3f) = Vector3f(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3f) = Vector3f(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3f(x * scalar, y * scalar, z * scalar)

    fun magnitude(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
}

/**
 * VFX Offset Konfigürasyonu
 * Bir jestin referans noktasına göre kayıtlı 3D offset vektörü
 */
data class VFXOffsetConfig(
    val gestureType: GestureType,
    val offset3D: Vector3f = Vector3f(),
    val referencePointLandmarkIndex: Int = 9, // Varsayılan: Avuç merkezi (landmark 9)
    val timestamp: Long = System.currentTimeMillis()
)