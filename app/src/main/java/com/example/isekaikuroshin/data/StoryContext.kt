package com.example.isekaikuroshin.data

/**
 * Holds all the relevant information about the current game state 
 * to be passed to the story generation engine.
 */
data class StoryContext(
    val userAction: String,
    val playerStats: PlayerState, // CORRECTED: Was PlayerStatsZ3
    val currentLocation: Location,
    val timeOfDay: TimeOfDay,
    val recentEvents: List<String> = emptyList()
)
