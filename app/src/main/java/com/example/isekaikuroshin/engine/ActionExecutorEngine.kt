package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.ai.GameAction
import com.example.isekaikuroshin.combat.CombatController
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.RegistryQuest
import com.example.isekaikuroshin.utils.GameLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionExecutorEngine @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val combatController: CombatController // G93: Combat system integration
) {

    // G107: Error rate tracking
    private var totalActionsExecuted = 0
    private var totalActionsFailed = 0

    suspend fun executeActions(actions: List<GameAction>) {
        if (actions.isEmpty()) {
            GameLogger.logSystem("ActionExecutorEngine: No actions to execute")
            return
        }

        GameLogger.logSystem("ActionExecutorEngine: Executing ${actions.size} action(s)")
        val startTime = System.currentTimeMillis() // G107: Performance tracking

        var successCount = 0
        var failCount = 0

        actions.forEach { action ->
            val success = executeAction(action)
            if (success) successCount++ else failCount++
        }

        val duration = System.currentTimeMillis() - startTime
        totalActionsExecuted += successCount
        totalActionsFailed += failCount

        // G107: Enhanced logging with metrics
        GameLogger.logSystem("ActionExecutorEngine: Batch complete - Success: $successCount, Failed: $failCount, Duration: ${duration}ms")
        GameLogger.logSystem("ActionExecutorEngine: Total stats - Executed: $totalActionsExecuted, Failed: $totalActionsFailed (${if (totalActionsExecuted > 0) (totalActionsFailed * 100 / (totalActionsExecuted + totalActionsFailed)) else 0}% failure rate)")
    }

    private suspend fun executeAction(action: GameAction): Boolean {
        return try {
            GameLogger.logSystem("ActionExecutorEngine: Executing action: ${action.actionType}")

            when (action.actionType) {
                "UPDATE_HEALTH" -> {
                    val changeAmount = action.parameters["changeAmount"]?.toIntOrNull()
                    if (changeAmount != null) {
                        gameStateManager.modifyHealth(changeAmount)
                        GameLogger.logSystem("Health updated by $changeAmount")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid changeAmount for UPDATE_HEALTH: ${action.parameters["changeAmount"]}")
                    }
                }

                "UPDATE_GOLD" -> {
                    val changeAmount = action.parameters["changeAmount"]?.toIntOrNull()
                    if (changeAmount != null) {
                        gameStateManager.updateGold(changeAmount)
                        GameLogger.logSystem("Gold updated by $changeAmount")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid changeAmount for UPDATE_GOLD: ${action.parameters["changeAmount"]}")
                    }
                }

                "UPDATE_MANA" -> {
                    val changeAmount = action.parameters["changeAmount"]?.toIntOrNull()
                    if (changeAmount != null) {
                        gameStateManager.modifyMana(changeAmount)
                        GameLogger.logSystem("Mana updated by $changeAmount")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid changeAmount for UPDATE_MANA: ${action.parameters["changeAmount"]}")
                    }
                }

                "ADD_EXPERIENCE" -> {
                    val amount = action.parameters["amount"]?.toIntOrNull()
                    if (amount != null) {
                        gameStateManager.addExperience(amount)
                        GameLogger.logSystem("Experience added: $amount")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid amount for ADD_EXPERIENCE: ${action.parameters["amount"]}")
                    }
                }

                "ADD_ITEM_TO_INVENTORY" -> {
                    val itemId = action.parameters["itemId"]
                    val quantity = action.parameters["quantity"]?.toIntOrNull()
                    if (itemId != null && quantity != null) {
                        gameStateManager.addItemToInventory(itemId, quantity)
                        GameLogger.logSystem("Item added to inventory: $itemId x$quantity")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid parameters for ADD_ITEM_TO_INVENTORY: itemId=$itemId, quantity=${action.parameters["quantity"]}")
                    }
                }

                "REMOVE_ITEM_FROM_INVENTORY" -> {
                    val itemId = action.parameters["itemId"]
                    val quantity = action.parameters["quantity"]?.toIntOrNull()
                    if (itemId != null && quantity != null) {
                        gameStateManager.removeItemFromInventory(itemId, quantity)
                        GameLogger.logSystem("Item removed from inventory: $itemId x$quantity")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid parameters for REMOVE_ITEM_FROM_INVENTORY: itemId=$itemId, quantity=${action.parameters["quantity"]}")
                    }
                }

                "EQUIP_ITEM" -> {
                    val itemId = action.parameters["itemId"]
                    val slot = action.parameters["slot"]
                    if (itemId != null && slot != null) {
                        gameStateManager.equipItem(itemId, slot)
                        GameLogger.logSystem("Item equipped: $itemId in slot $slot")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid parameters for EQUIP_ITEM: itemId=$itemId, slot=$slot")
                    }
                }

                "ADD_QUEST" -> {
                    val questId = action.parameters["questId"]
                    val title = action.parameters["title"]
                    val description = action.parameters["description"]

                    if (questId != null && title != null && description != null) {
                        val quest = RegistryQuest(
                            id = questId,
                            title = title,
                            description = description,
                            giverNpcId = action.parameters["giverNpcId"] ?: "AI",
                            rewards = emptyMap()
                        )
                        gameStateManager.addQuest(quest)
                        GameLogger.logSystem("Quest added: $title")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid parameters for ADD_QUEST: questId=$questId, title=$title, description=$description")
                    }
                }

                "COMPLETE_QUEST" -> {
                    val questId = action.parameters["questId"]
                    if (questId != null) {
                        gameStateManager.completeQuest(questId)
                        GameLogger.logSystem("Quest completed: $questId")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid questId for COMPLETE_QUEST: ${action.parameters["questId"]}")
                    }
                }

                "FAIL_QUEST" -> {
                    val questId = action.parameters["questId"]
                    if (questId != null) {
                        gameStateManager.failQuest(questId)
                        GameLogger.logSystem("Quest failed: $questId")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid questId for FAIL_QUEST: ${action.parameters["questId"]}")
                    }
                }

                "START_COMBAT" -> {
                    // G93: Use CombatController instead of GameStateManager.startCombat()
                    val enemyIds = action.parameters["enemyIds"]?.split(",")?.map { it.trim() }
                    if (!enemyIds.isNullOrEmpty()) {
                        GameLogger.logSystem("⚔️ START_COMBAT action received - Enemy IDs: ${enemyIds.joinToString(", ")}")
                        combatController.startCombat(enemyIds)
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "❌ Invalid enemyIds for START_COMBAT: ${action.parameters["enemyIds"]}")
                    }
                }

                "END_COMBAT" -> {
                    gameStateManager.endCombat()
                    GameLogger.logSystem("Combat ended")
                }

                "UPDATE_ENEMY_HEALTH" -> {
                    val enemyId = action.parameters["enemyId"]
                    val damage = action.parameters["damage"]?.toIntOrNull()
                    if (enemyId != null && damage != null) {
                        gameStateManager.updateEnemyHealth(enemyId, damage)
                        GameLogger.logSystem("Enemy health updated: $enemyId took $damage damage")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid parameters for UPDATE_ENEMY_HEALTH: enemyId=$enemyId, damage=${action.parameters["damage"]}")
                    }
                }

                "UPDATE_NPC_RELATIONSHIP" -> {
                    val npcId = action.parameters["npcId"]
                    val changeAmount = action.parameters["changeAmount"]?.toIntOrNull()
                    if (npcId != null && changeAmount != null) {
                        gameStateManager.updateNpcRelationship(npcId, changeAmount)
                        GameLogger.logSystem("NPC relationship updated: $npcId by $changeAmount")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid parameters for UPDATE_NPC_RELATIONSHIP: npcId=$npcId, changeAmount=${action.parameters["changeAmount"]}")
                    }
                }

                "DISCOVER_LOCATION" -> {
                    val locationId = action.parameters["locationId"]
                    if (locationId != null) {
                        gameStateManager.discoverLocation(locationId)
                        GameLogger.logSystem("Location discovered: $locationId")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid locationId for DISCOVER_LOCATION: ${action.parameters["locationId"]}")
                    }
                }

                "CHANGE_LOCATION" -> {
                    val locationId = action.parameters["locationId"]
                    if (locationId != null) {
                        gameStateManager.changeLocation(locationId)
                        GameLogger.logSystem("Location changed to: $locationId")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid locationId for CHANGE_LOCATION: ${action.parameters["locationId"]}")
                    }
                }

                "UPDATE_MORALITY" -> {
                    val changeAmount = action.parameters["changeAmount"]?.toFloatOrNull()
                    if (changeAmount != null) {
                        gameStateManager.updateMoralityScore(changeAmount)
                        GameLogger.logSystem("Morality updated by $changeAmount")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid changeAmount for UPDATE_MORALITY: ${action.parameters["changeAmount"]}")
                    }
                }

                "INCREMENT_THREAT_COUNTER" -> {
                    val amount = action.parameters["amount"]?.toIntOrNull()
                    if (amount != null) {
                        gameStateManager.incrementThreatCounter(amount)
                        GameLogger.logSystem("Threat counter incremented by $amount")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid amount for INCREMENT_THREAT_COUNTER: ${action.parameters["amount"]}")
                    }
                }

                "RESET_THREAT_COUNTER" -> {
                    gameStateManager.resetThreatCounter()
                    GameLogger.logSystem("Threat counter reset")
                }

                "ADVANCE_TIME" -> {
                    val newDay = gameStateManager.advanceTime()
                    GameLogger.logSystem("Time advanced" + if (newDay) " - New day!" else "")
                }

                "ADD_STORY_PAGE" -> {
                    val content = action.parameters["content"]
                    if (content != null) {
                        gameStateManager.addStoryPage(content)
                        GameLogger.logSystem("Story page added")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid content for ADD_STORY_PAGE: ${action.parameters["content"]}")
                    }
                }

                // ========================================
                // HİBRİT ROZET/UNVAN/BAŞARIM SİSTEMİ ACTIONs
                // ========================================

                "GRANT_BADGE" -> {
                    val badgeId = action.parameters["badgeId"]
                    if (badgeId != null) {
                        gameStateManager.grantBadge(badgeId)
                        GameLogger.logSystem("Badge granted: $badgeId")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid badgeId for GRANT_BADGE: ${action.parameters["badgeId"]}")
                    }
                }

                "REVOKE_BADGE" -> {
                    val badgeId = action.parameters["badgeId"]
                    if (badgeId != null) {
                        gameStateManager.revokeBadge(badgeId)
                        GameLogger.logSystem("Badge revoked: $badgeId")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid badgeId for REVOKE_BADGE: ${action.parameters["badgeId"]}")
                    }
                }

                "EQUIP_TITLE" -> {
                    val titleId = action.parameters["titleId"]
                    if (titleId != null) {
                        gameStateManager.equipTitle(titleId)
                        GameLogger.logSystem("Title equipped: $titleId")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid titleId for EQUIP_TITLE: ${action.parameters["titleId"]}")
                    }
                }

                "UNEQUIP_TITLE" -> {
                    gameStateManager.unequipTitle()
                    GameLogger.logSystem("Title unequipped")
                }

                "DICE_ROLL" -> {
                    // TODO-G111.2: DiceSystem Integration - Gerçek zar atma mekaniği
                    val actionType = action.parameters["actionType"] ?: "GENERIC"
                    val difficulty = action.parameters["difficulty"]?.toIntOrNull() ?: 12
                    val advantage = action.parameters["advantage"]?.toBooleanStrictOrNull() ?: false
                    val disadvantage = action.parameters["disadvantage"]?.toBooleanStrictOrNull() ?: false

                    // Stat-based skill check
                    val playerStats = gameStateManager.gameState.value.playerState
                    val skillModifier = when (actionType) {
                        "AGI_CHECK" -> (playerStats.agility / 10).toInt()
                        "INT_CHECK" -> (playerStats.intelligence / 10).toInt()
                        "STR_CHECK" -> (playerStats.strength / 10).toInt()
                        "PER_CHECK" -> 0 // PER stat henüz PlayerState'de yok, default 0
                        "CHA_CHECK" -> 0 // CHA stat henüz yok, default 0
                        "LUCK_CHECK" -> playerStats.luck / 10
                        else -> 0
                    }

                    // DiceSystem ile gerçek zar atma
                    val difficultyEnum = when (difficulty) {
                        in 0..5 -> DiceSystem.Difficulty.VERY_EASY
                        in 6..8 -> DiceSystem.Difficulty.EASY
                        in 9..12 -> DiceSystem.Difficulty.MEDIUM
                        in 13..15 -> DiceSystem.Difficulty.HARD
                        in 16..18 -> DiceSystem.Difficulty.VERY_HARD
                        else -> DiceSystem.Difficulty.LEGENDARY
                    }

                    val diceResult = DiceSystem.skillCheck(
                        skillModifier = skillModifier,
                        difficulty = difficultyEnum,
                        advantage = advantage,
                        disadvantage = disadvantage
                    )

                    GameLogger.logSystem("🎲 DICE ROLL: $actionType (DC $difficulty) = ${diceResult.total} → ${if (diceResult.success) "✅ SUCCESS" else "❌ FAIL"}")
                    GameLogger.logSystem("  - Stat modifier: +$skillModifier, Roll: ${diceResult.results}, Total: ${diceResult.total}")
                }

                "APPLY_STATUS_EFFECT" -> {
                    // G113.1: Apply status effect (buff/debuff) to player
                    val effectId = action.parameters["effectId"]
                    val magnitude = action.parameters["magnitude"]?.toIntOrNull()
                    val duration = action.parameters["duration"]?.toIntOrNull()

                    if (effectId != null) {
                        gameStateManager.applyStatusEffect(effectId, magnitude, duration)
                        GameLogger.logSystem("Status effect applied: $effectId (magnitude: $magnitude, duration: $duration)")
                    } else {
                        GameLogger.logError("ActionExecutorEngine", "Invalid effectId for APPLY_STATUS_EFFECT: ${action.parameters["effectId"]}")
                    }
                }

                "REST" -> {
                    // TODO-G112.6: Rest Mechanics - Oyuncu dinlenir, stamina/fatigue restore olur
                    val hours = action.parameters["hours"]?.toIntOrNull() ?: 1
                    gameStateManager.performRest(hours)
                    GameLogger.logSystem("Player rested for $hours hour(s)")
                }

                "NO_ACTION" -> {
                    GameLogger.logSystem("No action taken (NO_ACTION)")
                }

                else -> {
                    GameLogger.logError("ActionExecutorEngine", "Unknown actionType: ${action.actionType}")
                    return false // G107: Unknown action = failure
                }
            }
            true // G107: Action executed successfully
        } catch (e: Exception) {
            GameLogger.logError("ActionExecutorEngine", "Error executing action ${action.actionType}", e)
            false // G107: Exception = failure
        }
    }
}