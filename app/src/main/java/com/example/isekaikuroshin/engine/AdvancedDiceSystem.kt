package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.utils.GameLogger
import kotlin.random.Random

/**
 * L = Advanced Dice Integration System (Refactored for PlayerState)
 * This system is now more streamlined and directly interacts with the modern data classes.
 */

/**
 * L1 - ContextAdapterL1: Converts game state to DiceEngine context.
 */
object ContextAdapterL1 {

    private val locationDangerCalculator = LocationDangerCalculator()

    fun createDiceContext(
        gameState: GameStateZ7,
        actionType: DiceActionType,
        difficulty: Int = 12
    ): DiceContext {
        val playerState = gameState.playerState

        return DiceContext(
            // Environmental factors
            season = gameState.currentSeason,
            weather = gameState.currentWeather,
            timeOfDay = gameState.currentTimeOfDay,
            locationDanger = getCurrentLocationDanger(gameState),

            // Player condition from PlayerState
            healthPercentage = playerState.currentHealth.toFloat() / playerState.maxHealth,
            manaPercentage = playerState.currentMana.toFloat() / playerState.maxMana,
            moodState = calculateMoodState(playerState),

            // Player stats from PlayerState
            luck = playerState.passiveBonuses["luck"]?.toLong() ?: 0L, // Assuming luck is a passive bonus
            intelligence = playerState.intelligence.toLong(),
            charisma = playerState.passiveBonuses["charisma"]?.toLong() ?: 0L, // Assuming charisma is a passive bonus
            perception = playerState.passiveBonuses["perception"]?.toLong() ?: 0L, // Assuming perception is a passive bonus

            // Action context
            actionType = actionType,
            difficulty = difficulty,

            // Equipment bonuses
            equipmentBonus = calculateEquippedItemsBonus(gameState.equippedItems),

            // Status effects
            activeDebuffs = getActiveDebuffs(playerState),
            staminaPercentage = 1.0f, // TODO: Add stamina to PlayerState
            hungerLevel = 1.0f, // TODO: Add hunger to PlayerState
            fatigueLevel = 0.0f, // TODO: Add fatigue to PlayerState
            npcRelationship = 0.0f, // TODO: Add npcRelationship to GameState
            reputationLevel = 0 // TODO: Add reputation to GameState
        )
    }

    private fun getCurrentLocationDanger(gameState: GameStateZ7): Int {
        val currentLocation = gameState.knownLocations.find { it.id == gameState.currentLocationId }
            ?: return 3 // Default danger if location not found

        val dangerLevel = locationDangerCalculator.calculateDanger(
            location = currentLocation,
            playerLevel = gameState.playerState.level,
            currentTime = gameState.currentTimeOfDay
        )
        return (dangerLevel * 3).toInt().coerceIn(1, 6)
    }

    private fun calculateMoodState(playerState: PlayerState): MoodState {
        val healthPercentage = playerState.currentHealth.toFloat() / playerState.maxHealth
        return when {
            healthPercentage < 0.2 -> MoodState.FEARFUL
            healthPercentage < 0.5 -> MoodState.SAD
            healthPercentage > 0.9 -> MoodState.CONFIDENT
            else -> MoodState.NEUTRAL
        }
    }

    private fun calculateEquippedItemsBonus(equippedItems: Map<String, EquippedItemZ6>): Int {
        val totalBonus = equippedItems.values.sumOf { item ->
            item.statBonuses.values.map { bonusValue -> (bonusValue - 1f) * 10f }.sum().toInt() // DEĞİŞTİRİLDİ
        }
        return totalBonus.coerceAtMost(10)
    }

    private fun getActiveDebuffs(playerState: PlayerState): List<String> {
        val debuffs = mutableListOf<String>()
        if (playerState.currentHealth.toFloat() / playerState.maxHealth < 0.3) {
            debuffs.add("wounded")
        }
        return debuffs
    }
}

/**
 * L2 - ResultProcessorL2: Processes dice results into game effects.
 */
object ResultProcessorL2 {
    fun processResult(result: DiceResult, context: DiceContext): GameEffect {
        val effect = GameEffect()

        when (result.successLevel) {
            SuccessLevel.LEGENDARY_SUCCESS -> {
                effect.statChanges["experience"] = Random.nextInt(15, 25)
                effect.statChanges["luck_passive"] = 1 // Represents a temporary passive bonus
            }
            SuccessLevel.CRITICAL_SUCCESS -> {
                effect.statChanges["experience"] = Random.nextInt(10, 18)
                effect.statChanges[getRelevantStat(context.actionType)] = 1
            }
            SuccessLevel.GREAT_SUCCESS, SuccessLevel.SUCCESS -> {
                effect.statChanges["experience"] = Random.nextInt(5, 12)
            }
            SuccessLevel.PARTIAL_SUCCESS -> {
                effect.statChanges["experience"] = Random.nextInt(2, 6)
            }
            SuccessLevel.FAILURE -> { /* No positive effect */ }
            SuccessLevel.CRITICAL_FAILURE -> {
                effect.statChanges["health"] = -Random.nextInt(5, 15) // Negative value for damage
                effect.narrativeFlags.add("critical_failure_consequence")
            }
        }
        return effect
    }

    private fun getRelevantStat(actionType: DiceActionType): String {
        return when (actionType) {
            DiceActionType.COMBAT_ATTACK, DiceActionType.ATHLETICS -> "strength"
            DiceActionType.EXPLORATION_STEALTH -> "agility"
            DiceActionType.MAGIC_CASTING, DiceActionType.PUZZLE_SOLVING -> "intelligence"
            DiceActionType.EXPLORATION_SURVIVAL, DiceActionType.HEALING -> "vitality"
            else -> "strength"
        }
    }
}

/**
 * L3 - StoryIntegrationL3: Integrates dice results into narrative.
 */
object StoryIntegrationL3 {
    fun generateStoryText(playerAction: String, result: DiceResult, effect: GameEffect): String {
        val storyBuilder = StringBuilder()
        storyBuilder.append("🎲 $playerAction\n\n")
        storyBuilder.append("Zar Atışı: ${result.baseRoll} → ${result.modifiedRoll} ${getDiceEmoji(result.successLevel)}\n\n")
        storyBuilder.append(generateSuccessNarrative(result.successLevel))
        storyBuilder.append("\n")

        if (effect.statChanges.isNotEmpty()) {
            storyBuilder.append("\nSonuçlar:\n")
            effect.statChanges.forEach { (stat, change) ->
                val sign = if (change > 0) "+" else ""
                storyBuilder.append("• ${getStatDisplayName(stat)}: $sign$change\n")
            }
        }
        return storyBuilder.toString()
    }

    private fun getDiceEmoji(level: SuccessLevel) = when (level) {
        SuccessLevel.LEGENDARY_SUCCESS -> "🌟"
        SuccessLevel.CRITICAL_SUCCESS -> "⭐"
        SuccessLevel.GREAT_SUCCESS -> "✨"
        SuccessLevel.SUCCESS -> "✅"
        SuccessLevel.PARTIAL_SUCCESS -> "⚠️"
        SuccessLevel.FAILURE -> "❌"
        SuccessLevel.CRITICAL_FAILURE -> "💀"
    }

    private fun generateSuccessNarrative(level: SuccessLevel) = when (level) {
        SuccessLevel.LEGENDARY_SUCCESS -> "Efsanevi bir başarı! Bu olay uzun süre hatırlanacak."
        SuccessLevel.CRITICAL_SUCCESS -> "Mükemmel bir performans! Beklenmedik faydalar elde ettin."
        SuccessLevel.GREAT_SUCCESS -> "Harika bir başarı! Eylemini fazlasıyla tamamladın."
        SuccessLevel.SUCCESS -> "Eylemini başarıyla gerçekleştirdin."
        SuccessLevel.PARTIAL_SUCCESS -> "Kısmi başarı. İstediğini tam olarak elde edemedin."
        SuccessLevel.FAILURE -> "Başarısız oldun."
        SuccessLevel.CRITICAL_FAILURE -> "Feci bir başarısızlık! Ciddi sonuçlar doğurdu."
    }

    private fun getStatDisplayName(stat: String) = when (stat) {
        "strength" -> "Güç"
        "agility" -> "Çeviklik"
        "intelligence" -> "Zeka"
        "vitality" -> "Canlılık"
        "experience" -> "Tecrübe"
        "health" -> "Can"
        "mana" -> "Mana"
        "luck_passive" -> "Şans (Pasif)"
        else -> stat.replaceFirstChar { it.uppercase() }
    }
}

/**
 * L4 - ActionSystemL4: Main interface for dice-based actions.
 */
object ActionSystemL4 {
    fun performAction(
        gameStateManager: GameStateManager,
        playerAction: String,
        actionType: DiceActionType,
        difficulty: Int = 12
    ): String {
        GameLogger.logDiceSystem("Starting dice action: $playerAction")
        val gameState = gameStateManager.gameState.value

        val context = ContextAdapterL1.createDiceContext(gameState, actionType, difficulty)
        val diceResult = DiceEngine.calculateComplexRoll(context)
        val gameEffect = ResultProcessorL2.processResult(diceResult, context)

        applyGameEffects(gameStateManager, gameEffect)

        val storyText = StoryIntegrationL3.generateStoryText(playerAction, diceResult, gameEffect)
        gameStateManager.addStoryPage(storyText)

        GameLogger.logDiceSystem("Dice action completed: ${diceResult.successLevel}")
        return storyText
    }

    private fun applyGameEffects(gameStateManager: GameStateManager, effect: GameEffect) {
        effect.statChanges.forEach { (stat, change) ->
            when (stat) {
                "experience" -> gameStateManager.addExperience(change)
                "health" -> gameStateManager.modifyHealth(change)
                "mana" -> gameStateManager.modifyMana(change)
                "strength", "agility", "intelligence", "vitality" -> {
                    val currentStats = gameStateManager.gameState.value.playerState
                    val newStats = when(stat) {
                        "strength" -> currentStats.copy(strength = currentStats.strength + change)
                        "agility" -> currentStats.copy(agility = currentStats.agility + change)
                        "intelligence" -> currentStats.copy(intelligence = currentStats.intelligence + change)
                        "vitality" -> currentStats.copy(vitality = currentStats.vitality + change)
                        else -> currentStats
                    }
                    gameStateManager.updatePlayerState(newStats)
                }
                // TODO: Handle passive bonuses like 'luck_passive'
            }
        }
    }
}

// Simplified data classes for the L system
data class GameEffect(
    val statChanges: MutableMap<String, Int> = mutableMapOf(),
    val resourceGains: MutableMap<String, Int> = mutableMapOf(),
    val narrativeFlags: MutableList<String> = mutableListOf() // DEĞİŞTİRİLDİ
)