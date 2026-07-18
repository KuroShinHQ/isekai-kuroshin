package com.example.isekaikuroshin.data

import com.example.isekaikuroshin.engine.*

fun createTestData(): GameStateZ7 {
    return GameStateZ7(
        currentDay = 3,
        currentTimeOfDay = TimeOfDay.AFTERNOON,
        playerState = PlayerState(
            playerName = "Test Oyuncusu",
            level = 5,
            experience = 120,
            experienceToNextLevel = 500,
            currentHealth = 80,
            maxHealth = 150,
            currentMana = 60,
            maxMana = 100,
            strength = 15f,
            agility = 12f,
            intelligence = 18f,
            vitality = 14f,
            spirit = 16,
            luck = 9,
            moralityScore = 0.3f,
            statPoints = 2
        ),
        inventory = listOf(
            EquippedItemZ6(id = "sword_1", name = "Demir Kılıç", weight = 5.0f, rarity = "COMMON"),
            EquippedItemZ6(id = "potion_1", name = "Can İksiri", weight = 0.5f, rarity = "COMMON")
        ),
        storyPages = listOf(
            "Hikaye başlangıcı...",
            "Birinci gün geçti...",
            "İkinci gün geçti..."
        ),
        knownLocations = listOf(
            WorldLocations.FOREST_CAMP,
            WorldLocations.VILLAGE
        ),
        npcRelationships = mapOf(
            "cart_driver_001" to NPCRelationship(
                npcId = "cart_driver_001",
                relationshipLevel = RelationshipLevel.FRIENDLY,
                relationshipPoints = 25,
                firstMeetDate = System.currentTimeMillis(),
                lastInteractionDate = System.currentTimeMillis(),
                interactionHistory = listOf(
                    InteractionEvent("İlk karşılaşma", InteractionOutcome.SUCCESS, 10, System.currentTimeMillis()),
                    InteractionEvent("Yolculuk konuşması", InteractionOutcome.SUCCESS, 15, System.currentTimeMillis())
                )
            ),
            "mysterious_trader" to NPCRelationship(
                npcId = "mysterious_trader",
                relationshipLevel = RelationshipLevel.NEUTRAL,
                relationshipPoints = 0,
                firstMeetDate = System.currentTimeMillis(),
                lastInteractionDate = System.currentTimeMillis(),
                interactionHistory = listOf(
                    InteractionEvent("Esrarengiz tüccarla tanışma", InteractionOutcome.NEUTRAL, 0, System.currentTimeMillis())
                )
            ),
            "dark_knight" to NPCRelationship(
                npcId = "dark_knight",
                relationshipLevel = RelationshipLevel.HOSTILE,
                relationshipPoints = -40,
                firstMeetDate = System.currentTimeMillis(),
                lastInteractionDate = System.currentTimeMillis(),
                interactionHistory = listOf(
                    InteractionEvent("Düşmanca karşılaşma", InteractionOutcome.FAILURE, -20, System.currentTimeMillis()),
                    InteractionEvent("Çatışma", InteractionOutcome.CRITICAL_FAILURE, -20, System.currentTimeMillis())
                )
            ),
            "village_elder" to NPCRelationship(
                npcId = "village_elder",
                relationshipLevel = RelationshipLevel.ALLIED,
                relationshipPoints = 35,
                firstMeetDate = System.currentTimeMillis(),
                lastInteractionDate = System.currentTimeMillis(),
                interactionHistory = listOf(
                    InteractionEvent("Köy büyüğüyle tanışma", InteractionOutcome.SUCCESS, 15, System.currentTimeMillis()),
                    InteractionEvent("Bilgi alışverişi", InteractionOutcome.CRITICAL_SUCCESS, 20, System.currentTimeMillis())
                )
            ),
            "forest_witch" to NPCRelationship(
                npcId = "forest_witch",
                relationshipLevel = RelationshipLevel.UNFRIENDLY,
                relationshipPoints = -15,
                firstMeetDate = System.currentTimeMillis(),
                lastInteractionDate = System.currentTimeMillis(),
                interactionHistory = listOf(
                    InteractionEvent("Orman cadısıyla karşılaşma", InteractionOutcome.FAILURE, -15, System.currentTimeMillis())
                )
            )
        ),
        activeQuests = listOf(
            RegistryQuest(id = "quest_1", title = "Kayıp Koyun", description = "Köydeki kayıp koyunu bul.", giverNpcId = "npc_1")
        ),
        completedQuests = listOf(
            RegistryQuest(id = "quest_0", title = "İlk Adımlar", description = "Eğitimi tamamla.", giverNpcId = "npc_1")
        )
    )
}