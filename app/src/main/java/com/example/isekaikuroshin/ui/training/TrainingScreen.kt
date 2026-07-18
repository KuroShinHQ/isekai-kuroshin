package com.example.isekaikuroshin.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme
import com.example.isekaikuroshin.ui.theme.getDashboardColors

@Composable
fun TrainingScreen(
    navController: NavController,
    viewModel: TrainingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dashboardColors = getDashboardColors()

    // Log composable rendering
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("TrainingScreen composable rendered")
    }

    // TODO-FIX-CAMP-06: Scaffold + 9 ikonlu navigation bar eklendi (tutarlılık için)
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = "train",
                selectedItem = com.example.isekaikuroshin.ui.navigation.BottomNavItem.Camp
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dashboardColors.backgroundDark)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                TrainingHeader(dashboardColors = dashboardColors)

                // Spiritual Cultivation Hall
                SpiritualCultivationHall(
                    spiritualGp = uiState.spiritualGp,
                    onPerformBreathingRitual = { viewModel.onPerformBreathingRitual() },
                    onPerformMeridianClearing = { viewModel.onPerformMeridianClearing() },
                    onNavigateToSealPractice = {
                        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("Kadim Mühür Teknikleri button clicked - Navigating to seal_practice")
                        navController.navigate("seal_practice")
                    },
                    dashboardColors = dashboardColors
                )
            }

            // TODO-FIX-CAMP-06: Back button kaldırıldı - navigation bar ile değiştirildi
        }
    }
}

// TODO-FIX-CAMP-06: 9 ikonlu navigation bar component (NavController tabanlı)
@Composable
private fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String,
    selectedItem: com.example.isekaikuroshin.ui.navigation.BottomNavItem
) {
    val items = listOf(
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Status,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Inventory,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Quests,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Catalog,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Camp,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Map,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Journal,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.HealthHub,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Settings
    )

    NavigationBar {
        items.forEach { item ->
            val localizedTitle = com.example.isekaikuroshin.data.rememberLocalizedText(item.titleKey)
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = localizedTitle) },
                label = { Text(text = localizedTitle) },
                selected = item == selectedItem,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to start destination to avoid building large backstack
                        popUpTo("main") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun TrainingHeader(dashboardColors: DashboardColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = "Training",
            tint = dashboardColors.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = rememberLocalizedText("training_facility"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = dashboardColors.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BodyForgingWorkshop(
    physicalGp: Map<String, Int>,
    onPerformForging: () -> Unit,
    dashboardColors: DashboardColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.backgroundDark.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessibilityNew,
                    contentDescription = rememberLocalizedText("body_forging_workshop"),
                    tint = dashboardColors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = rememberLocalizedText("body_forging_workshop"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AnatomicalSilhouette(modifier = Modifier.height(200.dp))

                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GpProgressIndicator(
                        label = rememberLocalizedText("muscle_fiber"),
                        gp = physicalGp.getOrDefault("muscleFiberGp", 0),
                        color = dashboardColors.primary
                    )
                    GpProgressIndicator(
                        label = rememberLocalizedText("bone_density"),
                        gp = physicalGp.getOrDefault("boneDensityGp", 0),
                        color = dashboardColors.primary
                    )
                    GpProgressIndicator(
                        label = rememberLocalizedText("blood_flow"),
                        gp = physicalGp.getOrDefault("bloodFlowGp", 0),
                        color = Color(0xFFE57373) // A reddish color
                    )
                }
            }

            Button(
                onClick = onPerformForging,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = dashboardColors.primary)
            ) {
                Text(
                    text = rememberLocalizedText("perform_body_forging"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun SpiritualCultivationHall(
    spiritualGp: Map<String, Int>,
    onPerformBreathingRitual: () -> Unit,
    onPerformMeridianClearing: () -> Unit,
    onNavigateToSealPractice: () -> Unit,
    dashboardColors: DashboardColorScheme
) {
    // Log when this composable renders
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("SpiritualCultivationHall composable rendered - Kadim Mühür button should be visible")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.backgroundDark.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = rememberLocalizedText("spiritual_cultivation_hall"),
                    tint = dashboardColors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = rememberLocalizedText("spiritual_cultivation_hall"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GpProgressIndicator(
                    label = rememberLocalizedText("qi_control"),
                    gp = spiritualGp.getOrDefault("qiControlGp", 0),
                    color = dashboardColors.primary
                )
                GpProgressIndicator(
                    label = rememberLocalizedText("mana_affinity"),
                    gp = spiritualGp.getOrDefault("manaAffinityGp", 0),
                    color = Color(0xFF4FC3F7) // A light blue color
                )
                GpProgressIndicator(
                    label = rememberLocalizedText("meridian_purity"),
                    gp = spiritualGp.getOrDefault("meridianPurityGp", 0),
                    color = Color(0xFFBA68C8) // A purple color
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onPerformBreathingRitual,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = dashboardColors.primary)
                ) {
                    Text(
                        text = rememberLocalizedText("breathing_ritual"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 16.sp
                    )
                }
                Button(
                    onClick = onPerformMeridianClearing,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = dashboardColors.primary)
                ) {
                    Text(
                        text = rememberLocalizedText("meridian_clearing"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 16.sp
                    )
                }
            }

            // Kadim Mühür Teknikleri Button
            Button(
                onClick = onNavigateToSealPractice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A4C93)  // Mor tonunda özel renk
                )
            ) {
                Text(
                    text = "🖐️ ${rememberLocalizedText("ancient_seal_techniques")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}


@Composable
private fun GpProgressIndicator(label: String, gp: Int, color: Color) {
    val progress = (gp % 100) / 100f
    val level = gp / 100
    Column {
        Text(
            text = "$label (${rememberLocalizedText("level")} $level)",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = Color.Gray.copy(alpha = 0.5f)
        )
        Text(
            text = "$gp / ${(level + 1) * 100}",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun AnatomicalSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth(0.4f)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val headRadius = canvasWidth * 0.15f
        val bodyHeight = canvasHeight * 0.4f
        val legHeight = canvasHeight * 0.45f
        val armWidth = canvasWidth * 0.1f

        // Head
        drawCircle(
            color = Color.Gray,
            radius = headRadius,
            center = Offset(x = canvasWidth / 2, y = headRadius),
            style = Stroke(width = 4f)
        )

        // Body
        drawRoundRect(
            color = Color.Gray,
            topLeft = Offset(x = canvasWidth / 2 - canvasWidth * 0.2f, y = headRadius * 2),
            size = Size(canvasWidth * 0.4f, bodyHeight),
            cornerRadius = CornerRadius(x = 10f, y = 10f),
            style = Stroke(width = 4f)
        )

        // Legs
        val legWidth = canvasWidth * 0.15f
        // Left Leg
        drawRoundRect(
            color = Color.Gray,
            topLeft = Offset(x = canvasWidth / 2 - legWidth - 2f, y = headRadius * 2 + bodyHeight),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(x = 8f, y = 8f),
            style = Stroke(width = 4f)
        )
        // Right Leg
        drawRoundRect(
            color = Color.Gray,
            topLeft = Offset(x = canvasWidth / 2 + 2f, y = headRadius * 2 + bodyHeight),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(x = 8f, y = 8f),
            style = Stroke(width = 4f)
        )

        // Arms
        val armHeight = bodyHeight * 0.8f
        // Left Arm
        drawRoundRect(
            color = Color.Gray,
            topLeft = Offset(x = canvasWidth / 2 - canvasWidth * 0.2f - armWidth - 2f, y = headRadius * 2 + 10f),
            size = Size(armWidth, armHeight),
            cornerRadius = CornerRadius(x = 8f, y = 8f),
            style = Stroke(width = 4f)
        )
        // Right Arm
        drawRoundRect(
            color = Color.Gray,
            topLeft = Offset(x = canvasWidth / 2 + canvasWidth * 0.2f + 2f, y = headRadius * 2 + 10f),
            size = Size(armWidth, armHeight),
            cornerRadius = CornerRadius(x = 8f, y = 8f),
            style = Stroke(width = 4f)
        )
    }
}