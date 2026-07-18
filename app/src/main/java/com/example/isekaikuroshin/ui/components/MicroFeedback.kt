package com.example.isekaikuroshin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * GÖREV I: MicroFeedbackItem - Tek bir stat değişimi bildirimi
 */
data class MicroFeedbackItem(
    val statName: String,      // "Stamina", "HP", "Agility"
    val change: Int,           // -15, +5
    val icon: ImageVector,     // Icons.Default.DirectionsRun
    val color: Color = Color.White
)

/**
 * GÖREV I: MicroFeedbackOverlay - Stat değişimlerini gösteren animasyonlu overlay
 *
 * Kullanım:
 * - Günlük input sonrası stat güncellemeleri
 * - Egzersiz sonrası stat değişimleri
 * - Combat sonrası stat değişimleri
 *
 * Davranış:
 * - Yukarıdan kayarak gelir (slideInVertically)
 * - 3 saniye ekranda kalır
 * - Soluklaşarak kaybolur (fadeOut)
 * - Sağ üst köşede gösterilir
 */
@Composable
fun MicroFeedbackOverlay(
    feedbacks: List<MicroFeedbackItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    // Feedbacks değiştiğinde animasyon tetikle
    LaunchedEffect(feedbacks) {
        if (feedbacks.isNotEmpty()) {
            visible = true
            delay(3000)  // 3 saniye göster
            visible = false
            delay(500)   // Fade out animasyonu bitsin
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },  // Yukarıdan gelir
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = fadeOut(tween(500))
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            feedbacks.forEach { feedback ->
                MicroFeedbackCard(feedback)
            }
        }
    }
}

/**
 * GÖREV I: MicroFeedbackCard - Tek bir feedback kartı
 */
@Composable
private fun MicroFeedbackCard(feedback: MicroFeedbackItem) {
    val changeColor = if (feedback.change > 0) {
        Color(0xFF4CAF50)  // Yeşil (pozitif)
    } else {
        Color(0xFFF44336)  // Kırmızı (negatif)
    }

    val changeText = if (feedback.change > 0) {
        "+${feedback.change}"
    } else {
        "${feedback.change}"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = feedback.icon,
                contentDescription = null,
                tint = feedback.color,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = changeText,
                color = changeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = feedback.statName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * GÖREV I: Helper fonksiyonlar - Stat ismine göre ikon seç
 */
object MicroFeedbackHelper {

    fun getIconForStat(statName: String): ImageVector {
        return when (statName.uppercase()) {
            "STAMINA", "STA" -> Icons.Default.DirectionsRun
            "HP", "CAN" -> Icons.Default.Favorite
            "MP", "MANA" -> Icons.Default.Stars
            "AGILITY", "AGI", "ÇEVİKLİK" -> Icons.Default.Speed
            "STRENGTH", "STR", "GÜÇ" -> Icons.Default.FitnessCenter
            "INTELLIGENCE", "INT", "ZEKA" -> Icons.Default.Psychology
            "VITALITY", "VIT", "CANLILIK" -> Icons.Default.Shield
            "SPIRIT", "RUH" -> Icons.Default.SelfImprovement
            "LUCK", "ŞANS" -> Icons.Default.Star
            "HUNGER", "AÇLIK" -> Icons.Default.Restaurant
            "FATIGUE", "YORGUNLUK" -> Icons.Default.Bedtime
            "EXP", "TECRÜBE" -> Icons.Default.TrendingUp
            else -> Icons.Default.TrendingUp
        }
    }

    fun getColorForStat(statName: String): Color {
        return when (statName.uppercase()) {
            "HP", "CAN" -> Color(0xFFE91E63)  // Pink
            "MP", "MANA" -> Color(0xFF2196F3)  // Blue
            "STAMINA", "STA" -> Color(0xFF4CAF50)  // Green
            "AGILITY", "ÇEVİKLİK" -> Color(0xFFFFEB3B)  // Yellow
            "STRENGTH", "GÜÇ" -> Color(0xFFFF5722)  // Deep Orange
            "INTELLIGENCE", "ZEKA" -> Color(0xFF9C27B0)  // Purple
            "EXP", "TECRÜBE" -> Color(0xFF00BCD4)  // Cyan
            else -> Color.White
        }
    }

    /**
     * Convenience function - Stat değişimi bildirimini oluştur
     */
    fun createFeedback(statName: String, change: Int): MicroFeedbackItem {
        return MicroFeedbackItem(
            statName = statName,
            change = change,
            icon = getIconForStat(statName),
            color = getColorForStat(statName)
        )
    }
}
