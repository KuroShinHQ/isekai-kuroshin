package com.example.isekaikuroshin.ui.character

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import kotlin.math.sin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.ui.components.CardData
import com.example.isekaikuroshin.ui.components.CardRarity
import com.example.isekaikuroshin.ui.components.StandardCard
import com.example.isekaikuroshin.ui.components.ElementAffinityCard
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.data.PersistentDataManager

// CharacterTheme object kaldırıldı - artık MaterialTheme.colorScheme kullanılıyor

@Composable
private fun Modifier.animatedTechBackground(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "tech_grid")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_offset"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    // GÖREV #18: İskelet ayarlarını oku
    val gameData by PersistentDataManager.gameData.collectAsState()
    val settings = gameData.settingsData.uiSettings
    val strokeWidth = settings.skeletonGridStrokeWidth
    val gridColor = Color(settings.skeletonGridColor)
    val gridAlpha = settings.skeletonGridAlpha

    return this.drawBehind {
        drawTechGrid(animatedOffset, primaryColor, strokeWidth, gridColor, gridAlpha)
    }
}

private fun DrawScope.drawTechGrid(
    offset: Float,
    primaryColor: Color,
    strokeWidthDp: Float, // GÖREV #18: Ayarlanabilir kalınlık
    gridColor: Color,      // GÖREV #18: Ayarlanabilir renk
    gridAlpha: Float       // GÖREV #18: Ayarlanabilir şeffaflık
) {
    val gridSize = 50.dp.toPx()
    val strokeWidth = strokeWidthDp.dp.toPx() // Settings'ten gelen değer
    val color = gridColor.copy(alpha = gridAlpha) // Settings'ten gelen renk ve alpha

    // Vertical lines
    var x = (-gridSize + (offset % gridSize))
    while (x < size.width + gridSize) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth
        )
        x += gridSize
    }

    // Horizontal lines
    var y = (-gridSize + (offset % gridSize))
    while (y < size.height + gridSize) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
        y += gridSize
    }

    // Circuit-like connections
    repeat(5) { i ->
        val connectionX = (size.width * (i + 1) / 6) + sin(offset * 0.01f + i) * 20
        val connectionY = (size.height * 0.3f) + sin(offset * 0.015f + i * 2) * 30

        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = 2.dp.toPx(),
            center = Offset(connectionX, connectionY)
        )
    }
}

@Composable
fun CharacterStatusScreen(
    modifier: Modifier = Modifier,
    viewModel: CharacterStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // SORUN 3: Element Affinity removed - moved to SkillTree
    val gameData by PersistentDataManager.gameData.collectAsState()
    // val showElementAffinity = gameData.settingsData.uiSettings.showElementAffinity

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .animatedTechBackground(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Karakter başlığı
        item {
            CharacterHeader(name = uiState.characterName, level = uiState.level)
        }

        // Dağıtılmamış Stat Puanları (varsa)
        if (uiState.unspentStatPoints > 0 || uiState.pendingPoints > 0) {
            item {
                StatPointsHeader(
                    unspentPoints = uiState.unspentStatPoints,
                    pendingPoints = uiState.pendingPoints,
                    onConfirm = { viewModel.confirmAllocations() },
                    onCancel = { viewModel.cancelAllocations() }
                )
            }
        }

        // Yaşam İstatistikleri Kartı
        item {
            LifeStatsCard(stats = uiState.lifeStats)
        }

        // Ana İstatistikler Kartı
        item {
            MainStatsCard(
                stats = uiState.mainStats,
                hasUnspentPoints = uiState.unspentStatPoints > 0,
                onAllocate = { statName -> viewModel.modifyPendingAllocation(statName, 1) }
            )
        }

        // G118: Active Status Effects (Emoji-Based UI)
        item {
            ActiveStatusEffectsSection()
        }

        // TODO-G106.5: Archetype Profile Display
        item {
            Text(
                text = rememberLocalizedText("character_archetype"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            ArchetypeCard(
                dominantArchetype = uiState.dominantArchetype,
                archetypeScores = uiState.archetypeScores
            )
        }

        // G84 FIX: Titles başlığı HER ZAMAN görünür (like Skills section)
        if (uiState.allTitles.isNotEmpty()) {
            item {
                Text(
                    text = rememberLocalizedText("titles"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(uiState.allTitles.size) { index ->
                AnimatedClickableCard(data = uiState.allTitles[index])
            }
        }

        // G84 FIX: Badges başlığı HER ZAMAN görünür (fallback sayesinde liste asla boş değil)
        item {
            Text(
                text = rememberLocalizedText("status_and_badges"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(uiState.allBadges.size) { index ->
            AnimatedClickableCard(data = uiState.allBadges[index])
        }


        // Yetenekler
        item {
            Text(
                text = rememberLocalizedText("skills"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        uiState.skillCard?.let {
            item {
                AnimatedClickableCard(data = it.copy(imageRes = R.drawable.icon_skill_adaptation))
            }
        }

        // SORUN 3: Element Affinity removed from CharacterStatus - should be in SkillTree instead
        // if (showElementAffinity) {
        //     item {
        //         Spacer(modifier = Modifier.height(8.dp))
        //     }
        //
        //     item {
        //         ElementAffinityCard(
        //             affinities = gameData.playerData.elementAffinities.affinities
        //         )
        //     }
        // }
    }
}

@Composable
private fun CharacterHeader(name: String, level: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            surfaceColor.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.6f),
                            primaryColor.copy(alpha = 0.3f),
                            primaryColor.copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Text(
                    text = level,
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun LifeStatsCard(stats: List<Pair<String, String>>) {
    // 🔥 FIX: LanguageManager kullan (stringResource yerine)
    HolographicCard(
        title = rememberLocalizedText("vital_stats")
    ) {
        StatsGrid(
            stats = stats
        )
    }
}

@Composable
private fun MainStatsCard(
    stats: List<Pair<String, String>>,
    hasUnspentPoints: Boolean = false,
    onAllocate: (String) -> Unit = {}
) {
    // 🔥 FIX: LanguageManager kullan (stringResource yerine)
    HolographicCard(
        title = rememberLocalizedText("main_stats")
    ) {
        StatsGridWithAllocation(
            stats = stats,
            hasUnspentPoints = hasUnspentPoints,
            onAllocate = onAllocate
        )
    }
}

@Composable
private fun HolographicCard(
    title: String,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "holographic_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0.9f),
                            surfaceColor.copy(alpha = 0.95f),
                            surfaceColor.copy(alpha = 0.9f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = pulseAlpha * 0.8f),
                            primaryColor.copy(alpha = pulseAlpha * 0.3f),
                            primaryColor.copy(alpha = pulseAlpha * 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {
            // Kart başlığı
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .shadow(
                        elevation = 4.dp,
                        spotColor = primaryColor.copy(alpha = 0.3f),
                        ambientColor = primaryColor.copy(alpha = 0.1f)
                    )
            )

            // Kart içeriği
            content()
        }
    }
}

@Composable
private fun AnimatedClickableCard(data: CardData) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    StandardCard(
        data = data,
        modifier = Modifier
            .scale(scale)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Handle click action - can be customized based on card type
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    )
}

@Composable
private fun StatsGrid(
    stats: List<Pair<String, String>>
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.height(((stats.size / 2 + stats.size % 2) * 88).dp) // Sabit yükseklik (72dp + spacing)
    ) {
        items(stats) { (name, value) ->
            if (name.contains("/")) {
                LifeStatBar(
                    name = name,
                    value = value
                )
            } else {
                StatItem(
                    name = name,
                    value = value
                )
            }
        }
    }
}

@Composable
private fun LifeStatBar(
    name: String,
    value: String,
    modifier: Modifier = Modifier
) {
    // "100/100" formatından current ve max değerleri çıkar
    val (current, max) = try {
        val parts = value.split("/")
        Pair(parts[0].toFloat(), parts[1].toFloat())
    } catch (e: Exception) {
        Pair(0f, 100f)
    }

    val progress = if (max > 0) current / max else 0f
    val iconRes = getStatIcon(name)

    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val errorColor = MaterialTheme.colorScheme.error
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = modifier
            .height(64.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = primaryColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Başlık ve ikon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                iconRes?.let {
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = name,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = when (name.uppercase()) {
                                    "HP", "CAN" -> listOf(errorColor, errorColor.copy(alpha = 0.7f))
                                    "MP", "MANA" -> listOf(primaryContainerColor, primaryContainerColor.copy(alpha = 0.7f))
                                    else -> listOf(secondaryContainerColor, secondaryContainerColor.copy(alpha = 0.7f))
                                }
                            ),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }

            // Değer metni
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatItem(
    name: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val iconRes = getStatIcon(name)
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .height(72.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = primaryColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                drawHexagonPattern(alpha = 0.1f, color = primaryColor)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                iconRes?.let {
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = name,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .shadow(
                        elevation = 6.dp,
                        spotColor = primaryColor.copy(alpha = 0.5f),
                        ambientColor = primaryColor.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

private fun DrawScope.drawHexagonPattern(alpha: Float, color: Color) {
    val hexSize = 8.dp.toPx()
    val spacing = 12.dp.toPx()
    val patternColor = color.copy(alpha = alpha)
    val strokeWidth = 1.dp.toPx()

    val cols = (size.width / spacing).toInt() + 1
    val rows = (size.height / spacing).toInt() + 1

    for (row in 0..rows) {
        for (col in 0..cols) {
            val offsetX = if (row % 2 == 0) 0f else spacing / 2
            val x = col * spacing + offsetX
            val y = row * spacing * 0.866f // Hexagon height factor

            if (x > -hexSize && x < size.width + hexSize && y > -hexSize && y < size.height + hexSize) {
                drawHexagon(
                    center = Offset(x, y),
                    radius = hexSize / 2,
                    color = patternColor,
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

private fun DrawScope.drawHexagon(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    val angles = (0..5).map { it * 60f * kotlin.math.PI / 180f }

    angles.forEachIndexed { index, angle ->
        val x = center.x + radius * kotlin.math.cos(angle).toFloat()
        val y = center.y + radius * kotlin.math.sin(angle).toFloat()

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )
}

private fun getStatIcon(statName: String): Int? {
    return when (statName.uppercase()) {
        "HP", "CAN", "YAŞAM" -> R.drawable.icon_vit_seviye1
        "MP", "MANA" -> R.drawable.icon_int_seviye1
        "STAMINA", "DAY", "DAYANIŞ", "DAYANIKLILIK" -> R.drawable.icon_vit_seviye1
        // Main Stats - yeni özel ikonlar
        "STR", "GÜÇ", "KUVVET", "STRENGTH" -> R.drawable.icon_mainstat_str
        "AGI", "ÇEV", "ÇEVIKLIK", "ÇEVİKLİK", "AGILITY" -> R.drawable.icon_mainstat_agi
        "INT", "ZEK", "ZEKA", "INTELLIGENCE" -> R.drawable.icon_mainstat_int
        "VIT", "CANLILIK", "VITALITY" -> R.drawable.icon_mainstat_vit
        "SPIRIT", "RUH" -> R.drawable.icon_spirit_seviye1
        "LUCK", "ŞANS" -> R.drawable.icon_luck_seviye1
        "HUNGER", "AÇLIK" -> R.drawable.icon_hunger_seviye1
        "FATIGUE", "YORGUNLUK" -> R.drawable.icon_fatigue_seviye1
        else -> null
    }
}

private fun getStatKey(statName: String): String {
    return when (statName.uppercase()) {
        "GÜÇ", "KUVVET" -> "str"
        "ÇEVİKLİK" -> "agi"
        "ZEKA" -> "int"
        "CANLILIK" -> "vit"
        "RUH" -> "spirit"
        "ŞANS" -> "luck"
        else -> statName.lowercase()
    }
}

// ============= STAT ALLOCATION UI COMPONENTS =============

@Composable
private fun StatPointsHeader(
    unspentPoints: Int,
    pendingPoints: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val textColor = MaterialTheme.colorScheme.onBackground

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.2f),
                            secondaryContainerColor.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = primaryColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val unspentText = rememberLocalizedText("unspent_points")
            Text(
                text = unspentText.replace("%d", (unspentPoints - pendingPoints).toString()),
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )

            if (pendingPoints > 0) {
                val pendingText = rememberLocalizedText("pending_points")
                Text(
                    text = pendingText.replace("%d", pendingPoints.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryContainerColor,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondaryContainerColor
                        )
                    ) {
                        Text(rememberLocalizedText("confirm_button"), color = Color.Black)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryColor
                        )
                    ) {
                        Text(rememberLocalizedText("cancel_button"))
                    }
                }
            } else {
                Text(
                    text = rememberLocalizedText("stat_allocation_hint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatsGridWithAllocation(
    stats: List<Pair<String, String>>,
    hasUnspentPoints: Boolean,
    onAllocate: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.height(((stats.size / 2 + stats.size % 2) * 88).dp)
    ) {
        items(stats) { (name, value) ->
            StatItemWithAllocation(
                name = name,
                value = value,
                hasAllocateButton = hasUnspentPoints,
                onAllocate = { onAllocate(getStatKey(name)) }
            )
        }
    }
}

@Composable
private fun StatItemWithAllocation(
    name: String,
    value: String,
    hasAllocateButton: Boolean,
    onAllocate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRes = getStatIcon(name)
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = modifier
            .height(72.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = primaryColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                drawHexagonPattern(alpha = 0.1f, color = primaryColor)
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    iconRes?.let {
                        Image(
                            painter = painterResource(id = it),
                            contentDescription = name,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (hasAllocateButton) {
                IconButton(
                    onClick = onAllocate,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "↑",
                        color = secondaryContainerColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * G113.5: Status Effect Card - Shows active buffs/debuffs
 */
@Composable
private fun StatusEffectCard(effectData: StatusEffectDisplayData) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val isBuffColor = if (effectData.type == "BUFF")
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Effect name with type indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (effectData.type == "BUFF") "▲" else "▼",
                        color = isBuffColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = effectData.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Effect details
                Text(
                    text = "${rememberLocalizedText("magnitude")}: ${effectData.magnitude} | ${rememberLocalizedText("turns_remaining")}: ${effectData.remainingTurns}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Type badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = isBuffColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = effectData.type,
                    color = isBuffColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F1A)
@Composable
private fun CharacterStatusScreenPreview() {
    IsekaiKuroshinTheme {
        CharacterStatusScreen()
    }
}

@Preview(name = "Character Header", showBackground = true)
@Composable
private fun CharacterHeaderPreview() {
    IsekaiKuroshinTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            CharacterHeader(name = "Örnek Karakter", level = "Level 42")
        }
    }
}

@Preview(name = "Stats Cards", showBackground = true)
@Composable
private fun StatsCardsPreview() {
    IsekaiKuroshinTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LifeStatsCard(stats = listOf("HP" to "100/100", "MP" to "50/50", "Stamina" to "80/100"))
            MainStatsCard(stats = listOf("STR" to "25", "AGI" to "20", "INT" to "30", "VIT" to "22"))
        }
    }
}

/**
 * TODO-G106.5: Archetype Profile Card
 * Shows player's dominant archetype and progression scores
 */
@Composable
private fun ArchetypeCard(
    dominantArchetype: String,
    archetypeScores: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dominant Archetype Header
            Text(
                text = "${rememberLocalizedText("dominant_archetype")}: ${rememberLocalizedText("archetype_$dominantArchetype")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Archetype Scores
            archetypeScores.forEach { (archetype, score) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLocalizedText("archetype_$archetype"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (archetype == dominantArchetype)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { (score.toFloat() / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.width(100.dp).height(8.dp),
                            color = if (archetype == dominantArchetype)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary,
                        )

                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * G118: Active Status Effects Section (Emoji-Based UI)
 * Displays active buffs/debuffs as emoji row with tooltips
 */
@Composable
private fun ActiveStatusEffectsSection() {
    // TODO-G118: Veriyi GMResponse'tan çekecek şekilde güncelle
    // NOTE: activeStatusEffects şu anda GMResponse'tan geliyor ama persistent olmayabilir
    // Phase 2'de PersistentDataManager'a eklenebilir veya GameState'e taşınabilir
    val activeEffects = emptyList<com.example.isekaikuroshin.data.ActiveStatusEffectG118>() // Placeholder

    if (activeEffects.isEmpty()) {
        // Boş state - hiç efekt yok
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.alpha(0.5f)
                )

                Text(
                    text = rememberLocalizedText("no_active_effects"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    // Active effects var - emoji row göster
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section başlığı
            Text(
                text = rememberLocalizedText("active_effects"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Emoji row (horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                activeEffects.take(5).forEach { effect: com.example.isekaikuroshin.data.ActiveStatusEffectG118 ->
                    StatusEffectEmoji(effect)
                }
            }

            // Helper text
            Text(
                text = "(Long-press for details)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

/**
 * G118: Single Status Effect Emoji with tooltip
 */
@Composable
private fun StatusEffectEmoji(effect: com.example.isekaikuroshin.data.ActiveStatusEffectG118) {
    // Template lookup
    val template = com.example.isekaikuroshin.data.StatusEffectTemplateRegistry.getTemplate(effect.effectId)

    // Display emoji (custom override varsa onu, yoksa template default)
    val displayEmoji = effect.getDisplayEmoji(template)
    val displayName = effect.getDisplayName(template)

    // Category-based border color
    val borderColor = when (template?.category) {
        com.example.isekaikuroshin.data.StatusEffectCategory.BUFF -> Color(0xFF4CAF50) // Green
        com.example.isekaikuroshin.data.StatusEffectCategory.DEBUFF -> Color(0xFFF44336) // Red
        com.example.isekaikuroshin.data.StatusEffectCategory.ENVIRONMENTAL -> Color(0xFF2196F3) // Blue
        com.example.isekaikuroshin.data.StatusEffectCategory.SPECIAL -> Color(0xFF9C27B0) // Purple
        null -> MaterialTheme.colorScheme.outline // Unknown/custom
    }

    // Tooltip state
    var showTooltip by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(60.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = borderColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showTooltip = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Emoji
        Text(
            text = displayEmoji,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 32.sp
        )

        // Intensity indicator (small dot)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(8.dp)
                .background(
                    color = when {
                        effect.intensity >= 0.7f -> Color.Red // Strong
                        effect.intensity >= 0.4f -> Color.Yellow // Moderate
                        else -> Color.Green // Weak
                    },
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }

    // Tooltip Dialog (long-press)
    if (showTooltip) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTooltip = false },
            confirmButton = {
                TextButton(onClick = { showTooltip = false }) {
                    Text(rememberLocalizedText("ok"))
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(displayEmoji, fontSize = 32.sp)
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Description
                    Text(
                        text = effect.description,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${rememberLocalizedText("effect_duration")}:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (effect.duration == null)
                                rememberLocalizedText("effect_permanent")
                            else
                                "${effect.duration} ${rememberLocalizedText("effect_turns_remaining")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Intensity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Intensity:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                effect.intensity >= 0.7f -> "Strong (${(effect.intensity * 100).toInt()}%)"
                                effect.intensity >= 0.4f -> "Moderate (${(effect.intensity * 100).toInt()}%)"
                                else -> "Weak (${(effect.intensity * 100).toInt()}%)"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Category
                    template?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Category:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = it.category.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = borderColor
                            )
                        }
                    }
                }
            }
        )
    }
}