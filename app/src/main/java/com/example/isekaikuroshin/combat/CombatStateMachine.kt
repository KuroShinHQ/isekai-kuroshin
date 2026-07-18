package com.example.isekaikuroshin.combat

import com.example.isekaikuroshin.data.Enemy
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * G93: Combat State Machine - Finite state machine for turn-based combat
 *
 * Prevents invalid state transitions and infinite loops.
 * State changes are explicit and logged for debugging.
 *
 * Valid transitions:
 * IDLE → COMBAT_START → PLAYER_TURN_SELECT → PLAYER_TURN_EXECUTE →
 * ENEMY_TURN_EXECUTE → PLAYER_TURN_SELECT (loop) → VICTORY/DEFEAT → IDLE
 */
sealed class CombatState {
    object Idle : CombatState()
    data class CombatStart(val enemies: List<Enemy>) : CombatState()
    data class PlayerTurnSelect(val enemies: List<Enemy>) : CombatState()
    data class PlayerTurnExecute(val action: CombatAction, val enemies: List<Enemy>) : CombatState()
    data class EnemyTurnExecute(val enemyId: String, val enemies: List<Enemy>) : CombatState()
    data class Victory(val rewards: CombatRewards) : CombatState()
    data class Defeat(val finalMessage: String) : CombatState()
    data class RunSuccess(val escapeMessage: String) : CombatState()

    fun getStateName(): String = when (this) {
        is Idle -> "IDLE"
        is CombatStart -> "COMBAT_START"
        is PlayerTurnSelect -> "PLAYER_TURN_SELECT"
        is PlayerTurnExecute -> "PLAYER_TURN_EXECUTE"
        is EnemyTurnExecute -> "ENEMY_TURN_EXECUTE"
        is Victory -> "VICTORY"
        is Defeat -> "DEFEAT"
        is RunSuccess -> "RUN_SUCCESS"
    }
}

sealed class CombatAction {
    data class UseLearnedSpell(val spellId: String, val targetId: String) : CombatAction()
    data class FreestyleAction(val userInput: String) : CombatAction()
    object Run : CombatAction()
}

sealed class CombatEvent {
    data class StartCombat(val enemies: List<Enemy>) : CombatEvent()
    object PlayerTurnReady : CombatEvent()
    data class PlayerActionSelected(val action: CombatAction) : CombatEvent()
    object PlayerActionComplete : CombatEvent()
    data class EnemyTurnStart(val enemyId: String) : CombatEvent()
    object EnemyActionComplete : CombatEvent()
    data class CombatWon(val rewards: CombatRewards) : CombatEvent()
    data class CombatLost(val message: String) : CombatEvent()
    data class PlayerEscaped(val message: String) : CombatEvent()
    object CombatEnded : CombatEvent()
}

data class CombatRewards(
    val xp: Int,
    val gold: Int,
    val items: List<String> = emptyList(),
    val titles: List<String> = emptyList()
)

class CombatStateMachine {
    private val _state = MutableStateFlow<CombatState>(CombatState.Idle)
    val state: StateFlow<CombatState> = _state.asStateFlow()

    /**
     * Transition to new state based on event
     * Throws IllegalStateException if transition is invalid
     */
    fun transition(event: CombatEvent) {
        val currentState = _state.value
        val newState = when (currentState) {
            // IDLE → COMBAT_START
            is CombatState.Idle -> {
                when (event) {
                    is CombatEvent.StartCombat -> CombatState.CombatStart(event.enemies)
                    else -> throwInvalidTransition(currentState, event)
                }
            }

            // COMBAT_START → PLAYER_TURN_SELECT
            is CombatState.CombatStart -> {
                when (event) {
                    is CombatEvent.PlayerTurnReady -> CombatState.PlayerTurnSelect(currentState.enemies)
                    else -> throwInvalidTransition(currentState, event)
                }
            }

            // PLAYER_TURN_SELECT → PLAYER_TURN_EXECUTE or RUN_SUCCESS
            is CombatState.PlayerTurnSelect -> {
                when (event) {
                    is CombatEvent.PlayerActionSelected -> CombatState.PlayerTurnExecute(event.action, currentState.enemies)
                    is CombatEvent.PlayerEscaped -> CombatState.RunSuccess(event.message)
                    else -> throwInvalidTransition(currentState, event)
                }
            }

            // PLAYER_TURN_EXECUTE → ENEMY_TURN_EXECUTE or VICTORY or RUN_SUCCESS
            is CombatState.PlayerTurnExecute -> {
                when (event) {
                    is CombatEvent.EnemyTurnStart -> CombatState.EnemyTurnExecute(event.enemyId, currentState.enemies)
                    is CombatEvent.CombatWon -> CombatState.Victory(event.rewards)
                    is CombatEvent.PlayerEscaped -> CombatState.RunSuccess(event.message) // FIX: Allow escape during execution
                    else -> throwInvalidTransition(currentState, event)
                }
            }

            // ENEMY_TURN_EXECUTE → PLAYER_TURN_SELECT or DEFEAT
            is CombatState.EnemyTurnExecute -> {
                when (event) {
                    is CombatEvent.PlayerTurnReady -> CombatState.PlayerTurnSelect(currentState.enemies)
                    is CombatEvent.CombatLost -> CombatState.Defeat(event.message)
                    else -> throwInvalidTransition(currentState, event)
                }
            }

            // VICTORY/DEFEAT/RUN_SUCCESS → IDLE
            is CombatState.Victory,
            is CombatState.Defeat,
            is CombatState.RunSuccess -> {
                when (event) {
                    is CombatEvent.CombatEnded -> CombatState.Idle
                    else -> throwInvalidTransition(currentState, event)
                }
            }
        }

        GameLogger.logSystem("⚔️ Combat State: ${currentState.getStateName()} → ${newState.getStateName()}")
        _state.value = newState
    }

    private fun throwInvalidTransition(state: CombatState, event: CombatEvent): Nothing {
        throw IllegalStateException(
            "❌ Invalid combat state transition: ${state.getStateName()} + ${event::class.simpleName}"
        )
    }

    /**
     * Check if currently in combat
     */
    fun isInCombat(): Boolean {
        return when (_state.value) {
            is CombatState.Idle,
            is CombatState.Victory,
            is CombatState.Defeat,
            is CombatState.RunSuccess -> false
            else -> true
        }
    }

    /**
     * Get current enemies (if in combat)
     */
    fun getCurrentEnemies(): List<Enemy> {
        return when (val state = _state.value) {
            is CombatState.CombatStart -> state.enemies
            is CombatState.PlayerTurnSelect -> state.enemies
            is CombatState.PlayerTurnExecute -> state.enemies
            is CombatState.EnemyTurnExecute -> state.enemies
            else -> emptyList()
        }
    }

    /**
     * Reset to idle (for debugging/testing)
     */
    fun reset() {
        GameLogger.logSystem("⚔️ Combat state machine reset to IDLE")
        _state.value = CombatState.Idle
    }
}
