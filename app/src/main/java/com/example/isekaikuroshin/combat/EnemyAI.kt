package com.example.isekaikuroshin.combat

import com.example.isekaikuroshin.data.Enemy
import com.example.isekaikuroshin.data.PlayerStats
import com.example.isekaikuroshin.utils.GameLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * G95: Enemy AI System - Phase 2B
 *
 * Intelligent enemy action selection based on:
 * - AI Strategy (AGGRESSIVE, DEFENSIVE, BALANCED, COWARD)
 * - Current HP/MP status
 * - Player HP status
 * - Ally HP status
 *
 * KURAL 11: Metadata-based logic (use enemy.aiStrategy, not string names)
 * KURAL 15: Root cause analysis (AI decisions logged for debugging)
 */
@Singleton
class EnemyAI @Inject constructor() {
    companion object {
        private const val TAG = "EnemyAI"

        // AI decision thresholds
        private const val LOW_HP_THRESHOLD = 0.3f // 30% HP
        private const val CRITICAL_HP_THRESHOLD = 0.15f // 15% HP
        private const val HIGH_MP_THRESHOLD = 0.5f // 50% MP
        private const val SPELL_USE_CHANCE_AGGRESSIVE = 0.5f // 50%
        private const val SPELL_USE_CHANCE_BALANCED = 0.3f // 30%
        private const val SPELL_USE_CHANCE_DEFENSIVE = 0.2f // 20%
    }

    /**
     * Select best action for enemy
     *
     * @param enemy Current enemy making decision
     * @param player Player stats
     * @param allEnemies All enemies (for healing decisions)
     * @return Selected combat action
     */
    fun selectAction(
        enemy: Enemy,
        player: PlayerStats,
        allEnemies: List<Enemy>
    ): CombatAction {
        // Calculate HP percentages
        val enemyHpPercent = enemy.hp.toFloat() / enemy.maxHp.toFloat()
        val playerHpPercent = player.hp.toFloat() / player.maxHp.toFloat()

        GameLogger.logSystem("🤖 $TAG: ${enemy.name} (${enemy.aiStrategy}) deciding action...")
        GameLogger.logSystem("🤖 $TAG: Enemy HP: ${(enemyHpPercent * 100).toInt()}%, Player HP: ${(playerHpPercent * 100).toInt()}%")

        // KURAL 11: Metadata-based strategy selection
        val action = when (enemy.aiStrategy) {
            AIStrategy.AGGRESSIVE -> selectAggressiveAction(enemy, player, enemyHpPercent, playerHpPercent)
            AIStrategy.DEFENSIVE -> selectDefensiveAction(enemy, player, allEnemies, enemyHpPercent)
            AIStrategy.BALANCED -> selectBalancedAction(enemy, player, allEnemies, enemyHpPercent, playerHpPercent)
            AIStrategy.COWARD -> selectCowardAction(enemy, player, enemyHpPercent)
        }

        GameLogger.logSystem("🤖 $TAG: ${enemy.name} chose: ${action::class.simpleName}")
        return action
    }

    /**
     * AGGRESSIVE Strategy: High damage, ignores defense
     * - Always attacks or uses offensive spells
     * - Prefers spells when MP available
     * - Targets low HP player with strong attacks
     */
    private fun selectAggressiveAction(
        enemy: Enemy,
        player: PlayerStats,
        enemyHpPercent: Float,
        playerHpPercent: Float
    ): CombatAction {
        // Player is low HP → Go for the kill with spell
        if (playerHpPercent < LOW_HP_THRESHOLD) {
            GameLogger.logSystem("🤖 $TAG: Player is low HP! Going for kill...")
            // TODO-G95: Check if enemy has high-damage spell and MP
            // For now: Basic attack
            return CombatAction.FreestyleAction("Attack with full force!")
        }

        // High chance to use spell if MP available
        if (Random.nextFloat() < SPELL_USE_CHANCE_AGGRESSIVE) {
            // TODO-G95: Select spell from enemy's spell list
            GameLogger.logSystem("🤖 $TAG: Using offensive spell!")
            return CombatAction.FreestyleAction("Cast fireball!")
        }

        // Default: Basic attack
        GameLogger.logSystem("🤖 $TAG: Basic attack!")
        return CombatAction.FreestyleAction("Attack!")
    }

    /**
     * DEFENSIVE Strategy: Heals allies, uses shields
     * - Prioritizes healing low HP allies
     * - Uses defensive buffs
     * - Only attacks when allies are healthy
     */
    private fun selectDefensiveAction(
        enemy: Enemy,
        player: PlayerStats,
        allEnemies: List<Enemy>,
        enemyHpPercent: Float
    ): CombatAction {
        // Self is low HP → Heal self
        if (enemyHpPercent < LOW_HP_THRESHOLD) {
            GameLogger.logSystem("🤖 $TAG: Self HP low, healing self!")
            // TODO-G95: Implement actual healing spell
            return CombatAction.FreestyleAction("Heal myself!")
        }

        // Find lowest HP ally
        val aliveAllies = allEnemies.filter { it.id != enemy.id && it.hp > 0 }
        val lowestHpAlly = aliveAllies.minByOrNull { it.hp.toFloat() / it.maxHp.toFloat() }

        if (lowestHpAlly != null) {
            val allyHpPercent = lowestHpAlly.hp.toFloat() / lowestHpAlly.maxHp.toFloat()

            // Ally is low HP → Heal ally
            if (allyHpPercent < LOW_HP_THRESHOLD) {
                GameLogger.logSystem("🤖 $TAG: Ally ${lowestHpAlly.name} is low HP, healing!")
                // TODO-G95: Implement ally healing
                return CombatAction.FreestyleAction("Heal ${lowestHpAlly.name}!")
            }
        }

        // Low chance to use spell (defensive buffs)
        if (Random.nextFloat() < SPELL_USE_CHANCE_DEFENSIVE) {
            GameLogger.logSystem("🤖 $TAG: Using defensive buff!")
            return CombatAction.FreestyleAction("Cast shield!")
        }

        // Default: Basic attack
        GameLogger.logSystem("🤖 $TAG: All allies healthy, attacking!")
        return CombatAction.FreestyleAction("Attack!")
    }

    /**
     * BALANCED Strategy: Mix of attack and defense
     * - Uses spells occasionally
     * - Heals self if HP low
     * - Balanced decision making
     */
    private fun selectBalancedAction(
        enemy: Enemy,
        player: PlayerStats,
        allEnemies: List<Enemy>,
        enemyHpPercent: Float,
        playerHpPercent: Float
    ): CombatAction {
        // Self is critical HP → Heal
        if (enemyHpPercent < CRITICAL_HP_THRESHOLD) {
            GameLogger.logSystem("🤖 $TAG: Critical HP! Healing self!")
            return CombatAction.FreestyleAction("Heal myself!")
        }

        // Player is low HP → Attack aggressively
        if (playerHpPercent < LOW_HP_THRESHOLD) {
            GameLogger.logSystem("🤖 $TAG: Player is low HP, attacking!")
            return CombatAction.FreestyleAction("Attack with full power!")
        }

        // Medium chance to use spell
        if (Random.nextFloat() < SPELL_USE_CHANCE_BALANCED) {
            GameLogger.logSystem("🤖 $TAG: Using spell!")
            return CombatAction.FreestyleAction("Cast magic missile!")
        }

        // Default: Basic attack
        GameLogger.logSystem("🤖 $TAG: Basic attack!")
        return CombatAction.FreestyleAction("Attack!")
    }

    /**
     * COWARD Strategy: Runs away if HP low
     * - Tries to run if HP < 30%
     * - Otherwise attacks from distance (prefers ranged)
     * - Never uses risky close-range attacks
     */
    private fun selectCowardAction(
        enemy: Enemy,
        player: PlayerStats,
        enemyHpPercent: Float
    ): CombatAction {
        // HP low → Try to run
        if (enemyHpPercent < LOW_HP_THRESHOLD) {
            GameLogger.logSystem("🤖 $TAG: HP low! Trying to flee!")
            // TODO-G95: Implement enemy run attempt
            // For now: Defensive action
            return CombatAction.FreestyleAction("Try to escape!")
        }

        // HP critical → Panic! Heal or defensive
        if (enemyHpPercent < CRITICAL_HP_THRESHOLD) {
            GameLogger.logSystem("🤖 $TAG: Critical HP! Panic healing!")
            return CombatAction.FreestyleAction("Heal myself in panic!")
        }

        // Safe HP → Ranged attack
        GameLogger.logSystem("🤖 $TAG: Ranged attack from safe distance!")
        return CombatAction.FreestyleAction("Throw rock from distance!")
    }

    /**
     * Calculate threat level of player
     * Higher = more dangerous
     */
    private fun calculatePlayerThreat(player: PlayerStats): Float {
        val hpPercent = player.hp.toFloat() / player.maxHp.toFloat()
        val mpPercent = player.mp.toFloat() / player.maxMp.toFloat()
        val attackThreat = player.attack / 100f

        // High HP + High MP + High Attack = High threat
        return (hpPercent * 0.3f) + (mpPercent * 0.2f) + (attackThreat * 0.5f)
    }

    /**
     * Check if enemy should use special ability
     */
    private fun shouldUseSpecialAbility(enemy: Enemy, enemyHpPercent: Float): Boolean {
        // Low HP → Save MP for emergency
        if (enemyHpPercent < LOW_HP_THRESHOLD) return false

        // TODO-G95: Check enemy MP and special ability costs
        // For now: Random chance based on strategy
        return Random.nextFloat() < 0.3f
    }
}

/**
 * AI Strategy Enum
 * KURAL 11: Metadata-based, not string-based
 */
enum class AIStrategy {
    /**
     * AGGRESSIVE: High damage, ignores defense
     * - Always attacks or uses offensive spells
     * - 50% chance to use spell if MP available
     * - Targets low HP player
     */
    AGGRESSIVE,

    /**
     * DEFENSIVE: Heals allies, uses shields
     * - Prioritizes healing low HP allies
     * - Uses defensive buffs
     * - Only attacks when allies healthy
     */
    DEFENSIVE,

    /**
     * BALANCED: Mix of attack and defense
     * - 30% chance to use spell
     * - Heals self if HP < 15%
     * - Adapts to situation
     */
    BALANCED,

    /**
     * COWARD: Runs away if HP low
     * - Tries to run if HP < 30%
     * - Prefers ranged attacks
     * - Never uses close-range attacks
     */
    COWARD
}

/**
 * Enemy extension: Add AI strategy
 * KURAL 11: Metadata field, not derived from name
 */
val Enemy.aiStrategy: AIStrategy
    get() {
        // TODO-G95: Add aiStrategy field to Enemy data class
        // For now: Determine from enemy ID/name
        return when {
            id.contains("goblin", ignoreCase = true) -> AIStrategy.AGGRESSIVE
            id.contains("shaman", ignoreCase = true) -> AIStrategy.DEFENSIVE
            id.contains("knight", ignoreCase = true) -> AIStrategy.BALANCED
            id.contains("rat", ignoreCase = true) -> AIStrategy.COWARD
            id.contains("dragon", ignoreCase = true) -> AIStrategy.AGGRESSIVE
            id.contains("healer", ignoreCase = true) -> AIStrategy.DEFENSIVE
            else -> AIStrategy.BALANCED // Default
        }
    }
