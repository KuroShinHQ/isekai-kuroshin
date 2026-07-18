package com.example.isekaikuroshin.ui.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.PersistentDataManager
import kotlinx.coroutines.delay
import kotlin.random.Random

@UnstableApi
@Composable
fun PostDeathKarmaScreen(
    navController: NavController,
    dynamicVideoIds: List<Int>,
    dynamicPhotoIds: List<Int>
) {
    val context = LocalContext.current

    // POSTDEATH içerikleri - IntelligentContentEngine'den gelecek
    val backgroundVideos = remember(dynamicVideoIds) {
        if (dynamicVideoIds.isNotEmpty()) {
            android.util.Log.d("PostDeathKarmaScreen", "✅ KARMA SİSTEMİ AKTİF - ${dynamicVideoIds.size} kişiselleştirilmiş video yüklendi")
            dynamicVideoIds
        } else {
            android.util.Log.w("PostDeathKarmaScreen", "⚠️ KARMA SİSTEMİ BAŞARISIZ - Fallback: POSTDEATH/D1 videolar kullanılıyor")
            // Fallback: DEPTH 1 (Surface level) POSTDEATH videolar
            val fallbackVideos = com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForScreen(
                screenType = "POSTDEATH",
                depth = "D1"
            ).filter { it.fileName.startsWith("v_") }.map { it.resourceId }

            if (fallbackVideos.isNotEmpty()) fallbackVideos else listOf(R.raw.intro_animation)
        }
    }

    val backgroundImages = remember(dynamicPhotoIds) {
        if (dynamicPhotoIds.isNotEmpty()) {
            android.util.Log.d("PostDeathKarmaScreen", "✅ KARMA SİSTEMİ AKTİF - ${dynamicPhotoIds.size} kişiselleştirilmiş fotoğraf yüklendi")
            dynamicPhotoIds
        } else {
            android.util.Log.w("PostDeathKarmaScreen", "⚠️ KARMA SİSTEMİ BAŞARISIZ - Fallback: POSTDEATH/D1 fotoğraflar kullanılıyor")
            // Fallback: DEPTH 1 fotoğraflar
            val fallbackPhotos = com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForScreen(
                screenType = "POSTDEATH",
                depth = "D1"
            ).filter { it.fileName.startsWith("p_") }.map { it.resourceId }

            if (fallbackPhotos.isNotEmpty()) fallbackPhotos else listOf(R.drawable.ic_launcher_foreground)
        }
    }

    var currentImageIndex by remember { mutableIntStateOf(0) }
    var currentVideoIndex by remember {
        mutableIntStateOf(if (backgroundVideos.isNotEmpty()) Random.nextInt(0, backgroundVideos.size) else 0)
    }

    // FIX #43: Video kontrol state'leri (ReturningUser gibi)
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    // Video player setup - POSTDEATH video SES AÇIK!
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF // Sadece bir kez oynat
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

    // FIX #43: Video setup with volume control (AFTER prepare() like FIX #38)
    LaunchedEffect(currentVideoIndex, backgroundVideos.size, isMuted) {
        if (backgroundVideos.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${backgroundVideos[currentVideoIndex]}")
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            // FIX #43: Volume AFTER prepare() - MediaCodec hazır olduğunda ayarla
            exoPlayer.volume = if (isMuted) 0f else 1f
            exoPlayer.play()
        }
    }

    // FIX #43: Play/Pause control
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Video bittiğinde Umbros ekranına git
    LaunchedEffect(backgroundVideos.size) {
        if (backgroundVideos.isNotEmpty()) {
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        android.util.Log.d("PostDeathKarmaScreen", "🎬 POSTDEATH video bitti, Umbros ekranına gidiliyor")
                        navController.navigate("umbros_transition") {
                            popUpTo("postdeath_karma") { inclusive = true }
                        }
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
            label = "postdeath-slideshow"
        ) { index ->
            if (backgroundImages.isNotEmpty()) {
                Image(
                    painter = painterResource(id = backgroundImages[index]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // FIX #43: Arka plan overlay kaldırıldı - fotoğraflar net görünsün
        // (Video zaten yarı saydam efekt veriyor)

        // KATMAN 2: Video Oynatıcı (Tam Ekran)
        if (backgroundVideos.isNotEmpty()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // FIX #42: MERKEZ GÖZ BUTONU (ReturningUser gibi)
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
                    android.util.Log.d("PostDeathKarmaScreen", "👁️ Göz ikonuna tıklandı, Umbros ekranına gidiliyor")
                    exoPlayer.stop()
                    navController.navigate("umbros_transition") {
                        popUpTo("postdeath_karma") { inclusive = true }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RemoveRedEye,
                contentDescription = "Skip to Umbros",
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }

        // FIX #43: Video Kontrol Butonları (Sol Alt - ReturningUser gibi)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play/Pause butonu
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Mute/Unmute butonu
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
