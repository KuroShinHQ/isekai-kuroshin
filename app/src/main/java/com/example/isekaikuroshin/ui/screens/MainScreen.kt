package com.example.isekaikuroshin.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavHostController
import kotlin.math.absoluteValue
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.engine.Weather
import com.example.isekaikuroshin.engine.WeatherSoundManager
import com.example.isekaikuroshin.ui.character.CharacterCatalogScreen
import com.example.isekaikuroshin.ui.character.CharacterStatusScreen
import com.example.isekaikuroshin.ui.inventory.InventoryScreen
import com.example.isekaikuroshin.ui.journal.JournalScreen
import com.example.isekaikuroshin.ui.navigation.BottomNavItem
import com.example.isekaikuroshin.ui.settings.SettingsScreen
import com.example.isekaikuroshin.ui.healthhub.HealthHubScreen
import com.example.isekaikuroshin.ui.quest.QuestScreen
import com.example.isekaikuroshin.ui.map.MapScreen
import com.example.isekaikuroshin.ui.drone.DroneControllerScreen
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import androidx.navigation.compose.rememberNavController
import com.example.isekaikuroshin.data.rememberLocalizedText

import androidx.media3.common.util.UnstableApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import com.example.isekaikuroshin.data.LanguageManager

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@UnstableApi
@Composable
fun MainScreen(navController: NavHostController) {
    android.util.Log.d("MainScreen", "🎬 MainScreen COMPOSABLE BAŞLATILDI!")

    val context = LocalContext.current

    // FB#12: Ortam sesi MainScreen'de (tüm ekranlarda devam edecek)
    val weatherSoundManager = remember { WeatherSoundManager(context) }
    val gameData by PersistentDataManager.gameData.collectAsState()
    val ambientSoundEnabled = gameData.settingsData.uiSettings.enableAmbientSound

    // Ambient sound kontrolü (weather bazlı)
    LaunchedEffect(ambientSoundEnabled) {
        if (ambientSoundEnabled) {
            // Default olarak rainy ambient çalacak - journal ekranındayken weather bazlı değişir
            weatherSoundManager.playWeatherSound(Weather.RAINY, volume = 0.3f)
        } else {
            weatherSoundManager.stopSound()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            weatherSoundManager.release()
        }
    }
    val items = listOf(
        BottomNavItem.Status,
        BottomNavItem.Inventory,
        BottomNavItem.Quests,
        BottomNavItem.Catalog,
        BottomNavItem.Camp,
        BottomNavItem.Map,
        BottomNavItem.Journal,
        BottomNavItem.HealthHub,
        BottomNavItem.DroneController,
        BottomNavItem.Settings
    )
    android.util.Log.d("MainScreen", "📋 Bottom nav items created: ${items.size} items")
    items.forEachIndexed { index, item ->
        android.util.Log.d("MainScreen", "  [$index] ${item.route} - ${item.titleKey}")
    }

    val pagerState = rememberPagerState(pageCount = { items.size })
    android.util.Log.d("MainScreen", "📄 PagerState created: pageCount=${items.size}, currentPage=${pagerState.currentPage}")
    val coroutineScope = rememberCoroutineScope()
    var isNavExpanded by remember { mutableStateOf(false) }

    // 🔥 Haptic Feedback - sayfa değiştiğinde titreşim
    val hapticFeedback = LocalHapticFeedback.current
    val animationsEnabled = gameData.settingsData.uiSettings.enableAnimations

    // Sayfa değişimini dinle ve haptic feedback ver
    LaunchedEffect(pagerState.currentPage) {
        if (animationsEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            CompactLanguageSwitcher()
        },
        bottomBar = {
            CollapsibleBottomNavigationBar(
                pagerState = pagerState,
                items = items,
                coroutineScope = coroutineScope,
                isNavExpanded = isNavExpanded,
                onToggleExpand = { isNavExpanded = !isNavExpanded }
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            userScrollEnabled = true,
            beyondBoundsPageCount = 0 // GÖREV #1: UI Jank Fix - Sadece mevcut sayfa yüklensin (45 frame → <5 frame)
        ) { page ->
            android.util.Log.d("MainScreen", "🎬 HorizontalPager rendering page=$page, route=${items[page].route}")

            // 🔥 Instagram/TikTok Style Minimal Animation - Web Research Best Practice
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (animationsEnabled) {
                            // 🎯 Minimal Scale - Native & Smooth (1.0 → 0.95)
                            // Web araştırması: Alpha/Translation dikkat dağıtıcı, sadece scale kullan
                            val scale = lerp(1f, 0.95f, pageOffset.coerceIn(0f, 1f))
                            scaleX = scale
                            scaleY = scale
                        }
                    }
            ) {
                when (items[page].route) {
                BottomNavItem.Status.route -> CharacterStatusScreen()
                BottomNavItem.Inventory.route -> InventoryScreen()
                BottomNavItem.Quests.route -> QuestScreen(navController = navController)
                BottomNavItem.Catalog.route -> CharacterCatalogScreen(navController = navController)
                BottomNavItem.Camp.route -> CampScreen(navController = navController)
                BottomNavItem.Map.route -> MapScreen(onBackPressed = { navController.popBackStack() })
                BottomNavItem.Journal.route -> JournalScreen(onBackToDashboard = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(0) // Index 0 is CharacterStatusScreen
                    }
                })
                BottomNavItem.HealthHub.route -> HealthHubScreen(navController = navController)
                BottomNavItem.DroneController.route -> DroneControllerScreen()
                BottomNavItem.Settings.route -> SettingsScreen(navController = navController)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollapsibleBottomNavigationBar(
    pagerState: androidx.compose.foundation.pager.PagerState,
    items: List<BottomNavItem>,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    @Suppress("UNUSED_PARAMETER") isNavExpanded: Boolean,
    @Suppress("UNUSED_PARAMETER") onToggleExpand: () -> Unit
) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            val localizedTitle = rememberLocalizedText(item.titleKey)
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = localizedTitle) },
                label = { Text(text = localizedTitle) },
                selected = pagerState.currentPage == index,
                onClick = {
                    android.util.Log.d("MainScreen", "🔘 Bottom nav clicked: index=$index, route=${item.route}, title=$localizedTitle")
                    coroutineScope.launch {
                        android.util.Log.d("MainScreen", "📄 Scrolling to page $index")
                        pagerState.scrollToPage(index)
                        android.util.Log.d("MainScreen", "✅ Scrolled to page ${pagerState.currentPage}")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactLanguageSwitcher() {
    val currentLang by LanguageManager.currentLanguage.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf("TR", "EN")

    TopAppBar(
        title = { }, // Boş title, sadece sağ köşede buton olacak
        actions = {
            Box {
                // Kompakt dil butonu
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Change Language",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = LanguageManager.getLanguageDisplayName(currentLang),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Dropdown menü
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(LanguageManager.getLanguageDisplayName(lang))
                            },
                            onClick = {
                                LanguageManager.setLanguage(lang)
                                expanded = false
                            },
                            leadingIcon = {
                                if (lang == currentLang) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Preview(showBackground = true, name = "Main Screen - Dark")
@UnstableApi
@Composable
fun MainScreenPreview() {
    IsekaiKuroshinTheme(darkTheme = true) {
        val navController = rememberNavController()
        MainScreen(navController = navController)
    }
}
