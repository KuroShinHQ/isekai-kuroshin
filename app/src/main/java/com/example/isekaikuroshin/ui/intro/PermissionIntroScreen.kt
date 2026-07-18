package com.example.isekaikuroshin.ui.intro

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * G56: Permission Intro Screen
 *
 * "Neden bu izinler gerekli?" açıklama ekranı
 *
 * İzinler:
 * - CAMERA: Savaş sırasında el hareketleri ve gesture tanıma
 * - RECORD_AUDIO: Büyü sözcüklerini seslendirme
 * - POST_NOTIFICATIONS: Quest uyarıları ve hatırlatıcılar (Android 13+)
 */
@Composable
fun PermissionIntroScreen(
    onContinue: () -> Unit
) {
    // Animasyon için state
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1A1F3A),
                        Color(0xFF2D1B4E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Title with fade-in animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = tween(800)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = rememberLocalizedText("permission_intro_title"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = rememberLocalizedText("permission_intro_subtitle"),
                        fontSize = 16.sp,
                        color = Color(0xFFB8B8D0),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Permission cards with staggered animation
            PermissionCard(
                icon = Icons.Default.Camera,
                title = rememberLocalizedText("permission_camera_title"),
                description = rememberLocalizedText("permission_camera_desc"),
                example = rememberLocalizedText("permission_camera_example"),
                delayMillis = 200,
                isVisible = isVisible
            )

            Spacer(modifier = Modifier.height(20.dp))

            PermissionCard(
                icon = Icons.Default.Mic,
                title = rememberLocalizedText("permission_audio_title"),
                description = rememberLocalizedText("permission_audio_desc"),
                example = rememberLocalizedText("permission_audio_example"),
                delayMillis = 400,
                isVisible = isVisible
            )

            Spacer(modifier = Modifier.height(20.dp))

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = rememberLocalizedText("permission_notification_title"),
                description = rememberLocalizedText("permission_notification_desc"),
                example = rememberLocalizedText("permission_notification_example"),
                delayMillis = 600,
                isVisible = isVisible
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Privacy note
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 800))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E2A4A).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = rememberLocalizedText("permission_privacy_note"),
                            fontSize = 13.sp,
                            color = Color(0xFFB8B8D0),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button with pulse animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 1000)) +
                        scaleIn(animationSpec = tween(500, delayMillis = 1000))
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(pulseScale),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4C6FFF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = rememberLocalizedText("permission_continue_button"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    example: String,
    delayMillis: Int,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600, delayMillis = delayMillis)) +
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(600, delayMillis = delayMillis)
                )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2238)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4C6FFF),
                                    Color(0xFF2A3F7F)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color(0xFFB8B8D0),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Example badge
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF2D3A5F).copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = example,
                            fontSize = 12.sp,
                            color = Color(0xFF9FA8DA),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
