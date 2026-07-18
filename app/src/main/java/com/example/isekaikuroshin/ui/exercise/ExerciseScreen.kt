package com.example.isekaikuroshin.ui.exercise

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.mlkit.vision.pose.Pose
import com.example.isekaikuroshin.ui.components.CameraPreview
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * Exercise Screen - Egzersiz Ekranı (Şınav Sayma)
 *
 * Bu ekran, telefon kamerasını açar ve ML Kit kullanarak
 * tespit edilen iskelet yapısını gösterir ve şınav sayar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ExerciseScreen(
    navController: NavController,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Log.d(TAG, "🎬 ExerciseScreen başlatılıyor...")

    // Pose state
    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var viewWidth by remember { mutableFloatStateOf(0f) }
    var viewHeight by remember { mutableFloatStateOf(0f) }

    // PoseDetectorHelper - Hata durumunda null döndür
    val poseDetectorHelper = remember {
        try {
            Log.d(TAG, "🔧 PoseDetectorHelper oluşturuluyor...")
            PoseDetectorHelper(context).also {
                Log.d(TAG, "✅ PoseDetectorHelper başarıyla oluşturuldu")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ PoseDetectorHelper oluşturma hatası: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    // Eğer PoseDetectorHelper oluşturulamadıysa hata ekranı göster
    if (poseDetectorHelper == null) {
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
                        text = "❌ " + rememberLocalizedText("exercise_pose_detection_failed"),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = rememberLocalizedText("exercise_ml_kit_unavailable"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { navController.navigateUp() }) {
                        Text(rememberLocalizedText("exercise_go_back"))
                    }
                }
            }
        }
        return
    }

    // ═══ INITIALIZATION STATE (Performans Optimizasyonu) ═══
    val initializationState by viewModel.initializationState.collectAsState()

    // ViewModel state'leri
    val exerciseCount by viewModel.exerciseCount.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val formQuality by viewModel.formQuality.collectAsState()
    val currentAngle by viewModel.currentAngle.collectAsState()
    val exerciseState by viewModel.exerciseState.collectAsState()
    val formScore by viewModel.formScore.collectAsState()  // YENİ: Form skoru
    val repSuccessTrigger by viewModel.repSuccessTrigger.collectAsState()  // YENİ: Başarı efekti trigger

    // Cleanup
    DisposableEffect(poseDetectorHelper) {
        Log.d(TAG, "📌 DisposableEffect başlatıldı")
        onDispose {
            try {
                Log.d(TAG, "🧹 Kaynaklar temizleniyor...")
                poseDetectorHelper.close()
                Log.d(TAG, "✅ ExerciseScreen disposed, resources cleaned")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cleanup hatası: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "💪 " + rememberLocalizedText("exercise_push_up_counter"),
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
                            contentDescription = rememberLocalizedText("back")
                        )
                    }
                },
                actions = {
                    // Sıfırlama butonu
                    IconButton(onClick = {
                        viewModel.resetExercise()
                        Log.d(TAG, "🔄 Egzersiz sıfırlandı")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = rememberLocalizedText("exercise_reset"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ═══ LOADING SCREEN (Başlatma sırasında göster) ═══
            when (initializationState) {
                is InitializationState.Loading -> {
                    // Yükleme ekranı - arka planda bileşenler yüklenirken
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Dönme animasyonu ile yükleme göstergesi
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = Color(0xFF00FF88),
                                strokeWidth = 6.dp
                            )

                            Text(
                                text = "⚡ " + rememberLocalizedText("exercise_preparing"),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = rememberLocalizedText("exercise_pose_system_loading"),
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    return@Box  // Loading state'de kamera başlatma
                }

                is InitializationState.Error -> {
                    // Hata ekranı
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 64.sp
                            )
                            Text(
                                text = rememberLocalizedText("exercise_initialization_error"),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = (initializationState as InitializationState.Error).message,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                            Button(onClick = { navController.navigateUp() }) {
                                Text(rememberLocalizedText("exercise_go_back"))
                            }
                        }
                    }
                    return@Box
                }

                is InitializationState.Ready -> {
                    // Devam et - kamerayı başlat
                    Log.d(TAG, "✅ Initialization complete, camera starting...")
                }
            }

            // Camera preview with pose detection
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                imageAnalyzer = { imageProxy ->
                    try {
                        // Blocking pose detection on executor thread
                        val pose = poseDetectorHelper.detectPoseBlocking(imageProxy)

                        // Update state on main thread
                        ContextCompat.getMainExecutor(context).execute {
                            currentPose = pose
                            imageWidth = imageProxy.width
                            imageHeight = imageProxy.height

                            // Process pose in ViewModel
                            viewModel.processPose(pose)
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

            // Pose overlay (iskelet çizimi)
            // NOT: ExerciseScreen'de henüz normalizasyon yok, fallback olarak emptyList() kullanılıyor
            PoseOverlay(
                pose = currentPose,
                normalizedLandmarks = emptyList(),  // Fallback: Normalize veri yok
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                formQuality = formQuality  // Form kalitesini geç
            )

            // === FORM DOĞRULUK BARI (Üst kısım) ===
            FormQualityBar(
                formScore = formScore,
                formQuality = formQuality,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            // === ŞİNAV SAYACI UI (Merkez) ===
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Başarı efekti (repSuccessTrigger aktifken göster)
                if (repSuccessTrigger) {
                    RepSuccessEffect()
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Büyük sayaç
                    Surface(
                        modifier = Modifier.size(160.dp),
                        shape = RoundedCornerShape(80.dp),
                        color = getCounterColor(formQuality),
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = exerciseCount.toString(),
                                fontSize = 72.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // "ŞINAV" etiketi
                    Text(
                        text = rememberLocalizedText("exercise_push_up_label"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // === GERİ BİLDİRİM (Alt kısım) ===
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = getFeedbackColor(formQuality),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = feedback,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Debug bilgisi (geliştirme için)
                    if (currentAngle > 0f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rememberLocalizedText("exercise_elbow_angle_info")
                                .replace("{angle}", currentAngle.toInt().toString())
                                .replace("{state}", exerciseState.name),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Bitir butonu (en az 3 tekrar yapıldıysa göster)
                    if (exerciseCount >= 3) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                Log.d(TAG, "🏁 Bitir butonu tıklandı!")
                                Log.d(TAG, "🏁 Egzersiz bitiriliyor...")
                                viewModel.finishExercise { reward ->
                                    // TODO-G122: Callback çağrıldı
                                    Log.d(TAG, "✅ finishExercise callback çalıştı! Reward: XP=${reward.experience}, Calories=${reward.calories}")
                                    // Sonuç ekranına git
                                    val statsString = reward.stats.entries.joinToString(",") { "${it.key.name}:${it.value}" }
                                    val route = "exercise_result?exerciseType=PUSH_UP&repCount=${reward.experience / 2}&totalXP=${reward.experience}&stats=$statsString&calories=${reward.calories}&gold=${reward.gold}"
                                    Log.d(TAG, "🧭 Navigating to: $route")
                                    navController.navigate(route)
                                    Log.d(TAG, "✅ Navigation komutu gönderildi")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🏁 " + rememberLocalizedText("exercise_finish"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Form kalitesine göre sayaç rengi
 */
@Composable
fun getCounterColor(formQuality: FormQuality): Color {
    return when (formQuality) {
        FormQuality.EXCELLENT -> Color(0xFF4CAF50)  // Yeşil
        FormQuality.GOOD -> Color(0xFF8BC34A)       // Açık yeşil
        FormQuality.NEUTRAL -> Color(0xFF2196F3)    // Mavi
        FormQuality.POOR -> Color(0xFFFFC107)       // Sarı
        FormQuality.INVALID -> Color(0xFFF44336)    // Kırmızı
    }
}

/**
 * Form kalitesine göre feedback rengi
 */
@Composable
fun getFeedbackColor(formQuality: FormQuality): Color {
    return when (formQuality) {
        FormQuality.EXCELLENT -> Color(0xFF4CAF50).copy(alpha = 0.9f)
        FormQuality.GOOD -> Color(0xFF8BC34A).copy(alpha = 0.9f)
        FormQuality.NEUTRAL -> Color.Black.copy(alpha = 0.7f)
        FormQuality.POOR -> Color(0xFFFFC107).copy(alpha = 0.9f)
        FormQuality.INVALID -> Color(0xFFF44336).copy(alpha = 0.9f)
    }
}

/**
 * Tekrar Başarı Efekti - Geçerli tekrar sayıldığında gösterilen animasyon
 *
 * Isekai Kuroshin temalı, enerji patlaması efekti
 */
@Composable
fun RepSuccessEffect() {
    // Genişleme animasyonu (0 -> 1 -> kaybolur)
    val infiniteTransition = rememberInfiniteTransition(label = "success_effect")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale_animation"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_animation"
    )

    // Dönen yıldız efekti
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dış halka - Genişleyen enerji halkası
        Canvas(modifier = Modifier.size(300.dp)) {
            // Halka 1
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00FF88).copy(alpha = alpha * 0.6f),
                        Color(0xFF00FF88).copy(alpha = alpha * 0.3f),
                        Color.Transparent
                    )
                ),
                radius = 80.dp.toPx() * scale,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
            )

            // Halka 2 (daha büyük)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = alpha * 0.4f),
                        Color(0xFFFFD700).copy(alpha = alpha * 0.2f),
                        Color.Transparent
                    )
                ),
                radius = 100.dp.toPx() * scale,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
            )
        }

        // İç ışık patlaması
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {
            // 8 ışın (yıldız şeklinde)
            for (i in 0..7) {
                val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                val length = 60.dp.toPx() * (1.5f - scale * 0.3f)

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF).copy(alpha = alpha),
                            Color(0xFF00FF88).copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    start = center,
                    end = Offset(
                        center.x + length * kotlin.math.cos(angle),
                        center.y + length * kotlin.math.sin(angle)
                    ),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Merkez parlak nokta
        Canvas(modifier = Modifier.size(80.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        Color(0xFF00FF88).copy(alpha = alpha * 0.7f),
                        Color.Transparent
                    )
                ),
                radius = 40.dp.toPx() * (1.2f - scale * 0.1f)
            )
        }
    }
}

/**
 * Form Kalitesi Barı - Anlık form kalitesini % olarak gösterir
 *
 * Isekai Kuroshin temalı, büyülü bir bar tasarımı
 */
@Composable
fun FormQualityBar(
    formScore: Float,
    formQuality: FormQuality,
    modifier: Modifier = Modifier
) {
    // Animasyonlu skor değişimi
    val animatedScore by animateFloatAsState(
        targetValue = formScore,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "form_score_animation"
    )

    // Renk teması (form kalitesine göre)
    val barColor = when (formQuality) {
        FormQuality.EXCELLENT, FormQuality.GOOD -> Color(0xFF00FF88)  // Neon Yeşil
        FormQuality.NEUTRAL -> Color(0xFFFFD700)  // Altın
        FormQuality.POOR -> Color(0xFFFF6600)  // Turuncu
        FormQuality.INVALID -> Color(0xFFFF0044)  // Kırmızı
    }

    Surface(
        modifier = modifier
            .width(280.dp)
            .height(56.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Arka plan (boş bar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(22.dp)
                    )
            )

            // Dolu bar (form skoruna göre)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (animatedScore / 100f).coerceIn(0f, 1f))
                    .padding(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.8f),
                                barColor.copy(alpha = 1.0f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            )

            // Metin (skor yüzdesi)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = rememberLocalizedText("exercise_form_quality"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${animatedScore.toInt()}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

private const val TAG = "ExerciseScreen"
