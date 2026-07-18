package com.example.isekaikuroshin.ui.screens.training

/**
 * Represents the UI state for the Qi Cultivation and Dantian screen.
 */
data class QiCultivationUiState(
    // Dantian Status
    val currentStageName: String = "Qi Toplanma",
    val qiLevel: Int = 1,
    val maxQi: Int = 100,
    val currentQi: Int = 10,
    val qiPurity: Float = 0.75f, // 0.0f - 1.0f purity

    // Realm Progression
    val cultivationRealms: List<RealmInfo> = emptyList(), // List of all realms
    val currentRealmIndex: Int = 0 // Index of the active realm
)

/**
 * Holds information about a single cultivation realm/stage.
 */
data class RealmInfo(
    val name: String,
    val description: String,
    val isUnlocked: Boolean
)