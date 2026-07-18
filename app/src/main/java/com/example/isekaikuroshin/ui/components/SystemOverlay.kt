
package com.example.isekaikuroshin.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.isekaikuroshin.data.rememberLocalizedText

// --- Data Models ---

sealed interface OverlayData {
    val title: String

    data class Alert(
        override val title: String,
        val message: String
    ) : OverlayData

    data class Quest(
        override val title: String,
        val questName: String,
        val goals: List<String>,
        val isCorruptible: Boolean = false
    ) : OverlayData

    data class ItemAcquired(
        override val title: String,
        val itemName: String,
        val itemDescription: String,
        @DrawableRes val itemIcon: Int,
        val rarity: CardRarity,
        val iconEmoji: String? = null  // G135.3: AI-generated emoji icon
    ) : OverlayData

    data class Choice(
        override val title: String,
        val message: String,
        val options: List<Pair<String, () -> Unit>>
    ) : OverlayData
}

// --- Main Overlay Composable ---

@Composable
fun SystemOverlay(
    overlayData: OverlayData?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible || overlayData == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut(tween(300)) + fadeOut()
            ) {
                val rarity = (overlayData as? OverlayData.ItemAcquired)?.rarity
                SystemOverlayContent(
                    overlayData = overlayData,
                    rarity = rarity,
                    onDismiss = onDismiss,
                    modifier = modifier
                )
            }
        }
    }
}

// --- Content and Frame ---

@Composable
private fun SystemOverlayContent(
    overlayData: OverlayData,
    rarity: CardRarity?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    HolographicFrame(rarity = rarity, modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = getIconForOverlayType(overlayData),
                    contentDescription = null,
                    tint = rarity?.color ?: Color(0xFF00D1FF),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = overlayData.title,
                    color = rarity?.color ?: Color(0xFF00D1FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Kapat", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (overlayData) {
                is OverlayData.Alert -> AlertContent(overlayData)
                is OverlayData.Quest -> QuestContent(overlayData)
                is OverlayData.ItemAcquired -> ItemAcquiredContent(overlayData)
                is OverlayData.Choice -> ChoiceContent(overlayData, onDismiss)
            }
        }
    }
}

@Composable
private fun HolographicFrame(
    rarity: CardRarity?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "frame_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val baseGlowColor = rarity?.color ?: Color(0xFF8A2BE2)

    Box(
        modifier = modifier.fillMaxWidth(0.9f).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.matchParentSize().scale(pulseScale).background(baseGlowColor, shape = RoundedCornerShape(16.dp)).blur(30.dp)
        )
        Surface(
            color = Color(0xEE0A1F3A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(width = 1.dp, color = Color.Cyan.copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp))
        ) {
            content()
        }
    }
}

// --- Content Sections ---

@Composable
private fun AlertContent(alertData: OverlayData.Alert) {
    Text(
        text = alertData.message,
        color = Color.White,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun ChoiceContent(choiceData: OverlayData.Choice, onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            text = choiceData.message,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        choiceData.options.forEach { (text, action) ->
            Button(
                onClick = {
                    action()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color.Cyan)
            ) {
                Text(text, color = Color.White)
            }
        }
    }
}

@Composable
private fun QuestContent(questData: OverlayData.Quest) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        // Quest başlığı ve Umbros ikonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = questData.questName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )

            if (questData.isCorruptible) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            // TODO: Umbros pazarlık ekranını aç
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = "Umbros'un Gözü - Bu görev yozlaştırılabilir",
                        tint = Color(0xFF8B0000),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Görev hedefleri
        questData.goals.forEach { goal ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "• ",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Text(
                    text = goal,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ItemAcquiredContent(itemData: OverlayData.ItemAcquired) {
    // G135.5: Localize rarity display (KURAL 9 - no hardcoded text)
    val rarityText = itemData.rarity.getLocalizedName()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier.size(100.dp).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).border(1.dp, itemData.rarity.color.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // G135.3: Show emoji if available, otherwise fallback to icon
            if (!itemData.iconEmoji.isNullOrBlank()) {
                Text(
                    text = itemData.iconEmoji,
                    fontSize = 48.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Icon(
                    painter = painterResource(id = itemData.itemIcon),
                    contentDescription = itemData.itemName,
                    tint = itemData.rarity.color,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Text(text = itemData.itemName, color = itemData.rarity.color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(text = "($rarityText)", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
        Text(text = itemData.itemDescription, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

// --- Helpers ---

private fun getIconForOverlayType(overlayData: OverlayData): ImageVector {
    return when (overlayData) {
        is OverlayData.Alert -> Icons.Default.Warning
        is OverlayData.Quest -> Icons.Default.Star
        is OverlayData.ItemAcquired -> Icons.Default.CheckCircle
        is OverlayData.Choice -> Icons.Default.Games // Or a more suitable icon
    }
}

// G135.5: Localized rarity extension (KURAL 9 - no hardcoded text)
@Composable
private fun CardRarity.getLocalizedName(): String {
    val key = when (this) {
        CardRarity.COMMON -> "rarity_common"
        CardRarity.UNCOMMON -> "rarity_uncommon"
        CardRarity.RARE -> "rarity_rare"
        CardRarity.EPIC -> "rarity_epic"
        CardRarity.LEGENDARY -> "rarity_legendary"
        CardRarity.MYTHIC -> "rarity_mythic"
        CardRarity.ARTIFACT -> "rarity_artifact"
    }
    return rememberLocalizedText(key)
}
