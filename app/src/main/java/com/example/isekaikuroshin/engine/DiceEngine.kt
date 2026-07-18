package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.TimeOfDay
import kotlin.random.Random
import kotlinx.serialization.Serializable

// FIX-TASK-2.2: displayName değerleri İngilizce olmalı - UI katmanı çevirecek
@Serializable
enum class Season(val displayName: String) {
    SPRING("Spring"),
    SUMMER("Summer"),
    AUTUMN("Autumn"),
    WINTER("Winter")
}

// FIX-TASK-2.2: displayName değerleri İngilizce olmalı - UI katmanı çevirecek
@Serializable
enum class Weather(val displayName: String) {
    SUNNY("Sunny"),
    RAINY("Rainy"),
    STORMY("Stormy"),
    FOGGY("Foggy"),
    SNOWY("Snowy"),
    WINDY("Windy"),
    CLOUDY("Cloudy"),
    HOT("Hot"),
    COLD("Cold"),
    BLIZZARD("Blizzard")
}

// TimeOfDay enum moved to data package

@Serializable
enum class MoodState {
    EXCITED,
    HAPPY,
    CALM,
    NEUTRAL,
    TIRED,
    SAD,
    ANGRY,
    FEARFUL,
    CONFIDENT,
    ANXIOUS
}

@Serializable
data class DiceContext(
    // Environmental factors
    val season: Season,
    val weather: Weather,
    val timeOfDay: TimeOfDay,
    val locationDanger: Int, // 1-10 scale

    // Player condition
    val healthPercentage: Float, // 0.0 - 1.0
    val manaPercentage: Float,
    val staminaPercentage: Float,
    val hungerLevel: Float, // 0.0 (starving) - 1.0 (full)
    val fatigueLevel: Float, // 0.0 (rested) - 1.0 (exhausted)
    val moodState: MoodState,

    // Player stats
    val luck: Long,
    val intelligence: Long,
    val charisma: Long,
    val perception: Long,

    // Relationship factors
    val npcRelationship: Float, // -1.0 (enemy) to 1.0 (best friend)
    val reputationLevel: Int, // -100 to 100

    // Action context
    val actionType: DiceActionType,
    val difficulty: Int, // 1-20 scale
    val hasAdvantage: Boolean = false,
    val hasDisadvantage: Boolean = false,

    // Equipment bonuses
    val equipmentBonus: Int = 0,
    val magicalEnhancement: Float = 1.0f,

    // Status effects
    val activeBuffs: List<String> = emptyList(),
    val activeDebuffs: List<String> = emptyList(),

    // Recent history impact
    val recentSuccesses: Int = 0,
    val recentFailures: Int = 0,
    val consecutiveActions: Int = 0
)

@Serializable
enum class DiceActionType {
    COMBAT_ATTACK,
    COMBAT_DEFENSE,
    SOCIAL_PERSUASION,
    SOCIAL_DECEPTION,
    SOCIAL_INTIMIDATION,
    EXPLORATION_STEALTH,
    EXPLORATION_PERCEPTION,
    EXPLORATION_SURVIVAL,
    MAGIC_CASTING,
    CRAFTING,
    TRADING,
    LEARNING,
    HEALING,
    PUZZLE_SOLVING,
    ATHLETICS,
    INVESTIGATION
}

@Serializable
data class DiceResult(
    val baseRoll: Int,
    val modifiedRoll: Int,
    val isCriticalSuccess: Boolean,
    val isCriticalFailure: Boolean,
    val successLevel: SuccessLevel,
    val bonusEffects: List<String> = emptyList(),
    val penalties: List<String> = emptyList(),
    val environmentalEffects: List<String> = emptyList(),
    val narrativeFlags: List<String> = emptyList()
)

@Serializable
enum class SuccessLevel {
    CRITICAL_FAILURE,
    FAILURE,
    PARTIAL_SUCCESS,
    SUCCESS,
    GREAT_SUCCESS,
    CRITICAL_SUCCESS,
    LEGENDARY_SUCCESS
}

object DiceEngine {

    fun calculateComplexRoll(context: DiceContext, externalModifier: Int = 0): DiceResult {
        val baseRoll = Random.nextInt(1, 21) // d20
        var totalModifier = 0
        val bonusEffects = mutableListOf<String>()
        val penalties = mutableListOf<String>()
        val environmentalEffects = mutableListOf<String>()
        val narrativeFlags = mutableListOf<String>()

        // Environmental modifiers
        totalModifier += calculateEnvironmentalModifier(context, environmentalEffects)

        // Player condition modifiers
        totalModifier += calculateConditionModifier(context, penalties, bonusEffects)

        // Stat-based modifiers
        totalModifier += calculateStatModifier(context)

        // Relationship modifiers
        totalModifier += calculateRelationshipModifier(context, bonusEffects, penalties)

        // Action-specific modifiers
        totalModifier += calculateActionModifier(context, bonusEffects, penalties)

        // Equipment and magical modifiers
        totalModifier += (context.equipmentBonus * context.magicalEnhancement).toInt()

        // Status effect modifiers
        totalModifier += calculateStatusEffectModifier(context, bonusEffects, penalties)

        // Historical momentum modifiers
        totalModifier += calculateMomentumModifier(context, bonusEffects, penalties)

        // External modifiers (Umbros manipulation, etc.)
        totalModifier += externalModifier
        if (externalModifier != 0) {
            if (externalModifier > 0) {
                bonusEffects.add("External force aids your action (+$externalModifier)")
            } else {
                penalties.add("External force hinders your action ($externalModifier)")
            }
        }

        // Advantage/Disadvantage system
        val finalRoll = if (context.hasAdvantage && !context.hasDisadvantage) {
            val secondRoll = Random.nextInt(1, 21)
            maxOf(baseRoll, secondRoll).also {
                bonusEffects.add("Advantage: Rolled twice, took higher")
            }
        } else if (context.hasDisadvantage && !context.hasAdvantage) {
            val secondRoll = Random.nextInt(1, 21)
            minOf(baseRoll, secondRoll).also {
                penalties.add("Disadvantage: Rolled twice, took lower")
            }
        } else {
            baseRoll
        }

        val modifiedRoll = finalRoll + totalModifier

        // Determine critical results
        val isCriticalSuccess = finalRoll == 20 || modifiedRoll >= (context.difficulty + 15)
        val isCriticalFailure = finalRoll == 1 || modifiedRoll <= (context.difficulty - 10)

        // Generate narrative flags based on context
        generateNarrativeFlags(context, modifiedRoll, narrativeFlags)

        val successLevel = determineSuccessLevel(modifiedRoll, context.difficulty, isCriticalSuccess, isCriticalFailure)

        return DiceResult(
            baseRoll = finalRoll,
            modifiedRoll = modifiedRoll,
            isCriticalSuccess = isCriticalSuccess,
            isCriticalFailure = isCriticalFailure,
            successLevel = successLevel,
            bonusEffects = bonusEffects,
            penalties = penalties,
            environmentalEffects = environmentalEffects,
            narrativeFlags = narrativeFlags
        )
    }

    private fun calculateEnvironmentalModifier(context: DiceContext, effects: MutableList<String>): Int {
        var modifier = 0

        // Season effects
        when (context.season) {
            Season.SPRING -> {
                if (context.actionType in listOf(DiceActionType.EXPLORATION_SURVIVAL, DiceActionType.HEALING)) {
                    modifier += 2
                    effects.add("Spring renewal enhances life-based actions")
                }
            }
            Season.SUMMER -> {
                if (context.actionType == DiceActionType.ATHLETICS) {
                    modifier += 1
                    effects.add("Summer warmth boosts physical performance")
                }
            }
            Season.AUTUMN -> {
                if (context.actionType == DiceActionType.LEARNING) {
                    modifier += 2
                    effects.add("Autumn wisdom enhances learning")
                }
            }
            Season.WINTER -> {
                if (context.actionType == DiceActionType.EXPLORATION_STEALTH) {
                    modifier += 1
                    effects.add("Winter silence aids stealth")
                }
            }
        }

        // Weather effects
        when (context.weather) {
            Weather.RAINY -> {
                if (context.actionType == DiceActionType.EXPLORATION_STEALTH) modifier += 2
                else if (context.actionType == DiceActionType.EXPLORATION_PERCEPTION) modifier -= 1
                effects.add("Rain affects visibility and stealth")
            }
            Weather.STORMY -> {
                if (context.actionType == DiceActionType.MAGIC_CASTING) modifier += 3
                else modifier -= 2
                effects.add("Storm energy enhances magic but hinders other actions")
            }
            Weather.FOGGY -> {
                if (context.actionType == DiceActionType.EXPLORATION_PERCEPTION) modifier -= 3
                effects.add("Fog severely impairs perception")
            }
            Weather.SUNNY -> {
                modifier += 1
                effects.add("Clear weather provides general bonus")
            }
            Weather.BLIZZARD -> {
                modifier -= 4
                effects.add("Blizzard severely hinders all actions")
            }
            else -> { /* No special effects */ }
        }

        // Time of day effects
        when (context.timeOfDay) {
            TimeOfDay.MORNING -> {
                if (context.actionType == DiceActionType.MAGIC_CASTING) modifier += 1
                effects.add("Morning's magical energy provides minor bonus")
            }
            TimeOfDay.NOON -> {
                modifier += 1
                effects.add("Peak daylight provides clarity bonus")
            }
            TimeOfDay.NIGHT -> {
                if (context.actionType == DiceActionType.EXPLORATION_STEALTH) modifier += 2
                else if (context.actionType == DiceActionType.EXPLORATION_PERCEPTION) modifier -= 2
                effects.add("Darkness aids stealth but hinders perception")
            }
            // AFTERNOON and EVENING have no explicit modifiers here in the original code snippet
            // Make sure this is intended or if they should have effects like MORNING/NOON/NIGHT.
            // For now, I'm keeping it as per the provided snippet.
            else -> { /* No special effects for AFTERNOON/EVENING implicitly */ }
        }

        // Location danger effects
        if (context.locationDanger > 7) {
            modifier -= 2
            effects.add("Extreme danger causes stress penalty")
        } else if (context.locationDanger < 3) {
            modifier += 1
            effects.add("Safe environment provides confidence bonus")
        }

        return modifier
    }

    private fun calculateConditionModifier(context: DiceContext, penalties: MutableList<String>, bonuses: MutableList<String>): Int {
        var modifier = 0

        // Health effects
        when {
            context.healthPercentage < 0.2f -> {
                modifier -= 4
                penalties.add("Critical health severely impairs performance")
            }
            context.healthPercentage < 0.5f -> {
                modifier -= 2
                penalties.add("Low health reduces effectiveness")
            }
            context.healthPercentage > 0.9f -> {
                modifier += 1
                bonuses.add("Excellent health provides vitality bonus")
            }
        }

        // Stamina effects
        when {
            context.staminaPercentage < 0.3f -> {
                modifier -= 2
                penalties.add("Exhaustion impairs physical actions")
            }
            context.staminaPercentage > 0.8f -> {
                modifier += 1
                bonuses.add("High stamina boosts performance")
            }
        }

        // Hunger effects
        when {
            context.hungerLevel < 0.2f -> {
                modifier -= 3
                penalties.add("Starvation severely weakens abilities")
            }
            context.hungerLevel < 0.5f -> {
                modifier -= 1
                penalties.add("Hunger causes minor distraction")
            }
            context.hungerLevel > 0.8f -> {
                modifier += 1
                bonuses.add("Well-fed condition provides energy bonus")
            }
        }

        // Fatigue effects
        when {
            context.fatigueLevel > 0.8f -> {
                modifier -= 3
                penalties.add("Extreme fatigue impairs concentration")
            }
            context.fatigueLevel > 0.5f -> {
                modifier -= 1
                penalties.add("Tiredness reduces focus")
            }
            context.fatigueLevel < 0.2f -> {
                modifier += 1
                bonuses.add("Well-rested state enhances performance")
            }
        }

        // Mood effects
        when (context.moodState) {
            MoodState.EXCITED, MoodState.CONFIDENT -> {
                modifier += 2
                bonuses.add("Positive mood enhances capabilities")
            }
            MoodState.HAPPY, MoodState.CALM -> {
                modifier += 1
                bonuses.add("Good mood provides minor bonus")
            }
            MoodState.SAD, MoodState.ANXIOUS -> {
                modifier -= 2
                penalties.add("Negative mood impairs performance")
            }
            MoodState.ANGRY -> {
                if (context.actionType == DiceActionType.COMBAT_ATTACK) {
                    modifier += 1
                    bonuses.add("Anger fuels aggressive actions")
                } else {
                    modifier -= 1
                    penalties.add("Anger clouds judgment")
                }
            }
            MoodState.FEARFUL -> {
                modifier -= 3
                penalties.add("Fear severely impairs decision-making")
            }
            else -> { /* Neutral mood, no effects */ }
        }

        return modifier
    }

    private fun calculateStatModifier(context: DiceContext): Int {
        val luckMod = (context.luck / 20).toInt() // Every 20 points of luck = +1
        val intMod = when (context.actionType) {
            DiceActionType.MAGIC_CASTING, DiceActionType.PUZZLE_SOLVING, DiceActionType.LEARNING -> (context.intelligence / 15).toInt()
            else -> 0
        }
        val charismaMod = when (context.actionType) {
            DiceActionType.SOCIAL_PERSUASION, DiceActionType.SOCIAL_DECEPTION, DiceActionType.TRADING -> (context.charisma / 15).toInt()
            else -> 0
        }
        val perceptionMod = when (context.actionType) {
            DiceActionType.EXPLORATION_PERCEPTION, DiceActionType.INVESTIGATION -> (context.perception / 15).toInt()
            else -> 0
        }

        return luckMod + intMod + charismaMod + perceptionMod
    }

    private fun calculateRelationshipModifier(context: DiceContext, bonuses: MutableList<String>, penalties: MutableList<String>): Int {
        var modifier = 0

        // NPC relationship effects
        when {
            context.npcRelationship > 0.7f -> {
                modifier += 3
                bonuses.add("Strong positive relationship provides major bonus")
            }
            context.npcRelationship > 0.3f -> {
                modifier += 1
                bonuses.add("Good relationship provides minor bonus")
            }
            context.npcRelationship < -0.7f -> {
                modifier -= 3
                penalties.add("Strong negative relationship causes major penalty")
            }
            context.npcRelationship < -0.3f -> {
                modifier -= 1
                penalties.add("Poor relationship causes minor penalty")
            }
        }

        // Reputation effects
        when {
            context.reputationLevel > 50 -> {
                modifier += 2
                bonuses.add("Excellent reputation opens doors")
            }
            context.reputationLevel > 20 -> {
                modifier += 1
                bonuses.add("Good reputation provides advantage")
            }
            context.reputationLevel < -50 -> {
                modifier -= 2
                penalties.add("Terrible reputation creates obstacles")
            }
            context.reputationLevel < -20 -> {
                modifier -= 1
                penalties.add("Poor reputation causes suspicion")
            }
        }

        return modifier
    }

    private fun calculateActionModifier(context: DiceContext, bonuses: MutableList<String>, penalties: MutableList<String>): Int {
        var modifier = 0

        // Base difficulty adjustment
        modifier -= (context.difficulty - 10) / 2

        // Action-specific conditional modifiers
        when (context.actionType) {
            DiceActionType.MAGIC_CASTING -> {
                if (context.manaPercentage < 0.3f) {
                    modifier -= 2
                    penalties.add("Low mana impairs spellcasting")
                }
            }
            DiceActionType.COMBAT_ATTACK -> {
                if (context.healthPercentage < 0.5f) {
                    modifier -= 1
                    penalties.add("Injuries affect combat effectiveness")
                }
            }
            DiceActionType.SOCIAL_PERSUASION -> {
                if (context.moodState == MoodState.CALM || context.moodState == MoodState.CONFIDENT) {
                    modifier += 1
                    bonuses.add("Calm demeanor aids persuasion")
                }
            }
            else -> { /* No special action modifiers */ }
        }

        return modifier
    }

    private fun calculateStatusEffectModifier(context: DiceContext, bonuses: MutableList<String>, penalties: MutableList<String>): Int {
        var modifier = 0

        // Count and apply buff effects
        context.activeBuffs.forEach { buff ->
            when (buff.lowercase()) {
                "blessed", "divine_favor" -> {
                    modifier += 2
                    bonuses.add("Divine blessing enhances all actions")
                }
                "haste", "speed_boost" -> {
                    modifier += 1
                    bonuses.add("Enhanced speed improves reaction time")
                }
                "strength", "power_boost" -> {
                    if (context.actionType in listOf(DiceActionType.COMBAT_ATTACK, DiceActionType.ATHLETICS)) {
                        modifier += 2
                        bonuses.add("Enhanced strength boosts physical actions")
                    }
                }
                "wisdom", "clarity" -> {
                    if (context.actionType in listOf(DiceActionType.MAGIC_CASTING, DiceActionType.PUZZLE_SOLVING)) {
                        modifier += 2
                        bonuses.add("Mental clarity enhances intellectual actions")
                    }
                }
            }
        }

        // Count and apply debuff effects
        context.activeDebuffs.forEach { debuff ->
            when (debuff.lowercase()) {
                "cursed", "hex" -> {
                    modifier -= 2
                    penalties.add("Curse impedes all actions")
                }
                "poisoned" -> {
                    modifier -= 1
                    penalties.add("Poison weakens performance")
                }
                "confused", "dazed" -> {
                    modifier -= 2
                    penalties.add("Mental confusion impairs judgment")
                }
                "slowed", "encumbered" -> {
                    modifier -= 1
                    penalties.add("Movement restriction affects agility")
                }
            }
        }

        return modifier
    }

    private fun calculateMomentumModifier(context: DiceContext, bonuses: MutableList<String>, penalties: MutableList<String>): Int {
        var modifier = 0

        // Success momentum
        if (context.recentSuccesses > 2) {
            modifier += 2
            bonuses.add("Success momentum boosts confidence")
        } else if (context.recentSuccesses > 0) {
            modifier += 1
            bonuses.add("Recent success provides minor confidence")
        }

        // Failure penalty
        if (context.recentFailures > 2) {
            modifier -= 2
            penalties.add("Repeated failures shake confidence")
        } else if (context.recentFailures > 0) {
            modifier -= 1
            penalties.add("Recent failure causes minor doubt")
        }

        // Consecutive action fatigue
        if (context.consecutiveActions > 5) {
            modifier -= 1
            penalties.add("Repetitive actions cause mental fatigue")
        }

        return modifier
    }

    private fun generateNarrativeFlags(context: DiceContext, modifiedRoll: Int, flags: MutableList<String>) {
        // Environmental narrative flags
        if (context.weather == Weather.STORMY && context.actionType == DiceActionType.MAGIC_CASTING) {
            flags.add("storm_magic_resonance")
        }

        if (context.timeOfDay == TimeOfDay.NIGHT && modifiedRoll > 15) {
            flags.add("midnight_mystique")
        }

        // Condition-based narrative flags
        if (context.healthPercentage < 0.3f && modifiedRoll > context.difficulty) {
            flags.add("heroic_perseverance")
        }

        if (context.moodState == MoodState.FEARFUL && modifiedRoll > context.difficulty + 5) {
            flags.add("courage_under_pressure")
        }

        // Relationship narrative flags
        if (context.npcRelationship < -0.5f && modifiedRoll > context.difficulty) {
            flags.add("unexpected_breakthrough")
        }

        // Luck-based narrative flags
        if (context.luck > 50 && modifiedRoll == 20) {
            flags.add("fortune_favors_bold")
        }
    }

    private fun determineSuccessLevel(modifiedRoll: Int, difficulty: Int, isCritical: Boolean, isFailure: Boolean): SuccessLevel {
        return when {
            isFailure -> SuccessLevel.CRITICAL_FAILURE
            isCritical && modifiedRoll >= difficulty + 20 -> SuccessLevel.LEGENDARY_SUCCESS
            isCritical -> SuccessLevel.CRITICAL_SUCCESS
            modifiedRoll >= difficulty + 10 -> SuccessLevel.GREAT_SUCCESS
            modifiedRoll >= difficulty -> SuccessLevel.SUCCESS
            modifiedRoll >= difficulty - 3 -> SuccessLevel.PARTIAL_SUCCESS
            else -> SuccessLevel.FAILURE
        }
    }

    fun generateConsequence(result: DiceResult, context: DiceContext): String {
        val consequences = mutableListOf<String>()

        // Base consequence based on success level
        val baseConsequence = when (result.successLevel) {
            SuccessLevel.LEGENDARY_SUCCESS -> "Your action succeeds beyond all expectations, creating ripple effects that will be remembered for ages."
            SuccessLevel.CRITICAL_SUCCESS -> "Outstanding success! Your action not only succeeds but provides unexpected additional benefits."
            SuccessLevel.GREAT_SUCCESS -> "Excellent execution! Your action succeeds admirably with notable positive side effects."
            SuccessLevel.SUCCESS -> "Your action succeeds as intended, achieving the desired outcome."
            SuccessLevel.PARTIAL_SUCCESS -> "You achieve some success, but there are complications or limitations to the outcome."
            SuccessLevel.FAILURE -> "Your action fails to achieve the intended result, but without major negative consequences."
            SuccessLevel.CRITICAL_FAILURE -> "Not only does your action fail, but it creates significant problems and setbacks."
        }
        consequences.add(baseConsequence)

        // Add environmental consequences
        result.environmentalEffects.forEach { effect ->
            consequences.add("Environmental factor: $effect")
        }

        // Add narrative consequences based on flags
        result.narrativeFlags.forEach { flag ->
            when (flag) {
                "storm_magic_resonance" -> consequences.add("The storm's energy amplifies your magical abilities, but also attracts unwanted attention.")
                "midnight_mystique" -> consequences.add("The midnight hour reveals hidden truths and secret paths.")
                "heroic_perseverance" -> consequences.add("Your determination despite injury inspires those around you.")
                "courage_under_pressure" -> consequences.add("Overcoming your fear grants you inner strength and confidence.")
                "unexpected_breakthrough" -> consequences.add("Your success despite their hostility surprises everyone and may change their opinion.")
                "fortune_favors_bold" -> consequences.add("Lady Luck smiles upon your audacious action, granting serendipitous benefits.")
            }
        }

        // Add relationship consequences
        if (context.actionType in listOf(DiceActionType.SOCIAL_PERSUASION, DiceActionType.SOCIAL_DECEPTION, DiceActionType.SOCIAL_INTIMIDATION)) {
            when (result.successLevel) {
                SuccessLevel.CRITICAL_SUCCESS, SuccessLevel.LEGENDARY_SUCCESS ->
                    consequences.add("Your social success significantly improves your relationship and reputation.")
                SuccessLevel.CRITICAL_FAILURE ->
                    consequences.add("Your social failure damages relationships and may have lasting reputation consequences.")
                else -> { /* No special relationship effects */ }
            }
        }

        return consequences.joinToString(" ")
    }
}