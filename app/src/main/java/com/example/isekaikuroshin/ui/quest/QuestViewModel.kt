package com.example.isekaikuroshin.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.QuestEntry
import com.example.isekaikuroshin.data.PersistentDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class QuestUiState(
    val activeQuests: List<QuestEntry> = emptyList(),
    val completedQuests: List<QuestEntry> = emptyList()
)

@HiltViewModel
class QuestViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    val uiState: StateFlow<QuestUiState> = PersistentDataManager.gameData
        .map { gameData ->
            QuestUiState(
                activeQuests = gameData.questData.activeQuests,
                completedQuests = gameData.questData.completedQuests
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QuestUiState()
        )

    fun completeQuest(questId: String) {
        // Quest'i tamamlandı olarak işaretle
        PersistentDataManager.updateQuestData { questData ->
            val updatedActive = questData.activeQuests.map { quest ->
                if (quest.id == questId) quest.copy(isCompleted = true, progress = 1.0f)
                else quest
            }
            val completedQuest = updatedActive.find { it.id == questId }
            val newCompleted = if (completedQuest != null) {
                questData.completedQuests + completedQuest
            } else {
                questData.completedQuests
            }

            // G83: Log quest completion to Journal
            completedQuest?.let { quest ->
                com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                    "I completed the quest: '${quest.title}'"
                )
            }

            questData.copy(
                activeQuests = updatedActive.filter { !it.isCompleted },
                completedQuests = newCompleted
            )
        }
    }
}
