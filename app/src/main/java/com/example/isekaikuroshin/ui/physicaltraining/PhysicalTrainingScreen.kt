package com.example.isekaikuroshin.ui.physicaltraining

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.engine.pose.PoseEstimationEngine
import com.example.isekaikuroshin.ui.components.CameraPreview
import com.example.isekaikuroshin.ui.exercise.FormQuality
import com.example.isekaikuroshin.ui.exercise.PoseDetectorHelper
import com.example.isekaikuroshin.ui.exercise.PoseOverlay

/**
 * Physical Training Screen - Fiziksel Antrenman Ekranı
 *
 * Kadim Mühür sisteminden öğrenilen tekniklerle geliştirilmiş:
 * - Kalman filtreli iskelet stabilizasyonu
 * - State management ve tracking loss handling
 * - Kullanıcıya özel kalibrasyon
 * - Akıllı tekrar sayma
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalTrainingScreen(
    navController: NavController,
    viewModel: PhysicalTrainingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Log.d(TAG, "🎬 PhysicalTrainingScreen başlatılıyor...")

    val uiState by viewModel.uiState.collectAsState()

    // Pose detection helper
    var currentPose by remember { mutableStateOf<com.google.mlkit.vision.pose.Pose?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var viewWidth by remember { mutableFloatStateOf(0f) }
    var viewHeight by remember { mutableFloatStateOf(0f) }

    val poseDetectorHelper = remember {
        try {
            Log.d(TAG, "🔧 PoseDetectorHelper oluşturuluyor...")
            PoseDetectorHelper(context).also {
                Log.d(TAG, "✅ PoseDetectorHelper başarıyla oluşturuldu")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ PoseDetectorHelper oluşturma hatası: ${e.message}", e)
            null
        }
    }

    // Hata durumu - poseDetectorHelper null kontrolü
    if (poseDetectorHelper == null) {
        ErrorScreen(onBack = { navController.navigateUp() })
        return
    }

    // Cleanup - poseDetectorHelper artık null değil
    DisposableEffect(poseDetectorHelper) {
        onDispose {
            try {
                poseDetectorHelper.close()
                Log.d(TAG, "✅ PhysicalTrainingScreen disposed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cleanup hatası: ${e.message}", e)
            }
        }
    }

    // G91: Scaffold + 9 ikonlu navigation bar eklendi (tutarlılık için)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "💪 " + rememberLocalizedText("physical_training"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "⬅️ Geri butonu tıklandı")
                        navController.navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    // SORUN 5: Kalibrasyon butonu kaldırıldı
                    // if (uiState.selectedExercise != null && !uiState.isCalibrating) {
                    //     IconButton(onClick = { viewModel.startCalibration() }) {
                    //         Icon(
                    //             imageVector = Icons.Default.Settings,
                    //             contentDescription = "Kalibrasyon",
                    //             tint = if (uiState.hasCalibration) Color.Green else Color.Gray
                    //         )
                    //     }
                    // }

                    // Sıfırlama butonu
                    if (uiState.isSessionActive) {
                        IconButton(onClick = { viewModel.resetExercise() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sıfırla",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // G91: 9 ikonlu navigation bar
            BottomNavigationBar(
                navController = navController,
                currentRoute = "physical_training",
                selectedItem = com.example.isekaikuroshin.ui.navigation.BottomNavItem.HealthHub
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Egzersiz seçimi
                uiState.selectedExercise == null -> {
                    ExerciseSelectionScreen(
                        onExerciseSelected = { exerciseType ->
                            viewModel.selectExercise(exerciseType)
                        }
                    )
                }

                // Kalibrasyon aktif
                uiState.isCalibrating -> {
                    CalibrationScreen(
                        uiState = uiState,
                        poseDetectorHelper = poseDetectorHelper,
                        lifecycleOwner = lifecycleOwner,
                        onCapturePose = { pose -> viewModel.captureCalibrationPose(pose) },
                        onCancel = { viewModel.cancelCalibration() },
                        onFinish = { viewModel.finishCalibration() }
                    )
                }

                // Egzersiz aktif
                else -> {
                    ExerciseSessionScreen(
                        uiState = uiState,
                        poseDetectorHelper = poseDetectorHelper,
                        lifecycleOwner = lifecycleOwner,
                        onPoseDetected = { pose -> viewModel.processPose(pose) },
                        onFinish = { viewModel.finishExercise() }
                    )
                }
            }
        }
    }
}

/**
 * Egzersiz Seçim Ekranı
 */
@Composable
fun ExerciseSelectionScreen(
    onExerciseSelected: (PoseEstimationEngine.ExerciseType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = rememberLocalizedText("select_exercise"),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Hangi egzersizi yapmak istiyorsun?",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // TODO-G121: KURAL 9 - Hardcoded text kaldırıldı
        // Şınav
        ExerciseCard(
            title = rememberLocalizedText("exercise_type_push_up"),
            icon = "💪",
            description = rememberLocalizedText("exercise_push_up_desc"),
            onClick = { onExerciseSelected(PoseEstimationEngine.ExerciseType.PUSH_UP) }
        )

        // Mekik
        ExerciseCard(
            title = rememberLocalizedText("exercise_type_sit_up"),
            icon = "🔥",
            description = rememberLocalizedText("exercise_sit_up_desc"),
            onClick = { onExerciseSelected(PoseEstimationEngine.ExerciseType.SIT_UP) }
        )

        // İp Atlama
        ExerciseCard(
            title = rememberLocalizedText("exercise_type_rope_skipping"),
            icon = "🏃",
            description = rememberLocalizedText("exercise_rope_skipping_desc"),
            onClick = { onExerciseSelected(PoseEstimationEngine.ExerciseType.ROPE_SKIPPING) }
        )
    }
}

/**
 * Egzersiz Kartı
 */
@Composable
fun ExerciseCard(
    title: String,
    icon: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 48.sp,
                modifier = Modifier.padding(end = 20.dp)
            )

            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Egzersiz Oturumu Ekranı
 */
@Composable
fun ExerciseSessionScreen(
    uiState: PhysicalTrainingUiState,
    poseDetectorHelper: PoseDetectorHelper,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onPoseDetected: (com.google.mlkit.vision.pose.Pose?) -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentPose by remember { mutableStateOf<com.google.mlkit.vision.pose.Pose?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var viewWidth by remember { mutableFloatStateOf(0f) }
    var viewHeight by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Kamera preview
        CameraPreview(
            lifecycleOwner = lifecycleOwner,
            imageAnalyzer = { imageProxy ->
                try {
                    val pose = poseDetectorHelper.detectPoseBlocking(imageProxy)

                    ContextCompat.getMainExecutor(context).execute {
                        currentPose = pose
                        imageWidth = imageProxy.width
                        imageHeight = imageProxy.height
                        onPoseDetected(pose)  // Tekrar sayma için ViewModel'e gönder
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Pose detection error: ${e.message}", e)
                } finally {
                    imageProxy.close()
                }
            },
            onViewSizeChanged = { width, height ->
                viewWidth = width
                viewHeight = height
            },
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        )

        // TODO-G121: İskelet koordinat transformation düzeltmesi
        // G122'de eklenen transformPoseLandmarkToScreen() tekniğini kullanmak için
        // normalizedLandmarks yerine emptyList() kullanıyoruz (fallback yolu)
        // Bu sayede aspect ratio + offset hesabı devreye giriyor!
        PoseOverlay(
            pose = currentPose,  // Ham MediaPipe verisi
            normalizedLandmarks = emptyList(),  // TODO-G121: Fallback yolu kullan (transformPoseLandmarkToScreen)
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            formQuality = getFormQualityEnum(uiState.formQuality),
            relevantLandmarkTypes = uiState.relevantLandmarkTypes  // Egzersize özel filtreleme
        )

        // Sayaç ve feedback UI
        ExerciseStatsOverlay(
            uiState = uiState,
            onFinish = onFinish
        )
    }
}

/**
 * İstatistik Overlay
 */
@Composable
fun BoxScope.ExerciseStatsOverlay(
    uiState: PhysicalTrainingUiState,
    onFinish: () -> Unit
) {
    // Üst: Tekrar sayacı
    Surface(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 24.dp)
            .size(120.dp),
        shape = RoundedCornerShape(60.dp),
        color = getCounterColor(uiState.formQuality),
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = uiState.repCount.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }

    // Alt: Feedback
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(16.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.feedback,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (uiState.repCount >= 3) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("🏁 Bitir", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Kalibrasyon Ekranı - Canlı İskelet Görüntüleme ile
 */
@Composable
fun CalibrationScreen(
    uiState: PhysicalTrainingUiState,
    poseDetectorHelper: PoseDetectorHelper,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCapturePose: (com.google.mlkit.vision.pose.Pose) -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentPose by remember { mutableStateOf<com.google.mlkit.vision.pose.Pose?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var viewWidth by remember { mutableFloatStateOf(0f) }
    var viewHeight by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Kamera preview
        CameraPreview(
            lifecycleOwner = lifecycleOwner,
            imageAnalyzer = { imageProxy ->
                try {
                    val pose = poseDetectorHelper.detectPoseBlocking(imageProxy)
                    ContextCompat.getMainExecutor(context).execute {
                        currentPose = pose
                        imageWidth = imageProxy.width
                        imageHeight = imageProxy.height
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Pose detection error: ${e.message}", e)
                } finally {
                    imageProxy.close()
                }
            },
            onViewSizeChanged = { width, height ->
                viewWidth = width
                viewHeight = height
            },
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        )

        // ⚡ CANLI İskelet Overlay - Kalibrasyon sırasında da projeksiyon kullan
        PoseOverlay(
            pose = currentPose,  // Ham MediaPipe verisi (konum için)
            normalizedLandmarks = emptyList(),  // Kalibrasyon sırasında normalizasyon yok
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            formQuality = if (currentPose != null) FormQuality.NEUTRAL else FormQuality.INVALID
        )

        // Kalibrasyon Diyalogu - Kullanıcı etkileşimi ile güncellenir
        CalibrationDialog(
            uiState = uiState,
            isPoseDetected = currentPose != null,
            onCapturePose = {
                currentPose?.let { onCapturePose(it) }
            },
            onCancel = onCancel,
            onFinish = onFinish
        )
    }
}

/**
 * Kalibrasyon Diyalogu - State yönetimi kamera akışından ayrı
 */
@Composable
fun BoxScope.CalibrationDialog(
    uiState: PhysicalTrainingUiState,
    isPoseDetected: Boolean,
    onCapturePose: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kalibrasyon mesajı
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.calibrationMessage,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Durum göstergesi
                Text(
                    text = if (isPoseDetected) "✅ Vücut Algılandı" else "⏳ Vücut Bekleniyor...",
                    fontSize = 14.sp,
                    color = if (isPoseDetected) Color(0xFF4CAF50) else Color(0xFFFFB74D)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Butonlar - Sadece kullanıcı etkileşimi ile güncellenir
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // İptal
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(rememberLocalizedText("cancel_button"))
            }

            // Kaydet veya Tamamla
            if (uiState.calibrationStep != CalibrationStep.COMPLETE) {
                Button(
                    onClick = onCapturePose,
                    enabled = isPoseDetected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BCD4)
                    )
                ) {
                    Text("📸 ${rememberLocalizedText("save_button")}")
                }
            } else {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("✅ ${rememberLocalizedText("complete")}")
                }
            }
        }
    }
}

/**
 * Hata Ekranı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hata") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Text("❌ Pose Detection başlatılamadı")
        }
    }
}

// Helper functions
fun getCounterColor(formQuality: Float): Color {
    return when {
        formQuality >= 0.8f -> Color(0xFF4CAF50)
        formQuality >= 0.5f -> Color(0xFFFFEB3B)
        else -> Color(0xFFF44336)
    }
}

fun getFormQualityEnum(formQuality: Float): com.example.isekaikuroshin.ui.exercise.FormQuality {
    return when {
        formQuality >= 0.9f -> com.example.isekaikuroshin.ui.exercise.FormQuality.EXCELLENT
        formQuality >= 0.7f -> com.example.isekaikuroshin.ui.exercise.FormQuality.GOOD
        formQuality >= 0.5f -> com.example.isekaikuroshin.ui.exercise.FormQuality.NEUTRAL
        formQuality >= 0.3f -> com.example.isekaikuroshin.ui.exercise.FormQuality.POOR
        else -> com.example.isekaikuroshin.ui.exercise.FormQuality.INVALID
    }
}

private const val TAG = "PhysicalTrainingScreen"

// G91: 9 ikonlu navigation bar component (NavController tabanlı)
@Composable
private fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String,
    selectedItem: com.example.isekaikuroshin.ui.navigation.BottomNavItem
) {
    val items = listOf(
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Status,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Inventory,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Quests,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Catalog,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Camp,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Map,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Journal,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.HealthHub,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Settings
    )

    NavigationBar {
        items.forEach { item ->
            val localizedTitle = com.example.isekaikuroshin.data.rememberLocalizedText(item.titleKey)
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = localizedTitle) },
                label = { Text(text = localizedTitle) },
                selected = item == selectedItem,
                onClick = {
                    // G91 FIX: Sub-screen'den MainScreen sayfalarına dönmek için popBackStack kullan
                    if (item != selectedItem) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
