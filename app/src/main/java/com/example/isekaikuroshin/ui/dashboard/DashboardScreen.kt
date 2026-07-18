package com.example.isekaikuroshin.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.BorderStroke
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.Badge
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.utils.NotificationManager
import com.example.isekaikuroshin.utils.NavigationDebugger
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.ui.components.DiceRollOverlay
import com.example.isekaikuroshin.engine.DiceEngine
import com.example.isekaikuroshin.engine.DiceContext
import com.example.isekaikuroshin.engine.DiceActionType
import com.example.isekaikuroshin.engine.Season
import com.example.isekaikuroshin.engine.Weather
import com.example.isekaikuroshin.engine.MoodState
import com.example.isekaikuroshin.data.TimeOfDay
import com.example.isekaikuroshin.ui.theme.getDashboardColors
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme

// Data classes for user stats (based on HTML analysis)
data class UserStats(
    val str: Long = 15,
    val agi: Long = 12,
    val int: Long = 10,
    val vit: Long = 14,
    val spirit: Long = 11,
    val luck: Long = 8
)

data class UserInfo(
    val name: String = "Hunter",
    val codename: String = "Jinwoo",
    val level: Long = 10,
    val exp: Long = 60,
    val maxExp: Long = 100,
    val hp: Long = 75,
    val maxHp: Long = 100,
    val mp: Long = 50,
    val maxMp: Long = 100,
    val stamina: Long = 90,
    val maxStamina: Long = 100
)

// Extension functions for visual effects
fun Modifier.holographicGlow(
    color: Color,
    alpha: Float = 0.3f
) = this.drawBehind {
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = size.minDimension * 0.6f,
        blendMode = BlendMode.Screen
    )
}

fun Modifier.neonShadow(
    color: Color,
    elevation: Dp = 8.dp
) = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(8.dp),
    ambientColor = color.copy(alpha = 0.4f),
    spotColor = color.copy(alpha = 0.4f)
)

// Bottom Navigation Items (based on HTML)
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val hasNotification: Boolean = false,
    val notificationCount: Int = 0
)

// Social Menu Items for expandable social section
data class SocialMenuItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val hasNotification: Boolean = false,
    val notificationCount: Int = 0
)

@UnstableApi
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    userName: String = "Hunter", // This comes from navigation, typically linked to player's chosen name
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // Debug logging
    LaunchedEffect(Unit) {
        NavigationDebugger.logScreenComposition("dashboard")
    }

    DisposableEffect(Unit) {
        onDispose {
            NavigationDebugger.logScreenDisposal("dashboard")
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val notificationCounts by NotificationManager.notificationCounts.collectAsState()
    val dashboardColors = getDashboardColors()

    // Weather Sound Manager
    val context = LocalContext.current
    val weatherSoundManager = remember { com.example.isekaikuroshin.engine.WeatherSoundManager(context) }
    val gameData by PersistentDataManager.gameData.collectAsState()
    val ambientSoundEnabled = gameData.settingsData.uiSettings.enableAmbientSound
    val gameState by viewModel.gameStateManager.gameState.collectAsState()
    val currentWeather = gameState.currentWeather

    // Weather sound control
    LaunchedEffect(currentWeather, ambientSoundEnabled) {
        if (ambientSoundEnabled) {
            weatherSoundManager.playWeatherSound(currentWeather, volume = 0.7f)
        } else {
            weatherSoundManager.stopSound()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            weatherSoundManager.release()
        }
    }

    // Dice Roll Overlay State
    var showDiceOverlay by remember { mutableStateOf(false) }
    var currentDiceResult by remember { mutableStateOf<com.example.isekaikuroshin.engine.DiceResult?>(null) }
    var diceHasAdvantage by remember { mutableStateOf(false) }
    var diceHasDisadvantage by remember { mutableStateOf(false) }
    var diceTargetDifficulty by remember { mutableIntStateOf(10) }
    var diceBonusModifier by remember { mutableIntStateOf(0) }

    // Access playerState from uiState.playerStats (corrected from DashboardUiState structure)
    val playerState = uiState.playerStats // Access playerState from uiState

    val userStats = UserStats(
        str = playerState.strength.toLong(),
        agi = playerState.agility.toLong(),
        int = playerState.intelligence.toLong(),
        vit = playerState.vitality.toLong(),
        spirit = playerState.spirit.toLong(),
        luck = playerState.luck.toLong()
    )
    val userInfo = UserInfo(
        name = playerState.playerName, // Use player name from game state
        codename = userName, // Codename can remain the navigated one, or also from playerState if desired
        level = playerState.level.toLong(),
        exp = playerState.experience.toLong(),
        maxExp = playerState.experienceToNextLevel.toLong(),
        hp = playerState.currentHealth.toLong(),
        maxHp = playerState.maxHealth.toLong(),
        mp = playerState.currentMana.toLong(),
        maxMp = playerState.maxMana.toLong(),
        stamina = playerState.stamina.toLong(),
        maxStamina = 100L // Assuming maxStamina is fixed or comes from elsewhere if dynamic
    )

    // Collapsible states
    var mainStatsSectionC4IsExpanded by remember { mutableStateOf(true) }
    var titleSectionC7IsExpanded by remember { mutableStateOf(true) }
    var skillsSectionC9IsExpanded by remember { mutableStateOf(true) }

    // Expandable social menu state
    var socialMenuC15IsExpanded by remember { mutableStateOf(false) }

    val navItems = listOf(
        BottomNavItem(
            rememberLocalizedText("inventory"),
            Icons.Filled.Inventory,
            "inventory",
            hasNotification = NotificationManager.hasNotificationForRoute("inventory"),
            notificationCount = notificationCounts["inventory"] ?: 0
        ),
        BottomNavItem(
            rememberLocalizedText("adventure"),
            Icons.Filled.Explore,
            "adventure"
        ),
        BottomNavItem(
            rememberLocalizedText("journal"),
            Icons.Filled.Book,
            "journal",
            hasNotification = NotificationManager.hasNotificationForRoute("journal"),
            notificationCount = notificationCounts["journal"] ?: 0
        ),
        BottomNavItem(
            rememberLocalizedText("social"),
            Icons.Filled.Group,
            "social",
            hasNotification = listOf("character_catalog", "friends", "quests").any {
                NotificationManager.hasNotificationForRoute(it)
            },
            notificationCount = listOf("character_catalog", "friends", "quests").sumOf {
                notificationCounts[it] ?: 0
            }
        ),
        BottomNavItem(
            "Umbros",
            Icons.Filled.Psychology,
            "umbros_transition",
            hasNotification = false,
            notificationCount = 0
        ),
        BottomNavItem(
            rememberLocalizedText("settings"),
            Icons.Filled.Settings,
            "settings",
            hasNotification = NotificationManager.hasNotificationForRoute("settings"),
            notificationCount = notificationCounts["settings"] ?: 0
        )
    )

    val socialMenuItems = listOf(
        SocialMenuItem(
            rememberLocalizedText("characters"),
            Icons.Filled.Person,
            "character_catalog",
            hasNotification = NotificationManager.hasNotificationForRoute("character_catalog"),
            notificationCount = notificationCounts["character_catalog"] ?: 0
        ),
        SocialMenuItem(
            rememberLocalizedText("friends"),
            Icons.Filled.People,
            "friends",
            hasNotification = NotificationManager.hasNotificationForRoute("friends"),
            notificationCount = notificationCounts["friends"] ?: 0
        ),
        SocialMenuItem(
            rememberLocalizedText("quests"),
            Icons.AutoMirrored.Filled.Assignment,
            "quests",
            hasNotification = NotificationManager.hasNotificationForRoute("quests"),
            notificationCount = notificationCounts["quests"] ?: 0
        ),
        SocialMenuItem(
            rememberLocalizedText("guild"),
            Icons.Filled.Group,
            "guild"
        ),
        SocialMenuItem(
            rememberLocalizedText("training"),
            Icons.Filled.FitnessCenter,
            "train"
        )
    )

    var bottomNavigationC14SelectedItem by remember { mutableStateOf("adventure") } // Default active tab

    var showUmborusMessage by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(dashboardColors.backgroundDark)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp), // Space for bottom navigation
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showUmborusMessage) {
                UmborusMessageCard(onDismiss = { showUmborusMessage = false })
            }

            UserHeaderSectionC1(userInfo = userInfo, navController = navController, dashboardColors = dashboardColors)
            ExpBarSectionC2(current = userInfo.exp, max = userInfo.maxExp, dashboardColors = dashboardColors)
            // Pass the whole uiState to VitalBarsSectionC3 as it might need other fields like weightRatio
            VitalBarsSectionC3(uiState = uiState, dashboardColors = dashboardColors)

            // Assuming uiState.statusEffects is List<String> after refactoring StatusEffectZ2
            if (uiState.statusEffects.isNotEmpty()) {
                StatusEffectsSectionC12(statusEffects = uiState.statusEffects, dashboardColors = dashboardColors)
            }

            MainStatsSectionC4(
                stats = userStats,
                isExpanded = mainStatsSectionC4IsExpanded,
                onToggle = { mainStatsSectionC4IsExpanded = !mainStatsSectionC4IsExpanded },
                dashboardColors = dashboardColors,
                navController = navController,
                statPoints = playerState.statPoints // Use statPoints from playerState
            )

            TitleSectionC7(
                isExpanded = titleSectionC7IsExpanded,
                onToggle = { titleSectionC7IsExpanded = !titleSectionC7IsExpanded },
                dashboardColors = dashboardColors,
                gameStateManager = viewModel.gameStateManager
            )

            SkillsSectionC9(
                isExpanded = skillsSectionC9IsExpanded,
                onToggle = { skillsSectionC9IsExpanded = !skillsSectionC9IsExpanded },
                dashboardColors = dashboardColors
            )
        }

        HolographicBottomNavigationC14(
            items = navItems,
            selectedItem = bottomNavigationC14SelectedItem,
            onItemSelected = { route ->
                try {
                    NavigationDebugger.logButtonClick("Bottom Nav", route)
                    if (route == "social") {
                        socialMenuC15IsExpanded = !socialMenuC15IsExpanded
                        NavigationDebugger.logSocialMenuToggle(socialMenuC15IsExpanded)
                    } else {
                        socialMenuC15IsExpanded = false
                        if (route.isNotEmpty() && navController.currentDestination?.route != route) {
                            navController.navigate(route)
                        }
                        bottomNavigationC14SelectedItem = route
                    }
                } catch (e: Exception) {
                    NavigationDebugger.logNavigationError(route, e)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            dashboardColors = dashboardColors
        )

        if (socialMenuC15IsExpanded) {
            ExpandableSocialMenuC15(
                socialItems = socialMenuItems,
                onItemSelected = { route ->
                    try {
                        if (route.isNotEmpty() && navController.currentDestination?.route != route) {
                            navController.navigate(route)
                        }
                        bottomNavigationC14SelectedItem = route
                        socialMenuC15IsExpanded = false
                    } catch (e: Exception) {
                        NavigationDebugger.logCriticalError("Social Menu Navigation", "Navigation failed to $route", e)
                    }
                },
                onDismiss = { socialMenuC15IsExpanded = false },
                modifier = Modifier.align(Alignment.BottomCenter),
                dashboardColors = dashboardColors
            )
        }

        // Test Dice Roll Button (Top-right corner)
        Button(
            onClick = {
                // Create test dice context
                val testDiceContext = DiceContext(
                    season = Season.SPRING,
                    weather = Weather.SUNNY,
                    timeOfDay = TimeOfDay.MORNING,
                    locationDanger = 3,
                    healthPercentage = 1.0f,
                    manaPercentage = 0.8f,
                    staminaPercentage = 0.9f,
                    hungerLevel = 0.8f,
                    fatigueLevel = 0.2f,
                    moodState = MoodState.CONFIDENT,
                    luck = playerState.luck.toLong(),
                    intelligence = playerState.intelligence.toLong(),
                    charisma = playerState.intelligence.toLong(), // Using intelligence as fallback
                    perception = playerState.agility.toLong(), // Using agility as fallback
                    npcRelationship = 0.3f,
                    reputationLevel = 25,
                    actionType = DiceActionType.COMBAT_ATTACK,
                    difficulty = 15,
                    hasAdvantage = false,
                    hasDisadvantage = false,
                    equipmentBonus = 2,
                    magicalEnhancement = 1.2f
                )

                // Perform dice roll
                val result = DiceEngine.calculateComplexRoll(testDiceContext)

                // Set overlay state
                currentDiceResult = result
                diceHasAdvantage = testDiceContext.hasAdvantage
                diceHasDisadvantage = testDiceContext.hasDisadvantage
                diceTargetDifficulty = testDiceContext.difficulty
                diceBonusModifier = result.modifiedRoll - result.baseRoll
                showDiceOverlay = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("🎲 Test Dice", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        // Dice Roll Overlay
        if (showDiceOverlay && currentDiceResult != null) {
            DiceRollOverlay(
                diceResult = currentDiceResult!!,
                hasAdvantage = diceHasAdvantage,
                hasDisadvantage = diceHasDisadvantage,
                targetDifficulty = diceTargetDifficulty,
                bonusModifier = diceBonusModifier,
                onAnimationComplete = {
                    showDiceOverlay = false
                    currentDiceResult = null
                }
            )
        }
    }
}

@Composable
private fun UserHeaderSectionC1(userInfo: UserInfo, navController: NavController, dashboardColors: DashboardColorScheme) {
    // Implement or ensure this Composable is defined correctly elsewhere
    // For now, leaving as a stub to avoid further errors if it's not the focus
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Placeholder for user avatar, name, level etc.
        Text(text = "${userInfo.name} (Lvl ${userInfo.level})", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExpBarSectionC2(current: Long, max: Long, dashboardColors: DashboardColorScheme) {
    // Implement or ensure this Composable is defined correctly elsewhere
    Column {
        LinearProgressIndicator(
            progress = { if (max > 0) current.toFloat() / max.toFloat() else 0f },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = dashboardColors.primary,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
        Text("EXP: $current / $max", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

// IMPORTANT: VitalBarsSectionC3 and DashboardUiState must be in sync.
// DashboardUiState should hold playerState: PlayerState instead of playerStats.
@Composable
private fun VitalBarsSectionC3(uiState: DashboardUiState, dashboardColors: DashboardColorScheme) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VitalBar(
                label = rememberLocalizedText("hp"),
                current = uiState.playerStats.currentHealth.toLong(),
                max = uiState.playerStats.maxHealth.toLong(),
                color = MaterialTheme.colorScheme.error, // HP bar color from theme
                modifier = Modifier.weight(1f)
            )
            VitalBar(
                label = rememberLocalizedText("mp"),
                current = uiState.playerStats.currentMana.toLong(),
                max = uiState.playerStats.maxMana.toLong(),
                color = MaterialTheme.colorScheme.primaryContainer, // MP bar color from theme
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VitalBar( // Stamina
                label = rememberLocalizedText("stamina"),
                current = uiState.playerStats.stamina.toLong(),
                max = 100L, // Assuming maxStamina is fixed or comes from PlayerState if available
                color = MaterialTheme.colorScheme.secondaryContainer, // Stamina bar color from theme
                modifier = Modifier.weight(1f)
            )
            val weightColor = when {
                uiState.weightStatusDescription.contains("Hafif") -> Color.Green
                uiState.weightStatusDescription.contains("Normal") -> Color.Yellow
                uiState.weightStatusDescription.contains("Ağır") -> Color.Red
                else -> dashboardColors.primary
            }
            VitalBar(
                label = rememberLocalizedText("weight"),
                current = (uiState.weightRatio * 100).toLong(),
                max = 100L,
                color = weightColor,
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk
            )
        }
    }
}

@Composable
private fun MainStatsSectionC4(
    stats: UserStats,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    dashboardColors: DashboardColorScheme,
    navController: NavController,
    statPoints: Int // This now comes from playerState.statPoints
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(rememberLocalizedText("main_stats"), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) rememberLocalizedText("collapse") else rememberLocalizedText("expand"),
                tint = Color.White
            )
        }

        if (isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("STR", stats.str.toString(), Icons.Filled.FitnessCenter, dashboardColors, Modifier.weight(1f))
                    StatCard("AGI", stats.agi.toString(), Icons.Filled.FlashOn, dashboardColors, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("INT", stats.int.toString(), Icons.Filled.School, dashboardColors, Modifier.weight(1f))
                    StatCard("VIT", stats.vit.toString(), Icons.Filled.Favorite, dashboardColors, Modifier.weight(1f))
                }
                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("SPR", stats.spirit.toString(), Icons.Filled.Psychology, dashboardColors, Modifier.weight(1f))
                    StatCard("LCK", stats.luck.toString(), Icons.Filled.Star, dashboardColors, Modifier.weight(1f))
                }

                if (statPoints > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate("stat_allocation") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = dashboardColors.primary)
                    ) {
                        Text("${rememberLocalizedText("allocate_stat_points")} ($statPoints)", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, dashboardColors: DashboardColorScheme, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = dashboardColors.holographicBg,
        border = BorderStroke(1.dp, dashboardColors.holographicBorder),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = dashboardColors.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = dashboardColors.primary.copy(alpha = 0.8f))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun VitalBar(
    label: String,
    current: Long,
    max: Long,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
            Text("$current/$max", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (max > 0) current.toFloat() / max.toFloat() else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun TitleSectionC7(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    dashboardColors: DashboardColorScheme,
    gameStateManager: GameStateManager
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(rememberLocalizedText("titles_achievements"), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) rememberLocalizedText("collapse") else rememberLocalizedText("expand"),
                tint = Color.White
            )
        }
        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                // Dinamik Unvan Gösterimi
                val gameStateFlow by gameStateManager.gameState.collectAsState()
                val equippedTitle = gameStateManager.getEquippedTitle()

                if (equippedTitle != null) {
                    val activeTitle = rememberLocalizedText("active_title")
                    Text(
                        text = "$activeTitle ✨ ${equippedTitle.name} ✨",
                        color = dashboardColors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (equippedTitle.narrativeEffect != null) {
                        Text(
                            text = equippedTitle.narrativeEffect,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Text(
                        text = rememberLocalizedText("no_title"),
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dinamik Başarım Sayısı
                val totalBadges = gameStateFlow.activeBadges.filter { it.type == com.example.isekaikuroshin.data.BadgeType.BADGE }.size
                val totalAchievements = gameStateFlow.activeBadges.filter { it.type == com.example.isekaikuroshin.data.BadgeType.ACHIEVEMENT }.size
                val totalTitles = gameStateFlow.activeBadges.filter { it.type == com.example.isekaikuroshin.data.BadgeType.TITLE }.size

                Text("${rememberLocalizedText("badges_count")}: $totalBadges", color = Color.LightGray, fontSize = 14.sp)
                Text("${rememberLocalizedText("achievements_count")}: $totalAchievements", color = Color.LightGray, fontSize = 14.sp)
                Text("${rememberLocalizedText("titles_count")}: $totalTitles", color = Color.LightGray, fontSize = 14.sp)

                Button(onClick = { /* TODO: Navigate to achievements screen */ }) {
                    Text(rememberLocalizedText("view_all_badges"))
                }
            }
        }
    }
}

@Composable
fun SkillsSectionC9(isExpanded: Boolean, onToggle: () -> Unit, dashboardColors: DashboardColorScheme) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(rememberLocalizedText("skills_spells"), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) rememberLocalizedText("collapse") else rememberLocalizedText("expand"),
                tint = Color.White
            )
        }
        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text("${rememberLocalizedText("learned_skills_count")}: 12", color = Color.LightGray, fontSize = 14.sp)
                Text("${rememberLocalizedText("mastery_level")}: ${rememberLocalizedText("novice_mage")}", color = Color.LightGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { /* TODO: Navigate to skill tree */ }, colors = ButtonDefaults.buttonColors(containerColor = dashboardColors.primary.copy(alpha = 0.7f))) {
                    Text(rememberLocalizedText("view_skill_tree"), color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun UmborusMessageCard(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    if (visible) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(rememberLocalizedText("umborus_message"), fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { visible = false; onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(rememberLocalizedText("umborus_welcome"), color = Color.White)
            }
        }
    }
}

@Composable
fun StatusEffectsSectionC12(statusEffects: List<String>, dashboardColors: DashboardColorScheme) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(rememberLocalizedText("active_status_effects"), style = MaterialTheme.typography.titleMedium, color = Color.White)
        if (statusEffects.isEmpty()) {
            Text(rememberLocalizedText("none"), color = Color.LightGray, fontSize = 14.sp)
        } else {
            statusEffects.forEach { effectName ->
                Text(" - $effectName", color = dashboardColors.primary, fontSize = 14.sp)
            }
        }
    }
}


@Composable
fun HolographicBottomNavigationC14(
    items: List<BottomNavItem>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    dashboardColors: DashboardColorScheme
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) 
            .padding(horizontal = 8.dp, vertical = 4.dp) 
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        dashboardColors.holographicBg.copy(alpha = 0.8f),
                        dashboardColors.backgroundDark.copy(alpha = 0.9f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) 
            )
            .border(
                1.dp,
                dashboardColors.holographicBorder.copy(alpha = 0.7f),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.route == selectedItem
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onItemSelected(item.route) }
                        .padding(vertical = 8.dp) 
                        .graphicsLayer { 
                            if (isSelected) {
                                scaleX = 1.1f
                                scaleY = 1.1f
                            }
                        }
                        .holographicGlow( 
                            color = dashboardColors.primary,
                            alpha = if (isSelected) 0.5f else 0.0f
                        )
                ) {
                    BadgedBox(badge = {
                        if (item.hasNotification) {
                            Badge(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text(
                                    if (item.notificationCount > 0) item.notificationCount.toString() else "",
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) dashboardColors.primary else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(if (isSelected) 30.dp else 24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = if (isSelected) dashboardColors.primary else Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


@Composable
fun ExpandableSocialMenuC15(
    socialItems: List<SocialMenuItem>,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dashboardColors: DashboardColorScheme
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 80.dp) 
            .background(dashboardColors.backgroundDark.copy(alpha = 0.95f))
            .border(1.dp, dashboardColors.holographicBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp)
            .clickable(onClick = onDismiss) 
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            socialItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemSelected(item.route) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgedBox(badge = {
                        if (item.hasNotification) {
                            Badge(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text(
                                    if (item.notificationCount > 0) item.notificationCount.toString() else "",
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = dashboardColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = item.label,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
