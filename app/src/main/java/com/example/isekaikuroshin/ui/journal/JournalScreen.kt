package com.example.isekaikuroshin.ui.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import com.example.isekaikuroshin.ui.theme.JournalBrown
import com.example.isekaikuroshin.ui.theme.JournalDarkBrown
import com.example.isekaikuroshin.ui.theme.JournalLightBrown
import com.example.isekaikuroshin.ui.theme.JournalDarkRed
import com.example.isekaikuroshin.ui.theme.JournalSuccess
import com.example.isekaikuroshin.ui.theme.JournalWarning
import com.example.isekaikuroshin.ui.components.MicroFeedbackOverlay
import com.example.isekaikuroshin.ui.components.SystemOverlay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.CollectedItemsZ5
import com.example.isekaikuroshin.data.Enemy
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.TimeOfDay
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.ui.combat.CombatActionDialog
import com.example.isekaikuroshin.ui.combat.SpellSelectionDialog
import com.example.isekaikuroshin.ui.combat.SpellPerformanceDialog
import com.example.isekaikuroshin.engine.SpellPerformanceAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



@UnstableApi
@Composable
fun JournalScreen(onBackToDashboard: (() -> Unit)? = null, journalViewModel: JournalViewModel = hiltViewModel()) {
    // G141: Removed verbose composable log (her recomposition spam!)

    // G83: EventLogger'ı başlat (sadece bir kere çağrılır)
    LaunchedEffect(Unit) {
        com.example.isekaikuroshin.utils.EventLogger.init(journalViewModel)
        GameLogger.logSystem("✅ G83: EventLogger initialized with JournalViewModel")
    }

    val uiState by journalViewModel.uiState.collectAsState()
    // G141: Removed verbose uiState log (her state change spam!)

    val isFirstJournalVisit = remember { PersistentDataManager.isFirstJournalVisit() }
    // G141: Removed verbose isFirstJournalVisit log

    var showIntroVideo by remember { mutableStateOf(isFirstJournalVisit) }
    var pageState: PageState by remember { mutableStateOf(PageState.CLOSED) }
    var currentPage: Int by remember { mutableIntStateOf(1) }
    var showPageSelector by remember { mutableStateOf(false) }
    var isTimeDisplayExpanded by remember { mutableStateOf(false) }
    // G97: Spell selection dialog control
    var showSpellSelectionDialog by remember { mutableStateOf(false) }
    var showPerformanceDialog by remember { mutableStateOf(false) }
    var performanceResult by remember { mutableStateOf<SpellPerformanceAnalyzer.PerformanceResult?>(null) }
    var selectedSpellForPerformance by remember { mutableStateOf<com.example.isekaikuroshin.data.combat.LearnedSpell?>(null) }
    var combatResult by remember { mutableStateOf<com.example.isekaikuroshin.engine.SpellCombatHelper.SpellCombatResult?>(null) }  // FAZ 6
    val scope = rememberCoroutineScope() // G93: For combat actions

    GameLogger.logVerbose("JOURNAL-SCREEN", "🔵 State variables initialized")

    // TODO-FIX-JOURNAL-01: Story pages güncellendiğinde son sayfayı göster
    // NOT: storyPages.size yerine lastOrNull() kullanıyoruz çünkü son sayfa içeriği değiştiğinde de güncellenmeli
    LaunchedEffect(uiState.storyPages.lastOrNull()) {
        if (uiState.storyPages.isNotEmpty() && pageState == PageState.READING) {
            val lastPageIndex = uiState.storyPages.size
            if (currentPage != lastPageIndex) {
                currentPage = lastPageIndex
                GameLogger.logVerbose("JOURNAL-FIX", "Auto-navigating to last page: $currentPage / ${uiState.storyPages.size}")
            }
        }
    }

    // G93: Combat system now uses CombatStateMachine - no need for LaunchedEffect
    // Old combat dialog flag removed

    // FB#12: Weather sound MainScreen'de yönetiliyor, buradan kaldırıldı
    val context = LocalContext.current

    var showResourceNotification by remember { mutableStateOf(false) }
    var lastGainedResource by remember { mutableStateOf("") }



    val demonBgResources = remember {
        listOf(
            R.drawable.demon_bg_00, R.drawable.demon_bg_01, R.drawable.demon_bg_02,
            R.drawable.demon_bg_03, R.drawable.demon_bg_04, R.drawable.demon_bg_05,
            R.drawable.demon_bg_06, R.drawable.demon_bg_07, R.drawable.demon_bg_08,
            R.drawable.demon_bg_09, R.drawable.demon_bg_10, R.drawable.demon_bg_11,
            R.drawable.demon_bg_12, R.drawable.demon_bg_13, R.drawable.demon_bg_14,
            R.drawable.demon_bg_15, R.drawable.demon_bg_16, R.drawable.demon_bg_17
        )
    }
    var currentDemonBg by remember { mutableIntStateOf((0..17).random()) }

    LaunchedEffect(pageState) {
        if (pageState in listOf(PageState.OPENING, PageState.PAGE_TURNING, PageState.REVERSE_PAGE_TURNING, PageState.FINISHING)) {
            while (pageState in listOf(PageState.OPENING, PageState.PAGE_TURNING, PageState.REVERSE_PAGE_TURNING, PageState.FINISHING)) {
                var newBgIndex: Int
                do {
                    newBgIndex = (0 until demonBgResources.size).random()
                } while (newBgIndex == currentDemonBg && demonBgResources.size > 1)
                currentDemonBg = newBgIndex
                delay(2000)
            }
        }
    }

    var videoCompleted by remember { mutableStateOf(false) }

    // GÖREV #1: UI Jank Fix - ExoPlayer lazy initialization
    // ExoPlayer sadece animasyon gerektiğinde oluşturulacak (pageState değişince)
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }

    LaunchedEffect(pageState, context) {
        // Animasyon başladığında ExoPlayer'ı oluştur
        if (pageState in listOf(PageState.OPENING, PageState.PAGE_TURNING, PageState.REVERSE_PAGE_TURNING, PageState.FINISHING)) {
            if (exoPlayer == null) {
                GameLogger.logVerbose("JOURNAL-PERF", "Lazy loading ExoPlayer")
                exoPlayer = ExoPlayer.Builder(context).build().apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                videoCompleted = true
                            }
                        }
                    })
                }
            }
        }
    }

    LaunchedEffect(pageState, videoCompleted) {
        when (pageState) {
            PageState.OPENING, PageState.REVERSE_PAGE_TURNING -> {
                if (videoCompleted) {
                    pageState = PageState.READING
                    videoCompleted = false
                }
            }
            PageState.PAGE_TURNING -> {
                if (videoCompleted) {
                    currentPage += 1
                    pageState = PageState.READING
                    videoCompleted = false
                }
            }
            PageState.FINISHING -> {
                if (videoCompleted) {
                    onBackToDashboard?.invoke()
                    videoCompleted = false
                }
            }
            else -> { /* Do nothing */ }
        }
    }

    LaunchedEffect(pageState, context, exoPlayer) {
        exoPlayer?.let { player ->
            when (pageState) {
                PageState.OPENING -> {
                    val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.book_opening}")
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
                PageState.PAGE_TURNING -> {
                    val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.page_turn_forward}")
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
                PageState.REVERSE_PAGE_TURNING -> {
                    val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.page_turn_backward}")
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
                PageState.FINISHING -> {
                    val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.book_closing}")
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
                else -> {
                    player.pause()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }

    val focusManager = LocalFocusManager.current

    // G97: Combat state - defined at top level for wider scope access
    val combatState by journalViewModel.combatStateMachine.state.collectAsState()

    if (showIntroVideo) {
        JournalIntroVideoH1(
            onIntroComplete = {
                PersistentDataManager.setFirstJournalVisitCompleted()
                showIntroVideo = false
                pageState = PageState.OPENING
            }
        )
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (pageState) {
            PageState.CLOSED -> {
                JournalClosedBookH2(onOpenJournal = { pageState = PageState.OPENING })
            }

            PageState.OPENING, PageState.PAGE_TURNING, PageState.REVERSE_PAGE_TURNING, PageState.FINISHING -> {
                exoPlayer?.let { player ->
                    JournalAnimationBackgroundH3(
                        currentDemonBg = currentDemonBg,
                        demonBgResources = demonBgResources,
                        exoPlayer = player
                    )
                }
            }

            PageState.READING, PageState.NEW_PAGE -> {
                JournalMainReadingInterfaceH4(
                    journalViewModel = journalViewModel,
                    showResourceNotification = showResourceNotification,
                    lastGainedResource = lastGainedResource,
                    collectedItems = uiState.collectedItems,
                    currentPage = currentPage,
                    showPageSelector = showPageSelector,
                    userInput = journalViewModel.userInput.value,
                    onBackToDashboard = onBackToDashboard,
                    onPageStateChange = { newState: PageState -> pageState = newState },
                    onCurrentPageChange = { newPage: Int -> currentPage = newPage },
                    onShowPageSelectorChange = { show: Boolean -> showPageSelector = show },
                    onUserInputChange = { journalViewModel.userInput.value = it },
                    onTimeDisplayExpandedChange = { expanded: Boolean -> isTimeDisplayExpanded = expanded },
                    isTimeDisplayExpanded = isTimeDisplayExpanded,
                    focusManager = focusManager,
                    combatState = combatState // G97: Pass combat state
                )
            }
        }

        if (showPageSelector) {
            JournalPageSelectorDialogH5(
                journalViewModel = journalViewModel,
                currentPage = currentPage,
                onPageSelected = { pageNum: Int -> currentPage = pageNum },
                onDismiss = { showPageSelector = false }
            )
        }
        // G97: Combat UI Overlay System (combatState defined at top level - line 242)
        var showFreestyleInput by remember { mutableStateOf(false) }
        var freestyleInputText by remember { mutableStateOf("") }

        // Reset freestyle input when combat ends
        LaunchedEffect(combatState) {
            if (combatState is com.example.isekaikuroshin.combat.CombatState.Idle) {
                showFreestyleInput = false
                freestyleInputText = ""
            }
        }

        // G97: Fullscreen combat overlay (always visible during combat)
        if (combatState !is com.example.isekaikuroshin.combat.CombatState.Idle) {
            com.example.isekaikuroshin.ui.combat.CombatDialog(
                combatState = combatState,
                playerStats = com.example.isekaikuroshin.data.PlayerStats(
                    hp = uiState.playerState.currentHealth,
                    maxHp = uiState.playerState.maxHealth,
                    mp = uiState.playerState.currentMana,
                    maxMp = uiState.playerState.maxMana,
                    attack = uiState.playerState.physicalAttack,
                    defense = uiState.playerState.defense,
                    speed = 10 // TODO-G95: Get from playerState
                ),
                onUseSpell = {
                    // G97: Open spell selection dialog
                    GameLogger.logSystem("Combat: Use Spell clicked - opening spell selection")
                    showSpellSelectionDialog = true
                },
                onFreestyleAction = {
                    // G95: Toggle freestyle input (stays in dialog, doesn't dismiss)
                    GameLogger.logSystem("Combat: Freestyle Action - toggling input field")
                    showFreestyleInput = !showFreestyleInput
                    if (!showFreestyleInput) {
                        freestyleInputText = "" // Clear on cancel
                    }
                },
                onRun = {
                    // G97: Execute run action via CombatController
                    GameLogger.logSystem("Combat: Run clicked - attempting to escape")
                    scope.launch {
                        journalViewModel.executePlayerAction(com.example.isekaikuroshin.combat.CombatAction.Run)
                    }
                },
                onCombatEnd = {
                    // Combat ended, state machine already transitioned to IDLE
                    GameLogger.logSystem("Combat: Dialog will auto-close (state = IDLE)")
                },
                // G95: Freestyle input parameters
                showFreestyleInput = showFreestyleInput,
                freestyleInputText = freestyleInputText,
                onFreestyleInputChange = { freestyleInputText = it },
                onFreestyleSubmit = {
                    // G95: Submit freestyle action to combat system
                    GameLogger.logSystem("Combat: Freestyle action submitted: $freestyleInputText")
                    scope.launch {
                        journalViewModel.executePlayerAction(
                            com.example.isekaikuroshin.combat.CombatAction.FreestyleAction(freestyleInputText)
                        )
                        // Clear input and hide field
                        freestyleInputText = ""
                        showFreestyleInput = false
                    }
                }
            )
        }

        // GÖREV N - FAZ 4: Spell Selection Dialog (Gerçek Veri)
        if (showSpellSelectionDialog) {
            SpellSelectionDialog(
                learnedSpells = uiState.learnedSpells,
                onSpellSelected = { spell ->
                    GameLogger.logVerbose("JOURNAL-COMBAT", "Selected spell: ${spell.customName}")
                    selectedSpellForPerformance = spell
                    showSpellSelectionDialog = false

                    // G93: Check if in NEW combat system
                    if (combatState is com.example.isekaikuroshin.combat.CombatState.PlayerTurnSelect) {
                        // NEW combat system - execute spell through CombatController
                        val enemies = (combatState as com.example.isekaikuroshin.combat.CombatState.PlayerTurnSelect).enemies
                        val targetEnemy = enemies.firstOrNull()
                        if (targetEnemy != null) {
                            GameLogger.logSystem("⚔️ G93: Using spell ${spell.customName} on ${targetEnemy.name}")
                            scope.launch {
                                journalViewModel.executePlayerAction(
                                    com.example.isekaikuroshin.combat.CombatAction.UseLearnedSpell(
                                        spellId = spell.id,
                                        targetId = targetEnemy.id
                                    )
                                )
                            }
                        } else {
                            GameLogger.logSystem("⚠️ G93: No target enemy found")
                        }
                        return@SpellSelectionDialog
                    }

                    // FAZ 5: Performans analizi başlat (OLD combat system)
                    // GERÇEK VERİ: Spell Studio'daki kalibrasyon verilerini kullan
                    journalViewModel.startSpellPerformance(
                        spell = spell,
                        onCameraNeeded = {
                            // Spell Studio'da kaydedilmiş gesture verilerini kullan
                            GameLogger.logVerbose("JOURNAL-COMBAT", "Using calibrated gesture data from Spell Studio")

                            // Kalibrasyon verilerinden mock capture oluştur
                            // (Gerçekte kullanıcı Spell Studio'da zaten calibrate etmiş)
                            val capturedData = spell.personalTriggers.mapValues { (_, calibration) ->
                                when (calibration.triggerType) {
                                    com.example.isekaikuroshin.data.combat.TriggerType.SINGLE_HAND_GESTURE,
                                    com.example.isekaikuroshin.data.combat.TriggerType.DOUBLE_HAND_GESTURE -> {
                                        // Kalibrasyon accuracy'sini kullan (zaten Spell Studio'da ölçülmüş)
                                        calibration.accuracy
                                    }
                                    else -> ""
                                }
                            }

                            val result = journalViewModel.analyzeSpellPerformance(spell, capturedData)
                            performanceResult = result
                            showPerformanceDialog = true
                        },
                        onVoiceNeeded = {
                            // Spell Studio'da kaydedilmiş voice verilerini kullan
                            GameLogger.logVerbose("JOURNAL-COMBAT", "Using calibrated voice data from Spell Studio")

                            // Kalibrasyon verilerinden beklenen metni al
                            val capturedData = spell.personalTriggers.mapValues { (_, calibration) ->
                                when (calibration.triggerType) {
                                    com.example.isekaikuroshin.data.combat.TriggerType.VOICE -> {
                                        // Beklenen metni calibrationData'dan al
                                        calibration.calibrationData.trim('"')
                                    }
                                    else -> ""
                                }
                            }

                            val result = journalViewModel.analyzeSpellPerformance(spell, capturedData)
                            performanceResult = result

                            // FAZ 6: Combat result hesapla
                            val combat = journalViewModel.executeSpellCombat(spell, result.overallScore)
                            combatResult = combat

                            showPerformanceDialog = true
                        },
                        onPerformanceReady = { result ->
                            performanceResult = result

                            // FAZ 6: Combat result hesapla
                            val combat = journalViewModel.executeSpellCombat(spell, result.overallScore)
                            combatResult = combat

                            showPerformanceDialog = true
                        }
                    )
                },
                onDismiss = {
                    showSpellSelectionDialog = false
                }
            )
        }

        // GÖREV N - FAZ 5/6: Spell Performance Dialog (Combat Result ile)
        if (showPerformanceDialog && performanceResult != null && selectedSpellForPerformance != null) {
            SpellPerformanceDialog(
                result = performanceResult!!,
                spellName = selectedSpellForPerformance!!.customName,
                combatResult = combatResult,  // FAZ 6: Zar sonucu ve hasar
                onDismiss = {
                    showPerformanceDialog = false
                    performanceResult = null
                    combatResult = null
                },
                onContinue = {
                    showPerformanceDialog = false

                    // FAZ 6: Zar başarılıysa büyü kullanımını kaydet
                    if (combatResult?.diceRoll?.success == true) {
                        journalViewModel.incrementSpellCombatUsage(selectedSpellForPerformance!!.id)
                        GameLogger.logVerbose("JOURNAL-COMBAT", "Spell successfully cast! Damage: ${combatResult!!.spellPower}")
                        // TODO: Enemy'ye hasar uygula
                    }

                    performanceResult = null
                    selectedSpellForPerformance = null
                    combatResult = null
                }
            )
        }

        // GÖREV I: Mikro Geri Bildirimler Overlay (Sağ Üst Köşe)
        MicroFeedbackOverlay(
            feedbacks = uiState.microFeedbacks,
            onDismiss = { journalViewModel.clearMicroFeedbacks() },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // GÖREV #19-21: Journal Overlay/Notification Sistemi
        SystemOverlay(
            overlayData = uiState.currentOverlay,
            isVisible = uiState.isOverlayVisible,
            onDismiss = { journalViewModel.dismissOverlay() }
        )

        // STRATEJI #3: Memory Synthesis Loading Overlay
        if (uiState.isSynthesizingMemory) {
            MemorySynthesisLoadingOverlay(statusMessage = uiState.memorySynthesisStatus)
        }
    }
}

/**
 * STRATEJI #3: Memory synthesis loading overlay
 * Gün sonu özetlemesi tamamlanana kadar gösterilir
 * @param statusMessage Dinamik durum mesajı ("Lokal motor yükleniyor...", "Anılar özetleniyor...")
 */
@Composable
fun MemorySynthesisLoadingOverlay(statusMessage: String = "") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = JournalLightBrown,
                strokeWidth = 6.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = rememberLocalizedText("processing_daily_memories"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = JournalLightBrown,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dinamik durum mesajı - statusMessage boşsa varsayılan mesajı göster
            Text(
                text = if (statusMessage.isNotEmpty()) statusMessage else "Flash Memory özeti oluşturuluyor\nLütfen bekleyin...",
                fontSize = 14.sp,
                color = JournalLightBrown.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@UnstableApi
@Composable
fun JournalIntroVideoH1(onIntroComplete: () -> Unit) {
    val context = LocalContext.current

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(Unit) {
        val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.intro_animation}")
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onIntroComplete()
                }
            }
        }
        exoPlayer.addListener(listener)
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = onIntroComplete,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JournalBrown)
        ) {
            Text(rememberLocalizedText("skip"), color = Color.White)
        }
    }
}

@UnstableApi
@Composable
fun JournalClosedBookH2(onOpenJournal: () -> Unit) {
    val demonBgResources = remember {
        listOf(
            R.drawable.demon_bg_00, R.drawable.demon_bg_01, R.drawable.demon_bg_02,
            R.drawable.demon_bg_03, R.drawable.demon_bg_04, R.drawable.demon_bg_05,
            R.drawable.demon_bg_06, R.drawable.demon_bg_07, R.drawable.demon_bg_08,
            R.drawable.demon_bg_09, R.drawable.demon_bg_10, R.drawable.demon_bg_11,
            R.drawable.demon_bg_12, R.drawable.demon_bg_13, R.drawable.demon_bg_14,
            R.drawable.demon_bg_15, R.drawable.demon_bg_16, R.drawable.demon_bg_17
        )
    }
    var currentDemonBg by remember { mutableIntStateOf((0..17).random()) }

    LaunchedEffect(Unit) {
        while (true) {
            var newBgIndex: Int
            do {
                newBgIndex = (0 until demonBgResources.size).random()
            } while (newBgIndex == currentDemonBg && demonBgResources.size > 1)
            currentDemonBg = newBgIndex
            delay(1000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = demonBgResources[currentDemonBg]),
            contentDescription = "Demon Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Image(
            painter = painterResource(id = R.drawable.closed_mystical_book),
            contentDescription = "Mystical Journal Closed",
            modifier = Modifier
                .size(300.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onOpenJournal()
                },
            contentScale = ContentScale.Fit
        )
    }
}

@UnstableApi
@Composable
fun JournalAnimationBackgroundH3(
    currentDemonBg: Int,
    demonBgResources: List<Int>,
    exoPlayer: ExoPlayer
) {
    Image(
        painter = painterResource(id = demonBgResources[currentDemonBg]),
        contentDescription = "Demon Background",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    AndroidView(
        factory = { playerViewContext ->
            PlayerView(playerViewContext).apply {
                player = exoPlayer
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                alpha = 0.95f
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@UnstableApi
@Composable
fun JournalMainReadingInterfaceH4(
    journalViewModel: JournalViewModel,
    showResourceNotification: Boolean,
    lastGainedResource: String,
    collectedItems: CollectedItemsZ5?,
    currentPage: Int,
    @Suppress("UNUSED_PARAMETER") showPageSelector: Boolean,
    userInput: String,
    onBackToDashboard: (() -> Unit)?,
    onPageStateChange: (PageState) -> Unit,
    onCurrentPageChange: (Int) -> Unit,
    onShowPageSelectorChange: (Boolean) -> Unit,
    onUserInputChange: (String) -> Unit,
    onTimeDisplayExpandedChange: (Boolean) -> Unit,
    isTimeDisplayExpanded: Boolean,
    focusManager: FocusManager,
    combatState: com.example.isekaikuroshin.combat.CombatState // G97: Combat state for input control
) {
    val uiState by journalViewModel.uiState.collectAsState()
    val isEvenDay = uiState.currentDay % 2 == 0
    Image(
        painter = painterResource(id = if (isEvenDay) R.drawable.right_page else R.drawable.left_page),
        contentDescription = "Mystical Journal Page",
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = 0.95f,
                scaleY = 0.95f
            ),
        contentScale = ContentScale.Crop
    )

    if (uiState.isLoadingAIResponse) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = JournalBrown.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = rememberLocalizedText("ai_thinking"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showResourceNotification) {
        JournalContentAreaH4_2(
            lastGainedResource = lastGainedResource,
            onDismiss = { /* TODO */ }
        )
    }

    if (uiState.criticalWarnings.isNotEmpty()) {
        JournalNavigationButtonsH4_6(criticalWarnings = uiState.criticalWarnings)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        collectedItems?.let { collected: CollectedItemsZ5 ->
            if (collected.totalItems() > 0) {
                JournalCharacterDialogH4_5(collectedItems = collected)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 35.dp, end = 20.dp)
        ) {
            JournalCharacterPortraitH4_4(
                journalViewModel = journalViewModel,
                isTimeDisplayExpanded = isTimeDisplayExpanded,
                onToggleExpanded = onTimeDisplayExpandedChange
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = JournalDarkRed.copy(alpha = 0.4f), shape = CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (currentPage == 1) {
                            onPageStateChange(PageState.FINISHING)
                        } else {
                            onBackToDashboard?.invoke()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Dashboard",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = JournalBrown.copy(alpha = 0.3f), shape = CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (currentPage > 1) {
                            val isCurrentPageOdd = currentPage % 2 == 1
                            if (isCurrentPageOdd) {
                                onPageStateChange(PageState.REVERSE_PAGE_TURNING)
                            } else {
                                onCurrentPageChange(currentPage - 1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Page",
                    tint = if (currentPage == 1) Color.Gray else JournalBrown,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = JournalBrown.copy(alpha = 0.4f), shape = CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onShowPageSelectorChange(true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$currentPage", color = JournalBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            val canGoForward = currentPage < uiState.storyPages.size
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = JournalBrown.copy(alpha = 0.3f), shape = CircleShape)
                    .clickable(enabled = canGoForward, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        val isCurrentPageEven = currentPage % 2 == 0
                        if (isCurrentPageEven) {
                            if (currentPage < uiState.storyPages.size) {
                                onPageStateChange(PageState.PAGE_TURNING)
                            }
                        } else {
                            if (currentPage + 1 <= uiState.storyPages.size) {
                                onCurrentPageChange(currentPage + 1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Page",
                    tint = if (canGoForward) JournalBrown else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 60.dp, end = 40.dp, top = 80.dp, bottom = 100.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val currentPageText = uiState.storyPages.getOrNull(currentPage - 1) ?: ""
                val scrollState = rememberLazyListState()

                // TODO-FIX-JOURNAL-01: Debug log - Hangi sayfa render ediliyor
                LaunchedEffect(currentPage, uiState.storyPages.size) {
                    GameLogger.logVerbose("JOURNAL-UI", "Rendering page $currentPage of ${uiState.storyPages.size}")
                    GameLogger.logVerbose("JOURNAL-UI", "Current page text length: ${currentPageText.length}")
                    if (currentPageText.isNotEmpty()) {
                        GameLogger.logVerbose("JOURNAL-UI", "Page content preview: ${currentPageText.take(100)}...")
                    } else {
                        GameLogger.logVerbose("JOURNAL-UI", "⚠️ WARNING: Current page text is EMPTY!")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = scrollState
                ) {
                    item {
                        Text(
                            text = currentPageText,
                            color = JournalDarkBrown,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Left,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    if (uiState.isLoadingAIResponse) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = JournalBrown,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = rememberLocalizedText("ai_thinking"),
                                    color = JournalBrown,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                // TODO-FIX-06: Recomposition loop düzeltmesi - currentPageText key olarak kullanılmamalı
                // currentPageText her recomposition'da yeniden hesaplanıyor ve bu sonsuz döngüye yol açıyor
                // Bunun yerine sadece storyPages boyutu değiştiğinde scroll yapmalıyız
                LaunchedEffect(uiState.storyPages.size, currentPage) {
                    if (scrollState.layoutInfo.totalItemsCount > 0) {
                        scrollState.animateScrollToItem(scrollState.layoutInfo.totalItemsCount - 1)
                    }
                }

                val canAcceptMoreInputs = uiState.inCombat || uiState.inputCountThisTimeOfDay < 1
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rememberLocalizedText("what_do_you_want_to_do"),
                            color = JournalBrown,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (canAcceptMoreInputs) "${1 - uiState.inputCountThisTimeOfDay} Hakkın Kaldı" else "Hakkın Kalmadı",
                            color = if (canAcceptMoreInputs) JournalSuccess else JournalWarning,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val keyboardController = LocalSoftwareKeyboardController.current

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { newValue ->
                            if (canAcceptMoreInputs && !uiState.isLoadingAIResponse && combatState is com.example.isekaikuroshin.combat.CombatState.Idle) {
                                onUserInputChange(newValue)
                            }
                        },
                        placeholder = { Text(rememberLocalizedText("write_your_action"), color = JournalLightBrown) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JournalDarkBrown,
                            unfocusedTextColor = JournalDarkBrown,
                            focusedBorderColor = JournalBrown,
                            unfocusedBorderColor = JournalLightBrown,
                            disabledTextColor = JournalDarkBrown.copy(alpha = 0.5f),
                            disabledBorderColor = JournalLightBrown.copy(alpha = 0.3f)
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            platformImeOptions = PlatformImeOptions(
                                privateImeOptions = "defaultInputmethod=com.android.inputmethod.latin/.LatinIME;subtype=tr_TR:TurkishKeyboard"
                            )
                        ),
                        supportingText = {
                            Text(
                                text = rememberLocalizedText("turkish_chars_supported"),
                                fontSize = 10.sp,
                                color = JournalSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        singleLine = false,
                        // G97: Disable during combat
                        enabled = canAcceptMoreInputs && !uiState.isLoadingAIResponse && combatState is com.example.isekaikuroshin.combat.CombatState.Idle
                    )
                    Button(
                        onClick = {
                            if (canAcceptMoreInputs && !uiState.isLoadingAIResponse && userInput.isNotBlank() && combatState is com.example.isekaikuroshin.combat.CombatState.Idle) {
                                journalViewModel.processUserActions(userInput)
                                focusManager.clearFocus()
                                onUserInputChange("")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAcceptMoreInputs && !uiState.isLoadingAIResponse && combatState is com.example.isekaikuroshin.combat.CombatState.Idle) JournalBrown else JournalBrown.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        // G97: Disable during combat
                        enabled = canAcceptMoreInputs && !uiState.isLoadingAIResponse && userInput.isNotBlank() && combatState is com.example.isekaikuroshin.combat.CombatState.Idle
                    ) {
                        Text(
                            text = if (uiState.isLoadingAIResponse) "Yükleniyor..." else if (canAcceptMoreInputs) rememberLocalizedText("perform_action") else rememberLocalizedText("time_is_up"),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun JournalContentAreaH4_2(
    lastGainedResource: String,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(3000)
        onDismiss()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = JournalSuccess)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎁 Gained: $lastGainedResource",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@UnstableApi
@Composable
fun JournalCharacterPortraitH4_4(
    journalViewModel: JournalViewModel,
    isTimeDisplayExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit
) {
    val uiState by journalViewModel.uiState.collectAsState()
    Card(
        modifier = Modifier
            .clickable { onToggleExpanded(!isTimeDisplayExpanded) },
        colors = CardDefaults.cardColors(
            containerColor = JournalBrown.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (isTimeDisplayExpanded) {
                Text(
                    text = "📍 ${uiState.currentLocationString}",
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "🕐 ${rememberLocalizedText("day")} ${uiState.currentDay}",
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "☀️ ${uiState.currentTimeOfDay.displayName}",
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "🌡️ ${rememberLocalizedText("sunny")}",
                    color = Color.White,
                    fontSize = 9.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "📊 ${rememberLocalizedText("resources_ok")}",
                    color = Color.White,
                    fontSize = 9.sp,
                    textAlign = TextAlign.End
                )

                // G118: Active Status Effects (Emoji row)
                // NOTE: Placeholder - Phase 2'de persistent storage'dan gelecek
                val activeEffects = emptyList<com.example.isekaikuroshin.data.ActiveStatusEffectG118>()

                if (activeEffects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${rememberLocalizedText("active_effects")}: ",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            textAlign = TextAlign.End
                        )
                        activeEffects.take(3).forEach { effect: com.example.isekaikuroshin.data.ActiveStatusEffectG118 ->
                            val template = com.example.isekaikuroshin.data.StatusEffectTemplateRegistry.getTemplate(effect.effectId)
                            val displayEmoji = effect.getDisplayEmoji(template)
                            Text(
                                text = displayEmoji,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                        if (activeEffects.size > 3) {
                            Text(
                                text = "+${activeEffects.size - 3}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "📍 ${rememberLocalizedText("day").take(1)}${uiState.currentDay}",
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@UnstableApi
@Composable
fun JournalCharacterDialogH4_5(
    collectedItems: CollectedItemsZ5
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = JournalSuccess.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "📦 Inventory",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${rememberLocalizedText("total_items")} ${collectedItems.totalItems()}",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@UnstableApi
@Composable
fun JournalNavigationButtonsH4_6(
    criticalWarnings: List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (criticalWarnings.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "⚠️ Critical Warnings",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    criticalWarnings.forEach { warning ->
                        Text(
                            text = "• $warning",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun JournalPageSelectorDialogH5(
    journalViewModel: JournalViewModel,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by journalViewModel.uiState.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = rememberLocalizedText("quick_page_navigation"), color = JournalBrown, style = MaterialTheme.typography.headlineSmall) },
        text = {
            LazyColumn(modifier = Modifier.height(200.dp)) {
                val pageCount = uiState.storyPages.size
                val pageOptions = (1..pageCount).toList()

                items(pageOptions) { pageNum ->
                    TextButton(
                        onClick = {
                            onPageSelected(pageNum)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${rememberLocalizedText("page")} $pageNum",
                            color = if (pageNum == currentPage) JournalBrown else JournalLightBrown,
                            fontWeight = if (pageNum == currentPage) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(rememberLocalizedText("close"), color = JournalBrown) } },
        containerColor = Color(0xFFF5F5DC).copy(alpha = 0.95f)
    )
}

@Composable
fun TacticalMomentOverlay(enemies: List<Enemy>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { /* Consume clicks */ }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = rememberLocalizedText("battle_status"),
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                enemies.forEach { enemy ->
                    EnemyStatusRow(enemy)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Fireplace, contentDescription = "Ateş Topu", tint = Color.Red)
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.PanTool, contentDescription = "Sersemletici Darbe", tint = Color.Yellow)
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.FlashOn, contentDescription = "Yıldırım", tint = Color.Cyan)
                    }
                }
            }
        }
    }
}

@Composable
fun EnemyStatusRow(enemy: Enemy) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = enemy.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = "${enemy.hp} / ${enemy.maxHp}", color = Color.White)
        }
        LinearProgressIndicator(
            progress = { enemy.hp.toFloat() / enemy.maxHp.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = Color.Red,
            trackColor = Color.Gray
        )
    }
}
