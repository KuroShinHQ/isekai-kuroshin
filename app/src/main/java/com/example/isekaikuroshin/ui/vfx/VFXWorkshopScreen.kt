package com.example.isekaikuroshin.ui.vfx

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.GestureType
import com.example.isekaikuroshin.data.VFXLevel
import com.example.isekaikuroshin.ui.vfx.HandReferencePoint
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * VFX Workshop Screen - Kadim Mühür Tarzında Tasarım
 *
 * ⚡⚡⚡ NİHAİ DÜZELTME: Shared ViewModel (Default Parameter YOK!)
 * Bu ekran "aptal" bir Composable'dır. Kendi ViewModel'i YOKTUR.
 * Tüm veri ve fonksiyonları NavGraph/MainActivity'den gelen paylaşılan ViewModel'den alır.
 *
 * Renk Paleti (SealPracticeScreen'den):
 * - Primary (Mor): #6A4C93
 * - Success (Yeşil): #4CAF50
 * - Background: #1A1A2E, #0F0F1E
 * - Card: #2A2A3E (alpha 0.8)
 */
@Composable
fun VFXWorkshopScreen(
    navController: NavController,
    viewModel: VFXWorkshopViewModel  // ⚡ NO DEFAULT! NavGraph/MainActivity'den gelmelidir
) {
    val uiState by viewModel.uiState.collectAsState()

    // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Repository'den reaktif veri
    val creationSeal by viewModel.creationSeal.collectAsState()
    val directionSeal by viewModel.directionSeal.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // ⚡⚡⚡ DÜZELTME: Kaydırılabilir Column ekle
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())  // SCROLL EKLENDİ!
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Başlık
            Text(
                text = rememberLocalizedText("vfx_test_workshop"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Color.White
            )

            Text(
                text = rememberLocalizedText("test_water_effects"),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Efekt Seviyesi Seçimi
            VFXLevelSelectorCard(
                selectedLevel = uiState.selectedVFXLevel,
                onLevelSelected = viewModel::selectVFXLevel
            )

            // Hassasiyet Ayarı
            SensitivitySliderCard(
                sensitivity = uiState.sensitivityThreshold,
                onSensitivityChanged = viewModel::setSensitivityThreshold
            )

            // Kalibrasyon Paneli
            // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Açı sayılarını direkt Seal'dan al
            CalibrationPanelCard(
                isGestureCreationCalibrated = (creationSeal?.templateLandmarksList?.size ?: 0) > 0,
                isGestureDirectionCalibrated = (directionSeal?.templateLandmarksList?.size ?: 0) > 0,
                creationAngleCount = creationSeal?.templateLandmarksList?.size ?: 0,
                directionAngleCount = directionSeal?.templateLandmarksList?.size ?: 0,
                onCalibrateCreation = {
                    navController.navigate("vfx_gesture_calibration/CREATION")
                },
                onCalibrateDirection = {
                    navController.navigate("vfx_gesture_calibration/DIRECTION")
                }
            )

            // ⚡⚡⚡ YENİ: VFX Konumlandırma Kartı (3D Offset)
            VFXPositioningCard(
                creationOffsetConfig = uiState.creationOffsetConfig,
                directionOffsetConfig = uiState.directionOffsetConfig,
                onAddCreationOffset = {
                    navController.navigate("vfx_offset_calibration/CREATION")
                },
                onAddDirectionOffset = {
                    navController.navigate("vfx_offset_calibration/DIRECTION")
                },
                onDeleteCreationOffset = {
                    viewModel.deleteOffsetConfiguration(com.example.isekaikuroshin.data.GestureType.CREATION)
                },
                onDeleteDirectionOffset = {
                    viewModel.deleteOffsetConfiguration(com.example.isekaikuroshin.data.GestureType.DIRECTION)
                }
            )

            // ⚡⚡⚡ BÖLÜM B: YENİ - Davranış Ayarları Kartı
            BehaviorSettingsCard(
                uiState = uiState,
                onColorChange = viewModel::updateVfxColor,
                onSizeChange = viewModel::updateVfxSize,
                onShapeChange = viewModel::updateVfxShape,
                onAlphaChange = viewModel::updateVfxAlpha,
                onOffsetChange = viewModel::updateVfxOffset,
                onSpeedChange = viewModel::updateVfxSpeed,
                onReferencePointChange = viewModel::updateReferencePoint,
                onGhostHandToggle = viewModel::toggleGhostHand,
                onFpsMonitorToggle = viewModel::toggleFpsMonitor
            )

            // ⚡⚡⚡ DÜZELTME: weight() removed (verticalScroll ile uyumsuz)
            Spacer(modifier = Modifier.height(20.dp))

            // ⚡⚡⚡ YENİ: Kalibrasyon Özeti Kartı (Test Başlat'tan önce)
            CalibrationSummaryCard(
                selectedVFXLevel = uiState.selectedVFXLevel,
                sensitivityThreshold = uiState.sensitivityThreshold,
                creationAngleCount = creationSeal?.templateLandmarksList?.size ?: 0,
                directionAngleCount = directionSeal?.templateLandmarksList?.size ?: 0,
                isReadyForTest = viewModel.isGestureCalibrated(GestureType.CREATION) &&
                               viewModel.isGestureCalibrated(GestureType.DIRECTION)
            )

            // Testi Başlat Butonu
            // ⚡⚡⚡ BÖLÜM B: Her iki hareketin de EN AZ 1 AÇIYA sahip olup olmadığını kontrol et
            VFXTestButtonCard(
                isEnabled = viewModel.isGestureCalibrated(GestureType.CREATION) &&
                           viewModel.isGestureCalibrated(GestureType.DIRECTION),
                onStartTest = {
                    viewModel.startVFCTest()
                    navController.navigate("vfx_camera")
                }
            )

            // ⚡⚡⚡ YENİ: Kalibrasyon Merkezi'ne Kaydet Butonu
            if (viewModel.isGestureCalibrated(GestureType.CREATION) &&
                viewModel.isGestureCalibrated(GestureType.DIRECTION)) {
                SaveToPracticeSealButton(
                    onSave = {
                        viewModel.saveCompleteVFXTestAsPracticeSeals()
                    }
                )
            }

            // Alt boşluk (scroll için)
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Geri Dön Butonu (Sol Alt Köşe)
        FloatingActionButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            containerColor = Color(0xFF6A4C93),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri Dön",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Efekt Seviyesi Seçim Kartı (Toggle Button Stili)
 * ⚡⚡⚡ BÖLÜM B: Dinamik başlık (seçili seviyeyi göster)
 */
@Composable
private fun VFXLevelSelectorCard(
    selectedLevel: VFXLevel,
    onLevelSelected: (VFXLevel) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ⚡⚡⚡ BÖLÜM B: Dinamik başlık
            val levelName = when (selectedLevel) {
                VFXLevel.SPRITE -> rememberLocalizedText("sprite")
                VFXLevel.PARTICLE -> rememberLocalizedText("particle")
                VFXLevel.METABALL -> rememberLocalizedText("liquid")
            }
            Text(
                text = "⚡ ${rememberLocalizedText("effect_level")} $levelName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            // Toggle Button Group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VFXLevelToggleButton(
                    level = VFXLevel.SPRITE,
                    currentLevel = selectedLevel,
                    label = rememberLocalizedText("sprite"),
                    emoji = "💧",
                    onSelected = onLevelSelected,
                    modifier = Modifier.weight(1f)
                )
                VFXLevelToggleButton(
                    level = VFXLevel.PARTICLE,
                    currentLevel = selectedLevel,
                    label = rememberLocalizedText("particle"),
                    emoji = "✨",
                    onSelected = onLevelSelected,
                    modifier = Modifier.weight(1f)
                )
                VFXLevelToggleButton(
                    level = VFXLevel.METABALL,
                    currentLevel = selectedLevel,
                    label = rememberLocalizedText("liquid"),
                    emoji = "🌊",
                    onSelected = onLevelSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Toggle Button (Kadim Mühür Stili)
 */
@Composable
private fun RowScope.VFXLevelToggleButton(
    level: VFXLevel,
    currentLevel: VFXLevel,
    label: String,
    emoji: String,
    onSelected: (VFXLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = level == currentLevel
    val containerColor = if (isSelected) {
        Color(0xFF6A4C93) // Mor (Primary)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }

    Surface(
        onClick = { onSelected(level) },
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        shadowElevation = if (isSelected) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Hassasiyet Slider Kartı (Mor Tema)
 * ⚡⚡⚡ BÖLÜM B: Dinamik başlık (seçili hassasiyeti göster)
 */
@Composable
private fun SensitivitySliderCard(
    sensitivity: Float,
    onSensitivityChanged: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ⚡⚡⚡ BÖLÜM B: Dinamik başlık
                Text(
                    text = "🎯 ${rememberLocalizedText("sensitivity")} ${(sensitivity * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                // Yüzdeyi kaldırdık (başlıkta gösterildiği için)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mor Slider
            Slider(
                value = sensitivity,
                onValueChange = { onSensitivityChanged(it) },
                valueRange = 0.5f..0.95f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6A4C93),
                    activeTrackColor = Color(0xFF6A4C93),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = rememberLocalizedText("easy_50"),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = rememberLocalizedText("hard_95"),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Kalibrasyon Paneli (Kadim Mühür Kart Stili)
 */
@Composable
private fun CalibrationPanelCard(
    isGestureCreationCalibrated: Boolean,
    isGestureDirectionCalibrated: Boolean,
    creationAngleCount: Int,
    directionAngleCount: Int,
    onCalibrateCreation: () -> Unit,
    onCalibrateDirection: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🖐️ ${rememberLocalizedText("gesture_calibration")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            Text(
                text = rememberLocalizedText("calibrate_both_gestures"),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            // Hareket 1: Yaratma
            CalibrationItemRow(
                icon = "🖐️",
                label = rememberLocalizedText("creation"),
                description = rememberLocalizedText("open_hand_gesture_desc"),
                isCalibrated = isGestureCreationCalibrated,
                angleCount = creationAngleCount,
                onClick = onCalibrateCreation
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // Hareket 2: Yönlendirme
            CalibrationItemRow(
                icon = "✌️",
                label = rememberLocalizedText("direction"),
                description = rememberLocalizedText("peace_sign_desc"),
                isCalibrated = isGestureDirectionCalibrated,
                angleCount = directionAngleCount,
                onClick = onCalibrateDirection
            )
        }
    }
}

/**
 * Kalibrasyon Item Satırı
 */
@Composable
private fun CalibrationItemRow(
    icon: String,
    label: String,
    description: String,
    isCalibrated: Boolean,
    angleCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 32.sp
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = if (isCalibrated) {
                    Color(0xFF4CAF50)
                } else {
                    Color.White.copy(alpha = 0.5f)
                }
            )
        }

        // Status Badge
        if (isCalibrated) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = rememberLocalizedText("ready_angles").replace("{count}", angleCount.toString()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                // Düzenle butonu
                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = rememberLocalizedText("edit"),
                        fontSize = 11.sp,
                        color = Color(0xFF6A4C93)
                    )
                }
            }
        } else {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A4C93)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = rememberLocalizedText("calibrate"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * ⚡⚡⚡ YENİ: Kalibrasyon Özeti Kartı
 * Test başlatmadan önce kullanıcıya tüm ayarların özetini gösterir
 */
@Composable
private fun CalibrationSummaryCard(
    selectedVFXLevel: VFXLevel,
    sensitivityThreshold: Float,
    creationAngleCount: Int,
    directionAngleCount: Int,
    isReadyForTest: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isReadyForTest) {
            Color(0xFF6A4C93).copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        },
        shadowElevation = if (isReadyForTest) 8.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (isReadyForTest) {
                Color(0xFF6A4C93)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rememberLocalizedText("test_preparation"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                // Hazırlık Durumu Badge
                Surface(
                    shape = CircleShape,
                    color = if (isReadyForTest) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFFFF5252)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isReadyForTest) "✓" else "⚠",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isReadyForTest) rememberLocalizedText("ready") else rememberLocalizedText("missing"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Seçili VFX Seviyesi
            SummaryRow(
                icon = when (selectedVFXLevel) {
                    VFXLevel.SPRITE -> "💧"
                    VFXLevel.PARTICLE -> "✨"
                    VFXLevel.METABALL -> "🌊"
                },
                label = "Efekt Seviyesi:",
                value = when (selectedVFXLevel) {
                    VFXLevel.SPRITE -> "Sprite"
                    VFXLevel.PARTICLE -> "Parçacık"
                    VFXLevel.METABALL -> "Sıvı"
                }
            )

            // Hassasiyet
            SummaryRow(
                icon = "🎯",
                label = "Hassasiyet:",
                value = "${(sensitivityThreshold * 100).toInt()}%"
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // Hareket Özeti
            Text(
                text = rememberLocalizedText("calibrated_gestures"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Yaratma Hareketi
                GestureSummaryBadge(
                    modifier = Modifier.weight(1f),
                    icon = "🖐️",
                    label = rememberLocalizedText("creation"),
                    angleCount = creationAngleCount,
                    isCalibrated = creationAngleCount > 0
                )

                // Yönlendirme Hareketi
                GestureSummaryBadge(
                    modifier = Modifier.weight(1f),
                    icon = "✌️",
                    label = rememberLocalizedText("direction"),
                    angleCount = directionAngleCount,
                    isCalibrated = directionAngleCount > 0
                )
            }
        }
    }
}

/**
 * Özet Satırı (Icon + Label + Value)
 */
@Composable
private fun SummaryRow(
    icon: String,
    label: String,
    value: String
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
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6A4C93)
        )
    }
}

/**
 * Hareket Özet Badge'i
 */
@Composable
private fun GestureSummaryBadge(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    angleCount: Int,
    isCalibrated: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isCalibrated) {
            Color(0xFF4CAF50).copy(alpha = 0.15f)
        } else {
            Color(0xFFFF5252).copy(alpha = 0.15f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isCalibrated) {
                Color(0xFF4CAF50).copy(alpha = 0.5f)
            } else {
                Color(0xFFFF5252).copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                fontSize = 28.sp
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Açı Sayısı
            Surface(
                shape = CircleShape,
                color = if (isCalibrated) {
                    Color(0xFF4CAF50)
                } else {
                    Color(0xFFFF5252)
                }
            ) {
                Text(
                    text = rememberLocalizedText("angles_count").replace("{count}", angleCount.toString()),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * ⚡⚡⚡ YENİ: Kalibrasyon Merkezi'ne Kaydet Butonu
 */
@Composable
private fun SaveToPracticeSealButton(
    onSave: () -> Unit
) {
    var showSuccessMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                onSave()
                showSuccessMessage = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF4CAF50)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💾",
                    fontSize = 20.sp
                )
                Text(
                    text = rememberLocalizedText("save_to_calibration_center"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Text(
            text = rememberLocalizedText("save_as_practice_seal"),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Başarı mesajı
        if (showSuccessMessage) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                showSuccessMessage = false
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF4CAF50)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓",
                        fontSize = 20.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rememberLocalizedText("saved_view_in_center"),
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * ⚡⚡⚡ YENİ: VFX Konumlandırma Kartı
 * Kaydedilmiş referans noktalarını listeler ve yeni referans noktası eklemeye izin verir
 */
@Composable
private fun VFXPositioningCard(
    creationOffsetConfig: com.example.isekaikuroshin.data.VFXOffsetConfig?,
    directionOffsetConfig: com.example.isekaikuroshin.data.VFXOffsetConfig?,
    onAddCreationOffset: () -> Unit,
    onAddDirectionOffset: () -> Unit,
    onDeleteCreationOffset: () -> Unit,
    onDeleteDirectionOffset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = rememberLocalizedText("vfx_positioning"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            Text(
                text = rememberLocalizedText("adjust_3d_position"),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            // Yaratma Hareketi Offset
            OffsetConfigRow(
                icon = "🖐️",
                label = rememberLocalizedText("creation_gesture"),
                offsetConfig = creationOffsetConfig,
                onAdd = onAddCreationOffset,
                onDelete = onDeleteCreationOffset
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // Yönlendirme Hareketi Offset
            OffsetConfigRow(
                icon = "✌️",
                label = rememberLocalizedText("direction_gesture"),
                offsetConfig = directionOffsetConfig,
                onAdd = onAddDirectionOffset,
                onDelete = onDeleteDirectionOffset
            )
        }
    }
}

/**
 * Offset Konfigürasyon Satırı
 */
@Composable
private fun OffsetConfigRow(
    icon: String,
    label: String,
    offsetConfig: com.example.isekaikuroshin.data.VFXOffsetConfig?,
    onAdd: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 32.sp
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )

            if (offsetConfig != null) {
                // Kaydedilmiş offset bilgisi
                Text(
                    text = "Offset: (${String.format("%.2f", offsetConfig.offset3D.x)}, " +
                            "${String.format("%.2f", offsetConfig.offset3D.y)}, " +
                            "${String.format("%.2f", offsetConfig.offset3D.z)})",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50)
                )
            } else {
                Text(
                    text = rememberLocalizedText("not_set_yet"),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Butonlar
        if (offsetConfig != null) {
            // Sil ve Düzenle butonları
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5252)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = rememberLocalizedText("delete"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A4C93)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✏️ ${rememberLocalizedText("edit")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Ekle butonu
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A4C93)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = rememberLocalizedText("set"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Test Başlat Butonu (Ana Eylem)
 */
@Composable
private fun VFXTestButtonCard(
    isEnabled: Boolean,
    onStartTest: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onStartTest,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) Color(0xFF6A4C93) else MaterialTheme.colorScheme.surface,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isEnabled) 8.dp else 0.dp,
                pressedElevation = 12.dp
            )
        ) {
            Text(
                text = if (isEnabled) "🧪 Testi Başlat" else "⚠️ Önce Kalibre Edin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }

        if (!isEnabled) {
            Text(
                text = rememberLocalizedText("calibrate_all_gestures_warning"),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}


