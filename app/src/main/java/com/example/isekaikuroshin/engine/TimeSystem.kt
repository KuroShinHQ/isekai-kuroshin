package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PlayerState // DEĞİŞTİRİLDİ (PlayerStatsZ3 -> PlayerState)
import com.example.isekaikuroshin.data.TimeOfDay // TimeOfDay importu zaten data paketinden olmalı, kontrol edildi.
import com.example.isekaikuroshin.data.StatusEffectManager
import com.example.isekaikuroshin.utils.GameLogger
// StatType importu engine paketinden (com.example.isekaikuroshin.engine.StatType)

class TimeSystem(
    private val gameStateManager: GameStateManager,
    private val statusEffectManager: StatusEffectManager = StatusEffectManager()
) {
    /**
     * Advances the game time based on the number of actions taken.
     * Each 3 actions advance the time by one TimeOfDay segment.
     *
     * @param actions The number of actions performed by the player.
     */
    fun advanceTime(actions: Int) {
        // G102: Passive stat decrease per action
        // Each action causes passive stamina/hunger/fatigue changes
        repeat(actions) {
            gameStateManager.consumeStamina(staminaCost = 2)
            gameStateManager.decreaseHunger(amount = 1)
            gameStateManager.increaseFatigue(amount = 1)
        }
        GameLogger.logSystem("[G102] TimeSystem passive decrease: $actions actions processed")

        val timeSegmentsToAdvance = actions / 3 // Example: 3 actions = 1 time segment

        repeat(timeSegmentsToAdvance) {
            gameStateManager.advanceTime() // dayChanged variable assignment removed
            applyTimeOfDayEffects()

            // G113.4: Process status effects each time segment
            processStatusEffects()
        }
    }

    /**
     * G113.4: Process status effects (DoT, expiration, etc.)
     * Called every time segment advance
     */
    private fun processStatusEffects() {
        val currentState = gameStateManager.gameState.value
        val currentTurn = currentState.currentDay // Using day as turn counter
        val activeEffects = currentState.activeStatusEffects

        // Process turn effects (DoT damage, expiration check)
        val (updatedEffects, dotDamage) = statusEffectManager.processTurnEffects(
            currentTurn = currentTurn,
            activeEffects = activeEffects
        )

        // Update game state with new effect list
        if (updatedEffects != activeEffects) {
            gameStateManager.updateStatusEffects(updatedEffects)
            GameLogger.logSystem("TimeSystem: Status effects updated (${updatedEffects.size} active)")
        }

        // Apply DoT damage if any
        if (dotDamage != 0) {
            gameStateManager.modifyHealth(-dotDamage)
            GameLogger.logSystem("TimeSystem: DoT damage applied ($dotDamage total)")
        }
    }

    /**
     * Applies stat effects based on the current TimeOfDay.
     * Reads current player state from GameStateManager, updates them, and writes back.
     */
    private fun applyTimeOfDayEffects() {
        val currentState = gameStateManager.gameState.value
        val currentTimeOfDay = currentState.currentTimeOfDay
        val currentStats = currentState.playerState // DEĞİŞTİRİLDİ (playerStats -> playerState)

        var updatedStats = currentStats

        // PlayerState alan adları ve türleri dikkate alınarak güncellendi
        // TimeOfDay.statEffects içindeki StatType engine paketinden geliyor varsayımıyla devam ediyorum.
        for ((statType, multiplier) in currentTimeOfDay.statEffects) {
            updatedStats = when (statType) {
                // PlayerState.currentHealth ve PlayerState.currentMana Int türünde
                com.example.isekaikuroshin.engine.StatType.HP -> updatedStats.copy(currentHealth = (updatedStats.currentHealth * multiplier).toInt().coerceAtLeast(0))
                com.example.isekaikuroshin.engine.StatType.MP -> updatedStats.copy(currentMana = (updatedStats.currentMana * multiplier).toInt().coerceAtLeast(0))
                
                // PlayerState.stamina, hunger, fatigue, spirit, luck Int türünde
                com.example.isekaikuroshin.engine.StatType.STAMINA -> updatedStats.copy(stamina = (updatedStats.stamina * multiplier).toInt().coerceIn(0, 100))
                com.example.isekaikuroshin.engine.StatType.HUNGER -> updatedStats.copy(hunger = (updatedStats.hunger * multiplier).toInt().coerceIn(0, 100))
                com.example.isekaikuroshin.engine.StatType.FATIGUE -> updatedStats.copy(fatigue = (updatedStats.fatigue * multiplier).toInt().coerceIn(0, 100))
                com.example.isekaikuroshin.engine.StatType.SPIRIT -> updatedStats.copy(spirit = (updatedStats.spirit * multiplier).toInt().coerceAtLeast(1)) // Varsayılan min 1
                com.example.isekaikuroshin.engine.StatType.LUCK -> updatedStats.copy(luck = (updatedStats.luck * multiplier).toInt().coerceAtLeast(1))   // Varsayılan min 1
                com.example.isekaikuroshin.engine.StatType.PER -> updatedStats // EKLENDİ (PlayerState'de karşılığı yoksa şimdilik etki yok)
                com.example.isekaikuroshin.engine.StatType.WIS -> updatedStats // EKLENDİ (Kadim Mühür için - PlayerState'de karşılığı yoksa şimdilik etki yok)
                com.example.isekaikuroshin.engine.StatType.END -> updatedStats // EKLENDİ (Kadim Mühür için - PlayerState'de karşılığı yoksa şimdilik etki yok)


                // PlayerState.strength, agility, intelligence, vitality Float türünde
                com.example.isekaikuroshin.engine.StatType.STR -> updatedStats.copy(strength = (updatedStats.strength * multiplier).coerceAtLeast(1.0f))
                com.example.isekaikuroshin.engine.StatType.AGI -> updatedStats.copy(agility = (updatedStats.agility * multiplier).coerceAtLeast(1.0f))
                com.example.isekaikuroshin.engine.StatType.INT -> updatedStats.copy(intelligence = (updatedStats.intelligence * multiplier).coerceAtLeast(1.0f))
                com.example.isekaikuroshin.engine.StatType.VIT -> updatedStats.copy(vitality = (updatedStats.vitality * multiplier).coerceAtLeast(1.0f))
                
                // Yüzdelik ve düz değerler TimeSystem'de bu blokta doğrudan kullanılmıyor, 
                // ancak when kapsamlı olmalı. Gerçek etkileri başka bir sistemde (varsa) uygulanır.
                com.example.isekaikuroshin.engine.StatType.STR_PERCENT,
                com.example.isekaikuroshin.engine.StatType.STR_FLAT,
                com.example.isekaikuroshin.engine.StatType.AGI_PERCENT,
                com.example.isekaikuroshin.engine.StatType.AGI_FLAT,
                com.example.isekaikuroshin.engine.StatType.INT_PERCENT,
                com.example.isekaikuroshin.engine.StatType.INT_FLAT,
                com.example.isekaikuroshin.engine.StatType.VIT_PERCENT,
                com.example.isekaikuroshin.engine.StatType.VIT_FLAT,
                com.example.isekaikuroshin.engine.StatType.SPIRIT_PERCENT,
                com.example.isekaikuroshin.engine.StatType.SPIRIT_FLAT,
                com.example.isekaikuroshin.engine.StatType.LUCK_PERCENT,
                com.example.isekaikuroshin.engine.StatType.LUCK_FLAT,
                com.example.isekaikuroshin.engine.StatType.PER_PERCENT, // EKLENDİ
                com.example.isekaikuroshin.engine.StatType.PER_FLAT -> updatedStats // EKLENDİ (Bu türler için bu blokta değişiklik yok)
                else -> updatedStats // Bilinmeyen stat type'lar için değişiklik yok
            }
        }
        if (updatedStats != currentStats) { // Sadece değişiklik varsa güncelle
            gameStateManager.updatePlayerState(updatedStats) // DEĞİŞTİRİLDİ (updatePlayerStats -> updatePlayerState)
        }
    }

    // TODO: Add functions for triggering time-based events, hunger/thirst/fatigue increase
    // These will likely interact with other systems (e.g., StaminaSystem)
}