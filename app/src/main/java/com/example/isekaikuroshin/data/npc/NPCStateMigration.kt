package com.example.isekaikuroshin.data.npc

import com.example.isekaikuroshin.data.NPCRelationship
import com.example.isekaikuroshin.data.InteractionEvent
import com.example.isekaikuroshin.data.InteractionOutcome
import com.example.isekaikuroshin.data.database.DynamicNPCEntity
import com.example.isekaikuroshin.data.database.toDynamicNPCState
import com.example.isekaikuroshin.data.database.toEntity

/**
 * Migration and Bridge utilities for converting between legacy NPCRelationship
 * and new DynamicNPCState systems for the Nemesis System.
 */
object NPCStateMigration {

    /**
     * Convert legacy NPCRelationship to new DynamicNPCState
     * This function initializes a DynamicNPCState from existing relationship data
     */
    fun migrateFromNPCRelationship(
        npcId: String,
        baseRegistryId: String,
        npcRelationship: NPCRelationship
    ): DynamicNPCState {
        // Convert InteractionEvents to NPCMemoryEvents
        val memoryEvents = npcRelationship.interactionHistory.map { interactionEvent ->
            NPCMemoryEvent(
                timestamp = interactionEvent.timestamp,
                eventType = "INTERACTION",
                eventDescription = interactionEvent.action,
                emotionalImpact = calculateEmotionalImpact(interactionEvent.relationshipChange),
                locationId = "", // Legacy data doesn't have location info
                playerAction = interactionEvent.action,
                outcome = mapOutcomeToDescription(interactionEvent.outcome),
                action = interactionEvent.action
            )
        }

        // Determine personality traits based on relationship history
        val personalityTraits = derivePersonalityTraits(npcRelationship)

        // Calculate loyalty from relationship points
        val loyaltyToPlayer = npcRelationship.relationshipPoints

        // Calculate mood from current relationship level
        val currentMood = NPCMood.fromLoyalty(loyaltyToPlayer)

        return DynamicNPCState(
            npcId = npcId,
            baseRegistryId = baseRegistryId,
            level = 1, // Default starting level
            currentStrength = 10, // Default stats
            currentAgility = 10,
            currentIntelligence = 10,
            currentVitality = 10,
            personalityTraits = personalityTraits,
            currentMood = currentMood,
            loyaltyToPlayer = loyaltyToPlayer,
            historyWithPlayer = memoryEvents,
            interactionCount = npcRelationship.interactionHistory.size,
            lastSeenDate = npcRelationship.lastInteractionDate,
            lastLocationId = "",
            npcRelationships = emptyMap(), // Will be populated later
            nemesisLevel = 0, // Start as normal NPC
            vendettaReasons = emptyList(),
            adaptationTraits = emptyList(),
            currentLocationId = "",
            isAlive = true,
            isDead = false,
            isExiled = false,
            lastUpdateTime = System.currentTimeMillis(),
            evolutionPoints = 0,
            timesSeen = npcRelationship.interactionHistory.size,
            timesDefeated = 0, // Unknown from legacy data
            timesHelped = countHelpfulInteractions(npcRelationship.interactionHistory)
        )
    }

    /**
     * Convert DynamicNPCState back to NPCRelationship for compatibility
     */
    fun convertToNPCRelationship(dynamicState: DynamicNPCState): NPCRelationship {
        // Convert NPCMemoryEvents back to InteractionEvents
        // TODO-FIX-04: NPCMemoryEvent alanları nullable, null kontrolleri ekle
        val interactionHistory = dynamicState.historyWithPlayer.mapNotNull { memoryEvent ->
            val action = memoryEvent.action ?: return@mapNotNull null
            val outcome = memoryEvent.outcome ?: return@mapNotNull null
            val timestamp = memoryEvent.timestamp ?: return@mapNotNull null

            InteractionEvent(
                action = action,
                outcome = mapDescriptionToOutcome(outcome),
                relationshipChange = 20, // Default value since emotionalImpact is now String
                timestamp = timestamp
            )
        }

        return NPCRelationship(
            npcId = dynamicState.npcId,
            relationshipLevel = com.example.isekaikuroshin.data.RelationshipLevel.fromPoints(dynamicState.loyaltyToPlayer),
            relationshipPoints = dynamicState.loyaltyToPlayer,
            firstMeetDate = dynamicState.historyWithPlayer.mapNotNull { it.timestamp }.minOrNull() ?: System.currentTimeMillis(),
            lastInteractionDate = dynamicState.lastSeenDate,
            interactionHistory = interactionHistory
        )
    }

    /**
     * Mass migration function for converting all existing NPCRelationships
     */
    suspend fun migrateAllNPCRelationships(
        npcRelationships: Map<String, NPCRelationship>,
        getRegistryIdForNpc: (String) -> String // Function to get base registry ID
    ): List<DynamicNPCEntity> {
        return npcRelationships.map { (npcId, relationship) ->
            val baseRegistryId = getRegistryIdForNpc(npcId)
            migrateFromNPCRelationship(npcId, baseRegistryId, relationship).toEntity()
        }
    }

    // Helper functions

    private fun mapOutcomeToDescription(outcome: InteractionOutcome): String {
        return when (outcome) {
            InteractionOutcome.CRITICAL_SUCCESS -> "Mükemmel bir sonuç"
            InteractionOutcome.SUCCESS -> "Başarılı"
            InteractionOutcome.NEUTRAL -> "Nötr sonuç"
            InteractionOutcome.FAILURE -> "Başarısız"
            InteractionOutcome.CRITICAL_FAILURE -> "Felaket"
        }
    }

    private fun mapDescriptionToOutcome(description: String): InteractionOutcome {
        return when {
            description.contains("Mükemmel") || description.contains("mükemmel") -> InteractionOutcome.CRITICAL_SUCCESS
            description.contains("Başarılı") || description.contains("başarılı") -> InteractionOutcome.SUCCESS
            description.contains("Felaket") || description.contains("felaket") -> InteractionOutcome.CRITICAL_FAILURE
            description.contains("Başarısız") || description.contains("başarısız") -> InteractionOutcome.FAILURE
            else -> InteractionOutcome.NEUTRAL
        }
    }

    private fun calculateEmotionalImpact(relationshipChange: Int): String {
        // Convert relationship change to emotional impact string
        return when {
            relationshipChange <= -15 -> "MAJOR_NEGATIVE"
            relationshipChange <= -5 -> "NEGATIVE"
            relationshipChange >= 15 -> "MAJOR_POSITIVE"
            relationshipChange >= 5 -> "POSITIVE"
            else -> "NEUTRAL"
        }
    }

    private fun derivePersonalityTraits(npcRelationship: NPCRelationship): List<String> {
        val traits = mutableListOf<String>()

        val relationshipPoints = npcRelationship.relationshipPoints
        val interactionCount = npcRelationship.interactionHistory.size

        // Derive traits based on relationship pattern
        when {
            relationshipPoints <= -50 -> traits.add("hostile")
            relationshipPoints <= -20 -> traits.add("suspicious")
            relationshipPoints >= 50 -> traits.add("loyal")
            relationshipPoints >= 20 -> traits.add("friendly")
        }

        // Add traits based on interaction frequency
        when {
            interactionCount > 10 -> traits.add("social")
            interactionCount < 3 -> traits.add("reclusive")
        }

        // Analyze interaction patterns
        val recentInteractions = npcRelationship.interactionHistory.takeLast(5)
        val positiveInteractions = recentInteractions.count { it.relationshipChange > 0 }
        val negativeInteractions = recentInteractions.count { it.relationshipChange < 0 }

        when {
            negativeInteractions > positiveInteractions -> traits.add("grudge_holder")
            positiveInteractions > negativeInteractions -> traits.add("forgiving")
        }

        return traits.distinct()
    }

    private fun countHelpfulInteractions(history: List<InteractionEvent>): Int {
        return history.count {
            it.relationshipChange > 5 &&
            (it.outcome == InteractionOutcome.SUCCESS || it.outcome == InteractionOutcome.CRITICAL_SUCCESS)
        }
    }

    /**
     * Helper function to check if an NPC should be promoted to Nemesis status
     * based on their relationship history
     */
    fun shouldPromoteToNemesis(dynamicState: DynamicNPCState): Boolean {
        return dynamicState.loyaltyToPlayer <= -60 && // Very hostile
               dynamicState.timesDefeated >= 2 && // Defeated multiple times
               dynamicState.timesSeen >= 3 && // Has meaningful history
               dynamicState.nemesisLevel == 0 // Not already a nemesis
    }

    /**
     * Initialize a DynamicNPCState for a completely new NPC (no legacy data)
     */
    fun createNewDynamicNPCState(
        npcId: String,
        baseRegistryId: String,
        currentLocationId: String = ""
    ): DynamicNPCState {
        return DynamicNPCState(
            npcId = npcId,
            baseRegistryId = baseRegistryId,
            currentLocationId = currentLocationId
        )
    }
}