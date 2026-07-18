package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PlayerState
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Death Detection and Management System
 *
 * Manages player death detection, death events, and triggers appropriate death sequences.
 * It is now integrated with the GameStateManager to reset the game upon death.
 */
class DeathManager(
    private val gameStateManager: GameStateManager,
    private val scope: CoroutineScope
) {

    private val _deathEvent = MutableSharedFlow<DeathEvent>()
    val deathEvent: SharedFlow<DeathEvent> = _deathEvent.asSharedFlow()

    /**
     * Checks if the player has died based on their state.
     * If dead, it triggers the full death and game reset sequence.
     * @param playerState Current player state.
     * @return true if player has died, false otherwise.
     */
    fun checkForDeath(playerState: PlayerState): Boolean {
        if (playerState.currentHealth <= 0) {
            GameLogger.logSystem("Player has died. Health is ${playerState.currentHealth}.") // DEĞİŞTİRİLDİ
            triggerDeathSequence(playerState)
            return true
        }
        return false
    }

    /**
     * Triggers the death sequence, emits the death event, and resets the game.
     * @param playerState Final player state at time of death.
     */
    private fun triggerDeathSequence(playerState: PlayerState) {
        val deathCause = determineCauseOfDeath() // playerState argument removed
        val event = DeathEvent(
            timestamp = System.currentTimeMillis(),
            playerLevel = playerState.level,
            causeOfDeath = deathCause,
            finalPlayerState = playerState.copy() // Create a snapshot
        )

        // Emit death event to observers (like the UI) and then reset the game.
        if (_deathEvent.tryEmit(event)) {
            GameLogger.logSystem("Death event emitted for ${deathCause.description}. Resetting game state.")
            // After the event is sent, reset the game via the GameStateManager
            scope.launch {
                gameStateManager.resetGame()
            }
        } else {
            GameLogger.logSystem("UYARI: Failed to emit death event. Game reset will still proceed.")
            scope.launch {
                gameStateManager.resetGame()
            }
        }
    }

    /**
     * Determines the cause of death.
     * TODO-YS-03: Re-implement a more detailed cause-of-death system when stats like hunger/fatigue are added to PlayerState.
     * @return The determined cause of death.
     */
    private fun determineCauseOfDeath(): DeathCause { // state parameter removed
        // Currently, any death is considered a combat death.
        // This can be expanded later.
        return DeathCause.COMBAT
    }
}

/**
 * Represents a death event with all relevant information using the unified PlayerState.
 */
data class DeathEvent(
    val timestamp: Long,
    val playerLevel: Int,
    val causeOfDeath: DeathCause,
    val finalPlayerState: PlayerState
)

/**
 * Enum representing different causes of death.
 * The video paths can be used by the UI to show different death animations.
 * G63.3: Expanded death causes
 * KURAL 9: description uses localization keys
 */
enum class DeathCause(val videoPath: String, val description: String) {
    COMBAT("raw/death_transition_video.mp4", "death_cause_combat"), // KURAL 9: Localization key
    STARVATION("raw/death_transition_video.mp4", "death_cause_starvation"), // G63.3: Hunger = 0
    FATIGUE("raw/death_transition_video.mp4", "death_cause_fatigue"), // G63.3: Fatigue = 100
    UNKNOWN("raw/death_transition_video.mp4", "death_cause_unknown")
}