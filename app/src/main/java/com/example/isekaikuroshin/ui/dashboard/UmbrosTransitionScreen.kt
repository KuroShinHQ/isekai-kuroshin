package com.example.isekaikuroshin.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.C
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavController
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.ai.GlobalAIManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.isekaikuroshin.data.rememberLocalizedText

@UnstableApi
@Composable
fun UmbrosTransitionScreen(
    navController: NavController,
    onTransitionComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var videoComplete by remember { mutableStateOf(false) }
    var showNegotiation by remember { mutableStateOf(false) }

    // GÖREV: Theme integration - Use MaterialTheme colors
    val textColor = MaterialTheme.colorScheme.onSurface
    val backgroundColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    // GÖREV: Umbros Transition - Localized texts
    val umbrosInitialMsg = rememberLocalizedText("umbros_initial_message")
    val umbrosPhase1Msg = rememberLocalizedText("umbros_phase1_message")
    val umbrosPhase2Msg = rememberLocalizedText("umbros_phase2_message")
    val umbrosTitle = rememberLocalizedText("umbros_title")
    val umbrosRejectBtn = rememberLocalizedText("umbros_reject_button")
    val umbrosSendBtn = rememberLocalizedText("umbros_send_button")
    val umbrosAcceptBtn = rememberLocalizedText("umbros_accept_button")
    val umbrosDeathCause = rememberLocalizedText("umbros_death_cause")

    // Müzakere durumu
    var umbrosMessage by remember { mutableStateOf("") }
    var playerInput by remember { mutableStateOf("") }
    var negotiationPhase by remember { mutableStateOf(0) } // 0: Başlangıç, 1: Geçmiş, 2: Statlar, 3: Teklif

    // KARMA SYSTEM FIX: UMBROS içerikleri MediaDatabaseHelper'dan çekiliyor
    // UserEntryScreen'deki karma mekaniklerini kullanıyoruz
    val umbrosBackgroundImages = remember {
        val gameData = com.example.isekaikuroshin.data.PersistentDataManager.gameData.value
        val moralityScore = gameData.playerData.moralityScore

        android.util.Log.d("UmbrosTransition", "🎭 UMBROS Karma Sistemi - moralityScore: $moralityScore")

        // UMBROS screenType'ı için karma bazlı içerik filtrele
        val umbrosMedia = com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForScreen(
            screenType = "UMBROS",
            depth = "D1"
        ).filter { it.fileName.startsWith("p_") } // Photo files only
         .map { it.resourceId }

        // Eğer UMBROS içeriği yoksa fallback: hardcoded demon_bg
        if (umbrosMedia.isNotEmpty()) {
            android.util.Log.d("UmbrosTransition", "✅ UMBROS SİSTEMİ AKTİF - ${umbrosMedia.size} kişiselleştirilmiş fotoğraf yüklendi")
            umbrosMedia
        } else {
            android.util.Log.w("UmbrosTransition", "⚠️ UMBROS SİSTEMİ BAŞARISIZ - Fallback: demon_bg kullanılıyor")
            listOf(
                R.drawable.demon_bg_00, R.drawable.demon_bg_01, R.drawable.demon_bg_02,
                R.drawable.demon_bg_03, R.drawable.demon_bg_04, R.drawable.demon_bg_06,
                R.drawable.demon_bg_07, R.drawable.demon_bg_08, R.drawable.demon_bg_09,
                R.drawable.demon_bg_10, R.drawable.demon_bg_10_1, R.drawable.demon_bg_11,
                R.drawable.demon_bg_12, R.drawable.demon_bg_13, R.drawable.demon_bg_14,
                R.drawable.demon_bg_15, R.drawable.demon_bg_16, R.drawable.demon_bg_17
            )
        }
    }
    var backgroundIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // Start AI loading immediately when transition screen opens
        GlobalAIManager.startAILoading()

        while (true) {
            delay(3000) // 3 second interval
            backgroundIndex = (backgroundIndex + 1) % umbrosBackgroundImages.size
        }
    }

    // Initialize ExoPlayer for transition video
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.angeldevil}")
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f // UMBROS video SES AÇIK (ölüm animasyonu dramatik olmalı!)

            // Quick preparation for faster loading
            setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        videoComplete = true
                        showNegotiation = true
                        // İlk monolog başlatılır
                        umbrosMessage = umbrosInitialMsg
                    }
                }
            })
        }
    }

    // Cleanup on dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                // Skip video on click
                if (!videoComplete) {
                    videoComplete = true
                    showNegotiation = true
                    exoPlayer.stop()
                    umbrosMessage = umbrosInitialMsg
                }
            }
    ) {
        // Demon background slideshow
        Image(
            painter = painterResource(id = umbrosBackgroundImages[backgroundIndex]),
            contentDescription = "Demon Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Completely transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent) // Tamamen transparan
        )

        if (!showNegotiation) {
            // Umbros MP Eye Video - Ekranın tam ortasında
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Video çerçevesi - daha büyük ve belirgin
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(1f) // Kare çerçeve göz için
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(8.dp)
                ) {
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                    )
                }
            }

            // Merkezi muted göz ikonu
            IconButton(
                onClick = {
                    // Volume toggle placeholder
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 100.dp)
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.VisibilityOff,
                    contentDescription = "Muted",
                    tint = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Skip button
            IconButton(
                onClick = {
                    videoComplete = true
                    showNegotiation = true
                    exoPlayer.stop()
                    umbrosMessage = umbrosInitialMsg
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp)
                    .background(
                        backgroundColor.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Mood,
                    contentDescription = "Skip to Negotiation",
                    tint = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // Eye effect removed - user request (SORUN 2)

            // Müzakere Ekranı - şeffaf background
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Üst kısım: Umbros'un mesajları - şeffaf
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent // Tamamen transparan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = umbrosTitle,
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = umbrosMessage,
                            color = textColor.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Alt kısım: Oyuncu input alanı - şeffaf
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E2E).copy(alpha = 0.7f) // Daha şeffaf
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = playerInput,
                            onValueChange = { playerInput = it },
                            placeholder = {
                                Text(
                                    text = rememberLocalizedText("write_response_to_umbros"),
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                            ),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    // GÖREV: First User profili için özel yönlendirme
                                    // Paktı reddet - ölümü kaydet (wasUmbrosOfferAccepted = false)
                                    coroutineScope.launch {
                                        val currentData = PersistentDataManager.gameData.value
                                        val deathArchive = currentData.deathArchive

                                        PersistentDataManager.recordDeath(
                                            deathCause = umbrosDeathCause,
                                            wasUmbrosOfferAccepted = false,
                                            screenshotPath = null
                                        )

                                        // GÖREV: Reject basınca Death Statistics ekranına git
                                        PersistentDataManager.setFirstAIDialogVisitCompleted()

                                        // Death Statistics ekranına yönlendir
                                        navController.navigate("death_statistics") {
                                            popUpTo("umbros_transition") { inclusive = true }
                                        }
                                        onTransitionComplete()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Gray.copy(alpha = 0.6f)
                                )
                            ) {
                                Text(
                                    text = umbrosRejectBtn,
                                    color = textColor
                                )
                            }

                            Button(
                                onClick = {
                                    // Mesaj gönder ve müzakereyi ilerlet
                                    if (playerInput.isNotBlank()) {
                                        when (negotiationPhase) {
                                            0 -> {
                                                umbrosMessage = umbrosPhase1Msg
                                                negotiationPhase = 1
                                            }
                                            1 -> {
                                                umbrosMessage = umbrosPhase2Msg
                                                negotiationPhase = 2
                                            }
                                            2 -> {
                                                // GÖREV: First User profili için özel yönlendirme
                                                // Final teklif - paktı kabul et
                                                val currentData = PersistentDataManager.gameData.value
                                                val deathArchive = currentData.deathArchive
                                                val playerData = currentData.playerData

                                                // HOTFIX: Ölü oyuncu teklifi kabul edince Death Statistics'e gitsin
                                                // isAlive kontrolü eklendi (ölü oyuncu için)
                                                val isFirstUser = deathArchive.size == 0
                                                val isDead = !playerData.isAlive

                                                PersistentDataManager.setFirstAIDialogVisitCompleted()

                                                when {
                                                    // 1. Ölü oyuncu - Death Statistics ekranına yönlendir
                                                    isDead -> {
                                                        // Önce ölümü kaydet (wasUmbrosOfferAccepted = true)
                                                        coroutineScope.launch {
                                                            PersistentDataManager.recordDeath(
                                                                deathCause = umbrosDeathCause,
                                                                wasUmbrosOfferAccepted = true,
                                                                screenshotPath = null
                                                            )

                                                            navController.navigate("death_statistics") {
                                                                popUpTo("umbros_transition") { inclusive = true }
                                                            }
                                                        }
                                                    }
                                                    // 2. First User - UserEntry ekranına yönlendir
                                                    isFirstUser -> {
                                                        navController.navigate("user_entry") {
                                                            popUpTo("umbros_transition") { inclusive = true }
                                                        }
                                                    }
                                                    // 3. Returning User (alive) - Dashboard'a dön
                                                    else -> {
                                                        navController.navigate("dashboard") {
                                                            popUpTo("dashboard") { inclusive = false }
                                                        }
                                                    }
                                                }
                                                onTransitionComplete()
                                            }
                                        }
                                        playerInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                enabled = playerInput.isNotBlank()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (negotiationPhase == 2) umbrosAcceptBtn else umbrosSendBtn,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}