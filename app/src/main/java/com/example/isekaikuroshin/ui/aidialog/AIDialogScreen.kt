package com.example.isekaikuroshin.ui.aidialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.isekaikuroshin.R
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import com.example.isekaikuroshin.ai.GlobalAIManager
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout

// AI Entity data class
data class AIEntity(
    val name: String = "Umbros",
    val title: String = "The Sealed Demon",
    val mood: String = "Observant",
    val contractLevel: Int = 3,
    val powerLevel: Int = 75
)

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIDialogScreen(navController: NavController) {
    var currentDialog by remember { mutableStateOf("I sense your presence. What do you seek?") }
    var isAiThinking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Use Global AI Manager instead of local instance
    val isModelInitialized by GlobalAIManager.isModelInitialized.collectAsState()
    val loadingProgress by GlobalAIManager.loadingProgress.collectAsState()
    val loadingStatus by GlobalAIManager.loadingStatus.collectAsState()
    val isLoading by GlobalAIManager.isLoading.collectAsState()

    // No need for DisposableEffect - Global AI Manager handles lifecycle

    LaunchedEffect(isModelInitialized, loadingStatus, isLoading) {
        currentDialog = when {
            isLoading -> "$loadingStatus ${(loadingProgress * 100).toInt()} %"
            isModelInitialized -> "I sense your presence. What do you seek?"
            !isModelInitialized && !isLoading -> "Tap the red eye on Dashboard to activate AI"
            else -> "AI model failed to initialize"
        }
    }

    // Background slideshow animation with real demon images
    var backgroundIndex by remember { mutableIntStateOf(0) }
    val demonImages = listOf(
        R.drawable.demon_bg_00,
        R.drawable.demon_bg_01,
        R.drawable.demon_bg_02,
        R.drawable.demon_bg_03,
        R.drawable.demon_bg_04,
        R.drawable.demon_bg_05,
        R.drawable.demon_bg_06,
        R.drawable.demon_bg_07,
        R.drawable.demon_bg_08,
        R.drawable.demon_bg_09,
        R.drawable.demon_bg_10,
        R.drawable.demon_bg_11,
        R.drawable.demon_bg_12,
        R.drawable.demon_bg_13,
        R.drawable.demon_bg_14,
        R.drawable.demon_bg_15,
        R.drawable.demon_bg_16,
        R.drawable.demon_bg_17
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(800) // 0.8 second interval
            backgroundIndex = (backgroundIndex + 1) % demonImages.size
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background demon image
        Image(
            painter = painterResource(id = demonImages[backgroundIndex]),
            contentDescription = "Demon Background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.9f), // Increased visibility for examination
            contentScale = ContentScale.Crop
        )
        // Light overlay for readability (reduced opacity to show slideshow)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEC131E).copy(alpha = 0.1f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with report button only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { /* Report functionality */ }) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = "Report",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Main content centered
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // AI Entity Portrait Section
                UmbrosPortraitE1()

                Spacer(modifier = Modifier.height(32.dp))

                // Dialog Section with Progress Bar and Thinking Indicator
                DialogBubbleE2(
                    currentDialog = currentDialog,
                    showProgress = isLoading && loadingProgress > 0f,
                    progress = loadingProgress,
                    isThinking = isAiThinking
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Text input for chatting with AI
                AITextInputE4(
                    onMessageSent = { message ->
                        if (isModelInitialized) {
                            coroutineScope.launch {
                                isAiThinking = true
                                currentDialog = "Thinking..."

                                // Better prompt engineering for demon character
                                val enhancedPrompt = "You are Umbros, a powerful sealed demon with an ancient, mysterious personality. Keep responses under 100 words and maintain the dark fantasy character. User says: \"$message\""

                                try {
                                    val response = GlobalAIManager.generateResponse(enhancedPrompt)
                                    currentDialog = response
                                } catch (e: Exception) {
                                    currentDialog = "I cannot respond at this moment. Try again."
                                } finally {
                                    isAiThinking = false
                                }
                            }
                        } else {
                            currentDialog = "AI model is not ready yet."
                        }
                    },
                    enabled = isModelInitialized && !isAiThinking
                )
            }
        }

        // Back button at bottom left
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(
                    color = Color(0xFFEC131E).copy(alpha = 0.8f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@UnstableApi
@Composable
private fun UmbrosPortraitE1() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Demon portrait with red glow effect
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Color(0xFFEC131E), CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEC131E).copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        radius = 120f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Umbros Eye Effect Video - REMOVED (SORUN 2)
            // UmbrosEyeVideoE5(
            //     modifier = Modifier.size(96.dp)
            // )
        }

        // Name and status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Umbros",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💀",
                    fontSize = 14.sp
                )
                Text(
                    text = "Ω",
                    fontSize = 14.sp,
                    color = Color(0xFFEC131E),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Omega Level",
                    fontSize = 14.sp,
                    color = Color(0xFFEC131E).copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun DialogBubbleE2(
    currentDialog: String,
    showProgress: Boolean = false,
    progress: Float = 0f,
    isThinking: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 300.dp)  // Minimum and maximum height
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thinking indicator with animated dots
            if (isThinking) {
                ThinkingIndicatorE3()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Scrollable text for long responses
            Text(
                text = currentDialog,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = if (currentDialog.length > 50) TextAlign.Start else TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )

            if (showProgress) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFFEC131E),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun ThinkingIndicatorE3() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Text(
                text = "●",
                color = Color(0xFFEC131E).copy(alpha = alpha),
                fontSize = 20.sp,
                modifier = Modifier.alpha(
                    if (index == 1) alpha else alpha * 0.6f
                )
            )
            if (index < 2) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun AITextInputE4(onMessageSent: (String) -> Unit, enabled: Boolean = true) {
    var message by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            enabled = enabled,
            placeholder = {
                Text(
                    if (enabled) "Type your message..." else "AI is thinking...",
                    color = Color.White.copy(alpha = if (enabled) 0.4f else 0.2f)
                )
            },
            modifier = Modifier
                .weight(1f)
                .background(
                    Color.Black.copy(alpha = if (enabled) 0.3f else 0.1f),
                    RoundedCornerShape(50.dp)
                ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White.copy(alpha = 0.5f),
                focusedBorderColor = Color(0xFFEC131E),
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                cursorColor = Color(0xFFEC131E)
            ),
            shape = RoundedCornerShape(50.dp),
            singleLine = true
        )

        Button(
            onClick = {
                if (message.isNotBlank() && enabled) {
                    onMessageSent(message)
                    message = ""
                }
            },
            enabled = enabled && message.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEC131E),
                disabledContainerColor = Color(0xFFEC131E).copy(alpha = 0.3f)
            ),
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Text(
                text = "→",
                color = if (enabled && message.isNotBlank()) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// REMOVED: UmbrosEyeVideoE5 - eye_effect video removed per user request (SORUN 2)
// @UnstableApi
// @Composable
// private fun UmbrosEyeVideoE5(modifier: Modifier = Modifier) {
//     // Function disabled - eye_effect video should not be shown
// }
