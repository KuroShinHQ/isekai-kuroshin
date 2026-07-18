package com.example.isekaikuroshin.ui.screens.training

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit // Ice
import androidx.compose.material.icons.filled.FlashOn // Lightning
import androidx.lifecycle.ViewModel
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.GameStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ManaCoreViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManaCoreUiState())
    val uiState: StateFlow<ManaCoreUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        _uiState.value = ManaCoreUiState(
            currentMana = 1580,
            maxMana = 3200,
            manaRegenRate = 24.5f,
            corePurity = 0.87f,
            coreStageName = "Gümüş Çekirdek (Orta Faz)",
            elementalAffinities = listOf(
                ElementalAffinity("Buz", Icons.Default.AcUnit),
                ElementalAffinity("Yıldırım", Icons.Default.FlashOn)
            ),
            timelineState = TimelineState(
                currentStage = "Gümüş Çekirdek",
                subStages = listOf("Erken", "Orta", "Geç", "Zirve"),
                currentSubStageIndex = 1,
                nextMajorStage = "Altın Çekirdek (Kilitli)"
            ),
            currentCoreStageAsset = R.mipmap.ic_launcher_light // Placeholder (TODO: mana_core_stage_2)
        )
    }

    fun onMeditateClicked() {
        Log.d("ManaCoreViewModel", "Meditasyon butonuna tıklandı.")
        _uiState.update {
            it.copy(currentMana = (it.currentMana + 10).coerceAtMost(it.maxMana))
        }

        // G90: GameStateManager integration - XP kazanımı
        gameStateManager.addExperience(8) // 8 XP

        // G83: Log mana core meditation to Journal
        com.example.isekaikuroshin.utils.EventLogger.logCultivation(
            type = "Mana Core Meditation",
            breakthroughLevel = null,
            duration = 15,
            details = "Meditated to restore mana and enhance mana core stability. (+8 XP)"
        )
    }

    fun onCondenseClicked() {
        Log.d("ManaCoreViewModel", "Yoğunlaştır butonuna tıklandı.")

        val previousPurity = _uiState.value.corePurity

        _uiState.update {
            it.copy(corePurity = (it.corePurity + 0.01f).coerceAtMost(1.0f))
        }

        // G90: GameStateManager integration - XP kazanımı
        gameStateManager.addExperience(12) // 12 XP

        // G83: Log mana core condensation to Journal
        val newPurity = _uiState.value.corePurity
        com.example.isekaikuroshin.utils.EventLogger.logCultivation(
            type = "Mana Core Condensation",
            breakthroughLevel = if (newPurity >= 0.99f && previousPurity < 0.99f) 1 else null,
            duration = 20,
            details = "Condensed mana core to increase purity: ${(newPurity * 100).toInt()}%. (+12 XP)"
        )
    }

    fun onExpandClicked() {
        Log.d("ManaCoreViewModel", "Çekirdeği Genişlet butonuna tıklandı.")
        _uiState.update {
            it.copy(maxMana = it.maxMana + 5)
        }

        // G90: GameStateManager integration - XP kazanımı
        gameStateManager.addExperience(15) // 15 XP

        // G83 P4: EventLogger integration
        com.example.isekaikuroshin.utils.EventLogger.logCultivation(
            type = "Mana Core Expansion",
            duration = 15,
            details = "Expanded mana core capacity by channeling spiritual energy. New max mana: ${_uiState.value.maxMana}. This increases my mana pool. (+15 XP)"
        )
    }

    fun onPurifyClicked() {
        Log.d("ManaCoreViewModel", "Saflaştır butonuna tıklandı.")

        _uiState.update {
            it.copy(corePurity = (it.corePurity + 0.02f).coerceAtMost(1.0f))
        }

        // G90: GameStateManager integration - XP kazanımı
        gameStateManager.addExperience(18) // 18 XP

        // G83 P4: EventLogger integration
        val newPurity = _uiState.value.corePurity
        com.example.isekaikuroshin.utils.EventLogger.logCultivation(
            type = "Mana Core Purification",
            duration = 20,
            details = "Purified mana core to ${(newPurity * 100).toInt()}% purity. Removing impurities and refining mana quality. This improves spell casting efficiency. (+18 XP)"
        )
    }
}