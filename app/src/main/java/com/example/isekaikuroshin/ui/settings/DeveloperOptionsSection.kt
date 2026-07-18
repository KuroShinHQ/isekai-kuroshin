package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.Intent
import java.lang.Runtime
import kotlinx.coroutines.launch
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme
import com.example.isekaikuroshin.ui.components.OverlayData
import com.example.isekaikuroshin.engine.*
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.utils.MemoryMonitor

@Composable
fun DeveloperOptionsSection(
    settings: SettingsData,
    gameData: PersistentGameData,
    dashboardColors: DashboardColorScheme,
    navController: NavController,
    viewModel: SettingsViewModel,
    onShowOverlay: (OverlayData) -> Unit,
    onShowDiceOverlay: (DiceResult, Boolean, Boolean, Int, Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Pre-calculate localized texts for use in onClick lambdas (not @Composable context)
    val questTitle = rememberLocalizedText("new_quest_available")
    val questName = rememberLocalizedText("ancient_temple_quest")
    val questGoal1 = rememberLocalizedText("quest_goal_find_passage")
    val questGoal2 = rememberLocalizedText("quest_goal_defeat_golems")
    val questGoal3 = rememberLocalizedText("quest_goal_activate_crystal")
    val questGoal4 = rememberLocalizedText("quest_goal_collect_treasure")
    val itemTitle = rememberLocalizedText("legendary_item_acquired")
    val itemName = rememberLocalizedText("dragon_sword_item")
    val itemDesc = rememberLocalizedText("dragon_sword_desc")
    val alertTitle = rememberLocalizedText("critical_warning")
    val alertMessage = rememberLocalizedText("security_breach_message")

    // Debug Settings
    SettingsCategory(
        title = rememberLocalizedText("debug_settings"),
        icon = Icons.Default.BugReport,
        dashboardColors = dashboardColors
    ) {
        SwitchSetting(
            title = rememberLocalizedText("show_inventory_debug"),
            subtitle = rememberLocalizedText("show_inventory_debug_desc"),
            checked = settings.debugSettings.showInventoryDebugFlag,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        debugSettings = settingsData.debugSettings.copy(
                            showInventoryDebugFlag = enabled
                        )
                    )
                }
            }
        )

        SwitchSetting(
            title = rememberLocalizedText("performance_metrics"),
            subtitle = rememberLocalizedText("performance_metrics_desc"),
            checked = settings.debugSettings.showPerformanceMetrics,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        debugSettings = settingsData.debugSettings.copy(
                            showPerformanceMetrics = enabled
                        )
                    )
                }
            }
        )

        SwitchSetting(
            title = rememberLocalizedText("detailed_logging"),
            subtitle = rememberLocalizedText("detailed_logging_desc"),
            checked = settings.debugSettings.enableDetailedLogging,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        debugSettings = settingsData.debugSettings.copy(
                            enableDetailedLogging = enabled
                        )
                    )
                }
            }
        )

        // System Overlay Test Buttons
        ButtonSetting(
            title = rememberLocalizedText("show_random_quest_notification"),
            subtitle = rememberLocalizedText("test_system_overlay_quest"),
            buttonText = rememberLocalizedText("test_button"),
            onClick = {
                onShowOverlay(OverlayData.Quest(
                    title = questTitle,
                    questName = questName,
                    goals = listOf(
                        questGoal1,
                        questGoal2,
                        questGoal3,
                        questGoal4
                    )
                ))
            }
        )

        ButtonSetting(
            title = rememberLocalizedText("show_random_item_acquired"),
            subtitle = rememberLocalizedText("test_system_overlay_item"),
            buttonText = rememberLocalizedText("test_button"),
            onClick = {
                onShowOverlay(OverlayData.ItemAcquired(
                    title = itemTitle,
                    itemName = itemName,
                    itemDescription = itemDesc,
                    itemIcon = R.drawable.camp, // Temporarily use an available drawable
                    rarity = com.example.isekaikuroshin.ui.components.CardRarity.LEGENDARY
                ))
            }
        )

        ButtonSetting(
            title = rememberLocalizedText("show_system_alert_notification"),
            subtitle = rememberLocalizedText("test_system_overlay_alert"),
            buttonText = rememberLocalizedText("test_button"),
            onClick = {
                onShowOverlay(OverlayData.Alert(
                    title = alertTitle,
                    message = alertMessage
                ))
            }
        )

        ButtonSetting(
            title = rememberLocalizedText("director_engine_dice_test"),
            subtitle = rememberLocalizedText("test_perception_and_stat_effect"),
            buttonText = rememberLocalizedText("dice_test_button"),
            onClick = {
                viewModel.runDirectorEngineTest()
            }
        )

        ButtonSetting(
            title = rememberLocalizedText("action_executor_engine_test"),
            subtitle = rememberLocalizedText("test_ai_actions_to_gamestate"),
            buttonText = rememberLocalizedText("action_test_button"),
            onClick = {
                viewModel.runActionExecutorTest()
            }
        )

        // STRATEJI #3: Local AI Model Test
        ButtonSetting(
            title = rememberLocalizedText("local_ai_model_test"),
            subtitle = rememberLocalizedText("test_mediaipe_gemma_working"),
            buttonText = rememberLocalizedText("ai_test_button"),
            onClick = {
                coroutineScope.launch {
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("🤖 LOCAL AI MODEL TEST BAŞLATILDI")
                    GameLogger.logSystem("════════════════════════════════════════════════════")

                    try {
                        // Model durumunu kontrol et
                        val isModelReady = com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value
                        GameLogger.logSystem("📊 Model Status:")
                        GameLogger.logSystem("   - Model initialized: $isModelReady")

                        if (!isModelReady) {
                            GameLogger.logSystem("⚠️ Model henüz yüklenmedi - Yükleme başlatılıyor...")
                            com.example.isekaikuroshin.ai.GlobalAIManager.startAILoading()

                            // 5 saniye bekle
                            GameLogger.logSystem("⏳ Model yüklemesi için 5 saniye bekleniyor...")
                            kotlinx.coroutines.delay(5000)

                            val isReadyNow = com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value
                            GameLogger.logSystem("   - Model ready after wait: $isReadyNow")

                            if (!isReadyNow) {
                                GameLogger.logError("LocalAITest", "Model 5 saniye sonra hala yüklenmedi", null)
                                GameLogger.logSystem("❌ TEST BAŞARISIZ - Model yüklenemedi")
                                GameLogger.logSystem("════════════════════════════════════════════════════")
                                return@launch
                            }
                        }

                        // Test prompt'u
                        val testPrompt = "Write a single short sentence about a brave warrior."
                        GameLogger.logSystem("📝 Test Prompt:")
                        GameLogger.logSystem("   \"$testPrompt\"")

                        // AI'dan yanıt al
                        val startTime = System.currentTimeMillis()
                        val response = com.example.isekaikuroshin.ai.GlobalAIManager.generateResponse(testPrompt)
                        val endTime = System.currentTimeMillis()
                        val latency = endTime - startTime

                        GameLogger.logSystem("✅ AI RESPONSE RECEIVED:")
                        GameLogger.logSystem("   - Latency: ${latency}ms")
                        GameLogger.logSystem("   - Response length: ${response.length} chars")
                        GameLogger.logSystem("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        GameLogger.logSystem("📄 RESPONSE TEXT:")
                        GameLogger.logSystem(response)
                        GameLogger.logSystem("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                        if (response.isNotBlank() && !response.contains("model yükleniyor", ignoreCase = true)) {
                            GameLogger.logSystem("✅ LOCAL AI MODEL TEST BAŞARILI!")
                            GameLogger.logSystem("   Model çalışıyor ve yanıt üretiyor")
                        } else {
                            GameLogger.logSystem("⚠️ Model yanıt verdi ama fallback mesajı olabilir")
                        }

                    } catch (e: Exception) {
                        GameLogger.logError("LocalAITest", "Test sırasında hata oluştu", e)
                        GameLogger.logSystem("❌ TEST BAŞARISIZ - Exception: ${e.message}")
                    }

                    GameLogger.logSystem("════════════════════════════════════════════════════")
                }
            }
        )

        // 🆕 DYNAMIC LAUNCHER ICON TEST
        val context = LocalContext.current

        Text(
            text = "🎯 Dynamic Launcher Icon Test",
            style = MaterialTheme.typography.titleSmall,
            color = dashboardColors.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // DIVINE Icon Button
            Button(
                onClick = {
                    AppIconManager.switchIcon(context, AppIconManager.IconType.DIVINE)
                    GameLogger.logSystem("🌟 Icon değiştirildi: DIVINE")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                )
            ) {
                Text("DIVINE", color = Color.Black)
            }

            // DARK Icon Button
            Button(
                onClick = {
                    AppIconManager.switchIcon(context, AppIconManager.IconType.DARK)
                    GameLogger.logSystem("😈 Icon değiştirildi: DARK")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B0000)
                )
            ) {
                Text("DARK", color = Color.White)
            }

            // MYSTERY Icon Button
            Button(
                onClick = {
                    AppIconManager.switchIcon(context, AppIconManager.IconType.MYSTERY)
                    GameLogger.logSystem("🔮 Icon değiştirildi: MYSTERY")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9370DB)
                )
            ) {
                Text("MYSTERY", color = Color.White)
            }
        }

        Text(
            text = "⚠️ Icon değişimi launcher'da 2-5 saniye içinde görünür.",
            style = MaterialTheme.typography.bodySmall,
            color = dashboardColors.secondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        // Translation Quality Checker
        var showTranslationReport by remember { mutableStateOf(false) }
        var translationReport by remember { mutableStateOf("") }

        ButtonSetting(
            title = rememberLocalizedText("check_translation_quality"),
            subtitle = rememberLocalizedText("scan_for_translation_issues"),
            buttonText = rememberLocalizedText("check_button"),
            onClick = {
                coroutineScope.launch {
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("🌍 TRANSLATION QUALITY CHECK STARTED")
                    GameLogger.logSystem("════════════════════════════════════════════════════")

                    try {
                        // Run translation checker
                        val issues = TranslationChecker.checkAllTranslations()
                        val report = TranslationChecker.generateReport(issues)

                        // Log to console
                        GameLogger.logSystem(report)

                        // Show in dialog
                        translationReport = report
                        showTranslationReport = true

                        GameLogger.logSystem("✅ TRANSLATION CHECK COMPLETE")
                        GameLogger.logSystem("   Total issues found: ${issues.size}")
                    } catch (e: Exception) {
                        GameLogger.logError("TranslationCheck", "Error during translation check", e)
                        translationReport = "❌ ERROR: ${e.message}"
                        showTranslationReport = true
                    }

                    GameLogger.logSystem("════════════════════════════════════════════════════")
                }
            }
        )

        // Translation Report Dialog
        if (showTranslationReport) {
            TranslationReportDialog(
                report = translationReport,
                onDismiss = { showTranslationReport = false }
            )
        }

        // Translation test removed - Hybrid AI architecture doesn't use translation layer
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ═══════════════════════════════════════════════════════
    // 🚁 DRONE SYSTEM TESTS (TODO-D4.4)
    // ═══════════════════════════════════════════════════════
    SettingsCategory(
        title = rememberLocalizedText("drone_tests_category"),
        icon = Icons.Default.FlightTakeoff,
        dashboardColors = dashboardColors
    ) {
        // Test 1: TelemetryParser Test
        ButtonSetting(
            title = rememberLocalizedText("test_telemetry_parser"),
            subtitle = rememberLocalizedText("test_telemetry_parser_desc"),
            buttonText = rememberLocalizedText("test_button"),
            onClick = {
                coroutineScope.launch {
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("🧪 TELEMETRY PARSER TEST")
                    GameLogger.logSystem("════════════════════════════════════════════════════")

                    val testPackets = listOf(
                        "P1:85,P2:72,T:45,S:92,M:REFLEKS",
                        "P1:100,P2:100,T:0,S:100,M:SINEMATIK",
                        "P1:20,P2:15,T:80,S:50,M:REFLEKS",
                        "P1:INVALID,P2:90,T:50,S:95,M:TEST" // Invalid packet
                    )

                    var passedTests = 0
                    var failedTests = 0

                    testPackets.forEachIndexed { index, packet ->
                        GameLogger.logSystem("📦 Test ${index + 1}: $packet")
                        val result = com.example.isekaikuroshin.ble.TelemetryParser.parse(packet)

                        if (result != null) {
                            GameLogger.logSystem("   ✅ PASS - Parsed successfully")
                            GameLogger.logSystem("      Battery: ${result.gauntletBattery}%, ${result.droneBattery}%")
                            GameLogger.logSystem("      Throttle: ${result.throttle}%")
                            GameLogger.logSystem("      Signal: ${result.signalRssi}%")
                            GameLogger.logSystem("      Mode: ${result.flightMode}")
                            passedTests++
                        } else {
                            GameLogger.logSystem("   ❌ FAIL - Parse error (expected for invalid packet)")
                            failedTests++
                        }
                    }

                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("📊 RESULTS: $passedTests passed, $failedTests failed")
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                }
            }
        )

        // Test 2: DroneState Update Test
        ButtonSetting(
            title = rememberLocalizedText("test_drone_state"),
            subtitle = rememberLocalizedText("test_drone_state_desc"),
            buttonText = rememberLocalizedText("test_button"),
            onClick = {
                coroutineScope.launch {
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("🧪 DRONE STATE UPDATE TEST")
                    GameLogger.logSystem("════════════════════════════════════════════════════")

                    // Test 1: Full state update
                    GameLogger.logSystem("📝 Test 1: Full state update")
                    val testState = com.example.isekaikuroshin.data.DroneState(
                        gauntletBattery = 75,
                        droneBattery = 80,
                        throttle = 50,
                        signalRssi = 95,
                        flightMode = com.example.isekaikuroshin.data.FlightMode.CINEMATIC,
                        isConnected = true
                    )
                    com.example.isekaikuroshin.data.DroneStateManager.updateState(testState)
                    kotlinx.coroutines.delay(100)

                    val currentState = com.example.isekaikuroshin.data.DroneStateManager.state.value
                    GameLogger.logSystem("   ✅ State updated")
                    GameLogger.logSystem("      Battery: ${currentState.gauntletBattery}%, ${currentState.droneBattery}%")
                    GameLogger.logSystem("      Throttle: ${currentState.throttle}%")
                    GameLogger.logSystem("      Signal: ${currentState.signalRssi}%")
                    GameLogger.logSystem("      Mode: ${currentState.flightMode}")
                    GameLogger.logSystem("      Connected: ${currentState.isConnected}")

                    // Test 2: Partial update
                    GameLogger.logSystem("📝 Test 2: Partial battery update")
                    com.example.isekaikuroshin.data.DroneStateManager.updateBattery(60, 55)
                    kotlinx.coroutines.delay(100)

                    val updatedState = com.example.isekaikuroshin.data.DroneStateManager.state.value
                    GameLogger.logSystem("   ✅ Battery updated: ${updatedState.gauntletBattery}%, ${updatedState.droneBattery}%")

                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("📊 RESULT: DroneState working correctly!")
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                }
            }
        )

        // Test 3: BLE Command Encoding Test
        ButtonSetting(
            title = rememberLocalizedText("test_ble_commands"),
            subtitle = rememberLocalizedText("test_ble_commands_desc"),
            buttonText = rememberLocalizedText("test_button"),
            onClick = {
                coroutineScope.launch {
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("🧪 BLE COMMAND ENCODING TEST")
                    GameLogger.logSystem("════════════════════════════════════════════════════")

                    val testCommands = listOf(
                        com.example.isekaikuroshin.ble.DroneCommand.SAFE_ARM,
                        com.example.isekaikuroshin.ble.DroneCommand.SMART_LANDING,
                        com.example.isekaikuroshin.ble.DroneCommand.CRUISE_TOGGLE,
                        com.example.isekaikuroshin.ble.DroneCommand.MODE_REFLEX,
                        com.example.isekaikuroshin.ble.DroneCommand.MODE_CINEMATIC,
                        com.example.isekaikuroshin.ble.DroneCommand.FLIP_FORWARD,
                        com.example.isekaikuroshin.ble.DroneCommand.COMPASS_CALIBRATION,
                        com.example.isekaikuroshin.ble.DroneCommand.SMART_CATCH
                    )

                    testCommands.forEach { command ->
                        val encoded = command.encode()
                        val encodedHex = encoded.joinToString(" ") { "0x%02X".format(it) }

                        GameLogger.logSystem("📤 Command: $command")
                        GameLogger.logSystem("   ID: 0x${command.commandId.toString(16).uppercase()}")
                        GameLogger.logSystem("   Encoded: $encodedHex")
                        GameLogger.logSystem("   Description: ${command.description}")

                        // Test send command
                        val result = com.example.isekaikuroshin.ble.BleManager.sendCommand(command)
                        GameLogger.logSystem("   Result: $result")
                    }

                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("📊 RESULT: All ${testCommands.size} commands encoded successfully!")
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                }
            }
        )

        // Test 4: Full System Integration Test
        ButtonSetting(
            title = rememberLocalizedText("test_drone_full_system"),
            subtitle = rememberLocalizedText("test_drone_full_system_desc"),
            buttonText = rememberLocalizedText("test_button"),
            onClick = {
                coroutineScope.launch {
                    GameLogger.logSystem("════════════════════════════════════════════════════")
                    GameLogger.logSystem("🧪 FULL DRONE SYSTEM INTEGRATION TEST")
                    GameLogger.logSystem("════════════════════════════════════════════════════")

                    // Step 1: Parse telemetry
                    GameLogger.logSystem("📝 Step 1: Parse mock telemetry")
                    // Mock packet (generateMockPacket kaldırıldı - manuel oluşturuyoruz)
                    val mockPacket = "P1:85,P2:90,T:50,S:95,M:REFLEKS"
                    GameLogger.logSystem("   Generated packet: $mockPacket")

                    val parsedState = com.example.isekaikuroshin.ble.TelemetryParser.parse(mockPacket)
                    if (parsedState != null) {
                        GameLogger.logSystem("   ✅ Parse successful")

                        // Step 2: Update DroneState
                        GameLogger.logSystem("📝 Step 2: Update DroneState")
                        com.example.isekaikuroshin.data.DroneStateManager.updateState(parsedState)
                        kotlinx.coroutines.delay(100)
                        GameLogger.logSystem("   ✅ State updated")

                        // Step 3: Verify UI state
                        GameLogger.logSystem("📝 Step 3: Verify UI state (check TelemetryPanel)")
                        val currentState = com.example.isekaikuroshin.data.DroneStateManager.state.value
                        GameLogger.logSystem("   Battery: ${currentState.gauntletBattery}%, ${currentState.droneBattery}%")
                        GameLogger.logSystem("   Throttle: ${currentState.throttle}%")
                        GameLogger.logSystem("   Signal: ${currentState.signalRssi}%")
                        GameLogger.logSystem("   Mode: ${currentState.flightMode}")
                        GameLogger.logSystem("   ✅ UI should reflect these values")

                        // Step 4: Test command
                        GameLogger.logSystem("📝 Step 4: Send test command")
                        val testCommand = com.example.isekaikuroshin.ble.DroneCommand.SAFE_ARM
                        val result = com.example.isekaikuroshin.ble.BleManager.sendCommand(testCommand)
                        GameLogger.logSystem("   Command: $testCommand")
                        GameLogger.logSystem("   Result: $result")
                        GameLogger.logSystem("   ✅ Command sent")

                        GameLogger.logSystem("════════════════════════════════════════════════════")
                        GameLogger.logSystem("✅ FULL SYSTEM TEST PASSED!")
                        GameLogger.logSystem("   All components working:")
                        GameLogger.logSystem("   ✓ TelemetryParser")
                        GameLogger.logSystem("   ✓ DroneState")
                        GameLogger.logSystem("   ✓ BleManager (mock)")
                        GameLogger.logSystem("   ✓ UI Integration")
                        GameLogger.logSystem("════════════════════════════════════════════════════")
                    } else {
                        GameLogger.logSystem("   ❌ Parse failed")
                        GameLogger.logSystem("════════════════════════════════════════════════════")
                        GameLogger.logSystem("❌ TEST FAILED at Step 1")
                        GameLogger.logSystem("════════════════════════════════════════════════════")
                    }
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Dice Testing System
    SettingsCategory(
        title = rememberLocalizedText("dice_system_tests"),
        icon = Icons.Default.Casino,
        dashboardColors = dashboardColors
    ) {
        ButtonSetting(
            title = rememberLocalizedText("normal_dice_roll"),
            subtitle = rememberLocalizedText("standard_1d20_test"),
            buttonText = rememberLocalizedText("normal_button"),
            onClick = {
                GameLogger.logSystem("🎲 ZAR TEST BAŞLADI - Normal zar atışı testi")
                val context = DiceContext(
                    season = Season.SPRING,
                    weather = Weather.SUNNY,
                    timeOfDay = com.example.isekaikuroshin.data.TimeOfDay.MORNING,
                    locationDanger = 3,
                    healthPercentage = 1.0f,
                    manaPercentage = 0.8f,
                    staminaPercentage = 0.9f,
                    hungerLevel = 0.8f,
                    fatigueLevel = 0.2f,
                    moodState = MoodState.CONFIDENT,
                    luck = 50L,
                    intelligence = 60L,
                    charisma = 45L,
                    perception = 55L,
                    npcRelationship = 0.3f,
                    reputationLevel = 25,
                    actionType = DiceActionType.COMBAT_ATTACK,
                    difficulty = 12,
                    hasAdvantage = false,
                    hasDisadvantage = false,
                    equipmentBonus = 2,
                    magicalEnhancement = 1.0f
                )
                GameLogger.logSystem("🎲 DiceContext oluşturuldu, DiceEngine.calculateComplexRoll çağırılıyor...")
                val diceResult = DiceEngine.calculateComplexRoll(context)
                GameLogger.logSystem("🎲 DiceEngine sonucu: ${diceResult.toString()}")
                run {
                    val result = diceResult
                    val bonusModifier = result.modifiedRoll - result.baseRoll
                    onShowDiceOverlay(result, false, false, 12, bonusModifier)
                }
            }
        )

        ButtonSetting(
            title = rememberLocalizedText("advantage_dice_roll"),
            subtitle = rememberLocalizedText("advantage_2d20_high"),
            buttonText = rememberLocalizedText("advantage_button"),
            onClick = {
                val context = DiceContext(
                    season = Season.SUMMER,
                    weather = Weather.SUNNY,
                    timeOfDay = com.example.isekaikuroshin.data.TimeOfDay.NOON,
                    locationDanger = 2,
                    healthPercentage = 1.0f,
                    manaPercentage = 0.9f,
                    staminaPercentage = 1.0f,
                    hungerLevel = 0.9f,
                    fatigueLevel = 0.1f,
                    moodState = MoodState.CONFIDENT,
                    luck = 70L,
                    intelligence = 65L,
                    charisma = 60L,
                    perception = 58L,
                    npcRelationship = 0.5f,
                    reputationLevel = 40,
                    actionType = DiceActionType.SOCIAL_PERSUASION,
                    difficulty = 15,
                    hasAdvantage = true,
                    hasDisadvantage = false,
                    equipmentBonus = 3,
                    magicalEnhancement = 1.2f
                )
                val diceResult = DiceEngine.calculateComplexRoll(context)
                run {
                    val result = diceResult
                    val bonusModifier = result.modifiedRoll - result.baseRoll
                    onShowDiceOverlay(result, true, false, 15, bonusModifier)
                }
            }
        )

        ButtonSetting(
            title = rememberLocalizedText("disadvantage_dice_roll"),
            subtitle = rememberLocalizedText("disadvantage_2d20_low"),
            buttonText = rememberLocalizedText("disadvantage_button"),
            onClick = {
                val context = DiceContext(
                    season = Season.WINTER,
                    weather = Weather.STORMY,
                    timeOfDay = com.example.isekaikuroshin.data.TimeOfDay.NIGHT,
                    locationDanger = 7,
                    healthPercentage = 0.4f,
                    manaPercentage = 0.3f,
                    staminaPercentage = 0.3f,
                    hungerLevel = 0.4f,
                    fatigueLevel = 0.7f,
                    moodState = MoodState.ANXIOUS,
                    luck = 25L,
                    intelligence = 40L,
                    charisma = 30L,
                    perception = 35L,
                    npcRelationship = -0.2f,
                    reputationLevel = -10,
                    actionType = DiceActionType.EXPLORATION_STEALTH,
                    difficulty = 10,
                    hasAdvantage = false,
                    hasDisadvantage = true,
                    equipmentBonus = -1,
                    magicalEnhancement = 0.8f
                )
                val diceResult = DiceEngine.calculateComplexRoll(context)
                run {
                    val result = diceResult
                    val bonusModifier = result.modifiedRoll - result.baseRoll
                    onShowDiceOverlay(result, false, true, 10, bonusModifier)
                }
            }
        )

        ButtonSetting(
            title = rememberLocalizedText("high_difficulty_test"),
            subtitle = rememberLocalizedText("dc18_difficult_test"),
            buttonText = rememberLocalizedText("difficult_button"),
            onClick = {
                val context = DiceContext(
                    season = Season.AUTUMN,
                    weather = Weather.FOGGY,
                    timeOfDay = com.example.isekaikuroshin.data.TimeOfDay.NIGHT,
                    locationDanger = 8,
                    healthPercentage = 0.6f,
                    manaPercentage = 0.5f,
                    staminaPercentage = 0.6f,
                    hungerLevel = 0.6f,
                    fatigueLevel = 0.5f,
                    moodState = MoodState.NEUTRAL,
                    luck = 40L,
                    intelligence = 80L,
                    charisma = 50L,
                    perception = 75L,
                    npcRelationship = 0.0f,
                    reputationLevel = 10,
                    actionType = DiceActionType.PUZZLE_SOLVING,
                    difficulty = 18,
                    hasAdvantage = false,
                    hasDisadvantage = false,
                    equipmentBonus = 5,
                    magicalEnhancement = 1.1f
                )
                val diceResult = DiceEngine.calculateComplexRoll(context)
                run {
                    val result = diceResult
                    val bonusModifier = result.modifiedRoll - result.baseRoll
                    onShowDiceOverlay(result, false, false, 18, bonusModifier)
                }
            }
        )

        ButtonSetting(
            title = rememberLocalizedText("lucky_dice_test"),
            subtitle = rememberLocalizedText("high_modifier_easy_success"),
            buttonText = rememberLocalizedText("lucky_button"),
            onClick = {
                val context = DiceContext(
                    season = Season.SPRING,
                    weather = Weather.SUNNY,
                    timeOfDay = com.example.isekaikuroshin.data.TimeOfDay.MORNING,
                    locationDanger = 1,
                    healthPercentage = 1.0f,
                    manaPercentage = 1.0f,
                    staminaPercentage = 1.0f,
                    hungerLevel = 1.0f,
                    fatigueLevel = 0.0f,
                    moodState = MoodState.EXCITED,
                    luck = 100L,
                    intelligence = 90L,
                    charisma = 85L,
                    perception = 80L,
                    npcRelationship = 0.8f,
                    reputationLevel = 80,
                    actionType = DiceActionType.MAGIC_CASTING,
                    difficulty = 12,
                    hasAdvantage = true,
                    hasDisadvantage = false,
                    equipmentBonus = 8,
                    magicalEnhancement = 1.5f,
                    activeBuffs = listOf("blessed", "haste", "wisdom"),
                    recentSuccesses = 3
                )
                val diceResult = DiceEngine.calculateComplexRoll(context)
                run {
                    val result = diceResult
                    val bonusModifier = result.modifiedRoll - result.baseRoll
                    onShowDiceOverlay(result, true, false, 12, bonusModifier)
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Data Management
    SettingsCategory(
        title = rememberLocalizedText("data_management"),
        icon = Icons.Default.Storage,
        dashboardColors = dashboardColors
    ) {
        var showExportDialog by remember { mutableStateOf(false) }
        var showImportDialog by remember { mutableStateOf(false) }
        var showResetDialog by remember { mutableStateOf(false) }
        var showLoadTestDataDialog by remember { mutableStateOf(false) }
        var showDeathTestDialog by remember { mutableStateOf(false) }

        ButtonSetting(
            title = rememberLocalizedText("export_data"),
            subtitle = rememberLocalizedText("export_data_desc"),
            buttonText = rememberLocalizedText("export"),
            onClick = { showExportDialog = true }
        )

        ButtonSetting(
            title = rememberLocalizedText("import_data"),
            subtitle = rememberLocalizedText("import_data_desc"),
            buttonText = rememberLocalizedText("import"),
            onClick = { showImportDialog = true }
        )

        ButtonSetting(
            title = rememberLocalizedText("reset_all_data"),
            subtitle = rememberLocalizedText("reset_all_data_desc"),
            buttonText = rememberLocalizedText("reset"),
            onClick = { showResetDialog = true }
        )

        var showResetGameDialog by remember { mutableStateOf(false) }

        ButtonSetting(
            title = rememberLocalizedText("reset_game_only"),
            subtitle = rememberLocalizedText("reset_game_only_desc"),
            buttonText = rememberLocalizedText("reset_game_button"),
            onClick = { showResetGameDialog = true }
        )

        if (showResetGameDialog) {
            ResetGameOnlyDialog(
                onDismiss = { showResetGameDialog = false },
                onConfirm = {
                    coroutineScope.launch {
                        GameLogger.logSystem("🎮 SADECE OYUN SIFIRLAMA BAŞLADI - GameState temizleniyor")
                        viewModel.resetGame()
                        GameLogger.logSystem("✅ OYUN SIFIRLAMA TAMAMLANDI")
                        showResetGameDialog = false
                    }
                }
            )
        }

        ButtonSetting(
            title = rememberLocalizedText("load_test_data"),
            subtitle = rememberLocalizedText("load_test_data_desc"),
            buttonText = rememberLocalizedText("load_button"),
            onClick = { showLoadTestDataDialog = true }
        )

        ButtonSetting(
            title = rememberLocalizedText("test_death_state"),
            subtitle = rememberLocalizedText("test_death_state_desc"),
            buttonText = rememberLocalizedText("test_death_button"),
            onClick = { showDeathTestDialog = true }
        )

        // Death Archive
        if (gameData.deathArchive.isNotEmpty()) {
            ButtonSetting(
                title = rememberLocalizedText("death_archive"),
                subtitle = "${gameData.deathArchive.size} ${rememberLocalizedText("fallen_heroes")}",
                buttonText = rememberLocalizedText("view"),
                onClick = {
                    navController.navigate("death_archive")
                }
            )
        }

        // Dialogs
        if (showExportDialog) {
            ExportDataDialog(
                data = PersistentDataManager.exportData(),
                onDismiss = { showExportDialog = false }
            )
        }

        if (showImportDialog) {
            ImportDataDialog(
                onDismiss = { showImportDialog = false },
                onImport = { jsonData ->
                    PersistentDataManager.importData(jsonData)
                    showImportDialog = false
                }
            )
        }

        if (showResetDialog) {
            val context = LocalContext.current
            ResetDataDialog(
                onDismiss = { showResetDialog = false },
                onConfirm = {
                    coroutineScope.launch {
                        GameLogger.logSystem("🔄 VERİ SIFIRLAMA BAŞLADI - Tüm oyun verileri ve kalıcı ayarlar silinecek")

                        // Önce GameStateManager'ı sıfırla (SUSPEND - await edilir)
                        GameLogger.logSystem("🎮 GameStateManager sıfırlanıyor...")
                        viewModel.resetGame()

                        // Sonra PersistentDataManager'ı sıfırla (isFirstLaunch bayrağı dahil)
                        GameLogger.logSystem("💾 PersistentDataManager sıfırlanıyor (isFirstLaunch bayrağı dahil)...")
                        PersistentDataManager.resetAllData()

                        GameLogger.logSystem("✅ VERİ SIFIRLAMA TAMAMLANDI - Uygulama yeniden başlatılıyor...")
                        showResetDialog = false

                        // Uygulamayı yeniden başlat
                        val packageManager = context.packageManager
                        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                        val componentName = intent!!.component
                        val mainIntent = Intent.makeRestartActivityTask(componentName)
                        context.startActivity(mainIntent)
                        Runtime.getRuntime().exit(0)
                    }
                }
            )
        }

        if (showLoadTestDataDialog) {
            LoadTestDataDialog(
                onDismiss = { showLoadTestDataDialog = false },
                onConfirm = {
                    viewModel.loadTestData()
                    showLoadTestDataDialog = false
                }
            )
        }

        if (showDeathTestDialog) {
            DeathTestDialog(
                onDismiss = { showDeathTestDialog = false },
                onConfirm = {
                    GameLogger.logSystem("💀 DEBUG: ÖLÜM TESTİ başlatıldı - Oyuncu ölü olarak işaretleniyor")

                    coroutineScope.launch {
                        // DEATH-LOOP-FIX: Önce ölümü kaydet (DeathArchive'e ekle)
                        PersistentDataManager.recordDeath(
                            deathCause = "DEBUG_TEST",
                            wasUmbrosOfferAccepted = false, // Debug test için false
                            screenshotPath = null
                        )
                        GameLogger.logSystem("💀 DEBUG: Ölüm kaydı DeathArchive'e eklendi")

                        // Oyuncuyu ölü olarak işaretle (recordDeath zaten bunu yapıyor ama açıkça belirtelim)
                        PersistentDataManager.updatePlayerData { playerData ->
                            playerData.copy(isAlive = false)
                        }

                        // FIX #41: ÖLÜM AKIŞI DÜZELTME - DOĞRU AKIŞ: postdeath_karma → umbros_transition → death_statistics
                        GameLogger.logSystem("💀 DEBUG: Oyuncu ölü olarak işaretlendi, postdeath_karma ekranına yönlendiriliyor (Ölüm animasyonu)")
                        showDeathTestDialog = false

                        // PostDeath Karma (Ölüm animasyonu) ekranına yönlendir
                        navController.navigate("postdeath_karma") {
                            popUpTo("settings") { inclusive = false }
                        }
                    }
                }
            )
        }
    }

    // G58: Memory Profiling Stats
    val context = LocalContext.current
    val memoryStats by MemoryMonitor.memoryStats.collectAsState()

    // Start monitoring when Developer Mode is enabled
    LaunchedEffect(Unit) {
        MemoryMonitor.startMonitoring(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            MemoryMonitor.stopMonitoring()
        }
    }

    SettingsCategory(
        title = "Memory Profiling",
        icon = Icons.Default.Memory,
        dashboardColors = dashboardColors
    ) {
        // Memory Stats Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = dashboardColors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📊 Real-Time Memory Stats",
                    style = MaterialTheme.typography.titleSmall,
                    color = dashboardColors.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Heap Memory
                Text(
                    text = "Heap: ${String.format("%.1f", memoryStats.heapUsedMB)}MB / ${String.format("%.1f", memoryStats.heapMaxMB)}MB",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                // Native Memory
                Text(
                    text = "Native: ${String.format("%.1f", memoryStats.nativeAllocatedMB)}MB",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                // PSS Total
                val pssColor = when (memoryStats.memoryPressureLevel) {
                    MemoryMonitor.MemoryPressure.CRITICAL -> Color.Red
                    MemoryMonitor.MemoryPressure.HIGH -> Color(0xFFFF9800)
                    MemoryMonitor.MemoryPressure.MODERATE -> Color(0xFFFFEB3B)
                    MemoryMonitor.MemoryPressure.NORMAL -> Color.Green
                }

                Text(
                    text = "PSS: ${String.format("%.1f", memoryStats.pssTotalMB)}MB (${memoryStats.memoryPressureLevel})",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = pssColor,
                    fontWeight = FontWeight.Bold
                )

                // Available System
                Text(
                    text = "Available: ${String.format("%.1f", memoryStats.availableSystemMemoryMB)}MB",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Force GC Button
                Button(
                    onClick = {
                        MemoryMonitor.forceGarbageCollection()
                        MemoryMonitor.logMemorySnapshot(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("♻️ Force Garbage Collection")
                }
            }
        }
    }
}

@Composable
fun LoadTestDataDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Test Verilerini Yükle") },
        text = { Text("Mevcut oyun durumunuzun üzerine yazılacaktır. Emin misiniz?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yükle")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(rememberLocalizedText("cancel_button"))
            }
        }
    )
}

@Composable
fun DeathTestDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(rememberLocalizedText("test_death_title"))
        },
        text = {
            Text(rememberLocalizedText("test_death_message"))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(rememberLocalizedText("test_death_confirm"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(rememberLocalizedText("cancel_button"))
            }
        }
    )
}

@Composable
fun TranslationReportDialog(report: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Translation Quality Report")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = report,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ResetGameOnlyDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(rememberLocalizedText("reset_game_completely"))
        },
        text = {
            Text(
                "Bu işlem:\n" +
                "✓ Tüm oyun ilerlemesini sıfırlar\n" +
                "✓ Journal sayfalarını temizler\n" +
                "✓ Envanter ve stat'ları başlangıca döndürür\n" +
                "✓ Görevleri siler\n\n" +
                "⚠️ Ayarlar ve tercihleriniz KORUNUR\n\n" +
                "Bu işlem geri alınamaz. Emin misiniz?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.Red)
            ) {
                Text("OYUNU SIFIRLA", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(rememberLocalizedText("cancel_button"))
            }
        }
    )
}