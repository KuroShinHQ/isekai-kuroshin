package com.example.isekaikuroshin.ui.transition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.PersistentDataManager
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.ui.tooling.preview.Preview
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import androidx.compose.ui.text.font.FontWeight

@UnstableApi
@Composable
fun TransitionScreen(
    codename: String,
    onTransitionComplete: (String) -> Unit
) {
    val context = LocalContext.current
    var showHandEyeEmoji by remember { mutableStateOf(false) }
    val gameData by PersistentDataManager.gameData.collectAsState()
    val dynamicPlaylist = gameData.storyData.dynamicPlaylist

    // Background video player - FB#12: Geçiş ekranında arka plan videoları SES KAPALI
    var backgroundIndex by remember { mutableIntStateOf(0) }
    val backgroundPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f // Geçiş ekranında arka plan ses yok
        }
    }

    LaunchedEffect(backgroundIndex, dynamicPlaylist) {
        if (dynamicPlaylist.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(dynamicPlaylist[backgroundIndex])
            backgroundPlayer.setMediaItem(mediaItem)
            backgroundPlayer.prepare()
            backgroundPlayer.playWhenReady = true
            backgroundPlayer.repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Change video every 5 seconds
            if (dynamicPlaylist.isNotEmpty()) {
                backgroundIndex = (backgroundIndex + 1) % dynamicPlaylist.size
            }
        }
    }

    // Main transition video player - FB#12: Geçiş ekranında ses KAPALI
    val transitionPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f // Transition ekranında ses yok
        }
    }

    val playTransitionVideo = remember {
        {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.butterfly_transformation}")
            transitionPlayer.setMediaItem(mediaItem)
            transitionPlayer.prepare()
            transitionPlayer.play()
        }
    }

    LaunchedEffect(Unit) {
        transitionPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    transitionPlayer.seekTo(transitionPlayer.duration - 1) // Go to last frame
                    transitionPlayer.pause() // Pause at the end
                    showHandEyeEmoji = true
                }
            }
        })
        playTransitionVideo()
    }

    DisposableEffect(Unit) {
        onDispose {
            backgroundPlayer.release()
            transitionPlayer.release()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .clickable {
            // Skip animation on tap
            transitionPlayer.pause()
            onTransitionComplete(codename)
        }
    ) {
        // Background video player
        AndroidView(
            factory = { PlayerView(it).apply {
                player = backgroundPlayer
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(android.graphics.Color.BLACK)
            } },
            modifier = Modifier.fillMaxSize()
        )

        // Main transition video
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = transitionPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Clickable Emote Overlay (after transition video)
        if (showHandEyeEmoji) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onTransitionComplete(codename) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨", // Example emote
                    fontSize = 80.sp,
                    modifier = Modifier.alpha(0.9f)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Transition Screen - Dark")
@Composable
fun TransitionScreenPreview() {
    IsekaiKuroshinTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )

            // Transition video placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🦋",
                    fontSize = 120.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Emote overlay (visible state)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨",
                    fontSize = 80.sp,
                    modifier = Modifier.alpha(0.9f)
                )
            }

            // Loading text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TRANSFORMATION IN PROGRESS...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Codename: Preview Player",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}