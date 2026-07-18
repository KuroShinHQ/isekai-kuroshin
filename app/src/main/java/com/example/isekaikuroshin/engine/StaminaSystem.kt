package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PlayerState // PlayerState importu doğru
import kotlin.math.max
import kotlin.math.min

class StaminaSystem(private val gameStateManager: GameStateManager) {

    /**
     * Decreases player stamina and increases fatigue based on an action.
     * This function should be called when a player performs an action that consumes energy.
     *
     * @param staminaCost The amount of stamina to decrease.
     * @param fatigueGain The amount of fatigue to gain.
     */
    fun performAction(staminaCost: Int, fatigueGain: Int) { // Parametreler Int yapıldı
        val currentState = gameStateManager.gameState.value
        val currentStats = currentState.playerState

        // Long sabitler Int yapıldı (0L -> 0, 100L -> 100)
        // currentStats.stamina ve currentStats.fatigue zaten Int.
        // staminaCost ve fatigueGain artık Int.
        // Sonuçlar (newStamina, newFatigue) Int olacak.
        val newStamina = max(0, currentStats.stamina - staminaCost)
        val newFatigue = min(100, currentStats.fatigue + fatigueGain)

        val updatedStats = currentStats.copy(
            stamina = newStamina, // Int = Int, sorun yok
            fatigue = newFatigue  // Int = Int, sorun yok
        )
        gameStateManager.updatePlayerState(updatedStats)
    }

    /**
     * Recovers player stamina and reduces fatigue, typically through rest.
     *
     * @param staminaRecovery The amount of stamina to recover.
     * @param fatigueReduction The amount of fatigue to reduce.
     */
    fun rest(staminaRecovery: Int, fatigueReduction: Int) { // Parametreler Int yapıldı
        val currentState = gameStateManager.gameState.value
        val currentStats = currentState.playerState

        val newStamina = min(100, currentStats.stamina + staminaRecovery)
        val newFatigue = max(0, currentStats.fatigue - fatigueReduction)

        val updatedStats = currentStats.copy(
            stamina = newStamina,
            fatigue = newFatigue
        )
        gameStateManager.updatePlayerState(updatedStats)
    }

    /**
     * Increases hunger over time. This might be called by TimeSystem.
     *
     * @param hungerIncrease The amount of hunger to increase.
     */
    fun increaseHunger(hungerIncrease: Int) { // Parametre Int yapıldı
        val currentState = gameStateManager.gameState.value
        val currentStats = currentState.playerState

        val newHunger = min(100, currentStats.hunger + hungerIncrease)

        val updatedStats = currentStats.copy(
            hunger = newHunger // Int = Int, sorun yok
        )
        gameStateManager.updatePlayerState(updatedStats)
    }

    /**
     * TODO-G112.5: Fatigue Penalties - Düşük stamina ve yüksek fatigue stat debuff verir
     * @return Stat modifier (0.0-1.0 arası, 1.0 = normal, 0.5 = %50 debuff)
     */
    fun getFatigueMultiplier(): Float {
        val currentStats = gameStateManager.gameState.value.playerState
        val stamina = currentStats.stamina
        val fatigue = currentStats.fatigue

        // Stamina 30'un altındaysa penalty
        val staminaPenalty = if (stamina < 30) {
            0.7f + (stamina / 100f * 0.3f) // 0 stamina = 0.7x, 30 stamina = 1.0x
        } else {
            1.0f // Normal
        }

        // Fatigue 70'in üstündeyse penalty
        val fatiguePenalty = if (fatigue > 70) {
            1.0f - ((fatigue - 70) / 100f * 0.3f) // 70 fatigue = 1.0x, 100 fatigue = 0.7x
        } else {
            1.0f // Normal
        }

        // En kötü durumda 0.5x (min cap)
        return (staminaPenalty * fatiguePenalty).coerceAtLeast(0.5f)
    }

    /**
     * TODO-G112.5: Fatigue durumunu açıklayan metin döndürür
     */
    fun getFatigueStatus(): String {
        val multiplier = getFatigueMultiplier()
        return when {
            multiplier >= 0.95f -> "Dinç" // Normal
            multiplier >= 0.85f -> "Hafif Yorgun (-10% stat)" // Slight debuff
            multiplier >= 0.70f -> "Yorgun (-20% stat)" // Moderate debuff
            else -> "Bitkin (-30% stat)" // Heavy debuff
        }
    }

    // TODO: Add functions for consuming items to restore stamina/reduce hunger/fatigue
}