package com.example.isekaikuroshin.ui.combat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.combat.ElementType
import com.example.isekaikuroshin.data.combat.LearnedSpell
import com.example.isekaikuroshin.data.combat.getXPToNextLevel
import com.example.isekaikuroshin.data.combat.getLevelProgress

/**
 * G�REV N - FAZ 3: �renilmi_ B�y� Se�im Dialog
 *
 * Kullan1c1n1n sava_ta kullanabilecei �renilmi_ b�y�leri listeler.
 * Her b�y� kart1:
 * - B�y� ad1
 * - Skill tree seviyesi
 * - Element tipi
 * - Ortalama doruluk skoru
 */
@Composable
fun SpellSelectionDialog(
    learnedSpells: List<LearnedSpell>,
    onSpellSelected: (LearnedSpell) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Ba_l1k
                Text(
                    text = LanguageManager.getText("spell_selection_title"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Alt ba_l1k
                Text(
                    text = LanguageManager.getText("spell_selection_subtitle"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // �renilmi_ b�y� listesi
                if (learnedSpells.isEmpty()) {
                    // Bo_ durum
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = LanguageManager.getText("no_learned_spells"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = LanguageManager.getText("no_learned_spells_hint"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // B�y� listesi
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(learnedSpells) { spell ->
                            SpellCard(
                                spell = spell,
                                onClick = {
                                    onSpellSelected(spell)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 0ptal butonu
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(LanguageManager.getText("cancel"))
                }
            }
        }
    }
}

/**
 * Tek bir �renilmi_ b�y� kart1
 */
@Composable
fun SpellCard(
    spell: LearnedSpell,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Element ikonu
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getElementColor(spell.element)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getElementIcon(spell.element),
                    fontSize = 24.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // B�y� bilgileri
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // B�y� ad1
                Text(
                    text = spell.customName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Skill seviyesi
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${LanguageManager.getText("skill_level")}: ${spell.skillTreeLevel}/10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Kullan1m istatistikleri
                Text(
                    text = "${LanguageManager.getText("practice_count")}: ${spell.practiceCount} | ${LanguageManager.getText("combat_usage")}: ${spell.combatUsageCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                // FAZ 7: XP Progress Bar
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Lv.${spell.skillTreeLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { spell.getLevelProgress() },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${spell.getXPToNextLevel()} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Doruluk g�stergesi
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val accuracy = spell.getAverageAccuracy()
                val accuracyPercentage = (accuracy * 100).toInt()

                Text(
                    text = "$accuracyPercentage%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = getAccuracyColor(accuracy)
                )
                Text(
                    text = LanguageManager.getText("accuracy"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Element tipine g�re renk d�nd�r
 */
private fun getElementColor(element: ElementType): Color {
    return when (element) {
        ElementType.FIRE -> Color(0xFFE74C3C)        // K1rm1z1
        ElementType.WATER -> Color(0xFF3498DB)       // Mavi
        ElementType.EARTH -> Color(0xFF8B4513)       // Kahverengi
        ElementType.AIR -> Color(0xFF95A5A6)         // Gri
        ElementType.LIGHTNING -> Color(0xFFF39C12)   // Sar1
        ElementType.ICE -> Color(0xFF5DADE2)         // A�1k Mavi
        ElementType.LIGHT -> Color(0xFFF1C40F)       // Alt1n
        ElementType.DARK -> Color(0xFF34495E)        // Koyu Gri
        ElementType.NEUTRAL -> Color(0xFF7F8C8D)     // N�tr Gri
    }
}

/**
 * Element tipine g�re emoji/ikon d�nd�r
 */
private fun getElementIcon(element: ElementType): String {
    return when (element) {
        ElementType.FIRE -> "=%"
        ElementType.WATER -> "=�"
        ElementType.EARTH -> ">�"
        ElementType.AIR -> "=�"
        ElementType.LIGHTNING -> "�"
        ElementType.ICE -> "D"
        ElementType.LIGHT -> "("
        ElementType.DARK -> "<"
        ElementType.NEUTRAL -> "�"
    }
}

/**
 * Doruluk skoruna g�re renk d�nd�r
 */
private fun getAccuracyColor(accuracy: Float): Color {
    return when {
        accuracy >= 0.9f -> Color(0xFF27AE60)  // Ye_il
        accuracy >= 0.7f -> Color(0xFFF39C12)  // Turuncu
        accuracy >= 0.5f -> Color(0xFFE67E22)  // Koyu Turuncu
        else -> Color(0xFFE74C3C)              // K1rm1z1
    }
}
