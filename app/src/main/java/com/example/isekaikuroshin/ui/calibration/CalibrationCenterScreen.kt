package com.example.isekaikuroshin.ui.calibration

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.getCalibrationCount
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.sealpractice.getDifficultyColor

/**
 * Kalibrasyon Merkezi Ekranı
 *
 * Bu ekran, kullanıcıların mühürleri için özel kalibrasyonlar ekleyebileceği,
 * hassasiyet ayarlarını yapabileceği merkezi bir kontrol panelidir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationCenterScreen(
    navController: NavController,
    viewModel: CalibrationCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = "Kalibrasyon Merkezi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            if (uiState.availableSeals.isEmpty()) {
                EmptyStateView()
            } else {
                CalibrationSealList(
                    seals = uiState.availableSeals,
                    onSealSelected = { seal ->
                        viewModel.selectSealForCalibration(seal)
                        navController.navigate("calibration_camera/${seal.id}")
                    },
                    onThresholdChanged = { seal, newThreshold ->
                        viewModel.updateSealThreshold(seal, newThreshold)
                    },
                    navController = navController
                )
            }
        }
    }
}

/**
 * Mühür Listesi (Kalibrasyon İçin)
 */
@Composable
fun CalibrationSealList(
    seals: List<Seal>,
    onSealSelected: (Seal) -> Unit,
    onThresholdChanged: (Seal, Float) -> Unit,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Başlık bilgisi
        item {
            CalibrationInfoCard()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // VFX Test Atölyesi Butonu
        item {
            VFXWorkshopButton(navController = navController)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Mühür kartları
        items(seals) { seal ->
            CalibrationSealCard(
                seal = seal,
                onClick = { onSealSelected(seal) },
                onThresholdChanged = { newThreshold ->
                    onThresholdChanged(seal, newThreshold)
                }
            )
        }

        // Alt boşluk
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Bilgilendirme Kartı
 */
@Composable
fun CalibrationInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = rememberLocalizedText("how_it_works"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = rememberLocalizedText("calibration_instructions"),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Kalibrasyon Mühür Kartı
 */
@Composable
fun CalibrationSealCard(
    seal: Seal,
    onClick: () -> Unit,
    onThresholdChanged: (Float) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var currentThreshold by remember { mutableFloatStateOf(seal.acceptanceThreshold) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Üst kısım: Mühür bilgisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // İkon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    getDifficultyColor(seal.difficulty),
                                    getDifficultyColor(seal.difficulty).copy(alpha = 0.3f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🖐️",
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Bilgiler
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = rememberLocalizedText(seal.nameKey),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Kalibrasyon sayısı
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (seal.getCalibrationCount() > 0) Color(0xFF00FF88) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = rememberLocalizedText("saved_angles_count").replace("{count}", seal.getCalibrationCount().toString()),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Ayarlar butonu
                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Ayarları Aç/Kapat",
                        tint = Color.White
                    )
                }
            }

            // Alt kısım: Hassasiyet ayarı (genişletilebilir)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Kabul Eşiği Ayarı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rememberLocalizedText("acceptance_threshold"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "${(currentThreshold * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slider
                    Slider(
                        value = currentThreshold,
                        onValueChange = {
                            currentThreshold = it
                        },
                        onValueChangeFinished = {
                            onThresholdChanged(currentThreshold)
                        },
                        valueRange = 0.5f..0.99f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    // Hassasiyet açıklaması
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kolay (50%)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Zor (99%)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * VFX Workshop Butonu
 */
@Composable
fun VFXWorkshopButton(navController: NavController) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("vfx_workshop") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF6A00F4).copy(alpha = 0.9f),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🧪",
                    fontSize = 32.sp
                )
                Column {
                    Text(
                        text = rememberLocalizedText("vfx_test_workshop"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = rememberLocalizedText("test_water_effects"),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Git",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Boş Durum Görünümü
 */
@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = rememberLocalizedText("seal_not_found"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = rememberLocalizedText("seal_not_found_message"),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
