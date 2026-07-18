package com.example.isekaikuroshin.ui.vfx.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.GestureType
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.VFXLevel
import com.example.isekaikuroshin.ui.sealpractice.HandDetectorHelper
import com.example.isekaikuroshin.ui.vfx.HandReferencePoint
import com.example.isekaikuroshin.ui.vfx.VFXWorkshopUiState
import com.example.isekaikuroshin.ui.vfx.VFXWorkshopViewModel
import com.example.isekaikuroshin.utils.DeviceUtils
import com.example.isekaikuroshin.utils.GameLogger
import com.google.accompanist.permissions.*
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * VFX Test Kamera Ekranı
 *
 * ⚡⚡⚡ NİHAİ DÜZELTME: Shared ViewModel (Default Parameter YOK!)
 * Bu ekran "aptal" bir Composable'dır. Kendi ViewModel'i YOKTUR.
 * Tüm veri ve fonksiyonları NavGraph'tan gelen paylaşılan ViewModel'den alır.
 *
 * Kullanıcının kalibre ettiği el jestlerini tanıyarak VFX efektlerini kontrol eder.
 * Mevcut HandDetectorHelper sistemini kullanır (template matching ile).
 */
@OptIn(ExperimentalPermissionsApi::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun VFXTestCameraScreen(
    navController: NavController,
    viewModel: VFXWorkshopViewModel  // ⚡ NO DEFAULT! NavGraph'tan gelmelidir
) {
    val uiState by viewModel.uiState.collectAsState()

    // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Repository'den reaktif veri
    val creationSeal by viewModel.creationSeal.collectAsState()
    val directionSeal by viewModel.directionSeal.collectAsState()

    // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Seal'lardan gesture template'lerini oluştur
    val calibratedGestures = remember(creationSeal, directionSeal) {
        buildMap<GestureType, List<List<NormalizedPoint>>> {
            creationSeal?.let { seal ->
                if (seal.templateLandmarksList.isNotEmpty()) {
                    put(GestureType.CREATION, seal.templateLandmarksList)
                }
            }
            directionSeal?.let { seal ->
                if (seal.templateLandmarksList.isNotEmpty()) {
                    put(GestureType.DIRECTION, seal.templateLandmarksList)
                }
            }
        }
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.CAMERA
    )

    // Emulator detection
    val isEmulator = remember { DeviceUtils.isEmulator() }

    // HandDetectorHelper oluştur (mevcut sistem)
    val handDetectorHelper = remember(isEmulator) {
        try {
            Log.d(TAG, "🔧 HandDetectorHelper creating for VFX test...")
            HandDetectorHelper(context, isEmulator = isEmulator).also {
                Log.d(TAG, "✅ HandDetectorHelper created successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ HandDetectorHelper creation error: ${e.message}", e)
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

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    if (!cameraPermissionState.status.isGranted) {
        PermissionRequestUI(cameraPermissionState) { navController.navigateUp() }
    } else if (handDetectorHelper == null) {
        ErrorScreen { navController.navigateUp() }
    } else {
        CameraPreviewWithGestureDetection(
            context = context,
            lifecycleOwner = lifecycleOwner,
            handDetectorHelper = handDetectorHelper,
            uiState = uiState,
            calibratedGestures = calibratedGestures,
            onGestureDetected = { gestureType, position ->
                // Gesture tanıma callback'i
                Log.d(TAG, "🎯 Gesture detected: $gestureType at $position")
            },
            onTestStop = { navController.navigateUp() }
        )
    }
}

/**
 * İzin İsteme UI
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequestUI(
    cameraPermissionState: PermissionState,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Kamera izni gerekli")
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("İzin Ver")
            }
            TextButton(onClick = onBack) {
                Text(rememberLocalizedText("back"))
            }
        }
    }
}

/**
 * Kamera Önizleme ve Gesture Tanıma
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
private fun CameraPreviewWithGestureDetection(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    handDetectorHelper: HandDetectorHelper,
    uiState: VFXWorkshopUiState,
    calibratedGestures: Map<GestureType, List<List<NormalizedPoint>>>,
    onGestureDetected: (GestureType, Offset?) -> Unit,
    onTestStop: () -> Unit
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var currentHandResult by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(640) }
    var imageHeight by remember { mutableIntStateOf(480) }

    // VFX State Management (İki Aşamalı Sistem)
    var currentVFXEffect by remember { mutableStateOf<VFXEffect?>(null) }
    var lastDetectedGesture by remember { mutableStateOf<GestureType?>(null) }
    var gestureDebounceTime by remember { mutableLongStateOf(0L) }

    // ⚡⚡⚡ BÖLÜM C: FPS Monitoring
    var currentFps by remember { mutableFloatStateOf(0f) }
    var lastFrameTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var frameCount by remember { mutableIntStateOf(0) }

    // ⚡⚡⚡ BÖLÜM C: Ghost Hand State (son bilinen el pozisyonu)
    var lastKnownHandPosition by remember { mutableStateOf<Offset?>(null) }
    var isHandTracking by remember { mutableStateOf(false) }

    // Hand detection results collector
    LaunchedEffect(handDetectorHelper) {
        Log.d(TAG, "🎯 [GESTURE_DETECTION] LaunchedEffect started - waiting for hand results...")
        Log.d(TAG, "🎯 [GESTURE_DETECTION] Calibrated gestures count: ${calibratedGestures.size}")
        Log.d(TAG, "🎯 [GESTURE_DETECTION] Sensitivity threshold: ${uiState.sensitivityThreshold}")

        handDetectorHelper.results.collect { result ->
            currentHandResult = result

            // ⚡⚡⚡ BÖLÜM C: FPS Hesaplama
            val currentTime = System.currentTimeMillis()
            frameCount++
            val timeDiff = currentTime - lastFrameTime
            if (timeDiff >= 1000) { // Her saniyede bir güncelle
                currentFps = frameCount * 1000f / timeDiff
                frameCount = 0
                lastFrameTime = currentTime

                // ⚡⚡⚡ BÖLÜM D: FPS düşük olduğunda logla
                if (currentFps < 20f) {
                    GameLogger.logVfxTestWarning("⚠️ LOW FPS: ${currentFps.toInt()} FPS (threshold: 20 FPS)")
                }
            }

            // Gesture detection: Çoklu açı template matching
            if (result.landmarks().isNotEmpty()) {
                // ⚡⚡⚡ BÖLÜM C: El takibi BULUNDU
                val wasTracking = isHandTracking
                isHandTracking = true

                // ⚡⚡⚡ BÖLÜM D: El takibi yeniden bulundu
                if (!wasTracking) {
                    GameLogger.logVfxTest("🖐️ TRACKING RESTORED: Hand landmarks detected again")
                }

                Log.d(TAG, "🎯 [GESTURE_DETECTION] Hand detected! Landmarks: ${result.landmarks()[0].size}")

                val detectedLandmarks = result.landmarks()[0].map { landmark ->
                    NormalizedPoint(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z()
                    )
                }

                // ⚡⚡⚡ DÜZELTME: El pozisyonunu HER FRAME hesapla (smooth takip için)
                // ⚡⚡⚡ YENİ: 3D offset uygula (eğer kaydedilmişse)
                val handPosition = getHandPositionFromResultWith3DOffset(
                    result,
                    imageWidth,
                    imageHeight,
                    uiState.referencePoint,
                    gestureType = lastDetectedGesture,
                    creationOffsetConfig = uiState.creationOffsetConfig,
                    directionOffsetConfig = uiState.directionOffsetConfig
                )

                // Son bilinen pozisyonu kaydet (ghost hand için)
                lastKnownHandPosition = handPosition

                // Gesture tanıma: CREATION için debounce var, DIRECTION için YOK
                val debounceThreshold = if (lastDetectedGesture == GestureType.CREATION) {
                    500  // CREATION: 500ms debounce (çift yaratmayı önle)
                } else {
                    50   // DIRECTION: 50ms debounce (smooth hareket)
                }

                if (currentTime - gestureDebounceTime > debounceThreshold) {
                    Log.d(TAG, "🎯 [GESTURE_DETECTION] Debounce passed - checking gesture...")

                    // Çoklu açı template matching ile gesture tanı
                    val detectedGesture = detectGestureFromLandmarksMultiAngle(
                        detectedLandmarks,
                        calibratedGestures,
                        uiState.sensitivityThreshold
                    )

                    Log.d(TAG, "🎯 [GESTURE_DETECTION] Detected gesture: $detectedGesture, Hand position: $handPosition")

                    if (detectedGesture != null) {
                        // ⚡⚡⚡ BÖLÜM D: Jest başarıyla tanındı - LOGLA
                        GameLogger.logVfxTest("✅ GESTURE DETECTED: $detectedGesture")

                        // İki Aşamalı VFX Sistemi
                        when (detectedGesture) {
                            GestureType.CREATION -> {
                                // 1. Aşama: Efekt oluştur
                                if (handPosition != null) {
                                    currentVFXEffect = VFXEffect(
                                        id = System.currentTimeMillis(),
                                        type = uiState.selectedVFXLevel,
                                        position = handPosition,
                                        createdAt = currentTime
                                    )
                                    Log.d(TAG, "🌊 [VFX_CREATE] VFX Effect CREATED! Type: ${uiState.selectedVFXLevel}, Position: $handPosition")
                                    GameLogger.logVfxTest("🌊 VFX CREATED: Type=${uiState.selectedVFXLevel}, Position=$handPosition")
                                } else {
                                    Log.w(TAG, "⚠️ [VFX_CREATE] Hand position is NULL - cannot create effect!")
                                    GameLogger.logVfxTestWarning("Hand position NULL - cannot create effect")
                                }
                            }
                            GestureType.DIRECTION -> {
                                // 2. Aşama: Efekti hareket ettir (eğer efekt varsa)
                                if (currentVFXEffect != null && handPosition != null) {
                                    currentVFXEffect = currentVFXEffect!!.copy(position = handPosition)
                                    Log.d(TAG, "✋ [VFX_MOVE] VFX Effect MOVED to $handPosition")
                                    // DIRECTION için log'u azalt (çok fazla spam olmaması için)
                                } else {
                                    Log.w(TAG, "⚠️ [VFX_MOVE] Cannot move - currentVFXEffect: ${currentVFXEffect != null}, handPosition: ${handPosition != null}")
                                }
                            }
                        }

                        lastDetectedGesture = detectedGesture
                        gestureDebounceTime = currentTime

                        onGestureDetected(detectedGesture, handPosition)
                    } else {
                        // ⚡⚡⚡ YENİ: Gesture tanınmasa bile, eğer DIRECTION modundaysak pozisyonu güncelle
                        if (lastDetectedGesture == GestureType.DIRECTION && currentVFXEffect != null && handPosition != null) {
                            currentVFXEffect = currentVFXEffect!!.copy(position = handPosition)
                        }
                    }
                } else {
                    // ⚡⚡⚡ YENİ: Debounce aktif OLSA BİLE, DIRECTION modunda pozisyonu güncelle!
                    if (lastDetectedGesture == GestureType.DIRECTION && currentVFXEffect != null && handPosition != null) {
                        currentVFXEffect = currentVFXEffect!!.copy(position = handPosition)
                    }
                }
            } else {
                // ⚡⚡⚡ BÖLÜM C: El takibi KAYBOLDU
                val wasTracking = isHandTracking
                isHandTracking = false

                // ⚡⚡⚡ BÖLÜM D: El takibi kayboldu
                if (wasTracking) {
                    GameLogger.logVfxTestWarning("❌ TRACKING LOST: Hand landmarks not detected")
                }

                Log.d(TAG, "⚠️ [GESTURE_DETECTION] No hand landmarks in result - TRACKING LOST")
            }
        }
    }

    Log.d(TAG, "🎬 [CAMERA_SCREEN] Composing VFXTestCameraScreen...")
    Log.d(TAG, "🎬 [CAMERA_SCREEN] Calibrated gestures: ${calibratedGestures.keys}")
    Log.d(TAG, "🎬 [CAMERA_SCREEN] Selected VFX Level: ${uiState.selectedVFXLevel}")

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {  // ⚡⚡⚡ DEBUG: Siyah arka plan ekle

        // ⚡⚡⚡ DEBUG: Kamera yükleniyor göstergesi
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rememberLocalizedText("camera_starting"),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Kamera Preview
        AndroidView(
            factory = { ctx ->
                Log.d(TAG, "🎬 [CAMERA_FACTORY] Creating PreviewView...")

                val preview = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    Log.d(TAG, "🎬 [CAMERA_FACTORY] PreviewView created with COMPATIBLE mode")
                }

                // Kamera başlatma
                Log.d(TAG, "🎬 [CAMERA_FACTORY] Getting CameraProvider...")
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    try {
                        Log.d(TAG, "🎬 [CAMERA_LISTENER] CameraProvider listener triggered")
                        val cameraProvider = cameraProviderFuture.get()
                        Log.d(TAG, "🎬 [CAMERA_LISTENER] CameraProvider obtained successfully")

                        val cameraPreview = Preview.Builder().build().also {
                            it.setSurfaceProvider(preview.surfaceProvider)
                            Log.d(TAG, "🎬 [CAMERA_LISTENER] Preview surfaceProvider set")
                        }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imageAnalysis ->
                                imageAnalysis.setAnalyzer(
                                    ContextCompat.getMainExecutor(ctx),
                                    { imageProxy ->
                                        try {
                                            val bitmap = imageProxy.toBitmap()
                                            val timestampMs = System.currentTimeMillis()

                                            imageWidth = imageProxy.width
                                            imageHeight = imageProxy.height

                                            handDetectorHelper.detectHands(bitmap, timestampMs)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ [ANALYZER] Hand detection error: ${e.message}", e)
                                        } finally {
                                            imageProxy.close()
                                        }
                                    }
                                )
                                Log.d(TAG, "🎬 [CAMERA_LISTENER] ImageAnalyzer configured")
                            }

                        Log.d(TAG, "🎬 [CAMERA_LISTENER] Unbinding all previous use cases...")
                        cameraProvider.unbindAll()

                        Log.d(TAG, "🎬 [CAMERA_LISTENER] Binding to lifecycle...")

                        // ⚡⚡⚡ DÜZELTME: Emülatörde FRONT kamera yok, BACK kamera kullan!
                        val cameraSelector = if (DeviceUtils.isEmulator()) {
                            Log.d(TAG, "🎬 [CAMERA_LISTENER] Using BACK camera (Emulator detected)")
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            Log.d(TAG, "🎬 [CAMERA_LISTENER] Using FRONT camera (Real device)")
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }

                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            cameraPreview,
                            imageAnalyzer
                        )
                        Log.d(TAG, "✅ [CAMERA_LISTENER] Camera bound successfully!")
                        Log.d(TAG, "✅ [CAMERA_LISTENER] Camera info: ${camera.cameraInfo}")

                    } catch (exc: Exception) {
                        Log.e(TAG, "❌ [CAMERA_LISTENER] Camera binding error: ${exc.message}", exc)
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                Log.d(TAG, "🎬 [CAMERA_FACTORY] Returning preview...")
                preview
            },
            update = { view ->
                previewView = view
                Log.d(TAG, "🎬 [CAMERA_UPDATE] PreviewView updated")
            },
            modifier = Modifier.fillMaxSize()
        )

        // ⚡⚡⚡ VFX Overlay (Efekt Rendering) - BÖLÜM C: Yeni ayarları kullan
        if (currentVFXEffect != null) {
            Log.d(TAG, "🎨 [VFX_OVERLAY] Rendering VFX effect: ${currentVFXEffect!!.type} at ${currentVFXEffect!!.position}")

            // ⚡⚡⚡ BÖLÜM C: Offset uygula (kullanıcı ayarlarına göre)
            // NOT: Offset 0 ise sprite tam işaret parmağı ucunda olur
            val adjustedPosition = Offset(
                x = currentVFXEffect!!.position.x + uiState.vfxOffsetX,
                y = currentVFXEffect!!.position.y + uiState.vfxOffsetY
            )

            VFXEffectOverlay(
                effect = currentVFXEffect!!,
                position = adjustedPosition,
                color = uiState.vfxColor,
                size = uiState.vfxSize,
                shape = uiState.vfxShape,
                alpha = uiState.vfxAlpha,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Log.d(TAG, "⚠️ [VFX_OVERLAY] No VFX effect to render (currentVFXEffect is null)")
        }

        // ⚡⚡⚡ Geri Dön butonu (SOL ALT KÖŞE) - Ok işareti ile
        FloatingActionButton(
            onClick = {
                Log.d(TAG, "✅ [UI] Back button pressed - Navigating back")
                GameLogger.logVfxTest("User pressed back button - ending test")
                onTestStop()
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            containerColor = Color(0xFF6A4C93),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        // ⚡⚡⚡ BÖLÜM C: FPS Monitörü (Sol Üst Köşe)
        if (uiState.isFpsMonitorEnabled) {
            FpsMonitorOverlay(
                fps = currentFps,
                isHandTracking = isHandTracking,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }

        // ⚡⚡⚡ BÖLÜM C: Hayalet El (El takibi kaybolduğunda göster)
        if (uiState.isGhostHandEnabled && !isHandTracking && lastKnownHandPosition != null) {
            GhostHandOverlay(
                position = lastKnownHandPosition!!,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Debug: Gesture Indicator (Bottom Center)
        if (lastDetectedGesture != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = when (lastDetectedGesture) {
                        GestureType.CREATION -> "🌊 Efekt Oluşturuldu"
                        GestureType.DIRECTION -> "✋ Efekt Hareket Ediyor"
                        else -> ""
                    },
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Çoklu Açı Template Matching ile Gesture Tanıma
 *
 * Her gesture type için birden fazla açı (template) ile karşılaştırma yapar.
 * En yüksek benzerliği veren gesture type döndürülür.
 */
private fun detectGestureFromLandmarksMultiAngle(
    detectedLandmarks: List<NormalizedPoint>,
    calibratedGestures: Map<GestureType, List<List<NormalizedPoint>>>,
    threshold: Float
): GestureType? {
    var bestMatch: GestureType? = null
    var highestSimilarity = 0f

    for ((gestureType, templateAnglesList) in calibratedGestures) {
        // Her açı ile karşılaştır, en yüksek benzerliği al
        val maxSimilarityForGesture = templateAnglesList.maxOfOrNull { templateLandmarks ->
            calculateLandmarkSimilarity(detectedLandmarks, templateLandmarks)
        } ?: 0f

        if (maxSimilarityForGesture > highestSimilarity && maxSimilarityForGesture >= threshold) {
            highestSimilarity = maxSimilarityForGesture
            bestMatch = gestureType
        }
    }

    if (bestMatch != null) {
        Log.d(TAG, "✅ Gesture detected: $bestMatch (similarity: ${(highestSimilarity * 100).toInt()}%)")
    }

    return bestMatch
}

/**
 * Landmark benzerliği hesaplama
 * (Kalibrasyon sistemindeki pattern matching algoritması)
 */
private fun calculateLandmarkSimilarity(
    detected: List<NormalizedPoint>,
    template: List<NormalizedPoint>
): Float {
    if (detected.size != template.size || detected.isEmpty()) return 0f

    val distances = detected.zip(template).map { (d, t) ->
        val dx = d.x - t.x
        val dy = d.y - t.y
        val dz = d.z - t.z
        kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    val avgDistance = distances.average().toFloat()
    return (1f - avgDistance.coerceIn(0f, 1f)).coerceIn(0f, 1f)
}

/**
 * ⚡⚡⚡ BÖLÜM C: El pozisyonunu hesaplama (referans noktaya göre)
 */
private fun getHandPositionFromResult(
    result: HandLandmarkerResult,
    imageWidth: Int,
    imageHeight: Int,
    referencePoint: HandReferencePoint
): Offset? {
    if (result.landmarks().isEmpty()) return null

    // Referans noktaya göre landmark seç
    val landmarkIndex = when (referencePoint) {
        HandReferencePoint.PALM_CENTER -> 9  // Avuç içi merkezi
        HandReferencePoint.INDEX_FINGER -> 8  // İşaret parmağı ucu (MediaPipe INDEX_FINGER_TIP)
    }

    val landmark = result.landmarks()[0].getOrNull(landmarkIndex) ?: result.landmarks()[0].getOrNull(0)

    return landmark?.let {
        Offset(
            x = it.x() * imageWidth,
            y = it.y() * imageHeight
        )
    }
}

/**
 * ⚡⚡⚡ YENİ: El pozisyonunu hesaplama + 3D Offset Uygulama
 * Kaydedilmiş 3D offset konfigürasyonuna göre VFX'in nihai pozisyonunu hesaplar
 */
private fun getHandPositionFromResultWith3DOffset(
    result: HandLandmarkerResult,
    imageWidth: Int,
    imageHeight: Int,
    referencePoint: HandReferencePoint,
    gestureType: GestureType?,
    creationOffsetConfig: com.example.isekaikuroshin.data.VFXOffsetConfig?,
    directionOffsetConfig: com.example.isekaikuroshin.data.VFXOffsetConfig?
): Offset? {
    if (result.landmarks().isEmpty()) return null

    // Referans noktaya göre landmark seç (avuç merkezi - landmark 9)
    val landmark = result.landmarks()[0].getOrNull(9) ?: result.landmarks()[0].getOrNull(0)

    if (landmark == null) return null

    // Referans noktanın 3D koordinatları (normalize edilmiş 0-1)
    val handRef3D = com.example.isekaikuroshin.data.Vector3f(
        x = landmark.x(),
        y = landmark.y(),
        z = landmark.z()
    )

    // Gesture tipine göre offset konfigürasyonu seç
    val offsetConfig = when (gestureType) {
        GestureType.CREATION -> creationOffsetConfig
        GestureType.DIRECTION -> directionOffsetConfig
        else -> null
    }

    // Eğer offset kaydedilmişse uygula, yoksa referans noktayı kullan
    val vfx3D = if (offsetConfig != null) {
        handRef3D + offsetConfig.offset3D
    } else {
        handRef3D
    }

    // 3D normalize edilmiş koordinatları ekran koordinatlarına çevir
    return Offset(
        x = vfx3D.x * imageWidth,
        y = vfx3D.y * imageHeight
    )
}

/**
 * ⚡⚡⚡ BÖLÜM C: FPS Monitör Overlay (Performans Göstergesi)
 */
@Composable
private fun FpsMonitorOverlay(
    fps: Float,
    isHandTracking: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.8f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // FPS
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊",
                    fontSize = 16.sp
                )
                Text(
                    text = "FPS:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "${fps.toInt()}",
                    fontSize = 16.sp,
                    color = when {
                        fps >= 30 -> Color(0xFF4CAF50) // Yeşil (iyi)
                        fps >= 20 -> Color(0xFFFFEB3B) // Sarı (orta)
                        else -> Color(0xFFFF5252) // Kırmızı (kötü)
                    },
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // El Takip Durumu
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHandTracking) "🖐️" else "❌",
                    fontSize = 16.sp
                )
                Text(
                    text = if (isHandTracking) "Takip: Aktif" else "Takip: Kayıp",
                    fontSize = 11.sp,
                    color = if (isHandTracking) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}

/**
 * ⚡⚡⚡ BÖLÜM C: Hayalet El Overlay (El takibi kaybolduğunda göster)
 */
@Composable
private fun GhostHandOverlay(
    position: Offset,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Hayalet el işareti (yarı saydam daire ve çizgiler)
        val ghostColor = Color.White.copy(alpha = 0.3f)
        val pulseColor = Color.Cyan.copy(alpha = 0.2f)

        // Dış nabız efekti (pulse)
        drawCircle(
            color = pulseColor,
            radius = 100f,
            center = position
        )

        // Orta katman
        drawCircle(
            color = ghostColor,
            radius = 60f,
            center = position
        )

        // İç nokta
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = 20f,
            center = position
        )

        // Çarpı işareti (kayıp göstergesi)
        val crossSize = 40f
        drawLine(
            color = Color.Red.copy(alpha = 0.6f),
            start = Offset(position.x - crossSize, position.y - crossSize),
            end = Offset(position.x + crossSize, position.y + crossSize),
            strokeWidth = 6f
        )
        drawLine(
            color = Color.Red.copy(alpha = 0.6f),
            start = Offset(position.x + crossSize, position.y - crossSize),
            end = Offset(position.x - crossSize, position.y + crossSize),
            strokeWidth = 6f
        )
    }
}

/**
 * Hata Ekranı
 */
@Composable
private fun ErrorScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("❌ ${rememberLocalizedText("camera_failed")}")
            Button(onClick = onBack) {
                Text(rememberLocalizedText("back"))
            }
        }
    }
}

/**
 * VFX Effect Data Class
 */
data class VFXEffect(
    val id: Long,
    val type: VFXLevel,
    val position: Offset,
    val createdAt: Long
)

/**
 * VFX Effect Overlay Composable
 *
 * ⚡⚡⚡ BÖLÜM C: Kullanıcı tarafından özelleştirilebilir efektler!
 * VFXLevel'a göre farklı görsel efektler gösterir.
 * Renk, boyut ve pozisyon tamamen ayarlanabilir.
 */
@Composable
fun VFXEffectOverlay(
    effect: VFXEffect,
    position: Offset,
    color: Color,
    size: Float,
    shape: com.example.isekaikuroshin.data.VFXShape,
    alpha: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "🎨 [VFX_RENDER] Drawing effect type: ${effect.type}, shape: $shape at position: $position (size: $size, color: $color, alpha: $alpha)")

    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        // ⚡⚡⚡ YENİ: Önce shape'e göre temel şekli çiz
        drawVFXShape(shape, position, color, size, effect.type)

        // Merkez nokta (el pozisyonu işaretleyici)
        drawCircle(
            color = Color.Yellow,
            radius = 8f,
            center = position,
            alpha = 1.0f
        )
        drawCircle(
            color = Color.White,
            radius = 4f,
            center = position,
            alpha = 1.0f
        )
    }
}

/**
 * ⚡⚡⚡ YENİ: Shape'e göre VFX çiz
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVFXShape(
    shape: com.example.isekaikuroshin.data.VFXShape,
    position: Offset,
    color: Color,
    size: Float,
    vfxType: com.example.isekaikuroshin.data.VFXLevel
) {
    when (shape) {
        com.example.isekaikuroshin.data.VFXShape.CIRCLE -> {
            drawCircleVFX(position, color, size, vfxType)
        }
        com.example.isekaikuroshin.data.VFXShape.TRIANGLE -> {
            drawTriangleVFX(position, color, size, vfxType)
        }
        com.example.isekaikuroshin.data.VFXShape.RECTANGLE -> {
            drawRectangleVFX(position, color, size, vfxType)
        }
        com.example.isekaikuroshin.data.VFXShape.STAR -> {
            drawStarVFX(position, color, size, vfxType)
        }
    }
}

/**
 * Daire şekli çiz (mevcut koddan taşındı)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCircleVFX(
    position: Offset,
    color: Color,
    size: Float,
    vfxType: com.example.isekaikuroshin.data.VFXLevel
) {
    when (vfxType) {
            VFXLevel.SPRITE -> {
                // ⚡⚡⚡ BÖLÜM C: Kullanıcı tarafından özelleştirilebilir sprite
                Log.d(TAG, "💧 [SPRITE] Drawing SPRITE at $position with color $color and size $size")

                // Dış halka (glow effect)
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = 180f * size,
                    center = position
                )
                // Orta katman
                drawCircle(
                    color = color,
                    radius = 120f * size,
                    center = position,
                    alpha = 0.7f
                )
                // İç katman
                drawCircle(
                    color = color.copy(alpha = 0.9f),
                    radius = 80f * size,
                    center = position,
                    alpha = 0.9f
                )
            }
            VFXLevel.PARTICLE -> {
                // ⚡⚡⚡ BÖLÜM C: Kullanıcı tarafından özelleştirilebilir parçacıklar
                Log.d(TAG, "✨ [PARTICLE] Drawing PARTICLES at $position with color $color and size $size")

                // Dış halka (daha görünür)
                for (i in 0 until 16) {
                    val angle = (i * 22.5f) * (Math.PI / 180f).toFloat()
                    val distance = 120f * size
                    val particleOffset = Offset(
                        x = position.x + kotlin.math.cos(angle) * distance,
                        y = position.y + kotlin.math.sin(angle) * distance
                    )
                    drawCircle(
                        color = color,
                        radius = 30f * size,
                        center = particleOffset,
                        alpha = 0.9f  // 0.7f'den 0.9f'ye çıkarıldı (daha görünür)
                    )
                }

                // Merkez parçacık (daha büyük ve görünür)
                drawCircle(
                    color = color,
                    radius = 60f * size,  // 50f'den 60f'ye çıkarıldı
                    center = position,
                    alpha = 1.0f  // 0.9f'den 1.0f'ye çıkarıldı (tam opak)
                )
            }
            VFXLevel.METABALL -> {
                // ⚡⚡⚡ BÖLÜM C: Kullanıcı tarafından özelleştirilebilir sıvı simülasyonu
                Log.d(TAG, "🌊 [METABALL] Drawing METABALL at $position with color $color and size $size")

                // Dış glow (daha görünür)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.8f),  // 0.6f'den 0.8f'ye çıkarıldı
                            color.copy(alpha = 0.5f),  // 0.3f'den 0.5f'ye çıkarıldı
                            Color.Transparent
                        ),
                        center = position,
                        radius = 250f * size
                    ),
                    radius = 250f * size,
                    center = position
                )

                // Orta katman (daha görünür)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.95f),  // 0.8f'den 0.95f'ye çıkarıldı
                            color.copy(alpha = 0.7f),   // 0.6f'den 0.7f'ye çıkarıldı
                            Color.Transparent
                        ),
                        center = position,
                        radius = 180f * size
                    ),
                    radius = 180f * size,
                    center = position
                )

                // İç çekirdek (tam opak)
                drawCircle(
                    color = color,
                    radius = 100f * size,
                    center = position,
                    alpha = 1.0f  // 0.9f'den 1.0f'ye çıkarıldı
                )
            }
        }
}

/**
 * Üçgen şekli çiz
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangleVFX(
    position: Offset,
    color: Color,
    size: Float,
    vfxType: com.example.isekaikuroshin.data.VFXLevel
) {
    when (vfxType) {
        VFXLevel.SPRITE -> {
            // 3 katmanlı üçgen (dış, orta, iç)
            drawTriangle(position, 180f * size, color, alpha = 0.3f)
            drawTriangle(position, 120f * size, color, alpha = 0.7f)
            drawTriangle(position, 80f * size, color, alpha = 0.9f)
        }
        VFXLevel.PARTICLE -> {
            // 16 küçük üçgen çember şeklinde
            for (i in 0 until 16) {
                val angle = (i * 22.5f) * (Math.PI / 180f).toFloat()
                val particlePos = Offset(
                    x = position.x + kotlin.math.cos(angle) * 120f * size,
                    y = position.y + kotlin.math.sin(angle) * 120f * size
                )
                drawTriangle(particlePos, 30f * size, color, alpha = 0.9f)
            }
            // Merkez üçgen
            drawTriangle(position, 60f * size, color, alpha = 1.0f)
        }
        VFXLevel.METABALL -> {
            // Gradient üçgenler (sıvı efekti)
            drawTriangle(position, 250f * size, color, alpha = 0.8f)
            drawTriangle(position, 180f * size, color, alpha = 0.95f)
            drawTriangle(position, 100f * size, color, alpha = 1.0f)
        }
    }
}

// Helper: Üçgen çiz
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangle(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1.0f
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        // Üçgenin 3 köşesi (yukarı bakan)
        moveTo(center.x, center.y - radius) // Üst köşe
        lineTo(center.x + radius * 0.866f, center.y + radius * 0.5f) // Sağ alt
        lineTo(center.x - radius * 0.866f, center.y + radius * 0.5f) // Sol alt
        close()
    }
    drawPath(
        path = path,
        color = color,
        alpha = alpha
    )
}

/**
 * Dikdörtgen şekli çiz
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRectangleVFX(
    position: Offset,
    color: Color,
    size: Float,
    vfxType: com.example.isekaikuroshin.data.VFXLevel
) {
    when (vfxType) {
        VFXLevel.SPRITE -> {
            // 3 katmanlı dikdörtgen
            drawRotatedRect(position, 180f * size, color, alpha = 0.3f)
            drawRotatedRect(position, 120f * size, color, alpha = 0.7f)
            drawRotatedRect(position, 80f * size, color, alpha = 0.9f)
        }
        VFXLevel.PARTICLE -> {
            // 16 küçük dikdörtgen çember şeklinde
            for (i in 0 until 16) {
                val angle = (i * 22.5f) * (Math.PI / 180f).toFloat()
                val particlePos = Offset(
                    x = position.x + kotlin.math.cos(angle) * 120f * size,
                    y = position.y + kotlin.math.sin(angle) * 120f * size
                )
                drawRotatedRect(particlePos, 30f * size, color, alpha = 0.9f, rotation = angle)
            }
            // Merkez dikdörtgen
            drawRotatedRect(position, 60f * size, color, alpha = 1.0f)
        }
        VFXLevel.METABALL -> {
            // Gradient dikdörtgenler
            drawRotatedRect(position, 250f * size, color, alpha = 0.8f)
            drawRotatedRect(position, 180f * size, color, alpha = 0.95f)
            drawRotatedRect(position, 100f * size, color, alpha = 1.0f)
        }
    }
}

// Helper: Döndürülmüş dikdörtgen çiz
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRotatedRect(
    center: Offset,
    size: Float,
    color: Color,
    alpha: Float = 1.0f,
    rotation: Float = 0f
) {
    rotate(degrees = rotation * 180f / Math.PI.toFloat(), pivot = center) {
        drawRect(
            color = color,
            topLeft = Offset(center.x - size/2, center.y - size/2),
            size = androidx.compose.ui.geometry.Size(size, size),
            alpha = alpha
        )
    }
}

/**
 * Yıldız şekli çiz
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarVFX(
    position: Offset,
    color: Color,
    size: Float,
    vfxType: com.example.isekaikuroshin.data.VFXLevel
) {
    when (vfxType) {
        VFXLevel.SPRITE -> {
            // 3 katmanlı yıldız
            drawStar(position, 180f * size, color, alpha = 0.3f)
            drawStar(position, 120f * size, color, alpha = 0.7f)
            drawStar(position, 80f * size, color, alpha = 0.9f)
        }
        VFXLevel.PARTICLE -> {
            // 16 küçük yıldız çember şeklinde
            for (i in 0 until 16) {
                val angle = (i * 22.5f) * (Math.PI / 180f).toFloat()
                val particlePos = Offset(
                    x = position.x + kotlin.math.cos(angle) * 120f * size,
                    y = position.y + kotlin.math.sin(angle) * 120f * size
                )
                drawStar(particlePos, 30f * size, color, alpha = 0.9f)
            }
            // Merkez yıldız
            drawStar(position, 60f * size, color, alpha = 1.0f)
        }
        VFXLevel.METABALL -> {
            // Gradient yıldızlar
            drawStar(position, 250f * size, color, alpha = 0.8f)
            drawStar(position, 180f * size, color, alpha = 0.95f)
            drawStar(position, 100f * size, color, alpha = 1.0f)
        }
    }
}

// Helper: 5 köşeli yıldız çiz
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1.0f
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        val innerRadius = radius * 0.4f
        for (i in 0 until 10) {
            val angle = (i * 36f - 90f) * (Math.PI / 180f).toFloat()
            val r = if (i % 2 == 0) radius else innerRadius
            val x = center.x + r * kotlin.math.cos(angle)
            val y = center.y + r * kotlin.math.sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = path,
        color = color,
        alpha = alpha
    )
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

private const val TAG = "VFXTestCameraScreen"
