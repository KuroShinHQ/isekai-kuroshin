package com.example.isekaikuroshin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * GÖREV 3: Animasyonlu XP Kazanım Gösterimi
 *
 * Kullanıcı büyü pratiği yaptığında ekranda "+50 XP" şeklinde
 * animasyonlu bir bildirim gösterir.
 *
 * @param xpAmount Kazanılan XP miktarı
 * @param elementType Element türü (opsiyonel, renk için)
 * @param visible Overlay görünür mü?
 * @param onDismiss Overlay kaybolduğunda çağrılır
 */
@Composable
fun XPGainOverlay(
    xpAmount: Int,
    elementType: String = "FIRE",
    visible: Boolean,
    onDismiss: () -> Unit
) {
    // Otomatik kaybolma
    LaunchedEffect(visible) {
        if (visible) {
            delay(2000) // 2 saniye göster
            onDismiss()
        }
    }

    // Animasyon state'leri
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )

    // Element rengini belirle
    val elementColor = when (elementType.uppercase()) {
        "FIRE" -> Color(0xFFFF6B35)
        "WATER" -> Color(0xFF4A90E2)
        "EARTH" -> Color(0xFF8B6F47)
        "AIR" -> Color(0xFFC0C0C0)
        "LIGHTNING" -> Color(0xFFFFD700)
        else -> Color(0xFFFF9500) // Varsayılan: Turuncu
    }

    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .scale(scale)
                    .background(
                        color = elementColor.copy(alpha = alpha * 0.9f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "XP",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "+$xpAmount XP",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * XP Overlay State Management
 *
 * Usage:
 * ```
 * val xpOverlayState = rememberXPOverlayState()
 *
 * // Show XP gain
 * xpOverlayState.show(xpAmount = 50, elementType = "FIRE")
 *
 * // In your Composable
 * XPGainOverlay(
 *     xpAmount = xpOverlayState.xpAmount,
 *     elementType = xpOverlayState.elementType,
 *     visible = xpOverlayState.isVisible,
 *     onDismiss = { xpOverlayState.hide() }
 * )
 * ```
 */
@Stable
class XPOverlayState {
    var isVisible by mutableStateOf(false)
        private set
    var xpAmount by mutableIntStateOf(0)
        private set
    var elementType by mutableStateOf("FIRE")
        private set

    fun show(xpAmount: Int, elementType: String = "FIRE") {
        this.xpAmount = xpAmount
        this.elementType = elementType
        this.isVisible = true
    }

    fun hide() {
        this.isVisible = false
    }
}

@Composable
fun rememberXPOverlayState(): XPOverlayState {
    return remember { XPOverlayState() }
}
