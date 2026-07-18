package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.utils.GameLogger
import kotlin.random.Random

/**
 * Dice Roll System - Adds randomness and skill checks to story events
 */
object DiceSystem {

    enum class DiceType(val sides: Int, val emoji: String) {
        D4(4, "⚀"),
        D6(6, "🎲"),
        D8(8, "🎯"),
        D10(10, "🔢"),
        D12(12, "⭐"),
        D20(20, "🎰"),
        D100(100, "💯")
    }

    enum class Difficulty(val threshold: Int, val description: String) {
        VERY_EASY(5, "Çok Kolay"),
        EASY(8, "Kolay"),
        MEDIUM(12, "Orta"),
        HARD(15, "Zor"),
        VERY_HARD(18, "Çok Zor"),
        LEGENDARY(20, "Efsanevi")
    }

    /**
     * GÖREV N - FAZ 6: Advantage seviyeleri
     */
    enum class AdvantageLevel {
        DOUBLE_DISADVANTAGE,  // Roll 3, take lowest
        DISADVANTAGE,         // Roll 2, take lowest
        NORMAL,               // Roll 1
        ADVANTAGE,            // Roll 2, take highest
        DOUBLE_ADVANTAGE      // Roll 3, take highest
    }

    data class DiceRoll(
        val dice: DiceType,
        val count: Int,
        val modifier: Int,
        val results: List<Int>,
        val total: Int,
        val success: Boolean = false,
        val difficulty: Difficulty? = null,
        val advantageLevel: AdvantageLevel = AdvantageLevel.NORMAL  // FAZ 6
    )

    /**
     * Roll dice with specified parameters
     */
    fun rollDice(
        dice: DiceType = DiceType.D20,
        count: Int = 1,
        modifier: Int = 0
    ): DiceRoll {
        val results = (1..count).map { Random.nextInt(1, dice.sides + 1) }
        val total = results.sum() + modifier

        val roll = DiceRoll(
            dice = dice,
            count = count,
            modifier = modifier,
            results = results,
            total = total
        )

        GameLogger.logDiceSystem("Dice roll: ${count}${dice.name}${if (modifier != 0) "+$modifier" else ""} = $total ${dice.emoji}")
        return roll
    }

    /**
     * Skill check with difficulty
     * FAZ 6: AdvantageLevel desteği eklendi
     */
    fun skillCheck(
        skillModifier: Int,
        difficulty: Difficulty = Difficulty.MEDIUM,
        dice: DiceType = DiceType.D20,
        advantage: Boolean = false,
        disadvantage: Boolean = false,
        advantageLevel: AdvantageLevel = AdvantageLevel.NORMAL  // FAZ 6
    ): DiceRoll {
        // AdvantageLevel kullanılıyorsa onu kullan, yoksa eski API'yi destekle
        val effectiveAdvantageLevel = if (advantageLevel != AdvantageLevel.NORMAL) {
            advantageLevel
        } else {
            when {
                advantage -> AdvantageLevel.ADVANTAGE
                disadvantage -> AdvantageLevel.DISADVANTAGE
                else -> AdvantageLevel.NORMAL
            }
        }

        val rollCount = when (effectiveAdvantageLevel) {
            AdvantageLevel.DOUBLE_DISADVANTAGE, AdvantageLevel.DOUBLE_ADVANTAGE -> 3
            AdvantageLevel.DISADVANTAGE, AdvantageLevel.ADVANTAGE -> 2
            AdvantageLevel.NORMAL -> 1
        }

        val baseRoll = rollDice(dice, rollCount, skillModifier)
        val finalTotal = when (effectiveAdvantageLevel) {
            AdvantageLevel.DOUBLE_ADVANTAGE, AdvantageLevel.ADVANTAGE -> {
                baseRoll.results.maxOrNull()!! + skillModifier
            }
            AdvantageLevel.DOUBLE_DISADVANTAGE, AdvantageLevel.DISADVANTAGE -> {
                baseRoll.results.minOrNull()!! + skillModifier
            }
            AdvantageLevel.NORMAL -> baseRoll.total
        }

        val success = finalTotal >= difficulty.threshold

        val result = baseRoll.copy(
            total = finalTotal,
            success = success,
            difficulty = difficulty,
            advantageLevel = effectiveAdvantageLevel
        )

        val advantageText = when (effectiveAdvantageLevel) {
            AdvantageLevel.DOUBLE_ADVANTAGE -> " (Çift Avantaj 🌟)"
            AdvantageLevel.ADVANTAGE -> " (Avantaj)"
            AdvantageLevel.DOUBLE_DISADVANTAGE -> " (Çift Dezavantaj ⚠️)"
            AdvantageLevel.DISADVANTAGE -> " (Dezavantaj)"
            AdvantageLevel.NORMAL -> ""
        }

        GameLogger.logDiceSystem(
            "Skill check$advantageText: $finalTotal vs ${difficulty.threshold} (${difficulty.description}) = ${if (success) "BAŞARILI ✅" else "BAŞARISIZ ❌"}"
        )

        return result
    }

    /**
     * Combat roll
     */
    fun combatRoll(
        attackBonus: Int = 0,
        damageRoll: String = "1d6" // Format: "2d6+3"
    ): Pair<DiceRoll, DiceRoll> {
        // Attack roll (d20 + bonus)
        val attackRoll = rollDice(DiceType.D20, 1, attackBonus)

        // Damage roll (parse string like "2d6+3")
        val damageResult = parseDamageRoll(damageRoll)

        GameLogger.logDiceSystem("Combat: Attack=${attackRoll.total}, Damage=${damageResult.total}")
        return Pair(attackRoll, damageResult)
    }

    /**
     * Random event roll
     */
    fun randomEvent(): DiceRoll {
        val roll = rollDice(DiceType.D100, 1, 0)
        val eventType = when (roll.total) {
            in 1..10 -> "Çok Kötü Olay"
            in 11..25 -> "Kötü Olay"
            in 26..75 -> "Normal Olay"
            in 76..90 -> "İyi Olay"
            else -> "Çok İyi Olay"
        }

        GameLogger.logDiceSystem("Random event roll: ${roll.total}/100 - $eventType")
        return roll
    }

    /**
     * Luck check
     */
    fun luckCheck(luckStat: Int): DiceRoll {
        val difficulty = when {
            luckStat >= 15 -> Difficulty.EASY
            luckStat >= 10 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }

        return skillCheck(luckStat, difficulty)
    }

    /**
     * Parse damage roll string like "2d6+3"
     */
    private fun parseDamageRoll(rollString: String): DiceRoll {
        return try {
            val parts = rollString.lowercase().split("d")
            val count = parts[0].toIntOrNull() ?: 1

            val diceParts = parts[1].split("+", "-")
            val diceSize = diceParts[0].toIntOrNull() ?: 6
            val modifier = if (diceParts.size > 1) {
                val sign = if (rollString.contains("+")) 1 else -1
                (diceParts[1].toIntOrNull() ?: 0) * sign
            } else 0

            val dice = when (diceSize) {
                4 -> DiceType.D4
                6 -> DiceType.D6
                8 -> DiceType.D8
                10 -> DiceType.D10
                12 -> DiceType.D12
                20 -> DiceType.D20
                else -> DiceType.D6
            }

            rollDice(dice, count, modifier)
        } catch (e: Exception) {
            GameLogger.logError("DICE_SYSTEM", "Failed to parse damage roll: $rollString", e)
            rollDice(DiceType.D6, 1, 0)
        }
    }

    /**
     * Generate story prompt with dice results
     */
    fun generateDiceStoryPrompt(
        action: String,
        diceRoll: DiceRoll,
        characterName: String = "kahraman"
    ): String {
        val resultDescription = when {
            diceRoll.success == true -> when {
                diceRoll.total >= (diceRoll.difficulty?.threshold ?: 10) + 5 -> "mükemmel bir başarı"
                else -> "başarılı"
            }
            diceRoll.success == false -> when {
                diceRoll.total <= (diceRoll.difficulty?.threshold ?: 10) - 5 -> "feci bir başarısızlık"
                else -> "başarısız"
            }
            else -> when {
                diceRoll.total >= 15 -> "çok iyi"
                diceRoll.total >= 10 -> "iyi"
                diceRoll.total >= 5 -> "kötü"
                else -> "çok kötü"
            }
        }

        return """
            $characterName şu eylemi gerçekleştirmeye çalışıyor: "$action"

            Zar atışı: ${diceRoll.dice.emoji} ${diceRoll.total} (${diceRoll.results.joinToString(", ")})
            Sonuç: $resultDescription

            Bu zar sonucuna göre eylemi ve sonuçlarını detaylı bir şekilde anlat.
            Başarı durumuna uygun bir hikaye oluştur.
        """.trimIndent()
    }
}