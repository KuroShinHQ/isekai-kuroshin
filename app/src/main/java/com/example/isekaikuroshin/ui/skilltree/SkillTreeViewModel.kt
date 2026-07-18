package com.example.isekaikuroshin.ui.skilltree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.SkillNode // Assuming SkillNode is in data
import com.example.isekaikuroshin.data.SkillTreeUiState // Assuming SkillTreeUiState is in data
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SkillTreeViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkillTreeUiState())
    val uiState: StateFlow<SkillTreeUiState> = _uiState.asStateFlow()

    init {
        initializeSkillTree()

        viewModelScope.launch {
            gameStateManager.gameState.collectLatest { gameState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        // DEĞİŞTİRİLDİ: playerStats -> playerState
                        skillPoints = gameState.playerState.statPoints
                    )
                }
            }
        }
    }

    private fun initializeSkillTree() {
        // FB#10: Default sadece "Adaptation" yeteneği - GM journal ilerlemesine göre yeni yetenekler üretecek
        val defaultSkillNodes = listOf(
            SkillNode(
                id = "adaptation_human",
                name = "Adaptation",
                description = "Human adaptability - the core trait of humanity that allows learning and growth in any situation",
                x = 0.5f,
                y = 0.5f,
                parentId = null
            )
        )

        _uiState.update { currentState ->
            currentState.copy(
                skillNodes = defaultSkillNodes,
                unlockedSkills = setOf("adaptation_human") // Default olarak açık
            )
        }
    }

    private fun canUnlockSkill(skill: SkillNode, currentState: SkillTreeUiState): Boolean {
        if (currentState.skillPoints <= 0) return false
        if (currentState.unlockedSkills.contains(skill.id)) return false
        return skill.parentId == null || currentState.unlockedSkills.contains(skill.parentId)
    }

    fun unlockSkill(skillId: String) {
        val currentState = _uiState.value
        val skillToUnlock = currentState.skillNodes.find { it.id == skillId } ?: return

        if (canUnlockSkill(skillToUnlock, currentState)) {
            // Optimistic UI update
            _uiState.update { state ->
                state.copy(
                    unlockedSkills = state.unlockedSkills + skillId
                    // skillPoints will be updated by the gameState flow after GameStateManager modification
                )
            }
            // DEĞİŞTİRİLDİ: spendSkillPoint -> decrementStatPoints (GameStateManager'a eklenecek)
            gameStateManager.decrementStatPoints(1)

            // G83: Log skill unlock to Journal
            com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                "I unlocked the skill: ${skillToUnlock.name}"
            )
        }
    }

    /**
     * GÖREV 3: XP Sistemi
     * Element'e göre Skill Tree node'una XP ekler ve level up kontrol eder
     * @param elementType Element türü ("FIRE", "WATER", etc.)
     * @param xp Kazanılan XP miktarı
     * @return Level up oldu mu?
     */
    fun addXP(elementType: String, xp: Int): Boolean {
        val currentState = _uiState.value
        val skillNode = currentState.skillNodes.find { it.elementType == elementType } ?: return false

        val newXP = skillNode.currentXP + xp
        var newLevel = skillNode.level
        var remainingXP = newXP
        var leveledUp = false

        // Level up kontrolü (birden fazla level atlanabilir)
        while (remainingXP >= skillNode.xpToNextLevel) {
            remainingXP -= skillNode.xpToNextLevel
            newLevel++
            leveledUp = true
        }

        // SkillNode'u güncelle
        val updatedNode = skillNode.copy(
            level = newLevel,
            currentXP = remainingXP,
            xpToNextLevel = calculateXPForNextLevel(newLevel) // Her level daha fazla XP gerektirir
        )

        // UI state'i güncelle
        _uiState.update { state ->
            state.copy(
                skillNodes = state.skillNodes.map { node ->
                    if (node.id == skillNode.id) updatedNode else node
                }
            )
        }

        return leveledUp
    }

    /**
     * Her level için gereken XP'yi hesaplar (üstel artış)
     */
    private fun calculateXPForNextLevel(level: Int): Int {
        return (100 * Math.pow(1.15, (level - 1).toDouble())).toInt()
    }
}
