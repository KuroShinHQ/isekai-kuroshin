package com.example.isekaikuroshin.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.npc.NPCMemoryEvent
import com.example.isekaikuroshin.data.npc.NPCMood
import com.example.isekaikuroshin.data.world.FactionStatus
import com.example.isekaikuroshin.data.world.ResourceType
import com.example.isekaikuroshin.engine.Season
import com.example.isekaikuroshin.engine.Weather

/**
 * Room Type Converters for complex data types
 * Handles serialization and deserialization of custom objects to/from database storage
 */
class DatabaseConverters { // Renamed from AppTypeConverters
    private val gson = Gson()

    // Enum Converters
    @TypeConverter
    fun fromTimeOfDay(timeOfDay: TimeOfDay): String = timeOfDay.name

    @TypeConverter
    fun toTimeOfDay(timeOfDay: String): TimeOfDay = TimeOfDay.valueOf(timeOfDay)

    @TypeConverter
    fun fromSeason(season: Season): String = season.name

    @TypeConverter
    fun toSeason(season: String): Season = Season.valueOf(season)

    @TypeConverter
    fun fromWeather(weather: Weather): String = weather.name

    @TypeConverter
    fun toWeather(weather: String): Weather = Weather.valueOf(weather)

    @TypeConverter
    fun fromDirection(direction: Direction): String = direction.name

    @TypeConverter
    fun toDirection(direction: String): Direction = Direction.valueOf(direction)

    // List<String> Converters
    @TypeConverter
    fun fromStringList(list: List<String>): String = gson.toJson(list)

    @TypeConverter
    fun toStringList(data: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(data, listType)
    }

    // List<Location> Converters
    @TypeConverter
    fun fromLocationList(list: List<Location>): String = gson.toJson(list)

    @TypeConverter
    fun toLocationList(data: String): List<Location> {
        val listType = object : TypeToken<List<Location>>() {}.type
        return gson.fromJson(data, listType)
    }

    // Map<String, NPCRelationship> Converters
    @TypeConverter
    fun fromNPCRelationshipMap(map: Map<String, NPCRelationship>): String = gson.toJson(map)

    @TypeConverter
    fun toNPCRelationshipMap(data: String): Map<String, NPCRelationship> {
        val mapType = object : TypeToken<Map<String, NPCRelationship>>() {}.type
        return gson.fromJson(data, mapType)
    }

    // Map<String, EquippedItemZ6> Converters
    @TypeConverter
    fun fromEquippedItemMap(map: Map<String, EquippedItemZ6>): String = gson.toJson(map)

    @TypeConverter
    fun toEquippedItemMap(data: String): Map<String, EquippedItemZ6> {
        val mapType = object : TypeToken<Map<String, EquippedItemZ6>>() {}.type
        return gson.fromJson(data, mapType)
    }

    // List<EquippedItemZ6> Converters
    @TypeConverter
    fun fromEquippedItemList(list: List<EquippedItemZ6>): String = gson.toJson(list)

    @TypeConverter
    fun toEquippedItemList(data: String): List<EquippedItemZ6> {
        val listType = object : TypeToken<List<EquippedItemZ6>>() {}.type
        return gson.fromJson(data, listType)
    }

    // List<Badge> Converters
    @TypeConverter
    fun fromBadgeList(list: List<Badge>): String = gson.toJson(list)

    @TypeConverter
    fun toBadgeList(data: String): List<Badge> {
        val listType = object : TypeToken<List<Badge>>() {}.type
        return gson.fromJson(data, listType)
    }

    // List<Skill> Converters
    @TypeConverter
    fun fromSkillList(list: List<Skill>): String = gson.toJson(list)

    @TypeConverter
    fun toSkillList(data: String): List<Skill> {
        val listType = object : TypeToken<List<Skill>>() {}.type
        return gson.fromJson(data, listType)
    }

    // List<RegistryQuest> Converters
    @TypeConverter
    fun fromRegistryQuestList(list: List<RegistryQuest>): String = gson.toJson(list)

    @TypeConverter
    fun toRegistryQuestList(data: String): List<RegistryQuest> {
        val listType = object : TypeToken<List<RegistryQuest>>() {}.type
        return gson.fromJson(data, listType)
    }

    // Complex Object Converters
    @TypeConverter
    fun fromPlayerState(playerState: PlayerState): String = gson.toJson(playerState)

    @TypeConverter
    fun toPlayerState(data: String): PlayerState = gson.fromJson(data, PlayerState::class.java)

    @TypeConverter
    fun fromResourcesZ4(resources: ResourcesZ4): String = gson.toJson(resources)

    @TypeConverter
    fun toResourcesZ4(data: String): ResourcesZ4 = gson.fromJson(data, ResourcesZ4::class.java)

    @TypeConverter
    fun fromCollectedItemsZ5(items: CollectedItemsZ5): String = gson.toJson(items)

    @TypeConverter
    fun toCollectedItemsZ5(data: String): CollectedItemsZ5 = gson.fromJson(data, CollectedItemsZ5::class.java)

    @TypeConverter
    fun fromUmbrosContract(contract: UmbrosContract?): String? = contract?.let { gson.toJson(it) }

    @TypeConverter
    fun toUmbrosContract(data: String?): UmbrosContract? = data?.let { gson.fromJson(it, UmbrosContract::class.java) }

    // DocumentChunk related converters
    @TypeConverter
    fun fromDoubleList(list: List<Double>): String = gson.toJson(list)

    @TypeConverter
    fun toDoubleList(data: String): List<Double> {
        val listType = object : TypeToken<List<Double>>() {}.type
        return gson.fromJson(data, listType)
    }

    // NPC related converters
    @TypeConverter
    fun fromNPCMemoryEventList(list: List<NPCMemoryEvent>): String = gson.toJson(list)

    @TypeConverter
    fun toNPCMemoryEventList(data: String): List<NPCMemoryEvent> {
        val listType = object : TypeToken<List<NPCMemoryEvent>>() {}.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    fun fromStringIntMap(map: Map<String, Int>): String = gson.toJson(map)

    @TypeConverter
    fun toStringIntMap(data: String): Map<String, Int> {
        val mapType = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(data, mapType)
    }

    @TypeConverter
    fun fromNPCMood(mood: NPCMood): String = mood.name

    @TypeConverter
    fun toNPCMood(mood: String): NPCMood = NPCMood.valueOf(mood)

    // Faction related converters
    @TypeConverter
    fun fromFactionStatus(status: FactionStatus): String = status.name

    @TypeConverter
    fun toFactionStatus(status: String): FactionStatus = FactionStatus.valueOf(status)

    @TypeConverter
    fun fromResourceType(resourceType: ResourceType): String = resourceType.name

    @TypeConverter
    fun toResourceType(resourceType: String): ResourceType = ResourceType.valueOf(resourceType)

    // List<FactionStatus> Converters
    @TypeConverter
    fun fromFactionStatusList(list: List<FactionStatus>): String = gson.toJson(list)

    @TypeConverter
    fun toFactionStatusList(data: String): List<FactionStatus> {
        val listType = object : TypeToken<List<FactionStatus>>() {}.type
        return gson.fromJson(data, listType)
    }

    // ========================================
    // KADIM MÜHÜR TEKNİKLERİ - SEAL CONVERTERS
    // ========================================

    // List<NormalizedPoint> Converters
    @TypeConverter
    fun fromNormalizedPointList(list: List<NormalizedPoint>): String = gson.toJson(list)

    @TypeConverter
    fun toNormalizedPointList(data: String): List<NormalizedPoint> {
        val listType = object : TypeToken<List<NormalizedPoint>>() {}.type
        return gson.fromJson(data, listType)
    }

    // List<List<NormalizedPoint>> Converters (for multi-template support)
    @TypeConverter
    fun fromNormalizedPointListList(list: List<List<NormalizedPoint>>): String = gson.toJson(list)

    @TypeConverter
    fun toNormalizedPointListList(data: String): List<List<NormalizedPoint>> {
        val listType = object : TypeToken<List<List<NormalizedPoint>>>() {}.type
        return gson.fromJson(data, listType)
    }

    // PracticeMetrics Converter
    @TypeConverter
    fun fromPracticeMetrics(metrics: PracticeMetrics): String = gson.toJson(metrics)

    @TypeConverter
    fun toPracticeMetrics(data: String): PracticeMetrics = gson.fromJson(data, PracticeMetrics::class.java)

    // List<Seal> Converters
    @TypeConverter
    fun fromSealList(list: List<Seal>): String = gson.toJson(list)

    @TypeConverter
    fun toSealList(data: String): List<Seal> {
        val listType = object : TypeToken<List<Seal>>() {}.type
        return gson.fromJson(data, listType)
    }

    // SealDifficulty Converter
    @TypeConverter
    fun fromSealDifficulty(difficulty: SealDifficulty): String = difficulty.name

    @TypeConverter
    fun toSealDifficulty(difficulty: String): SealDifficulty = SealDifficulty.valueOf(difficulty)
}
