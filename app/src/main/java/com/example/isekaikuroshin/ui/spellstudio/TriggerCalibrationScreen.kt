package com.example.isekaikuroshin.ui.spellstudio

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.data.spell.SpellStep
import com.example.isekaikuroshin.data.spell.SpellTrigger
import com.example.isekaikuroshin.engine.GestureRecognitionEngine
import com.example.isekaikuroshin.ui.components.CameraPreview
import com.example.isekaikuroshin.ui.components.XPGainOverlay
import com.example.isekaikuroshin.ui.components.rememberXPOverlayState
import com.example.isekaikuroshin.ui.sealpractice.HandDetectorHelper
import com.example.isekaikuroshin.ui.sealpractice.HandLandmarkOverlay
import com.example.isekaikuroshin.utils.DeviceUtils
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Tetikleyici Kalibrasyon Ekranı
 *
 * Kullanıcının el hareketini veya sesli komutu kaydettiği ekran
 * VFX Atölyesi'ndeki çalışan kamera mantığı buraya entegre edildi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun TriggerCalibrationScreen(
    step: SpellStep,
    viewModel: SpellStudioViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // El sayısı seçimi için state
    var selectedHandMode by remember { mutableStateOf<HandMode?>(null) }

    // Emulator detection
    val isEmulator = remember { DeviceUtils.isEmulator() }

    // HandDetectorHelper (sadece kamera modu için)
    val handDetectorHelper = remember(isEmulator, selectedHandMode) {
        if (selectedHandMode != null && (step.trigger is SpellTrigger.SingleHandGesture || step.trigger is SpellTrigger.DoubleHandGesture)) {
            try {
                Log.d(TAG, "🔧 HandDetectorHelper creating for spell trigger calibration...")
                HandDetectorHelper(context, isEmulator = isEmulator).also {
                    // El sayısını ayarla
                    when (selectedHandMode) {
                        HandMode.SINGLE -> it.setNumHands(1)
                        HandMode.DOUBLE -> it.setNumHands(2)
                        null -> {}
                    }
                    Log.d(TAG, "✅ HandDetectorHelper created with ${selectedHandMode?.handCount} hand(s)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ HandDetectorHelper creation error: ${e.message}", e)
                null
            }
        } else {
            null
        }
    }

    // Cleanup
    DisposableEffect(handDetectorHelper) {
        onDispose {
            try {
                handDetectorHelper?.close()
                Log.d(TAG, "✅ HandDetectorHelper disposed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cleanup error: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tetikleyici Ayarla") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.returnToRecipeEditor() }) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // El hareketi kalibrasyonu - kamera modu
                (step.trigger is SpellTrigger.SingleHandGesture || step.trigger is SpellTrigger.DoubleHandGesture) && selectedHandMode != null -> {
                    if (handDetectorHelper != null) {
                        GestureCalibrationCameraContent(
                            step = step,
                            viewModel = viewModel,
                            handDetectorHelper = handDetectorHelper,
                            lifecycleOwner = lifecycleOwner,
                            handMode = selectedHandMode!!,
                            onBackPressed = {
                                selectedHandMode = null
                            }
                        )
                    } else {
                        // Hata durumu
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = rememberLocalizedText("camera_failed"),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { selectedHandMode = null }) {
                                Text(rememberLocalizedText("back"))
                            }
                        }
                    }
                }

                // El hareketi - seçim ekranı
                step.trigger is SpellTrigger.SingleHandGesture || step.trigger is SpellTrigger.DoubleHandGesture -> {
                    HandModeSelectionContent(
                        trigger = step.trigger,
                        onModeSelected = { mode ->
                            selectedHandMode = mode
                        },
                        onBackPressed = {
                            viewModel.returnToRecipeEditor()
                        }
                    )
                }

                // Sesli komut
                step.trigger is SpellTrigger.VoiceCommand -> {
                    VoiceCommandCalibrationContent(
                        trigger = step.trigger,
                        viewModel = viewModel
                    )
                }

                // Zamanlayıcı
                step.trigger is SpellTrigger.Timer -> {
                    TimerConfigurationContent(
                        trigger = step.trigger,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

/**
 * El Modu Seçim İçeriği
 */
@Composable
private fun HandModeSelectionContent(
    trigger: SpellTrigger,
    onModeSelected: (HandMode) -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PanTool,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "El Hareketi Kalibrasyonu",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = rememberLocalizedText("how_many_hands"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Tek El Butonu
        Button(
            onClick = { onModeSelected(HandMode.SINGLE) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PanTool, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tek El Hareketi")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Çift El Butonu
        Button(
            onClick = { onModeSelected(HandMode.DOUBLE) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.BackHand, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Çift El Hareketi")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBackPressed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(rememberLocalizedText("back"))
        }
    }
}

/**
 * Hareket Kalibrasyon Kamera İçeriği
 *
 * ⚡⚡⚡ VFX WORKSHOP ENTEGRASYONU:
 * - ViewModel ile veri kalıcılığı
 * - Canlı doğruluk hesaplama
 * - GestureRecognitionEngine entegrasyonu
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
private fun GestureCalibrationCameraContent(
    step: SpellStep,
    viewModel: SpellStudioViewModel,
    handDetectorHelper: HandDetectorHelper,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    handMode: HandMode,
    onBackPressed: () -> Unit
) {
    // ⚡⚡⚡ YENİ: GestureRecognitionEngine (Hilt Inject)
    val recognitionEngine = remember {
        GestureRecognitionEngine()
    }

    // Hand detection state
    var currentHandResult by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(640) }
    var imageHeight by remember { mutableIntStateOf(480) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // ⚡⚡⚡ YENİ: ViewModel'den Seal'ı yükle
    val calibrationSeals by viewModel.calibrationSeals.collectAsState()
    val currentSeal = calibrationSeals[step.id]
    val savedGestureCount = currentSeal?.templateLandmarksList?.size ?: 0

    // ⚡⚡⚡ YENİ: Canlı doğruluk
    val currentAccuracy by viewModel.currentAccuracy.collectAsState()
    val accuracy = currentAccuracy[step.id] ?: 0f

    // Snackbar state
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // GÖREV 3: XP Overlay State
    val xpOverlayState = rememberXPOverlayState()
    val xpGainEvent by viewModel.xpGainEvent.collectAsState()

    // GÖREV 3: XP Gain Event'i dinle ve overlay göster
    LaunchedEffect(xpGainEvent) {
        xpGainEvent?.let { event ->
            xpOverlayState.show(
                xpAmount = event.xpAmount,
                elementType = event.elementType
            )
            // Event'i temizle (tek seferlik gösterim)
            viewModel.clearXPGainEvent()
        }
    }

    // ⚡⚡⚡ YENİ: Seal'ı yükle (ilk render'da)
    LaunchedEffect(step.id) {
        viewModel.loadOrCreateSealForStep(step.id)
    }

    // Hand detection sonuçlarını topla
    LaunchedEffect(handDetectorHelper) {
        handDetectorHelper.results.collect { result ->
            currentHandResult = result
            // ⚡⚡⚡ YENİ: Canlı doğruluk hesapla
            viewModel.processHandDetectionForStep(step.id, result, recognitionEngine)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        CameraPreview(
            lifecycleOwner = lifecycleOwner,
            imageAnalyzer = { imageProxy ->
                try {
                    val bitmap = imageProxy.toBitmap()
                    val timestampMs = System.currentTimeMillis()

                    imageWidth = imageProxy.width
                    imageHeight = imageProxy.height
                    rotationDegrees = imageProxy.imageInfo.rotationDegrees

                    handDetectorHelper.detectHands(bitmap, timestampMs)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Hand detection error: ${e.message}", e)
                } finally {
                    imageProxy.close()
                }
            },
            onViewSizeChanged = { _, _ -> }
        )

        // Hand landmark overlay
        HandLandmarkOverlay(
            handResult = currentHandResult,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = 0f,
            viewHeight = 0f,
            rotationDegrees = rotationDegrees,
            isFrontCamera = true
        )

        // Top info panel
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
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (handMode) {
                        HandMode.SINGLE -> "🖐️ Tek El Hareketi"
                        HandMode.DOUBLE -> "✌️ Çift El Hareketi"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Kaydedilen açı sayısı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLocalizedText("saved_angle"),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$savedGestureCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A4C93)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ⚡⚡⚡ YENİ: Canlı Doğruluk Göstergesi
                if (savedGestureCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rememberLocalizedText("accuracy"),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${(accuracy * 100).toInt()}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                accuracy >= 0.8f -> Color(0xFF00FF88)
                                accuracy >= 0.6f -> Color(0xFFFFB800)
                                else -> Color(0xFFFF5252)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // El Algılama Durumu
                val isHandDetected = currentHandResult != null &&
                    currentHandResult!!.landmarks().isNotEmpty() &&
                    currentHandResult!!.landmarks().size == handMode.handCount

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "El Durumu:",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (isHandDetected) Color(0xFF00FF88) else Color(0xFFFF5252),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (isHandDetected) "Algılandı" else "Algılanmadı",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHandDetected) Color(0xFF00FF88) else Color(0xFFFF5252)
                        )
                    }
                }
            }
        }

        // Bottom control panel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.8f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = rememberLocalizedText("hold_hand_and_save"),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ⚡⚡⚡ YENİ: Yeni Açı Ekle butonu (ViewModel ile)
                    Button(
                        onClick = {
                            if (currentHandResult != null) {
                                // ⚡⚡⚡ ViewModel fonksiyonunu çağır
                                viewModel.addAngleWithNormalization(step.id, currentHandResult, recognitionEngine)
                                snackbarMessage = "✅ Yeni açı eklendi!"
                                showSnackbar = true
                                Log.d(TAG, "🔵 Gesture saved! Total: ${savedGestureCount + 1}")
                            } else {
                                snackbarMessage = "❌ El algılanamadı!"
                                showSnackbar = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A4C93)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rememberLocalizedText("add_new_angle"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ⚡⚡⚡ YENİ: Sıfırla butonu (ViewModel ile)
                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllCalibrationsForStep(step.id)
                            snackbarMessage = "🗑️ Tüm açılar temizlendi"
                            showSnackbar = true
                        },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sıfırla",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Geri Dön Butonu
                OutlinedButton(
                    onClick = onBackPressed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rememberLocalizedText("go_back"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Snackbar
        if (showSnackbar) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp),
                action = {
                    TextButton(onClick = { showSnackbar = false }) {
                        Text(rememberLocalizedText("ok"), color = Color.White)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Text(snackbarMessage, color = Color.White)
            }
        }

        // GÖREV 3: XP Gain Overlay
        XPGainOverlay(
            xpAmount = xpOverlayState.xpAmount,
            elementType = xpOverlayState.elementType,
            visible = xpOverlayState.isVisible,
            onDismiss = { xpOverlayState.hide() }
        )
    }
}

/**
 * Sesli Komut Kalibrasyon İçeriği
 */
@Composable
private fun VoiceCommandCalibrationContent(
    trigger: SpellTrigger.VoiceCommand,
    viewModel: SpellStudioViewModel
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mikrofon ikonu (dinleme durumunda animasyonlu)
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = if (isListening) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = rememberLocalizedText("voice_command_recording"),
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dinleme durumu göstergesi
        if (isListening) {
            Text(
                text = "🎤 Dinliyorum...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = rememberLocalizedText("please_say_command"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = rememberLocalizedText("record_your_command"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tanınan metin göstergesi
        if (recognizedText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = rememberLocalizedText("recognized_command"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$recognizedText\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Mikrofonu Aç/Durdur butonu
        Button(
            onClick = {
                if (isListening) {
                    // Zaten dinliyorsa, dinlemeyi durdur
                    isListening = false
                    Log.d(TAG, "🎤 Voice recording stopped by user")
                } else {
                    // Yeni dinleme başlat
                    isListening = true
                    Log.d(TAG, "🎤 Starting voice recognition...")

                    try {
                        val recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                            putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        }

                        recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
                            override fun onReadyForSpeech(params: android.os.Bundle?) {
                                Log.d(TAG, "🎤 Ready for speech...")
                            }

                            override fun onBeginningOfSpeech() {
                                Log.d(TAG, "🎤 Speech started...")
                            }

                            override fun onResults(results: android.os.Bundle?) {
                                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                                val spokenText = matches?.firstOrNull() ?: ""
                                Log.i(TAG, "🎤 Recognized: \"$spokenText\"")

                                recognizedText = spokenText
                                isListening = false
                                snackbarMessage = "✅ Ses kaydı tamamlandı: \"$spokenText\""
                                showSnackbar = true

                                recognizer.destroy()
                            }

                            override fun onError(error: Int) {
                                Log.e(TAG, "❌ Speech recognition error: $error")
                                isListening = false
                                snackbarMessage = "❌ Ses tanıma hatası. Mikrofonu kontrol edin."
                                showSnackbar = true
                                recognizer.destroy()
                            }

                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onPartialResults(partialResults: android.os.Bundle?) {}
                            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                            override fun onEndOfSpeech() {
                                Log.d(TAG, "🎤 Speech ended")
                            }
                        })

                        recognizer.startListening(intent)
                        Log.i(TAG, "🎤 SpeechRecognizer started (offline mode)")

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to start SpeechRecognizer: ${e.message}", e)
                        isListening = false
                        snackbarMessage = "❌ Ses tanıma başlatılamadı"
                        showSnackbar = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isListening) "Dinlemeyi Durdur" else "Mikrofonu Aç")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { viewModel.returnToRecipeEditor() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(rememberLocalizedText("back"))
        }

        // Snackbar
        if (showSnackbar) {
            Snackbar(
                modifier = Modifier.padding(bottom = 16.dp),
                action = {
                    TextButton(onClick = { showSnackbar = false }) {
                        Text(rememberLocalizedText("ok"))
                    }
                }
            ) {
                Text(snackbarMessage)
            }
        }

        // FAZ 7: Level Up Dialog
        val levelUpSpellId by viewModel.levelUpSpellId.collectAsState()
        levelUpSpellId?.let { spellId ->
            val spellInfo = remember(spellId) { viewModel.getLearnedSpellInfo(spellId) }
            spellInfo?.let { (spellName, newLevel) ->
                LevelUpDialog(
                    spellName = spellName,
                    newLevel = newLevel,
                    onDismiss = { viewModel.dismissLevelUpDialog() }
                )
            }
        }
    }
}

/**
 * Zamanlayıcı Ayarlama İçeriği
 */
@Composable
private fun TimerConfigurationContent(
    trigger: SpellTrigger.Timer,
    viewModel: SpellStudioViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = rememberLocalizedText("timer_setting"),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = rememberLocalizedText("set_delay_time").replace("{delayMs}", trigger.delayMs.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = { viewModel.returnToRecipeEditor() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(rememberLocalizedText("back"))
        }
    }
}

/**
 * ImageProxy to Bitmap extension
 */
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

/**
 * El Modu
 */
enum class HandMode(val handCount: Int) {
    SINGLE(1),
    DOUBLE(2)
}

/**
 * HandDetectorHelper'a el sayısı ayarlama extension'ı
 */
private fun HandDetectorHelper.setNumHands(numHands: Int) {
    // TODO: HandDetectorHelper'da setNumHands metodunu implement et
    Log.d(TAG, "🔧 Setting number of hands to: $numHands")
}

private const val TAG = "TriggerCalibrationScreen"
