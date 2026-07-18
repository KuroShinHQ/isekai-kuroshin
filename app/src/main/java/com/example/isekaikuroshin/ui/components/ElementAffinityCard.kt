package com.example.isekaikuroshin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.ElementAffinity
import com.example.isekaikuroshin.data.ElementType
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * GÖREV 4: Element Affinity Display Card
 *
 * Oyuncunun element affinity'lerini progress bar ile gösterir.
 */
@Composable
fun ElementAffinityCard(
    affinities: Map<String, ElementAffinity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "⚡ ${rememberLocalizedText("element_affinity_title")}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rememberLocalizedText("element_affinity_description"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Her element için progress bar
            val orderedElements = listOf("FIRE", "WATER", "EARTH", "AIR", "LIGHTNING", "LIGHT", "DARK", "NEUTRAL")
            orderedElements.forEach { elementType ->
                affinities[elementType]?.let { affinity ->
                    ElementAffinityRow(
                        affinity = affinity,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ElementAffinityRow(
    affinity: ElementAffinity,
    modifier: Modifier = Modifier
) {
    val elementColor = getElementColor(affinity.elementType.name)
    val bonusPercentage = (affinity.getAffinityBonus() * 100).toInt()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getElementIcon(affinity.elementType.name),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = rememberLocalizedText("element_${affinity.elementType.name.lowercase()}"),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Lv.${affinity.level} (+$bonusPercentage%)",
                fontSize = 12.sp,
                color = elementColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(affinity.getProgressPercentage())
                    .fillMaxHeight()
                    .background(
                        color = elementColor,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        // XP Text
        Text(
            text = "${affinity.currentXP} / ${affinity.xpToNextLevel} XP",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun getElementColor(elementType: String): Color {
    return when (elementType.uppercase()) {
        "FIRE" -> Color(0xFFFF6B35)
        "WATER" -> Color(0xFF4A90E2)
        "EARTH" -> Color(0xFF8B6F47)
        "AIR" -> Color(0xFFC0C0C0)
        "LIGHTNING" -> Color(0xFFFFD700)
        "LIGHT" -> Color(0xFFFFF8DC)
        "DARK" -> Color(0xFF8B008B)
        "NEUTRAL" -> Color(0xFF9E9E9E)
        else -> Color.White
    }
}

private fun getElementIcon(elementType: String): String {
    return when (elementType.uppercase()) {
        "FIRE" -> "🔥"
        "WATER" -> "💧"
        "EARTH" -> "🌍"
        "AIR" -> "💨"
        "LIGHTNING" -> "⚡"
        "LIGHT" -> "✨"
        "DARK" -> "🌑"
        "NEUTRAL" -> "⚪"
        else -> "❓"
    }
}

