package com.example.isekaikuroshin.ui.vfx

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.GestureType
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.Vector3f
import com.example.isekaikuroshin.data.VFXOffsetConfig
import com.example.isekaikuroshin.ui.sealpractice.HandDetectorHelper
import com.example.isekaikuroshin.utils.DeviceUtils
import com.google.accompanist.permissions.*
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * VFX Offset Kalibrasyon Ekranı
 *
 * Kullanıcının, canlı kamera görüntüsü üzerinde bir VFX efektini
 * sürükleyip el pozisyonuna göre 3D offset'ini ayarlamasını sağlar.
 *
 * Akış:
 * 1. Kullanıcı elini kamera önüne tutar (el iskeleti yeşil çizilir)
 * 2. Ekranda bir VFX ikonu (💧) belirir
 * 3. Kullanıcı ikonu parmağıyla sürükler
 * 4. "Kaydet" butonuna basınca:
 *    a. Referans noktanın (örn: avuç merkezi) 3D pozisyonu alınır
 *    b. Sürüklenen ikonun ekran pozisyonu 3D uzaya çevrilir
 *    c. Fark vektörü (offset) hesaplanır ve kaydedilir
 */
@OptIn(ExperimentalPermissionsApi::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun VFXOffsetCalibrationScreen(
    navController: NavController,
    viewModel: VFXWorkshopViewModel,
    gestureType: GestureType
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.CAMERA
    )

    val isEmulator = remember { DeviceUtils.isEmulator() }

    val handDetectorHelper = remember(isEmulator) {
        try {
            Log.d(TAG, "🔧 Creating HandDetectorHelper for offset calibration...")
            HandDetectorHelper(context, isEmulator = isEmulator)
        } catch (e: Exception) {
            Log.e(TAG, "❌ HandDetectorHelper creation error: ${e.message}", e)
            null
        }
    }

    DisposableEffect(handDetectorHelper) {
        onDispose {
            handDetectorHelper?.close()
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    if (!cameraPermissionState.status.isGranted) {
        PermissionRequestScreen { navController.navigateUp() }
    } else if (handDetectorHelper == null) {
        ErrorScreen { navController.navigateUp() }
    } else {
        OffsetCalibrationContent(
            context = context,
            lifecycleOwner = lifecycleOwner,
            handDetectorHelper = handDetectorHelper,
            gestureType = gestureType,
            onSave = { offsetConfig ->
                viewModel.saveOffsetConfiguration(gestureType, offsetConfig)
                navController.navigateUp()
            },
            onCancel = { navController.navigateUp() }
        )
    }
}

/**
 * Ana Kalibrasyon İçeriği
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
private fun OffsetCalibrationContent(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    handDetectorHelper: HandDetectorHelper,
    gestureType: GestureType,
    onSave: (VFXOffsetConfig) -> Unit,
    onCancel: () -> Unit
) {
    var currentHandResult by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(640) }
    var imageHeight by remember { mutableIntStateOf(480) }

    // VFX İkon pozisyonu (ekran koordinatları - sürüklenebilir)
    // ⚡⚡⚡ DÜZELTME: Başlangıç pozisyonu null, el tespit edilince avuç merkezine yapışır
    var vfxIconPosition by remember { mutableStateOf<Offset?>(null) }

    // El referans noktası pozisyonu (ekran koordinatları)
    var handReferencePosition by remember { mutableStateOf<Offset?>(null) }

    // El referans noktası 3D koordinatları (normalize)
    var handReference3D by remember { mutableStateOf<Vector3f?>(null) }

    // ⚡⚡⚡ YENİ: İlk el tespitinde ikonu avuç merkezine yapıştır
    var isIconInitialized by remember { mutableStateOf(false) }

    // ⚡⚡⚡ YENİ: Takip Modu - Top eli takip etsin mi?
    var isTrackingEnabled by remember { mutableStateOf(false) }

    // Hand detection results collector
    // ⚡⚡⚡ RAW MODE: Sadece result'u sakla, tüm hesaplamalar Canvas'ta!
    LaunchedEffect(handDetectorHelper) {
        handDetectorHelper.results.collect { result ->
            currentHandResult = result

            if (result.landmarks().isNotEmpty()) {
                val landmarks = result.landmarks()[0]
                val palmCenter = landmarks.getOrNull(9)

                if (palmCenter != null) {
                    // 3D koordinatları sakla (kaydetme için gerekli)
                    handReference3D = Vector3f(
                        x = palmCenter.x(),
                        y = palmCenter.y(),
                        z = palmCenter.z()
                    )
                }
            } else {
                // El kaybolunca state'leri temizle
                handReferencePosition = null
                handReference3D = null
                isIconInitialized = false
                vfxIconPosition = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Kamera Preview
        AndroidView(
            factory = { ctx ->
                val preview = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val cameraPreview = Preview.Builder().build().also {
                            it.setSurfaceProvider(preview.surfaceProvider)
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
                                            imageWidth = imageProxy.width
                                            imageHeight = imageProxy.height

                                            handDetectorHelper.detectHands(
                                                bitmap,
                                                System.currentTimeMillis()
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ Hand detection error: ${e.message}", e)
                                        } finally {
                                            imageProxy.close()
                                        }
                                    }
                                )
                            }

                        cameraProvider.unbindAll()

                        val cameraSelector = if (DeviceUtils.isEmulator()) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            cameraPreview,
                            imageAnalyzer
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Camera binding error: ${e.message}", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                preview
            },
            modifier = Modifier.fillMaxSize()
        )

        // ⚡⚡⚡ YENİ: Referans El Silüeti (el tespit edilmediğinde göster)
        if (currentHandResult == null || currentHandResult!!.landmarks().isEmpty()) {
            ReferenceHandSilhouette(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                modifier = Modifier.fillMaxSize()
            )
        }

        // El İskeleti Overlay (el tespit edildiğinde göster)
        // ⚡⚡⚡ RAW MODE: Her frame yeniden hesaplanır, gecikme YOK!
        if (currentHandResult != null && currentHandResult!!.landmarks().isNotEmpty()) {
            HandSkeletonOverlay(
                result = currentHandResult!!,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                modifier = Modifier.fillMaxSize(),
                onPalmCenterScreenPosition = { position ->
                    // ⚡⚡⚡ GERÇEK ZAMANLI: Canvas içinde hesaplanan pozisyon
                    handReferencePosition = position

                    // ⚡⚡⚡ TAKİP MODU: Aktifse topu sürekli güncelle
                    if (isTrackingEnabled) {
                        vfxIconPosition = position
                    }
                    // ⚡⚡⚡ İlk el tespitinde VFX ikonunu avuç merkezine yapıştır
                    else if (!isIconInitialized) {
                        vfxIconPosition = position
                        isIconInitialized = true
                        Log.d(TAG, "📍 VFX icon initialized at palm center: $position")
                    }
                }
            )
        }

        // Referans Nokta İşaretleyici (Avuç Merkezi)
        if (handReferencePosition != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Green,
                    radius = 20f,
                    center = handReferencePosition!!,
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = Color.Green,
                    radius = 8f,
                    center = handReferencePosition!!
                )
            }
        }

        // Sürüklenebilir VFX İkonu (sadece el tespit edildiğinde göster)
        if (vfxIconPosition != null) {
            DraggableVFXIcon(
                position = vfxIconPosition!!,
                onPositionChanged = { newPos -> vfxIconPosition = newPos },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bağlantı Çizgisi (Referans Nokta → VFX İkonu)
        if (handReferencePosition != null && vfxIconPosition != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Cyan.copy(alpha = 0.6f),
                    start = handReferencePosition!!,
                    end = vfxIconPosition!!,
                    strokeWidth = 3f
                )
            }
        }

        // Üst Bilgi Paneli
        TopInfoPanel(
            gestureType = gestureType,
            handDetected = handReferencePosition != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        // ⚡⚡⚡ YENİ: SAĞ ÜST - Takip Kontrol Paneli
        TrackingControlPanel(
            isTrackingEnabled = isTrackingEnabled,
            onToggleTracking = { isTrackingEnabled = !isTrackingEnabled },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        // Alt Kontrol Paneli
        BottomControlPanel(
            canSave = handReferencePosition != null && handReference3D != null && vfxIconPosition != null,
            onSave = {
                // 3D offset hesapla
                val offset3D = calculateOffset3D(
                    vfxIconScreen = vfxIconPosition!!,
                    handReferenceScreen = handReferencePosition!!,
                    handReference3D = handReference3D!!,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight
                )

                val offsetConfig = VFXOffsetConfig(
                    gestureType = gestureType,
                    offset3D = offset3D,
                    referencePointLandmarkIndex = 9, // Avuç merkezi
                    timestamp = System.currentTimeMillis()
                )

                Log.d(TAG, "💾 Saving offset config: $offsetConfig")
                onSave(offsetConfig)
            },
            onCancel = onCancel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
        )
    }
}

/**
 * Sürüklenebilir VFX İkonu
 */
@Composable
private fun DraggableVFXIcon(
    position: Offset,
    onPositionChanged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val newPos = Offset(
                    x = (position.x + dragAmount.x).coerceIn(0f, size.width.toFloat()),
                    y = (position.y + dragAmount.y).coerceIn(0f, size.height.toFloat())
                )
                onPositionChanged(newPos)
            }
        }
    ) {
        // Dış halka (glow)
        drawCircle(
            color = Color(0xFF00BFFF).copy(alpha = 0.3f),
            radius = 100f,
            center = position
        )

        // Orta daire
        drawCircle(
            color = Color(0xFF00BFFF),
            radius = 60f,
            center = position,
            alpha = 0.8f
        )

        // İç nokta
        drawCircle(
            color = Color.White,
            radius = 20f,
            center = position
        )

        // Sürükle işareti (çarpı)
        val crossSize = 30f
        drawLine(
            color = Color.White,
            start = Offset(position.x - crossSize, position.y),
            end = Offset(position.x + crossSize, position.y),
            strokeWidth = 4f
        )
        drawLine(
            color = Color.White,
            start = Offset(position.x, position.y - crossSize),
            end = Offset(position.x, position.y + crossSize),
            strokeWidth = 4f
        )
    }
}

/**
 * ⚡⚡⚡ YENİ: Referans El Silüeti
 * El tespit edilmediğinde kullanıcıya rehberlik için soluk renkli bir el silüeti gösterir
 */
@Composable
private fun ReferenceHandSilhouette(
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Ekran merkezine yakın bir pozisyon belirle
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Sabit el silüeti landmark'ları (normalize edilmiş pozisyonlar)
        // MediaPipe hand landmark pozisyonlarını taklit ediyoruz
        val referenceLandmarks = listOf(
            // Bilek (0)
            Offset(centerX, centerY + 100f),
            // Başparmak (1-4)
            Offset(centerX - 40f, centerY + 60f),
            Offset(centerX - 60f, centerY + 30f),
            Offset(centerX - 70f, centerY),
            Offset(centerX - 80f, centerY - 20f),
            // İşaret parmağı (5-8)
            Offset(centerX - 20f, centerY + 40f),
            Offset(centerX - 20f, centerY),
            Offset(centerX - 20f, centerY - 40f),
            Offset(centerX - 20f, centerY - 80f),
            // Orta parmak (9-12)
            Offset(centerX, centerY + 40f), // Avuç merkezi
            Offset(centerX, centerY),
            Offset(centerX, centerY - 50f),
            Offset(centerX, centerY - 100f),
            // Yüzük parmağı (13-16)
            Offset(centerX + 20f, centerY + 40f),
            Offset(centerX + 20f, centerY),
            Offset(centerX + 20f, centerY - 40f),
            Offset(centerX + 20f, centerY - 80f),
            // Serçe parmak (17-20)
            Offset(centerX + 40f, centerY + 40f),
            Offset(centerX + 40f, centerY + 10f),
            Offset(centerX + 40f, centerY - 20f),
            Offset(centerX + 40f, centerY - 50f)
        )

        // El bağlantı çizgileri (soluk sarı)
        val connections = listOf(
            // Başparmak
            0 to 1, 1 to 2, 2 to 3, 3 to 4,
            // İşaret parmağı
            0 to 5, 5 to 6, 6 to 7, 7 to 8,
            // Orta parmak
            0 to 9, 9 to 10, 10 to 11, 11 to 12,
            // Yüzük parmağı
            0 to 13, 13 to 14, 14 to 15, 15 to 16,
            // Serçe parmak
            0 to 17, 17 to 18, 18 to 19, 19 to 20,
            // Avuç içi bağlantıları
            5 to 9, 9 to 13, 13 to 17
        )

        connections.forEach { (start, end) ->
            drawLine(
                color = Color.Yellow.copy(alpha = 0.3f),
                start = referenceLandmarks[start],
                end = referenceLandmarks[end],
                strokeWidth = 4f
            )
        }

        // Landmark noktaları (soluk sarı)
        referenceLandmarks.forEach { landmark ->
            drawCircle(
                color = Color.Yellow.copy(alpha = 0.3f),
                radius = 8f,
                center = landmark
            )
        }

        // Avuç merkezi özel işaretleme (landmark 9)
        drawCircle(
            color = Color.Yellow.copy(alpha = 0.5f),
            radius = 20f,
            center = referenceLandmarks[9],
            style = Stroke(width = 3f)
        )
    }
}

/**
 * El İskeleti Overlay (Yeşil Çizgiler)
 * ⚡⚡⚡ RAW MODE: MediaPipe'dan gelen veriyi filtresiz, gecikmesiz çiz!
 * HandLandmarkOverlay.kt'deki başarılı sistemi taklit ediyor
 */
@Composable
private fun HandSkeletonOverlay(
    result: HandLandmarkerResult,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier,
    onPalmCenterScreenPosition: (Offset) -> Unit = {}
) {
    Canvas(modifier = modifier) {
        if (result.landmarks().isEmpty()) return@Canvas

        val landmarks = result.landmarks()[0]

        // ⚡⚡⚡ RAW MODE: DOĞRUDAN Canvas içinde dönüşüm yap (gecikme YOK!)
        // Callback'ler yerine her frame'de yeniden hesapla
        val screenPositions = landmarks.mapIndexed { index, landmark ->
            val position = transformLandmarkToScreen(
                landmark = landmark,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                canvasWidth = size.width,
                canvasHeight = size.height,
                rotationDegrees = 0,
                isFrontCamera = true
            )

            // Avuç merkezi pozisyonunu bildir (sadece landmark 9 için)
            if (index == 9) {
                onPalmCenterScreenPosition(position)
            }

            position
        }

        // El bağlantı çizgileri (yeşil)
        val connections = listOf(
            // Başparmak
            0 to 1, 1 to 2, 2 to 3, 3 to 4,
            // İşaret parmağı
            0 to 5, 5 to 6, 6 to 7, 7 to 8,
            // Orta parmak
            0 to 9, 9 to 10, 10 to 11, 11 to 12,
            // Yüzük parmağı
            0 to 13, 13 to 14, 14 to 15, 15 to 16,
            // Serçe parmak
            0 to 17, 17 to 18, 18 to 19, 19 to 20,
            // Avuç içi bağlantıları
            5 to 9, 9 to 13, 13 to 17
        )

        // Bağlantı çizgilerini çiz
        connections.forEach { (start, end) ->
            if (start < screenPositions.size && end < screenPositions.size) {
                // Glow efekti
                drawLine(
                    color = Color.Green.copy(alpha = 0.3f),
                    start = screenPositions[start],
                    end = screenPositions[end],
                    strokeWidth = 12f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // Ana çizgi
                drawLine(
                    color = Color.Green.copy(alpha = 0.8f),
                    start = screenPositions[start],
                    end = screenPositions[end],
                    strokeWidth = 6f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        // Landmark noktaları (yeşil)
        screenPositions.forEach { position ->
            // Dış halka
            drawCircle(
                color = Color.Green.copy(alpha = 0.5f),
                radius = 10f,
                center = position
            )

            // İç nokta
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = position
            )
        }
    }
}

/**
 * ⚡⚡⚡ KRİTİK: Koordinat Dönüşüm Fonksiyonu
 * MediaPipe'ın normalize koordinatlarını ekran koordinatlarına doğru dönüştürür
 * (HandLandmarkOverlay.kt'den alınmıştır)
 */
private fun transformLandmarkToScreen(
    landmark: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
    imageWidth: Int,
    imageHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    rotationDegrees: Int,
    isFrontCamera: Boolean
): Offset {
    // 1. Normalize koordinatları ImageProxy koordinatlarına dönüştür
    var x = landmark.x() * imageWidth.toFloat()
    var y = landmark.y() * imageHeight.toFloat()

    // 2. Rotasyonu uygula
    val (rotatedX, rotatedY, rotatedWidth, rotatedHeight) = when (rotationDegrees) {
        0 -> Tuple4(x, y, imageWidth.toFloat(), imageHeight.toFloat())
        90 -> Tuple4(
            y,
            imageWidth.toFloat() - x,
            imageHeight.toFloat(),
            imageWidth.toFloat()
        )
        180 -> Tuple4(
            imageWidth.toFloat() - x,
            imageHeight.toFloat() - y,
            imageWidth.toFloat(),
            imageHeight.toFloat()
        )
        270 -> Tuple4(
            imageHeight.toFloat() - y,
            x,
            imageHeight.toFloat(),
            imageWidth.toFloat()
        )
        else -> Tuple4(x, y, imageWidth.toFloat(), imageHeight.toFloat())
    }

    // 3. Ön kamera için ayna efektini uygula
    var mirroredX = rotatedX
    var mirroredY = rotatedY

    if (isFrontCamera) {
        mirroredX = rotatedWidth - rotatedX
        mirroredY = rotatedHeight - mirroredY
    }

    // 4. Aspect ratio korunarak ölçekleme ve offset hesapla
    val srcAspect = rotatedWidth / rotatedHeight
    val dstAspect = canvasWidth / canvasHeight

    val (scale, offsetX, offsetY) = if (srcAspect > dstAspect) {
        val s = canvasWidth / rotatedWidth
        val scaledHeight = rotatedHeight * s
        val offY = (canvasHeight - scaledHeight) / 2f
        Triple(s, 0f, offY)
    } else {
        val s = canvasHeight / rotatedHeight
        val scaledWidth = rotatedWidth * s
        val offX = (canvasWidth - scaledWidth) / 2f
        Triple(s, offX, 0f)
    }

    // 5. Ölçekleme ve offset uygula
    val screenX = mirroredX * scale + offsetX
    val screenY = mirroredY * scale + offsetY

    return Offset(screenX, screenY)
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * ⚡⚡⚡ YENİ: Takip Kontrol Paneli (Sağ Üst Köşe)
 * VFX ikonunun eli takip edip etmeyeceğini kontrol eder
 */
@Composable
private fun TrackingControlPanel(
    isTrackingEnabled: Boolean,
    onToggleTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.8f),
        onClick = onToggleTracking
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // İkon
            Text(
                text = if (isTrackingEnabled) "🔓" else "🔒",
                fontSize = 20.sp
            )

            // Durum metni
            Column {
                Text(
                    text = if (isTrackingEnabled) "Takip Açık" else "Pozisyon Kilitli",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTrackingEnabled) Color(0xFF00FF88) else Color(0xFFFF9800)
                )
                Text(
                    text = if (isTrackingEnabled) "Top eli takip ediyor" else "Top sabit duruyor",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Üst Bilgi Paneli
 */
@Composable
private fun TopInfoPanel(
    gestureType: GestureType,
    handDetected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when (gestureType) {
                    GestureType.CREATION -> "🖐️ Yaratma Hareketi - 3D Offset Ayarı"
                    GestureType.DIRECTION -> "✌️ Yönlendirme Hareketi - 3D Offset Ayarı"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

            Text(
                text = if (handDetected) {
                    "✅ El İskeleti Görünür - Mavi İkonu Sürükleyin"
                } else {
                    "⚠️ Lütfen Elinizi Kameraya Gösterin"
                },
                fontSize = 13.sp,
                color = if (handDetected) Color.Green else Color.Yellow,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (handDetected) {
                Text(
                    text = rememberLocalizedText("blue_icon_appears"),
                    fontSize = 11.sp,
                    color = Color.Cyan,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = rememberLocalizedText("drag_icon_instruction"),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = rememberLocalizedText("cyan_line_offset"),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * Alt Kontrol Paneli
 */
@Composable
private fun BottomControlPanel(
    canSave: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // İptal Butonu
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = rememberLocalizedText("cancel"),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Kaydet Butonu
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (canSave) "✅ Kaydet" else "⏳ El Bekleniyor",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 3D Offset Hesaplama
 *
 * Ekran koordinatlarını 3D normalize uzaya çevirip fark vektörünü hesaplar
 */
private fun calculateOffset3D(
    vfxIconScreen: Offset,
    handReferenceScreen: Offset,
    handReference3D: Vector3f,
    imageWidth: Int,
    imageHeight: Int
): Vector3f {
    // Ekran koordinatlarını normalize et (0-1 arası)
    val vfxNormalized = Offset(
        x = vfxIconScreen.x / imageWidth,
        y = vfxIconScreen.y / imageHeight
    )

    // Z değerini el referansından al (derinlik korunsun)
    val vfxIcon3D = Vector3f(
        x = vfxNormalized.x,
        y = vfxNormalized.y,
        z = handReference3D.z
    )

    // Offset vektörü = VFX pozisyonu - El referans pozisyonu
    val offset = vfxIcon3D - handReference3D

    Log.d(TAG, "📐 Offset hesaplandı: offset=$offset")
    Log.d(TAG, "📐 Hand Ref 3D: $handReference3D, VFX Icon 3D: $vfxIcon3D")

    return offset
}

/**
 * İzin İsteme Ekranı
 */
@Composable
private fun PermissionRequestScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(rememberLocalizedText("camera_permission_required"), color = Color.White)
            TextButton(onClick = onBack) {
                Text(rememberLocalizedText("back"), color = Color.White)
            }
        }
    }
}

/**
 * Hata Ekranı
 */
@Composable
private fun ErrorScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("❌ ${rememberLocalizedText("camera_failed")}", color = Color.White)
            Button(onClick = onBack) {
                Text(rememberLocalizedText("back"))
            }
        }
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

private const val TAG = "VFXOffsetCalibration"
