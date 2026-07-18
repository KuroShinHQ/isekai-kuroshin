package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * Kadim Mühür (Ancient Seal) - Hand gesture mudra
 *
 * Bu sınıf, oyuncuların kamera ile öğrenip uyguladığı el mudraları temsil eder.
 * Her mühür, MediaPipe Hands ile tespit edilen 21-point hand landmark template'i ile eşleşir.
 */
@Serializable
data class Seal(
    val id: String,
    // G87 FIX: KURAL 9 - Hardcoded text yerine translation keys
    val nameKey: String,        // Translation key (e.g., "seal_fire_blast")
    val descriptionKey: String, // Translation key (e.g., "seal_fire_blast_desc")
    val loreTextKey: String,    // Translation key (e.g., "seal_fire_blast_lore")

    // Difficulty
    val difficulty: SealDifficulty,
    val tier: String,  // "F", "E", "D", "C", "B", "A", "S", "SS"

    // Template matching data (Multi-angle support)
    val templateLandmarksList: MutableList<List<NormalizedPoint>>,  // Birden fazla açı şablonu
    val acceptanceThreshold: Float = 0.80f,  // Kabul eşiği (varsayılan %80)
    val toleranceThreshold: Float = 0.15f,  // %15 sapma kabul edilebilir (deprecated, acceptanceThreshold kullan)

    // Requirements
    val prerequisiteSealId: String? = null,  // Bu mührü master et önce
    val minPlayerLevel: Int = 1,

    // Related skills
    val relatedSkillIds: List<String> = emptyList(),  // Bu mührü master edince açılan skill'ler

    // Mastery progress
    val masteryLevel: Int = 0,  // 0-100
    val practiceMetrics: PracticeMetrics = PracticeMetrics(),

    // Visual
    val iconPath: String? = null,
    val visualGuideAnimation: String? = null  // Lottie animation dosyası
)

/**
 * Seal Difficulty Levels
 *
 * Progressive difficulty sistemi - oyuncu basit mühürlerle başlayıp
 * yavaş yavaş karmaşık mudralara ilerler.
 */
enum class SealDifficulty {
    NOVICE,        // Basit el şekilleri (2-3 parmak, açık/kapalı el)
    INTERMEDIATE,  // Orta karmaşıklık (5+ landmark, basit açılar)
    ADVANCED,      // Karmaşık şekiller (ince açılar, parmak kombinasyonları)
    MASTER,        // Çok karmaşık (çift el kombinasyonları)
    LEGENDARY      // Extreme (dinamik hareketler - future feature)
}

/**
 * Practice Metrics
 *
 * Oyuncunun bir mührü pratik etme istatistikleri.
 * GameStateManager tarafından güncellenir.
 */
@Serializable
data class PracticeMetrics(
    val totalAttempts: Int = 0,
    val successfulExecutions: Int = 0,
    val perfectExecutions: Int = 0,  // >= 95% accuracy
    val failedAttempts: Int = 0,
    val lastPracticeTimestamp: Long = 0,
    val practiceStreak: Int = 0,  // Arka arkaya başarılı gün sayısı
    val bestAccuracy: Float = 0f,  // En iyi skor (0.0 - 1.0)
    val avgAccuracy: Float = 0f    // Ortalama skor (0.0 - 1.0)
)

/**
 * Normalized 3D Point
 *
 * MediaPipe Hands landmark'ları için normalize edilmiş koordinatlar.
 * El boyutundan bağımsız (hand-size independent) karşılaştırma için kullanılır.
 */
@Serializable
data class NormalizedPoint(
    val x: Float,
    val y: Float,
    val z: Float  // Derinlik bilgisi
)

/**
 * Extension: Calculate success rate
 */
fun PracticeMetrics.getSuccessRate(): Float {
    if (totalAttempts == 0) return 0f
    return successfulExecutions.toFloat() / totalAttempts.toFloat()
}

/**
 * Extension: Calculate perfect rate
 */
fun PracticeMetrics.getPerfectRate(): Float {
    if (totalAttempts == 0) return 0f
    return perfectExecutions.toFloat() / totalAttempts.toFloat()
}

/**
 * Extension: Check if player is mastering this seal
 */
fun Seal.isMastered(): Boolean {
    return masteryLevel >= 100
}

/**
 * Extension: Get mastery progress percentage
 */
fun Seal.getMasteryProgress(): Float {
    return masteryLevel.toFloat() / 100f
}

/**
 * Extension: Check if seal is unlocked for player
 */
fun Seal.isUnlockedFor(playerLevel: Int, masteredSealIds: List<String>): Boolean {
    // Level requirement check
    if (playerLevel < minPlayerLevel) return false

    // Prerequisite check
    prerequisiteSealId?.let { prereqId ->
        if (!masteredSealIds.contains(prereqId)) return false
    }

    return true
}

/**
 * Extension: Get calibration count (number of registered angles)
 */
fun Seal.getCalibrationCount(): Int {
    return templateLandmarksList.size
}

/**
 * Extension: Check if seal has any calibrations
 */
fun Seal.hasCalibrations(): Boolean {
    return templateLandmarksList.isNotEmpty()
}

/**
 * Extension: Add new calibration angle
 */
fun Seal.addCalibration(landmarks: List<NormalizedPoint>): Seal {
    val updatedList = templateLandmarksList.toMutableList()
    updatedList.add(landmarks)
    return this.copy(templateLandmarksList = updatedList)
}

/**
 * Extension: Clear all calibrations (reset to empty)
 */
fun Seal.clearCalibrations(): Seal {
    return this.copy(templateLandmarksList = mutableListOf())
}

/**
 * Extension: Update acceptance threshold
 */
fun Seal.updateAcceptanceThreshold(newThreshold: Float): Seal {
    return this.copy(acceptanceThreshold = newThreshold.coerceIn(0.5f, 0.99f))
}
