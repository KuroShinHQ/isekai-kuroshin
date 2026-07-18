package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.sound.SoundManager
import com.example.isekaikuroshin.ui.theme.getDashboardColors
import com.example.isekaikuroshin.ui.components.SystemOverlay
import com.example.isekaikuroshin.ui.components.OverlayData
import com.example.isekaikuroshin.ui.components.DiceRollOverlay
import androidx.activity.ComponentActivity
import com.example.isekaikuroshin.engine.*
import com.example.isekaikuroshin.sound.SoundManagerEntryPoint
import com.example.isekaikuroshin.utils.GameLogger
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // FB#12: SoundManager'ı Hilt EntryPoint üzerinden al
    val context = LocalContext.current as ComponentActivity
    val soundManager = remember {
        val appContext = context.applicationContext
        EntryPointAccessors.fromApplication(
            appContext,
            SoundManagerEntryPoint::class.java
        ).soundManager()
    }
    val gameData by PersistentDataManager.gameData.collectAsState()
    val settings = gameData.settingsData

    // SystemOverlay states for testing
    var showOverlay by remember { mutableStateOf(false) }
    var overlayData by remember { mutableStateOf<OverlayData?>(null) }

    // Dice Roll Overlay State for testing
    var showDiceOverlay by remember { mutableStateOf(false) }
    var currentDiceResult by remember { mutableStateOf<DiceResult?>(null) }
    var diceHasAdvantage by remember { mutableStateOf(false) }
    var diceHasDisadvantage by remember { mutableStateOf(false) }
    var diceTargetDifficulty by remember { mutableIntStateOf(10) }
    var diceBonusModifier by remember { mutableIntStateOf(0) }

    // Developer Mode state
    var developerModeTapCount by remember { mutableIntStateOf(0) }

    // Get dynamic colors based on theme settings
    val dashboardColors = getDashboardColors()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(dashboardColors.backgroundDark)
                .padding(16.dp)
        ) {
            // Header (simplified without back button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = rememberLocalizedText("settings"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = dashboardColors.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SETTINGS REORGANIZATION: Material Design 3 best practices
                // Mantıklı gruplandırma: Oyun → Görünüm → AI → İstatistikler → Geliştirici → Topluluk

                // ═══════════════════════════════════════════════════════
                // 🎮 OYUN AYARLARI
                // ═══════════════════════════════════════════════════════
                item {
                    CategoryHeader(
                        title = rememberLocalizedText("game_settings_category"),
                        emoji = "🎮",
                        dashboardColors = dashboardColors
                    )
                }

                item {
                    GameSettingsSection(
                        settings = settings,
                        dashboardColors = dashboardColors,
                        soundManager = soundManager
                    )
                }

                item {
                    NotificationSettingsSection(
                        settings = settings,
                        dashboardColors = dashboardColors
                    )
                }

                // G59: Performance Settings Navigation
                item {
                    Button(
                        onClick = { navController.navigate("performance_settings") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dashboardColors.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Column {
                                    Text(
                                        text = rememberLocalizedText("performance_settings_title"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "FPS, Graphics, Thermal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // ═══════════════════════════════════════════════════════
                // 🎨 GÖRÜNÜM
                // ═══════════════════════════════════════════════════════
                item {
                    CategoryHeader(
                        title = rememberLocalizedText("appearance_category"),
                        emoji = "🎨",
                        dashboardColors = dashboardColors
                    )
                }

                item {
                    UISettingsSection(
                        settings = settings,
                        dashboardColors = dashboardColors
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // ═══════════════════════════════════════════════════════
                // 🤖 YAPAY ZEKA
                // ═══════════════════════════════════════════════════════
                item {
                    CategoryHeader(
                        title = rememberLocalizedText("ai_category"),
                        emoji = "🤖",
                        dashboardColors = dashboardColors
                    )
                }

                item {
                    StoryAndAISettingsSection(
                        settings = settings,
                        dashboardColors = dashboardColors,
                        viewModel = viewModel
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // ═══════════════════════════════════════════════════════
                // 📊 KULLANIM İSTATİSTİKLERİ
                // ═══════════════════════════════════════════════════════
                item {
                    CategoryHeader(
                        title = rememberLocalizedText("usage_stats_category"),
                        emoji = "📊",
                        dashboardColors = dashboardColors
                    )
                }

                item {
                    UsageStatsSection(
                        dashboardColors = dashboardColors
                    )
                }

                // ═══════════════════════════════════════════════════════
                // 🔧 GELİŞTİRİCİ SEÇENEKLERİ (gizli)
                // ═══════════════════════════════════════════════════════
                if (settings.debugSettings.isDeveloperModeEnabled) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    item {
                        CategoryHeader(
                            title = rememberLocalizedText("developer_category"),
                            emoji = "🔧",
                            dashboardColors = dashboardColors
                        )
                    }

                    item {
                        DeveloperOptionsSection(
                            settings = settings,
                            gameData = gameData,
                            dashboardColors = dashboardColors,
                            navController = navController,
                            viewModel = viewModel,
                            onShowOverlay = { overlay ->
                                overlayData = overlay
                                showOverlay = true
                            },
                            onShowDiceOverlay = { result, hasAdvantage, hasDisadvantage, targetDifficulty, bonusModifier ->
                                currentDiceResult = result
                                diceHasAdvantage = hasAdvantage
                                diceHasDisadvantage = hasDisadvantage
                                diceTargetDifficulty = targetDifficulty
                                diceBonusModifier = bonusModifier
                                GameLogger.logSystem("🎲 showDiceOverlay = true olarak ayarlanıyor...")
                                showDiceOverlay = true
                                GameLogger.logSystem("🎲 Overlay koşulları - showDiceOverlay: $showDiceOverlay, currentDiceResult != null: ${currentDiceResult != null}")
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ═══════════════════════════════════════════════════════
                // 🌐 TOPLULUK
                // ═══════════════════════════════════════════════════════
                item {
                    CategoryHeader(
                        title = rememberLocalizedText("community_category"),
                        emoji = "🌐",
                        dashboardColors = dashboardColors
                    )
                }

                item {
                    SocialMediaSection()
                }

                item {
                    MockPollSection()
                }

                // Version number (for developer mode activation)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rememberLocalizedText("version"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                developerModeTapCount++
                                if (developerModeTapCount >= 7) {
                                    if (!settings.debugSettings.isDeveloperModeEnabled) {
                                        // Enable developer mode
                                        PersistentDataManager.updateSettingsData { settingsData ->
                                            settingsData.copy(
                                                debugSettings = settingsData.debugSettings.copy(
                                                    isDeveloperModeEnabled = true
                                                )
                                            )
                                        }
                                        Toast.makeText(context, LanguageManager.getText("developer_mode_enabled"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Disable developer mode
                                        PersistentDataManager.updateSettingsData { settingsData ->
                                            settingsData.copy(
                                                debugSettings = settingsData.debugSettings.copy(
                                                    isDeveloperModeEnabled = false
                                                )
                                            )
                                        }
                                        Toast.makeText(context, LanguageManager.getText("developer_mode_disabled"), Toast.LENGTH_SHORT).show()
                                    }
                                    // Reset tap count
                                    developerModeTapCount = 0
                                }
                            }
                        )
                    }
                }
            }
        }

        // DiceRollOverlay positioned at top center of screen
        if (showDiceOverlay && currentDiceResult != null) {
            GameLogger.logSystem("🎲 DiceRollOverlay görüntüleniyor!")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)) // Semi-transparent background
            ) {
                DiceRollOverlay(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
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
            GameLogger.logSystem("🎲 DiceRollOverlay başarıyla oluşturuldu")
        } else {
            if (showDiceOverlay) {
                GameLogger.logSystem("🎲 DiceRollOverlay görüntülenemiyor - currentDiceResult NULL!")
            }
        }

        // SystemOverlay for testing (modal overlay)
        SystemOverlay(
            overlayData = overlayData,
            isVisible = showOverlay,
            onDismiss = {
                showOverlay = false
                overlayData = null
            }
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen Preview")
@Composable
fun SettingsScreenPreview() {
    val navController = rememberNavController()
    // Not: Bu temel önizleme, Hilt ViewModel'i veya PersistentDataManager gibi
    // karmaşık bağımlılıklar nedeniyle tam olarak doğru görüntülenmeyebilir.
    // Gerekirse, bu bağımlılıklar için sahte (fake) implementasyonlar sağlamanız
    // veya SettingsScreen'ı önizleme modunda bu bağımlılıklar olmadan çalışacak
    // şekilde düzenlemeniz gerekebilir.
    // Örneğin, projenizin tema Composable'ı ile sarmalamak da gerekebilir:
    // YourProjectTheme { SettingsScreen(navController = navController) }
    SettingsScreen(navController = navController)
}

// ============================================
// GÖREV 1: Sosyal Medya İkonları
// ============================================

@Composable
fun SocialMediaSection() {
    val context = LocalContext.current
    val dashboardColors = getDashboardColors()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = rememberLocalizedText("follow_us_social"),
                style = MaterialTheme.typography.titleMedium,
                color = dashboardColors.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // LinkedIn
                SocialMediaIcon(
                    emoji = "💼",
                    label = "LinkedIn",
                    url = "https://linkedin.com/in/YOUR_PROFILE", // TODO: Gerçek link ekle
                    context = context,
                    dashboardColors = dashboardColors
                )

                // GitHub
                SocialMediaIcon(
                    emoji = "💻",
                    label = "GitHub",
                    url = "https://github.com/YOUR_USERNAME", // TODO: Gerçek link ekle
                    context = context,
                    dashboardColors = dashboardColors
                )

                // YouTube
                SocialMediaIcon(
                    emoji = "📺",
                    label = "YouTube",
                    url = "https://youtube.com/@YOUR_CHANNEL", // TODO: Gerçek link ekle
                    context = context,
                    dashboardColors = dashboardColors
                )

                // Kick
                SocialMediaIcon(
                    emoji = "🎮",
                    label = "Kick",
                    url = "https://kick.com/YOUR_CHANNEL", // TODO: Gerçek link ekle
                    context = context,
                    dashboardColors = dashboardColors
                )
            }
        }
    }
}

@Composable
fun SocialMediaIcon(
    emoji: String,
    label: String,
    url: String,
    context: android.content.Context,
    dashboardColors: com.example.isekaikuroshin.ui.theme.DashboardColorScheme
) {
    // Pre-calculate localized text in Composable scope
    val linkOpenFailedText = rememberLocalizedText("link_open_failed")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.data = android.net.Uri.parse(url)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, linkOpenFailedText, Toast.LENGTH_SHORT).show()
                }
            }
            .padding(8.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 32.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================
// GÖREV 2: MOCK Anket Sistemi
// ============================================

@Composable
fun MockPollSection() {
    val dashboardColors = getDashboardColors()
    var userIsDonor by remember { mutableStateOf(false) }
    var hasVoted by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }

    // MOCK: Örnek anket
    val mockPoll = remember {
        com.example.isekaikuroshin.data.community.Poll(
            titleTr = "Gelecek Güncelleme İçin Önceliğiniz?",
            titleEn = "Your Priority for Next Update?",
            descriptionTr = "Hangi alanda geliştirme görmek istersiniz?",
            descriptionEn = "Which area would you like to see improvements?",
            options = listOf(
                com.example.isekaikuroshin.data.community.PollOption(
                    id = "ui",
                    textTr = "🎨 UI İyileştirmeleri",
                    textEn = "🎨 UI Improvements",
                    voteCount = 35
                ),
                com.example.isekaikuroshin.data.community.PollOption(
                    id = "combat",
                    textTr = "⚔️ Oynanış Güncellemeleri (Savaş Temalı)",
                    textEn = "⚔️ Gameplay Updates (Combat Themed)",
                    voteCount = 42
                ),
                com.example.isekaikuroshin.data.community.PollOption(
                    id = "optimization",
                    textTr = "⚡ Optimizasyon Geliştirmeleri",
                    textEn = "⚡ Optimization Improvements",
                    voteCount = 23
                )
            ),
            totalVotes = 100,
            isActive = true
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = rememberLocalizedText("community_poll"),
                style = MaterialTheme.typography.titleMedium,
                color = dashboardColors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = mockPoll.titleEn,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = mockPoll.descriptionEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!userIsDonor) {
                // Bağışçı değilse uyarı göster
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = rememberLocalizedText("poll_donor_required"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = { userIsDonor = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dashboardColors.primary
                        )
                    ) {
                        Text(rememberLocalizedText("mock_mark_as_donor"))
                    }
                }
            } else {
                // Bağışçıysa oylama UI
                mockPoll.options.forEachIndexed { index, option ->
                    PollOptionItem(
                        option = option,
                        totalVotes = mockPoll.totalVotes,
                        isSelected = selectedOption == index,
                        hasVoted = hasVoted,
                        onClick = {
                            if (!hasVoted) {
                                selectedOption = index
                                hasVoted = true
                            }
                        },
                        dashboardColors = dashboardColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = rememberLocalizedText("total_votes") + ": ${mockPoll.totalVotes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (hasVoted) {
                    Text(
                        text = rememberLocalizedText("vote_recorded"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PollOptionItem(
    option: com.example.isekaikuroshin.data.community.PollOption,
    totalVotes: Int,
    isSelected: Boolean,
    hasVoted: Boolean,
    onClick: () -> Unit,
    dashboardColors: com.example.isekaikuroshin.ui.theme.DashboardColorScheme
) {
    val percentage = if (totalVotes > 0) {
        (option.voteCount.toFloat() / totalVotes * 100).toInt()
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !hasVoted) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && hasVoted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.textTr,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            if (hasVoted) {
                Text(
                    text = "$percentage% (${option.voteCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================
// CATEGORY HEADER - Material Design 3
// ============================================

@Composable
fun CategoryHeader(
    title: String,
    emoji: String,
    dashboardColors: com.example.isekaikuroshin.ui.theme.DashboardColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = dashboardColors.primary,
            letterSpacing = 1.2.sp
        )
    }

    // Divider
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = dashboardColors.primary.copy(alpha = 0.3f)
    )
}
