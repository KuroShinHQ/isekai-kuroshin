package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PlayerState
import com.example.isekaikuroshin.data.UmbrosContract // DEĞİŞTİRİLDİ
import com.example.isekaikuroshin.utils.GameLogger
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Umbros Pact Management System
 *
 * Handles the dark pact system where players can accept penalties to continue playing
 * after death. This manager is now stateless and operates directly on the GameStateManager.
 */
class UmbrosPactManager(private val gameStateManager: GameStateManager) {

    /**
     * Represents the terms of an Umbros pact with escalating penalties.
     */
    data class PactTerms(
        val hpReduction: Float,
        val statPenalty: Int,
        val experienceLoss: Int,
        val curseDuration: Int,
        val acceptanceCount: Int
    )

    /**
     * Generates pact terms based on how many times the player has died.
     * @param deathCount Number of previous deaths/pacts.
     * @return PactTerms with escalating penalties.
     */
    fun generatePactTerms(deathCount: Int): PactTerms {
        return PactTerms(
            hpReduction = 0.1f + (deathCount * 0.05f),      // Each death adds 5% more reduction
            statPenalty = 2 + deathCount,                   // Each death adds 1 more stat penalty
            experienceLoss = 1000 + (deathCount * 500),     // Each death adds 500 more XP loss
            curseDuration = 5 + deathCount,                 // Each death adds 1 more curse turn
            acceptanceCount = deathCount
        )
    }

    /**
     * Applies pact penalties directly to the game state via the GameStateManager.
     * @param pactTerms The pact terms to apply.
     */
    fun applyPactPenalties(pactTerms: PactTerms) {
        val currentState = gameStateManager.gameState.value
        val currentPlayerState = currentState.playerState

        // 1. Apply Stat Penalties
        val penalizedPlayerState = currentPlayerState.copy(
            strength = max(1f, currentPlayerState.strength - pactTerms.statPenalty),
            agility = max(1f, currentPlayerState.agility - pactTerms.statPenalty),
            intelligence = max(1f, currentPlayerState.intelligence - pactTerms.statPenalty),
            vitality = max(1f, currentPlayerState.vitality - pactTerms.statPenalty)
        )

        // 2. Recalculate derived stats (like max HP/MP) AFTER stat penalties
        var finalPlayerState = recalculateDerivedStats(penalizedPlayerState)

        // 3. Apply HP Reduction based on the NEW max HP
        val newMaxHp = (finalPlayerState.maxHealth * (1 - pactTerms.hpReduction)).roundToInt()
        finalPlayerState = finalPlayerState.copy(
            maxHealth = newMaxHp,
            currentHealth = newMaxHp // Heal to the new, lower max HP
        )

        // 4. Update the player state in the manager
        gameStateManager.updatePlayerState(finalPlayerState)

        // 5. Apply Experience Loss
        gameStateManager.addExperience(-pactTerms.experienceLoss)

        // 6. Set the Umbros Contract
        val newContract = UmbrosContract(
            isActive = true,
            remainingCurse = pactTerms.curseDuration,
            totalContracts = pactTerms.acceptanceCount + 1,
            penaltyMultiplier = 1 + (pactTerms.acceptanceCount * 0.1f)
        )
        gameStateManager.updateUmbrosContract(newContract)

        GameLogger.logSystem("Umbros'un Laneti kabul edildi. Ağır bir bedel ödendi.")
    }

    /**
     * Recalculates derived stats based on primary attributes.
     * This is a helper to ensure consistency after stat changes.
     */
    private fun recalculateDerivedStats(playerState: PlayerState): PlayerState {
        val newMaxHealth = (playerState.vitality * 10).roundToInt()
        val newMaxMana = (playerState.intelligence * 5).roundToInt()
        val newPhysicalAttack = (playerState.strength * 2).roundToInt()
        val newMagicPower = (playerState.intelligence * 1.5).roundToInt()
        val newDefense = (playerState.vitality * 1.2).roundToInt()

        return playerState.copy(
            maxHealth = newMaxHealth,
            maxMana = newMaxMana,
            physicalAttack = newPhysicalAttack,
            magicPower = newMagicPower,
            defense = newDefense
        )
    }

    /**
     * Reduces curse duration by one (called after each journal entry).
     */
    fun decrementCurse() {
        val contract = gameStateManager.gameState.value.umbrosContract
        if (contract?.isActive == true && contract.remainingCurse > 0) {
            val newRemainingCurse = contract.remainingCurse - 1
            val newContract = contract.copy(
                remainingCurse = newRemainingCurse,
                isActive = newRemainingCurse > 0
            )
            gameStateManager.updateUmbrosContract(newContract)
            if (newRemainingCurse == 0) {
                GameLogger.logSystem("Umbros'un Laneti kalktı!")
            }
        }
    }

    /**
     * Gets the current curse status as a displayable string.
     * @return Human-readable curse status.
     */
    fun getCurseStatusText(): String {
        val contract = gameStateManager.gameState.value.umbrosContract
        return if (contract?.isActive == true) {
            "Umbros'un Laneti: ${contract.remainingCurse} giriş kaldı"
        } else {
            ""
        }
    }
}