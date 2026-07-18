package com.example.isekaikuroshin.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.RegistryQuest
import com.example.isekaikuroshin.ui.components.CardData
import com.example.isekaikuroshin.ui.components.CardRarity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class QuestsViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    private val _selectedTabIndex = MutableStateFlow(0)

    val uiState: StateFlow<QuestsUiState> = combine(
        gameStateManager.gameState,
        _selectedTabIndex
    ) { gameState, tabIndex ->
        val filteredQuests = when (tabIndex) {
            0 -> gameState.activeQuests // Ana Görevler (şimdilik tüm aktifler)
            1 -> emptyList() // Yan Görevler (henüz ayrım yok)
            2 -> gameState.completedQuests // Tamamlananlar
            else -> emptyList()
        }.map { it.toQuestCard() } // Convert to UI model

        QuestsUiState(
            selectedTabIndex = tabIndex,
            quests = filteredQuests
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuestsUiState()
    )

    fun onTabSelected(tabIndex: Int) {
        _selectedTabIndex.value = tabIndex
    }

    /**
     * Helper function to convert a RegistryQuest from the game state
     * into a CardData.QuestCard for the UI.
     * G135: KURAL 9 - Use localization keys (titleKey, descriptionKey)
     */
    private fun RegistryQuest.toQuestCard(): CardData.QuestCard {
        // G135: Prefer titleKey/descriptionKey over hardcoded title/description (KURAL 9)
        val localizedTitle = if (!this.titleKey.isNullOrBlank()) {
            LanguageManager.getText(this.titleKey)
        } else {
            this.title  // Fallback to legacy field
        }

        val localizedDescription = if (!this.descriptionKey.isNullOrBlank()) {
            LanguageManager.getText(this.descriptionKey)
        } else {
            this.description  // Fallback to legacy field
        }

        return CardData.QuestCard(
            id = this.id,
            name = localizedTitle,
            description = localizedDescription,
            imageRes = R.drawable.icon_map_seviye1, // Placeholder icon
            rarity = CardRarity.RARE, // Placeholder rarity, can be determined by quest type
            suggestedLevel = 1, // Placeholder level
            rewards = this.rewards.map { "${it.value}x ${it.key}" } // Format rewards
        )
    }
}