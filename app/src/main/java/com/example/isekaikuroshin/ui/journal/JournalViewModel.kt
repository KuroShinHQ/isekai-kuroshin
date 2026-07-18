package com.example.isekaikuroshin.ui.journal

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.engine.*
import com.example.isekaikuroshin.engine.combat.RealityEngine
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.utils.NetworkMonitor
import com.example.isekaikuroshin.utils.AIResponseCache
import com.example.isekaikuroshin.utils.PendingSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

// Journal ekranının ihtiyaç duyduğu TÜM verileri içeren tek bir state class'ı
data class JournalUiState(
    val storyPages: List<String> = emptyList(),
    val currentDay: Int = 1,
    val currentTimeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val currentLocationString: String = "",
    val compassString: String = "",
    val playerState: PlayerState = PlayerState(),
    val criticalWarnings: List<String> = emptyList(),
    val resources: ResourcesZ4? = null,
    val collectedItems: CollectedItemsZ5? = null,
    val inputCountThisTimeOfDay: Int = 0,
    val isLoadingAIResponse: Boolean = false,
    // Savaş durumu için yeni alanlar
    val inCombat: Boolean = false,
    val currentEnemies: List<Enemy> = emptyList(),
    // Hava durumu bilgisi
    val currentWeather: Weather = Weather.SUNNY,
    // STRATEJI #3: Memory synthesis loading
    val isSynthesizingMemory: Boolean = false,
    val memorySynthesisStatus: String = "",
    // GÖREV I: Mikro geri bildirimler
    val microFeedbacks: List<com.example.isekaikuroshin.ui.components.MicroFeedbackItem> = emptyList(),
    // GÖREV N - FAZ 4: Öğrenilmiş büyü listesi
    val learnedSpells: List<com.example.isekaikuroshin.data.combat.LearnedSpell> = emptyList(),
    // GÖREV #19-21: Journal Overlay/Notification Sistemi
    val currentOverlay: com.example.isekaikuroshin.ui.components.OverlayData? = null,
    val isOverlayVisible: Boolean = false
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val gameStateManager: com.example.isekaikuroshin.data.GameStateManager,
    private val gameMasterEngine: GameMasterEngine,
    @ApplicationContext private val context: Context,
    // HATA #4 FIX: lateinit yerine constructor injection kullan
    private val gameStateManagerForMemory: com.example.isekaikuroshin.game.GameStateManager,
    // G81: Combat system için ActionExecutorEngine inject
    private val actionExecutorEngine: ActionExecutorEngine,
    // G93: Combat state machine
    val combatStateMachine: com.example.isekaikuroshin.combat.CombatStateMachine,
    // G93: Combat controller
    private val combatController: com.example.isekaikuroshin.combat.CombatController
) : ViewModel() {

    private val basicStoryEngine = BasicStoryEngine(context, gameStateManager)
    private val actionIntentParser = ActionIntentParser

    // 🌐 Çevrimdışı Mod Desteği
    private val networkMonitor = NetworkMonitor(context)
    private val aiResponseCache = AIResponseCache(context)
    private val pendingSyncManager = PendingSyncManager(context)
    private val profileUpdaterEngine = ProfileUpdaterEngine
    private val classQuestEngine = ClassQuestEngine

    // UI'ın dinleyeceği tek bir StateFlow
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    val userInput = mutableStateOf("")
    private var lastTimeOfDay: TimeOfDay = TimeOfDay.MORNING

    init {
        // G141: Removed verbose init log (spam azaltma)

        viewModelScope.launch {
            // G141: Removed verbose coroutine log (spam azaltma)
            gameStateManager.gameState.collectLatest { stateValue ->
                // G141: Removed verbose gameState update log (her frame spam!)

                // Girdi sayacını sadece zaman dilimi değiştiğinde sıfırla
                if (lastTimeOfDay != stateValue.currentTimeOfDay) {
                    _uiState.update { it.copy(inputCountThisTimeOfDay = 0) }
                    lastTimeOfDay = stateValue.currentTimeOfDay
                }

                // G141: Removed verbose story pages log (her update spam!)

                // Tek bir update bloğu ile tüm state'i güncelle
                _uiState.update { currentState ->
                    currentState.copy(
                        storyPages = stateValue.storyPages,
                        currentDay = stateValue.currentDay,
                        currentTimeOfDay = stateValue.currentTimeOfDay,
                        currentLocationString = "", // TODO: Get from GameStateManager
                        compassString = "", // TODO: Get from GameStateManager
                        playerState = stateValue.playerState,
                        resources = stateValue.resources,
                        collectedItems = stateValue.collectedItems,
                        inCombat = stateValue.inCombat,
                        currentEnemies = stateValue.currentEnemies,
                        criticalWarnings = generateCriticalWarnings(stateValue.playerState),
                        currentWeather = stateValue.currentWeather,
                        learnedSpells = PersistentDataManager.gameData.value.playerData.learnedSpells
                    )
                }
            }
        }

        // STRATEJI #3: Memory synthesis state'i dinle (game.GameStateManager'dan)
        viewModelScope.launch {
            try {
                // G141: Removed verbose coroutine log
                gameStateManagerForMemory.isSynthesizingMemory.collect { isSynthesizing ->
                    // G141: Removed verbose memory synthesis log (her update spam!)
                    _uiState.update { it.copy(isSynthesizingMemory = isSynthesizing) }
                }
            } catch (e: Exception) {
                GameLogger.logError("JournalViewModel", "Error collecting isSynthesizingMemory", e)
            }
        }

        // STRATEJI #3: Memory synthesis status mesajını dinle (game.GameStateManager'dan)
        viewModelScope.launch {
            try {
                // G141: Removed verbose coroutine log
                gameStateManagerForMemory.memorySynthesisStatus.collect { status ->
                    // G141: Removed verbose status log (her update spam!)
                    _uiState.update { it.copy(memorySynthesisStatus = status) }
                }
            } catch (e: Exception) {
                GameLogger.logError("JournalViewModel", "Error collecting memorySynthesisStatus", e)
            }
        }

        // G141: Removed verbose init complete log
    }

    private fun generateCriticalWarnings(playerState: PlayerState): List<String> {
        val warnings = mutableListOf<String>()
        if (playerState.currentHealth <= 20) warnings.add("❤️ HP Kritik!")
        if (playerState.currentMana <= 20) warnings.add("💙 MP Kritik!")
        // TODO: Add weight capacity warning
        return warnings
    }

    fun processUserActions(input: String) {
        if (input.isBlank()) return
        if (!_uiState.value.inCombat && _uiState.value.inputCountThisTimeOfDay >= 1) {
            return
        }

        if (!_uiState.value.inCombat) {
            _uiState.update { it.copy(inputCountThisTimeOfDay = it.inputCountThisTimeOfDay + 1) }
        }

        viewModelScope.launch {
            // G93: Check if in NEW combat system (state machine based)
            if (combatStateMachine.isInCombat()) {
                // NEW combat system - route through CombatController
                GameLogger.logSystem("⚔️ G93: Freestyle action via new combat system: $input")
                try {
                    executePlayerAction(com.example.isekaikuroshin.combat.CombatAction.FreestyleAction(input))
                } catch (e: Exception) {
                    GameLogger.logSystem("❌ G93: Freestyle action failed: ${e.message}")
                }
                return@launch
            }

            _uiState.update { it.copy(isLoadingAIResponse = true) }

            // ÖNEMLİ: currentStoryPageText'i BURADA alıyoruz (executeGMActions'dan ÖNCE)
            val currentStoryPageText = _uiState.value.storyPages.lastOrNull() ?: ""
            GameLogger.logSystem("[JOURNAL-VM] Current page text length BEFORE GM: ${currentStoryPageText.length}")

            val inCombat = _uiState.value.inCombat
            val currentEnemies = _uiState.value.currentEnemies
            val rawAiResponse: String

            if (inCombat) {
                // SAVAŞ MANTIĞI (FAZ3-06)
                val currentState = gameStateManager.gameState.value

                // Adım A: Niyeti ayrıştır
                val intent = actionIntentParser.parse(input, currentEnemies)

                // Adım A2: Çevresel etkileri al
                val effects = EnvironmentEngine.getActiveEffects(currentState)

                // TODO-G106-FIX: Map CombatActionType → ActionType (archetype tracking)
                val archetypeAction = when (intent.type) {
                    com.example.isekaikuroshin.engine.CombatActionType.ATTACK ->
                        com.example.isekaikuroshin.engine.ActionType.ATTACK
                    com.example.isekaikuroshin.engine.CombatActionType.DEFEND ->
                        com.example.isekaikuroshin.engine.ActionType.DEFEND
                    com.example.isekaikuroshin.engine.CombatActionType.CAST_SPELL ->
                        com.example.isekaikuroshin.engine.ActionType.CAST_SPELL
                    com.example.isekaikuroshin.engine.CombatActionType.BLIND,
                    com.example.isekaikuroshin.engine.CombatActionType.CRIPPLE ->
                        com.example.isekaikuroshin.engine.ActionType.STEALTH // Explorer action
                    com.example.isekaikuroshin.engine.CombatActionType.UNKNOWN ->
                        com.example.isekaikuroshin.engine.ActionType.OTHER
                }

                // Adım B: Profili Güncelle
                val updatedProfile = profileUpdaterEngine.updateProfile(archetypeAction, currentState.playerState.playerProfile)
                gameStateManager.updatePlayerProfile(updatedProfile)

                // Check for class quest triggers
                val triggeredArchetype = classQuestEngine.checkAndTriggerQuests(currentState.playerState.copy(playerProfile = updatedProfile))
                if (triggeredArchetype != null) {
                    // TODO: gameStateManager.triggerClassQuest(triggeredArchetype)
                }

                // FAZ 9.1: RealityEngine ile Fantastik Eylem Analizi
                // PlayerState → PlayerStatsPersistent conversion
                val playerStats = PlayerStatsPersistent(
                    hp = currentState.playerState.currentHealth.toLong(),
                    maxHp = currentState.playerState.maxHealth.toLong(),
                    mp = currentState.playerState.currentMana.toLong(),
                    maxMp = currentState.playerState.maxMana.toLong(),
                    stamina = currentState.playerState.stamina.toLong(),
                    maxStamina = currentState.playerState.maxStamina.toLong(),
                    strength = currentState.playerState.strength.toLong(),
                    vitality = currentState.playerState.vitality.toLong(),
                    agility = currentState.playerState.agility.toLong(),
                    intelligence = currentState.playerState.intelligence.toLong(),
                    spirit = currentState.playerState.spirit.toLong(),
                    luck = currentState.playerState.luck.toLong(),
                    elementAffinities = emptyMap()  // Element affinity şimdilik boş
                )
                val realityCheck = RealityEngine.analyzeFantasyAction(input, playerStats)

                GameLogger.logSystem("[FAZ 9.1] Fantasy Action Analysis:")
                GameLogger.logSystem("  - Input: $input")
                GameLogger.logSystem("  - Dice Modifier: ${realityCheck.diceModifier}")
                GameLogger.logSystem("  - GM Comment: ${realityCheck.gmComment}")

                // Çevresel etkileri hesapla
                var totalAgiModifier = 0.0f
                for (effect in effects) {
                    val agiBonus = effect.statModifier.getOrElse(com.example.isekaikuroshin.engine.StatType.AGI) { 0.0f }
                    totalAgiModifier += agiBonus
                }

                // Adım C: Gelişmiş zar atışını simüle et (RealityEngine modifier dahil)
                val baseSuccessChance = 0.5f
                val realityModifier = realityCheck.diceModifier.toFloat() / 20f // -10~+10 → -0.5~+0.5
                val successChance = (baseSuccessChance + totalAgiModifier + realityModifier).coerceIn(0.1f, 0.9f)
                val isSuccess = Random.nextFloat() < successChance

                GameLogger.logSystem("  - Base Chance: $baseSuccessChance")
                GameLogger.logSystem("  - Reality Modifier: $realityModifier")
                GameLogger.logSystem("  - Final Chance: $successChance")
                GameLogger.logSystem("  - Result: ${if (isSuccess) "SUCCESS" else "FAIL"}")

                // Adım D: Yapılandırılmış prompt oluştur
                val combatPrompt = """
SİSTEM TALİMATI: Sen bir savaş anlatıcısısın. Sana verilen olayın sonucunu, atmosferik ve etkileyici bir şekilde, 1-2 cümleyle anlat.

OLAY:
- Oyuncu Eylemi: \"$input\"
- Gerçekleşen Niyet: ${intent.type.name}
- Hedef: ${intent.target ?: "Belirtilmedi"}
- Zar Sonucu: ${if (isSuccess) "BAŞARILI" else "BAŞARISIZ"}
- GM Yorumu: ${realityCheck.gmComment.ifEmpty { "Uygun eylem" }}
- Çevresel Durum: ${effects.joinToString { it.name }.ifEmpty { "Normal" }}
- Anlatım Talimatı: Bu sonucun hikayesini yaz. Başarılıysa düşmanın nasıl etkilendiğini, başarısızsa oyuncunun nasıl ıskaladığını veya düşmanın nasıl savuşturduğunu anlat. GM yorumunu ve çevresel durumu da dikkate al.
"""
                // Adım E: AI Çağrısı (Çevrimdışı mod desteği ile)
                val isOnlineCombat = networkMonitor.isOnline()
                rawAiResponse = if (isOnlineCombat) {
                    val response = basicStoryEngine.generateStoryResponse(combatPrompt)
                    // Cache combat response
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date())
                    aiResponseCache.saveJournalSummary("${today}_combat", response)
                    response
                } else {
                    // Offline: use cached or placeholder
                    pendingSyncManager.addPendingItem("combat", combatPrompt)
                    "\n⚠️ Çevrimdışı Mod - Savaş simülasyonu çalışmıyor\n\n${intent.type.name} eyleminiz kaydedildi."
                }

                // FAZ 9.2: Mekanik Etki - Hasar Hesaplama ve Uygulama
                if (isSuccess && intent.target != null) {
                    // TODO-G113.3: Apply status effect stat modifiers to combat damage
                    val statModifiers = com.example.isekaikuroshin.data.StatusEffectManager()
                        .calculateStatModifiers(currentState.activeStatusEffects)

                    val strengthModifier = statModifiers["strength"] ?: 0

                    // Hasar hesaplama: Base 20 + STR bonus + Reality modifier + Status Effect modifiers
                    val baseDamage: Int = 20
                    val strBonus: Int = (playerStats.strength / 10L).toInt().coerceAtLeast(0)
                    val realityBonus: Int = realityCheck.diceModifier.coerceAtLeast(0)
                    val statusEffectBonus: Int = (strengthModifier / 10).coerceAtLeast(-5) // Max -5 penalty, no max bonus
                    val totalDamage: Int = (baseDamage + strBonus + realityBonus + statusEffectBonus).coerceAtLeast(1)

                    GameLogger.logSystem("[COMBAT] Damage calculation:")
                    GameLogger.logSystem("  - Base: $baseDamage")
                    GameLogger.logSystem("  - STR Bonus: $strBonus (STR: ${playerStats.strength})")
                    GameLogger.logSystem("  - Reality Bonus: $realityBonus")
                    GameLogger.logSystem("  - Status Effect Bonus: $statusEffectBonus")
                    GameLogger.logSystem("  - Total: $totalDamage")

                    // TODO-G112.3: Combat Stamina Drain - Saldırı stamina tüketir
                    gameStateManager.consumeCombatStamina(staminaCost = 10, fatigueGain = 3)
                    GameLogger.logSystem("[COMBAT] Stamina cost: -10 stamina, +3 fatigue")

                    // TODO-G106.2: Track combat action for archetype profiling
                    gameStateManager.trackPlayerAction(com.example.isekaikuroshin.engine.ActionType.ATTACK)

                    // Düşmana hasar uygula
                    val enemyDefeated = gameStateManager.updateEnemyHp(intent.target, totalDamage)

                    // Düşman öldü mü?
                    if (enemyDefeated) {
                        gameStateManager.removeDefeatedEnemies()

                        // Tüm düşmanlar öldüyse savaşı bitir
                        val aliveEnemies = _uiState.value.currentEnemies.filter { it.hp > 0 }
                        if (aliveEnemies.isEmpty()) {
                            gameStateManager.updateCombatStatus(false)
                            GameLogger.logSystem("[COMBAT] 🎉 Victory! All enemies defeated!")
                        }
                    }
                }
            } else {
                // SAVAŞ DIŞI MANTIK - GAME MASTER ENGINE KULLANIMI
                GameLogger.logSystem("=== JOURNAL: Using GameMasterEngine for non-combat action ===")
                GameLogger.logSystem("Player input: $input")

                val currentState = gameStateManager.gameState.value

                // 🌐 ÇEVRİMDIŞI MOD KONTROLÜ
                val isOnline = networkMonitor.isOnline()
                GameLogger.logSystem("🌐 Network status: ${if (isOnline) "ONLINE" else "OFFLINE"}")

                // G105.1: gmResponse'u dışarıda tanımla (scope sorunu fix)
                var gmResponse: GMResponse? = null

                rawAiResponse = if (isOnline) {
                    // ✅ ONLINE: Normal AI çağrısı
                    val gmResult = gameMasterEngine.generateStoryWithContext(input, currentState)

                    if (gmResult.isSuccess) {
                        GameLogger.logSystem("GameMasterEngine success: Using context-aware story")
                        gmResponse = gmResult.getOrNull()

                        // TODO-AI-06: GM Eylem Yürütücüsü - GMResponse'u işle
                        if (gmResponse != null) {
                            // GÖREV #19-21: Overlay callback'i showOverlay fonksiyonuna bağla
                            gameStateManager.executeGMActions(gmResponse) { overlayData ->
                                GameLogger.logSystem("OVERLAY: ${overlayData::class.simpleName} - ${overlayData}")
                                // UI'ye overlay göster
                                showOverlay(overlayData)
                            }

                            // G81: Execute structured actions (Combat system)
                            if (gmResponse.structuredActions.isNotEmpty()) {
                                GameLogger.logSystem("=== G81: EXECUTING STRUCTURED ACTIONS (Normal Flow) ===")
                                val gameActions = gmResponse.structuredActions.map { structuredAction ->
                                    com.example.isekaikuroshin.ai.GameAction(
                                        actionType = structuredAction.actionType,
                                        parameters = structuredAction.parameters
                                    )
                                }
                                actionExecutorEngine.executeActions(gameActions)
                                GameLogger.logSystem("=== G81: STRUCTURED ACTIONS COMPLETE ===")
                            }
                        }

                        val aiText = gmResponse?.journalEntry ?: "GameMaster engine returned null response"

                        // 💾 AI cevabını cache'le
                        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        aiResponseCache.saveJournalSummary(today, aiText)
                        GameLogger.logSystem("💾 Cached AI response for $today")

                        aiText
                    } else {
                        GameLogger.logError("JournalViewModel", "GameMasterEngine failed, falling back to BasicStoryEngine", gmResult.exceptionOrNull() as? Exception)
                        // Fallback to basic story engine
                        val fallbackPrompt = "Oyuncu şunu yaptı: $input. Hikayeye devam et."
                        val response = basicStoryEngine.generateStoryResponse(fallbackPrompt)

                        // Cache fallback response too
                        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        aiResponseCache.saveJournalSummary(today, response)

                        response
                    }
                } else {
                    // 📴 OFFLINE: Cache'den eski cevap göster ve pending'e ekle
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date())

                    // Pending listesine ekle (internet gelince işlenecek)
                    pendingSyncManager.addPendingItem("journal", input)
                    GameLogger.logSystem("📴 Added to pending sync: $input")

                    // Cache'den en son özeti bul
                    val cached = aiResponseCache.getJournalSummary(today)

                    if (cached != null) {
                        GameLogger.logSystem("💾 Using cached response from: ${cached.getFormattedDate()}")
                        "\n⚠️ Çevrimdışısınız - ${cached.getFormattedDate()} tarihli yanıt gösteriliyor\n\n${cached.response}"
                    } else {
                        GameLogger.logSystem("⚠️ No cache available, using placeholder")
                        "\n⚠️ Çevrimdışısınız - İnternet bağlantısı gerekli\n\nGirişiniz kaydedildi ve internet bağlantısı geri geldiğinde işlenecek."
                    }
                }

                // G105.1: Ahlak Puanı entegrasyonu - AI sentiment + keyword analysis
                // Önce keyword-based scoring (fallback)
                val keywordScore = MoralityEngine.analyzeAndGetScore(input)

                // AI'dan gelen sentiment analysis varsa onu kullan (daha doğru)
                val finalScore = if (gmResponse != null && gmResponse.moralityDelta != null) {
                    GameLogger.logSystem("G105.1: Using AI sentiment analysis (moralityDelta: ${gmResponse.moralityDelta})")
                    gmResponse.moralityDelta!!
                } else {
                    GameLogger.logSystem("G105.1: Using keyword-based scoring (fallback: $keywordScore)")
                    keywordScore
                }

                if (finalScore != 0.0f) {
                    gameStateManager.updateMoralityScore(finalScore)
                }

                // TODO-G106.4: Archetype Tracking - AI'dan gelen action'ı işle
                if (gmResponse != null && gmResponse.playerAction != null) {
                    try {
                        val actionType = com.example.isekaikuroshin.engine.ActionType.valueOf(gmResponse.playerAction!!)
                        gameStateManager.trackPlayerAction(actionType)
                        GameLogger.logSystem("G106.4: Tracked player action: ${gmResponse.playerAction}")
                    } catch (e: IllegalArgumentException) {
                        GameLogger.logError("JournalViewModel", "Invalid playerAction from AI: ${gmResponse.playerAction}", e)
                        // Fallback: keyword detection
                        gameStateManager.detectAndTrackAction(input)
                    }
                } else {
                    // Fallback: keyword-based detection
                    gameStateManager.detectAndTrackAction(input)
                }

                // TODO: gameStateManager.processJournalEntryForPassiveGp(input)
                gameStateManager.advanceTime()

                // G102: Passive stat decrease per user action
                gameStateManager.consumeStamina(staminaCost = 3)
                gameStateManager.decreaseHunger(amount = 2)
                gameStateManager.increaseFatigue(amount = 1)
                GameLogger.logSystem("[G102] Passive stat decrease: -3 stamina, -2 hunger, +1 fatigue")
            }

            val finalContent = "$currentStoryPageText\n\n> $input\n\n$rawAiResponse"

            // TODO-FIX-JOURNAL-01: Debug log - Son sayfa güncelleniyor
            GameLogger.logSystem("[JOURNAL-VM] Updating last story page. New content length: ${finalContent.length}")
            GameLogger.logSystem("[JOURNAL-VM] Content preview: ${finalContent.takeLast(150)}")

            gameStateManager.updateLastStoryPage(finalContent)

            // STRATEJI #3: Add dialogue to Flash Memory
            com.example.isekaikuroshin.ai.GlobalAIManager.addDialogueToMemory("Player", input)
            com.example.isekaikuroshin.ai.GlobalAIManager.addDialogueToMemory("AI", rawAiResponse)

            // TEST-LOG: Sayfa sayısı tracking
            val currentPageCount = _uiState.value.storyPages.size
            GameLogger.logSystem("[TEST-PAGES] Current story pages count: $currentPageCount")
            GameLogger.logSystem("[TEST-PAGES] Current day: ${_uiState.value.currentDay}")
            GameLogger.logSystem("[TEST-PAGES] Current time: ${_uiState.value.currentTimeOfDay}")

            _uiState.update { it.copy(isLoadingAIResponse = false) }
        }
        userInput.value = ""
    }

    /**
     * Test fonksiyonu - GameMasterEngine'in tam entegrasyonunu test eder
     */
    fun testGameMasterEngine() {
        viewModelScope.launch {
            GameLogger.logSystem("=== JOURNAL: STARTING GAMEMASTER ENGINE TEST ===")

            try {
                val currentState = gameStateManager.gameState.value
                val testInput = "Gizemli kulede keşif yapıyorum"

                // GameMasterEngine test'ini çalıştır
                val result = gameMasterEngine.runCompleteTest(testInput, currentState)

                if (result.isSuccess) {
                    val gmResponse = result.getOrNull()
                    val storyText = gmResponse?.journalEntry ?: "Test başarılı ama hikaye metni boş"
                    GameLogger.logSystem("✅ GAMEMASTER ENGINE TEST SUCCESS")
                    GameLogger.logSystem("Generated story: ${storyText.take(200)}...")

                    // GMResponse detaylarını logla
                    gmResponse?.let { response ->
                        GameLogger.logSystem("Items gained: ${response.itemsGained}")
                        GameLogger.logSystem("Quests updated: ${response.questsUpdated}")
                        GameLogger.logSystem("Stats changed: ${response.statsChanged}")

                        // G61c: Apply GM response to persistent data
                        gameStateManagerForMemory.applyGMResponse(response)

                        // G81: Execute structured actions (Combat system)
                        if (response.structuredActions.isNotEmpty()) {
                            GameLogger.logSystem("=== G81: EXECUTING STRUCTURED ACTIONS (Test Flow) ===")
                            val gameActions = response.structuredActions.map { structuredAction ->
                                com.example.isekaikuroshin.ai.GameAction(
                                    actionType = structuredAction.actionType,
                                    parameters = structuredAction.parameters
                                )
                            }
                            actionExecutorEngine.executeActions(gameActions)
                            GameLogger.logSystem("=== G81: STRUCTURED ACTIONS COMPLETE ===")
                        }

                        // TODO-AI-06: executeGMActions method removed (doesn't exist)
                        // gameStateManager.executeGMActions(response) { overlayData ->
                        //     GameLogger.logSystem("TEST OVERLAY: ${overlayData::class.simpleName} - ${overlayData}")
                        // }
                    }

                    // Test hikayesini günlüğe ekle
                    val currentPage = _uiState.value.storyPages.lastOrNull() ?: ""
                    val testEntry = "$currentPage\n\n🧪 **GAMEMASTER ENGINE TEST**\n> $testInput\n\n$storyText"
                    gameStateManager.updateLastStoryPage(testEntry)

                } else {
                    GameLogger.logError("JournalViewModel", "GameMaster Engine test failed", result.exceptionOrNull() as? Exception)
                }

            } catch (e: Exception) {
                GameLogger.logError("JournalViewModel", "Exception during GameMaster test", e)
            }

            GameLogger.logSystem("=== JOURNAL: GAMEMASTER ENGINE TEST COMPLETE ===")
        }
    }

    /**
     * GÖREV I: Mikro geri bildirim ekle
     */
    fun addMicroFeedback(statName: String, change: Int) {
        _uiState.update { current ->
            current.copy(
                microFeedbacks = current.microFeedbacks + com.example.isekaikuroshin.ui.components.MicroFeedbackHelper.createFeedback(
                    statName, change
                )
            )
        }
    }

    /**
     * GÖREV I: Mikro geri bildirimleri temizle
     */
    fun clearMicroFeedbacks() {
        _uiState.update { it.copy(microFeedbacks = emptyList()) }
    }

    // ============================
    // GÖREV N - FAZ 4: SPELL COMBAT USAGE
    // ============================

    /**
     * Büyü savaşta kullanıldığında usage count'u artır
     */
    fun incrementSpellCombatUsage(spellId: String) {
        gameStateManager.incrementSpellCombatUsage(spellId)
    }

    // ============================
    // GÖREV N - FAZ 5: SPELL PERFORMANCE ANALYSIS
    // ============================

    /**
     * Seçilen büyü için performans analizi başlat
     *
     * @param spell Kullanılacak büyü
     * @param onCameraNeeded Kamera gerekiyorsa callback
     * @param onVoiceNeeded Ses tanıma gerekiyorsa callback
     * @param onPerformanceReady Performans analizi hazır olduğunda callback
     */
    fun startSpellPerformance(
        spell: com.example.isekaikuroshin.data.combat.LearnedSpell,
        onCameraNeeded: () -> Unit,
        onVoiceNeeded: () -> Unit,
        onPerformanceReady: (com.example.isekaikuroshin.engine.SpellPerformanceAnalyzer.PerformanceResult) -> Unit
    ) {
        // Büyünün trigger tiplerini kontrol et
        val needsCamera = spell.personalTriggers.values.any {
            it.triggerType == com.example.isekaikuroshin.data.combat.TriggerType.SINGLE_HAND_GESTURE ||
            it.triggerType == com.example.isekaikuroshin.data.combat.TriggerType.DOUBLE_HAND_GESTURE
        }

        val needsVoice = spell.personalTriggers.values.any {
            it.triggerType == com.example.isekaikuroshin.data.combat.TriggerType.VOICE
        }

        when {
            needsCamera -> onCameraNeeded()
            needsVoice -> onVoiceNeeded()
            else -> {
                // Timer-only spell, otomatik başarılı
                val mockData = spell.personalTriggers.keys.associateWith { "" }
                val result = com.example.isekaikuroshin.engine.SpellPerformanceAnalyzer.analyzePerformance(
                    spell = spell,
                    capturedData = mockData
                )
                onPerformanceReady(result)
            }
        }
    }

    /**
     * Yakalanan veriyi analiz et ve performans sonucu oluştur
     */
    fun analyzeSpellPerformance(
        spell: com.example.isekaikuroshin.data.combat.LearnedSpell,
        capturedData: Map<String, Any>
    ): com.example.isekaikuroshin.engine.SpellPerformanceAnalyzer.PerformanceResult {
        return com.example.isekaikuroshin.engine.SpellPerformanceAnalyzer.analyzePerformance(
            spell = spell,
            capturedData = capturedData
        )
    }

    // ============================
    // GÖREV N - FAZ 6: SPELL COMBAT MECHANICS
    // ============================

    /**
     * Büyü savaş sonucunu hesapla (dice roll + damage)
     */
    fun executeSpellCombat(
        spell: com.example.isekaikuroshin.data.combat.LearnedSpell,
        performanceScore: Float
    ): com.example.isekaikuroshin.engine.SpellCombatHelper.SpellCombatResult {
        return com.example.isekaikuroshin.engine.SpellCombatHelper.executeSpellCombat(
            spell = spell,
            performanceScore = performanceScore,
            difficulty = com.example.isekaikuroshin.engine.DiceSystem.Difficulty.MEDIUM
        )
    }

    // ============================
    // GÖREV #19-21: JOURNAL OVERLAY/NOTIFICATION SYSTEM
    // ============================

    /**
     * Overlay göster
     * Backend'den gelen overlay verilerini UI'ye ilet
     */
    fun showOverlay(overlayData: com.example.isekaikuroshin.ui.components.OverlayData) {
        GameLogger.logVerbose("JOURNAL-OVERLAY", "Showing overlay: ${overlayData::class.simpleName}")
        _uiState.update {
            it.copy(
                currentOverlay = overlayData,
                isOverlayVisible = true
            )
        }
    }

    /**
     * Overlay kapat
     */
    fun dismissOverlay() {
        GameLogger.logVerbose("JOURNAL-OVERLAY", "Dismissing overlay")
        _uiState.update {
            it.copy(
                currentOverlay = null,
                isOverlayVisible = false
            )
        }
    }

    // ============================
    // G93: COMBAT ACTIONS
    // ============================

    /**
     * Execute player combat action
     * Wraps CombatController call for UI layer
     */
    suspend fun executePlayerAction(action: com.example.isekaikuroshin.combat.CombatAction) {
        GameLogger.logSystem("⚔️ JournalViewModel: Executing combat action: ${action::class.simpleName}")
        try {
            combatController.executePlayerAction(action)
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Combat action failed: ${e.message}")
            // Add error message to story
            gameStateManager.addStoryPage("⚠️ Combat error: ${e.message}")
        }
    }
}
