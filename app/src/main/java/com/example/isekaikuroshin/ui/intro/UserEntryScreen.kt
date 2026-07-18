package com.example.isekaikuroshin.ui.intro

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.sound.SoundManager
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.delay
import kotlin.random.Random

@UnstableApi
@Composable
fun UserEntryScreen(
    onNavigateToTransition: (String) -> Unit = {},
    onNavigateToDashboard: (String) -> Unit = {},
    viewModel: UserEntryViewModel = hiltViewModel()
) {
    // FB#12: SoundManager'ı context'ten al
    val context = LocalContext.current as android.app.Activity
    val soundManager = remember {
        val appContext = context.applicationContext
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            appContext,
            com.example.isekaikuroshin.sound.SoundManagerEntryPoint::class.java
        ).soundManager()
    }
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    when (uiState.flowState) {
        UserFlowState.YENI_KULLANICI -> {
            NewUserContent(
                soundManager = soundManager,
                dynamicVideoIds = uiState.dynamicVideoIds,  // GÖREV: FIRSTUSER videoları
                dynamicPhotoIds = uiState.dynamicPhotoIds,  // GÖREV: FIRSTUSER fotoğrafları
                onAccept = { codename, age, gender ->
                    viewModel.onNewUserAccept(codename, age, gender)
                    onNavigateToTransition(codename)
                }
            )
        }
        // FIX-DEATH-VIDEO: Ölümden dönen oyuncu da "returning user" - karma sistemi kullanmalı!
        UserFlowState.GERI_DONEN_KULLANICI, UserFlowState.OLUM_SONRASI -> {
            ReturningUserContent(
                soundManager = soundManager,
                playerName = uiState.playerName,
                moralityScore = uiState.moralityScore,
                dynamicVideoIds = uiState.dynamicVideoIds,  // ← ÖLÜM YANKISI videoları burada gösterilecek
                dynamicPhotoIds = uiState.dynamicPhotoIds,
                onContinue = {
                    android.util.Log.d("UserEntryScreen", "🎯 onContinue lambda çağrıldı - playerName: ${uiState.playerName}")
                    viewModel.onReturningUserContinue()
                    android.util.Log.d("UserEntryScreen", "🎯 onNavigateToDashboard çağrılıyor...")
                    onNavigateToDashboard(uiState.playerName)
                    android.util.Log.d("UserEntryScreen", "✅ onNavigateToDashboard çağrıldı")
                }
            )
        }
        UserFlowState.LOADING -> {
            LoadingScreen()
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Initializing...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@UnstableApi
@Composable
private fun NewUserContent(
    soundManager: SoundManager,
    dynamicVideoIds: List<Int>,  // GÖREV: IntelligentContentEngine'den FIRSTUSER videoları
    dynamicPhotoIds: List<Int>,  // GÖREV: IntelligentContentEngine'den FIRSTUSER fotoğrafları
    onAccept: (String, Int, String) -> Unit
) {
    val context = LocalContext.current
    var codename by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("18") }
    var gender by remember { mutableStateOf("Male") }
    var showDialog by remember { mutableStateOf(false) }
    var showGenderDropdown by remember { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female")

    // GÖREV: IntelligentContentEngine'den gelen FIRSTUSER videoları (fallback ile)
    val backgroundVideos = remember(dynamicVideoIds) {
        if (dynamicVideoIds.isNotEmpty()) {
            android.util.Log.d("NewUserContent", "✅ FIRSTUSER SİSTEMİ AKTİF - ${dynamicVideoIds.size} kişiselleştirilmiş video yüklendi")
            dynamicVideoIds
        } else {
            android.util.Log.w("NewUserContent", "⚠️ FIRSTUSER SİSTEMİ BAŞARISIZ - dynamicVideoIds boş! Fallback: NÖTR + D1 videolar kullanılıyor.")
            // Fallback: DEPTH 1 (Surface level) - TÜM attribute'lar dahil
            val fallbackVideos = com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForScreen(
                screenType = "FIRSTUSER",
                depth = "D1"
            ).map { it.resourceId }

            if (fallbackVideos.isNotEmpty()) fallbackVideos else listOf(R.raw.intro_animation)
        }
    }

    // GÖREV: IntelligentContentEngine'den gelen FIRSTUSER fotoğrafları (fallback ile)
    val backgroundImages = remember(dynamicPhotoIds) {
        if (dynamicPhotoIds.isNotEmpty()) {
            android.util.Log.d("NewUserContent", "✅ FIRSTUSER SİSTEMİ AKTİF - ${dynamicPhotoIds.size} kişiselleştirilmiş fotoğraf yüklendi")
            dynamicPhotoIds
        } else {
            android.util.Log.w("NewUserContent", "⚠️ FIRSTUSER SİSTEMİ BAŞARISIZ - dynamicPhotoIds boş! Fallback: NÖTR + D1 fotoğraflar kullanılıyor.")
            // Fallback: DEPTH 1 fotoğraflar - TÜM attribute'lar dahil
            val fallbackPhotos = com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForScreen(
                screenType = "FIRSTUSER",
                depth = "D1"
            ).filter { media ->
                // Sadece fotoğraflar
                media.fileName.startsWith("p_")
            }.map { it.resourceId }

            if (fallbackPhotos.isNotEmpty()) fallbackPhotos else emptyList()
        }
    }.ifEmpty {
        // FALLBACK: Liste boşsa placeholder fotoğraf kullan
        listOf(R.drawable.ic_launcher_foreground)
    }

    var currentImageIndex by remember { mutableIntStateOf(0) }
    var currentVideoIndex by remember {
        mutableIntStateOf(if (backgroundVideos.isNotEmpty()) Random.nextInt(0, backgroundVideos.size) else 0)
    }

    // FB#12: Video ses kontrolü için state
    // TODO-FIX-VIDEO-11: Video sesi default AÇIK yapıldı (false = unmuted)
    var isVideoMuted by remember { mutableStateOf(false) }

    // Video player setup - FB#12: Giriş ekranında video SES AÇIK (default)
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            // FIX #38: Volume initial değeri kaldırıldı - prepare() sonrası ayarlanacak
        }
    }

    // Fotoğraf slideshow
    LaunchedEffect(backgroundImages.size) {
        if (backgroundImages.isNotEmpty()) {
            while (true) {
                delay(4000) // 4 saniyede bir değiş
                currentImageIndex = (currentImageIndex + 1) % backgroundImages.size
            }
        }
    }

    // Video setup
    LaunchedEffect(currentVideoIndex, backgroundVideos.size, isVideoMuted) {
        if (backgroundVideos.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${backgroundVideos[currentVideoIndex]}")
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            // FIX #38: Volume AFTER prepare() - MediaCodec hazır olduğunda ayarla
            exoPlayer.volume = if (isVideoMuted) 0f else 1f
            exoPlayer.play()
            android.util.Log.d("UserEntryScreen", "🎥 FIRSTUSER Video loaded & volume set AFTER prepare: muted=$isVideoMuted, volume=${exoPlayer.volume}")
        }
    }

    // Video bittiğinde sonrakine geç
    LaunchedEffect(backgroundVideos.size) {
        if (backgroundVideos.isNotEmpty()) {
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        currentVideoIndex = (currentVideoIndex + 1) % backgroundVideos.size
                    }
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // KATMAN 1 (En Arka): Background Slideshow
        Crossfade(
            targetState = currentImageIndex,
            modifier = Modifier.fillMaxSize(),
            label = "background-slideshow"
        ) { index ->
            Image(
                painter = painterResource(id = backgroundImages[index]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // KATMAN 2 (Orta): Video Player (%70 genişlik, %50 şeffaflık)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.6f)
        ) {
            AndroidView(
                factory = { PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                } },
                modifier = Modifier.fillMaxSize()
            )

            // FB#12: Video ses aç/kapa butonu (sağ üst köşe)
            IconButton(
                onClick = { isVideoMuted = !isVideoMuted },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isVideoMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isVideoMuted) "Unmute Video" else "Mute Video",
                    tint = Color.White
                )
            }
        }

        // KATMAN 3 (En Ön): Ana UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // G128: Disclaimer metni (sol üst)
            Text(
                text = rememberLocalizedText("intro_disclaimer"),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 8.dp),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Start,
                lineHeight = 14.sp,
                style = MaterialTheme.typography.bodySmall
            )

            // Merkez içerik
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Transfer is starting...",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome, Chosen One.",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = codename,
                    onValueChange = { codename = it },
                    label = { Text("Who are you", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Red,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // TODO-FIX-AGE-10: Yaş giriş alanı - "0" gösterme, boş kalmalı, minimum 18 yaş
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { newValue ->
                            // Sadece rakam girişine izin ver veya boş
                            if (newValue.isEmpty()) {
                                ageText = newValue  // Boş değer kabul et (0 gösterme)
                            } else if (newValue.all { it.isDigit() }) {
                                // Maksimum 3 karakter (999 yaş) ve "0" ile başlamasın
                                if (newValue.length <= 3 && !newValue.startsWith("0")) {
                                    ageText = newValue
                                }
                            }
                        },
                        label = { Text("Age (min 18)", color = Color.White.copy(alpha = 0.8f)) },
                        placeholder = { Text("18", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        modifier = Modifier.weight(0.4f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    // Cinsiyet dropdown
                    Box(modifier = Modifier.weight(0.6f)) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = { },
                            label = { Text("Cinsiyet", color = Color.White.copy(alpha = 0.8f)) },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Cinsiyet Seç",
                                    tint = Color.White,
                                    modifier = Modifier.clickable { showGenderDropdown = !showGenderDropdown }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Red,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            interactionSource = remember { MutableInteractionSource() }
                                .also { interactionSource ->
                                    LaunchedEffect(interactionSource) {
                                        interactionSource.interactions.collect {
                                            if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                                showGenderDropdown = !showGenderDropdown
                                            }
                                        }
                                    }
                                }
                        )

                        DropdownMenu(
                            expanded = showGenderDropdown,
                            onDismissRequest = { showGenderDropdown = false },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
                        ) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
                                    onClick = {
                                        gender = option
                                        showGenderDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Kabul Et butonu
            Button(
                onClick = {
                    val age = ageText.toIntOrNull() ?: 18 // Default 18
                    val validAge = age.coerceIn(18, 120) // PEGI 18: Minimum 18 yaş
                    val trimmedCodename = codename.trim()
                    GameLogger.logSystem("🎯 'KABUL ET' butonuna tıklandı - Codename: '$trimmedCodename', Age: $validAge, Gender: '$gender'")

                    // GÖREV #3: Kullanıcı Adı Validasyonu - Min 3 karakter
                    if (trimmedCodename.isEmpty() || trimmedCodename.length < 3) {
                        showDialog = true
                    } else if (age < 18) {
                        // Yaş 18'den küçükse uyarı göster
                        showDialog = true
                    } else {
                        onAccept(trimmedCodename, validAge, gender)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "BEGIN",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // Version bilgisi (sağ alt - fotoğrafların üstünde)
        Text(
            text = "v0.0.1",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
    }

    // Uyarı dialog'u
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    "Invalid Input",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Please enter a valid name (minimum 3 characters) and ensure your age is 18 or above.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // GÖREV: PEGI 18 dialog kaldırıldı - LegalConsentScreen'de zaten var
}

@UnstableApi
@Composable
private fun ReturningUserContent(
    soundManager: SoundManager,
    playerName: String,
    moralityScore: Float,
    dynamicVideoIds: List<Int>,
    dynamicPhotoIds: List<Int>,
    onContinue: () -> Unit
) {
    val context = LocalContext.current

    // Video kontrolleri için state
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) } // FB#12: Video default olarak sesli (returning user için)
    var currentVideoIndex by remember { mutableIntStateOf(0) }

    // KARMA SİSTEMİ: IntelligentContentEngine tarafından üretilen kişiselleştirilmiş fotoğraflar
    val backgroundImages = remember(dynamicPhotoIds, moralityScore) {
        if (dynamicPhotoIds.isEmpty()) {
            // Fallback: MediaDatabase'den karma bazlı dinamik seçim
            android.util.Log.w("ReturningUserContent", "⚠️ KARMA SİSTEMİ BAŞARISIZ - MediaDatabase fallback kullanılıyor (karma: $moralityScore)")

            val karmaRange = when {
                moralityScore < -0.3f -> -1.0f..-0.3f  // Negatif karma
                moralityScore > 0.3f -> 0.3f..1.0f     // Pozitif karma
                else -> -0.2f..0.2f                     // Nötr karma
            }

            val fallbackPhotos = com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForKarmaRange(
                screenType = "RETURNINGUSER",
                karmaRange = karmaRange,
                depth = "D1"
            ).filter { it.fileName.startsWith("p_") }
             .map { it.resourceId }

            if (fallbackPhotos.isNotEmpty()) fallbackPhotos else emptyList()
        } else {
            // ✅ KARMA SİSTEMİ AKTİF - Kişiselleştirilmiş fotoğraflar
            android.util.Log.d("ReturningUserContent", "✅ KARMA SİSTEMİ AKTİF - ${dynamicPhotoIds.size} kişiselleştirilmiş fotoğraf yüklendi")
            dynamicPhotoIds
        }
    }.ifEmpty {
        // FALLBACK: Liste boşsa placeholder fotoğraf kullan
        listOf(R.drawable.ic_launcher_foreground)
    }

    var currentImageIndex by remember { mutableIntStateOf(0) }

    // Video player setup
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            // FIX #38: Volume initial değeri kaldırıldı - prepare() sonrası ayarlanacak
        }
    }

    // Fotoğraf slideshow
    LaunchedEffect(backgroundImages.size) {
        if (backgroundImages.isNotEmpty()) {
            while (true) {
                delay(3000)
                currentImageIndex = (currentImageIndex + 1) % backgroundImages.size
            }
        }
    }

    // KARMA SİSTEMİ: IntelligentContentEngine tarafından üretilen kişiselleştirilmiş videolar
    LaunchedEffect(currentVideoIndex, dynamicVideoIds, isMuted) {
        if (dynamicVideoIds.isEmpty()) {
            android.util.Log.w("ReturningUserContent", "⚠️ KARMA SİSTEMİ BAŞARISIZ - dynamicVideoIds boş! Video oynatılamıyor.")
        } else {
            android.util.Log.d("ReturningUserContent", "✅ KARMA SİSTEMİ AKTİF - Video ${currentVideoIndex + 1}/${dynamicVideoIds.size} oynatılıyor")
            val videoResId = dynamicVideoIds[currentVideoIndex % dynamicVideoIds.size]
            val videoUri = "android.resource://${context.packageName}/$videoResId"
            val mediaItem = MediaItem.fromUri(videoUri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            // FIX #38: Volume AFTER prepare() - MediaCodec hazır olduğunda ayarla
            exoPlayer.volume = if (isMuted) 0f else 1f
            if (isPlaying) exoPlayer.play()
            android.util.Log.d("ReturningUserContent", "🎥 RETURNINGUSER Video loaded & volume set AFTER prepare: muted=$isMuted, volume=${exoPlayer.volume}")
        }
    }


    // Video kontrolleri
    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    val playNextVideo = {
        if (dynamicVideoIds.isNotEmpty()) {
            currentVideoIndex = (currentVideoIndex + 1) % dynamicVideoIds.size
        }
    }

    // ✅ AUTOPLAY FIX: Video bitince sıradaki videoyu otomatik oynat
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    playNextVideo()
                }
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // KATMAN 1: Background Slideshow
        Crossfade(
            targetState = currentImageIndex,
            modifier = Modifier.fillMaxSize(),
            label = "returning-user-slideshow"
        ) { index ->
            Image(
                painter = painterResource(id = backgroundImages[index]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // KATMAN 2: KARMA SİSTEMİ - Kişiselleştirilmiş video player
        if (dynamicVideoIds.isNotEmpty()) {
            AndroidView(
                factory = { PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                } },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.6f)
            )
        }

        // KATMAN 3: Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        // G128: Disclaimer metni (sol üst)
        Text(
            text = rememberLocalizedText("intro_disclaimer"),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Start,
            lineHeight = 14.sp,
            style = MaterialTheme.typography.bodySmall
        )

        // KATMAN 4: Hoşgeldin mesajı
        Text(
            text = "Welcome back, $playerName",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        // KATMAN 5: Merkez göz butonu (Icon)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .clickable {
                    try {
                        GameLogger.logSystem("👁️ Geri dönen kullanıcı GÖZ butonuna tıkladı - Dashboard'a geçiliyor")
                        android.util.Log.d("UserEntryScreen", "🔵 onContinue() çağrılıyor...")
                        onContinue()
                        android.util.Log.d("UserEntryScreen", "✅ onContinue() çağrıldı")
                    } catch (e: Exception) {
                        android.util.Log.e("UserEntryScreen", "❌ onContinue() çağrılırken HATA: ${e.message}", e)
                        e.printStackTrace()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.RemoveRedEye,
                contentDescription = "Continue to Dashboard",
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }

        // KATMAN 6: Video kontrolleri (sol alt)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Oynat/Durdur
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White
                )
            }

            // Ses Aç/Kapa
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White
                )
            }

            // Sonraki Video
            IconButton(
                onClick = playNextVideo,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next Video",
                    tint = Color.White
                )
            }
        }

        // KATMAN 7: Version bilgisi (sağ alt - fotoğrafların üstünde)
        Text(
            text = "v0.0.1",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true, name = "User Entry Screen - Loading")
@Composable
fun UserEntryScreenLoadingPreview() {
    IsekaiKuroshinTheme(darkTheme = true) {
        LoadingScreen()
    }
}

@Preview(showBackground = true, name = "User Entry Screen - New User")
@Composable
fun UserEntryScreenNewUserPreview() {
    IsekaiKuroshinTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            // Basitleştirilmiş preview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Transfer is starting...",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome, Chosen One.",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = "Preview User",
                    onValueChange = { },
                    label = { Text("Who are you", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "ACCEPT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@UnstableApi
@Preview(showBackground = true, name = "User Entry Screen - Returning User")
@Composable
fun UserEntryScreenReturningUserPreview() {
    // Preview için SoundManager mock'u gerekli - bu preview devre dışı
    // ReturningUserContent artık SoundManager parametresi bekliyor
}
