package com.example.isekaikuroshin.ui.exercise

import kotlin.math.roundToInt

/**
 * Stat Tipleri (RPG karakter özellikleri)
 */
enum class StatType {
    STRENGTH,       // Güç
    ENDURANCE,      // Dayanıklılık
    AGILITY,        // Çeviklik
    VITALITY,       // Canlılık
    INTELLIGENCE    // Zeka
}

/**
 * Egzersiz Tipleri
 */
enum class ExerciseType(val displayName: String) {
    PUSH_UP("Şınav"),
    SQUAT("Squat"),
    SIT_UP("Mekik"),
    PULL_UP("Barfiks"),
    PLANK("Plank")
}

/**
 * Egzersiz Stat Ödülü Konfigürasyonu
 */
data class ExerciseStatReward(
    val exerciseType: ExerciseType,
    val statsPerRep: Map<StatType, Float>,
    val statsPerSet: Map<StatType, Float>,
    val caloriesPerRep: Float
)

/**
 * Egzersiz Ödülü (Hesaplanan sonuç)
 */
data class WorkoutReward(
    val stats: Map<StatType, Float>,
    val experience: Int,
    val calories: Float,
    val gold: Int = 0,
    val formQualityAverage: Float = 0f,
    val xpMultiplier: Float = 1.0f  // YENİ: Variable Reward multiplier (1.0x, 1.5x, 2.0x, 3.0x)
)

/**
 * Oyunlaştırma Motoru
 *
 * Egzersiz verilerini alır, RPG stat artışlarını ve XP kazanımlarını hesaplar.
 */
class GamificationEngine {

    companion object {
        /**
         * Egzersiz başına ödül konfigürasyonları
         */
        private val exerciseRewards = mapOf(
            ExerciseType.PUSH_UP to ExerciseStatReward(
                exerciseType = ExerciseType.PUSH_UP,
                statsPerRep = mapOf(
                    StatType.STRENGTH to 0.15f,      // Her şınav +0.15 Güç
                    StatType.ENDURANCE to 0.08f      // Her şınav +0.08 Dayanıklılık
                ),
                statsPerSet = mapOf(
                    StatType.VITALITY to 1.0f        // Her set +1 Canlılık
                ),
                caloriesPerRep = 0.35f               // ~0.35 kcal/tekrar
            ),

            ExerciseType.SQUAT to ExerciseStatReward(
                exerciseType = ExerciseType.SQUAT,
                statsPerRep = mapOf(
                    StatType.STRENGTH to 0.12f,      // Bacak gücü
                    StatType.ENDURANCE to 0.12f,     // Kardiyovasküler dayanıklılık
                    StatType.AGILITY to 0.05f        // Denge ve çeviklik
                ),
                statsPerSet = mapOf(
                    StatType.VITALITY to 1.5f
                ),
                caloriesPerRep = 0.40f
            ),

            ExerciseType.SIT_UP to ExerciseStatReward(
                exerciseType = ExerciseType.SIT_UP,
                statsPerRep = mapOf(
                    StatType.STRENGTH to 0.10f,      // Core gücü
                    StatType.ENDURANCE to 0.10f
                ),
                statsPerSet = mapOf(
                    StatType.VITALITY to 0.8f
                ),
                caloriesPerRep = 0.30f
            ),

            ExerciseType.PULL_UP to ExerciseStatReward(
                exerciseType = ExerciseType.PULL_UP,
                statsPerRep = mapOf(
                    StatType.STRENGTH to 0.25f,      // En zor egzersiz, en yüksek ödül
                    StatType.ENDURANCE to 0.15f,
                    StatType.AGILITY to 0.05f
                ),
                statsPerSet = mapOf(
                    StatType.VITALITY to 2.0f
                ),
                caloriesPerRep = 0.80f
            ),

            ExerciseType.PLANK to ExerciseStatReward(
                exerciseType = ExerciseType.PLANK,
                statsPerRep = mapOf(
                    StatType.STRENGTH to 0.05f,      // Saniye başına
                    StatType.ENDURANCE to 0.15f      // Dayanıklılık odaklı
                ),
                statsPerSet = mapOf(
                    StatType.VITALITY to 1.2f
                ),
                caloriesPerRep = 0.10f  // Saniye başına
            )
        )
    }

    /**
     * Egzersiz ödülünü hesaplar
     *
     * @param exerciseType Egzersiz tipi
     * @param repCount Tekrar sayısı
     * @param duration Süre (milliseconds)
     * @param formQualityAvg Ortalama form kalitesi (0.0 - 1.0)
     * @return Hesaplanan ödül
     */
    fun calculateReward(
        exerciseType: ExerciseType,
        repCount: Int,
        duration: Long = 0L,
        formQualityAvg: Float = 0.8f
    ): WorkoutReward {
        val rewardConfig = exerciseRewards[exerciseType]
            ?: throw IllegalArgumentException("Unknown exercise type: $exerciseType")

        // Temel stat kazanımları
        val baseStats = mutableMapOf<StatType, Float>()

        // Tekrar başına stat'lar
        rewardConfig.statsPerRep.forEach { (stat, value) ->
            baseStats[stat] = value * repCount
        }

        // Set başına stat'lar (her egzersiz 1 set sayılır)
        rewardConfig.statsPerSet.forEach { (stat, value) ->
            baseStats[stat] = (baseStats[stat] ?: 0f) + value
        }

        // Form kalitesi bonusu (0.8x - 1.2x çarpan)
        val formBonus = 0.8f + (formQualityAvg * 0.4f)

        // Final stat'lar (form bonusu ile)
        val finalStats = baseStats.mapValues { (_, value) ->
            (value * formBonus * 100f).roundToInt() / 100f  // 2 ondalık basamak
        }

        // XP hesaplama
        val baseXP = when (exerciseType) {
            ExerciseType.PUSH_UP -> 2
            ExerciseType.SQUAT -> 2
            ExerciseType.SIT_UP -> 1
            ExerciseType.PULL_UP -> 5
            ExerciseType.PLANK -> 1
        }

        // ═══ VARIABLE REWARD SYSTEM (Dopamine Loop) ═══
        // Rastgele XP çarpanı - Slot machine gibi rastgelelik kullanıcı motivasyonunu artırır
        // Kaynak: "Variable Ratio Reinforcement Schedule" (Behavioral Psychology)
        val xpMultiplier = determineXpMultiplier()

        val totalXP = (baseXP * repCount * formBonus * xpMultiplier).toInt()

        // Kalori hesaplama
        val calories = (rewardConfig.caloriesPerRep * repCount * 100f).roundToInt() / 100f

        // Altın (currency) hesaplama
        val gold = (repCount / 5) + if (formQualityAvg > 0.9f) 5 else 0

        return WorkoutReward(
            stats = finalStats,
            experience = totalXP,
            calories = calories,
            gold = gold,
            formQualityAverage = formQualityAvg,
            xpMultiplier = xpMultiplier  // Slot machine çarpanı
        )
    }

    /**
     * XP Çarpanı Belirleme (Variable Reward System)
     *
     * Slot machine mantığıyla rastgele çarpan:
     * - 70% olasılık: 1.0x (Normal)
     * - 20% olasılık: 1.5x (Bonus!)
     * - 7% olasılık: 2.0x (Mega Bonus!!)
     * - 3% olasılık: 3.0x (JACKPOT!!!)
     *
     * KAYNAK: "Variable Ratio Reinforcement Schedule" (B.F. Skinner)
     * Bu sistem slot machine ve loot box sistemlerinde kullanılır.
     * Düşük olasılıklı büyük ödüller dopamin salınımını maksimize eder.
     */
    private fun determineXpMultiplier(): Float {
        val random = kotlin.random.Random.nextFloat()  // 0.0 - 1.0 arası

        return when {
            random < 0.03f -> {
                // 3% - JACKPOT!
                android.util.Log.d("GamificationEngine", "🎰 JACKPOT!!! 3.0x XP çarpanı!")
                3.0f
            }
            random < 0.10f -> {
                // 7% (0.03 + 0.07) - Mega Bonus
                android.util.Log.d("GamificationEngine", "🎉 Mega Bonus! 2.0x XP çarpanı!")
                2.0f
            }
            random < 0.30f -> {
                // 20% (0.10 + 0.20) - Bonus
                android.util.Log.d("GamificationEngine", "⭐ Bonus! 1.5x XP çarpanı!")
                1.5f
            }
            else -> {
                // 70% - Normal
                1.0f
            }
        }
    }

    /**
     * Form kalitesi ortalamasını hesaplar
     */
    fun calculateAverageFormQuality(formQualityHistory: List<FormQuality>): Float {
        if (formQualityHistory.isEmpty()) return 0.8f

        val qualityValues = formQualityHistory.map { quality ->
            when (quality) {
                FormQuality.EXCELLENT -> 1.0f
                FormQuality.GOOD -> 0.85f
                FormQuality.NEUTRAL -> 0.7f
                FormQuality.POOR -> 0.5f
                FormQuality.INVALID -> 0.3f
            }
        }

        return qualityValues.average().toFloat()
    }

    /**
     * Stat artışı mesajını formatlar
     */
    fun formatStatIncrease(stat: StatType, value: Float): String {
        val sign = if (value >= 0) "+" else ""
        return when (stat) {
            StatType.STRENGTH -> "💪 Güç $sign$value"
            StatType.ENDURANCE -> "🏃 Dayanıklılık $sign$value"
            StatType.AGILITY -> "⚡ Çeviklik $sign$value"
            StatType.VITALITY -> "❤️ Canlılık $sign$value"
            StatType.INTELLIGENCE -> "🧠 Zeka $sign$value"
        }
    }
}
