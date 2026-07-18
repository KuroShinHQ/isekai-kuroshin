package com.example.isekaikuroshin.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.isekaikuroshin.engine.DeathEvent // Assuming this will be updated
import com.example.isekaikuroshin.engine.DeathCause
import com.example.isekaikuroshin.engine.UmbrosPactManager
import com.example.isekaikuroshin.engine.IntelligentContentEngine // GÖREV: Death Archive bazlı video seçimi
import com.example.isekaikuroshin.data.PlayerState // DEĞİŞTİRİLDİ: PlayerStatsZ3 -> PlayerState
import com.example.isekaikuroshin.data.PersistentDataManager // GÖREV: Death Archive kontrolü
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.ai.GlobalAIManager
import com.example.isekaikuroshin.data.rememberLocalizedText
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext

/**
 * Death Sequence Screen - Manages the death experience and Umbros choice dialog
 * Displays death video, death information, and Umbros pact choice to the player
 */
@Composable
fun DeathSequenceScreen(
    deathEvent: DeathEvent,
    onUmbrosDialogRequested: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onArchiveAndRestart: () -> Unit,
    onDeathConfirmed: () -> Unit, // YENİ: Ölüm onaylandı (UserEntry'ye dön)
    intelligentContentEngine: IntelligentContentEngine, // GÖREV: Dinamik video seçimi için
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showVideo by remember { mutableStateOf(true) }
    var showUmbrosTransitionVideo by remember { mutableStateOf(false) } // ✨ YENİ: Umbros geçiş videosu
    var showUmbrosChoice by remember { mutableStateOf(false) }
    var showDeathTransitionVideo by remember { mutableStateOf(false) } // Ölüm geçiş videosu (Umbros reddi)
    var currentVideoPath by remember { mutableStateOf(deathEvent.causeOfDeath.videoPath) }

    // Start AI loading when death sequence begins
    LaunchedEffect(Unit) {
        GlobalAIManager.startAILoading()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (showVideo) {
            DeathVideoPlayer(
                videoPath = currentVideoPath,
                onVideoEnd = {
                    when {
                        showDeathTransitionVideo -> {
                            // Ölüm geçiş videosu bitti (Umbros reddedildi), kullanıcı ölür
                            showVideo = false
                            onDeathConfirmed() // ÖLÜM ONAYLANDI - UserEntry (OLUM_SONRASI)
                        }
                        showUmbrosTransitionVideo -> {
                            // Umbros geçiş videosu bitti, Umbros dialog göster
                            showVideo = false
                            showUmbrosTransitionVideo = false
                            showUmbrosChoice = true
                        }
                        else -> {
                            // İlk ölüm videosu bitti, Umbros geçiş videosunu oynat
                            showVideo = false
                            showUmbrosTransitionVideo = true

                            // GÖREV: IntelligentContentEngine ile Death Archive bazlı video seçimi
                            // Death Archive'den son ölüm kaydını ve oyuncu durumunu al
                            val gameData = PersistentDataManager.gameData.value
                            val deathArchive = gameData.deathArchive
                            val lastDeathRecord = deathArchive.lastOrNull()

                            // POSTDEATH etiketli videolardan uygun olanı seç
                            val playlist = intelligentContentEngine.generateDeathEchoPlaylist(
                                deathCount = deathArchive.size,
                                lastDeathRecord = lastDeathRecord,
                                maxVideos = 5,  // 5 video al, birini rastgele seç
                                maxPhotos = 0   // Fotoğraf gerekmez, sadece video
                            )

                            // Playlist'ten rastgele bir video seç (varsa)
                            val selectedVideoId = if (playlist.videos.isNotEmpty()) {
                                playlist.videos.random()
                            } else {
                                // Fallback: MediaDatabase'den UMBROS videoları
                                val umbrosVideos = com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForScreen(
                                    screenType = "UMBROS",
                                    depth = "D1"
                                ).filter { it.fileName.startsWith("v_") }
                                 .map { it.resourceId }

                                umbrosVideos.randomOrNull() ?: R.raw.intro_animation
                            }

                            currentVideoPath = "android.resource://${context.packageName}/$selectedVideoId"

                            showVideo = true
                        }
                    }
                }
            )
        }

        if (showUmbrosChoice) {
            UmbrosChoiceDialog(
                deathEvent = deathEvent,
                onAcceptPact = onUmbrosDialogRequested,
                onDeclinePact = {
                    // Umbros reddedildi, ölüm geçiş videosunu oynat
                    showUmbrosChoice = false
                    showDeathTransitionVideo = true

                    // MediaDatabase'den death transition videosu al
                    val deathTransitionVideo = com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForScreen(
                        screenType = "DEATH_TRANSITION",
                        depth = "D1"
                    ).firstOrNull { it.fileName.startsWith("v_") }?.resourceId ?: R.raw.intro_animation

                    currentVideoPath = "android.resource://${context.packageName}/$deathTransitionVideo"
                    showVideo = true
                }
            )
        }
    }
}

/**
 * Placeholder for death video player
 * In a real implementation, this would use ExoPlayer or similar
 */
@Composable
private fun DeathVideoPlayer(
    videoPath: String,
    onVideoEnd: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "death_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "death_alpha"
    )

    // Simulate video playback with a delayed callback
    LaunchedEffect(videoPath) {
        delay(3000) // Simulate 3-second video
        onVideoEnd()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Dangerous,
                contentDescription = "Death",
                modifier = Modifier.size(120.dp),
                tint = Color.Red
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = rememberLocalizedText("death"),
                style = MaterialTheme.typography.displayLarge,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = rememberLocalizedText("video_loading").format(videoPath),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Umbros Choice Dialog - The heart of the death system
 * Offers the player a choice between accepting Umbros' pact or restarting
 */
@Composable
private fun UmbrosChoiceDialog(
    deathEvent: DeathEvent,
    onAcceptPact: () -> Unit,
    onDeclinePact: () -> Unit
) {
    // TODO: Use actual death count from GameStateManager or other source for pact generation
    // For now, using death count 0 (first death)
    val pactTerms = remember {
        UmbrosPactManager.PactTerms(
            hpReduction = 0.1f,
            statPenalty = 2,
            experienceLoss = 1000,
            curseDuration = 5,
            acceptanceCount = 0
        )
    }

    Dialog(
        onDismissRequest = { /* Cannot dismiss death dialog */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .padding(12.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF9C27B0),
                            Color(0xFF673AB7),
                            Color(0xFF9C27B0)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            UmbrosDialogContent(
                deathEvent = deathEvent,
                pactTerms = pactTerms,
                onAcceptPact = onAcceptPact,
                onDeclinePact = onDeclinePact
            )
        }
    }
}

/**
 * Content of the Umbros choice dialog
 */
@Composable
private fun UmbrosDialogContent(
    deathEvent: DeathEvent,
    pactTerms: UmbrosPactManager.PactTerms,
    onAcceptPact: () -> Unit,
    onDeclinePact: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "umbros_pulse")
    val umbrosAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "umbros_alpha"
    )

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = rememberLocalizedText("death"),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = deathEvent.causeOfDeath.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            // Assuming deathEvent.playerLevel is still valid
            text = rememberLocalizedText("level_death_message").format(deathEvent.playerLevel),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = rememberLocalizedText("umbros_shadows"),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF9C27B0).copy(alpha = umbrosAlpha),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = rememberLocalizedText("umbros_pact_question"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9C27B0),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(24.dp))

        PactTermsPreview(pactTerms)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onAcceptPact,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .padding(end = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Accept",
                        tint = Color.White
                    )
                    Text(rememberLocalizedText("accept_pact"), color = Color.White, fontSize = 12.sp)
                }
            }

            Button(
                onClick = onDeclinePact,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF424242)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .padding(start = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Decline",
                        tint = Color.White
                    )
                    Text(rememberLocalizedText("decline_and_end"), color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PactTermsPreview(pactTerms: UmbrosPactManager.PactTerms) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = rememberLocalizedText("pact_cost"),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            PactTermItem(rememberLocalizedText("hp_reduction"), "${(pactTerms.hpReduction * 100).toInt()}%")
            PactTermItem(rememberLocalizedText("stat_penalty"), "${pactTerms.statPenalty} ${rememberLocalizedText("points")}")
            PactTermItem(rememberLocalizedText("xp_loss"), "${pactTerms.experienceLoss} ${rememberLocalizedText("points")}")
            PactTermItem(rememberLocalizedText("curse_duration"), "${pactTerms.curseDuration} ${rememberLocalizedText("entries")}")
        }
    }
}

@Composable
private fun PactTermItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color(0xFFFF6B6B),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 891)
@Composable
fun DeathSequenceScreenPreview() {
    IsekaiKuroshinTheme {
        val context = androidx.compose.ui.platform.LocalContext.current
        val mockEngine = IntelligentContentEngine(context)

        val mockDeathEvent = DeathEvent(
            timestamp = System.currentTimeMillis(),
            playerLevel = 5,
            causeOfDeath = DeathCause.COMBAT,
            finalPlayerState = PlayerState() // DEĞİŞTİRİLDİ: finalStats -> finalPlayerState
        )

        DeathSequenceScreen(
            deathEvent = mockDeathEvent,
            onUmbrosDialogRequested = {},
            onArchiveAndRestart = {},
            onDeathConfirmed = {}, // YENİ parametre eklendi
            intelligentContentEngine = mockEngine // GÖREV: Mock engine
        )
    }
}
