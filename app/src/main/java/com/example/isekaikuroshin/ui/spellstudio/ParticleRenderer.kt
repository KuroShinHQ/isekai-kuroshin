package com.example.isekaikuroshin.ui.spellstudio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ÇOK BASIT PARÇACIK SİSTEMI
 * Sadece ekranda görünen, çalışan parçacıklar
 */

data class SimpleParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var life: Float = 1f
)

@Composable
fun ParticleRenderer(
    particleManager: com.example.isekaikuroshin.engine.vfx.ParticleSystemManager,
    modifier: Modifier = Modifier
) {
    val activeEmitters by particleManager.activeEmitters.collectAsState()

    // Basit parçacık listesi
    var particles by remember { mutableStateOf<List<SimpleParticle>>(emptyList()) }

    // Animasyon döngüsü
    LaunchedEffect(activeEmitters) {
        while (isActive) {
            val newParticles = mutableListOf<SimpleParticle>()

            // Her emitter için parçacık oluştur
            activeEmitters.forEach { emitter ->
                // 5 parçacık oluştur
                repeat(5) {
                    val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
                    val speed = Random.nextFloat() * 100f + 50f

                    newParticles.add(
                        SimpleParticle(
                            x = emitter.position.x,
                            y = emitter.position.y,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed,
                            color = emitter.particleColor,
                            size = 10f
                        )
                    )
                }
            }

            // Parçacıkları güncelle
            particles = (particles + newParticles).mapNotNull { p ->
                // Fizik
                p.x += p.vx * 0.016f
                p.y += p.vy * 0.016f
                p.vy += 200f * 0.016f // Yerçekimi
                p.life -= 0.016f

                // Ömrü bittiyse sil
                if (p.life > 0f) p else null
            }

            delay(16) // 60 FPS
        }
    }

    // Parçacıkları çiz
    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.life),
                radius = p.size,
                center = Offset(p.x, p.y)
            )
        }
    }
}
