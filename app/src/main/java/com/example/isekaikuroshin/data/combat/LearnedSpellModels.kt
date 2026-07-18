package com.example.isekaikuroshin.data.combat

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * G�REV N - FAZ 1: �renilmi_ B�y� Veri Modeli
 *
 * Kullan1c1n1n B�y� St�dyosu'nda olu_turduu ve �rendii b�y�leri takip eder.
 * Bu model, b�y�n�n pratik seviyesi, kullan1m istatistikleri ve ki_iselle_tirilmi_
 * tetikleyici kalibrasyonlar1n1 i�erir.
 */
@Serializable
data class LearnedSpell(
    val id: String = UUID.randomUUID().toString(),

    // Referans: SpellRecipe ID
    val recipeId: String,

    // Kullan1c1n1n verdii �zel isim (�rn: "Ate_ Topu", "Zihin Okuma")
    val customName: String,

    // �renilme zaman1 (Unix timestamp)
    val learnedAt: Long = System.currentTimeMillis(),

    // Pratik seviyesi (0-10)
    // 0 = Yeni �renildi, 10 = Usta seviyesi
    val skillTreeLevel: Int = 0,

    // Pratik yapma say1s1
    val practiceCount: Int = 0,

    // Sava_ta kullan1m say1s1
    val combatUsageCount: Int = 0,

    // FAZ 7: XP Sistemi
    val currentXP: Int = 0,  // Mevcut XP (0-100 arası her seviye için)

    // Son kullan1m zaman1 (Unix timestamp)
    val lastUsed: Long = 0,

    // Element tipi (ate_, su, vb.)
    val element: ElementType = ElementType.NEUTRAL,

    // Her ad1m i�in ki_iselle_tirilmi_ tetikleyici kalibrasyonu
    // Key: SpellStep ID, Value: TriggerCalibration
    val personalTriggers: Map<String, TriggerCalibration> = emptyMap()
) {
    /**
     * Ortalama doruluk skorunu hesaplar (t�m tetikleyicilerin ortalamas1)
     */
    fun getAverageAccuracy(): Float {
        if (personalTriggers.isEmpty()) return 0f
        val totalAccuracy = personalTriggers.values.sumOf { it.accuracy.toDouble() }
        return (totalAccuracy / personalTriggers.size).toFloat()
    }

    /**
     * Skill tree seviyesine g�re pratik gerekliliini kontrol eder
     * Her 10 pratik = +1 seviye
     */
    fun canLevelUp(): Boolean {
        val requiredPractice = (skillTreeLevel + 1) * 10
        return practiceCount >= requiredPractice && skillTreeLevel < 10
    }

    /**
     * Pratik yap1ld11nda �ar1l1r
     */
    fun addPractice(): LearnedSpell {
        val newPracticeCount = practiceCount + 1
        val newLevel = if (canLevelUp()) skillTreeLevel + 1 else skillTreeLevel
        return copy(
            practiceCount = newPracticeCount,
            skillTreeLevel = newLevel,
            lastUsed = System.currentTimeMillis()
        )
    }

    /**
     * Sava_ta kullan1ld11nda �ar1l1r
     */
    fun usedInCombat(): LearnedSpell {
        return copy(
            combatUsageCount = combatUsageCount + 1,
            lastUsed = System.currentTimeMillis()
        )
    }
}

/**
 * Tetikleyici kalibrasyon verisi
 * Her b�y� ad1m1 i�in kullan1c1n1n �zelle_tirilmi_ tetikleyici verisi
 */
@Serializable
data class TriggerCalibration(
    // Hangi SpellStep'e ait
    val stepId: String,

    // Tetikleyici tipi
    val triggerType: TriggerType,

    // Kalibrasyon verisi (JSON format1nda)
    // Voice -> {"command": "ignis", "language": "tr"}
    // Gesture -> {"landmarks": [...], "gesture_name": "fire_hand"}
    val calibrationData: String,

    // Son pratikteki doruluk skoru (0.0 - 1.0)
    val accuracy: Float = 0f,

    // Son kalibrasyon zaman1 (Unix timestamp)
    val lastCalibrated: Long = System.currentTimeMillis()
) {
    /**
     * Kalibrasyonun g�ncel olup olmad11n1 kontrol eder
     * 30 g�nden eski kalibrasyonlar g�ncel deildir
     */
    fun isCalibrationFresh(): Boolean {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - lastCalibrated < thirtyDaysInMillis
    }

    /**
     * Doruluk seviyesini deerlendirir
     */
    fun getAccuracyLevel(): AccuracyLevel {
        return when {
            accuracy >= 0.9f -> AccuracyLevel.EXCELLENT
            accuracy >= 0.7f -> AccuracyLevel.GOOD
            accuracy >= 0.5f -> AccuracyLevel.FAIR
            accuracy >= 0.3f -> AccuracyLevel.POOR
            else -> AccuracyLevel.VERY_POOR
        }
    }
}

/**
 * Tetikleyici tipleri
 */
@Serializable
enum class TriggerType {
    VOICE,                  // Sesli komut
    SINGLE_HAND_GESTURE,   // Tek el hareketi
    DOUBLE_HAND_GESTURE,   // �ift el hareketi
    TIMER                  // Zaman tabanl1 (otomatik)
}

/**
 * Element tipleri
 */
@Serializable
enum class ElementType {
    NEUTRAL,    // N�tr
    FIRE,       // Ate_
    WATER,      // Su
    EARTH,      // Toprak
    AIR,        // Hava
    LIGHTNING,  // ^im_ek
    ICE,        // Buz
    LIGHT,      // I_1k
    DARK        // Karanl1k
}

/**
 * Doruluk seviyeleri
 */
enum class AccuracyLevel {
    EXCELLENT,   // M�kemmel (90%+)
    GOOD,        // 0yi (70-89%)
    FAIR,        // Orta (50-69%)
    POOR,        // Zay1f (30-49%)
    VERY_POOR    // �ok zay1f (<30%)
}

/**
 * FAZ 7: LearnedSpell Extension Functions (XP Sistemi)
 */
fun LearnedSpell.getXPToNextLevel(): Int = if (skillTreeLevel >= 10) 0 else (100 - currentXP)
fun LearnedSpell.getLevelProgress(): Float = if (skillTreeLevel >= 10) 1.0f else (currentXP / 100f)

fun LearnedSpell.addPracticeWithXP(xpGained: Int = 10): LearnedSpell {
    val newPracticeCount = practiceCount + 1
    val newXP = currentXP + xpGained

    return if (newXP >= 100 && skillTreeLevel < 10) {
        copy(
            practiceCount = newPracticeCount,
            currentXP = newXP - 100,
            skillTreeLevel = skillTreeLevel + 1,
            lastUsed = System.currentTimeMillis()
        )
    } else {
        copy(
            practiceCount = newPracticeCount,
            currentXP = if (skillTreeLevel >= 10) currentXP else newXP,
            lastUsed = System.currentTimeMillis()
        )
    }
}
