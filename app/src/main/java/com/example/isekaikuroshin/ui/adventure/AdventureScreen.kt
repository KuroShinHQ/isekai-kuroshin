package com.example.isekaikuroshin.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.ui.theme.getDashboardColors
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.LanguageManager

@Composable
fun AdventureScreen(navController: NavController) {
    // Get dynamic colors based on theme settings
    val dashboardColors = getDashboardColors()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full screen camp background
        Image(
            painter = painterResource(id = R.drawable.camp),
            contentDescription = "Camp Scene",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // Essential Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdventureActionCardD1(
                        title = LanguageManager.getText("training"),
                        description = LanguageManager.getText("training_facility"),
                        icon = Icons.Default.FitnessCenter,
                        onClick = { navController.navigate("train") },
                        dashboardColors = dashboardColors,
                        modifier = Modifier.weight(1f)
                    )

                    AdventureActionCardD1(
                        title = LanguageManager.getText("mana_core"),
                        description = LanguageManager.getText("cultivate_inner_energy").take(25) + "...",
                        icon = Icons.Default.Psychology,
                        onClick = { navController.navigate("mana_cultivation") },
                        dashboardColors = dashboardColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdventureActionCardD1(
                        title = "Save Game",
                        description = "Manual save progress",
                        icon = Icons.Default.Save,
                        onClick = {
                            PersistentDataManager.saveGameData()
                        },
                        dashboardColors = dashboardColors,
                        modifier = Modifier.weight(1f)
                    )

                    AdventureActionCardD1(
                        title = "Rest",
                        description = "Recover health and mana",
                        icon = Icons.Default.Hotel,
                        onClick = {
                            // Rest action - could restore HP/MP
                        },
                        dashboardColors = dashboardColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Status with Map button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = dashboardColors.holographicBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Current Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = dashboardColors.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusItemD2("Location", "Forest Camp")
                            StatusItemD2("Weather", "Clear")
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                FloatingActionButton(
                    onClick = { navController.navigate("map") },
                    containerColor = dashboardColors.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back button at bottom
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dashboardColors.primary.copy(alpha = 0.8f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Dashboard",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun AdventureActionCardD1(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    dashboardColors: DashboardColorScheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.holographicBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = dashboardColors.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun StatusItemD2(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

