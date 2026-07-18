
package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * Represents the relationship status and history with a single Non-Player Character (NPC).
 */
@Serializable
data class NPCRelationship(
    val npcId: String,
    val relationshipLevel: RelationshipLevel,
    val relationshipPoints: Int, // -100 (enemy) to +100 (beloved)
    val firstMeetDate: Long,
    val lastInteractionDate: Long,
    val interactionHistory: List<InteractionEvent>
)

/**
 * Defines the different tiers of a relationship with an NPC, including the dice roll modifier.
 */
@Serializable
enum class RelationshipLevel(val range: IntRange, val modifier: Float, val displayName: String) {
    ENEMY((-100..-51), -0.5f, "Düşman"),
    HOSTILE((-50..-21), -0.25f, "Düşmanca"),
    UNFRIENDLY((-20..-6), -0.1f, "Soğuk"),
    NEUTRAL((-5..5), 0.0f, "Nötr"),
    FRIENDLY((6..20), 0.1f, "Arkadaşça"),
    ALLIED((21..50), 0.25f, "Müttefik"),
    BELOVED((51..100), 0.5f, "Sevgili");

    companion object {
        fun fromPoints(points: Int): RelationshipLevel {
            return entries.find { points in it.range } ?: NEUTRAL
        }
    }
}

/**
 * Records a single interaction event with an NPC.
 */
@Serializable
data class InteractionEvent(
    val action: String,
    val outcome: InteractionOutcome,
    val relationshipChange: Int,
    val timestamp: Long
)

/**
 * Represents the outcome of a social interaction.
 */
@Serializable
enum class InteractionOutcome {
    CRITICAL_SUCCESS, SUCCESS, NEUTRAL, FAILURE, CRITICAL_FAILURE
}
