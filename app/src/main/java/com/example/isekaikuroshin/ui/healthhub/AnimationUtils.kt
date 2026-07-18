package com.example.isekaikuroshin.ui.healthhub

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * FAZ 3.1: Animasyonlu Sayaç Composable
 * Slot makinesi efekti ile sayı değişimini gösterir
 *
 * @param targetValue Hedef değer
 * @param modifier Modifier
 * @param suffix Değer sonuna eklenecek metin (örn: "kg", "kcal")
 * @param textColor Metin rengi
 * @param fontSize Font boyutu
 */
@Composable
fun AnimatedCounter(
    targetValue: Float,
    modifier: Modifier = Modifier,
    suffix: String = "",
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: Int = 24
) {
    var currentValue by remember { mutableStateOf(0f) }

    // Animasyonlu değer geçişi
    val animatedValue by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "counter_animation"
    )

    LaunchedEffect(animatedValue) {
        currentValue = animatedValue
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%.1f", currentValue),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        if (suffix.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = suffix,
                fontSize = (fontSize * 0.7).toInt().sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * FAZ 3.1: Konfeti Animasyonu
 * Başarı durumlarında gösterilecek konfeti efekti
 *
 * @param isVisible Animasyonun görünürlüğü
 * @param onAnimationComplete Animasyon tamamlandığında çağrılır
 */
@Composable
fun ConfettiAnimation(
    isVisible: Boolean,
    onAnimationComplete: () -> Unit = {}
) {
    if (!isVisible) return

    // Konfeti parçacıkları
    val particleCount = 30
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                color = listOf(
                    Color(0xFFFF6B6B),
                    Color(0xFF4ECDC4),
                    Color(0xFFFFE66D),
                    Color(0xFF95E1D3),
                    Color(0xFFF38181)
                ).random(),
                startX = Random.nextFloat(),
                velocity = Random.nextFloat() * 2f + 1f,
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        delay(500)
        onAnimationComplete()
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        particles.forEach { particle ->
            val progress = animationProgress.value
            val x = size.width * particle.startX
            val y = size.height * (1f - progress) * particle.velocity
            val rotation = particle.rotation * progress

            drawCircle(
                color = particle.color.copy(alpha = 1f - progress),
                radius = 8f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Konfeti parçacığı veri modeli
 */
private data class ConfettiParticle(
    val color: Color,
    val startX: Float,
    val velocity: Float,
    val rotation: Float
)

/**
 * FAZ 3.1: Nabız Animasyonu
 * Hedef tamamlama veya önemli olaylar için
 *
 * @param isActive Animasyonun aktif olup olmadığı
 * @param content İçerik composable
 */
@Composable
fun PulseAnimation(
    isActive: Boolean,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale
        )
    ) {
        content()
    }
}

/**
 * FAZ 3.1: Ardışık Gün Göstergesi (Streak Flame)
 * Kullanıcının ardışık günlerdeki aktivitesini gösterir
 *
 * @param streakDays Ardışık gün sayısı
 * @param modifier Modifier
 */
@Composable
fun StreakFlameIcon(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val flameEmoji = when {
        streakDays >= 30 -> "🔥🔥🔥"
        streakDays >= 14 -> "🔥🔥"
        streakDays >= 7 -> "🔥"
        else -> "⭐"
    }

    // Titreşim animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "flame_shake")
    val shake by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake_offset"
    )

    Column(
        modifier = modifier.graphicsLayer(translationX = shake),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = flameEmoji,
            fontSize = 32.sp
        )
        Text(
            text = rememberLocalizedText("streak_days").replace("{days}", streakDays.toString()),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * FAZ 3.1: Yüzde İlerleme Çubuğu Animasyonu
 * Progress bar ile birlikte animasyonlu yüzde gösterimi
 *
 * @param currentValue Mevcut değer
 * @param targetValue Hedef değer
 * @param modifier Modifier
 * @param onTargetReached Hedefe ulaşıldığında çağrılır
 */
@Composable
fun AnimatedProgressWithPercentage(
    currentValue: Float,
    targetValue: Float,
    modifier: Modifier = Modifier,
    onTargetReached: () -> Unit = {}
) {
    val progress = (currentValue / targetValue).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )

    // Hedefe ulaşma kontrolü
    LaunchedEffect(animatedProgress) {
        if (animatedProgress >= 1f) {
            onTargetReached()
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress bar
        androidx.compose.material3.LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = if (animatedProgress >= 1f)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Yüzde metni
        Text(
            text = "${(animatedProgress * 100).roundToInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * FAZ 3.1: Başarı Rozeti Animasyonu
 * Yeni başarı kazanıldığında gösterilecek animasyon
 *
 * @param badgeEmoji Rozet emoji
 * @param badgeTitle Rozet başlığı
 * @param isVisible Görünürlük
 */
@Composable
fun AchievementBadgeAnimation(
    badgeEmoji: String,
    badgeTitle: String,
    isVisible: Boolean
) {
    if (!isVisible) return

    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(-180f) }

    LaunchedEffect(isVisible) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(
        modifier = Modifier
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                rotationZ = rotation.value
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = badgeEmoji,
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = badgeTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * FAZ 5: Subtle Floating Particles Background
 * Arka planda hafif yüzen parçacıklar (ambient animation)
 *
 * @param particleCount Parçacık sayısı (daha az = daha performanslı)
 * @param color Parçacık rengi
 */
@Composable
fun SubtleFloatingParticles(
    particleCount: Int = 15,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
) {
    val particles = remember {
        List(particleCount) {
            FloatingParticle(
                initialY = Random.nextFloat(),
                initialX = Random.nextFloat(),
                speed = Random.nextFloat() * 0.3f + 0.1f,
                size = Random.nextFloat() * 20f + 10f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "floating_particles")

    // Tüm parçacıkların animasyonlarını Canvas dışında hesapla
    val particleAnimations = particles.map { particle ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (10000 / particle.speed).toInt(),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "particle_${particle.hashCode()}"
        )
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        particles.forEachIndexed { index, particle ->
            val time = particleAnimations[index].value

            val y = (size.height * particle.initialY - time * size.height) % size.height
            val x = size.width * particle.initialX

            drawCircle(
                color = color,
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Floating particle veri modeli
 */
private data class FloatingParticle(
    val initialY: Float,
    val initialX: Float,
    val speed: Float,
    val size: Float
)
