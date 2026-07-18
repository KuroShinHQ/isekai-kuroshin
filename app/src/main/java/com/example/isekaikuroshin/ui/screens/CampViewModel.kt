package com.example.isekaikuroshin.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.ai.GlobalAIManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CampViewModel @Inject constructor(
    val gameStateManager: GameStateManager  // public yaptım (StatusPanel erişsin)
) : ViewModel() {

    /**
     * Manuel save işlemi - KarmaBasedContentEngine'i tetikler
     * TODO-FIX-07: PersistentDataManager senkronizasyonu eklendi
     */
    fun saveGame() {
        viewModelScope.launch {
            try {
                val currentPlayerState = gameStateManager.gameState.value.playerState

                // 1. GameStateManager'ı kaydet (Room Database)
                gameStateManager.updatePlayerState(currentPlayerState)

                // 2. TODO-FIX-07: PersistentDataManager'ı da senkronize et (EncryptedSharedPreferences)
                // Bu, UserEntryViewModel'in kayıt kontrolünün çalışması için kritik!
                PersistentDataManager.updatePlayerData { currentData ->
                    currentData.copy(
                        name = currentPlayerState.playerName,
                        level = currentPlayerState.level,
                        experience = currentPlayerState.experience.toLong(),
                        moralityScore = currentPlayerState.moralityScore,
                        isAlive = currentPlayerState.currentHealth > 0
                    )
                }

                GameLogger.logSystem("🎮 Manuel SAVE GAME tetiklendi - KarmaBasedContentEngine çalışacak")
                GameLogger.logSystem("✅ PersistentDataManager senkronize edildi (TODO-FIX-07)")
            } catch (e: Exception) {
                GameLogger.logError("CampViewModel", "Save işlemi başarısız: ${e.message}")
            }
        }
    }

    /**
     * Rest işlemi - Can ve mana yenilemesi
     */
    fun restAtCamp() {
        viewModelScope.launch {
            try {
                val currentPlayerState = gameStateManager.gameState.value.playerState
                val newPlayerState = currentPlayerState.copy(
                    currentHealth = currentPlayerState.maxHealth,
                    currentMana = currentPlayerState.maxMana
                )

                gameStateManager.updatePlayerState(newPlayerState)
                GameLogger.logSystem("🏕️ Kampta dinlenildi - Can ve mana yenilendi")

                // G83: Log rest activity to Journal
                com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                    "I rested at camp and recovered my health and mana"
                )
            } catch (e: Exception) {
                GameLogger.logError("CampViewModel", "Rest işlemi başarısız: ${e.message}")
            }
        }
    }

    /**
     * EMERGENCY DEBUG: Fix dead player issue
     */
    fun emergencyResurrectPlayer() {
        viewModelScope.launch {
            try {
                PersistentDataManager.resurrectPlayer()
                GameLogger.logSystem("🔧 EMERGENCY FIX: Player resurrected - isAlive set to true")
            } catch (e: Exception) {
                GameLogger.logError("CampViewModel", "Resurrection failed: ${e.message}")
            }
        }
    }

    /**
     * End of Day - Trigger memory synthesis (Strateji #3)
     * Called when player rests/sleeps at camp
     */
    fun endOfDay() {
        viewModelScope.launch {
            try {
                val currentDay = gameStateManager.gameState.value.currentDay

                GameLogger.logSystem("🌙 End of Day $currentDay triggered - Starting memory synthesis...")

                // Trigger Strateji #3 memory synthesis
                GlobalAIManager.synthesizeEndOfDayMemory(currentDay)

                GameLogger.logSystem("✅ End of Day $currentDay completed - Memory synthesis finished")
            } catch (e: Exception) {
                GameLogger.logError("CampViewModel", "End of day process failed: ${e.message}", e)
            }
        }
    }
}