package com.example.isekaikuroshin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PlayerState // DEĞİŞTİRİLDİ: PlayerStatsZ3 -> PlayerState
// import com.example.isekaikuroshin.data.StatusEffectZ2 // KALDIRILDI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


data class DashboardUiState(
    val playerStats: PlayerState = PlayerState(), // DEĞİŞTİRİLDİ
    val statPoints: Int = 0,
    val statusEffects: List<String> = emptyList(), // DEĞİŞTİRİLDİ: StatusEffectZ2 -> String (geçici)
    val weightRatio: Float = 0f,
    val weightStatusDescription: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    val gameStateManager: GameStateManager
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = gameStateManager.gameState
        .map { gameState ->
            DashboardUiState(
                playerStats = gameState.playerState, // DEĞİŞTİRİLDİ
                statPoints = gameState.playerState.statPoints, // DEĞİŞTİRİLDİ
                statusEffects = emptyList(), // DEĞİŞTİRİLDİ (PlayerState'de statusEffects yok, şimdilik boş)
                weightRatio = gameStateManager.getWeightCapacityRatio(), // Şimdilik bırakıldı
                weightStatusDescription = gameStateManager.getWeightStatusDescription() // Şimdilik bırakıldı
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState() // playerStats PlayerState(), statusEffects emptyList() olacak
        )
}
