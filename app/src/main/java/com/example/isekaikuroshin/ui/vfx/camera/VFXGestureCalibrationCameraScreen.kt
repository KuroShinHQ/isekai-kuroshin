package com.example.isekaikuroshin.ui.vfx.camera

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.DefaultSeals
import com.example.isekaikuroshin.data.GestureType
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.engine.GestureRecognitionEngine
import com.example.isekaikuroshin.ui.components.CameraPreview
import com.example.isekaikuroshin.ui.sealpractice.HandDetectorHelper
import com.example.isekaikuroshin.ui.sealpractice.HandLandmarkOverlay
import com.example.isekaikuroshin.ui.vfx.CalibrationResult
import com.example.isekaikuroshin.ui.vfx.VFXWorkshopViewModel
import com.example.isekaikuroshin.utils.DeviceUtils
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * VFX Gesture Kalibrasyon Kamera Ekranı
 *
 * Kullanıcının VFX Workshop için bir hareketi kalibre edebileceği kamera ekranı.
 * Sadece bir kez hareket kaydeder ve geri döner.
 */
/**
 * ⚡⚡⚡ NİHAİ DÜZELTME: Shared ViewModel (Default Parameter YOK!)
 *
 * Bu ekran "aptal" bir Composable'dır. Kendi ViewModel'i YOKTUR.
 * Tüm veri ve fonksiyonları NavGraph'tan gelen paylaşılan ViewModel'den alır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun VFXGestureCalibrationCameraScreen(
    navController: NavController,
    gestureTypeStr: String,
    viewModel: VFXWorkshopViewModel  // ⚡ NO DEFAULT! NavGraph'tan gelmelidir
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // GestureRecognitionEngine (Singleton - direkt oluştur)
    // Not: Singleton olduğu için her çağrıda aynı instance dönecek
    val gestureRecognitionEngine = remember {
        GestureRecognitionEngine()
    }

    // Parse gesture type
    val gestureType = try {
        GestureType.valueOf(gestureTypeStr.uppercase())
    } catch (e: Exception) {
        Log.e(TAG, "❌ Invalid gesture type: $gestureTypeStr")
        GestureType.CREATION // fallback
    }

    // Emulator detection
    val isEmulator = remember { DeviceUtils.isEmulator() }

    // HandDetectorHelper
    val handDetectorHelper = remember(isEmulator) {
        try {
            Log.d(TAG, "🔧 HandDetectorHelper creating for VFX gesture calibration...")
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

    // Error handling
    if (handDetectorHelper == null) {
        ErrorScreen(
            navController = navController,
            onNavigateBack = { navController.navigateUp() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = rememberLocalizedText("gesture_calibration"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when (gestureType) {
                                GestureType.CREATION -> "🖐️ ${rememberLocalizedText("creation_gesture")}"
                                GestureType.DIRECTION -> "✌️ ${rememberLocalizedText("direction_gesture")}"
                            },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = rememberLocalizedText("back"),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        VFXGestureCalibrationContent(
            modifier = Modifier.padding(paddingValues),
            gestureType = gestureType,
            viewModel = viewModel,
            handDetectorHelper = handDetectorHelper,
            lifecycleOwner = lifecycleOwner,
            gestureRecognitionEngine = gestureRecognitionEngine,
            onBackPressed = {
                // Kullanıcı manuel olarak geri döndü
                navController.navigateUp()
            }
        )
    }
}

/**
 * VFX Gesture Kalibrasyon İçeriği
 *
 * Gelişmiş özellikler:
 * - Çoklu açı kaydı
 * - Canlı doğruluk göstergesi (GestureRecognitionEngine ile)
 * - Kaydedilen açıları görüntüleme ve silme
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun VFXGestureCalibrationContent(
    modifier: Modifier = Modifier,
    gestureType: GestureType,
    viewModel: VFXWorkshopViewModel,
    handDetectorHelper: HandDetectorHelper,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    gestureRecognitionEngine: GestureRecognitionEngine,
    onBackPressed: () -> Unit
) {
    // Hand detection state
    var currentHandResult by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(640) }
    var imageHeight by remember { mutableIntStateOf(480) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // Snackbar state
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Repository'den reaktif veri (Single Source of Truth)
    val currentSeal by when (gestureType) {
        GestureType.CREATION -> viewModel.creationSeal
        GestureType.DIRECTION -> viewModel.directionSeal
    }.collectAsState()

    val savedAngleCount = currentSeal?.templateLandmarksList?.size ?: 0

    // ⚡⚡⚡ YENİ: Canlı doğruluğu ViewModel'den al (çalışan kodun mantığı)
    val accuracyMap by viewModel.currentAccuracy.collectAsState()
    val currentAccuracy = accuracyMap[gestureType] ?: 0f

    // Açı listesi görünürlüğü
    var showAngleList by remember { mutableStateOf(false) }

    // ⚡⚡⚡ DÜZELTİLDİ: ViewModel'in processHandDetection fonksiyonunu çağır
    LaunchedEffect(handDetectorHelper) {
        handDetectorHelper.results.collect { result ->
            currentHandResult = result
            // ViewModel'e gönder (çalışan kodun mantığı)
            viewModel.processHandDetection(result, gestureType, gestureRecognitionEngine)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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

        // Top info panel with calibration list
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
                    text = when (gestureType) {
                        GestureType.CREATION -> "🖐️ ${rememberLocalizedText("open_hand_gesture")}"
                        GestureType.DIRECTION -> "✌️ ${rememberLocalizedText("peace_sign")}"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Kaydedilen Açı Sayısı (clickable)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLocalizedText("saved_angles"),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Açı sayısını Repository'den al
                        Text(
                            text = "$savedAngleCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6A4C93)
                        )
                        if (savedAngleCount > 0) {
                            IconButton(
                                onClick = { showAngleList = !showAngleList },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (showAngleList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (showAngleList) rememberLocalizedText("hide_list") else rememberLocalizedText("show_list"),
                                    tint = Color(0xFF6A4C93)
                                )
                            }
                        }
                    }
                }

                // Calibration list (expandable)
                // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Repository'den gelen açı sayısını kullan
                if (showAngleList && savedAngleCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // ⚡⚡⚡ DÜZELTME: itemsIndexed kullan (items değil!)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(currentSeal?.templateLandmarksList ?: emptyList()) { index, _ ->
                            CalibrationAngleItem(
                                index = index + 1,
                                onDelete = {
                                    Log.d(TAG, "🗑️ [DELETE] Deleting angle $index from $gestureType")
                                    viewModel.deleteCalibrationAngle(gestureType, index)
                                    snackbarMessage = "🗑️ Açı ${index + 1} silindi"
                                    showSnackbar = true
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Canlı doğruluk göstergesi (her zaman göster)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = rememberLocalizedText("live_match"),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        // Referans bilgisi göster
                        if (savedAngleCount == 0) {
                            Text(
                                text = rememberLocalizedText("no_angles_yet"),
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Text(
                        text = "${(currentAccuracy * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = getAccuracyColor(currentAccuracy)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { currentAccuracy.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = getAccuracyColor(currentAccuracy),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // El Algılama Durumu
                val isHandDetected = currentHandResult != null && currentHandResult!!.landmarks().isNotEmpty()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLocalizedText("hand_status"),
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
                            text = if (isHandDetected) rememberLocalizedText("detected") else rememberLocalizedText("not_detected"),
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
                    text = rememberLocalizedText("hold_hand_instruction"),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Yeni Açı Ekle butonu
                    Button(
                        onClick = {
                            Log.d(TAG, "🔵 [ADD_BUTTON] Button clicked! Current angle count BEFORE: $savedAngleCount")
                            Log.d(TAG, "🔵 [ADD_BUTTON] Hand detected: ${currentHandResult != null}")

                            // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Açı ekle (Repository'ye direkt yaz)
                            viewModel.addAngleWithNormalization(
                                result = currentHandResult,
                                gestureType = gestureType,
                                recognitionEngine = gestureRecognitionEngine
                            )

                            // ⚡⚡⚡ DÜZELTME: StateFlow güncellenene kadar bekle
                            snackbarMessage = "✅ Yeni açı eklendi!"
                            showSnackbar = true

                            Log.d(TAG, "🔵 [ADD_BUTTON] Current angle count AFTER (will update): ${savedAngleCount + 1}")
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

                    // Sıfırla butonu
                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllCalibrations(gestureType)
                            snackbarMessage = "🗑️ Tüm açılar temizlendi"
                            showSnackbar = true
                        },
                        modifier = Modifier
                            .size(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = rememberLocalizedText("reset"),
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    }
}

/**
 * Hata Ekranı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScreen(
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(rememberLocalizedText("error"), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, rememberLocalizedText("back"), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                Text("❌ ${rememberLocalizedText("camera_failed")}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onNavigateBack) {
                    Text(rememberLocalizedText("go_back"))
                }
            }
        }
    }
}

/**
 * Kalibrasyon Açısı Liste Öğesi
 */
@Composable
fun CalibrationAngleItem(
    index: Int,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF6A4C93),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = rememberLocalizedText("saved_angle_index").replace("{index}", index.toString()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = rememberLocalizedText("delete"),
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Doğruluk rengi hesaplama
 */
fun getAccuracyColor(accuracy: Float): Color {
    return when {
        accuracy >= 0.9f -> Color(0xFF00FF88)
        accuracy >= 0.7f -> Color(0xFFFFD700)
        accuracy >= 0.5f -> Color(0xFFFF9800)
        else -> Color(0xFFFF5252)
    }
}

/**
 * ImageProxy to Bitmap extension
 * Converts YUV_420_888 format to Bitmap for MediaPipe processing
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

private const val TAG = "VFXGestureCalibrationCameraScreen"
