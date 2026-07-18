package com.example.isekaikuroshin.ui.vfx.effects

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SpriteWaterEffect(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    targetPosition: Offset? = null
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val center = targetPosition ?: Offset(size.width / 2f, size.height / 2f)
        
        if (isActive) {
            // Su damlası animasyonu
            drawCircle(
                color = Color(0xFF4FC3F7).copy(alpha = alpha),
                radius = 40f * scale,
                center = center
            )
            
            // İç merkez
            drawCircle(
                color = Color(0xFF03A9F4).copy(alpha = alpha * 0.8f),
                radius = 20f * scale,
                center = center
            )
            
            // Parıltı efekti
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.6f),
                radius = 10f * scale,
                center = center + Offset(-8f, -8f)
            )
        }
    }
}

@Composable
fun ParticleWaterEffect(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    targetPosition: Offset? = null
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(isActive) {
        if (isActive) {
            // Partikülleri oluştur
            particles.clear()
            repeat(100) {
                particles.add(
                    Particle(
                        position = targetPosition ?: Offset(
                            Random.nextFloat() * 300 + 50,
                            Random.nextFloat() * 300 + 50
                        ),
                        velocity = Offset(
                            (Random.nextFloat() - 0.5f) * 2f,
                            (Random.nextFloat() - 0.5f) * 2f
                        ),
                        size = Random.nextFloat() * 4f + 2f,
                        color = Color(
                            red = 0.2f + Random.nextFloat() * 0.3f,
                            green = 0.5f + Random.nextFloat() * 0.4f,
                            blue = 0.8f + Random.nextFloat() * 0.2f
                        ),
                        life = 1f
                    )
                )
            }
        }
    }

    LaunchedEffect(particles) {
        if (isActive) {
            while (true) {
                particles.forEachIndexed { index, particle ->
                    particles[index] = particle.copy(
                        position = particle.position + particle.velocity,
                        life = particle.life - 0.01f
                    )
                }
                particles.removeAll { it.life <= 0f }
                delay(16) // ~60 FPS
            }
        } else {
            particles.clear()
        }
    }

    Canvas(modifier = modifier) {
        if (isActive) {
            particles.forEach { particle ->
                drawCircle(
                    color = particle.color.copy(alpha = particle.life),
                    radius = particle.size,
                    center = particle.position
                )
            }
        }
    }
}

@Composable
fun MetaballWaterEffect(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    targetPosition: Offset? = null
) {
    val animationValue by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 500)
    )
    
    val time = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                time.value += 0.016f
                delay(16)
            }
        } else {
            time.value = 0f
        }
    }

    Canvas(modifier = modifier) {
        if (isActive) {
            val center = targetPosition ?: Offset(size.width / 2f, size.height / 2f)
            
            // Meta-ball efekti - basitleştirilmiş sürüm
            val metaballs = listOf(
                center + Offset(
                    cos(time.value * 20) * 20f,
                    sin(time.value * 15) * 15f
                ),
                center + Offset(
                    cos(time.value * 15 + 2f) * 15f,
                    sin(time.value * 20 + 1f) * 20f
                ),
                center + Offset(
                    cos(time.value * 18 + 1f) * 18f,
                    sin(time.value * 12 + 3f) * 12f
                )
            )

            metaballs.forEachIndexed { index, metaballPos ->
                val alpha = 0.7f + sin(time.value + index) * 0.2f
                val size = 30f + cos(time.value + index) * 10f
                
                drawCircle(
                    color = Color(0xFF2196F3).copy(alpha = alpha),
                    radius = size * animationValue,
                    center = metaballPos
                )
            }
        }
    }
}

// Yardımcı veri sınıfları
data class Particle(
    val position: Offset,
    val velocity: Offset,
    val size: Float,
    val color: Color,
    var life: Float
)