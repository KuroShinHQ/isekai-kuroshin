package com.example.isekaikuroshin.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
// Correct StatType import should be from the engine package
import com.example.isekaikuroshin.engine.StatType 
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

/**
 * TrainingUiState data class containing only the data needed for TrainingScreen.
 * This is derived from the single source of truth, PlayerState.
 */
data class TrainingUiState(
    val strength: Int = 10,
    val agility: Int = 10,
    val intelligence: Int = 10,
    val vitality: Int = 10,
    val gold: Int = 0,
    val strengthTrainingCost: Int = 50,
    val agilityTrainingCost: Int = 45,
    val intelligenceTrainingCost: Int = 55,
    val vitalityTrainingCost: Int = 60,
    val canTrainStrength: Boolean = false,
    val canTrainAgility: Boolean = false,
    val canTrainIntelligence: Boolean = false,
    val canTrainVitality: Boolean = false,
    val physicalGp: Map<String, Int> = emptyMap(),
    val spiritualGp: Map<String, Int> = emptyMap()
)

/**
 * TrainingViewModel serves as an intermediary between TrainingScreen and GameStateManager.
 * It exposes the UI state and handles user intents.
 */
@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    /**
     * Maps the full GameStateZ7 to the simplified TrainingUiState.
     * This keeps the UI clean and dependent only on what it needs.
     */
    val uiState: StateFlow<TrainingUiState> = gameStateManager.gameState
        .map { gameState ->
            val playerState = gameState.playerState
            // Ensure getTrainingCost is called with the correct StatType (from engine package)
            val strengthCost = gameStateManager.getTrainingCost(StatType.STR)
            val agilityCost = gameStateManager.getTrainingCost(StatType.AGI)
            val intelligenceCost = gameStateManager.getTrainingCost(StatType.INT)
            val vitalityCost = gameStateManager.getTrainingCost(StatType.VIT)

            TrainingUiState(
                strength = playerState.strength.toInt(),
                agility = playerState.agility.toInt(),
                intelligence = playerState.intelligence.toInt(),
                vitality = playerState.vitality.toInt(),
                gold = playerState.gold,
                strengthTrainingCost = strengthCost,
                agilityTrainingCost = agilityCost,
                intelligenceTrainingCost = intelligenceCost,
                vitalityTrainingCost = vitalityCost,
                canTrainStrength = playerState.gold >= strengthCost,
                canTrainAgility = playerState.gold >= agilityCost,
                canTrainIntelligence = playerState.gold >= intelligenceCost,
                canTrainVitality = playerState.gold >= vitalityCost,
                physicalGp = mapOf(
                    "muscleFiberGp" to 0,
                    "boneDensityGp" to 0,
                    "bloodFlowGp" to 0
                ),
                spiritualGp = mapOf(
                    "qiControlGp" to 0,
                    "manaAffinityGp" to 0,
                    "meridianPurityGp" to 0
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrainingUiState()
        )

    /**
     * UI event handlers that delegate training actions to the GameStateManager.
     */
    fun onTrainStrength() {
        gameStateManager.trainStat(StatType.STR)
    }

    fun onTrainAgility() {
        gameStateManager.trainStat(StatType.AGI)
    }

    fun onTrainIntelligence() {
        gameStateManager.trainStat(StatType.INT)
    }

    fun onTrainVitality() {
        gameStateManager.trainStat(StatType.VIT)
    }

    // GP Training methods
    fun onPerformBodyForging() {
        // G90: GameStateManager integration - XP kazanımı
        gameStateManager.addExperience(15) // 15 XP kazanımı

        // G83 P4: EventLogger integration
        com.example.isekaikuroshin.utils.EventLogger.logTraining(
            type = "Body Forging",
            duration = 30,
            intensity = "Medium",
            details = "Performed body forging ritual to strengthen muscle fibers and bone density. This should improve my strength and vitality stats. (+15 XP)"
        )
    }

    fun onPerformBreathingRitual() {
        // G90: GameStateManager integration - XP kazanımı
        gameStateManager.addExperience(10) // 10 XP kazanımı

        // G83 P4: EventLogger integration
        com.example.isekaikuroshin.utils.EventLogger.logCultivation(
            type = "Breathing Ritual",
            duration = 20,
            details = "Performed ancient breathing techniques to enhance Qi control and spiritual energy. This should improve my intelligence and spirit stats. (+10 XP)"
        )
    }

    fun onPerformMeridianClearing() {
        // G90: GameStateManager integration - XP kazanımı
        gameStateManager.addExperience(12) // 12 XP kazanımı

        // G83 P4: EventLogger integration
        com.example.isekaikuroshin.utils.EventLogger.logCultivation(
            type = "Meridian Clearing",
            duration = 25,
            details = "Cleared meridian pathways to improve energy flow and mana affinity. This should enhance my mana regeneration and spiritual power. (+12 XP)"
        )
    }
}