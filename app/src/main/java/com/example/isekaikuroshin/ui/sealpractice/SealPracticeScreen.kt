package com.example.isekaikuroshin.ui.sealpractice

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.SealDifficulty
import com.example.isekaikuroshin.engine.PerformanceLevel
import com.example.isekaikuroshin.ui.components.CameraPreview
import com.example.isekaikuroshin.utils.DeviceUtils
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * Seal Practice Screen - Kadim Mühür Teknikleri Pratik Ekranı
 *
 * Bu ekran, oyuncuların el hareketleriyle mühür pratiği yapabilecekleri ana ekrandır.
 *
 * Özellikler:
 * - Mevcut mühürlerin listesi
 * - Real-time kamera ile el algılama
 * - Anlık geri bildirim ve doğruluk skoru
 * - Ustalık seviyesi takibi
 * - Başarı animasyonları
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealPracticeScreen(
    navController: NavController,
    viewModel: SealPracticeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()

    // Log screen rendering
    LaunchedEffect(Unit) {
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("========================================")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("SealPracticeScreen composable rendered")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("========================================")
    }

    // Tutorial dialog state (ilk kez girişte göster)
    var showTutorial by remember { mutableStateOf(true) }

    // Emulator detection for smart MediaPipe initialization
    val isEmulator = remember { DeviceUtils.isEmulator() }

    // HandDetectorHelper - Hata durumunda null döndür
    val handDetectorHelper = remember(isEmulator) {
        try {
            Log.d(TAG, "🔧 HandDetectorHelper oluşturuluyor... (Emulator: $isEmulator)")
            HandDetectorHelper(context, isEmulator = isEmulator).also {
                Log.d(TAG, "✅ HandDetectorHelper başarıyla oluşturuldu")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ HandDetectorHelper oluşturma hatası: ${e.message}", e)
            null
        }
    }

    // Cleanup
    DisposableEffect(handDetectorHelper) {
        Log.d(TAG, "📌 DisposableEffect başlatıldı")
        onDispose {
            try {
                Log.d(TAG, "🧹 Kaynaklar temizleniyor...")
                handDetectorHelper?.close()
                Log.d(TAG, "✅ SealPracticeScreen disposed, resources cleaned")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cleanup hatası: ${e.message}", e)
            }
        }
    }

    // Eğer HandDetectorHelper oluşturulamadıysa hata ekranı göster
    if (handDetectorHelper == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(rememberLocalizedText("error")) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = rememberLocalizedText("back")
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = rememberLocalizedText("hand_detection_failed"),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = rememberLocalizedText("mediapipe_unavailable"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { navController.navigateUp() }) {
                        Text(rememberLocalizedText("go_back"))
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = rememberLocalizedText("ancient_seal_techniques"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "⬅️ Geri butonu tıklandı")
                        if (uiState.sessionActive) {
                            viewModel.endSession()
                        } else {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            when {
                // Oturum aktif değil - Mühür seçim ekranı
                !uiState.sessionActive -> {
                    SealSelectionScreen(
                        availableSeals = uiState.availableSeals,
                        onSealSelected = { seal ->
                            viewModel.startPracticeSession(seal)
                        },
                        navController = navController
                    )
                }

                // Oturum özeti göster
                uiState.sessionSummary != null -> {
                    SessionSummaryDialog(
                        summary = uiState.sessionSummary!!,
                        onDismiss = {
                            viewModel.resetSession()
                        },
                        onTryAgain = {
                            uiState.selectedSeal?.let { seal ->
                                viewModel.resetSession()
                                viewModel.startPracticeSession(seal)
                            }
                        }
                    )
                }

                // Oturum aktif - Pratik ekranı
                uiState.selectedSeal != null -> {
                    PracticeSessionScreen(
                        seal = uiState.selectedSeal!!,
                        uiState = uiState,
                        handDetectorHelper = handDetectorHelper,
                        lifecycleOwner = lifecycleOwner,
                        onHandDetected = { result ->
                            viewModel.processHandDetection(result)
                        },
                        onEndSession = {
                            viewModel.endSession()
                        }
                    )
                }
            }

            // Ustalık diyalogu
            if (uiState.showMasteryDialog) {
                MasteryAchievedDialog(
                    message = uiState.masteryMessage,
                    onDismiss = {
                        viewModel.dismissMasteryDialog()
                    }
                )
            }

            // Tutorial dialog (ilk kez göster)
            if (showTutorial && !uiState.sessionActive) {
                SealPracticeTutorialDialog(
                    onDismiss = { showTutorial = false }
                )
            }
        }
    }
}

/**
 * Mühür Seçim Ekranı
 */
@Composable
fun SealSelectionScreen(
    availableSeals: List<Seal>,
    onSealSelected: (Seal) -> Unit,
    @Suppress("UNUSED_PARAMETER") navController: NavController? = null
) {
    // Log seal selection screen
    LaunchedEffect(availableSeals.size) {
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("SealSelectionScreen: ${availableSeals.size} seals available")
        if (availableSeals.isEmpty()) {
            com.example.isekaikuroshin.utils.GameLogger.logSealPractice("⚠️ WARNING: No seals available! Check initializeDefaultSeals()")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // G27-FIX: Kalibrasyon Merkezi butonu KALDIRILDI (artık kullanılmıyor)

        // Başlık
        Text(
            text = rememberLocalizedText("select_seal"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = rememberLocalizedText("select_seal_practice"),
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (availableSeals.isEmpty()) {
            // Hiç mühür yoksa
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 64.sp
                    )
                    Text(
                        text = rememberLocalizedText("no_seals_unlocked"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = rememberLocalizedText("level_up_unlock"),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Mühür listesi
            val practiceTierText = rememberLocalizedText("practice_tier")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pratik Alanı Bölümü (Yeni!)
                val practiceSeals = availableSeals.filter { it.tier == practiceTierText }
                if (practiceSeals.isNotEmpty()) {
                    item {
                        PracticeSectionHeader()
                    }
                    items(practiceSeals) { seal ->
                        SealCard(
                            seal = seal,
                            onClick = { onSealSelected(seal) },
                            isPracticeMode = true
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.3f),
                            thickness = 2.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Element Mühürleri Bölümü
                val elementSeals = availableSeals.filter { it.tier != practiceTierText }
                if (elementSeals.isNotEmpty()) {
                    item {
                        ElementSectionHeader()
                    }
                    items(elementSeals) { seal ->
                        SealCard(
                            seal = seal,
                            onClick = { onSealSelected(seal) },
                            isPracticeMode = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pratik Alanı Başlık
 */
@Composable
fun PracticeSectionHeader() {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎯 ${rememberLocalizedText("practice_tier")} ${rememberLocalizedText("area")}",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.2f)
            ) {
                Text(
                    text = rememberLocalizedText("beginner"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = rememberLocalizedText("test_basic_gestures"),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Element Mühürleri Başlık
 */
@Composable
fun ElementSectionHeader() {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "⚡ ${rememberLocalizedText("element_seals")}",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF2196F3)
        )
        Text(
            text = rememberLocalizedText("powerful_combat_seals"),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Mühür Kartı
 */
@Composable
fun SealCard(
    seal: Seal,
    onClick: () -> Unit,
    isPracticeMode: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isPracticeMode) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isPracticeMode) {
            Color(0xFF2A3E2A).copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        },
        shadowElevation = if (isPracticeMode) 8.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mühür ikonu
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                getDifficultyColor(seal.difficulty),
                                getDifficultyColor(seal.difficulty).copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🖐️",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Mühür bilgileri
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = rememberLocalizedText(seal.nameKey),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = rememberLocalizedText(seal.descriptionKey),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ustalık progress bar
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { seal.masteryLevel / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = getDifficultyColor(seal.difficulty),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${seal.masteryLevel}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Zorluk rozeti
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getDifficultyColor(seal.difficulty).copy(alpha = 0.3f)
            ) {
                Text(
                    text = rememberLocalizedText("seal_difficulty_${seal.difficulty.name.lowercase()}"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = getDifficultyColor(seal.difficulty),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Pratik Oturumu Ekranı
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun PracticeSessionScreen(
    seal: Seal,
    uiState: SealPracticeUiState,
    handDetectorHelper: HandDetectorHelper,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onHandDetected: (com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult) -> Unit,
    onEndSession: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current

    // Hand detection sonucu
    var currentHandResult by remember { mutableStateOf<com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(640) }
    var imageHeight by remember { mutableIntStateOf(480) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // Hand detection sonuçlarını dinle (LaunchedEffect ile)
    LaunchedEffect(handDetectorHelper) {
        handDetectorHelper.results.collect { result ->
            currentHandResult = result
            onHandDetected(result)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Kamera önizlemesi
        CameraPreview(
            lifecycleOwner = lifecycleOwner,
            imageAnalyzer = { imageProxy ->
                try {
                    val bitmap = imageProxy.toBitmap()
                    val timestampMs = System.currentTimeMillis()

                    // Update image dimensions and rotation
                    imageWidth = imageProxy.width
                    imageHeight = imageProxy.height
                    rotationDegrees = imageProxy.imageInfo.rotationDegrees

                    Log.d(TAG, "📸 ImageProxy: ${imageWidth}x${imageHeight}, rotation: ${rotationDegrees}°")

                    // Hand detection (non-blocking, sends to Flow)
                    handDetectorHelper.detectHands(bitmap, timestampMs)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Hand detection error: ${e.message}", e)
                } finally {
                    imageProxy.close()
                }
            },
            onViewSizeChanged = { _, _ -> }
            // cameraSelector parametresi kaldırıldı - CameraPreview default değeri kullanacak (FRONT_CAMERA)
        )

        // Hand landmark overlay (el iskeletini göster)
        // ⚡⚡⚡ KÖKTƏN ÇÖZÜM: MediaPipe'dan gelen RAW veri DOĞRUDAN çiziliyor!
        // ZERO filtreleme, ZERO smoothing, ZERO kalman - sadece RAW çizim!
        HandLandmarkOverlay(
            handResult = currentHandResult,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = 0f,  // Canvas kendi boyutunu kullanır
            viewHeight = 0f,
            rotationDegrees = rotationDegrees,
            isFrontCamera = true
        )

        // Üst bilgi paneli
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Mühür adı
                Text(
                    text = rememberLocalizedText(seal.nameKey),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // İstatistikler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = rememberLocalizedText("attempts"),
                        value = uiState.attemptCount.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    StatItem(
                        label = rememberLocalizedText("successful"),
                        value = uiState.successCount.toString(),
                        color = Color(0xFF00FF88)
                    )

                    // CANLI DOĞRULUK GÖSTERGESİ (Yeni!)
                    StatItem(
                        label = rememberLocalizedText("accuracy"),
                        value = "${(uiState.currentAccuracy * 100).toInt()}%",
                        color = getAccuracyColor(uiState.currentAccuracy)
                    )
                }

                // Canlı doğruluk çubuğu (Progress Bar)
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rememberLocalizedText("live_match"),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = when {
                                uiState.currentAccuracy >= 0.9f -> "🌟 Mükemmel!"
                                uiState.currentAccuracy >= 0.7f -> "✅ İyi"
                                uiState.currentAccuracy >= 0.5f -> "⚠️ Orta"
                                else -> "❌ Zayıf"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = getAccuracyColor(uiState.currentAccuracy)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { uiState.currentAccuracy.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = getAccuracyColor(uiState.currentAccuracy),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Başarı animasyonu
        if (uiState.showSuccessAnimation) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.8f),
                contentAlignment = Alignment.Center
            ) {
                SuccessAnimation()
            }
        }

        // Geri bildirim paneli (Alt)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = getFeedbackBackgroundColor(uiState.lastPerformanceLevel),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.currentFeedback,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bitir butonu (en az 3 deneme yapıldıysa)
                if (uiState.attemptCount >= 3) {
                    Button(
                        onClick = onEndSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A4C93)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🏁 Oturumu Bitir",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * İstatistik Item
 */
@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Oturum Özeti Diyalogu
 */
@Composable
fun SessionSummaryDialog(
    summary: SessionSummary,
    onDismiss: () -> Unit,
    onTryAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = rememberLocalizedText("session_completed"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryRow(rememberLocalizedText("duration"), "${summary.duration} ${rememberLocalizedText("minutes")}")
                SummaryRow(rememberLocalizedText("total_attempts"), summary.attempts.toString())
                SummaryRow(rememberLocalizedText("successes"), summary.successes.toString())
                SummaryRow(rememberLocalizedText("success_rate"), "${summary.successRate}%")
                SummaryRow(rememberLocalizedText("best_accuracy"), "${(summary.bestAccuracy * 100).toInt()}%")
            }
        },
        confirmButton = {
            Button(onClick = onTryAgain) {
                Text(rememberLocalizedText("try_again"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(rememberLocalizedText("close"))
            }
        }
    )
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Medium)
        Text(text = value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Ustalık Kazanıldı Diyalogu
 */
@Composable
fun MasteryAchievedDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = rememberLocalizedText("mastery_achieved"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(text = message, textAlign = TextAlign.Center)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(rememberLocalizedText("great"))
            }
        }
    )
}

/**
 * Seal Practice Tutorial Dialog
 */
@Composable
fun SealPracticeTutorialDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(text = "🖐️", fontSize = 48.sp)
        },
        title = {
            Text(
                text = rememberLocalizedText("ancient_seal_techniques"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = rememberLocalizedText("seal_welcome_text"),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = rememberLocalizedText("how_it_works"),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = rememberLocalizedText("seal_tutorial_steps"),
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = rememberLocalizedText("seal_mastery_tip"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(rememberLocalizedText("understood"))
            }
        }
    )
}

/**
 * Başarı Animasyonu
 */
@Composable
fun SuccessAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF00FF88),
            modifier = Modifier.size((100 * scale).dp)
        )
    }
}

// Helper Functions

fun getDifficultyColor(difficulty: SealDifficulty): Color {
    return when (difficulty) {
        SealDifficulty.NOVICE -> Color(0xFF4CAF50)
        SealDifficulty.INTERMEDIATE -> Color(0xFF2196F3)
        SealDifficulty.ADVANCED -> Color(0xFF9C27B0)
        SealDifficulty.MASTER -> Color(0xFFFF9800)
        SealDifficulty.LEGENDARY -> Color(0xFFFFD700)
    }
}

fun getAccuracyColor(accuracy: Float): Color {
    return when {
        accuracy >= 0.9f -> Color(0xFF00FF88)
        accuracy >= 0.7f -> Color(0xFFFFD700)
        accuracy >= 0.5f -> Color(0xFFFF9800)
        else -> Color(0xFFFF5252)
    }
}

fun getFeedbackBackgroundColor(performanceLevel: PerformanceLevel?): Color {
    return when (performanceLevel) {
        PerformanceLevel.PERFECT -> Color(0xFF00FF88).copy(alpha = 0.9f)
        PerformanceLevel.GOOD -> Color(0xFF4CAF50).copy(alpha = 0.9f)
        PerformanceLevel.ACCEPTABLE -> Color(0xFF2196F3).copy(alpha = 0.9f)
        PerformanceLevel.FAILED -> Color(0xFFFF5252).copy(alpha = 0.9f)
        null -> Color.Black.copy(alpha = 0.7f)
    }
}

// ImageProxy to Bitmap extension
private fun androidx.camera.core.ImageProxy.toBitmap(): android.graphics.Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private const val TAG = "SealPracticeScreen"
