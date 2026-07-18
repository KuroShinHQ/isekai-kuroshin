
package com.example.isekaikuroshin.ui.screens.training

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.isekaikuroshin.data.GameStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class QiCultivationViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiCultivationUiState())
    val uiState: StateFlow<QiCultivationUiState> = _uiState.asStateFlow()

    init {
        loadInitialCultivationState()
    }

    private fun loadInitialCultivationState() {
        val realms = listOf(
            RealmInfo("Qi Toplanma / Arındırma", "Tohumun Atılması", isUnlocked = true),
            RealmInfo("Temel Kurma", "Köprüler ve Yolların İnşası", isUnlocked = false),
            RealmInfo("Altın Çekirdek", "İçsel Güneşin Doğuşu", isUnlocked = false),
            RealmInfo("Yükselen Ruh", "İkinci Bir Ben'in Doğuşu", isUnlocked = false),
            RealmInfo("Ruha Dönüşüm", "Doğa Yasalarına Dokunmak", isUnlocked = false),
            RealmInfo("Boşluk Arındırma", "Hiçlik ve Varlık Arasında", isUnlocked = false),
            RealmInfo("Beden Bütünleşme", "Üçlü Birliğe Ulaşmak", isUnlocked = false),
            RealmInfo("Büyük Yükseliş", "Ölümsüzlük Tribülasyonu ve Geçiş", isUnlocked = false)
        )

        _uiState.value = QiCultivationUiState(
            currentStageName = realms[0].name,
            qiLevel = 1,
            maxQi = 100,
            currentQi = 10,
            qiPurity = 0.75f,
            cultivationRealms = realms,
            currentRealmIndex = 0
        )
    }

    fun onMeditateClicked() {
        Log.d("QiViewModel", "Meditasyon butonuna tıklandı.")
        _uiState.update {
            it.copy(currentQi = (it.currentQi + 10).coerceAtMost(it.maxQi))
        }

        // G90: GameStateManager integration - XP kazanımı
        gameStateManager.addExperience(10) // 10 XP

        // G83: Log Qi meditation to Journal
        com.example.isekaikuroshin.utils.EventLogger.logCultivation(
            type = "Qi Meditation",
            breakthroughLevel = null,
            duration = 15,
            details = "Meditated to gather Qi and refine spiritual energy. (+10 XP)"
        )
    }

    fun onBreakthroughClicked() {
        Log.d("QiViewModel", "Atılım Yap butonuna tıklandı.")

        val previousRealmIndex = _uiState.value.currentRealmIndex

        _uiState.update { currentState ->
            val nextIndex = currentState.currentRealmIndex + 1
            if (nextIndex < currentState.cultivationRealms.size) {
                currentState.copy(
                    currentRealmIndex = nextIndex,
                    currentStageName = currentState.cultivationRealms[nextIndex].name
                )
            } else {
                currentState // Already at the last realm
            }
        }

        // G83: Log Qi breakthrough to Journal (only if successful)
        val newRealmIndex = _uiState.value.currentRealmIndex
        if (newRealmIndex > previousRealmIndex) {
            // G90: GameStateManager integration - XP kazanımı
            val xpReward = 50 + (newRealmIndex * 20) // Seviye başına artan XP
            gameStateManager.addExperience(xpReward)

            com.example.isekaikuroshin.utils.EventLogger.logCultivation(
                type = "Qi Cultivation Breakthrough",
                breakthroughLevel = newRealmIndex,
                duration = 60,
                details = "Breakthrough to realm ${_uiState.value.currentStageName}! Achieved new cultivation level. (+${xpReward} XP)"
            )
        }
    }

    fun onTechniquesClicked() {
        Log.d("QiViewModel", "Teknikler ekranı açılacak.")
        // No state change for now, just logging.
    }
}
