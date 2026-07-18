package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.combat.LearnedSpell
import com.example.isekaikuroshin.data.PlayerStatsPersistent
import com.example.isekaikuroshin.engine.combat.RealityEngine

/**
 * G�REV N - FAZ 6: B�y� Sava_ Helper
 *
 * LearnedSpell verilerini kullanarak sava_ mekanii hesaplamalar1 yapar:
 * - SkillTreeLevel � DiceSystem.AdvantageLevel mapping
 * - Performance score � Dice modifier
 * - Spell power calculation
 */
object SpellCombatHelper {

    /**
     * Skill tree level'a g�re advantage seviyesi hesapla
     *
     * Skill Tree Level (0-10):
     * - 0-2: Normal (avantaj yok)
     * - 3-5: Advantage (2 zar, en y�ksek)
     * - 6-8: Double Advantage (3 zar, en y�ksek)
     * - 9-10: Double Advantage + bonus modifier
     */
    fun calculateAdvantageLevel(
        spell: LearnedSpell,
        performanceScore: Float  // 0.0 - 1.0
    ): DiceSystem.AdvantageLevel {
        val skillLevel = spell.skillTreeLevel

        // K�t� performans cezas1 (<%50 ba_ar1)
        if (performanceScore < 0.5f) {
            return when {
                skillLevel >= 6 -> DiceSystem.AdvantageLevel.NORMAL  // D�_�r�l�yor
                skillLevel >= 3 -> DiceSystem.AdvantageLevel.DISADVANTAGE
                else -> DiceSystem.AdvantageLevel.DOUBLE_DISADVANTAGE
            }
        }

        // Normal/0yi performans
        return when {
            skillLevel >= 9 -> DiceSystem.AdvantageLevel.DOUBLE_ADVANTAGE  // Master level
            skillLevel >= 6 -> DiceSystem.AdvantageLevel.DOUBLE_ADVANTAGE  // Expert
            skillLevel >= 3 -> DiceSystem.AdvantageLevel.ADVANTAGE         // Proficient
            else -> DiceSystem.AdvantageLevel.NORMAL                       // Beginner
        }
    }

    /**
     * Performance score'a g�re dice modifier hesapla
     *
     * Performance (0.0-1.0) � Modifier:
     * - 0.95+: +5
     * - 0.85-0.94: +3
     * - 0.70-0.84: +2
     * - 0.50-0.69: +0
     * - 0.30-0.49: -2
     * - <0.30: -5
     */
    fun calculatePerformanceModifier(performanceScore: Float): Int {
        return when {
            performanceScore >= 0.95f -> 5
            performanceScore >= 0.85f -> 3
            performanceScore >= 0.70f -> 2
            performanceScore >= 0.50f -> 0
            performanceScore >= 0.30f -> -2
            else -> -5
        }
    }

    /**
     * Skill tree bonus modifier (seviye 9-10 i�in ekstra)
     */
    fun calculateSkillTreeBonus(skillLevel: Int): Int {
        return when {
            skillLevel >= 10 -> 3  // Master bonus
            skillLevel >= 9 -> 2   // Expert bonus
            else -> 0
        }
    }

    /**
     * Toplam modifier hesapla
     */
    fun calculateTotalModifier(
        spell: LearnedSpell,
        performanceScore: Float
    ): Int {
        val performanceMod = calculatePerformanceModifier(performanceScore)
        val skillBonus = calculateSkillTreeBonus(spell.skillTreeLevel)
        return performanceMod + skillBonus
    }

    /**
     * B�y� g�c� hesapla (damage/healing i�in)
     *
     * Base power + SkillTreeLevel scaling + Performance scaling
     */
    fun calculateSpellPower(
        spell: LearnedSpell,
        performanceScore: Float,
        basePower: Int = 10  // B�y�n�n temel g�c�
    ): Int {
        val skillScaling = spell.skillTreeLevel * 2  // Her seviye +2 power
        val performanceScaling = (performanceScore * 10).toInt()  // %100 = +10 power

        return basePower + skillScaling + performanceScaling
    }

    /**
     * Sava_ sonucu i�in dice roll yap
     */
    fun rollSpellAttack(
        spell: LearnedSpell,
        performanceScore: Float,
        difficulty: DiceSystem.Difficulty = DiceSystem.Difficulty.MEDIUM
    ): DiceSystem.DiceRoll {
        val advantageLevel = calculateAdvantageLevel(spell, performanceScore)
        val modifier = calculateTotalModifier(spell, performanceScore)

        return DiceSystem.skillCheck(
            skillModifier = modifier,
            difficulty = difficulty,
            dice = DiceSystem.DiceType.D20,
            advantageLevel = advantageLevel
        )
    }

    /**
     * Sava_ sonucu detayl1 rapor
     *
     * FAZ 8.3: RealityEngine entegrasyonu eklendi
     */
    data class SpellCombatResult(
        val diceRoll: DiceSystem.DiceRoll,
        val spellPower: Int,
        val advantageLevel: DiceSystem.AdvantageLevel,
        val totalModifier: Int,
        val successMessage: String,
        // FAZ 8.3: Reality Engine feedback
        val gmComment: String = "",          // "Manan yetersiz!" gibi
        val luckMessage: String = "",        // "✨ KADER MÜDAHALESI!" gibi
        val wasLuckSaved: Boolean = false    // LUCK ile kurtarıldı mı?
    )

    /**
     * B�y� sava_ sonucunu hesapla
     */
    fun executeSpellCombat(
        spell: LearnedSpell,
        performanceScore: Float,
        difficulty: DiceSystem.Difficulty = DiceSystem.Difficulty.MEDIUM
    ): SpellCombatResult {
        val diceRoll = rollSpellAttack(spell, performanceScore, difficulty)
        val spellPower = calculateSpellPower(spell, performanceScore)
        val advantageLevel = calculateAdvantageLevel(spell, performanceScore)
        val totalModifier = calculateTotalModifier(spell, performanceScore)

        val successMessage = if (diceRoll.success) {
            " ${spell.customName} ba_ar1l1! $spellPower hasar!"
        } else {
            "L ${spell.customName} ba_ar1s1z oldu!"
        }

        return SpellCombatResult(
            diceRoll = diceRoll,
            spellPower = spellPower,
            advantageLevel = advantageLevel,
            totalModifier = totalModifier,
            successMessage = successMessage
        )
    }

    /**
     * FAZ 8.3: RealityEngine + LUCK Entegrasyonlu Versiyon
     */
    fun executeSpellCombatWithReality(
        spell: LearnedSpell,
        performanceScore: Float,
        difficulty: DiceSystem.Difficulty = DiceSystem.Difficulty.MEDIUM,
        playerStats: PlayerStatsPersistent
    ): SpellCombatResult {
        var gmComment: String
        var luckMessage = ""
        var wasLuckSaved = false
        var realityModifier: Int

        val realityCheck = RealityEngine.analyzeAction(spell, playerStats)
        if (!realityCheck.isValid) {
            return SpellCombatResult(
                diceRoll = DiceSystem.DiceRoll(
                    dice = DiceSystem.DiceType.D20,
                    count = 1,
                    modifier = 0,
                    results = listOf(0),
                    total = 0,
                    success = false,
                    difficulty = difficulty,
                    advantageLevel = DiceSystem.AdvantageLevel.NORMAL
                ),
                spellPower = 0,
                advantageLevel = DiceSystem.AdvantageLevel.NORMAL,
                totalModifier = 0,
                successMessage = "X ${spell.customName} kullanilamadi!",
                gmComment = realityCheck.gmComment,
                luckMessage = "",
                wasLuckSaved = false
            )
        }
        gmComment = realityCheck.gmComment
        realityModifier = realityCheck.diceModifier

        val advantageLevel = calculateAdvantageLevel(spell, performanceScore)
        val performanceMod = calculatePerformanceModifier(performanceScore)
        val skillBonus = calculateSkillTreeBonus(spell.skillTreeLevel)
        val totalModifier = performanceMod + skillBonus + realityModifier

        val diceRoll = DiceSystem.skillCheck(
            skillModifier = totalModifier,
            difficulty = difficulty,
            dice = DiceSystem.DiceType.D20,
            advantageLevel = advantageLevel
        )

        var finalSuccess = diceRoll.success
        if (!diceRoll.success) {
            val luckResult = RealityEngine.checkLuckIntervention(
                diceResult = diceRoll.total,
                targetDC = difficulty.threshold,
                luckStat = playerStats.luck
            )
            if (luckResult.saved) {
                finalSuccess = true
                wasLuckSaved = true
                luckMessage = luckResult.message
            }
        }

        val spellPower = calculateSpellPower(spell, performanceScore)
        val successMessage = if (finalSuccess) {
            if (wasLuckSaved) "o ${spell.customName} mucizevi bir sekilde basarili! $spellPower hasar!"
            else ". ${spell.customName} basarili! $spellPower hasar!"
        } else {
            "X ${spell.customName} basarisiz oldu!"
        }

        return SpellCombatResult(
            diceRoll = diceRoll.copy(success = finalSuccess),
            spellPower = if (finalSuccess) spellPower else 0,
            advantageLevel = advantageLevel,
            totalModifier = totalModifier,
            successMessage = successMessage,
            gmComment = gmComment,
            luckMessage = luckMessage,
            wasLuckSaved = wasLuckSaved
        )
    }
}
