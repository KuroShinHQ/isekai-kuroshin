package com.example.isekaikuroshin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.StatusEffect
import com.example.isekaikuroshin.data.StatusEffectType
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.utils.EmojiHelper // G126: Emoji display

/**
 * G82: Status Effect Card Component
 *
 * Buff/Debuff'ları görselleştiren kompakt kart bileşeni.
 * Combat, CharacterStatus ve diğer UI ekranlarında kullanılır.
 *
 * KULLANIM:
 * ```kotlin
 * StatusEffectCard(
 *     effect = CommonStatusEffects.poison(),
 *     remainingTurns = 3,
 *     onRemove = { /* Effecti kaldır */ }
 * )
 * ```
 */

// Ana tema renkleri - Composable context'te kullanılmalı
@Composable
private fun getCardTheme() = object {
    val background = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary
    val text = MaterialTheme.colorScheme.onBackground
}

/**
 * Kompakt Status Effect Card (Horizontal Layout)
 *
 * Combat ekranında veya diğer yerlerde yan yana gösterim için optimize edilmiş.
 * @param effect StatusEffect instance
 * @param remainingTurns Kalan turn sayısı (0 = sonsuz)
 * @param onClick Kart tıklandığında detay göster (opsiyonel)
 * @param onRemove Effecti erken kaldırma (opsiyonel, admin/debug)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusEffectCard(
    effect: StatusEffect,
    remainingTurns: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    // Effect tipine göre renk
    val effectColor = when (effect.type) {
        StatusEffectType.BUFF -> Color(0xFF4CAF50) // Yeşil
        StatusEffectType.DEBUFF -> Color(0xFFE53935) // Kırmızı
    }

    // Süreli mi yoksa kalıcı mı?
    val isPermanent = effect.durationTurns == -1

    // Parlama efekti (debufflar için daha hızlı)
    val infiniteTransition = rememberInfiniteTransition(label = "effect_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (effect.type == StatusEffectType.DEBUFF) 1500 else 3000,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .width(140.dp)
            .height(90.dp)
            .combinedClickable(
                enabled = onClick != null || onRemove != null,
                onClickLabel = "Detay gör",
                onLongClickLabel = "Kaldır",
                onClick = { onClick?.invoke() },
                onLongClick = { onRemove?.invoke() }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        effectColor.copy(alpha = pulseAlpha * 0.5f),
                        effectColor.copy(alpha = pulseAlpha * 0.8f)
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        getCardTheme().background.copy(alpha = 0.9f),
                        effectColor.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst kısım: Emoji + İsim + Tip badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // G126: Emoji icon
                    Text(
                        text = EmojiHelper.getStatusEffectEmoji(effect),
                        fontSize = 20.sp,
                        modifier = Modifier.graphicsLayer {
                            shadowElevation = 2.dp.toPx()
                        }
                    )

                    // Effect ismi
                    Text(
                        text = effect.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = effectColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            shadowElevation = 2.dp.toPx()
                        }
                    )
                }

                // Tip badge
                Box(
                    modifier = Modifier
                        .background(
                            color = effectColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (effect.type == StatusEffectType.BUFF) "BUFF" else "DEBUFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = effectColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Açıklama (kompakt) - Dinamik oluştur
            val description = when {
                effect.tickDamage > 0 -> "${effect.tickDamage} damage/turn"
                effect.tickDamage < 0 -> "${-effect.tickDamage} heal/turn"
                effect.statModifiers.isNotEmpty() -> effect.statModifiers.entries.joinToString(", ") { "${it.key} ${if (it.value > 0) "+" else ""}${it.value}" }
                else -> effect.type.name
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = getCardTheme().text.copy(alpha = 0.7f),
                fontSize = 9.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 10.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Alt kısım: Magnitude + Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Magnitude (eğer varsa)
                if (effect.magnitude != 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Etki:",
                            style = MaterialTheme.typography.labelSmall,
                            color = getCardTheme().text.copy(alpha = 0.6f),
                            fontSize = 8.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (effect.magnitude > 0) "+${effect.magnitude}" else "${effect.magnitude}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (effect.magnitude > 0) Color.Green else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isPermanent) Color(0xFFFFB300) else effectColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isPermanent) "∞" else "$remainingTurns tur",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPermanent) Color.White else effectColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

/**
 * Detaylı Status Effect Card (Vertical Layout)
 *
 * Status ekranında veya modal'da kullanılır.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusEffectCardDetailed(
    effect: StatusEffect,
    remainingTurns: Int,
    currentTurn: Int,
    startTurn: Int,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    val effectColor = when (effect.type) {
        StatusEffectType.BUFF -> Color(0xFF4CAF50)
        StatusEffectType.DEBUFF -> Color(0xFFE53935)
    }

    val isPermanent = effect.durationTurns == -1
    val progress = if (!isPermanent) {
        val totalTurns = effect.durationTurns
        val elapsedTurns = currentTurn - startTurn
        (totalTurns - elapsedTurns).toFloat() / totalTurns.toFloat()
    } else {
        1f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onRemove != null) {
                    Modifier.combinedClickable(
                        onLongClickLabel = "Kaldır",
                        onLongClick = { onRemove.invoke() },
                        onClick = {}
                    )
                } else Modifier
            )
            .border(
                width = 2.dp,
                color = effectColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        getCardTheme().background.copy(alpha = 0.95f),
                        effectColor.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Başlık: Emoji + İsim + Tip badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // G126: Emoji icon (larger for detailed view)
                    Text(
                        text = EmojiHelper.getStatusEffectEmoji(effect),
                        fontSize = 32.sp
                    )

                    Text(
                        text = effect.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = effectColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = effectColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (effect.type == StatusEffectType.BUFF) "BUFF" else "DEBUFF",
                        style = MaterialTheme.typography.labelMedium,
                        color = effectColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Açıklama - Dinamik oluştur
            val detailedDescription = when {
                effect.tickDamage > 0 -> "Deals ${effect.tickDamage} damage each turn"
                effect.tickDamage < 0 -> "Restores ${-effect.tickDamage} health each turn"
                effect.statModifiers.isNotEmpty() -> {
                    val mods = effect.statModifiers.entries.joinToString(", ") {
                        "${it.key.replaceFirstChar { char -> char.uppercase() }} ${if (it.value > 0) "+" else ""}${it.value}"
                    }
                    "Modifies: $mods"
                }
                else -> "${effect.type.name} effect"
            }
            Text(
                text = detailedDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = getCardTheme().text.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 16.sp
            )

            HorizontalDivider(color = getCardTheme().text.copy(alpha = 0.2f))

            // İstatistikler
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Magnitude
                if (effect.magnitude != 0) {
                    StatRow(
                        label = "Etki Gücü",
                        value = if (effect.magnitude > 0) "+${effect.magnitude}" else "${effect.magnitude}",
                        valueColor = if (effect.magnitude > 0) Color.Green else Color.Red
                    )
                }

                // Tick Damage (DoT)
                if (effect.tickDamage != 0) {
                    StatRow(
                        label = "Tur Başına Hasar",
                        value = if (effect.tickDamage > 0) "${effect.tickDamage}" else "+${-effect.tickDamage}",
                        valueColor = if (effect.tickDamage > 0) Color.Red else Color.Green
                    )
                }

                // Duration
                StatRow(
                    label = "Süre",
                    value = if (isPermanent) "Kalıcı" else "$remainingTurns / ${effect.durationTurns} tur",
                    valueColor = if (isPermanent) Color(0xFFFFB300) else effectColor
                )

                // Stat Modifiers
                if (effect.statModifiers.isNotEmpty()) {
                    Text(
                        text = "Stat Değişiklikleri:",
                        style = MaterialTheme.typography.labelMedium,
                        color = getCardTheme().text.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    effect.statModifiers.forEach { (stat, value) ->
                        StatRow(
                            label = stat,
                            value = if (value > 0) "+$value" else "$value",
                            valueColor = if (value > 0) Color.Green else Color.Red
                        )
                    }
                }
            }

            // Progress bar (sadece süreli effectler için)
            if (!isPermanent) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Kalan Süre",
                        style = MaterialTheme.typography.labelSmall,
                        color = getCardTheme().text.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = effectColor,
                        trackColor = getCardTheme().text.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = getCardTheme().text.copy(alpha = 0.7f),
            fontSize = 11.sp
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

// Preview
@Preview(showBackground = true, backgroundColor = 0xFF0A0F1A)
@Composable
private fun StatusEffectCardPreview() {
    IsekaiKuroshinTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1A))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kompakt kartlar (yan yana)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusEffectCard(
                    effect = StatusEffect(
                        effectId = "poison",
                        name = "Zehir",
                        type = StatusEffectType.DEBUFF,
                        durationTurns = 3,
                        magnitude = -5,
                        tickDamage = 5
                    ),
                    remainingTurns = 2
                )

                StatusEffectCard(
                    effect = StatusEffect(
                        effectId = "strength_buff",
                        name = "Güç",
                        type = StatusEffectType.BUFF,
                        durationTurns = 5,
                        magnitude = 10,
                        statModifiers = mapOf("STR" to 10)
                    ),
                    remainingTurns = 4
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detaylı kart
            StatusEffectCardDetailed(
                effect = StatusEffect(
                    effectId = "berserk",
                    name = "Berserk Modu",
                    type = StatusEffectType.BUFF,
                    durationTurns = 4,
                    magnitude = 20,
                    tickDamage = 0,
                    statModifiers = mapOf("STR" to 15, "AGI" to 10, "VIT" to -5)
                ),
                remainingTurns = 2,
                currentTurn = 12,
                startTurn = 10
            )
        }
    }
}
