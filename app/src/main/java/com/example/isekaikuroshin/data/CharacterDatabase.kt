package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * Character Database System - NPCs, Companions, and Encountered Characters
 * G86: Metadata-based system with translation keys and unlock conditions
 */

@Serializable
data class UnlockCondition(
    val minDay: Int = 1,                    // Minimum journal day to unlock
    val requiredQuestId: String? = null,    // Quest ID that must be completed
    val requiredEventId: String? = null,    // Event ID that must occur
    val alwaysUnlocked: Boolean = false     // Always visible (for starter characters)
)

@Serializable
data class GameCharacter(
    val id: String,

    // G86: Metadata-based translation keys (KURAL 9 & 11)
    val nameKey: String,                    // Translation key for name
    val surnameKey: String = "",            // Translation key for surname
    val titleKey: String = "",              // Translation key for title
    val professionKey: String = "",         // Translation key for profession
    val backstoryKey: String = "",          // Translation key for backstory
    val personalityKey: String = "",        // Translation key for personality
    val appearanceKey: String = "",         // Translation key for appearance

    val age: Int = 25,
    val stats: CharacterStats = CharacterStats(),
    val locationKey: String = "",           // Translation key for location
    val relationship: Int = 0,              // -100 to +100 (hostile to ally)
    val isAlive: Boolean = true,
    val isCompanion: Boolean = false,
    val dialogueHistory: List<String> = emptyList(),
    val firstMetDay: Int = 1,
    val lastSeenDay: Int = 1,
    val specialAbilitiesKeys: List<String> = emptyList(),  // Translation keys for abilities
    val itemsKeys: List<String> = emptyList(),             // Translation keys for items
    val secretsKeys: List<String> = emptyList(),           // Translation keys for secrets

    // G86: Unlock system
    val unlockCondition: UnlockCondition = UnlockCondition(alwaysUnlocked = true)
)

@Serializable
data class CharacterStats(
    val hp: Int = 100,
    val strength: Int = 10,
    val agility: Int = 10,
    val intelligence: Int = 10,
    val charisma: Int = 10,
    val wisdom: Int = 10,
    val luck: Int = 10,
    val level: Int = 1
)

/**
 * Predefined NPCs - Starting characters like the cart driver
 */
object PredefinedCharacters {

    // G86: Metadata-based with translation keys (KURAL 9 & 11)
    val CART_DRIVER = GameCharacter(
        id = "cart_driver_001",
        nameKey = "char_cart_driver_name",
        surnameKey = "char_cart_driver_surname",
        titleKey = "char_cart_driver_title",
        professionKey = "char_cart_driver_profession",
        backstoryKey = "char_cart_driver_backstory",
        personalityKey = "char_cart_driver_personality",
        appearanceKey = "char_cart_driver_appearance",
        age = 55,
        stats = CharacterStats(
            hp = 80,
            strength = 15,
            agility = 8,
            intelligence = 12,
            charisma = 6,
            wisdom = 18,
            luck = 10,
            level = 5
        ),
        locationKey = "char_cart_driver_location",
        relationship = 10,
        isCompanion = true,
        specialAbilitiesKeys = listOf("ability_road_knowledge", "ability_horse_care", "ability_weather_prediction"),
        itemsKeys = listOf("item_whip", "item_road_map", "item_water_canteen", "item_dried_meat"),
        secretsKeys = listOf("secret_war_wounds", "secret_treasure_knowledge"),
        unlockCondition = UnlockCondition(alwaysUnlocked = true) // Starter character
    )

    // G86: Metadata-based - Unlocks after 3 days in marketplace
    val MYSTERIOUS_TRADER = GameCharacter(
        id = "mysterious_trader",
        nameKey = "char_mysterious_trader_name",
        surnameKey = "char_mysterious_trader_surname",
        titleKey = "char_mysterious_trader_title",
        professionKey = "char_mysterious_trader_profession",
        backstoryKey = "char_mysterious_trader_backstory",
        personalityKey = "char_mysterious_trader_personality",
        appearanceKey = "char_mysterious_trader_appearance",
        age = 45,
        stats = CharacterStats(
            hp = 70,
            strength = 8,
            agility = 16,
            intelligence = 19,
            charisma = 14,
            wisdom = 17,
            luck = 15,
            level = 6
        ),
        locationKey = "char_mysterious_trader_location",
        relationship = 0,
        specialAbilitiesKeys = listOf("ability_rare_item_detection", "ability_stealth", "ability_bargaining"),
        itemsKeys = listOf("item_mysterious_map", "item_mana_crystal", "item_antique_amulet"),
        secretsKeys = listOf("secret_true_identity_unknown", "secret_mage_past"),
        unlockCondition = UnlockCondition(minDay = 3) // Unlocks after 3 days
    )

    // G86: Metadata-based - Unlocks after 7 days (late-game encounter)
    val DARK_KNIGHT = GameCharacter(
        id = "dark_knight",
        nameKey = "char_dark_knight_name",
        surnameKey = "char_dark_knight_surname",
        titleKey = "char_dark_knight_title",
        professionKey = "char_dark_knight_profession",
        backstoryKey = "char_dark_knight_backstory",
        personalityKey = "char_dark_knight_personality",
        appearanceKey = "char_dark_knight_appearance",
        age = 35,
        stats = CharacterStats(
            hp = 120,
            strength = 18,
            agility = 12,
            intelligence = 8,
            charisma = 5,
            wisdom = 7,
            luck = 6,
            level = 8
        ),
        locationKey = "char_dark_knight_location",
        relationship = -40,
        specialAbilitiesKeys = listOf("ability_dark_magic", "ability_heavy_attack", "ability_armor_master"),
        itemsKeys = listOf("item_cursed_sword", "item_dark_armor", "item_soul_seal"),
        secretsKeys = listOf("secret_saving_lost_love", "secret_seeking_curse_break"),
        unlockCondition = UnlockCondition(minDay = 7) // Unlocks after 7 days
    )

    // G86: Metadata-based - Unlocks after 2 days (village arrival)
    val VILLAGE_ELDER = GameCharacter(
        id = "village_elder",
        nameKey = "char_village_elder_name",
        surnameKey = "char_village_elder_surname",
        titleKey = "char_village_elder_title",
        professionKey = "char_village_elder_profession",
        backstoryKey = "char_village_elder_backstory",
        personalityKey = "char_village_elder_personality",
        appearanceKey = "char_village_elder_appearance",
        age = 68,
        stats = CharacterStats(
            hp = 60,
            strength = 6,
            agility = 4,
            intelligence = 20,
            charisma = 18,
            wisdom = 22,
            luck = 12,
            level = 4
        ),
        locationKey = "char_village_elder_location",
        relationship = 35,
        specialAbilitiesKeys = listOf("ability_village_knowledge", "ability_leadership", "ability_old_stories"),
        itemsKeys = listOf("item_village_records", "item_family_photos", "item_old_map"),
        secretsKeys = listOf("secret_was_adventurer", "secret_knows_treasure_location"),
        unlockCondition = UnlockCondition(minDay = 2) // Unlocks after 2 days
    )

    // G86: Metadata-based - Unlocks after 5 days (forest exploration)
    val FOREST_WITCH = GameCharacter(
        id = "forest_witch",
        nameKey = "char_forest_witch_name",
        surnameKey = "char_forest_witch_surname",
        titleKey = "char_forest_witch_title",
        professionKey = "char_forest_witch_profession",
        backstoryKey = "char_forest_witch_backstory",
        personalityKey = "char_forest_witch_personality",
        appearanceKey = "char_forest_witch_appearance",
        age = 52,
        stats = CharacterStats(
            hp = 85,
            strength = 7,
            agility = 11,
            intelligence = 21,
            charisma = 8,
            wisdom = 19,
            luck = 13,
            level = 7
        ),
        locationKey = "char_forest_witch_location",
        relationship = -15,
        specialAbilitiesKeys = listOf("ability_healing_magic", "ability_plant_knowledge", "ability_nature_communication"),
        itemsKeys = listOf("item_healing_herbs", "item_spell_book", "item_crystal_ball"),
        secretsKeys = listOf("secret_old_love_in_village", "secret_knows_powerful_protection_magic"),
        unlockCondition = UnlockCondition(minDay = 5) // Unlocks after 5 days
    )

    val ALL_PREDEFINED = listOf(CART_DRIVER, MYSTERIOUS_TRADER, DARK_KNIGHT, VILLAGE_ELDER, FOREST_WITCH)
}

/**
 * G78 + G86: Extension function to convert GameCharacter to DynamicNPCEntity
 * Used for seeding predefined NPCs into Room database for WorldUpdateEngine
 * G86: Updated to use translation keys with LanguageManager
 */
fun GameCharacter.toDynamicNPCEntity(): com.example.isekaikuroshin.data.database.DynamicNPCEntity {
    return com.example.isekaikuroshin.data.database.DynamicNPCEntity(
        npcId = this.id,
        baseRegistryId = this.id, // Same as npcId for predefined characters

        // Dynamic stats from CharacterStats
        level = this.stats.level,
        currentStrength = this.stats.strength,
        currentAgility = this.stats.agility,
        currentIntelligence = this.stats.intelligence,
        currentVitality = 10, // Default vitality, not in CharacterStats

        // G86: Dynamic personality (using translation key)
        personalityTraits = if (this.personalityKey.isNotEmpty()) {
            listOf(LanguageManager.getText(this.personalityKey))
        } else emptyList(),
        currentMood = "NEUTRAL", // Default mood
        loyaltyToPlayer = this.relationship,

        // History & memory
        historyWithPlayer = emptyList(), // No initial memory
        interactionCount = 0,
        lastSeenDate = this.lastSeenDay.toLong(),
        // G86: Location using translation key
        lastLocationId = if (this.locationKey.isNotEmpty()) {
            LanguageManager.getText(this.locationKey)
        } else "unknown_location",

        // NPC-NPC relationships
        npcRelationships = emptyMap(),

        // Nemesis features (initially none)
        nemesisLevel = 0,
        vendettaReasons = emptyList(),
        adaptationTraits = emptyList(),

        // G86: World state (location using translation key)
        currentLocationId = if (this.locationKey.isNotEmpty()) {
            LanguageManager.getText(this.locationKey)
        } else "unknown_location",
        isAlive = this.isAlive,
        isDead = !this.isAlive,
        isExiled = false,

        // Evolution tracking
        lastUpdateTime = System.currentTimeMillis(),
        evolutionPoints = 0,
        timesSeen = 0,
        timesDefeated = 0,
        timesHelped = 0,

        // G86: Advanced features (using translation keys)
        skills = this.specialAbilitiesKeys.map { LanguageManager.getText(it) },
        titles = if (this.titleKey.isNotEmpty()) {
            listOf(LanguageManager.getText(this.titleKey))
        } else emptyList(),
        badges = emptyList(),
        age = this.age,
        gender = "Unknown" // GameCharacter doesn't have gender field
    )
}