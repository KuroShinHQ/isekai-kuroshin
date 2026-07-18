package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.isekaikuroshin.data.PerformanceMode
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.DarkDashboardColors
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme
import com.example.isekaikuroshin.utils.PerformanceManager

/**
 * G59: Performance Settings UI
 *
 * Performans ayarları ekranı:
 * - Performance mode presets (HIGH/BALANCED/BATTERY)
 * - FPS limiter (30/45/60)
 * - Thermal monitoring
 * - Auto-throttle on heat
 * - Particle effects toggle
 * - Post-processing toggle
 * - Shadows toggle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val gameData by PersistentDataManager.gameData.collectAsState()
    val performanceSettings = gameData.settingsData.performanceSettings

    // Performance monitoring
    val performanceStats by PerformanceManager.performanceStats.collectAsState()

    // Start monitoring when screen opens
    LaunchedEffect(Unit) {
        PerformanceManager.startMonitoring(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            PerformanceManager.stopMonitoring()
        }
    }

    val dashboardColors = DarkDashboardColors

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = rememberLocalizedText("performance_settings_title"),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = dashboardColors.backgroundDark
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dashboardColors.backgroundDark,
                            dashboardColors.holographicBg
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Real-Time Performance Stats Card
            PerformanceStatsCard(
                stats = performanceStats,
                dashboardColors = dashboardColors
            )

            // Performance Mode Selector
            PerformanceModeCard(
                currentMode = performanceSettings.performanceMode,
                onModeSelected = { mode ->
                    PerformanceManager.applyPerformanceMode(mode)
                },
                dashboardColors = dashboardColors
            )

            // FPS Settings Card
            FpsSettingsCard(
                targetFps = performanceSettings.targetFps,
                onFpsChanged = { fps ->
                    PersistentDataManager.updateSettingsData { settings ->
                        settings.copy(
                            performanceSettings = settings.performanceSettings.copy(
                                targetFps = fps
                            )
                        )
                    }
                },
                dashboardColors = dashboardColors
            )

            // Thermal Management Card
            ThermalManagementCard(
                thermalMonitoringEnabled = performanceSettings.thermalMonitoringEnabled,
                autoThrottleOnHeat = performanceSettings.autoThrottleOnHeat,
                maxThermalThreshold = performanceSettings.maxThermalThreshold,
                onThermalMonitoringChanged = { enabled ->
                    PersistentDataManager.updateSettingsData { settings ->
                        settings.copy(
                            performanceSettings = settings.performanceSettings.copy(
                                thermalMonitoringEnabled = enabled
                            )
                        )
                    }
                },
                onAutoThrottleChanged = { enabled ->
                    PersistentDataManager.updateSettingsData { settings ->
                        settings.copy(
                            performanceSettings = settings.performanceSettings.copy(
                                autoThrottleOnHeat = enabled
                            )
                        )
                    }
                },
                onThresholdChanged = { threshold ->
                    PersistentDataManager.updateSettingsData { settings ->
                        settings.copy(
                            performanceSettings = settings.performanceSettings.copy(
                                maxThermalThreshold = threshold
                            )
                        )
                    }
                },
                dashboardColors = dashboardColors
            )

            // Graphics Settings Card
            GraphicsSettingsCard(
                enableParticleEffects = performanceSettings.enableParticleEffects,
                enablePostProcessing = performanceSettings.enablePostProcessing,
                enableShadows = performanceSettings.enableShadows,
                onParticleEffectsChanged = { enabled ->
                    PersistentDataManager.updateSettingsData { settings ->
                        settings.copy(
                            performanceSettings = settings.performanceSettings.copy(
                                enableParticleEffects = enabled
                            )
                        )
                    }
                },
                onPostProcessingChanged = { enabled ->
                    PersistentDataManager.updateSettingsData { settings ->
                        settings.copy(
                            performanceSettings = settings.performanceSettings.copy(
                                enablePostProcessing = enabled
                            )
                        )
                    }
                },
                onShadowsChanged = { enabled ->
                    PersistentDataManager.updateSettingsData { settings ->
                        settings.copy(
                            performanceSettings = settings.performanceSettings.copy(
                                enableShadows = enabled
                            )
                        )
                    }
                },
                dashboardColors = dashboardColors
            )
        }
    }
}

@Composable
private fun PerformanceStatsCard(
    stats: PerformanceManager.PerformanceStats,
    dashboardColors: DashboardColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 ${rememberLocalizedText("performance_realtime_stats")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // FPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(rememberLocalizedText("performance_current_fps"))
                Text(
                    text = "${stats.currentFps} FPS",
                    fontWeight = FontWeight.Bold
                )
            }

            // CPU Temperature
            val cpuTempColor = when (stats.thermalState) {
                PerformanceManager.ThermalState.CRITICAL -> Color.Red
                PerformanceManager.ThermalState.HOT -> Color(0xFFFF9800)
                PerformanceManager.ThermalState.WARM -> Color(0xFFFFEB3B)
                PerformanceManager.ThermalState.NORMAL -> Color.Green
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(rememberLocalizedText("performance_cpu_temp"))
                Text(
                    text = "${String.format("%.1f", stats.cpuTemperature)}°C (${stats.thermalState})",
                    color = cpuTempColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Battery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(rememberLocalizedText("performance_battery"))
                Text(
                    text = "${stats.batteryLevel}% ${if (stats.isBatteryCharging) "⚡" else ""}",
                    fontWeight = FontWeight.Bold
                )
            }

            // Throttle status
            if (stats.isThrottled) {
                Text(
                    text = "⚠️ ${rememberLocalizedText("performance_throttled")}",
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PerformanceModeCard(
    currentMode: String,
    onModeSelected: (PerformanceMode) -> Unit,
    dashboardColors: DashboardColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = rememberLocalizedText("performance_mode_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // HIGH mode
            PerformanceModeOption(
                mode = PerformanceMode.HIGH,
                title = rememberLocalizedText("performance_mode_high"),
                description = rememberLocalizedText("performance_mode_high_desc"),
                icon = "🔥",
                isSelected = currentMode == "HIGH",
                onClick = { onModeSelected(PerformanceMode.HIGH) }
            )

            // BALANCED mode
            PerformanceModeOption(
                mode = PerformanceMode.BALANCED,
                title = rememberLocalizedText("performance_mode_balanced"),
                description = rememberLocalizedText("performance_mode_balanced_desc"),
                icon = "⚖️",
                isSelected = currentMode == "BALANCED",
                onClick = { onModeSelected(PerformanceMode.BALANCED) }
            )

            // BATTERY mode
            PerformanceModeOption(
                mode = PerformanceMode.BATTERY,
                title = rememberLocalizedText("performance_mode_battery"),
                description = rememberLocalizedText("performance_mode_battery_desc"),
                icon = "🔋",
                isSelected = currentMode == "BATTERY",
                onClick = { onModeSelected(PerformanceMode.BATTERY) }
            )
        }
    }
}

@Composable
private fun PerformanceModeOption(
    mode: PerformanceMode,
    title: String,
    description: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, style = MaterialTheme.typography.titleLarge)
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected")
            }
        }
    }
}

@Composable
private fun FpsSettingsCard(
    targetFps: Int,
    onFpsChanged: (Int) -> Unit,
    dashboardColors: DashboardColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = rememberLocalizedText("performance_fps_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${rememberLocalizedText("performance_fps_current")}: $targetFps FPS",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FpsButton(fps = 30, currentFps = targetFps, onClick = { onFpsChanged(30) })
                FpsButton(fps = 45, currentFps = targetFps, onClick = { onFpsChanged(45) })
                FpsButton(fps = 60, currentFps = targetFps, onClick = { onFpsChanged(60) })
            }
        }
    }
}

@Composable
private fun FpsButton(
    fps: Int,
    currentFps: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (currentFps == fps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text("$fps FPS")
    }
}

@Composable
private fun ThermalManagementCard(
    thermalMonitoringEnabled: Boolean,
    autoThrottleOnHeat: Boolean,
    maxThermalThreshold: Float,
    onThermalMonitoringChanged: (Boolean) -> Unit,
    onAutoThrottleChanged: (Boolean) -> Unit,
    onThresholdChanged: (Float) -> Unit,
    dashboardColors: DashboardColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = rememberLocalizedText("performance_thermal_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Thermal monitoring toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(rememberLocalizedText("performance_thermal_monitoring"))
                Switch(
                    checked = thermalMonitoringEnabled,
                    onCheckedChange = onThermalMonitoringChanged
                )
            }

            // Auto-throttle toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rememberLocalizedText("performance_auto_throttle"))
                    Text(
                        text = rememberLocalizedText("performance_auto_throttle_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = autoThrottleOnHeat,
                    onCheckedChange = onAutoThrottleChanged
                )
            }

            // Threshold slider
            if (thermalMonitoringEnabled) {
                Column {
                    Text(
                        text = "${rememberLocalizedText("performance_thermal_threshold")}: ${String.format("%.0f", maxThermalThreshold)}°C"
                    )
                    Slider(
                        value = maxThermalThreshold,
                        onValueChange = onThresholdChanged,
                        valueRange = 40f..50f,
                        steps = 9
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphicsSettingsCard(
    enableParticleEffects: Boolean,
    enablePostProcessing: Boolean,
    enableShadows: Boolean,
    onParticleEffectsChanged: (Boolean) -> Unit,
    onPostProcessingChanged: (Boolean) -> Unit,
    onShadowsChanged: (Boolean) -> Unit,
    dashboardColors: DashboardColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = rememberLocalizedText("performance_graphics_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Particle effects
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rememberLocalizedText("performance_particle_effects"))
                    Text(
                        text = rememberLocalizedText("performance_particle_effects_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = enableParticleEffects,
                    onCheckedChange = onParticleEffectsChanged
                )
            }

            // Post-processing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rememberLocalizedText("performance_post_processing"))
                    Text(
                        text = rememberLocalizedText("performance_post_processing_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = enablePostProcessing,
                    onCheckedChange = onPostProcessingChanged
                )
            }

            // Shadows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rememberLocalizedText("performance_shadows"))
                    Text(
                        text = rememberLocalizedText("performance_shadows_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = enableShadows,
                    onCheckedChange = onShadowsChanged
                )
            }
        }
    }
}
