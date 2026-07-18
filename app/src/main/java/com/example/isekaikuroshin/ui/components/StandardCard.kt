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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import kotlin.math.absoluteValue
import com.example.isekaikuroshin.data.rememberLocalizedText

// JOURNAL_SYSTEM_DIAGRAM.md'den gelen, stat bonuslarını tanımlayan enum
enum class StatType {
    STR_PERCENT, STR_FLAT,
    AGI_PERCENT, AGI_FLAT,
    INT_PERCENT, INT_FLAT,
    VIT_PERCENT, VIT_FLAT,
    SPIRIT_PERCENT, SPIRIT_FLAT,
    LUCK_PERCENT, LUCK_FLAT
}

// FRONT_END_REWORK_PLAN.md'de tanımlanan, kart nadirliğini belirleyen enum
// ItemRarity ile tam uyumlu 7 seviye
// G135.5: displayName kaldırıldı - localization için @Composable extension kullan (KURAL 9)
enum class CardRarity(val color: Color) {
    COMMON(Color(0xFF9E9E9E)),
    UNCOMMON(Color(0xFF4CAF50)),
    RARE(Color(0xFF2196F3)),
    EPIC(Color(0xFF9C27B0)),
    LEGENDARY(Color(0xFFFF9800)),
    MYTHIC(Color(0xFFE91E63)),
    ARTIFACT(Color(0xFFFFD700))
}

// Farklı kart türlerini yönetmek için sealed interface
sealed interface CardData {
    val id: String
    val name: String
    val description: String
    val imageRes: Int? // FB#6: Nullable - yetenekler/rozetler icon olmadan da olabilir
    val rarity: CardRarity

    data class SkillCard(
        override val id: String,
        override val name: String,
        override val description: String,
        override val imageRes: Int?,
        override val rarity: CardRarity,
        val rank: String,
        val statBonuses: Map<StatType, Float>
    ) : CardData

    data class ItemCard(
        override val id: String,
        override val name: String,
        override val description: String,
        override val imageRes: Int?,
        override val rarity: CardRarity,
        val itemType: com.example.isekaikuroshin.data.ItemType = com.example.isekaikuroshin.data.ItemType.MISC,
        val durability: Int,
        val gearScore: Int,
        val statBonuses: Map<StatType, Float>,
        val quantity: Int = 1,              // Stackleme için miktar
        val isStackable: Boolean = false,    // Bu item stacklenebilir mi?
        val equipmentSlot: String = "NONE"  // G80: Equipment slot (MAIN_HAND, HEAD, CHEST, etc.)
    ) : CardData

    data class BadgeCard(
        override val id: String,
        override val name: String,
        override val description: String,
        override val imageRes: Int?,
        override val rarity: CardRarity,
        val unlockCondition: String
    ) : CardData

    data class QuestCard(
        override val id: String,
        override val name: String, // Görev Adı
        override val description: String, // Görev Özeti
        override val imageRes: Int?, // Görevle ilgili bir ikon
        override val rarity: CardRarity, // Görev türünü belirtmek için
        val suggestedLevel: Int, // Tavsiye edilen seviye
        val rewards: List<String> // Ödüllerin listesi (örn: "500 XP", "Nadir Kılıç")
    ) : CardData
}

// Ana tema renkleri - Composable context'te kullanılmalı
@Composable
private fun getCardTheme() = object {
    val background = MaterialTheme.colorScheme.background // Çok Koyu Mavi
    val accent = MaterialTheme.colorScheme.primary // Cyan
    val text = MaterialTheme.colorScheme.onBackground // Kirli Beyaz
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StandardCard(
    data: CardData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    // Basma durumu ve animasyonları
    var isPressed by remember { mutableStateOf(false) }

    // Press animasyonu
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale_animation"
    )

    // Sürekli parlama efekti animasyonu (daha yumuşak)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // G135.5: Localize rarity for semantics (must be outside semantics block)
    val rarityText = data.rarity.getLocalizedName()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .scale(animatedScale)
            .semantics {
                contentDescription = "${data.name}, $rarityText, ${data.description}"
                if (onClick != null) {
                    role = Role.Button
                }
            }
    ) {
        // Ana kart yapısı - üstten taşan resim için padding
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
                .combinedClickable(
                    enabled = onClick != null || onLongClick != null,
                    onClickLabel = "Kartı seç",
                    onLongClickLabel = "Sil",
                    onClick = { onClick?.invoke() },
                    onLongClick = { onLongClick?.invoke() }
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            getCardTheme().accent.copy(alpha = pulseAlpha * 0.4f),
                            data.rarity.color.copy(alpha = pulseAlpha * 0.3f),
                            getCardTheme().accent.copy(alpha = pulseAlpha * 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            getCardTheme().background.copy(alpha = 0.9f),
                            getCardTheme().background.copy(alpha = 0.95f)
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
                    .fillMaxSize()
                    .padding(20.dp)
                    .padding(top = 16.dp), // Üstten taşan resim için ekstra padding
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Kart içeriği
                CardContent(data = data)

                // Alt kısım - stat bonusları veya özel bilgiler
                CardBottomSection(data = data)
            }
        }

        // Üstten taşan resim/ikon
        OverlayImage(
            imageRes = data.imageRes,
            rarity = data.rarity,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Rarity etiketi kaldırıldı - sadece border rengi kalıyor
    }
}

@Composable
private fun OverlayImage(
    imageRes: Int?, // FB#6: Nullable - icon olmadan da gösterilebilir
    rarity: CardRarity,
    modifier: Modifier = Modifier
) {
    // TODO-FIX-ICON-05: Icon yoksa placeholder göster
    val actualImageRes = imageRes ?: android.R.drawable.ic_menu_help // Soru işareti placeholder

    Image(
        painter = painterResource(id = actualImageRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(48.dp)
            .offset(y = (-12).dp)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = rarity.color.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        rarity.color.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}

@Composable
private fun RarityBadge(
    rarity: CardRarity,
    modifier: Modifier = Modifier,
    text: String
) {
    Box(
        modifier = modifier
            .offset(x = (-6).dp, y = 6.dp)
            .background(
                color = rarity.color.copy(alpha = 0.8f),
                shape = RoundedCornerShape(bottomStart = 6.dp, topEnd = 8.dp)
            )
            .border(
                width = 1.dp,
                color = rarity.color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(bottomStart = 6.dp, topEnd = 8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun CardContent(data: CardData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kart başlığı - parlayan efektli
        Text(
            text = data.name,
            style = MaterialTheme.typography.titleMedium,
            color = getCardTheme().accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer {
                shadowElevation = 4.dp.toPx()
            }
        )

        // Tip-spesifik bilgi (rank, gear score, vb.)
        when (data) {
            is CardData.SkillCard -> {
                Text(
                    text = "Seviye: ${data.rank}",
                    style = MaterialTheme.typography.bodySmall,
                    color = data.rarity.color,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
            is CardData.ItemCard -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Item Type Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = getItemTypeColor(data.itemType),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = getItemTypeLabel(data.itemType),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "GS: ${data.gearScore}",
                            style = MaterialTheme.typography.bodySmall,
                            color = getCardTheme().accent,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Dayan.: ${data.durability}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (data.durability > 70) Color.Green else if (data.durability > 30) Color.Yellow else Color.Red,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                        // Quantity gösterimi (stacklable ise)
                        if (data.isStackable && data.quantity > 1) {
                            Text(
                                text = "x${data.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            is CardData.BadgeCard -> {
                Text(
                    text = data.unlockCondition,
                    style = MaterialTheme.typography.bodySmall,
                    color = getCardTheme().accent.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is CardData.QuestCard -> {
                Text(
                    text = rememberLocalizedText("rewards").replace("{rewards}", data.rewards.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = getCardTheme().accent.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Açıklama metni - kısaltmalı
        Text(
            text = data.description,
            style = MaterialTheme.typography.bodySmall,
            color = getCardTheme().text.copy(alpha = 0.8f),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun CardBottomSection(data: CardData) {
    when (data) {
        is CardData.SkillCard, is CardData.ItemCard -> {
            val statBonuses = when (data) {
                is CardData.SkillCard -> data.statBonuses
                is CardData.ItemCard -> data.statBonuses
                else -> emptyMap()
            }

            if (statBonuses.isNotEmpty()) {
                StatBonusesSection(statBonuses = statBonuses)
            }
        }
        is CardData.BadgeCard, is CardData.QuestCard -> {
            // Badge ve Quest kartları için özel bir alt kısım (şimdilik boş)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatBonusesSection(statBonuses: Map<StatType, Float>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Bonuslar:",
            style = MaterialTheme.typography.labelSmall,
            color = getCardTheme().text.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        // FlowRow benzeri düzen için chunked kullanıyoruz
        val chunkedBonuses = statBonuses.toList().chunked(2)
        chunkedBonuses.forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chunk.forEach { (statType, value) ->
                    StatChip(
                        statType = statType,
                        value = value,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    statType: StatType,
    value: Float,
    modifier: Modifier = Modifier
) {
    val (displayText, chipColor) = getStatDisplayInfo(statType, value)

    Box(
        modifier = modifier
            .background(
                color = chipColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 0.5.dp,
                color = chipColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun getStatDisplayInfo(statType: StatType, value: Float): Pair<String, Color> {
    val statName = when (statType) {
        StatType.STR_PERCENT, StatType.STR_FLAT -> "GÜÇ"
        StatType.AGI_PERCENT, StatType.AGI_FLAT -> "ÇEV"
        StatType.INT_PERCENT, StatType.INT_FLAT -> "ZEK"
        StatType.VIT_PERCENT, StatType.VIT_FLAT -> "CAN"
        StatType.SPIRIT_PERCENT, StatType.SPIRIT_FLAT -> "RUH"
        StatType.LUCK_PERCENT, StatType.LUCK_FLAT -> "ŞAN"
    }

    val isPercentage = statType.name.contains("PERCENT")
    val displayValue = if (isPercentage) {
        "${if (value >= 0) "+" else ""}${(value * 100).toInt()}%"
    } else {
        "${if (value >= 0) "+" else ""}${value.toInt()}"
    }

    val color = when {
        value > 0 -> Color(0xFF4CAF50) // Yeşil - pozitif bonus
        value < 0 -> Color(0xFFFF5722) // Kırmızı - negatif bonus
        else -> Color.Gray // Gri - nötr
    }

    return Pair("$statName $displayValue", color)
}

// Helper functions for item type
private fun getItemTypeColor(type: com.example.isekaikuroshin.data.ItemType): Color {
    return when (type) {
        com.example.isekaikuroshin.data.ItemType.WEAPON -> Color(0xFFE53935)
        com.example.isekaikuroshin.data.ItemType.ARMOR -> Color(0xFF1E88E5)
        com.example.isekaikuroshin.data.ItemType.EQUIPMENT -> Color(0xFF8E24AA)
        com.example.isekaikuroshin.data.ItemType.CONSUMABLE -> Color(0xFF43A047)
        com.example.isekaikuroshin.data.ItemType.MATERIAL -> Color(0xFFFB8C00)
        com.example.isekaikuroshin.data.ItemType.QUEST -> Color(0xFFFFD600)
        com.example.isekaikuroshin.data.ItemType.CURRENCY -> Color(0xFFFDD835)
        com.example.isekaikuroshin.data.ItemType.TRINKET -> Color(0xFFAB47BC)
        com.example.isekaikuroshin.data.ItemType.MISC -> Color(0xFF757575)
    }
}

// G135.5: Localized item type labels (KURAL 9 - no hardcoded text)
@Composable
private fun getItemTypeLabel(type: com.example.isekaikuroshin.data.ItemType): String {
    val key = when (type) {
        com.example.isekaikuroshin.data.ItemType.WEAPON -> "item_type_weapon"
        com.example.isekaikuroshin.data.ItemType.ARMOR -> "item_type_armor"
        com.example.isekaikuroshin.data.ItemType.EQUIPMENT -> "item_type_equipment"
        com.example.isekaikuroshin.data.ItemType.CONSUMABLE -> "item_type_consumable"
        com.example.isekaikuroshin.data.ItemType.MATERIAL -> "item_type_material"
        com.example.isekaikuroshin.data.ItemType.QUEST -> "item_type_quest"
        com.example.isekaikuroshin.data.ItemType.CURRENCY -> "item_type_currency"
        com.example.isekaikuroshin.data.ItemType.TRINKET -> "item_type_trinket"
        com.example.isekaikuroshin.data.ItemType.MISC -> "item_type_misc"
    }
    return com.example.isekaikuroshin.data.rememberLocalizedText(key)
}

// G135.5: Localized rarity labels (KURAL 9 - no hardcoded text)
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
    return com.example.isekaikuroshin.data.rememberLocalizedText(key)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F1A)
@Composable
private fun StandardCardPreview() {
    IsekaiKuroshinTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(getCardTheme().background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Skill Card Preview
            StandardCard(
                data = CardData.SkillCard(
                    id = "lightning_strike",
                    name = "Şimşek Çarpması",
                    description = "Elektrikle yüklü hızlı saldırı. Düşmanları felç eder ve yüksek hasar verir.",
                    imageRes = android.R.drawable.star_on, // Placeholder
                    rarity = CardRarity.RARE,
                    rank = "Altın",
                    statBonuses = mapOf(
                        StatType.AGI_PERCENT to 0.10f,
                        StatType.INT_FLAT to 5f,
                        StatType.LUCK_PERCENT to 0.05f
                    )
                )
            )

            // Item Card Preview
            StandardCard(
                data = CardData.ItemCard(
                    id = "legendary_sword",
                    name = "Ejderha Kılıcı",
                    description = "Antik ejderha ateşiyle dövülmüş efsanevi kılıç. Ateş elementine dirençli düşmanları dahi yakabilir.",
                    imageRes = android.R.drawable.star_big_on, // Placeholder
                    rarity = CardRarity.LEGENDARY,
                    durability = 85,
                    gearScore = 250,
                    statBonuses = mapOf(
                        StatType.STR_PERCENT to 0.15f,
                        StatType.VIT_FLAT to 10f
                    )
                )
            )

            // Badge Card Preview
            StandardCard(
                data = CardData.BadgeCard(
                    id = "dragon_slayer",
                    name = "Ejderha Avcısı",
                    description = "Efsanevi ejderhaları alt eden nadir savaşçılara verilen prestijli unvan.",
                    imageRes = android.R.drawable.star_big_off, // Placeholder
                    rarity = CardRarity.LEGENDARY,
                    unlockCondition = "5 ejderha öldür"
                )
            )
        }
    }
}

@Preview(name = "Single Card - Dark Theme", showBackground = true, backgroundColor = 0xFF0A0F1A)
@Composable
private fun SingleCardPreview() {
    IsekaiKuroshinTheme {
        Box(
            modifier = Modifier
                .size(300.dp, 220.dp)
                .background(getCardTheme().background)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            StandardCard(
                data = CardData.SkillCard(
                    id = "fire_mastery",
                    name = "Ateş Ustalığı",
                    description = "Ateş büyüsü üzerinde gelişmiş kontrol sağlar.",
                    imageRes = android.R.drawable.star_on,
                    rarity = CardRarity.UNCOMMON,
                    rank = "Gümüş",
                    statBonuses = mapOf(
                        StatType.INT_PERCENT to 0.12f,
                        StatType.SPIRIT_FLAT to 8f
                    )
                ),
                onClick = { /* Test click */ }
            )
        }
    }
}