package com.example.isekaikuroshin.ui.character

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme


enum class RelationshipLevel(val range: IntRange, val color: Color, val displayName: String) {
    ENEMY((-100..-51), Color(0xFFFF1744), "Düşman"),
    HOSTILE((-50..-21), Color(0xFFFF5722), "Düşmanca"),
    UNFRIENDLY((-20..-6), Color(0xFFFF9800), "Soğuk"),
    NEUTRAL((-5..5), Color(0xFF9E9E9E), "Nötr"),
    FRIENDLY((6..20), Color(0xFF4CAF50), "Arkadaşça"),
    ALLIED((21..50), Color(0xFF2196F3), "Müttefik"),
    BELOVED((51..100), Color(0xFF9C27B0), "Sevgili");

    companion object {
        fun fromValue(value: Int): RelationshipLevel {
            return values().find { value in it.range } ?: NEUTRAL
        }
    }
}

@Composable
fun CharacterCatalogScreen(
    navController: NavController,
    viewModel: CharacterCatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with technological styling
            Text(
                text = rememberLocalizedText("character_catalog").uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )

            // Character list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.characters, key = { it.id }) { character ->
                    CharacterCard(character = character)
                }
            }
        }
    }
}

@Composable
fun CharacterCard(character: CharacterCardState) {
    val relationshipLevel = RelationshipLevel.fromValue(character.relationshipLevel)

    // G86: Apply gray filter for locked characters
    val contentAlpha = if (character.isLocked) 0.3f else 1.0f
    val borderAlpha = if (character.isLocked) 0.1f else 0.3f

    // G86: Localized unlock hint (KURAL 9)
    val lockedHint = rememberLocalizedText("character_locked_hint")

    // Pulsing animation for relationship ring (disabled for locked characters)
    val infiniteTransition = rememberInfiniteTransition(label = "relationship_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (character.isLocked) 0.2f else 0.4f,
        targetValue = if (character.isLocked) 0.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                            Color(0xFF1976D2).copy(alpha = borderAlpha * 1.5f),
                            MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color(0xFF1A237E).copy(alpha = if (character.isLocked) 0.03f else 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Character portrait with relationship ring
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Relationship ring
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(
                                width = 3.dp,
                                color = relationshipLevel.color.copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )

                    // Character portrait
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        relationshipLevel.color.copy(alpha = if (character.isLocked) 0.1f else 0.3f),
                                        Color(0xFF1A237E).copy(alpha = if (character.isLocked) 0.2f else 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // G86: Show lock icon for locked characters, initials for unlocked
                        if (character.isLocked) {
                            Text(
                                text = "🔒",
                                fontSize = 24.sp
                            )
                        } else {
                            Text(
                                text = character.name.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right side: Character details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Name and title (G86: Apply gray filter if locked)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (character.isLocked) "???" else character.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (character.title.isNotEmpty() && !character.isLocked) {
                            Text(
                                text = " - ${character.title}",
                                fontSize = 14.sp,
                                color = Color(0xFF64B5F6).copy(alpha = contentAlpha),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Profession (G86: Hide if locked)
                    if (character.role.isNotEmpty() && !character.isLocked) {
                        Text(
                            text = character.role,
                            fontSize = 13.sp,
                            color = Color(0xFFBBDEFB).copy(alpha = contentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Biography (G86: Show unlock hint if locked, using localized text - KURAL 9)
                    if (character.bio.isNotEmpty() || character.isLocked) {
                        Text(
                            text = if (character.isLocked) lockedHint else character.bio,
                            fontSize = 11.sp,
                            color = Color(0xFFE3F2FD).copy(alpha = if (character.isLocked) 0.4f else 0.8f),
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontStyle = if (character.isLocked) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                        )
                    }

                    // Relationship status
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = relationshipLevel.color.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = relationshipLevel.color.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = relationshipLevel.displayName,
                                fontSize = 9.sp,
                                color = relationshipLevel.color,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (character.location.isNotEmpty()) {
                            Text(
                                text = character.location,
                                fontSize = 9.sp,
                                color = Color(0xFF90CAF9),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F1A)
@Composable
private fun CharacterCatalogScreenPreview() {
    IsekaiKuroshinTheme {
        CharacterCatalogScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F1A)
@Composable
private fun CharacterCardPreview() {
    IsekaiKuroshinTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Friendly character
            CharacterCard(
                character = CharacterCardState(
                    id = "friendly_npc",
                    name = "Mehmet",
                    title = "Usta Arabacı",
                    role = "At Arabası Şoförü",
                    bio = "30 yıldır bu yolları bilir. Eski bir asker, şimdi yolcuları güvenle taşır.",
                    location = "At Arabası",
                    relationshipLevel = 25
                )
            )

            // Neutral character
            CharacterCard(
                character = CharacterCardState(
                    id = "neutral_npc",
                    name = "Esrarengiz Tüccar",
                    title = "Gölgeler Ustası",
                    role = "Nadir Eşya Tüccarı",
                    bio = "Kimliği belirsiz bir tüccar. Her zaman gölgelerde gizlenir.",
                    location = "Pazar Yeri",
                    relationshipLevel = 0
                )
            )

            // Hostile character
            CharacterCard(
                character = CharacterCardState(
                    id = "hostile_npc",
                    name = "Kara Şövalye",
                    title = "Lanetli Savaşçı",
                    role = "Eski Şövalye",
                    bio = "Bir zamanlar onurlu bir şövalye, şimdi karanlık güçlerin etkisinde.",
                    location = "Karanlık Orman",
                    relationshipLevel = -40
                )
            )
        }
    }
}