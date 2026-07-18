
package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.NPCRelationship

// Basic NPC data structure as requested
data class NPC(val id: String, val name: String, val baseDifficulty: Float = 0.5f)

data class SocialActionInfo(
    val npcId: String,
    val actionType: SocialActionType,
    val difficulty: Float
)

enum class SocialActionType(val primaryStat: String, val baseDifficulty: Float) {
    PERSUASION("int", 0.6f),
    TRADE("spirit", 0.4f),
    INTIMIDATION("str", 0.7f),
    ASSISTANCE("vit", 0.3f),
    CONVERSATION("spirit", 0.2f)
}

/**
 * Analyzes user text to detect social actions directed at NPCs.
 * This is a placeholder implementation; NLP integration will be done later.
 */
class SocialActionProcessor {

    /**
     * Detects a social action from a user's input string.
     * @param userAction The text input from the user (e.g., from the journal).
     * @param knownNPCs A list of NPCs the player knows.
     * @return A SocialActionInfo object if a valid action and NPC are detected, otherwise null.
     */
    fun detectSocialAction(userAction: String, knownNPCs: List<NPC>): SocialActionInfo? {
        val actionText = userAction.lowercase()

        // First, find which NPC is being targeted
        val targetNPC = knownNPCs.find { npc -> actionText.contains(npc.name.lowercase()) }
        
        targetNPC?.let { npc ->
            // Second, determine the type of social action
            val actionType = determineSocialActionType(actionText)
            if (actionType != null) {
                // Combine base difficulties for a final difficulty score
                val finalDifficulty = (actionType.baseDifficulty + npc.baseDifficulty) / 2
                return SocialActionInfo(
                    npcId = npc.id,
                    actionType = actionType,
                    difficulty = finalDifficulty
                )
            }
        }
        
        return null // No valid action or NPC found
    }

    /**
     * Determines the social action type based on keywords in the text.
     */
    private fun determineSocialActionType(actionText: String): SocialActionType? {
        return when {
            actionText.contains("ikna et") || actionText.contains("kandır") -> SocialActionType.PERSUASION
            actionText.contains("ticaret yap") || actionText.contains("satın al") || actionText.contains("sat") -> SocialActionType.TRADE
            actionText.contains("korkut") || actionText.contains("tehdit et") -> SocialActionType.INTIMIDATION
            actionText.contains("yardım et") || actionText.contains("destek ol") -> SocialActionType.ASSISTANCE
            actionText.contains("konuş") || actionText.contains("sohbet et") -> SocialActionType.CONVERSATION
            else -> null
        }
    }
}
