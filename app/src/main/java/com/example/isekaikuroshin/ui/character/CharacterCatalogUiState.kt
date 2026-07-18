
package com.example.isekaikuroshin.ui.character

/**
 * Represents the UI state for the Character Catalog screen.
 */
data class CharacterCatalogUiState(
    val characters: List<CharacterCardState> = emptyList()
)

/**
 * Represents the state for a single character card.
 * G86: Added isLocked field for unlock progression system
 */
data class CharacterCardState(
    val id: String,
    val name: String,
    val title: String,
    val role: String, // Corresponds to 'profession' in GameCharacter
    val bio: String, // Corresponds to 'backstory' in GameCharacter
    val location: String,
    val relationshipLevel: Int,
    val isLocked: Boolean = false // G86: True if character hasn't been unlocked yet
)
