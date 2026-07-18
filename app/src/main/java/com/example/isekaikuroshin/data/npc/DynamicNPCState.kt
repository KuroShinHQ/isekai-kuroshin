package com.example.isekaikuroshin.data.npc

import kotlinx.serialization.Serializable

/**
 * Dynamic NPC State - The core data model for evolving NPCs in the Nemesis System
 *
 * This represents the current dynamic state of an NPC that changes over time
 * and in response to player actions, separate from the static RegistryNPC template.
 */
@Serializable
data class DynamicNPCState(
    val npcId: String,
    val baseRegistryId: String, // Reference to RegistryNPC for base info

    // ============ DYNAMIC STATS ============
    val level: Int = 1,
    val currentStrength: Int = 10,
    val currentAgility: Int = 10,
    val currentIntelligence: Int = 10,
    val currentVitality: Int = 10,

    // ============ DYNAMIC PERSONALITY ============
    val personalityTraits: List<String> = emptyList(), // ["vengeful", "tactical", "cowardly"]
    val currentMood: NPCMood = NPCMood.NEUTRAL,
    val loyaltyToPlayer: Int = 0, // -100 (enemy) to +100 (devoted ally)

    // ============ HISTORY & MEMORY ============
    val historyWithPlayer: List<NPCMemoryEvent> = emptyList(),
    val interactionCount: Int = 0,
    val lastSeenDate: Long = 0,
    val lastLocationId: String = "",

    // ============ NPC-NPC RELATIONSHIPS ============
    val npcRelationships: Map<String, Int> = emptyMap(), // npcId to relationship (-100 to 100)

    // ============ NEMESIS FEATURES ============
    val nemesisLevel: Int = 0, // 0=Normal, 1-5=Nemesis levels
    val vendettaReasons: List<String> = emptyList(), // ["defeated_by_player", "lost_honor"]
    val adaptationTraits: List<String> = emptyList(), // ["fire_resistance", "dodge_mastery"]

    // ============ ADVANCED FEATURES (TODO-NEM-04) ============
    val skills: List<String> = emptyList(), // ["heavy_armor", "fire_magic", "intimidation"]
    val titles: List<String> = emptyList(), // ["The Butcher of Blackwood", "Hero of the Village"]
    val badges: List<String> = emptyList(), // ["dragon_slayer_badge", "veteran_warrior"]
    val age: Int = 25,
    val gender: String = "Unknown",

    // ============ WORLD STATE ============
    val currentLocationId: String = "",
    val isAlive: Boolean = true,
    val isDead: Boolean = false,
    val isExiled: Boolean = false,

    // ============ EVOLUTION TRACKING ============
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val evolutionPoints: Int = 0,
    val timesSeen: Int = 0,
    val timesDefeated: Int = 0,
    val timesHelped: Int = 0
)

/**
 * Represents a single memory event that the NPC has about the player
 * TODO-NEM-03: Updated for enhanced memory tracking
 * TODO-FIX-04: Tüm alanlar nullable - AI boş {} gönderebilir
 */
@Serializable
data class NPCMemoryEvent(
    val timestamp: Long? = null,
    val eventType: String? = null,           // "FIRST_MEETING", "COMBAT", "TRADE", "HELPED", "BETRAYED"
    val eventDescription: String? = null,    // "Defeated in combat at the village square"
    val emotionalImpact: String? = null,     // "NEGATIVE", "POSITIVE", "NEUTRAL", "MAJOR_NEGATIVE", "MAJOR_POSITIVE"
    val locationId: String? = null,     // Where this happened
    val playerAction: String? = null,   // Deprecated, kept for compatibility
    val outcome: String? = null,        // Deprecated, kept for compatibility
    val action: String? = null          // Deprecated, kept for compatibility
)

/**
 * NPC's current emotional state/mood
 */
@Serializable
enum class NPCMood(val displayName: String, val modifier: Float) {
    ENRAGED("Öfkeli", -0.8f),
    HOSTILE("Düşmanca", -0.6f),
    ANGRY("Kızgın", -0.4f),
    ANNOYED("Sinirli", -0.2f),
    NEUTRAL("Nötr", 0.0f),
    PLEASED("Memnun", 0.2f),
    FRIENDLY("Arkadaşça", 0.4f),
    GRATEFUL("Minnettarlık", 0.6f),
    DEVOTED("Sadık", 0.8f);

    companion object {
        fun fromLoyalty(loyalty: Int): NPCMood {
            return when {
                loyalty <= -80 -> ENRAGED
                loyalty <= -60 -> HOSTILE
                loyalty <= -40 -> ANGRY
                loyalty <= -20 -> ANNOYED
                loyalty <= 20 -> NEUTRAL
                loyalty <= 40 -> PLEASED
                loyalty <= 60 -> FRIENDLY
                loyalty <= 80 -> GRATEFUL
                else -> DEVOTED
            }
        }
    }
}

/**
 * NPC Evolution events - tracks major changes in NPC state
 */
@Serializable
data class NPCEvolutionEvent(
    val npcId: String,
    val eventType: String,           // "level_up", "nemesis_promotion", "personality_change"
    val description: String,         // "Promoted to Nemesis Level 2 due to repeated defeats"
    val beforeState: String,         // JSON snapshot of key stats before
    val afterState: String,          // JSON snapshot of key stats after
    val timestamp: Long,
    val triggeredBy: String = ""     // "player_action", "time_passage", "world_event"
)