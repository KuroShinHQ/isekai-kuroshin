package com.example.isekaikuroshin.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.isekaikuroshin.data.Location
import com.example.isekaikuroshin.data.LocationType
import com.example.isekaikuroshin.data.rememberLocalizedText
import kotlin.random.Random
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBackPressed: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLocationsList by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = rememberLocalizedText("world_map"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = rememberLocalizedText("discovered_places"),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // List view button
                        IconButton(
                            onClick = { showLocationsList = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = rememberLocalizedText("location_list"),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map canvas
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    MapCanvas(
                        locations = uiState.knownLocations,
                        currentLocationId = uiState.currentLocationId,
                        selectedLocation = uiState.selectedLocation,
                        onLocationSelected = viewModel::onLocationSelected,
                        onLocationDeselected = viewModel::onLocationDeselected
                    )

                    // Current location info
                    uiState.knownLocations.find { it.id == uiState.currentLocationId }?.let { currentLocation ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${rememberLocalizedText("current_location")}: ${rememberLocalizedText(currentLocation.name)}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Back button - Modern floating design
        FloatingActionButton(
            onClick = {
                Log.d("MapScreen", "🔙 Back button clicked in MapScreen")
                onBackPressed()
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = rememberLocalizedText("back"),
                modifier = Modifier.size(28.dp)
            )
        }

        // Location details dialog
        uiState.selectedLocation?.let { location ->
            LocationDetailsDialog(
                location = location,
                isCurrentLocation = location.id == uiState.currentLocationId,
                playerLevel = uiState.playerLevel,
                onDismiss = viewModel::onLocationDeselected,
                onTravel = { viewModel.onTravelToLocation(location.id) }
            )
        }

        // Locations list dialog
        if (showLocationsList) {
            LocationsListDialog(
                locations = uiState.knownLocations,
                currentLocationId = uiState.currentLocationId,
                onDismiss = { showLocationsList = false },
                onLocationSelected = { location ->
                    showLocationsList = false
                    viewModel.onLocationSelected(location)
                }
            )
        }
    }
}

@Composable
private fun MapCanvas(
    locations: List<Location>,
    currentLocationId: String,
    selectedLocation: Location?,
    onLocationSelected: (Location) -> Unit,
    onLocationDeselected: () -> Unit
) {
    val density = LocalDensity.current

    // Generate consistent positions for locations
    val locationPositions = remember(locations) {
        val positions = mutableMapOf<String, Offset>()
        val random = Random(locations.hashCode())
        locations.forEach { location ->
            positions[location.id] = Offset(
                x = 0.1f + random.nextFloat() * 0.8f, // Keep away from edges
                y = 0.1f + random.nextFloat() * 0.8f
            )
        }
        positions
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(locations) {
                detectTapGestures { offset ->
                    val canvasWidth = size.width.toFloat()
                    val canvasHeight = size.height.toFloat()
                    val clickRadiusPx = with(density) { 25.dp.toPx() }

                    val clickedLocation = locations.find { location ->
                        val position = locationPositions[location.id] ?: return@find false
                        val locationCenter = Offset(
                            position.x * canvasWidth,
                            position.y * canvasHeight
                        )
                        (offset - locationCenter).getDistance() <= clickRadiusPx
                    }

                    if (clickedLocation != null) {
                        if (selectedLocation?.id == clickedLocation.id) {
                            onLocationDeselected()
                        } else {
                            onLocationSelected(clickedLocation)
                        }
                    } else {
                        onLocationDeselected()
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // TODO-G114.3: Draw fog-of-war cleared circles (only for discovered locations)
        locations.filter { it.isDiscovered }.forEach { location ->
            val position = locationPositions[location.id] ?: return@forEach
            val center = Offset(
                x = position.x * canvasWidth,
                y = position.y * canvasHeight
            )

            // Clear fog around discovered location (visibility radius)
            val fogClearRadius = 80f // Visibility radius around location
            drawCircle(
                color = Color(0x00FFFFFF), // Transparent (clears fog)
                radius = fogClearRadius,
                center = center,
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Subtle fade edge for cleared area
            drawCircle(
                color = Color(0x40FFFFFF), // Semi-transparent white glow
                radius = fogClearRadius - 5f,
                center = center
            )
        }

        // TODO-G114.3: Draw fog-of-war overlay (covers undiscovered areas)
        drawRect(
            color = Color(0x99000000), // Dark fog overlay
            size = size
        )

        locations.filter { it.isDiscovered }.forEach { location ->
            val position = locationPositions[location.id] ?: return@forEach
            val isCurrentLocation = location.id == currentLocationId
            val isSelected = selectedLocation?.id == location.id

            val center = Offset(
                x = position.x * canvasWidth,
                y = position.y * canvasHeight
            )

            // Location styling based on type and state
            val (baseColor, glowColor, radius) = when {
                isCurrentLocation -> Triple(
                    Color(0xFFFFD700),
                    Color(0xFFFF8C00),
                    22f
                )
                location.type == LocationType.CITY -> Triple(
                    Color(0xFF4CAF50),
                    Color(0xFF2E7D32),
                    18f
                )
                location.type == LocationType.DUNGEON -> Triple(
                    Color(0xFFFF5722),
                    Color(0xFFD32F2F),
                    16f
                )
                location.type == LocationType.SPECIAL -> Triple(
                    Color(0xFF9C27B0),
                    Color(0xFF7B1FA2),
                    16f
                )
                else -> Triple(
                    Color(0xFF2196F3),
                    Color(0xFF1565C0),
                    14f
                )
            }

            // Glow effect for current or selected location
            if (isCurrentLocation || isSelected) {
                drawCircle(
                    color = glowColor.copy(alpha = 0.4f),
                    radius = radius + 12f,
                    center = center
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.2f),
                    radius = radius + 18f,
                    center = center
                )
            }

            // Location border and fill
            drawCircle(
                color = glowColor,
                radius = radius + 2f,
                center = center
            )
            drawCircle(
                color = baseColor,
                radius = radius,
                center = center
            )

            // Inner highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius * 0.6f,
                center = center
            )

            // Danger level indicator
            if (location.dangerLevel > 3) {
                drawCircle(
                    color = Color.Red,
                    radius = 4f,
                    center = Offset(center.x + radius * 0.7f, center.y - radius * 0.7f)
                )
            }
        }
    }
}

@Composable
private fun LocationDetailsDialog(
    location: Location,
    isCurrentLocation: Boolean,
    playerLevel: Int,
    onDismiss: () -> Unit,
    onTravel: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Location name and type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = getLocationTypeColor(location.type)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rememberLocalizedText(location.name),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = getLocationTypeText(location.type),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = rememberLocalizedText(location.description),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LocationStatCard(
                        label = rememberLocalizedText("danger"),
                        value = "${location.dangerLevel}/10",
                        color = when {
                            location.dangerLevel <= 3 -> Color(0xFF4CAF50)
                            location.dangerLevel <= 6 -> Color(0xFFFF9800)
                            else -> Color(0xFFFF5722)
                        }
                    )
                    LocationStatCard(
                        label = rememberLocalizedText("required_level"),
                        value = "${location.requiredLevel}",
                        color = if (playerLevel >= location.requiredLevel)
                            Color(0xFF4CAF50) else Color(0xFFFF5722)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(rememberLocalizedText("close"))
                    }

                    if (!isCurrentLocation) {
                        val canTravel = playerLevel >= location.requiredLevel
                        Button(
                            onClick = onTravel,
                            enabled = canTravel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color.Gray
                            )
                        ) {
                            Text(
                                text = if (canTravel) rememberLocalizedText("travel") else rememberLocalizedText("level_insufficient"),
                                color = Color.White
                            )
                        }
                    } else {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = rememberLocalizedText("current_location"),
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun LocationsListDialog(
    locations: List<Location>,
    currentLocationId: String,
    onDismiss: () -> Unit,
    onLocationSelected: (Location) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = rememberLocalizedText("known_locations"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(locations) { location ->
                        LocationListItem(
                            location = location,
                            isCurrentLocation = location.id == currentLocationId,
                            onClick = { onLocationSelected(location) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(rememberLocalizedText("close"), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LocationListItem(
    location: Location,
    isCurrentLocation: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentLocation)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = getLocationTypeColor(location.type),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rememberLocalizedText(location.name),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getLocationTypeText(location.type),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (isCurrentLocation) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = rememberLocalizedText("current_location"),
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getLocationTypeColor(type: LocationType): Color {
    return when (type) {
        LocationType.CITY -> Color(0xFF4CAF50)
        LocationType.TOWN -> Color(0xFF8BC34A)
        LocationType.VILLAGE -> Color(0xFFCDDC39)
        LocationType.WILDERNESS -> Color(0xFF2196F3)
        LocationType.DUNGEON -> Color(0xFFFF5722)
        LocationType.RUIN -> Color(0xFF795548)
        LocationType.CAVE -> Color(0xFF607D8B)
        LocationType.SPECIAL -> Color(0xFF9C27B0)
    }
}

@Composable
private fun getLocationTypeText(type: LocationType): String {
    return when (type) {
        LocationType.CITY -> rememberLocalizedText("city")
        LocationType.TOWN -> rememberLocalizedText("town")
        LocationType.VILLAGE -> rememberLocalizedText("village")
        LocationType.WILDERNESS -> rememberLocalizedText("wilderness")
        LocationType.DUNGEON -> rememberLocalizedText("dungeon")
        LocationType.RUIN -> rememberLocalizedText("ruin")
        LocationType.CAVE -> rememberLocalizedText("cave")
        LocationType.SPECIAL -> rememberLocalizedText("special")
    }
}