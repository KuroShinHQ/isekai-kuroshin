package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.isekaikuroshin.data.npc.DynamicNPCState
import com.example.isekaikuroshin.data.npc.NPCMemoryEvent
import com.example.isekaikuroshin.data.npc.NPCMood

/**
 * Room Entity for storing Dynamic NPC State in the database
 *
 * This entity stores the evolving state of NPCs including their stats, personality,
 * history with the player, and nemesis progression data.
 */
@Entity(tableName = "dynamic_npc_state")
@TypeConverters(Converters::class)
data class DynamicNPCEntity(
    @PrimaryKey
    val npcId: String,
    val baseRegistryId: String,

    // ============ DYNAMIC STATS ============
    val level: Int,
    val currentStrength: Int,
    val currentAgility: Int,
    val currentIntelligence: Int,
    val currentVitality: Int,

    // ============ DYNAMIC PERSONALITY ============
    val personalityTraits: List<String>,
    val currentMood: String, // NPCMood enum as string
    val loyaltyToPlayer: Int,

    // ============ HISTORY & MEMORY ============
    val historyWithPlayer: List<NPCMemoryEvent>,
    val interactionCount: Int,
    val lastSeenDate: Long,
    val lastLocationId: String,

    // ============ NPC-NPC RELATIONSHIPS ============
    val npcRelationships: Map<String, Int>,

    // ============ NEMESIS FEATURES ============
    val nemesisLevel: Int,
    val vendettaReasons: List<String>,
    val adaptationTraits: List<String>,

    // ============ WORLD STATE ============
    val currentLocationId: String,
    val isAlive: Boolean,
    val isDead: Boolean,
    val isExiled: Boolean,

    // ============ EVOLUTION TRACKING ============
    val lastUpdateTime: Long,
    val evolutionPoints: Int,
    val timesSeen: Int,
    val timesDefeated: Int,
    val timesHelped: Int,

    // ============ ADVANCED FEATURES (TODO-NEM-04) ============
    val skills: List<String> = emptyList(),
    val titles: List<String> = emptyList(),
    val badges: List<String> = emptyList(),
    val age: Int = 25,
    val gender: String = "Unknown"
)

/**
 * Extension functions to convert between DynamicNPCState and DynamicNPCEntity
 */
fun DynamicNPCState.toEntity(): DynamicNPCEntity {
    return DynamicNPCEntity(
        npcId = this.npcId,
        baseRegistryId = this.baseRegistryId,
        level = this.level,
        currentStrength = this.currentStrength,
        currentAgility = this.currentAgility,
        currentIntelligence = this.currentIntelligence,
        currentVitality = this.currentVitality,
        personalityTraits = this.personalityTraits,
        currentMood = this.currentMood.name,
        loyaltyToPlayer = this.loyaltyToPlayer,
        historyWithPlayer = this.historyWithPlayer,
        interactionCount = this.interactionCount,
        lastSeenDate = this.lastSeenDate,
        lastLocationId = this.lastLocationId,
        npcRelationships = this.npcRelationships,
        nemesisLevel = this.nemesisLevel,
        vendettaReasons = this.vendettaReasons,
        adaptationTraits = this.adaptationTraits,
        currentLocationId = this.currentLocationId,
        isAlive = this.isAlive,
        isDead = this.isDead,
        isExiled = this.isExiled,
        lastUpdateTime = this.lastUpdateTime,
        evolutionPoints = this.evolutionPoints,
        timesSeen = this.timesSeen,
        timesDefeated = this.timesDefeated,
        timesHelped = this.timesHelped,
        skills = this.skills,
        titles = this.titles,
        badges = this.badges,
        age = this.age,
        gender = this.gender
    )
}

fun DynamicNPCEntity.toDynamicNPCState(): DynamicNPCState {
    return DynamicNPCState(
        npcId = this.npcId,
        baseRegistryId = this.baseRegistryId,
        level = this.level,
        currentStrength = this.currentStrength,
        currentAgility = this.currentAgility,
        currentIntelligence = this.currentIntelligence,
        currentVitality = this.currentVitality,
        personalityTraits = this.personalityTraits,
        currentMood = NPCMood.valueOf(this.currentMood),
        loyaltyToPlayer = this.loyaltyToPlayer,
        historyWithPlayer = this.historyWithPlayer,
        interactionCount = this.interactionCount,
        lastSeenDate = this.lastSeenDate,
        lastLocationId = this.lastLocationId,
        npcRelationships = this.npcRelationships,
        nemesisLevel = this.nemesisLevel,
        vendettaReasons = this.vendettaReasons,
        adaptationTraits = this.adaptationTraits,
        currentLocationId = this.currentLocationId,
        isAlive = this.isAlive,
        isDead = this.isDead,
        isExiled = this.isExiled,
        lastUpdateTime = this.lastUpdateTime,
        evolutionPoints = this.evolutionPoints,
        timesSeen = this.timesSeen,
        timesDefeated = this.timesDefeated,
        timesHelped = this.timesHelped,
        skills = this.skills,
        titles = this.titles,
        badges = this.badges,
        age = this.age,
        gender = this.gender
    )
}