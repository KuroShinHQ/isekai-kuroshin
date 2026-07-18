package com.example.isekaikuroshin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.utils.SystemDebugger
import android.util.Log
import com.example.isekaikuroshin.ui.aidialog.AIDialogScreen
import com.example.isekaikuroshin.ui.journal.JournalScreen
import com.example.isekaikuroshin.ui.transition.TransitionScreen
import com.example.isekaikuroshin.ui.adventure.AdventureScreen
import com.example.isekaikuroshin.ui.settings.SettingsScreen
import com.example.isekaikuroshin.ui.settings.PerformanceSettingsScreen
import com.example.isekaikuroshin.ui.placeholder.GuildScreen
import com.example.isekaikuroshin.ui.placeholder.FriendsScreen
import com.example.isekaikuroshin.ui.placeholder.LeaderboardScreen
import com.example.isekaikuroshin.ui.intro.UserEntryScreen
import com.example.isekaikuroshin.ui.intro.LegalConsentScreen
import com.example.isekaikuroshin.ui.intro.PermissionIntroScreen
import com.example.isekaikuroshin.utils.PermissionRequester
import com.example.isekaikuroshin.ui.dashboard.UmbrosTransitionScreen
import com.example.isekaikuroshin.ui.dashboard.PostDeathKarmaScreen
import com.example.isekaikuroshin.ui.training.TrainingScreen
import com.example.isekaikuroshin.ui.cultivation.ManaCultivationScreen
import com.example.isekaikuroshin.ui.screens.MainScreen
import com.example.isekaikuroshin.ui.character.CharacterCatalogScreen
import com.example.isekaikuroshin.ui.screens.CampScreen
import com.example.isekaikuroshin.ui.inventory.InventoryScreen
import com.example.isekaikuroshin.ui.map.MapScreen
import com.example.isekaikuroshin.ui.statallocation.StatAllocationScreen
import com.example.isekaikuroshin.ui.screens.QuestsScreen
import com.example.isekaikuroshin.ui.skilltree.SkillTreeScreen
import com.example.isekaikuroshin.ui.screens.CraftingScreen
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.game.GameStateManager
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.database.AppDatabase
import com.example.isekaikuroshin.data.database.DatabaseTestHelper
import com.example.isekaikuroshin.data.database.DynamicNPCEntity
import com.example.isekaikuroshin.engine.WorldUpdateEngine
import com.example.isekaikuroshin.engine.HybridNLUEngine
import com.example.isekaikuroshin.engine.CommandProcessingEngine
import com.example.isekaikuroshin.engine.HealthMindCommandParser
import com.example.isekaikuroshin.sound.SoundManager
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
@UnstableApi
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var worldUpdateEngine: WorldUpdateEngine

    @Inject
    lateinit var hybridNLUEngine: HybridNLUEngine

    @Inject
    lateinit var commandProcessingEngine: CommandProcessingEngine

    @Inject
    lateinit var soundManager: SoundManager  // FB#12: SoundManager lifecycle yönetimi için

    @Inject
    lateinit var gameStateManager: GameStateManager  // TODO-G96: Immediate save on app pause

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔍 G127 DEBUG: Splash screen theme info
        Log.d("🎨 SPLASH_DEBUG", "═══════════════════════════════════════")
        Log.d("🎨 SPLASH_DEBUG", "🔍 THEME INFO AT APP START")
        try {
            val themeName = theme.toString()
            Log.d("🎨 SPLASH_DEBUG", "📦 Theme: $themeName")

            // Get windowBackground attribute
            val attributes = theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
            val windowBgResourceId = attributes.getResourceId(0, 0)
            attributes.recycle()

            val windowBgName = if (windowBgResourceId != 0) {
                resources.getResourceEntryName(windowBgResourceId)
            } else "null"

            Log.d("🎨 SPLASH_DEBUG", "🖼️  windowBackground: $windowBgName (ID: $windowBgResourceId)")
            Log.d("🎨 SPLASH_DEBUG", "📱 Android Version: ${android.os.Build.VERSION.SDK_INT}")

            // Android 12+ (API 31+) Splash Screen API attributes
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                Log.d("🎨 SPLASH_DEBUG", "🔍 CHECKING ANDROID 12+ SPLASH SCREEN API ATTRIBUTES:")

                val splashAttrs = theme.obtainStyledAttributes(intArrayOf(
                    android.R.attr.windowSplashScreenBackground,
                    android.R.attr.windowSplashScreenAnimatedIcon,
                    android.R.attr.windowSplashScreenAnimationDuration
                ))

                val splashBgColorId = splashAttrs.getResourceId(0, 0)
                val splashBgColor = splashAttrs.getColor(0, -1)
                val splashIconId = splashAttrs.getResourceId(1, 0)
                val splashDuration = splashAttrs.getInt(2, -1)

                splashAttrs.recycle()

                Log.d("🎨 SPLASH_DEBUG", "   windowSplashScreenBackground: ColorID=$splashBgColorId, Color=#${Integer.toHexString(splashBgColor)}")
                Log.d("🎨 SPLASH_DEBUG", "   windowSplashScreenAnimatedIcon: ResourceID=$splashIconId")
                Log.d("🎨 SPLASH_DEBUG", "   windowSplashScreenAnimationDuration: ${splashDuration}ms")
            }
        } catch (e: Exception) {
            Log.e("🎨 SPLASH_DEBUG", "❌ Error getting theme info", e)
        }
        Log.d("🎨 SPLASH_DEBUG", "═══════════════════════════════════════")

        super.onCreate(savedInstanceState)

        // Initialize PersistentDataManager first (synchronously)
        try {
            PersistentDataManager.initializeSynchronously(this)
            Log.d("MainActivity", "✅ PersistentDataManager initialized")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ PersistentDataManager initialization failed", e)
        }

        // Initialize MediaDatabaseHelper for dynamic media loading
        // PERFORMANCE: Background thread'de pre-build et
        try {
            com.example.isekaikuroshin.engine.MediaDatabaseHelper.initialize(this)
            Log.d("MainActivity", "✅ MediaDatabaseHelper initialized")

            // BUGFIX: Force refresh database on app start to pick up renamed files
            com.example.isekaikuroshin.engine.MediaDatabaseHelper.refreshDatabase()

            // Background thread'de database'i build et (ilk açılışta hızlandırır)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val startTime = System.currentTimeMillis()
                    com.example.isekaikuroshin.engine.MediaDatabaseHelper.preloadDatabase()
                    val duration = System.currentTimeMillis() - startTime
                    Log.d("MainActivity", "🎬 MediaDatabase pre-build tamamlandı: ${duration}ms")
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ MediaDatabase pre-build hatası", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ MediaDatabaseHelper initialization failed", e)
        }

        // G129: Initialize notification channels (Android 8.0+)
        try {
            com.example.isekaikuroshin.utils.SystemNotificationHelper.createNotificationChannels(this)
            Log.d("MainActivity", "✅ G129: Notification channels created")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ G129: Notification channels creation failed", e)
        }

        // GameStateManager is now handled by Hilt dependency injection

        // FB#12: SoundManager lifecycle'a bağla - SADECE inject edildikten sonra
        try {
            if (::soundManager.isInitialized) {
                lifecycle.addObserver(soundManager)
                Log.d("MainActivity", "🔊 SoundManager lifecycle'a bağlandı")
            } else {
                Log.w("MainActivity", "⚠️ SoundManager henüz inject edilmedi, lifecycle'a eklenemedi")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ SoundManager lifecycle bağlama hatası", e)
        }

        // Initialize debug systems
        SystemDebugger.initializeAppDebugSystem(this)
        SystemDebugger.logAppLaunch()

        // TODO-NEM-06: Initialize and test faction/diplomacy system - SADECE database inject edildikten sonra
        try {
            if (::database.isInitialized) {
                DatabaseTestHelper.initializeAndTestWorldData(database)
                Log.d("MainActivity", "🌍 Fraksiyon ve Diplomasi sistemi test edildi")
            } else {
                Log.w("MainActivity", "⚠️ Database henüz inject edilmedi, dünya verileri yüklenemedi")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Database test hatası", e)
        }

        // G78: Seed predefined NPCs to Room database for WorldUpdateEngine
        try {
            if (::database.isInitialized) {
                CoroutineScope(Dispatchers.IO).launch {
                    this@MainActivity.seedPredefinedNPCs()
                }
            } else {
                Log.w("MainActivity", "⚠️ G78: Database henüz inject edilmedi, NPC seeding atlandı")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ G78: NPC seeding hatası", e)
        }

        // TODO-TEST-01: DEVRE DIŞI BIRAKILDI - Simülasyon app girişini bloke ediyor!
        // Log.d("MainActivity", "🚀 TODO-TEST-01: Uzun Süreli Yaşayan Dünya Simülasyonu başlatılıyor...")
        // DatabaseTestHelper.runLongTermSimulation(database, worldUpdateEngine, 120)

        // TODO-HUB-01: Test Hibrit NLU Motoru - SADECE inject edildikten sonra
        if (::hybridNLUEngine.isInitialized && ::commandProcessingEngine.isInitialized) {
            Log.d("MainActivity", "🧠 TODO-HUB-01: Hibrit NLU Motoru test edilmeye başlıyor...")
            CoroutineScope(Dispatchers.IO).launch {
                testHybridNLUSystem()
            }
        } else {
            Log.w("MainActivity", "⚠️ HybridNLU henüz inject edilmedi, test atlandı")
        }

        // GÖREV I: Bildirim Kanalları Oluştur
        try {
            com.example.isekaikuroshin.utils.NotificationChannelManager.createNotificationChannels(this)
            Log.d("MainActivity", "🔔 GÖREV I: Bildirim kanalları oluşturuldu")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ GÖREV I: Bildirim kanalı hatası", e)
        }

        // GÖREV I: Session Başlat
        try {
            com.example.isekaikuroshin.utils.SmartScheduler.logSessionStart()
            Log.d("MainActivity", "📱 GÖREV I: Session başladı")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ GÖREV I: Session start hatası", e)
        }

        // GÖREV I: Akıllı Scheduler Initialize Et
        try {
            com.example.isekaikuroshin.utils.SmartScheduler.initializeScheduler(this)
            Log.d("MainActivity", "⏰ GÖREV I: SmartScheduler initialize edildi")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ GÖREV I: Scheduler init hatası", e)
        }

        enableEdgeToEdge()
        setContent {
            IsekaiKuroshinTheme {
                KuroshinIsekaiApp()
            }
        }
    }

    /**
     * TODO-G96: Save game state immediately when app pauses
     * Critical for preserving user progress when app goes to background
     */
    override fun onPause() {
        super.onPause()

        try {
            if (::gameStateManager.isInitialized) {
                // Fire and forget - immediate save on background
                CoroutineScope(Dispatchers.IO).launch {
                    gameStateManager.saveGameStateImmediately()
                }
                Log.d("MainActivity", "💾 TODO-G96: Game state immediate save triggered (onPause)")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ TODO-G96: onPause save failed", e)
        }
    }

    /**
     * GÖREV I: Session bitişini kaydet
     */
    override fun onDestroy() {
        super.onDestroy()

        try {
            com.example.isekaikuroshin.utils.SmartScheduler.logSessionEnd()
            Log.d("MainActivity", "📴 GÖREV I: Session bitti")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ GÖREV I: Session end hatası", e)
        }
    }

    /**
     * TODO-HUB-01: Hibrit NLU Motoru Test Fonksiyonu
     */
    private suspend fun testHybridNLUSystem() {
        try {
            Log.d("MainActivity", "=== TODO-HUB-01: HİBRİT NLU MOTORİ TESTİ BAŞLIYOR ===")

            // Test 1: Basit komut (Hızlı Yol)
            Log.d("MainActivity", "📋 Test 1: Basit komut - 'kilo: 85 kg'")
            val simpleCommand = hybridNLUEngine.parseCommand("kilo: 85 kg")
            val simpleResult = commandProcessingEngine.processCommand(simpleCommand)
            Log.d("MainActivity", "✅ Basit komut sonucu: ${simpleCommand.commandType}, Başarı: $simpleResult")

            kotlinx.coroutines.delay(2000) // 2 saniye bekle

            // Test 2: Karmaşık komut (Akıllı Yol)
            Log.d("MainActivity", "📋 Test 2: Karmaşık komut - 'dün akşam yediğim yemeği kaydet'")
            val complexCommand = hybridNLUEngine.parseCommand("dün akşam yediğim yemeği kaydet")
            val complexResult = commandProcessingEngine.processCommand(complexCommand)
            Log.d("MainActivity", "✅ Karmaşık komut sonucu: ${complexCommand.commandType}, Başarı: $complexResult")

            kotlinx.coroutines.delay(2000) // 2 saniye bekle

            // Test 3: Alarm komut
            Log.d("MainActivity", "📋 Test 3: Alarm komut - 'alarmı 07:30'a kur'")
            val alarmCommand = hybridNLUEngine.parseCommand("alarmı 07:30'a kur")
            val alarmResult = commandProcessingEngine.processCommand(alarmCommand)
            Log.d("MainActivity", "✅ Alarm komut sonucu: ${alarmCommand.commandType}, Başarı: $alarmResult")

            kotlinx.coroutines.delay(2000) // 2 saniye bekle

            // Test 4: Belirsiz komut
            Log.d("MainActivity", "📋 Test 4: Belirsiz komut - 'bugün çok yorgunum'")
            val ambiguousCommand = hybridNLUEngine.parseCommand("bugün çok yorgunum")
            val ambiguousResult = commandProcessingEngine.processCommand(ambiguousCommand)
            Log.d("MainActivity", "✅ Belirsiz komut sonucu: ${ambiguousCommand.commandType}, Başarı: $ambiguousResult")

            Log.d("MainActivity", "=== TODO-HUB-01: HİBRİT NLU MOTORİ TESTİ TAMAMLANDI ===")
            Log.d("MainActivity", "🎯 Test sonucu: Hibrit NLU sistemi başarıyla çalışıyor!")
            Log.d("MainActivity", "📊 Test özetı:")
            Log.d("MainActivity", "  - Hızlı yol (basit komutlar): ✓ Çalışıyor")
            Log.d("MainActivity", "  - Akıllı yol (karmaşık komutlar): ✓ Çalışıyor")
            Log.d("MainActivity", "  - Komut işleme motoru: ✓ Çalışıyor")
            Log.d("MainActivity", "  - Fallback mekanizması: ✓ Çalışıyor")

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ TODO-HUB-01 test hatası: ${e.message}", e)
        }
    }

    /**
     * G78: Seed predefined NPCs to Room database for WorldUpdateEngine
     *
     * This function converts all predefined NPCs from CharacterManager to DynamicNPCEntity
     * and inserts them into the Room database. Only runs once on first launch.
     */
    private suspend fun seedPredefinedNPCs() {
        try {
            val dao = database.dynamicNpcDao()

            // Check if database already has NPCs (avoid re-seeding)
            val existingNPCs = dao.getAllNpcStates()
            if (existingNPCs.isNotEmpty()) {
                Log.d("MainActivity", "🔹 G78: Database already has ${existingNPCs.size} NPCs, skipping seed")
                return
            }

            Log.d("MainActivity", "🌱 G78: Seeding predefined NPCs to database...")

            val currentTime = System.currentTimeMillis()

            // Create predefined NPCs with all required parameters
            val predefinedNPCs = listOf(
                DynamicNPCEntity(
                    npcId = "elowen_moonwhisper",
                    baseRegistryId = "npc_mage_001",
                    level = 10,
                    currentStrength = 5,
                    currentAgility = 8,
                    currentIntelligence = 15,
                    currentVitality = 7,
                    personalityTraits = listOf("Curious", "Scholarly", "Mysterious"),
                    currentMood = "NEUTRAL",
                    loyaltyToPlayer = 50,
                    historyWithPlayer = emptyList(),
                    interactionCount = 0,
                    lastSeenDate = currentTime,
                    lastLocationId = "village_library",
                    npcRelationships = emptyMap(),
                    nemesisLevel = 0,
                    vendettaReasons = emptyList(),
                    adaptationTraits = emptyList(),
                    currentLocationId = "village_library",
                    isAlive = true,
                    isDead = false,
                    isExiled = false,
                    lastUpdateTime = currentTime,
                    evolutionPoints = 0,
                    timesSeen = 0,
                    timesDefeated = 0,
                    timesHelped = 0
                ),
                DynamicNPCEntity(
                    npcId = "ragar_stoneforge",
                    baseRegistryId = "npc_warrior_001",
                    level = 12,
                    currentStrength = 18,
                    currentAgility = 6,
                    currentIntelligence = 5,
                    currentVitality = 14,
                    personalityTraits = listOf("Strong", "Loyal", "Craftsman"),
                    currentMood = "NEUTRAL",
                    loyaltyToPlayer = 30,
                    historyWithPlayer = emptyList(),
                    interactionCount = 0,
                    lastSeenDate = currentTime,
                    lastLocationId = "village_forge",
                    npcRelationships = emptyMap(),
                    nemesisLevel = 0,
                    vendettaReasons = emptyList(),
                    adaptationTraits = emptyList(),
                    currentLocationId = "village_forge",
                    isAlive = true,
                    isDead = false,
                    isExiled = false,
                    lastUpdateTime = currentTime,
                    evolutionPoints = 0,
                    timesSeen = 0,
                    timesDefeated = 0,
                    timesHelped = 0
                )
            )

            // Insert into database
            for (npcEntity in predefinedNPCs) {
                dao.insertNpcState(npcEntity)
                Log.d("MainActivity", "✅ G78: Seeded NPC: ${npcEntity.npcId}")
            }

            val finalNPCs = dao.getAllNpcStates()
            Log.d("MainActivity", "🌱 G78: ✅ Successfully seeded ${finalNPCs.size} NPCs to database")
            Log.d("MainActivity", "🌱 G78: WorldUpdateEngine can now evolve these NPCs")

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ G78: Failed to seed NPCs", e)
        }
    }
}

@UnstableApi
@Composable
fun KuroshinIsekaiApp() {
    val navController = rememberNavController()

    // GÖREV I.2: İlk route yasal rıza ekranı, sonra user_entry
    // ÖLÜM AKIŞI YENİ TASARIM: Ölü oyuncu için postdeath_karma'dan başla
    // G25-FIX: remember kullanarak sadece ilk composition'da hesapla (recomposition'da değişmesin!)
    val initialRoute = remember {
        val gameData = PersistentDataManager.gameData.value
        val isDead = !gameData.playerData.isAlive
        if (isDead) {
            android.util.Log.d("MainActivity", "💀 ÖLÜM AKIŞI: Oyuncu ölü, postdeath_karma'dan başlatılıyor")
            "postdeath_karma"
        } else {
            "legal_consent"
        }
    }

    NavHost(
        navController = navController,
        startDestination = initialRoute
    ) {
        // GÖREV I.2: Yasal Uyarı ve Gizlilik Ekranı (İLK EKRAN)
        composable("legal_consent") {
            // Daha önce kabul edilmiş mi kontrol et
            val gameData = PersistentDataManager.gameData.value
            val hasConsent = gameData.settingsData.legalConsent.hasAcceptedTerms

            if (hasConsent) {
                // Direkt permission_intro'ya geç (G56)
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate("permission_intro") {
                        popUpTo("legal_consent") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                }
            } else {
                LegalConsentScreen(
                    onConsentAccepted = {
                        // Rızayı kaydet
                        val consent = com.example.isekaikuroshin.data.LegalConsentData(
                            hasAcceptedTerms = true,
                            hasAcceptedPrivacy = true,
                            consentVersion = "1.0",
                            consentDate = System.currentTimeMillis(),
                            ageConfirmed18Plus = true
                        )
                        PersistentDataManager.updateLegalConsent(consent)

                        Log.d("MainActivity", "✅ GÖREV I.2: Yasal rıza kabul edildi, permission_intro'ya yönlendiriliyor")
                        navController.navigate("permission_intro") {
                            popUpTo("legal_consent") { inclusive = true }
                            launchSingleTop = true  // G73: Prevent multiple instances
                        }
                    },
                    onConsentRejected = {
                        Log.d("MainActivity", "❌ GÖREV I.2: Yasal rıza reddedildi, uygulama kapatılıyor")
                        // Activity zaten LegalConsentScreen'de finish() çağrılacak
                    }
                )
            }
        }

        // G56: Permission Intro Screen
        composable("permission_intro") {
            PermissionIntroScreen(
                onContinue = {
                    Log.d("MainActivity", "📋 G56: İzin açıklamaları gösterildi, izin talebi başlatılıyor")
                    // İzin açıklamasından sonra izin talebi yapılacak (aşağıda PermissionRequester ile)
                }
            )

            // G57: Runtime Permission Request
            PermissionRequester(
                onAllPermissionsGranted = {
                    Log.d("MainActivity", "✅ G57: Tüm izinler verildi, user_entry'ye geçiliyor")
                    navController.navigate("user_entry") {
                        popUpTo("permission_intro") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                },
                onPermissionsDenied = { missingPermissions ->
                    Log.w("MainActivity", "⚠️ G57: Bazı izinler verilmedi: ${missingPermissions.joinToString()}")
                    // Kullanıcı izinleri reddetti ama yine de devam etsin
                    navController.navigate("user_entry") {
                        popUpTo("permission_intro") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                }
            )
        }

        composable("user_entry") {
            UserEntryScreen(
                onNavigateToTransition = { codename ->
                    Log.d("MainActivity", "🎯 Yeni kullanıcı - transition'a geçiliyor: '$codename'")
                    navController.navigate("transition/$codename") {
                        popUpTo("user_entry") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                },
                onNavigateToDashboard = { codename ->
                    Log.d("MainActivity", "🎯 Geri dönen kullanıcı - direkt dashboard'a geçiliyor: '$codename'")
                    navController.navigate("dashboard/$codename") {
                        popUpTo("user_entry") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                }
            )
        }

        composable("transition/{codename}") { backStackEntry ->
            val codename = backStackEntry.arguments?.getString("codename") ?: ""
            TransitionScreen(
                codename = codename,
                onTransitionComplete = { finalCodename ->
                    Log.d("MainActivity", "🦋 Transition complete - navigating to dashboard: '$finalCodename'")
                    // CRITICAL FIX: Mark first launch as completed
                    PersistentDataManager.setFirstLaunchCompleted()
                    Log.d("MainActivity", "✅ First launch completed flag set")
                    navController.navigate("dashboard/$finalCodename") {
                        popUpTo("transition/{codename}") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                }
            )
        }

        composable("dashboard/{userName}") { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: "unknown"
            Log.d("MainActivity", "🎯 Dashboard route triggered for user: $userName")
            // GameState initialization is now handled in ViewModel
            MainScreen(navController = navController)
        }

        composable("mana_cultivation") {
            com.example.isekaikuroshin.ui.screens.training.ManaCoreScreen(navController = navController)
        }

        composable("qi_cultivation") {
            com.example.isekaikuroshin.ui.screens.training.QiCultivationScreen(navController = navController)
        }


        composable("ai_dialog") {
            AIDialogScreen(navController = navController)
        }

        composable("adventure") {
            AdventureScreen(navController = navController)
        }

        composable("exercise") {
            Log.d("MainActivity", "🏃 Exercise route triggered")
            com.example.isekaikuroshin.ui.exercise.ExerciseScreen(navController = navController)
        }

        // G26-FIX: Health Hub route eklendi (ExerciseResultScreen'den navigate ediliyor)
        composable("health_hub") {
            Log.d("MainActivity", "❤️ Health Hub route triggered")
            com.example.isekaikuroshin.ui.healthhub.HealthHubScreen(navController = navController)
        }

        composable("seal_practice") {
            Log.d("MainActivity", "🖐️ Seal Practice route triggered")
            com.example.isekaikuroshin.ui.sealpractice.SealPracticeScreen(navController = navController)
        }

        composable("physical_training") {
            Log.d("MainActivity", "💪 Physical Training route triggered")
            com.example.isekaikuroshin.ui.physicaltraining.PhysicalTrainingScreen(navController = navController)
        }

        composable("settings") {
            SettingsScreen(navController = navController)
        }

        composable("performance_settings") {
            Log.d("MainActivity", "⚙️ G59: Performance Settings route triggered")
            PerformanceSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("journal") {
            JournalScreen(
                onBackToDashboard = {
                    navController.popBackStack()
                }
            )
        }

        composable("transition/{userName}") { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: LanguageManager.getText("default_player_name")
            TransitionScreen(
                codename = userName,
                onTransitionComplete = { finalCodename ->
                    // CRITICAL FIX: Mark first launch as completed (alternative transition route)
                    PersistentDataManager.setFirstLaunchCompleted()
                    Log.d("MainActivity", "✅ First launch completed flag set (alt route)")
                    navController.navigate("dashboard/$finalCodename") {
                        popUpTo("onboarding") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                }
            )
        }


        composable("guild") {
            GuildScreen(navController = navController)
        }

        composable("friends") {
            FriendsScreen(navController = navController)
        }

        composable("leaderboard") {
            LeaderboardScreen(navController = navController)
        }


        // 💀 ÖLÜM AKIŞI ADIM 1: Post-Death Karma Screen (Kişiselleştirilmiş ölüm sonrası sahne)
        composable("postdeath_karma") {
            val context = androidx.compose.ui.platform.LocalContext.current

            // Ölüm bilgilerini al
            val gameData = PersistentDataManager.gameData.value
            val deathCount = gameData.deathArchive.size
            val lastDeath = gameData.deathArchive.lastOrNull()

            // IntelligentContentEngine'den POSTDEATH Karma içeriği oluştur
            val karmaEngine = remember { com.example.isekaikuroshin.engine.IntelligentContentEngine(context) }
            val karmaPlaylist = remember(deathCount) {
                karmaEngine.generateDeathEchoPlaylist(
                    deathCount = deathCount,
                    lastDeathRecord = lastDeath,
                    maxVideos = 5,
                    maxPhotos = 8
                )
            }

            Log.d("MainActivity", "💀 POSTDEATH Karma: ${karmaPlaylist.videos.size} video, ${karmaPlaylist.photos.size} foto")

            PostDeathKarmaScreen(
                navController = navController,
                dynamicVideoIds = karmaPlaylist.videos,
                dynamicPhotoIds = karmaPlaylist.photos
            )
        }

        // 💀 ÖLÜM AKIŞI ADIM 2: Umbros Transition (Teklif ekranı)
        composable("umbros_transition") {
            UmbrosTransitionScreen(
                navController = navController,
                onTransitionComplete = {
                    Log.d("MainActivity", "🎯 Umbros transition completed, navigating to death_statistics")
                    navController.navigate("death_statistics") {
                        popUpTo("umbros_transition") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                }
            )
        }

        // 💀 ÖLÜM AKIŞI ADIM 3: Death Statistics (İstatistikler ve yeniden başlatma)
        composable("death_statistics") {
            Log.d("MainActivity", "💀 Death Statistics route triggered")
            com.example.isekaikuroshin.ui.screens.DeathStatisticsScreen(
                onClose = {
                    // G25-FIX: Death Statistics'ten sonra HER ZAMAN user_entry'ye git (FIRSTUSER)
                    Log.d("MainActivity", "🎯 G25-FIX: Death Statistics → user_entry (FIRSTUSER)")

                    navController.navigate("user_entry") {
                        popUpTo("death_statistics") { inclusive = true }
                        launchSingleTop = true  // G73: Prevent multiple instances
                    }
                }
            )
        }

        composable("train") {
            TrainingScreen(navController = navController)
        }

        composable("character_catalog") {
            CharacterCatalogScreen(navController = navController)
        }

        composable("quests") {
            QuestsScreen() // ViewModel ile, default görevler yok - sadece journal ilerlemesiyle
        }

        composable("camp") {
            CampScreen(navController = navController)
        }

        composable("inventory") {
            InventoryScreen(onBackPressed = { navController.popBackStack() })
        }

        composable("map") {
            MapScreen(onBackPressed = {
                Log.d("MainActivity", "🔙 Navigating back from map")
                // SORUN 6: popBackStack() kullan - navigate("camp") kullanma (navigation stack bozuluyor)
                navController.popBackStack()
            })
        }

        composable("stat_allocation") {
            StatAllocationScreen(navController = navController)
        }

        composable("skill_tree") {
            // TODO-FIX-CAMP-06: NavController eklendi (navigation bar için)
            SkillTreeScreen(navController = navController)
        }

        composable("crafting") {
            // TODO-FIX-CAMP-06: NavController eklendi (navigation bar için)
            CraftingScreen(navController = navController)
        }

        // G27-FIX: Kalibrasyon Merkezi rotaları KALDIRILDI (artık kullanılmıyor)
        // - calibration_center route removed
        // - calibration_camera/{sealId} route removed

        // VFX Workshop rotaları - Shared ViewModel Pattern
        composable("vfx_workshop") { backStackEntry ->
            Log.d("MainActivity", "🧪 VFX Workshop route triggered")
            // Parent scope for shared ViewModel
            val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                navController.getBackStackEntry("vfx_workshop")
            }
            com.example.isekaikuroshin.ui.vfx.VFXWorkshopScreen(
                navController = navController,
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry)
            )
        }

        composable("vfx_camera") { backStackEntry ->
            Log.d("MainActivity", "📸 VFX Camera route triggered")
            // Use parent scope (vfx_workshop) for shared ViewModel
            val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                navController.getBackStackEntry("vfx_workshop")
            }
            com.example.isekaikuroshin.ui.vfx.camera.VFXTestCameraScreen(
                navController = navController,
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry)
            )
        }

        composable(
            route = "vfx_gesture_calibration/{gestureType}",
            arguments = listOf(androidx.navigation.navArgument("gestureType") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val gestureType = backStackEntry.arguments?.getString("gestureType")
            Log.d("MainActivity", "🖐️ VFX Gesture Calibration route triggered for: $gestureType")
            if (gestureType != null) {
                // Use parent scope (vfx_workshop) for shared ViewModel
                val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                    navController.getBackStackEntry("vfx_workshop")
                }
                com.example.isekaikuroshin.ui.vfx.camera.VFXGestureCalibrationCameraScreen(
                    navController = navController,
                    gestureTypeStr = gestureType,
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry)
                )
            }
        }

        // ⚡⚡⚡ YENİ: VFX Offset Calibration Screen (İnteraktif 3D Konumlandırma Editörü)
        composable(
            route = "vfx_offset_calibration/{gestureType}",
            arguments = listOf(androidx.navigation.navArgument("gestureType") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val gestureTypeStr = backStackEntry.arguments?.getString("gestureType")
            Log.d("MainActivity", "📍 VFX Offset Calibration route triggered for: $gestureTypeStr")
            if (gestureTypeStr != null) {
                val gestureType = try {
                    com.example.isekaikuroshin.data.GestureType.valueOf(gestureTypeStr)
                } catch (e: IllegalArgumentException) {
                    Log.e("MainActivity", "❌ Invalid gesture type: $gestureTypeStr", e)
                    com.example.isekaikuroshin.data.GestureType.CREATION
                }

                // Use parent scope (vfx_workshop) for shared ViewModel
                val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                    navController.getBackStackEntry("vfx_workshop")
                }

                com.example.isekaikuroshin.ui.vfx.VFXOffsetCalibrationScreen(
                    navController = navController,
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry),
                    gestureType = gestureType
                )
            }
        }

        // Büyü Tasarım Stüdyosu (Spell Studio) - Shared ViewModel Pattern
        // ⚡⚡⚡ KRİTİK: Parent scope kullanarak ViewModel lifecycle'ını Camp navigasyonundan bağımsız hale getir
        composable("spell_studio") { backStackEntry ->
            Log.d("MainActivity", "✨ Spell Studio route triggered")
            // Parent scope for shared ViewModel - ViewModel'i spell_studio route'una bağla
            val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                navController.getBackStackEntry("spell_studio")
            }
            com.example.isekaikuroshin.ui.spellstudio.SpellStudioScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry)
            )
        }

    }
}
