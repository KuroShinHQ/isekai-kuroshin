package com.example.isekaikuroshin.data.database

import androidx.room.TypeConverter
import com.example.isekaikuroshin.data.* // Covers TimeOfDay, Location, UmbrosContract etc. from 'data' package
import com.example.isekaikuroshin.data.Location // Explicit import for KSP
import com.example.isekaikuroshin.data.UmbrosContract // Corrected import path
import com.example.isekaikuroshin.engine.Season // Added explicit import for engine.Season
import com.example.isekaikuroshin.engine.Weather // Added explicit import for engine.Weather
import com.example.isekaikuroshin.data.npc.NPCMemoryEvent
import com.example.isekaikuroshin.data.npc.NPCMood
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for complex data types in GameStateZ7
 * These converters allow Room to store complex objects as JSON strings
 */
class Converters {
    private val gson = Gson()

    // PlayerState Converter (NEW)
    @TypeConverter
    fun fromPlayerState(state: PlayerState): String {
        return gson.toJson(state)
    }

    @TypeConverter
    fun toPlayerState(stateJson: String): PlayerState {
        return gson.fromJson(stateJson, PlayerState::class.java)
    }

    // ResourcesZ4 Converter
    @TypeConverter
    fun fromResources(resources: ResourcesZ4): String {
        return gson.toJson(resources)
    }

    @TypeConverter
    fun toResources(resourcesJson: String): ResourcesZ4 {
        return gson.fromJson(resourcesJson, ResourcesZ4::class.java)
    }

    // CollectedItemsZ5 Converter
    @TypeConverter
    fun fromCollectedItems(items: CollectedItemsZ5): String {
        return gson.toJson(items)
    }

    @TypeConverter
    fun toCollectedItems(itemsJson: String): CollectedItemsZ5 {
        return gson.fromJson(itemsJson, CollectedItemsZ5::class.java)
    }

    // TimeOfDay Converter
    @TypeConverter
    fun fromTimeOfDay(timeOfDay: TimeOfDay): String { // Uses data.TimeOfDay via wildcard import
        return timeOfDay.name
    }

    @TypeConverter
    fun toTimeOfDay(timeOfDayName: String): TimeOfDay {
        return TimeOfDay.valueOf(timeOfDayName)
    }

    // Season Converter
    @TypeConverter
    fun fromSeason(season: Season): String { // Now uses engine.Season via explicit import
        return season.name
    }

    @TypeConverter
    fun toSeason(seasonName: String): Season {
        return Season.valueOf(seasonName)
    }

    // Weather Converter
    @TypeConverter
    fun fromWeather(weather: Weather): String { // Now uses engine.Weather via explicit import
        return weather.name
    }

    @TypeConverter
    fun toWeather(weatherName: String): Weather {
        return Weather.valueOf(weatherName)
    }

    // Direction Converter
    @TypeConverter
    fun fromDirection(direction: Direction): String { // Assuming data.Direction
        return direction.name
    }

    @TypeConverter
    fun toDirection(directionName: String): Direction {
        return Direction.valueOf(directionName)
    }

    // List<Location> Converter
    @TypeConverter
    fun fromLocationList(locations: List<Location>): String {
        return gson.toJson(locations)
    }

    @TypeConverter
    fun toLocationList(locationsJson: String): List<Location> {
        val type = object : TypeToken<List<Location>>() {}.type
        return gson.fromJson(locationsJson, type)
    }

    // Map<String, NPCRelationship> Converter
    @TypeConverter
    fun fromNPCRelationshipMap(relationships: Map<String, NPCRelationship>): String {
        return gson.toJson(relationships)
    }

    @TypeConverter
    fun toNPCRelationshipMap(relationshipsJson: String): Map<String, NPCRelationship> {
        val type = object : TypeToken<Map<String, NPCRelationship>>() {}.type
        return gson.fromJson(relationshipsJson, type)
    }

    // Map<String, EquippedItemZ6> Converter
    @TypeConverter
    fun fromEquippedItemMap(items: Map<String, EquippedItemZ6>): String {
        return gson.toJson(items)
    }

    @TypeConverter
    fun toEquippedItemMap(itemsJson: String): Map<String, EquippedItemZ6> {
        val type = object : TypeToken<Map<String, EquippedItemZ6>>() {}.type
        return gson.fromJson(itemsJson, type)
    }

    // List<Badge> Converter (Updated for Hibrit System)
    @TypeConverter
    fun fromBadgeList(badges: List<Badge>): String {
        return gson.toJson(badges)
    }

    @TypeConverter
    fun toBadgeList(badgesJson: String): List<Badge> {
        val type = object : TypeToken<List<Badge>>() {}.type
        return gson.fromJson(badgesJson, type)
    }

    // BadgeType Enum Converter
    @TypeConverter
    fun fromBadgeType(badgeType: com.example.isekaikuroshin.data.BadgeType): String {
        return badgeType.name
    }

    @TypeConverter
    fun toBadgeType(badgeTypeName: String): com.example.isekaikuroshin.data.BadgeType {
        return com.example.isekaikuroshin.data.BadgeType.valueOf(badgeTypeName)
    }

    // List<Skill> Converter
    @TypeConverter
    fun fromSkillList(skills: List<Skill>): String {
        return gson.toJson(skills)
    }

    @TypeConverter
    fun toSkillList(skillsJson: String): List<Skill> {
        val type = object : TypeToken<List<Skill>>() {}.type
        return gson.fromJson(skillsJson, type)
    }

    // List<String> Converter (for storyPages)
    @TypeConverter
    fun fromStringList(strings: List<String>): String {
        return gson.toJson(strings)
    }

    @TypeConverter
    fun toStringList(stringsJson: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(stringsJson, type)
    }

    // UmbrosContract Converter (nullable)
    @TypeConverter
    fun fromUmbrosContract(contract: UmbrosContract?): String? { // Uses data.UmbrosContract
        return contract?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toUmbrosContract(contractJson: String?): UmbrosContract? {
        return contractJson?.let { gson.fromJson(it, UmbrosContract::class.java) }
    }

    // List<RegistryQuest> Converter
    @TypeConverter
    fun fromRegistryQuestList(quests: List<RegistryQuest>): String {
        return gson.toJson(quests)
    }

    @TypeConverter
    fun toRegistryQuestList(questsJson: String): List<RegistryQuest> {
        val type = object : TypeToken<List<RegistryQuest>>() {}.type
        return gson.fromJson(questsJson, type)
    }

    // List<EquippedItemZ6> Converter
    @TypeConverter
    fun fromEquippedItemList(items: List<EquippedItemZ6>): String {
        return gson.toJson(items)
    }

    @TypeConverter
    fun toEquippedItemList(itemsJson: String): List<EquippedItemZ6> {
        val type = object : TypeToken<List<EquippedItemZ6>>() {}.type
        return gson.fromJson(itemsJson, type)
    }

    // ============ DYNAMIC NPC CONVERTERS ============

    // List<NPCMemoryEvent> Converter
    @TypeConverter
    fun fromNPCMemoryEventList(events: List<NPCMemoryEvent>): String {
        return gson.toJson(events)
    }

    @TypeConverter
    fun toNPCMemoryEventList(eventsJson: String): List<NPCMemoryEvent> {
        val type = object : TypeToken<List<NPCMemoryEvent>>() {}.type
        return gson.fromJson(eventsJson, type)
    }

    // Map<String, Int> Converter (for NPC relationships)
    @TypeConverter
    fun fromStringIntMap(map: Map<String, Int>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toStringIntMap(mapJson: String): Map<String, Int> {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(mapJson, type)
    }

    // ============ KADIM MÜHÜR TEKNİKLERİ (SEAL SYSTEM) CONVERTERS ============

    // List<NormalizedPoint> Converter
    @TypeConverter
    fun fromNormalizedPointList(points: List<NormalizedPoint>): String {
        return gson.toJson(points)
    }

    @TypeConverter
    fun toNormalizedPointList(pointsJson: String): List<NormalizedPoint> {
        val type = object : TypeToken<List<NormalizedPoint>>() {}.type
        return gson.fromJson(pointsJson, type)
    }

    // List<List<NormalizedPoint>> Converter (for multi-template support)
    @TypeConverter
    fun fromNormalizedPointListList(templates: List<List<NormalizedPoint>>): String {
        return gson.toJson(templates)
    }

    @TypeConverter
    fun toNormalizedPointListList(templatesJson: String): List<List<NormalizedPoint>> {
        val type = object : TypeToken<List<List<NormalizedPoint>>>() {}.type
        return gson.fromJson(templatesJson, type)
    }

    // PracticeMetrics Converter
    @TypeConverter
    fun fromPracticeMetrics(metrics: PracticeMetrics): String {
        return gson.toJson(metrics)
    }

    @TypeConverter
    fun toPracticeMetrics(metricsJson: String): PracticeMetrics {
        return gson.fromJson(metricsJson, PracticeMetrics::class.java)
    }

    // SealDifficulty Enum Converter
    @TypeConverter
    fun fromSealDifficulty(difficulty: SealDifficulty): String {
        return difficulty.name
    }

    @TypeConverter
    fun toSealDifficulty(difficultyName: String): SealDifficulty {
        return SealDifficulty.valueOf(difficultyName)
    }

    // List<Seal> Converter
    @TypeConverter
    fun fromSealList(seals: List<Seal>): String {
        return gson.toJson(seals)
    }

    @TypeConverter
    fun toSealList(sealsJson: String): List<Seal> {
        val type = object : TypeToken<List<Seal>>() {}.type
        return gson.fromJson(sealsJson, type)
    }
}
