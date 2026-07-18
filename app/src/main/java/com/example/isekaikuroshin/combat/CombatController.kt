package com.example.isekaikuroshin.combat

import com.example.isekaikuroshin.data.Enemy
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.StatusEffectManager
import com.example.isekaikuroshin.engine.SpellCombatHelper
import com.example.isekaikuroshin.engine.MagicEngine
import com.example.isekaikuroshin.game.GameStateManager as GameStateManagerZ7
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * G93: Combat Controller - Orchestrates combat flow using state machine
 *
 * Coordinates:
 * - State machine transitions
 * - GameStateManager updates (NO nested updates!)
 * - Combat calculations (damage, victory/defeat checks)
 * - Enemy AI (basic placeholder for G93, full AI in G95)
 *
 * IMPORTANT: All GameStateManager calls are SEPARATE, no nesting!
 */
@Singleton
class CombatController @Inject constructor(
    private val stateMachine: CombatStateMachine,
    private val gameStateManager: GameStateManager,
    private val spellCombatHelper: SpellCombatHelper,
    private val gameStateManagerZ7: GameStateManagerZ7 // TODO-G96: For immediate save on critical events
) {
    companion object {
        private const val TAG = "CombatController"
    }

    // G113.3: Status Effect Manager for stat modifiers
    private val statusEffectManager = StatusEffectManager()

    /**
     * Start combat with given enemies
     * G93 FIX: NO nested updates!
     * G96 FIX: Add gameState inCombat check to prevent infinite loop
     */
    suspend fun startCombat(enemyIds: List<String>) {
        // Guard 1: Already in combat (state machine)?
        if (stateMachine.isInCombat()) {
            GameLogger.logSystem("⚠️ Cannot start combat, already in combat state: ${stateMachine.state.value.getStateName()}")
            return
        }

        // Guard 2: Already in combat (game state)?
        if (gameStateManager.gameState.value.inCombat) {
            GameLogger.logSystem("⚠️ Cannot start combat, gameState.inCombat = true")
            return
        }

        GameLogger.logSystem("⚔️ Starting combat with ${enemyIds.size} enemies")

        // Create enemy objects
        // KURAL 9: Enemy names use ID as localization key (UI will resolve via LanguageManager)
        val enemies = enemyIds.map { id ->
            Enemy(
                id = id,
                name = "enemy_$id", // Localization key (e.g., "enemy_goblin_1")
                hp = 100,
                maxHp = 100,
                attack = 15,
                defense = 10
            )
        }

        // STEP 1: Transition state machine
        stateMachine.transition(CombatEvent.StartCombat(enemies))

        // STEP 2: Update GameState (SEPARATE update, NO nesting!)
        gameStateManager.updateCombatState(inCombat = true, enemies = enemies)

        // STEP 3: Add story page (SEPARATE update, NO nesting!)
        delay(100) // Small delay to ensure previous update completes
        gameStateManager.addStoryPage(
            buildCombatStartMessage(enemies)
        )

        // STEP 4: Transition to player turn
        delay(500) // Dramatic pause
        stateMachine.transition(CombatEvent.PlayerTurnReady)
    }

    /**
     * Player selects an action
     */
    suspend fun executePlayerAction(action: CombatAction) {
        val currentState = stateMachine.state.value
        if (currentState !is CombatState.PlayerTurnSelect) {
            throw IllegalStateException("Cannot execute player action in state: ${currentState.getStateName()}")
        }

        val enemies = currentState.enemies
        GameLogger.logSystem("⚔️ Player action: ${action::class.simpleName}")

        // Transition to execute state
        stateMachine.transition(CombatEvent.PlayerActionSelected(action))

        when (action) {
            is CombatAction.UseLearnedSpell -> {
                executeLearnedSpellAction(action, enemies)
            }
            is CombatAction.FreestyleAction -> {
                executeFreestyleAction(action, enemies)
            }
            is CombatAction.Run -> {
                executeRunAction(enemies)
            }
        }
    }

    /**
     * Execute learned spell action
     */
    private suspend fun executeLearnedSpellAction(
        action: CombatAction.UseLearnedSpell,
        enemies: List<Enemy>
    ) {
        // Find target enemy
        val targetEnemy = enemies.find { it.id == action.targetId }
        if (targetEnemy == null) {
            GameLogger.logSystem("⚠️ Target enemy not found: ${action.targetId}")
            // Skip to enemy turn
            proceedToEnemyTurn(enemies)
            return
        }

        // TODO-G93: Get performance score from camera gesture (placeholder for now)
        val performanceScore = 0.75f // Placeholder: 75% accuracy

        // TODO-G115.2: Get actual spell from LearnedSpellManager
        // For now: Use placeholder spell (Fireball)
        // KURAL 9: Spell names are localization keys
        val spell = MagicEngine.getDefaultSpells().first { it.name == "spell_fireball" } // Placeholder

        // G113.3: Apply status effect stat modifiers
        val currentState = gameStateManager.gameState.value
        val statModifiers = statusEffectManager.calculateStatModifiers(currentState.activeStatusEffects)
        val playerState = currentState.playerState

        // TODO-G115.2: Use MagicEngine to calculate spell damage with all modifiers
        val damage = MagicEngine.calculateSpellDamage(
            spell = spell,
            casterIntelligence = playerState.intelligence,
            casterSpirit = playerState.spirit,
            performanceScore = performanceScore,
            targetElement = null, // TODO-G115.3: Get enemy element affinity
            statusEffectBonuses = statModifiers
        )

        // Calculate mana cost
        val manaCost = MagicEngine.calculateManaCost(
            spell = spell,
            casterIntelligence = playerState.intelligence,
            statusEffectBonuses = statModifiers
        )

        GameLogger.logSystem("[G115.2] MagicEngine spell damage: ${spell.name} → $damage damage (mana: $manaCost)")
        if (statModifiers.isNotEmpty()) {
            GameLogger.logSystem("[G115.2] Stat modifiers: INT=${playerState.intelligence}, SPIRIT=${playerState.spirit}, performance=$performanceScore")
        }

        // G97: Consume stamina for spell casting
        gameStateManager.consumeCombatStamina(staminaCost = 15, fatigueGain = 2)
        GameLogger.logSystem("[G97] Spell cast stamina cost: -15 stamina, +2 fatigue")

        // Apply damage
        targetEnemy.hp = maxOf(0, targetEnemy.hp - damage)
        GameLogger.logSystem("⚔️ Spell hit ${targetEnemy.name} for $damage damage! HP: ${targetEnemy.hp}/${targetEnemy.maxHp}")

        // Update enemies in GameState
        gameStateManager.updateCombatState(inCombat = true, enemies = enemies)

        // Add combat log
        delay(100)
        gameStateManager.addStoryPage(
            "⚡ Your spell hits ${targetEnemy.name} for $damage damage!\n${targetEnemy.name}: ${targetEnemy.hp}/${targetEnemy.maxHp} HP"
        )

        // Check victory
        if (checkVictory(enemies)) {
            return // Victory transition already handled
        }

        // Proceed to enemy turn
        proceedToEnemyTurn(enemies)
    }

    /**
     * Execute freestyle action (AI-based)
     */
    private suspend fun executeFreestyleAction(
        action: CombatAction.FreestyleAction,
        enemies: List<Enemy>
    ) {
        GameLogger.logSystem("⚔️ Freestyle action: ${action.userInput}")

        // G97: Consume stamina for freestyle action
        gameStateManager.consumeCombatStamina(staminaCost = 10, fatigueGain = 3)
        GameLogger.logSystem("[G97] Freestyle action stamina cost: -10 stamina, +3 fatigue")

        // TODO-G99: Apply status effect stat modifiers to freestyle damage
        val currentState = gameStateManager.gameState.value
        val statModifiers = statusEffectManager.calculateStatModifiers(currentState.activeStatusEffects)
        val strengthBonus = statModifiers["strength"] ?: 0

        // TODO-G94: Call GameMasterEngine to process freestyle action
        // For now: Simple placeholder logic with stat modifiers
        val baseDamage = Random.nextInt(15, 35) // Random damage 15-35
        val damageBonus = (strengthBonus / 10).coerceAtLeast(0)
        val damage = baseDamage + damageBonus
        val targetEnemy = enemies.first() // Attack first enemy

        if (statModifiers.isNotEmpty()) {
            GameLogger.logSystem("[G99] Freestyle stat modifiers - STR: +$strengthBonus → Damage: $baseDamage + $damageBonus = $damage")
        }

        targetEnemy.hp = maxOf(0, targetEnemy.hp - damage)
        GameLogger.logSystem("⚔️ Attack hits ${targetEnemy.name} for $damage damage!")

        // Update state
        gameStateManager.updateCombatState(inCombat = true, enemies = enemies)

        // Add log
        delay(100)
        gameStateManager.addStoryPage(
            "⚔️ ${action.userInput}\nYou deal $damage damage to ${targetEnemy.name}!\n${targetEnemy.name}: ${targetEnemy.hp}/${targetEnemy.maxHp} HP"
        )

        // Check victory
        if (checkVictory(enemies)) {
            return
        }

        // Proceed to enemy turn
        proceedToEnemyTurn(enemies)
    }

    /**
     * Execute run action
     */
    private suspend fun executeRunAction(enemies: List<Enemy>) {
        // G97: Consume stamina for running
        gameStateManager.consumeCombatStamina(staminaCost = 5, fatigueGain = 1)
        GameLogger.logSystem("[G97] Run attempt stamina cost: -5 stamina, +1 fatigue")

        // Run away chance: 50% base + (player speed - avg enemy speed) * 5%
        val runChance = 0.5f + Random.nextFloat() * 0.3f // 50-80% chance
        val success = Random.nextFloat() < runChance

        if (success) {
            GameLogger.logSystem("✅ Run away successful!")
            val message = "🏃 You successfully escaped from combat!"

            // Update state
            gameStateManager.updateCombatState(inCombat = false, enemies = emptyList())

            // Add log
            delay(100)
            gameStateManager.addStoryPage(message)

            // Transition to run success
            stateMachine.transition(CombatEvent.PlayerEscaped(message))

            // End combat
            delay(1000)
            stateMachine.transition(CombatEvent.CombatEnded)
        } else {
            GameLogger.logSystem("❌ Run away failed!")
            gameStateManager.addStoryPage("🏃 You tried to run but the enemies blocked your escape!")

            // Failed run = enemy gets free attack
            proceedToEnemyTurn(enemies)
        }
    }

    /**
     * Enemy turn (basic AI for G93, full AI in G95)
     */
    private suspend fun proceedToEnemyTurn(enemies: List<Enemy>) {
        val aliveEnemies = enemies.filter { it.hp > 0 }
        if (aliveEnemies.isEmpty()) {
            // All enemies dead (shouldn't happen here, but safety check)
            handleVictory(enemies)
            return
        }

        // Simple enemy AI: First alive enemy attacks
        val attackingEnemy = aliveEnemies.first()
        stateMachine.transition(CombatEvent.EnemyTurnStart(attackingEnemy.id))

        delay(1000) // Dramatic pause

        // TODO-G99: Apply status effect defense modifiers
        val currentState = gameStateManager.gameState.value
        val statModifiers = statusEffectManager.calculateStatModifiers(currentState.activeStatusEffects)
        val vitalityBonus = statModifiers["vitality"] ?: 0
        val defenseBonus = (vitalityBonus / 10).coerceAtLeast(0)

        // Enemy attacks player
        val damage = attackingEnemy.attack + Random.nextInt(-5, 5) // Attack ±5 variance
        val playerStats = gameStateManager.getPlayerStats()
        val totalDefense = playerStats.defense + defenseBonus
        val actualDamage = maxOf(1, damage - totalDefense) // Min 1 damage

        if (statModifiers.isNotEmpty()) {
            GameLogger.logSystem("[G99] Defense modifiers - VIT: +$vitalityBonus → Defense: ${playerStats.defense} + $defenseBonus = $totalDefense")
        }

        GameLogger.logSystem("⚔️ ${attackingEnemy.name} attacks for $actualDamage damage!")

        // Apply damage to player
        val newPlayerHp = maxOf(0, playerStats.hp - actualDamage)
        gameStateManager.updatePlayerHp(newPlayerHp)

        // Add log
        delay(100)
        gameStateManager.addStoryPage(
            "⚔️ ${attackingEnemy.name} attacks you for $actualDamage damage!\nYour HP: $newPlayerHp/${playerStats.maxHp}"
        )

        // G97: Decrease hunger every enemy turn
        gameStateManager.decreaseHunger(1)
        GameLogger.logSystem("[G97] Combat hunger decreased by 1")

        // Check defeat
        if (newPlayerHp <= 0) {
            handleDefeat()
            return
        }

        // Return to player turn
        delay(1000)
        stateMachine.transition(CombatEvent.PlayerTurnReady)
    }

    /**
     * Check if all enemies are defeated
     */
    private suspend fun checkVictory(enemies: List<Enemy>): Boolean {
        val aliveEnemies = enemies.filter { it.hp > 0 }
        if (aliveEnemies.isEmpty()) {
            handleVictory(enemies)
            return true
        }
        return false
    }

    /**
     * Handle victory
     * G96: Immediate save after critical event
     * G62: Combat history tracking
     */
    private suspend fun handleVictory(enemies: List<Enemy>) {
        GameLogger.logSystem("✅ Combat victory!")

        // Calculate rewards
        val xp = enemies.size * 50 // 50 XP per enemy
        val gold = enemies.size * 20 // 20 gold per enemy
        val rewards = CombatRewards(xp = xp, gold = gold)

        // Update state
        gameStateManager.updateCombatState(inCombat = false, enemies = emptyList())

        // Add victory message
        delay(100)
        gameStateManager.addStoryPage(
            buildVictoryMessage(rewards)
        )

        // Apply rewards
        gameStateManager.addExperience(xp)
        gameStateManager.updateGold(gold)

        // G62: Record combat victory in history
        gameStateManager.recordCombatVictory(
            enemiesKilled = enemies.size,
            xpGained = xp,
            goldGained = gold
        )

        // G96: IMMEDIATE SAVE - Victory is a critical event!
        gameStateManagerZ7.saveGameStateImmediately()
        GameLogger.logSystem("💾 [G96] Victory - Game state saved immediately")

        // Transition to victory
        stateMachine.transition(CombatEvent.CombatWon(rewards))

        // End combat after delay
        delay(2000)
        stateMachine.transition(CombatEvent.CombatEnded)
    }

    /**
     * Handle defeat
     * G96: Immediate save after critical event
     * G63.2: Death mechanic with penalties
     */
    private suspend fun handleDefeat() {
        GameLogger.logSystem("❌ Combat defeat!")
        val message = "💀 You have been defeated!\nYou lose consciousness..."

        // Add defeat message
        delay(100)
        gameStateManager.addStoryPage(message)

        // Transition to defeat
        stateMachine.transition(CombatEvent.CombatLost(message))

        // G63.2: Handle death with penalties (XP loss, gold loss, respawn)
        val (xpLost, goldLost) = gameStateManager.handleDeath("death_cause_combat") // KURAL 9: Localization key

        // G62: Record combat defeat in history
        gameStateManager.recordCombatDefeat(xpLost = xpLost, goldLost = goldLost)

        // G96: IMMEDIATE SAVE - Defeat is a critical event!
        gameStateManagerZ7.saveGameStateImmediately()
        GameLogger.logSystem("💾 [G96] Defeat - Game state saved immediately")

        // End combat
        delay(2000)
        stateMachine.transition(CombatEvent.CombatEnded)
    }

    /**
     * Build combat start message
     */
    private fun buildCombatStartMessage(enemies: List<Enemy>): String {
        return buildString {
            append("⚔️ COMBAT STARTED!\n\n")
            enemies.forEachIndexed { index, enemy ->
                append("${index + 1}. ${enemy.name} (HP: ${enemy.hp}/${enemy.maxHp})\n")
            }
            append("\nPrepare for battle!")
        }
    }

    /**
     * Build victory message
     */
    private fun buildVictoryMessage(rewards: CombatRewards): String {
        return buildString {
            append("⭐ VICTORY! ⭐\n\n")
            append("You have defeated all enemies!\n\n")
            append("Rewards:\n")
            append("• +${rewards.xp} XP\n")
            append("• +${rewards.gold} Gold\n")
            if (rewards.items.isNotEmpty()) {
                append("\nItems found:\n")
                rewards.items.forEach { item ->
                    append("• $item\n")
                }
            }
        }
    }
}
