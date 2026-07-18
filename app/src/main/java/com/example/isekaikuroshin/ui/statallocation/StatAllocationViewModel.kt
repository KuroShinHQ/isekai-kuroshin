package com.example.isekaikuroshin.ui.statallocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PlayerState // DEĞİŞTİRİLDİ: PlayerStatsZ3 -> PlayerState
import com.example.isekaikuroshin.engine.StatType // Bu import doğru (engine.StatType kullanılacak)
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatAllocationUiState(
    val playerStats: PlayerState = PlayerState(), // DEĞİŞTİRİLDİ
    val statPoints: Int = 0,
    // pendingIncreases zaten engine.StatType kullanıyor (yukarıdaki import sayesinde)
    val pendingIncreases: Map<StatType, Int> = emptyMap()
)

@HiltViewModel
class StatAllocationViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    // _pendingIncreases zaten engine.StatType kullanıyor
    private val _pendingIncreases = MutableStateFlow<Map<StatType, Int>>(emptyMap())

    val uiState: StateFlow<StatAllocationUiState> = combine(
        gameStateManager.gameState,
        _pendingIncreases
    ) { gameState, pending ->
        val playerState = gameState.playerState
        android.util.Log.d("StatAllocationVM", "📊 UI State updated:")
        android.util.Log.d("StatAllocationVM", "   Player level: ${playerState.level}")
        android.util.Log.d("StatAllocationVM", "   Stat points: ${playerState.statPoints}")
        android.util.Log.d("StatAllocationVM", "   STR: ${playerState.strength}, VIT: ${playerState.vitality}")
        android.util.Log.d("StatAllocationVM", "   AGI: ${playerState.agility}, INT: ${playerState.intelligence}")
        android.util.Log.d("StatAllocationVM", "   Pending increases: $pending")

        StatAllocationUiState(
            playerStats = playerState,
            statPoints = playerState.statPoints,
            pendingIncreases = pending
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatAllocationUiState() // playerStats PlayerState() olacak
    )

    fun increaseStat(stat: StatType) { // stat parametresi engine.StatType
        // uiState.value.statPoints, PlayerState'den geliyor.
        val currentStatPoints = uiState.value.statPoints
        val pendingSum = _pendingIncreases.value.values.sum()
        val availablePoints = currentStatPoints - pendingSum

        android.util.Log.d("StatAllocationVM", "🔵 increaseStat called - stat: ${stat.name}")
        android.util.Log.d("StatAllocationVM", "   Total stat points: $currentStatPoints")
        android.util.Log.d("StatAllocationVM", "   Pending increases: $pendingSum")
        android.util.Log.d("StatAllocationVM", "   Available points: $availablePoints")

        if (availablePoints > 0) {
            _pendingIncreases.value = _pendingIncreases.value.toMutableMap().apply {
                this[stat] = (this[stat] ?: 0) + 1
            }
            android.util.Log.d("StatAllocationVM", "   ✅ Increased ${stat.name} to: ${_pendingIncreases.value[stat]}")
        } else {
            android.util.Log.d("StatAllocationVM", "   ❌ Cannot increase - no available points!")
        }
    }

    fun decreaseStat(stat: StatType) { // stat parametresi engine.StatType
        val currentAmount = _pendingIncreases.value[stat] ?: 0
        if (currentAmount > 0) {
            _pendingIncreases.value = _pendingIncreases.value.toMutableMap().apply {
                this[stat] = currentAmount - 1
                if (this[stat] == 0) {
                    remove(stat)
                }
            }
        }
    }

    fun confirmAllocation() {
        val allocations = _pendingIncreases.value // Bu Map<engine.StatType, Int>
        android.util.Log.d("StatAllocationVM", "💾 confirmAllocation called")
        android.util.Log.d("StatAllocationVM", "   Allocations to apply: $allocations")

        if (allocations.isNotEmpty()) {
            // GameStateManager.applyStatAllocations artık Map<engine.StatType, Int> bekliyor.
            // Bu çağrı artık uyumlu olmalı.
            android.util.Log.d("StatAllocationVM", "   ✅ Applying allocations to GameStateManager...")
            gameStateManager.applyStatAllocations(allocations)

            // G83: Log stat allocation to Journal
            val statsDescription = allocations.entries.joinToString(", ") { (stat, value) ->
                "${stat.name} +$value"
            }
            com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                "I allocated stat points: $statsDescription"
            )

            android.util.Log.d("StatAllocationVM", "   ✅ Allocations applied successfully")
            resetAllocation()
        } else {
            android.util.Log.d("StatAllocationVM", "   ❌ No allocations to apply (empty)")
        }
    }

    fun resetAllocation() {
        _pendingIncreases.value = emptyMap()
    }
}
