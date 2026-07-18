
package com.example.isekaikuroshin.ui.screens

import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    companion object {
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 3.0f
        private const val ZOOM_STEP = 0.2f
    }

    init {
        loadLocations()
    }

    private fun loadLocations() {
        val locations = listOf(
            LocationMarkerState(
                id = "forest_camp",
                name = "Orman Kampı",
                status = LocationStatus.CURRENT,
                xOffset = 150.dp, // Approximated center
                yOffset = 250.dp
            ),
            LocationMarkerState(
                id = "abandoned_mine",
                name = "Terkedilmiş Maden",
                status = LocationStatus.DISCOVERED,
                xOffset = 100.dp,
                yOffset = 150.dp
            ),
            LocationMarkerState(
                id = "unknown_cave",
                name = "???",
                status = LocationStatus.UNKNOWN,
                xOffset = 200.dp, // Approximated bottom-right
                yOffset = 400.dp
            )
        )
        _uiState.value = MapUiState(locations = locations)
    }

    fun zoomIn() {
        val currentState = _uiState.value
        val newZoom = (currentState.zoomLevel + ZOOM_STEP).coerceAtMost(MAX_ZOOM)
        _uiState.value = currentState.copy(zoomLevel = newZoom)
    }

    fun zoomOut() {
        val currentState = _uiState.value
        val newZoom = (currentState.zoomLevel - ZOOM_STEP).coerceAtLeast(MIN_ZOOM)
        _uiState.value = currentState.copy(zoomLevel = newZoom)
    }

    fun resetToCenter() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            zoomLevel = 1.0f,
            panX = 0f,
            panY = 0f
        )
    }
}
