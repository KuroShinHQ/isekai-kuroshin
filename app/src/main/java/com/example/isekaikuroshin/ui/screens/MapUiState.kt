
package com.example.isekaikuroshin.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Represents the UI state for the Map screen.
 */
data class MapUiState(
    val locations: List<LocationMarkerState> = emptyList(),
    val zoomLevel: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

/**
 * Represents the state for a single location marker on the map.
 */
data class LocationMarkerState(
    val id: String,
    val name: String,
    val status: LocationStatus,
    val xOffset: Dp,
    val yOffset: Dp
)

/**
 * Defines the status of a location on the map, which determines its color and description.
 */
enum class LocationStatus(val color: Color, val description: String) {
    CURRENT(Color(0xFF00FF87), "Mevcut Konum"),
    DISCOVERED(Color(0xFF00B2FF), "Keşfedilmiş"),
    UNKNOWN(Color.Gray.copy(alpha = 0.7f), "Bilinmeyen")
}
