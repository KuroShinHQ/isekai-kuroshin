package com.example.isekaikuroshin.ui.exercise

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.rememberLocalizedText
import kotlinx.coroutines.delay

/**
 * Egzersiz Sonuç Ekranı
 *
 * Egzersiz bittiğinde gösterilir ve kullanıcının kazandığı
 * XP, stat artışları ve diğer ödülleri gösterir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseResultScreen(
    navController: NavController,
    exerciseType: String = "PUSH_UP",
    repCount: Int = 0,
    totalXP: Int = 0,
    statGains: Map<String, Float> = emptyMap(),
    calories: Float = 0f,
    gold: Int = 0,
    xpMultiplier: Float = 1.0f  // YENİ: Variable Reward çarpanı
) {
    // Animasyon state'leri
    var showContent by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    // Başlangıçta animasyonları tetikle
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
        delay(600)
        showStats = true
    }

    // Arka plan gradient rengi
    val backgroundColor = Color(0xFF1A237E) // Koyu mavi

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(rememberLocalizedText("exercise_completed")) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("health_hub") {
                            popUpTo("exercise") { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // Tebrikler başlığı
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -50 })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(80.dp)
                            )

                            Text(
                                text = "🎉 Tebrikler!",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Text(
                                text = getExerciseDisplayName(exerciseType),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Tekrar sayısı (büyük)
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = scaleIn() + fadeIn()
                    ) {
                        Surface(
                            modifier = Modifier.size(180.dp),
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            shadowElevation = 12.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = repCount.toString(),
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "TEKRAR",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Bonus Multiplier Badge (1.5x, 2.0x, 3.0x için göster)
                if (xpMultiplier > 1.0f) {
                    item {
                        AnimatedVisibility(
                            visible = showContent,
                            enter = scaleIn(
                                initialScale = 0.3f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn()
                        ) {
                            BonusMultiplierBadge(multiplier = xpMultiplier)
                        }
                    }
                }

                // Kazanılan XP
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = slideInHorizontally(initialOffsetX = { -300 }) + fadeIn()
                    ) {
                        RewardCard(
                            icon = "⭐",
                            label = "Kazanılan XP",
                            value = "+$totalXP XP",
                            backgroundColor = Color(0xFFFFC107),
                            multiplier = xpMultiplier  // Çarpanı göster
                        )
                    }
                }

                // Kalori
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = slideInHorizontally(initialOffsetX = { 300 }) + fadeIn()
                    ) {
                        RewardCard(
                            icon = "🔥",
                            label = "Yakılan Kalori",
                            value = "${calories.toInt()} kcal",
                            backgroundColor = Color(0xFFFF5722)
                        )
                    }
                }

                // Altın
                if (gold > 0) {
                    item {
                        AnimatedVisibility(
                            visible = showContent,
                            enter = slideInHorizontally(initialOffsetX = { -300 }) + fadeIn()
                        ) {
                            RewardCard(
                                icon = "💰",
                                label = "Kazanılan Altın",
                                value = "+$gold Altın",
                                backgroundColor = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                // Stat Artışları
                if (statGains.isNotEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = showStats,
                            enter = expandVertically() + fadeIn()
                        ) {
                            StatGainsSection(statGains = statGains)
                        }
                    }
                }

                // Ana menüye dön butonu
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            navController.navigate("health_hub") {
                                popUpTo("exercise") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = rememberLocalizedText("return_main_menu"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ödül kartı bileşeni
 */
@Composable
fun RewardCard(
    icon: String,
    label: String,
    value: String,
    backgroundColor: Color,
    multiplier: Float = 1.0f  // Opsiyonel çarpan gösterimi
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = icon,
                        fontSize = 40.sp
                    )
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Multiplier badge (sağ üst köşe)
            if (multiplier > 1.0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Red.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "${multiplier}x",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Stat artışları bölümü
 */
@Composable
fun StatGainsSection(statGains: Map<String, Float>) {
    val gamificationEngine = remember { GamificationEngine() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = rememberLocalizedText("stat_increases"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            statGains.forEach { (statName, value) ->
                val statType = try {
                    StatType.valueOf(statName)
                } catch (e: Exception) {
                    null
                }

                if (statType != null) {
                    StatGainRow(
                        statText = gamificationEngine.formatStatIncrease(statType, value),
                        value = value
                    )
                }
            }
        }
    }
}

/**
 * Tek bir stat artışı satırı
 */
@Composable
fun StatGainRow(statText: String, value: Float) {
    var animatedValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(value) {
        val duration = 800
        val steps = 50
        val stepValue = value / steps
        val stepDelay = duration / steps

        repeat(steps) {
            delay(stepDelay.toLong())
            animatedValue += stepValue
        }
        animatedValue = value
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statText.substringBefore("+"),
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "+${String.format("%.1f", animatedValue)}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    }
}

/**
 * Egzersiz tipini görünen isme çevirir
 */
fun getExerciseDisplayName(exerciseType: String): String {
    return try {
        ExerciseType.valueOf(exerciseType).displayName
    } catch (e: Exception) {
        "Egzersiz"
    }
}

/**
 * Bonus Multiplier Badge (Slot Machine Ödülü)
 *
 * Variable Reward sisteminin görsel geri bildirimi.
 * Kullanıcıya bonus kazandığını heyecanlı bir şekilde gösterir.
 */
@Composable
fun BonusMultiplierBadge(multiplier: Float) {
    // Çarpana göre renk ve emoji belirle
    val (badgeColor, badgeEmoji, badgeText) = when (multiplier) {
        3.0f -> Triple(
            Color(0xFFFFD700),  // Altın sarısı
            "🎰",
            "JACKPOT!!!"
        )
        2.0f -> Triple(
            Color(0xFFFF1744),  // Parlak kırmızı
            "🎉",
            "MEGA BONUS!!"
        )
        1.5f -> Triple(
            Color(0xFF00E676),  // Parlak yeşil
            "⭐",
            "BONUS!"
        )
        else -> Triple(
            Color(0xFF2196F3),
            "✓",
            "Normal"
        )
    }

    // Glow animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "bonus_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Scale pulse animasyonu
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Ana badge kartı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = badgeColor.copy(alpha = glowAlpha)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Emoji (pulsing)
                    Text(
                        text = badgeEmoji,
                        fontSize = (48 * scale).sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Bonus text
                    Text(
                        text = badgeText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    // Multiplier
                    Text(
                        text = "${multiplier}x XP",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }
        }

        // Alt açıklama
        Text(
            text = rememberLocalizedText("lucky_bonus_xp"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
