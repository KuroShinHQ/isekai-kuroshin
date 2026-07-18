
package com.example.isekaikuroshin.ui.screens.training

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the UI state for the Mana Core cultivation screen.
 */
data class ManaCoreUiState(
    // Left Panel Metrics
    val currentMana: Int = 1580,
    val maxMana: Int = 3200,
    val manaRegenRate: Float = 24.5f,
    val corePurity: Float = 0.87f, // Represented as a float 0.0 to 1.0
    val coreStageName: String = "Gümüş Çekirdek (Orta Faz)",
    val elementalAffinities: List<ElementalAffinity> = emptyList(),

    // Right Panel Timeline
    val timelineState: TimelineState = TimelineState(),

    // Central Visualization
    val currentCoreStageAsset: Int = 0 // Placeholder for drawable resource ID
)

/**
 * Represents an elemental affinity with its corresponding icon.
 */
data class ElementalAffinity(
    val name: String,
    val icon: ImageVector
)

/**
 * Represents the state of the core progression timeline.
 */
data class TimelineState(
    val currentStage: String = "Gümüş Çekirdek",
    val subStages: List<String> = listOf("Erken", "Orta", "Geç", "Zirve"),
    val currentSubStageIndex: Int = 1, // "Orta" is the second item
    val nextMajorStage: String = "Altın Çekirdek (Kilitli)"
)
