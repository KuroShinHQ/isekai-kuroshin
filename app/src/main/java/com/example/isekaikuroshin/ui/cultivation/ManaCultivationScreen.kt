package com.example.isekaikuroshin.ui.cultivation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.ui.theme.getDashboardColors
import com.example.isekaikuroshin.data.LanguageManager
import androidx.compose.animation.core.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun ManaCultivationScreen(navController: NavController) {
    // Get dynamic colors based on theme settings
    val dashboardColors = getDashboardColors()
    val gameData by com.example.isekaikuroshin.data.PersistentDataManager.gameData.collectAsState()
    var showLotusVideo by remember { mutableStateOf(false) }
    var currentCoreStage by remember { mutableIntStateOf(1) } // 1, 2, 3 halka

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Dynamic background based on theme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            dashboardColors.primary.copy(alpha = 0.1f),
                            dashboardColors.primary.copy(alpha = 0.3f),
                            dashboardColors.primary.copy(alpha = 0.1f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header - centered title only
            Text(
                text = LanguageManager.getText("mana_core"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = LanguageManager.getText("cultivate_inner_energy"),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mana Core Visualization with video background
            ManaCoreVisualizationWithVideo(
                coreStage = currentCoreStage,
                dashboardColors = dashboardColors,
                onVideoClick = {
                    // Optional: Play video on core click
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Grid (based on HTML design)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(getManaStats(gameData, currentCoreStage)) { stat ->
                    ManaCoreStatCard(
                        stat = stat,
                        dashboardColors = dashboardColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Core Timeline (Progress System)
            ManaCoreTimeline(
                currentStage = currentCoreStage,
                dashboardColors = dashboardColors
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (based on HTML design)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ManaCoreActionButton(
                        title = LanguageManager.getText("meditate"),
                        icon = Icons.Default.Spa,
                        isPrimary = true,
                        onClick = {
                            // Meditation logic here
                            // G83: Log meditation activity
                            com.example.isekaikuroshin.utils.EventLogger.logCultivation(
                                type = "Mana Meditation",
                                breakthroughLevel = null
                            )
                        },
                        dashboardColors = dashboardColors,
                        modifier = Modifier.weight(1f)
                    )

                    ManaCoreActionButton(
                        title = LanguageManager.getText("condense"),
                        icon = Icons.Default.AutoFixHigh,
                        isPrimary = false,
                        onClick = {
                            // Show lotus video on successful advancement
                            showLotusVideo = true
                            val hadBreakthrough = currentCoreStage < 3
                            if (hadBreakthrough) {
                                currentCoreStage += 1
                                // G83: Log breakthrough cultivation
                                com.example.isekaikuroshin.utils.EventLogger.logCultivation(
                                    type = "Mana Core Condensation",
                                    breakthroughLevel = currentCoreStage
                                )
                            } else {
                                // G83: Log regular cultivation
                                com.example.isekaikuroshin.utils.EventLogger.logCultivation(
                                    type = "Mana Core Condensation",
                                    breakthroughLevel = null
                                )
                            }
                        },
                        dashboardColors = dashboardColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ManaCoreActionButton(
                        title = LanguageManager.getText("expand_core"),
                        icon = Icons.Default.Psychology,
                        isPrimary = false,
                        onClick = {
                            // Expand core logic
                            // G83: Log core expansion
                            com.example.isekaikuroshin.utils.EventLogger.logCultivation(
                                type = "Mana Core Expansion",
                                breakthroughLevel = null
                            )
                        },
                        dashboardColors = dashboardColors,
                        modifier = Modifier.weight(1f)
                    )

                    ManaCoreActionButton(
                        title = LanguageManager.getText("purify"),
                        icon = Icons.Default.AutoFixHigh,
                        isPrimary = false,
                        onClick = {
                            // Purify logic
                            // G83: Log purification
                            com.example.isekaikuroshin.utils.EventLogger.logCultivation(
                                type = "Mana Core Purification",
                                breakthroughLevel = null
                            )
                        },
                        dashboardColors = dashboardColors,
                        modifier = Modifier.weight(1f)
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
                    contentDescription = LanguageManager.getText("back"),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.getText("back"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Lotus Blossom Video Overlay
    if (showLotusVideo) {
        LotusBlossomVideoOverlay(
            onVideoEnd = { showLotusVideo = false }
        )
    }
}

@Composable
private fun ManaCoreVisualizationWithVideo(
    coreStage: Int,
    dashboardColors: com.example.isekaikuroshin.ui.theme.DashboardColorScheme,
    onVideoClick: () -> Unit
) {
    // Pulsing animation for mana core
    val pulseAnimation = rememberInfiniteTransition("pulse")
    val alpha by pulseAnimation.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dashboardColors.primary.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
            .clickable { onVideoClick() },
        contentAlignment = Alignment.Center
    ) {
        // Background cosmic effect
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            dashboardColors.primary.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Mana core image based on stage
        // TODO: mana_core_stage_X görselleri eklenmeli (oyun mekaniği)
        val coreImageResource = when (coreStage) {
            1 -> R.mipmap.ic_launcher_divine  // Placeholder
            2 -> R.mipmap.ic_launcher_light   // Placeholder
            3 -> R.mipmap.ic_launcher_mystery // Placeholder
            else -> R.mipmap.ic_launcher_divine
        }

        Image(
            painter = painterResource(id = coreImageResource),
            contentDescription = "Mana Core Stage $coreStage",
            modifier = Modifier
                .size(140.dp)
                .alpha(alpha),
            contentScale = ContentScale.Fit
        )

        // Ring indicators around the core
        repeat(coreStage) { ringIndex ->
            Box(
                modifier = Modifier
                    .size((160 + ringIndex * 25).dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = dashboardColors.primary.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
        }

        // Play button overlay
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ManaCoreStatCard(
    stat: ManaStat,
    dashboardColors: com.example.isekaikuroshin.ui.theme.DashboardColorScheme
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.holographicBg
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stat.label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stat.value,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ManaCoreTimeline(
    currentStage: Int,
    dashboardColors: com.example.isekaikuroshin.ui.theme.DashboardColorScheme
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.holographicBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = LanguageManager.getText("core_timeline"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Timeline stages
            val stages = listOf(
                LanguageManager.getText("bronze_ring"),
                LanguageManager.getText("silver_ring"),
                LanguageManager.getText("gold_ring")
            )
            stages.forEachIndexed { index, stageName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Stage indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (index + 1 <= currentStage) dashboardColors.primary
                                else dashboardColors.primary.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index + 1 <= currentStage) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Stage info
                    Column {
                        Text(
                            text = stageName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (index + 1 == currentStage) {
                            Text(
                                text = "${LanguageManager.getText("current_stage")} - ${LanguageManager.getText("sub_phase")} ${(index + 1)}/3",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            // Progress bar for current stage
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(dashboardColors.primary.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.66f) // Example progress
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(dashboardColors.primary)
                                )
                            }
                        } else if (index + 1 > currentStage) {
                            val stageCount = index + 1 - currentStage
                            val unlockText = if (stageCount > 1)
                                "$stageCount ${LanguageManager.getText("unlock_in_stages")}"
                            else
                                "$stageCount ${LanguageManager.getText("unlock_in_stage")}"
                            Text(
                                text = unlockText,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManaCoreActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    dashboardColors: com.example.isekaikuroshin.ui.theme.DashboardColorScheme,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) dashboardColors.primary
                           else dashboardColors.primary.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LotusBlossomVideoOverlay(
    onVideoEnd: () -> Unit
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(Unit) {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri("android.resource://com.example.isekaikuroshin/${R.raw.lotus_blossom_animation}"))
            prepare()
            playWhenReady = true

            // Auto close when video ends
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onVideoEnd()
                    }
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }

    // Full screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onVideoEnd() }, // Allow tap to close
        contentAlignment = Alignment.Center
    ) {
        exoPlayer?.let { player ->
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        this.player = player
                        useController = false // Hide controls for pure experience
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Data class for mana stats
private data class ManaStat(
    val label: String,
    val value: String
)

private fun getManaStats(
    gameData: com.example.isekaikuroshin.data.PersistentGameData,
    coreStage: Int
): List<ManaStat> {
    return listOf(
        ManaStat(LanguageManager.getText("mana_pool"), "${gameData.playerData.stats.mp}/1200"),
        ManaStat(LanguageManager.getText("mana_regen"), "50/min"),
        ManaStat(LanguageManager.getText("core_purity"), "85%"),
        ManaStat(LanguageManager.getText("core_stage"), when(coreStage) {
            1 -> LanguageManager.getText("bronze")
            2 -> LanguageManager.getText("silver")
            3 -> LanguageManager.getText("gold")
            else -> "Unknown"
        }),
        ManaStat(LanguageManager.getText("elemental_affinity"), LanguageManager.getText("mystic"))
    )
}