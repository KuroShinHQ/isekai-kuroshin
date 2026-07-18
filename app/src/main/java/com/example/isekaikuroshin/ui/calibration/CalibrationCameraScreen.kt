package com.example.isekaikuroshin.ui.calibration

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.getCalibrationCount
import com.example.isekaikuroshin.ui.components.CameraPreview
import com.example.isekaikuroshin.ui.components.DebugOverlay
import com.example.isekaikuroshin.ui.sealpractice.HandDetectorHelper
import com.example.isekaikuroshin.ui.sealpractice.HandLandmarkOverlay
import com.example.isekaikuroshin.utils.DeviceUtils
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Kalibrasyon Kamera Ekranı
 *
 * Bu ekran, kullanıcının farklı açılardan el pozisyonlarını kaydedebileceği
 * canlı kamera ekranıdır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun CalibrationCameraScreen(
    navController: NavController,
    sealId: String,
    viewModel: CalibrationCameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()

    // Load seal on first composition
    LaunchedEffect(sealId) {
        viewModel.loadSeal(sealId)
    }

    // Emulator detection
    val isEmulator = remember { DeviceUtils.isEmulator() }

    // HandDetectorHelper
    val handDetectorHelper = remember(isEmulator) {
        try {
            Log.d(TAG, "🔧 HandDetectorHelper creating for calibration...")
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
        ErrorScreen(navController = navController)
        return
    }

    if (uiState.selectedSeal == null) {
        LoadingScreen()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kalibrasyon",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = rememberLocalizedText(uiState.selectedSeal!!.nameKey),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDebugMode() }) {
                        Icon(
                            imageVector = if (uiState.isDebugMode) Icons.Default.BugReport else Icons.Default.Build,
                            contentDescription = "Debug Mode",
                            tint = if (uiState.isDebugMode) Color(0xFF00FF88) else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        CalibrationCameraContent(
            modifier = Modifier.padding(paddingValues),
            seal = uiState.selectedSeal!!,
            handDetectorHelper = handDetectorHelper,
            lifecycleOwner = lifecycleOwner,
            currentAccuracy = uiState.currentAccuracy,
            isDebugMode = uiState.isDebugMode,
            debugInfo = uiState.debugInfo,
            onAddCalibration = { landmarks ->
                viewModel.addCalibrationAngle(landmarks)
            },
            onClearCalibrations = {
                viewModel.clearAllCalibrations()
            },
            onDeleteCalibration = { index ->
                viewModel.deleteCalibrationAngle(index)
            },
            onHandDetected = { result ->
                viewModel.processHandDetection(result)
            }
        )
    }
}

/**
 * Kalibrasyon Kamera İçeriği
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun CalibrationCameraContent(
    modifier: Modifier = Modifier,
    seal: Seal,
    handDetectorHelper: HandDetectorHelper,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    currentAccuracy: Float,
    isDebugMode: Boolean,
    debugInfo: com.example.isekaikuroshin.engine.DebugInfo,
    onAddCalibration: (List<NormalizedPoint>) -> Unit,
    onClearCalibrations: () -> Unit,
    onDeleteCalibration: (Int) -> Unit,
    onHandDetected: (HandLandmarkerResult) -> Unit
) {
    // Hand detection state
    var currentHandResult by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(640) }
    var imageHeight by remember { mutableIntStateOf(480) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // Snackbar state
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // List visibility state
    var showCalibrationList by remember { mutableStateOf(false) }

    // Collect hand detection results
    LaunchedEffect(handDetectorHelper) {
        handDetectorHelper.results.collect { result ->
            currentHandResult = result
            onHandDetected(result)
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

        // Hand landmark overlay (sadece debug mode kapalıyken göster)
        if (!isDebugMode) {
            HandLandmarkOverlay(
                handResult = currentHandResult,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = 0f,
                viewHeight = 0f,
                rotationDegrees = rotationDegrees,
                isFrontCamera = true
            )
        }

        // Debug overlay (sadece debug mode açıkken göster)
        if (isDebugMode) {
            DebugOverlay(
                debugInfo = debugInfo,
                modifier = Modifier.fillMaxSize()
            )
        }

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
                // Kayıtlı açı sayısı (clickable)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${seal.getCalibrationCount()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (seal.getCalibrationCount() > 0) {
                            IconButton(
                                onClick = { showCalibrationList = !showCalibrationList },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (showCalibrationList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (showCalibrationList) "Listeyi Gizle" else "Listeyi Göster",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Calibration list (expandable)
                if (showCalibrationList && seal.templateLandmarksList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(seal.templateLandmarksList) { index, _ ->
                            CalibrationAngleItem(
                                index = index + 1,
                                onDelete = {
                                    onDeleteCalibration(index)
                                    snackbarMessage = "✅ Açı ${index + 1} silindi"
                                    showSnackbar = true
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Canlı doğruluk göstergesi
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Yeni Açı Ekle butonu
                    Button(
                        onClick = {
                            currentHandResult?.let { result ->
                                if (result.landmarks().isNotEmpty()) {
                                    val landmarks = result.landmarks()[0]
                                    val normalizedLandmarks = landmarks.map { landmark ->
                                        NormalizedPoint(
                                            x = landmark.x(),
                                            y = landmark.y(),
                                            z = landmark.z()
                                        )
                                    }
                                    onAddCalibration(normalizedLandmarks)
                                    snackbarMessage = "✅ Yeni açı eklendi!"
                                    showSnackbar = true
                                } else {
                                    snackbarMessage = "❌ El algılanamadı"
                                    showSnackbar = true
                                }
                            } ?: run {
                                snackbarMessage = "❌ El algılanamadı"
                                showSnackbar = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
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
                            onClearCalibrations()
                            snackbarMessage = "🗑️ Kalibrasyonlar sıfırlandı"
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
                            contentDescription = "Sıfırla",
                            tint = Color(0xFFFF5252)
                        )
                    }
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = rememberLocalizedText("saved_angle_num").replace("{index}", index.toString()),
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
                    contentDescription = "Sil",
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
 * Hata Ekranı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hata") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
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
            Text("❌ Kamera başlatılamadı")
        }
    }
}

/**
 * Yükleniyor Ekranı
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
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

private const val TAG = "CalibrationCameraScreen"
