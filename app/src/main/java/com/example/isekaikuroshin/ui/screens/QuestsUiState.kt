
package com.example.isekaikuroshin.ui.screens

import com.example.isekaikuroshin.ui.components.CardData

/**
 * Represents the UI state for the Quests screen.
 */
data class QuestsUiState(
    val selectedTabIndex: Int = 0,
    val quests: List<CardData.QuestCard> = emptyList(),
    val tabs: List<String> = listOf("main_quests", "side_quests", "completed") // Translation keys
)
