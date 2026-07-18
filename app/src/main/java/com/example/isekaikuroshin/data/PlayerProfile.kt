package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfile(
    val playerName: String = "",
    val playerAge: Int = 18,
    val playerGender: String = "Unspecified", // KURAL 16: EN defaults, LanguageManager çevirir
    val archetypeScores: Map<String, Int> = mapOf(
        "Warrior" to 0,
        "Explorer" to 0,
        "Craftsman" to 0,
        "Mystic" to 0
    ),
    val dominantArchetype: String = "Unknown", // KURAL 16: EN default
    val triggeredClassQuests: Set<String> = emptySet(),
    val savedApiKey: String = "" // User's saved API key
)