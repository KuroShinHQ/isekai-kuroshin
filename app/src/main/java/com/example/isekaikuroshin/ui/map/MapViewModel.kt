package com.example.isekaikuroshin.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.Location
import com.example.isekaikuroshin.data.LocationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class MapUiState(
    val knownLocations: List<Location> = emptyList(),
    val currentLocationId: String = "",
    val playerLevel: Int = 1,
    val selectedLocation: Location? = null,
    val canTravel: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        gameStateManager.gameState
            .onEach { gameState ->
                _uiState.value = _uiState.value.copy(
                    knownLocations = gameState.knownLocations,
                    currentLocationId = gameState.currentLocationId,
                    playerLevel = gameState.playerState.level
                )
            }
            .launchIn(viewModelScope)
    }

    fun onLocationSelected(location: Location) {
        _uiState.value = _uiState.value.copy(
            selectedLocation = location
        )
    }

    fun onLocationDeselected() {
        _uiState.value = _uiState.value.copy(
            selectedLocation = null
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun onTravelToLocation(locationId: String) {
        // G83 P4: EventLogger integration
        val selectedLoc = _uiState.value.selectedLocation
        if (selectedLoc != null) {
            com.example.isekaikuroshin.utils.EventLogger.logExploration(
                location = selectedLoc.name,
                details = "Traveled to ${selectedLoc.name} (${selectedLoc.type}). Exploring new area and searching for quests, resources, and encounters."
            )
        }

        // Travel logic would be implemented here via GameStateManager
        // Example: gameStateManager.travelToLocation(locationId)
        // The actual implementation would depend on backend travel mechanics
    }
}