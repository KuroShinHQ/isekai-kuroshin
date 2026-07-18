package com.example.isekaikuroshin.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameCharacter
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.utils.CharacterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CharacterCatalogViewModel @Inject constructor(
    gameStateManager: GameStateManager
) : ViewModel() {

    // G86: Metadata-based system with unlock logic and LanguageManager
    val uiState: StateFlow<CharacterCatalogUiState> = combine(
        gameStateManager.gameState,
        LanguageManager.currentLanguage
    ) { gameState, _ ->
        // G76: Get all characters from CharacterManager
        val allCharacters = CharacterManager.getAllCharacters()

        val characters = allCharacters.map { character ->
            // G76: Get relationship level from gameState if it exists, otherwise default to 0
            val relationshipLevel = gameState.npcRelationships[character.id]?.relationshipPoints ?: 0

            // G86: Check unlock condition (KURAL 11 - metadata-based logic)
            val isUnlocked = checkUnlockCondition(character, gameState.currentDay)

            // G86: Convert using LanguageManager (KURAL 9 - no hardcoded text)
            CharacterCardState(
                id = character.id,
                name = LanguageManager.getText(character.nameKey),
                title = LanguageManager.getText(character.titleKey),
                role = LanguageManager.getText(character.professionKey),
                bio = LanguageManager.getText(character.backstoryKey),
                location = LanguageManager.getText(character.locationKey),
                relationshipLevel = relationshipLevel,
                isLocked = !isUnlocked  // G86: Add locked state
            )
        }
        CharacterCatalogUiState(characters = characters)
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CharacterCatalogUiState()
    )

    /**
     * G86: Check if character meets unlock conditions
     * KURAL 11: Metadata-based logic, not string comparison
     */
    private fun checkUnlockCondition(character: GameCharacter, currentDay: Int): Boolean {
        val condition = character.unlockCondition

        // Always unlocked characters (like starter NPCs)
        if (condition.alwaysUnlocked) return true

        // Check day requirement
        if (currentDay < condition.minDay) return false

        // TODO: Check quest/event requirements when quest system is integrated
        // if (condition.requiredQuestId != null) {
        //     val isQuestCompleted = gameState.completedQuests.contains(condition.requiredQuestId)
        //     if (!isQuestCompleted) return false
        // }

        return true
    }
}