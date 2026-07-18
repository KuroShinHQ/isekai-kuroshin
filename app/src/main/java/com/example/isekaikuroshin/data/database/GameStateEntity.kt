package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.isekaikuroshin.data.* // Covers TimeOfDay, Location, UmbrosContract etc. from 'data' package
import com.example.isekaikuroshin.engine.Season // Correct import for Season
import com.example.isekaikuroshin.engine.Weather // Correct import for Weather

/**
 * Room Entity for persisting GameStateZ7 to local database
 * This entity mirrors the structure of GameStateZ7 but with Room annotations
 */
@Entity(tableName = "game_state")
@TypeConverters(Converters::class)
data class GameStateEntity(
    @PrimaryKey
    val id: Int = 1, // Single row for current game state

    // Time and World
    val currentDay: Int = 1,
    val currentTimeOfDay: TimeOfDay = TimeOfDay.MORNING, // Corrected from SABAH
    val currentSeason: Season = Season.SPRING, // Uses engine.Season
    val currentWeather: Weather = Weather.SUNNY, // Corrected from GUNESLI, uses engine.Weather
    val currentLocationId: String = WorldLocations.FOREST_CAMP.id,
    val knownLocations: List<Location> = listOf(WorldLocations.FOREST_CAMP),
    val currentDirection: Direction = Direction.DOGU,
    val travelDescription: String = "Yeşil ovalardan doğuya doğru ilerliyor",

    // Social System
    val npcRelationships: Map<String, NPCRelationship> = emptyMap(),

    // Player Data
    val playerState: PlayerState = PlayerState(),
    val resources: ResourcesZ4 = ResourcesZ4(),
    val collectedItems: CollectedItemsZ5 = CollectedItemsZ5(),

    // Equipment System
    val equippedItems: Map<String, EquippedItemZ6> = emptyMap(),
    val inventory: List<EquippedItemZ6> = emptyList(),

    // Character Progression System - Active Badges and Skills
    val activeBadges: List<Badge> = emptyList(),
    val activeSkills: List<Skill> = emptyList(),

    // Story Progress
    val storyPages: List<String> = listOf(
        LanguageManager.getText("chosen_one_story")
    ),
    val currentPage: Int = 1,

    // Quest System
    val activeQuests: List<RegistryQuest> = emptyList(),
    val completedQuests: List<RegistryQuest> = emptyList(),

    // Game Mechanics
    val isGameActive: Boolean = true,
    val lastActionTime: Long = System.currentTimeMillis(),

    // Death System
    val umbrosContract: UmbrosContract? = null // Uses data.UmbrosContract via wildcard or explicit import
)

/**
 * Extension functions to convert between GameStateZ7 and GameStateEntity
 */
fun GameStateZ7.toEntity(): GameStateEntity {
    return GameStateEntity(
        id = 1,
        currentDay = this.currentDay,
        currentTimeOfDay = this.currentTimeOfDay,
        currentSeason = this.currentSeason, // Will now correctly map if GameStateZ7 also uses engine.Season
        currentWeather = this.currentWeather, // Will now correctly map if GameStateZ7 also uses engine.Weather
        currentLocationId = this.currentLocationId,
        knownLocations = this.knownLocations,
        currentDirection = this.currentDirection,
        travelDescription = this.travelDescription,
        npcRelationships = this.npcRelationships,
        playerState = this.playerState,
        resources = this.resources,
        collectedItems = this.collectedItems,
        equippedItems = this.equippedItems,
        inventory = this.inventory,
        activeBadges = this.activeBadges,
        activeSkills = this.activeSkills,
        storyPages = this.storyPages,
        currentPage = this.currentPage,
        activeQuests = this.activeQuests,
        completedQuests = this.completedQuests,
        isGameActive = this.isGameActive,
        lastActionTime = this.lastActionTime,
        umbrosContract = this.umbrosContract
    )
}

fun GameStateEntity.toGameState(): GameStateZ7 {
    return GameStateZ7(
        currentDay = this.currentDay,
        currentTimeOfDay = this.currentTimeOfDay,
        currentSeason = this.currentSeason, // Will now correctly map if GameStateEntity uses engine.Season
        currentWeather = this.currentWeather, // Will now correctly map if GameStateEntity uses engine.Weather
        currentLocationId = this.currentLocationId,
        knownLocations = this.knownLocations,
        currentDirection = this.currentDirection,
        travelDescription = this.travelDescription,
        npcRelationships = this.npcRelationships,
        playerState = this.playerState,
        resources = this.resources,
        collectedItems = this.collectedItems,
        equippedItems = this.equippedItems,
        inventory = this.inventory,
        activeBadges = this.activeBadges,
        activeSkills = this.activeSkills,
        storyPages = this.storyPages,
        currentPage = this.currentPage,
        activeQuests = this.activeQuests,
        completedQuests = this.completedQuests,
        isGameActive = this.isGameActive,
        lastActionTime = this.lastActionTime,
        umbrosContract = this.umbrosContract
    )
}
