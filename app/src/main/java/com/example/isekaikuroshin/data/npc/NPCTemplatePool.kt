package com.example.isekaikuroshin.data.npc

import com.example.isekaikuroshin.data.LocationType

/**
 * G138: NPC Template Pool - Default NPC Templates for Auto-Spawn System
 *
 * Provides 20 pre-defined NPC templates that can be spawned automatically
 * when a location has no NPCs and AI doesn't create them.
 *
 * KURAL 11: Uses metadata-based logic (no string comparison for business logic)
 * KURAL 9: Display names will be localized via LanguageManager in UI
 */

/**
 * Template for creating default NPCs
 */
data class NPCTemplate(
    val baseRegistryId: String,      // Unique template ID
    val archetype: NPCArchetype,      // Role/type of NPC
    val defaultLoyalty: Int,          // -100 to 100
    val defaultLevel: Int,            // Starting level
    val personalityTraits: List<String>,
    val preferredLocations: List<LocationType>
)

/**
 * NPC Archetypes - Defines NPC behavior and role
 */
enum class NPCArchetype(
    val baseStrength: Int,
    val baseAgility: Int,
    val baseIntelligence: Int,
    val baseVitality: Int
) {
    // Friendly NPCs
    MERCHANT(5, 8, 12, 10),          // High INT, low combat
    HEALER(4, 6, 14, 12),            // High INT, high VIT
    VILLAGER(6, 6, 8, 10),           // Balanced, civilian
    FARMER(8, 5, 6, 12),             // High STR, high VIT
    SCHOLAR(3, 5, 16, 8),            // Very high INT

    // Neutral NPCs
    TOWN_GUARD(12, 10, 8, 14),      // Balanced fighter
    BLACKSMITH(14, 7, 10, 12),      // High STR
    WANDERER(8, 12, 10, 10),        // High AGI
    HERMIT(6, 6, 14, 8),            // High INT, low social
    HUNTER(10, 14, 8, 10),          // High AGI

    // Hostile NPCs
    BANDIT(11, 13, 7, 9),           // High AGI, aggressive
    WILD_BEAST(14, 12, 4, 12),      // High STR, feral
    ROGUE(9, 16, 10, 8),            // Very high AGI
    CULTIST(8, 8, 14, 8),           // High INT, dangerous
    CORRUPTED_SOLDIER(13, 11, 7, 13) // High combat stats
}

/**
 * G138: NPC Template Pool Object
 *
 * Provides functions to:
 * 1. Get random NPC by location type
 * 2. Get NPC by loyalty (friendly/neutral/hostile)
 * 3. Get all templates
 */
object NPCTemplatePool {

    /**
     * Pool of 20 default NPC templates
     */
    private val templates = listOf(
        // === FRIENDLY NPCs (Loyalty: 20-60) ===
        NPCTemplate(
            baseRegistryId = "template_merchant",
            archetype = NPCArchetype.MERCHANT,
            defaultLoyalty = 40,
            defaultLevel = 3,
            personalityTraits = listOf("greedy", "helpful", "talkative"),
            preferredLocations = listOf(LocationType.TOWN, LocationType.CITY, LocationType.VILLAGE)
        ),
        NPCTemplate(
            baseRegistryId = "template_healer",
            archetype = NPCArchetype.HEALER,
            defaultLoyalty = 50,
            defaultLevel = 4,
            personalityTraits = listOf("kind", "wise", "patient"),
            preferredLocations = listOf(LocationType.TOWN, LocationType.CITY, LocationType.VILLAGE)
        ),
        NPCTemplate(
            baseRegistryId = "template_villager",
            archetype = NPCArchetype.VILLAGER,
            defaultLoyalty = 30,
            defaultLevel = 1,
            personalityTraits = listOf("friendly", "cautious", "gossip"),
            preferredLocations = listOf(LocationType.VILLAGE, LocationType.TOWN)
        ),
        NPCTemplate(
            baseRegistryId = "template_farmer",
            archetype = NPCArchetype.FARMER,
            defaultLoyalty = 25,
            defaultLevel = 2,
            personalityTraits = listOf("hardworking", "simple", "honest"),
            preferredLocations = listOf(LocationType.VILLAGE, LocationType.WILDERNESS)
        ),
        NPCTemplate(
            baseRegistryId = "template_scholar",
            archetype = NPCArchetype.SCHOLAR,
            defaultLoyalty = 35,
            defaultLevel = 5,
            personalityTraits = listOf("curious", "knowledgeable", "eccentric"),
            preferredLocations = listOf(LocationType.CITY, LocationType.TOWN, LocationType.RUIN)
        ),

        // === NEUTRAL NPCs (Loyalty: -10 to 10) ===
        NPCTemplate(
            baseRegistryId = "template_town_guard",
            archetype = NPCArchetype.TOWN_GUARD,
            defaultLoyalty = 0,
            defaultLevel = 5,
            personalityTraits = listOf("disciplined", "vigilant", "stern"),
            preferredLocations = listOf(LocationType.CITY, LocationType.TOWN, LocationType.VILLAGE)
        ),
        NPCTemplate(
            baseRegistryId = "template_blacksmith",
            archetype = NPCArchetype.BLACKSMITH,
            defaultLoyalty = 5,
            defaultLevel = 6,
            personalityTraits = listOf("gruff", "skilled", "proud"),
            preferredLocations = listOf(LocationType.CITY, LocationType.TOWN, LocationType.VILLAGE)
        ),
        NPCTemplate(
            baseRegistryId = "template_wanderer",
            archetype = NPCArchetype.WANDERER,
            defaultLoyalty = -5,
            defaultLevel = 4,
            personalityTraits = listOf("mysterious", "aloof", "experienced"),
            preferredLocations = listOf(LocationType.WILDERNESS, LocationType.TOWN, LocationType.SPECIAL)
        ),
        NPCTemplate(
            baseRegistryId = "template_hermit",
            archetype = NPCArchetype.HERMIT,
            defaultLoyalty = 0,
            defaultLevel = 7,
            personalityTraits = listOf("reclusive", "wise", "paranoid"),
            preferredLocations = listOf(LocationType.WILDERNESS, LocationType.CAVE, LocationType.RUIN)
        ),
        NPCTemplate(
            baseRegistryId = "template_hunter",
            archetype = NPCArchetype.HUNTER,
            defaultLoyalty = 10,
            defaultLevel = 5,
            personalityTraits = listOf("practical", "quiet", "skilled"),
            preferredLocations = listOf(LocationType.WILDERNESS, LocationType.VILLAGE)
        ),

        // === HOSTILE NPCs (Loyalty: -60 to -20) ===
        NPCTemplate(
            baseRegistryId = "template_bandit_scout",
            archetype = NPCArchetype.BANDIT,
            defaultLoyalty = -40,
            defaultLevel = 3,
            personalityTraits = listOf("aggressive", "greedy", "cunning"),
            preferredLocations = listOf(LocationType.WILDERNESS, LocationType.RUIN)
        ),
        NPCTemplate(
            baseRegistryId = "template_bandit_leader",
            archetype = NPCArchetype.BANDIT,
            defaultLoyalty = -50,
            defaultLevel = 6,
            personalityTraits = listOf("ruthless", "tactical", "charismatic"),
            preferredLocations = listOf(LocationType.WILDERNESS, LocationType.RUIN, LocationType.CAVE)
        ),
        NPCTemplate(
            baseRegistryId = "template_wild_wolf",
            archetype = NPCArchetype.WILD_BEAST,
            defaultLoyalty = -30,
            defaultLevel = 3,
            personalityTraits = listOf("feral", "pack_hunter", "territorial"),
            preferredLocations = listOf(LocationType.WILDERNESS, LocationType.CAVE)
        ),
        NPCTemplate(
            baseRegistryId = "template_wild_bear",
            archetype = NPCArchetype.WILD_BEAST,
            defaultLoyalty = -35,
            defaultLevel = 5,
            personalityTraits = listOf("aggressive", "powerful", "territorial"),
            preferredLocations = listOf(LocationType.WILDERNESS, LocationType.CAVE)
        ),
        NPCTemplate(
            baseRegistryId = "template_rogue_assassin",
            archetype = NPCArchetype.ROGUE,
            defaultLoyalty = -55,
            defaultLevel = 7,
            personalityTraits = listOf("stealthy", "deadly", "merciless"),
            preferredLocations = listOf(LocationType.CITY, LocationType.TOWN, LocationType.WILDERNESS)
        ),
        NPCTemplate(
            baseRegistryId = "template_rogue_thief",
            archetype = NPCArchetype.ROGUE,
            defaultLoyalty = -35,
            defaultLevel = 4,
            personalityTraits = listOf("sneaky", "opportunistic", "cowardly"),
            preferredLocations = listOf(LocationType.CITY, LocationType.TOWN, LocationType.RUIN)
        ),
        NPCTemplate(
            baseRegistryId = "template_cultist",
            archetype = NPCArchetype.CULTIST,
            defaultLoyalty = -60,
            defaultLevel = 6,
            personalityTraits = listOf("fanatic", "dangerous", "unpredictable"),
            preferredLocations = listOf(LocationType.RUIN, LocationType.CAVE, LocationType.SPECIAL)
        ),
        NPCTemplate(
            baseRegistryId = "template_corrupted_guard",
            archetype = NPCArchetype.CORRUPTED_SOLDIER,
            defaultLoyalty = -45,
            defaultLevel = 5,
            personalityTraits = listOf("corrupted", "violent", "fallen"),
            preferredLocations = listOf(LocationType.RUIN, LocationType.DUNGEON, LocationType.SPECIAL)
        ),
        NPCTemplate(
            baseRegistryId = "template_dungeon_monster",
            archetype = NPCArchetype.WILD_BEAST,
            defaultLoyalty = -50,
            defaultLevel = 7,
            personalityTraits = listOf("monstrous", "bloodthirsty", "relentless"),
            preferredLocations = listOf(LocationType.DUNGEON, LocationType.CAVE, LocationType.RUIN)
        ),
        NPCTemplate(
            baseRegistryId = "template_cave_goblin",
            archetype = NPCArchetype.BANDIT,
            defaultLoyalty = -35,
            defaultLevel = 2,
            personalityTraits = listOf("cowardly", "sneaky", "tribal"),
            preferredLocations = listOf(LocationType.CAVE, LocationType.WILDERNESS, LocationType.RUIN)
        )
    )

    /**
     * G138: Get random NPC template suitable for location type
     *
     * Prioritizes templates that prefer this location type,
     * falls back to any template if none found.
     */
    fun getRandomNPCForLocation(locationType: LocationType): NPCTemplate {
        // Filter templates that prefer this location
        val suitableTemplates = templates.filter { it.preferredLocations.contains(locationType) }

        return if (suitableTemplates.isNotEmpty()) {
            suitableTemplates.random()
        } else {
            // Fallback: return any random template
            templates.random()
        }
    }

    /**
     * G138: Get multiple random NPCs for location (for spawning 1-2 NPCs)
     */
    fun getRandomNPCsForLocation(locationType: LocationType, count: Int): List<NPCTemplate> {
        val suitableTemplates = templates.filter { it.preferredLocations.contains(locationType) }
        val pool = if (suitableTemplates.isNotEmpty()) suitableTemplates else templates

        return pool.shuffled().take(count.coerceIn(1, pool.size))
    }

    /**
     * Get friendly NPC template (loyalty >= 20)
     */
    fun getFriendlyNPC(): NPCTemplate {
        return templates.filter { it.defaultLoyalty >= 20 }.random()
    }

    /**
     * Get neutral NPC template (loyalty -10 to 10)
     */
    fun getNeutralNPC(): NPCTemplate {
        return templates.filter { it.defaultLoyalty in -10..10 }.random()
    }

    /**
     * Get hostile NPC template (loyalty <= -20)
     */
    fun getHostileNPC(): NPCTemplate {
        return templates.filter { it.defaultLoyalty <= -20 }.random()
    }

    /**
     * Get all templates (for debug/testing)
     */
    fun getAllTemplates(): List<NPCTemplate> = templates

    /**
     * Get template by ID
     */
    fun getTemplateById(id: String): NPCTemplate? {
        return templates.find { it.baseRegistryId == id }
    }
}
