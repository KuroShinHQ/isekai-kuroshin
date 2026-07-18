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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import com.example.isekaikuroshin.data.spell.SpellRecipe
import com.example.isekaikuroshin.data.spell.SpellTrigger
import com.example.isekaikuroshin.engine.GestureRecognitionEngine
import com.example.isekaikuroshin.engine.vfx.ActionContext
import com.example.isekaikuroshin.engine.vfx.SpellActionExecutor
import com.example.isekaikuroshin.ui.components.CameraPreview
import com.example.isekaikuroshin.ui.sealpractice.HandDetectorHelper
import com.example.isekaikuroshin.ui.sealpractice.HandLandmarkOverlay
import com.example.isekaikuroshin.utils.DeviceUtils
import com.example.isekaikuroshin.utils.GestureDataConverter
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Büyü Test Ekranı
 *
 * Kullanıcının oluşturduğu büyüyü test ettiği ekran
 * VFX Atölyesi'ndeki çalışan test kamerası mantığı buraya entegre edildi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun SpellTestScreen(
    spell: SpellRecipe,
    viewModel: SpellStudioViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ⚡⚡⚡ KRİTİK FİX: ViewModel'den inject edilmiş SpellActionExecutor'ı kullan
    val spellActionExecutor = viewModel.spellActionExecutor
    Log.e(TAG, "🔥🔥🔥 SpellActionExecutor initialized from ViewModel: ${spellActionExecutor != null}")

    // Test durumu
    var testMode by remember { mutableStateOf<TestMode>(TestMode.PreTest) }

    // Emulator detection
    val isEmulator = remember { DeviceUtils.isEmulator() }

    // ⚡ G68 FIX: HandDetectorHelper - Hata durumunda null döndür (SealPracticeScreen pattern)
    // testMode kontrolü KALDIRILDI - başlangıçta oluştur, hata varsa early return ile ekranı gösterme
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
        onDispose {
            try {
                handDetectorHelper?.close()
                Log.d(TAG, "✅ HandDetectorHelper disposed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cleanup error: ${e.message}", e)
            }
        }
    }

    // ⚡ G68 FIX: Early return pattern (SealPracticeScreen'den kopyalandı)
    // Eğer HandDetectorHelper oluşturulamadıysa hata ekranı göster ve çık
    if (handDetectorHelper == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(rememberLocalizedText("error")) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitTestMode() }) {
                            Icon(Icons.Default.Close, rememberLocalizedText("close"))
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
                    Button(onClick = { viewModel.exitTestMode() }) {
                        Text(rememberLocalizedText("go_back"))
                    }
                }
            }
        }
        return  // ⚡ EARLY RETURN: Kamera koduna hiç girme!
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test: ${spell.name}") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.exitTestMode() }) {
                        Icon(Icons.Default.Close, rememberLocalizedText("close"))
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
            when (testMode) {
                TestMode.PreTest -> {
                    PreTestContent(
                        spell = spell,
                        onStartTest = { testMode = TestMode.Testing },
                        onExit = { viewModel.exitTestMode() }
                    )
                }
                TestMode.Testing -> {
                    // ⚡ G68 FIX: Null check kaldırıldı - early return sayesinde handDetectorHelper asla null değil
                    SpellTestCameraContent(
                        spell = spell,
                        handDetectorHelper = handDetectorHelper,
                        lifecycleOwner = lifecycleOwner,
                        onStopTest = { testMode = TestMode.PreTest },
                        spellActionExecutor = spellActionExecutor  // ⚡ YENİ: Executor'ı geç!
                    )
                }
            }
        }
    }
}

/**
 * Test Öncesi İçerik
 */
@Composable
private fun PreTestContent(
    spell: SpellRecipe,
    onStartTest: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Science,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = rememberLocalizedText("spell_test_screen"),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = rememberLocalizedText("spell_consists_of_steps")
                .replace("{name}", spell.name)
                .replace("{count}", spell.steps.size.toString()),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Test öncesi kontrol listesi
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = rememberLocalizedText("pre_test_checklist"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                spell.steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = rememberLocalizedText("step_number").replace("{number}", (index + 1).toString()),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${rememberLocalizedText("trigger_colon")} ${getTriggerName(step.trigger)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(rememberLocalizedText("start_test"))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(rememberLocalizedText("exit"))
        }
    }
}

/**
 * Spell Test Kamera İçeriği
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
private fun SpellTestCameraContent(
    spell: SpellRecipe,
    handDetectorHelper: HandDetectorHelper,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onStopTest: () -> Unit,
    spellActionExecutor: SpellActionExecutor  // ⚡ KRİTİK FİX: Non-null executor from ViewModel
) {
    // ⚡ Context'i Composable seviyesinde al
    val appContext = LocalContext.current

    // Hand detection state
    var currentHandResult by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(640) }
    var imageHeight by remember { mutableIntStateOf(480) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // Test state
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var testProgress by remember { mutableStateOf<TestProgress>(TestProgress.WaitingForTrigger) }
    var successfulSteps by remember { mutableIntStateOf(0) }

    // Snackbar state
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // ⚡⚡⚡ GERÇEK HAREKET TANIMA ENGINE
    val recognitionEngine = remember { GestureRecognitionEngine() }

    // ⚡⚡⚡ Hareket tanıma accuracy state
    var currentAccuracy by remember { mutableFloatStateOf(0f) }

    // ⚡⚡⚡ YENİ: El pozisyonu state (efekt için)
    var handPosition by remember { mutableStateOf<Offset?>(null) }

    // ⚡ CoroutineScope for launching coroutines from callbacks
    val coroutineScope = rememberCoroutineScope()

    // ⚡⚡⚡ YENİ: Timer ve VoiceCommand tetikleyicileri için LaunchedEffect
    LaunchedEffect(currentStepIndex) {
        if (currentStepIndex >= spell.steps.size) return@LaunchedEffect

        val currentStep = spell.steps[currentStepIndex]

        // Voice Command tetikleyicisi kontrolü
        // ⚡⚡⚡ REAL Android SpeechRecognizer (offline capable)
        if (currentStep.trigger is SpellTrigger.VoiceCommand && testProgress == TestProgress.WaitingForTrigger) {
            Log.i("VoiceCommand", "🎤 Voice command trigger: waiting for \"${currentStep.trigger.command}\"")
            snackbarMessage = "🎤 Söyle: \"${currentStep.trigger.command}\""
            showSnackbar = true

            try {
                // Android SpeechRecognizer kullan (cihazın kendi tanıma motoru)
                val recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(appContext)
                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "tr-TR") // Türkçe
                    putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Çevrimdışı tercih et
                }

                recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) {
                        Log.d("VoiceCommand", "🎤 Ready for speech...")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("VoiceCommand", "🎤 Speech started...")
                    }

                    override fun onResults(results: android.os.Bundle?) {
                        val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull() ?: ""
                        Log.i("VoiceCommand", "🎤 Recognized: \"$spokenText\"")

                        // Komut eşleşmesi kontrolü (büyük/küçük harf duyarsız)
                        if (spokenText.contains(currentStep.trigger.command, ignoreCase = true)) {
                            testProgress = TestProgress.TriggerDetected
                            Log.i("VoiceCommand", "✅ Voice command MATCHED!")

                            // Efekti tetikle
                            try {
                                val actionContext = ActionContext(
                                    handPosition = handPosition,
                                    screenSize = Offset(imageWidth.toFloat(), imageHeight.toFloat())
                                )
                                Log.d(TAG, "🎬 EXECUTE CALLED (VoiceCommand): Action=${currentStep.action::class.simpleName}")
                                spellActionExecutor.executeAction(currentStep.action, actionContext)
                                Log.d(TAG, "✅ Effect executed successfully: ${currentStep.action::class.simpleName}")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error executing action (VoiceCommand): ${e.message}", e)
                            }

                            currentStepIndex++
                            successfulSteps++
                            snackbarMessage = "✅ Sesli komut tanındı: \"$spokenText\""
                            showSnackbar = true

                            // ⚡ Coroutine scope kullanarak delay çağır
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000)
                                testProgress = TestProgress.WaitingForTrigger
                            }
                        } else {
                            Log.w("VoiceCommand", "❌ Voice command NOT matched. Expected: \"${currentStep.trigger.command}\", Got: \"$spokenText\"")
                            snackbarMessage = "❌ Yanlış komut. Tekrar dene: \"${currentStep.trigger.command}\""
                            showSnackbar = true
                        }
                        recognizer.destroy()
                    }

                    override fun onError(error: Int) {
                        Log.e("VoiceCommand", "❌ Speech recognition error: $error")
                        snackbarMessage = "❌ Ses tanıma hatası. Mikrofonu kontrol edin."
                        showSnackbar = true
                        recognizer.destroy()
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onPartialResults(partialResults: android.os.Bundle?) {}
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                    override fun onEndOfSpeech() {}
                })

                recognizer.startListening(intent)
                Log.i("VoiceCommand", "🎤 SpeechRecognizer started (offline mode)")

            } catch (e: Exception) {
                Log.e("VoiceCommand", "❌ Failed to start SpeechRecognizer: ${e.message}", e)
                snackbarMessage = "❌ Ses tanıma başlatılamadı"
                showSnackbar = true
            }
        }

        // Timer tetikleyicisi kontrolü
        if (currentStep.trigger is SpellTrigger.Timer && testProgress == TestProgress.WaitingForTrigger) {
            testProgress = TestProgress.TriggerDetected
            Log.d(TAG, "⏱️ Timer trigger: waiting ${currentStep.trigger.delayMs}ms")

            // Belirtilen süre kadar bekle
            kotlinx.coroutines.delay(currentStep.trigger.delayMs)

            Log.d(TAG, "✅ Timer completed!")

            // Efekti tetikle
            try {
                val context = ActionContext(
                    handPosition = handPosition,
                    screenSize = Offset(imageWidth.toFloat(), imageHeight.toFloat())
                )
                Log.d(TAG, "🎬 EXECUTE CALLED (Timer): Action=${currentStep.action::class.simpleName}")
                spellActionExecutor.executeAction(currentStep.action, context)
                Log.d(TAG, "✅ Effect executed successfully: ${currentStep.action::class.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error executing action (Timer): ${e.message}", e)
            }

            // Bir sonraki adıma geç
            currentStepIndex++
            successfulSteps++
            snackbarMessage = "✅ Adım $currentStepIndex başarılı! (Zamanlayıcı)"
            showSnackbar = true

            kotlinx.coroutines.delay(1000)
            testProgress = TestProgress.WaitingForTrigger
        }
    }

    // ⚡⚡⚡ YENİ: Hand detection sonuçlarını topla VE HAREKET TANIMA YAP!
    LaunchedEffect(handDetectorHelper, currentStepIndex) {
        handDetectorHelper.results.collect { result ->
            currentHandResult = result

            // Test tamamlandıysa işlem yapma
            if (currentStepIndex >= spell.steps.size) return@collect

            val currentStep = spell.steps[currentStepIndex]

            // Sadece el hareketi tetikleyicileri için kontrol yap
            if (result != null && result.landmarks().isNotEmpty() &&
                (currentStep.trigger is SpellTrigger.SingleHandGesture ||
                 currentStep.trigger is SpellTrigger.DoubleHandGesture)
            ) {
                // ⚡⚡⚡ GERÇEK HAREKET TANIMA: JSON → Seal → GestureRecognitionEngine
                val accuracy = when (currentStep.trigger) {
                    is SpellTrigger.SingleHandGesture -> {
                        try {
                            Log.d(TAG, "🔄 Processing single hand gesture")
                            // JSON gestureData'yı Seal'e dönüştür
                            val seal = GestureDataConverter.jsonToSeal(
                                gestureDataJson = currentStep.trigger.gestureData,
                                gestureName = currentStep.trigger.gestureName,
                                acceptanceThreshold = 0.75f
                            )

                            if (seal != null && result.landmarks().isNotEmpty()) {
                                // MediaPipe landmarks → NormalizedPoint
                                val liveHandLandmarks = result.landmarks()[0].map { landmark ->
                                    NormalizedPoint(
                                        x = landmark.x(),
                                        y = landmark.y(),
                                        z = landmark.z()
                                    )
                                }

                                // GERÇEK hareket tanıma!
                                val gestureResult = recognitionEngine.evaluateGesture(
                                    liveHandLandmarks = liveHandLandmarks,
                                    targetSeal = seal
                                )

                                Log.d(TAG, "✅ Gesture recognition successful: ${(gestureResult.accuracy * 100).toInt()}%")
                                gestureResult.accuracy
                            } else {
                                Log.w(TAG, "⚠️ Failed to convert JSON to Seal or no hand detected")
                                0f
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing single hand gesture: ${e.message}", e)
                            0f
                        }
                    }
                    is SpellTrigger.DoubleHandGesture -> {
                        try {
                            Log.d(TAG, "🔄 Processing double hand gesture")
                            // Çift el için: Her iki elin ayrı ayrı tanınması
                            if (result.landmarks().size >= 2) {
                                // Sol el için Seal
                                val leftSeal = GestureDataConverter.jsonToSeal(
                                    gestureDataJson = currentStep.trigger.leftGestureData,
                                    gestureName = currentStep.trigger.leftGestureName,
                                    acceptanceThreshold = 0.75f
                                )

                                // Sağ el için Seal
                                val rightSeal = GestureDataConverter.jsonToSeal(
                                    gestureDataJson = currentStep.trigger.rightGestureData,
                                    gestureName = currentStep.trigger.rightGestureName,
                                    acceptanceThreshold = 0.75f
                                )

                                if (leftSeal != null && rightSeal != null) {
                                    // Sol el landmarks
                                    val leftHandLandmarks = result.landmarks()[0].map { landmark ->
                                        NormalizedPoint(landmark.x(), landmark.y(), landmark.z())
                                    }

                                    // Sağ el landmarks
                                    val rightHandLandmarks = result.landmarks()[1].map { landmark ->
                                        NormalizedPoint(landmark.x(), landmark.y(), landmark.z())
                                    }

                                    // Her iki eli de tanı
                                    val leftResult = recognitionEngine.evaluateGesture(leftHandLandmarks, leftSeal)
                                    val rightResult = recognitionEngine.evaluateGesture(rightHandLandmarks, rightSeal)

                                    // Ortalama accuracy
                                    val avgAccuracy = (leftResult.accuracy + rightResult.accuracy) / 2f
                                    Log.d(TAG, "✅ Double hand recognition: L=${(leftResult.accuracy * 100).toInt()}% R=${(rightResult.accuracy * 100).toInt()}% Avg=${(avgAccuracy * 100).toInt()}%")
                                    avgAccuracy
                                } else {
                                    Log.w(TAG, "⚠️ Failed to convert double hand JSON to Seals")
                                    0f
                                }
                            } else {
                                Log.w(TAG, "⚠️ Double hand gesture but only ${result.landmarks().size} hand(s) detected")
                                0f
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing double hand gesture: ${e.message}", e)
                            0f
                        }
                    }
                    else -> 0f
                }

                currentAccuracy = accuracy

                // ⚡⚡⚡ FİX: El pozisyonunu kaydet (rotasyon dönüşümü ile)
                if (result.landmarks().isNotEmpty() && result.landmarks()[0].size > 12) {
                    val middleFinger = result.landmarks()[0][12]

                    // ⚡⚡⚡ KRİTİK FİX: Ekran koordinatlarına dönüştürme (90° rotasyon için özel mantık)
                    // Kamera görüntüsü 90° döndürülmüş, bu yüzden koordinat dönüşümü gerekli
                    val normalizedX = middleFinger.x()
                    val normalizedY = middleFinger.y()

                    // 90 derece rotasyon için: Kamera görüntüsü (imageWidth x imageHeight) iken
                    // ekranda (imageHeight x imageWidth) olarak görünüyor
                    val screenX = normalizedY * imageHeight
                    val screenY = (1f - normalizedX) * imageWidth

                    handPosition = Offset(screenX, screenY)

                    Log.d(TAG, "🎯 Hand position: Normalized($normalizedX, $normalizedY) -> Screen($screenX, $screenY)")
                    Log.d(TAG, "   Image size: ${imageWidth}x${imageHeight}, Rotation: $rotationDegrees°")
                }

                // Eşik değerin üzerindeyse başarılı!
                if (accuracy >= 0.75f && testProgress == TestProgress.WaitingForTrigger) {
                    testProgress = TestProgress.TriggerDetected

                    Log.d(TAG, "✅ Gesture recognized! Accuracy: ${(accuracy * 100).toInt()}%")

                    // ⚡⚡⚡ CRITICAL LOG for trigger chain debugging
                    Log.i(TAG, "⚡ SUCCESSFUL gesture for step ${currentStepIndex + 1}. Accuracy: ${(accuracy * 100).toInt()}%. Triggering action: ${currentStep.action::class.simpleName}")

                    // ⚡⚡⚡ KRİTİK FİX: Efekti TETİKLE!
                    try {
                        val context = ActionContext(
                            handPosition = handPosition,
                            screenSize = Offset(imageWidth.toFloat(), imageHeight.toFloat())
                        )
                        Log.e(TAG, "🎬🎬🎬 EXECUTE CALLED: Action=${currentStep.action::class.simpleName}, HandPos=$handPosition")
                        spellActionExecutor.executeAction(currentStep.action, context)
                        Log.e(TAG, "✅✅✅ Effect executed successfully: ${currentStep.action::class.simpleName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error executing action: ${e.message}", e)
                    }

                    // Bir sonraki adıma geç
                    currentStepIndex++
                    successfulSteps++
                    snackbarMessage = "✅ Adım $currentStepIndex başarılı! Doğruluk: ${(accuracy * 100).toInt()}%"
                    showSnackbar = true

                    // Bir süre bekle ve sonraki adıma hazır ol
                    kotlinx.coroutines.delay(1000)
                    testProgress = TestProgress.WaitingForTrigger
                }
            }
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

        // Top progress panel
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
                    text = "📖 \"${spell.name}\" Testi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // İlerleme göstergesi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLocalizedText("step_colon"),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${currentStepIndex + 1} / ${spell.steps.size}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A4C93)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // İlerleme çubuğu
                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1).toFloat() / spell.steps.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF6A4C93),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mevcut adım bilgisi
                if (currentStepIndex < spell.steps.size) {
                    val currentStep = spell.steps[currentStepIndex]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = rememberLocalizedText("step_number").replace("{number}", (currentStepIndex + 1).toString()) + ":",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = getTriggerName(currentStep.trigger),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Test tamamlandı
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF00FF88)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = rememberLocalizedText("test_completed"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = rememberLocalizedText("steps_successful")
                                    .replace("{successful}", successfulSteps.toString())
                                    .replace("{total}", spell.steps.size.toString()),
                                fontSize = 12.sp,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // El durumu
                val isHandDetected = currentHandResult != null && currentHandResult!!.landmarks().isNotEmpty()

                // G88: Doğruluk Göstergesi (%80+ yeşil, altı kırmızı)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLocalizedText("accuracy_colon"),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // G88: Yeşil/Kırmızı nokta (%80 threshold)
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (currentAccuracy >= 0.80f) Color(0xFF00FF88) else Color(0xFFFF5252),
                                    shape = CircleShape
                                )
                        )
                        // G88: Doğruluk yüzdesi
                        Text(
                            text = "${(currentAccuracy * 100).toInt()}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentAccuracy >= 0.80f) Color(0xFF00FF88) else Color(0xFFFF5252)
                        )
                    }
                }

                // ⚡⚡⚡ YENİ: VFX Önizleme Durumu
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎆 ${rememberLocalizedText("active_effects_colon")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${spellActionExecutor.particleManager.activeEmitters.value.size}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD600)
                    )
                }
            }
        }

        // ⚡⚡⚡ GÖREV E: Sesli Komut için Mikrofon Butonu
        val currentTrigger = if (currentStepIndex < spell.steps.size) {
            spell.steps[currentStepIndex].trigger
        } else {
            null
        }

        if (currentTrigger is SpellTrigger.VoiceCommand) {
            // Mikrofon butonu (sağ alt köşe)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
            ) {
                VoiceTriggerButton(
                    voiceCommands = listOf(currentTrigger),
                    onVoiceCommandMatched = { matchedCommand ->
                        Log.i(TAG, "🎤 Voice command matched: \"${matchedCommand.command}\"")

                        testProgress = TestProgress.TriggerDetected

                        // Efekti tetikle
                        try {
                            val actionContext = ActionContext(
                                handPosition = handPosition,
                                screenSize = Offset(imageWidth.toFloat(), imageHeight.toFloat())
                            )
                            Log.d(TAG, "🎬 EXECUTE CALLED (VoiceButton): Action=${spell.steps[currentStepIndex].action::class.simpleName}")
                            spellActionExecutor.executeAction(spell.steps[currentStepIndex].action, actionContext)
                            Log.d(TAG, "✅ Effect executed successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error executing action: ${e.message}", e)
                        }

                        currentStepIndex++
                        successfulSteps++
                        snackbarMessage = "✅ Sesli komut tanındı: \"${matchedCommand.command}\""
                        showSnackbar = true

                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1000)
                            testProgress = TestProgress.WaitingForTrigger
                        }
                    }
                )
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
                    text = if (currentStepIndex < spell.steps.size) {
                        rememberLocalizedText("please_perform_trigger")
                    } else {
                        rememberLocalizedText("test_completed_exclamation")
                    },
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Yeniden Başlat butonu
                    if (currentStepIndex >= spell.steps.size) {
                        Button(
                            onClick = {
                                currentStepIndex = 0
                                successfulSteps = 0
                                testProgress = TestProgress.WaitingForTrigger
                                snackbarMessage = "🔄 Test yeniden başlatıldı"
                                showSnackbar = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A4C93)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(rememberLocalizedText("restart"))
                        }
                    } else {
                        // ⚡⚡⚡ FIX: "Adımı Atla" butonu kaldırıldı (UX sorunu)
                        // Test akışı sadece başarılı hareket ile ilerlemeli.
                        // NOT: Geliştirme için debug versiyonu kullanın.
                        // Bu alan boş bırakıldı - buton artık görünmüyor.
                    }
                }

                // Testi Durdur Butonu
                OutlinedButton(
                    onClick = onStopTest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(rememberLocalizedText("stop_test"))
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

        // ⚡⚡⚡ G88 FIX: PARTICLE RENDERER - Z-Order düzeltildi, en üstte render edilmeli!
        // Tüm UI elementlerinin ÜSTÜNDEki son katman olarak yerleştirildi
        ParticleRenderer(
            particleManager = spellActionExecutor.particleManager,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Tetikleyici adını al
 */
@Composable
private fun getTriggerName(trigger: SpellTrigger): String {
    return when (trigger) {
        is SpellTrigger.SingleHandGesture -> rememberLocalizedText("single_hand_gesture")
        is SpellTrigger.DoubleHandGesture -> rememberLocalizedText("double_hand_gesture")
        is SpellTrigger.VoiceCommand -> "${rememberLocalizedText("voice_command_colon")} \"${trigger.command}\""
        is SpellTrigger.Timer -> "${rememberLocalizedText("timer_colon")} ${trigger.delayMs}ms"
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
 * Test Modu
 */
enum class TestMode {
    PreTest,
    Testing
}

/**
 * Test İlerlemesi
 */
enum class TestProgress {
    WaitingForTrigger,
    TriggerDetected,
    ExecutingAction
}

/**
 * ⚡⚡⚡ KRİTİK DÜZELTME: Landmark koordinatlarını ekran koordinatlarına dönüştür
 *
 * MediaPipe'dan gelen normalize edilmiş koordinatlar (0.0-1.0) ve kamera rotasyonu
 * hesaba katılarak gerçek ekran pikselleri hesaplanır.
 *
 * @param normalizedX Normalize edilmiş X koordinatı (0.0-1.0)
 * @param normalizedY Normalize edilmiş Y koordinatı (0.0-1.0)
 * @param imageWidth Kamera görüntüsünün genişliği
 * @param imageHeight Kamera görüntüsünün yüksekliği
 * @param rotationDegrees Kamera rotasyonu (0, 90, 180, 270)
 * @param isFrontCamera Ön kamera mı? (mirror için)
 * @return Ekran koordinatları (Offset)
 */
private fun transformLandmarkToScreenCoordinates(
    normalizedX: Float,
    normalizedY: Float,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    isFrontCamera: Boolean
): Offset {
    // Önce normalize koordinatları piksel koordinatlarına çevir
    var x = normalizedX * imageWidth
    var y = normalizedY * imageHeight

    // Rotasyon dönüşümü uygula
    when (rotationDegrees) {
        90 -> {
            // 90° saat yönünde rotasyon
            // x' = y
            // y' = imageWidth - x
            val temp = x
            x = y
            y = imageWidth - temp
        }
        180 -> {
            // 180° rotasyon
            x = imageWidth - x
            y = imageHeight - y
        }
        270 -> {
            // 270° rotasyon (90° saat yönünün tersi)
            // x' = imageHeight - y
            // y' = x
            val temp = x
            x = imageHeight - y
            y = temp
        }
        // 0° -> değişiklik yok
    }

    // Ön kamera için mirror (ayna) efekti
    if (isFrontCamera && rotationDegrees == 0) {
        x = imageWidth - x
    }

    return Offset(x, y)
}

private const val TAG = "SpellTestScreen"
