package com.example.isekaikuroshin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.Skill
import com.example.isekaikuroshin.utils.EmojiHelper

/**
 * G126: Skill Card Component
 *
 * Skill'leri görselleştiren kompakt kart bileşeni.
 * CharacterStatus ve SkillTree ekranlarında kullanılır.
 */
@Composable
fun SkillCard(
    skill: Skill,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // Rarity'ye göre renk
    val rarityColor = when (skill.rarity.name) {
        "F", "G" -> Color(0xFF9E9E9E) // Gray
        "E", "D" -> Color(0xFF4CAF50) // Green
        "C", "B" -> Color(0xFF2196F3) // Blue
        "A" -> Color(0xFF9C27B0) // Purple
        "S" -> Color(0xFFFFB300) // Gold
        "SS", "SSS", "SSS+" -> Color(0xFFFF5722) // Red-Orange
        else -> Color(0xFF9E9E9E)
    }

    // Element'e göre accent color
    val elementColor = when (skill.elementType.name.lowercase()) {
        "fire" -> Color(0xFFFF5722)
        "water" -> Color(0xFF2196F3)
        "ice" -> Color(0xFF00BCD4)
        "lightning", "electric" -> Color(0xFFFFEB3B)
        "earth" -> Color(0xFF795548)
        "wind" -> Color(0xFF9E9E9E)
        "light", "holy" -> Color(0xFFFFC107)
        "dark", "shadow" -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B) // Neutral
    }

    Card(
        modifier = modifier
            .width(160.dp)
            .height(100.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick.invoke() }
                } else Modifier
            )
            .border(
                width = 1.dp,
                color = rarityColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        elementColor.copy(alpha = 0.1f)
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
            // Header: Emoji + Name + Rarity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // G126: Emoji icon
                    Text(
                        text = EmojiHelper.getSkillEmoji(skill),
                        fontSize = 20.sp
                    )

                    // Skill name
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = rarityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Rarity badge
                Box(
                    modifier = Modifier
                        .background(
                            color = rarityColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = skill.rarity.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = rarityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Element & Tier
            Text(
                text = "${skill.elementType.name} • ${skill.tier}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Footer: Level + Mana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Level
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lv.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 8.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${skill.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Mana cost
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MP:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 8.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${skill.manaCost}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Detaylı Skill Card (Vertical Layout)
 * Skill detay modal'ında kullanılır
 */
@Composable
fun SkillCardDetailed(
    skill: Skill,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val rarityColor = when (skill.rarity.name) {
        "F", "G" -> Color(0xFF9E9E9E)
        "E", "D" -> Color(0xFF4CAF50)
        "C", "B" -> Color(0xFF2196F3)
        "A" -> Color(0xFF9C27B0)
        "S" -> Color(0xFFFFB300)
        "SS", "SSS", "SSS+" -> Color(0xFFFF5722)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = rarityColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Emoji + Name + Rarity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // G126: Emoji icon (larger)
                    Text(
                        text = EmojiHelper.getSkillEmoji(skill),
                        fontSize = 32.sp
                    )

                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = rarityColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = rarityColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = skill.rarity.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = rarityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Description
            Text(
                text = skill.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 16.sp
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

            // Stats
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkillStatRow("Element", skill.elementType.name, Color(0xFF2196F3))
                SkillStatRow("Tier", skill.tier, Color(0xFF9C27B0))
                SkillStatRow("Level", "${skill.level} / ${skill.maxLevel}", Color.Green)
                SkillStatRow("Mana Cost", "${skill.manaCost} MP", Color(0xFF2196F3))

                if (skill.baseCooldown > 0) {
                    SkillStatRow("Cooldown", "${skill.baseCooldown}s", Color.Red)
                }

                if (skill.diceModifier != 0) {
                    SkillStatRow(
                        "Dice Modifier",
                        "${if (skill.diceModifier > 0) "+" else ""}${skill.diceModifier}",
                        if (skill.diceModifier > 0) Color.Green else Color.Red
                    )
                }
            }

            // Stat Bonuses
            if (skill.statBonuses.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

                Text(
                    text = "Stat Bonuses:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )

                skill.statBonuses.forEach { (stat, value) ->
                    SkillStatRow(
                        stat.name,
                        "${if (value > 0) "+" else ""}${value}",
                        if (value > 0) Color.Green else Color.Red
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillStatRow(
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
