package com.example.isekaikuroshin.ui.healthhub

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.util.Log
import android.os.Build
import kotlinx.coroutines.launch
import com.example.isekaikuroshin.engine.HealthInsight
import com.example.isekaikuroshin.data.rememberLocalizedText
import java.text.SimpleDateFormat
import java.util.*
import me.bytebeats.views.charts.line.LineChart
import me.bytebeats.views.charts.line.LineChartData
import me.bytebeats.views.charts.line.render.line.SolidLineDrawer
import me.bytebeats.views.charts.line.render.point.FilledCircularPointDrawer
import me.bytebeats.views.charts.line.render.xaxis.SimpleXAxisDrawer
import me.bytebeats.views.charts.line.render.yaxis.SimpleYAxisDrawer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import kotlin.math.roundToInt

/**
 * TODO-HUB-02: Temel "Health & Mind Hub" Arayüzü
 *
 * Bu ekran, SYS-20 analiz raporunda belirtilen "Faz 1: Temel Altyapı ve Komut Sistemi"nin
 * arayüz ayağını oluşturur. Kullanıcının doğal dil komutlarını girebileceği bir metin giriş
 * alanı ve şimdilik boş olan temel sağlık kartlarını içerir.
 */

/**
 * Sağlık kartı veri modeli
 */
data class HealthCard(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val commandHint: String
)

/**
 * Ana Health Hub ekranı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthHubScreen(
    navController: NavController,
    viewModel: HealthHubViewModel = hiltViewModel()
) {
    Log.d("HealthHubScreen", "🎬 HealthHubScreen COMPOSABLE BAŞLATILDI!")

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Health Connect permissions launcher
    val healthConnectPermissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { grantedPermissions ->
            Log.d("HealthHubScreen", "TODO-HUB-03: Health Connect izinleri sonucu alındı")
            Log.d("HealthHubScreen", "Verilen izinler: $grantedPermissions")
            if (grantedPermissions.isNotEmpty()) {
                Log.d("HealthHubScreen", "✅ Health Connect izinleri verildi")
            } else {
                Log.w("HealthHubScreen", "⚠️ Health Connect izinleri reddedildi")
            }
        }
    )

    // HealthConnectManager'a launcher'ı ayarla
    LaunchedEffect(Unit) {
        viewModel.setHealthConnectPermissionsLauncher(healthConnectPermissionsLauncher)
        Log.d("HealthHubScreen", "TODO-HUB-03: Health Connect launcher ViewModel'e ayarlandı")
    }

    // Health Hub: Image picker launcher for camera/gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            Log.d("HealthHubScreen", "Health Hub: Görüntü seçildi, analiz başlatılıyor...")
            viewModel.processImageEvidence(it)
        }
    }

    // UI state
    var commandText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // TODO-HUB-04: Weight history state
    val weightHistory by viewModel.weightHistory.collectAsState()

    // TODO-HUB-07: Exercise history state
    val exerciseHistory by viewModel.exerciseHistory.collectAsState()
    val weeklyExerciseSummary by viewModel.weeklyExerciseSummary.collectAsState()

    // TODO-HUB-10: Nutrition history state
    val groupedNutritionHistory by viewModel.groupedNutritionHistory.collectAsState()

    // TODO-HUB-08: Health insight state
    val currentHealthInsight by viewModel.currentHealthInsight.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    var showInsightDialog by remember { mutableStateOf(false) }

    // TODO-HUB-12: Language learning state
    val languageSessionResponse by viewModel.languageSessionResponse.collectAsState()
    val isLanguageLearning by viewModel.isLanguageLearning.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    // TODO-HUB-13: Chat interface state
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isSendingMessage by viewModel.isSendingMessage.collectAsState()

    // TODO-HUB-15: Language progress state
    val languageProgress by viewModel.languageProgress.collectAsState()

    // Health Hub: Image analysis state
    val pendingImageAnalysis by viewModel.pendingImageAnalysis.collectAsState()
    val showConfirmationDialog by viewModel.showConfirmationDialog.collectAsState()
    val isProcessingImage by viewModel.isProcessingImage.collectAsState()

    // FIX-TASK-3.1: Health Hub lokalizasyon - Metinleri önce al
    val weightTrackingTitle = rememberLocalizedText("weight_tracking")
    val nutritionLogTitle = rememberLocalizedText("nutrition_log")
    val exerciseLogTitle = rememberLocalizedText("exercise_log")
    val languagePracticeTitle = rememberLocalizedText("language_practice")
    val waterIntakeTitle = rememberLocalizedText("water_intake")
    val sleepTrackingTitle = rememberLocalizedText("sleep_tracking")
    val medicationTitle = rememberLocalizedText("medication")
    val moodTrackingTitle = rememberLocalizedText("mood_tracking")
    val noDataYet = rememberLocalizedText("no_data_yet")

    Log.d("HealthHubScreen", "📝 Lokalize metinler yüklendi: exercise='$exerciseLogTitle'")

    // Kart listesini oluştur
    val healthCards = remember(
        weightTrackingTitle, nutritionLogTitle, exerciseLogTitle, languagePracticeTitle,
        waterIntakeTitle, sleepTrackingTitle, medicationTitle, moodTrackingTitle, noDataYet
    ) {
        listOf(
            HealthCard(
                id = "weight_tracking",
                title = weightTrackingTitle,
                icon = "🏋️",
                description = noDataYet,
                commandHint = "kilo: "
            ),
            HealthCard(
                id = "nutrition_log",
                title = nutritionLogTitle,
                icon = "🥗",
                description = noDataYet,
                commandHint = "yedim: "
            ),
            HealthCard(
                id = "exercise_log",
                title = exerciseLogTitle,
                icon = "🏃‍♂️",
                description = noDataYet,
                commandHint = "egzersiz: "
            ),
            HealthCard(
                id = "language_practice",
                title = languagePracticeTitle,
                icon = "🧠",
                description = noDataYet,
                commandHint = "dil: "
            ),
            HealthCard(
                id = "water_intake",
                title = waterIntakeTitle,
                icon = "💧",
                description = noDataYet,
                commandHint = "su: "
            ),
            HealthCard(
                id = "sleep_tracking",
                title = sleepTrackingTitle,
                icon = "😴",
                description = noDataYet,
                commandHint = "uyudum: "
            ),
            HealthCard(
                id = "medication",
                title = medicationTitle,
                icon = "💊",
                description = noDataYet,
                commandHint = "ilaç: "
            ),
            HealthCard(
                id = "mood_tracking",
                title = moodTrackingTitle,
                icon = "😊",
                description = noDataYet,
                commandHint = "bugün: "
            )
        ).also {
            Log.d("HealthHubScreen", "✅ Kart listesi oluşturuldu: ${it.size} kart")
        }
    }

    Log.d("HealthHubScreen", "📊 healthCards.size = ${healthCards.size}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = rememberLocalizedText("health_mind_hub"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Sabit komut giriş alanı
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                rememberLocalizedText("speak_to_interface"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        enabled = !isProcessing,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Health Hub: Camera button
                    IconButton(
                        onClick = {
                            Log.d("HealthHubScreen", "Health Hub: Kamera butonu tıklandı")
                            imagePickerLauncher.launch("image/*")
                        },
                        enabled = !isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = rememberLocalizedText("upload_photo"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            if (commandText.isNotBlank() && !isProcessing) {
                                coroutineScope.launch {
                                    isProcessing = true
                                    try {
                                        Log.d("HealthHubScreen", "=== TODO-HUB-02: KOMUT İŞLEME ===")
                                        Log.d("HealthHubScreen", "Kullanıcı komutu: '$commandText'")

                                        // HybridNLUEngine ile komutu işle
                                        val result = viewModel.processCommand(commandText)

                                        Log.d("HealthHubScreen", "✅ Komut başarıyla işlendi: ${result.commandType}")
                                        Log.d("HealthHubScreen", "Parametreler: ${result.parameters}")
                                        Log.d("HealthHubScreen", "Güven skoru: ${result.confidence}")

                                        // Komutu temizle
                                        commandText = ""

                                    } catch (e: Exception) {
                                        Log.e("HealthHubScreen", "❌ Komut işleme hatası: ${e.message}", e)
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        enabled = commandText.isNotBlank() && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = rememberLocalizedText("send"),
                                tint = if (commandText.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // FAZ 5: Subtle background animation
        Box(modifier = Modifier.fillMaxSize()) {
            SubtleFloatingParticles(
                particleCount = 12,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
            )

            // Ana içerik alanı
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // === Health Hub Cards Grid ===
                item {
                    Text(
                        text = rememberLocalizedText("health_mind_hub"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(600.dp), // Fixed height for nested grid
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false // Disable nested scrolling
                    ) {
            items(healthCards) { card ->
                Log.d("HealthHubScreen", "🔍 Rendering card: id='${card.id}', title='${card.title}'")
                when (card.id) {
                    "exercise_log" -> {
                        Log.d("HealthHubScreen", "✅ EXERCISE_LOG case'ine girildi!")
                        // Fiziksel Antrenman - Gelişmiş egzersiz sistemi
                        // NOT: Kalman filtresi + Kalibrasyon + Akıllı tekrar sayma
                        ExerciseTrackingCard(
                            card = card,
                            exerciseHistory = exerciseHistory,
                            weeklyExerciseSummary = weeklyExerciseSummary,
                            viewModel = viewModel,
                            onClick = {
                                try {
                                    Log.d("HealthHubScreen", "💪 Fiziksel Antrenman kartına tıklandı")
                                    // YENİ: Gelişmiş Fiziksel Antrenman ekranına git
                                    navController.navigate("physical_training")
                                    Log.d("HealthHubScreen", "✅ Navigation başarılı")
                                } catch (e: Exception) {
                                    Log.e("HealthHubScreen", "❌ Navigation hatası: ${e.message}", e)
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                    "weight_tracking" -> {
                        // TODO-HUB-04: Özel Kilo Takibi kartı
                        WeightTrackingCard(
                            card = card,
                            weightHistory = weightHistory,
                            onClick = {
                                commandText = card.commandHint
                                Log.d("HealthHubScreen", "📋 İnteraktif ipucu: '${card.title}' kartına tıklandı")
                                Log.d("HealthHubScreen", "💡 Komut ipucu yüklendi: '${card.commandHint}'")
                            }
                        )
                    }
                    "nutrition_log" -> {
                        // TODO-HUB-10: Özel Beslenme Günlüğü kartı
                        NutritionTrackingCard(
                            card = card,
                            groupedNutritionHistory = groupedNutritionHistory,
                            healthConnectManager = viewModel.healthConnectManager,
                            onClick = {
                                commandText = card.commandHint
                                Log.d("HealthHubScreen", "📋 İnteraktif ipucu: '${card.title}' kartına tıklandı")
                                Log.d("HealthHubScreen", "💡 Komut ipucu yüklendi: '${card.commandHint}'")
                            }
                        )
                    }
                    "language_practice" -> {
                        // TODO-HUB-12: Özel Dil Pratiği kartı
                        // TODO-HUB-15: İlerleme verisi eklendi
                        // G120: Dil seçimine göre öğretme dili (EN seçiliyse İngilizce öğret, TR seçiliyse Türkçe öğret)
                        val targetLanguage = if (com.example.isekaikuroshin.data.LanguageManager.currentLanguage.value == "EN") "English" else "Turkish"
                        LanguagePracticeCard(
                            card = card,
                            isLoading = isLanguageLearning,
                            languageProgress = languageProgress,
                            onStartPractice = {
                                viewModel.startLanguagePractice(targetLanguage, "A2")
                            }
                        )
                    }
                    else -> {
                        Log.e("HealthHubScreen", "❌ ELSE bloğuna düşüldü! card.id='${card.id}'")
                        HealthCardItem(
                            card = card,
                            onClick = {
                                // İnteraktif komut ipucu: Karta tıklayınca metin alanını doldur
                                commandText = card.commandHint
                                Log.d("HealthHubScreen", "📋 İnteraktif ipucu: '${card.title}' kartına tıklandı")
                                Log.d("HealthHubScreen", "💡 Komut ipucu yüklendi: '${card.commandHint}'")
                            }
                        )
                    }
                }
            } // End of items block
                    } // End of nested LazyVerticalGrid
                } // End of outer item (grid container)

                // TODO-HUB-08: Sağlık Analizi Butonu
                item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable {
                            if (!isAnalyzing) {
                                viewModel.runHealthAnalysis()
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = rememberLocalizedText("analyzing"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = rememberLocalizedText("weekly_analysis"),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = rememberLocalizedText("weekly_analysis_button"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = rememberLocalizedText("analyze_activity_level"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } // End of Card
            } // End of item (Sağlık Analizi)

                // === G129: Daily Alarms Section (Yan yana 3 sütun) ===
                item {
                    Text(
                        text = rememberLocalizedText("daily_alarms_title"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Language Learning Alarm (Compact)
                        CompactAlarmCard(
                            title = "📚",
                            alarmId = com.example.isekaikuroshin.utils.AlarmScheduler.AlarmIds.LANGUAGE_LEARNING_DAILY,
                            defaultHour = 9,
                            defaultMinute = 0,
                            modifier = Modifier.weight(1f)
                        )

                        // Exercise Alarm (Compact)
                        CompactAlarmCard(
                            title = "💪",
                            alarmId = com.example.isekaikuroshin.utils.AlarmScheduler.AlarmIds.EXERCISE_DAILY,
                            defaultHour = 18,
                            defaultMinute = 0,
                            modifier = Modifier.weight(1f)
                        )

                        // Meditation Alarm (Compact)
                        CompactAlarmCard(
                            title = "🧘",
                            alarmId = com.example.isekaikuroshin.utils.AlarmScheduler.AlarmIds.MEDITATION_DAILY,
                            defaultHour = 21,
                            defaultMinute = 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } // End of LazyColumn
        } // End of Box

        // TODO-HUB-08: Health Insight Dialog
        LaunchedEffect(currentHealthInsight) {
            currentHealthInsight?.let {
                showInsightDialog = true
            }
        }

        if (showInsightDialog && currentHealthInsight != null) {
            HealthInsightDialog(
                insight = currentHealthInsight!!,
                onDismiss = {
                    showInsightDialog = false
                    viewModel.clearHealthInsight()
                }
            )
        }

        // TODO-HUB-13: Chat History Handler
        LaunchedEffect(chatHistory.size) {
            if (chatHistory.isNotEmpty()) {
                showLanguageDialog = true
                Log.d("HealthHubScreen", "TODO-HUB-13: Chat history güncellendi, dialog gösteriliyor")
            }
        }

        if (showLanguageDialog && chatHistory.isNotEmpty()) {
            LanguageChatDialog(
                chatHistory = chatHistory,
                isSendingMessage = isSendingMessage,
                onSendMessage = { message ->
                    viewModel.sendChatMessage(message)
                },
                onDismiss = {
                    showLanguageDialog = false
                    viewModel.clearLanguageSession()
                }
            )
        }

        // Health Hub: Image Confirmation Dialog
        if (showConfirmationDialog && pendingImageAnalysis != null) {
            ImageConfirmationDialog(
                analysis = pendingImageAnalysis!!,
                isProcessing = isProcessingImage,
                onConfirm = { viewModel.confirmImageData() },
                onCorrect = { correction -> viewModel.applyUserCorrection(correction) },
                onDismiss = { viewModel.dismissImageDialog() }
            )
        }
    } // End of Scaffold content lambda (paddingValues)
} // End of HealthHubScreen

/**
 * FAZ 5: Sağlık kartı bileşeni - Haptic feedback + Press animation
 */
@Composable
fun HealthCardItem(
    card: HealthCard,
    onClick: () -> Unit
) {
    // FAZ 5: Haptic feedback + Press animation
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_press_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material.ripple.rememberRipple()
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // İkon
            Text(
                text = card.icon,
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Başlık
            Text(
                text = card.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Açıklama
            Text(
                text = card.description,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * TODO-HUB-05: Kilo Takibi kartı - Animasyonlu çizgi grafiği ile
 */
@Composable
fun WeightTrackingCard(
    card: HealthCard,
    weightHistory: List<WeightRecord>,
    onClick: () -> Unit
) {
    // FAZ 2.3: Fade-in animasyonu için veri değişimini izle
    var isDataLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(weightHistory.size) {
        if (weightHistory.isNotEmpty()) {
            isDataLoaded = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isDataLoaded) 1f else 0.7f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "weight_card_fade"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() }
            .graphicsLayer(alpha = alpha),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Başlık ve ikon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = card.icon,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = rememberLocalizedText("weight_change_graph"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // FAZ 3.2: Güncel kilo animasyonlu sayaç
            if (weightHistory.isNotEmpty()) {
                val currentWeight = weightHistory.lastOrNull()?.weight?.inKilograms?.toFloat() ?: 0f
                AnimatedCounter(
                    targetValue = currentWeight,
                    suffix = "kg",
                    textColor = MaterialTheme.colorScheme.primary,
                    fontSize = 18,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kilo grafiği veya boş mesaj
            if (weightHistory.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📊",
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = rememberLocalizedText("no_weight_history"),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Grafik için veri hazırlama
                val chartData = remember(weightHistory) {
                    prepareChartData(weightHistory)
                }

                // Animasyonlu çizgi grafiği
                LineChart(
                    lineChartData = chartData,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    animation = androidx.compose.animation.core.tween(
                        durationMillis = 1500,
                        easing = androidx.compose.animation.core.EaseInOutCubic
                    ),
                    pointDrawer = FilledCircularPointDrawer(
                        color = MaterialTheme.colorScheme.primary,
                        diameter = 4.dp // GÖREV #15: 6dp → 4dp (daha temiz görünüm)
                    ),
                    lineDrawer = SolidLineDrawer(
                        color = MaterialTheme.colorScheme.primary,
                        thickness = 2.dp // GÖREV #15: 3dp → 2dp (daha okunaklı)
                    ),
                    xAxisDrawer = SimpleXAxisDrawer(
                        labelTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    yAxisDrawer = SimpleYAxisDrawer(
                        labelTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    horizontalOffset = 5f
                )
            }
        }
    }
}

/**
 * TODO-HUB-05: WeightRecord listesini çizgi grafiği verisine dönüştürme
 */
fun prepareChartData(weightHistory: List<WeightRecord>): LineChartData {
    if (weightHistory.isEmpty()) {
        return LineChartData(
            points = emptyList()
        )
    }

    // Son 30 günlük veriyi al ve tarihe göre sırala
    val sortedRecords = weightHistory
        .sortedBy { it.time }
        .takeLast(30)

    // En eski tarihi bul (X ekseni için offset)
    val firstDate = sortedRecords.firstOrNull()?.time?.toEpochMilli() ?: 0L

    // Veri noktalarını oluştur
    val points = sortedRecords.mapIndexed { index, record ->
        me.bytebeats.views.charts.line.LineChartData.Point(
            value = record.weight.inKilograms.toFloat(),
            label = SimpleDateFormat("dd/MM", Locale.forLanguageTag("tr-TR"))
                .format(Date.from(record.time))
        )
    }

    return LineChartData(
        points = points
    )
}

/**
 * TODO-HUB-04: Tek bir kilo kaydı gösterimi
 */
@Composable
fun WeightHistoryItem(record: WeightRecord) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.forLanguageTag("tr-TR"))
    val formattedDate = dateFormat.format(Date.from(record.time))
    val weightKg = String.format("%.1f", record.weight.inKilograms)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formattedDate,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = "${weightKg} kg",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * TODO-HUB-07: Egzersiz Takibi kartı - Özet ve liste görünümü ile
 */
@Composable
fun ExerciseTrackingCard(
    card: HealthCard,
    exerciseHistory: List<ExerciseSessionRecord>,
    weeklyExerciseSummary: String,
    viewModel: HealthHubViewModel,
    onClick: () -> Unit
) {
    Log.d("ExerciseTrackingCard", "🎨 ExerciseTrackingCard composing...")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = {
            Log.d("ExerciseTrackingCard", "🖱️ Card tıklandı!")
            onClick()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Başlık ve ikon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = card.icon,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = rememberLocalizedText("exercise_log_7days"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Özet alanı
            Text(
                text = weeklyExerciseSummary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Liste alanı
            if (exerciseHistory.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(exerciseHistory.take(3)) { exercise ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.getExerciseEmoji(exercise.exerciseType),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = viewModel.getExerciseTypeName(exercise.exerciseType),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "${viewModel.getExerciseDurationInMinutes(exercise)} Dk",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = viewModel.formatExerciseDate(exercise),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (exerciseHistory.size > 3) {
                        item {
                            val moreCount = exerciseHistory.size - 3
                            val andMoreText = rememberLocalizedText("and_x_more")
                            Text(
                                text = andMoreText.replace("%d", moreCount.toString()),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rememberLocalizedText("no_exercise_this_week"),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * TODO-HUB-10: Beslenme Takibi kartı - Öğünlere göre gruplandırılmış liste görünümü ile
 */
@Composable
fun NutritionTrackingCard(
    card: HealthCard,
    groupedNutritionHistory: Map<Int, List<NutritionRecord>>,
    healthConnectManager: HealthConnectManager,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Başlık ve ikon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = card.icon,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = rememberLocalizedText("nutrition_log_today"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Beslenme verilerini göster
            if (groupedNutritionHistory.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedNutritionHistory.forEach { (mealType, records) ->
                        if (records.isNotEmpty()) {
                            item {
                                // Öğün başlığı
                                Text(
                                    text = healthConnectManager.getMealTypeName(mealType),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            items(records.take(2)) { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = record.name ?: rememberLocalizedText("unknown_food"),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            if (records.size > 2) {
                                item {
                                    val moreCount = records.size - 2
                                    val andMoreText = rememberLocalizedText("and_x_more")
                                    Text(
                                        text = "   ${andMoreText.replace("%d", moreCount.toString())}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rememberLocalizedText("no_nutrition_today"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * TODO-HUB-12: Dil Pratiği kartı
 * TODO-HUB-15: İlerleme görselleştirmesi eklendi
 */
@Composable
fun LanguagePracticeCard(
    card: HealthCard,
    isLoading: Boolean,
    languageProgress: com.example.isekaikuroshin.engine.mind.LanguageProgressTracker.LanguageProgress?,
    onStartPractice: () -> Unit
) {
    // TODO-HUB-15: İlerleme animasyonu için
    val animatedProgress by animateFloatAsState(
        targetValue = (languageProgress?.progressPercent ?: 0f) / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.icon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = card.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TODO-HUB-15: Seviye Göstergesi
            if (languageProgress != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = rememberLocalizedText("your_english_level"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = languageProgress.cefrLevel,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = rememberLocalizedText("progress"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${languageProgress.progressPercent.toInt()}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TODO-HUB-15: İlerleme Çubuğu
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val remainingPercent = (100 - languageProgress.progressPercent).toInt()
                    val toNextLevelText = rememberLocalizedText("to_next_level")
                    Text(
                        text = toNextLevelText.replace("%d", remainingPercent.toString()),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text(
                    text = rememberLocalizedText("start_language_session"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Başlat Butonu
            Button(
                onClick = {
                    if (!isLoading) {
                        onStartPractice()
                        Log.d("HealthHubScreen", "TODO-HUB-12: Start English Practice button clicked")
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(rememberLocalizedText("preparing"))
                } else {
                    Text(
                        text = rememberLocalizedText("start_english_practice"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Seviye bilgisi
            Text(
                text = rememberLocalizedText("level_a2_basic"),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * TODO-HUB-13: İnteraktif Dil Sohbet Diyalogu
 */
@Composable
fun LanguageChatDialog(
    chatHistory: List<com.example.isekaikuroshin.engine.mind.ChatMessage>,
    isSendingMessage: Boolean,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var userInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new message arrives
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🧠 ", fontSize = 20.sp)
                        Text(
                            text = rememberLocalizedText("language_tutor"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = rememberLocalizedText("close")
                        )
                    }
                }

                HorizontalDivider()

                // Chat messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatHistory.size) { index ->
                        val message = chatHistory[index]
                        ChatBubble(message = message)
                    }

                    // Loading indicator
                    if (isSendingMessage) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(rememberLocalizedText("ai_thinking"), fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Input area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(rememberLocalizedText("write_your_response")) },
                        enabled = !isSendingMessage,
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (userInput.isNotBlank()) {
                                onSendMessage(userInput)
                                userInput = ""
                            }
                        },
                        enabled = !isSendingMessage && userInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = rememberLocalizedText("send_message"),
                            tint = if (userInput.isNotBlank() && !isSendingMessage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * TODO-HUB-13: Sohbet balonu bileşeni
 */
@Composable
fun ChatBubble(message: com.example.isekaikuroshin.engine.mind.ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (message.isFromUser) 12.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 12.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (message.isFromUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * TODO-HUB-08: Sağlık İçgörüsü Diyalogu
 */
@Composable
fun HealthInsightDialog(
    insight: HealthInsight,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(rememberLocalizedText("ok"))
            }
        },
        title = {
            Text(
                text = insight.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = insight.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                insight.recommendedAction?.let { action ->
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "💡 ${rememberLocalizedText("recommended_action")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = action,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

/**
 * G129: Daily Alarm Card (Language Learning / Exercise Reminder)
 * Simple alarm setup UI with time picker and enable/disable switch
 *
 * TODO-G129-PHASE2: Add custom sound picker (SAF integration)
 * TODO-G129-PHASE2: Add SharedPreferences to persist alarm settings
 * TODO-G129-PHASE2: Reschedule alarm on boot (BOOT_COMPLETED receiver)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyAlarmCard(
    title: String,
    alarmId: Int,
    defaultHour: Int = 9,
    defaultMinute: Int = 0
) {
    val context = LocalContext.current
    var isAlarmEnabled by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(defaultHour) }
    var selectedMinute by remember { mutableIntStateOf(defaultMinute) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Localized strings (remember outside of callbacks)
    val timeToPractice = rememberLocalizedText("time_to_practice")
    val alarmEnabledText = rememberLocalizedText("alarm_enabled")
    val alarmDisabledText = rememberLocalizedText("alarm_disabled")
    val alarmTimeText = rememberLocalizedText("alarm_time")
    val okText = rememberLocalizedText("ok")
    val cancelText = rememberLocalizedText("cancel")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = isAlarmEnabled,
                    onCheckedChange = { enabled ->
                        isAlarmEnabled = enabled

                        if (enabled) {
                            // Check permissions (Android 12+ & Android 13+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (!com.example.isekaikuroshin.utils.AlarmScheduler.canScheduleExactAlarms(context)) {
                                    // Open permission settings
                                    com.example.isekaikuroshin.utils.AlarmScheduler.openAlarmPermissionSettings(context)
                                    return@Switch
                                }
                            }

                            if (!com.example.isekaikuroshin.utils.SystemNotificationHelper.hasNotificationPermission(context)) {
                                // Request notification permission (handled by system)
                                return@Switch
                            }

                            // Schedule alarm
                            com.example.isekaikuroshin.utils.AlarmScheduler.scheduleDailyAlarm(
                                context = context,
                                alarmId = alarmId,
                                hourOfDay = selectedHour,
                                minute = selectedMinute,
                                title = title,
                                message = timeToPractice,
                                soundUri = null, // TODO-G129-PHASE2: Custom sound
                                targetRoute = "health_hub"
                            )

                            com.example.isekaikuroshin.utils.GameLogger.logSystem("✅ G129: Alarm enabled: $title at $selectedHour:$selectedMinute")
                        } else {
                            // Cancel alarm
                            com.example.isekaikuroshin.utils.AlarmScheduler.cancelAlarm(context, alarmId)
                            com.example.isekaikuroshin.utils.GameLogger.logSystem("🔕 G129: Alarm disabled: $title")
                        }
                    }
                )
            }

            // Status text
            Text(
                text = if (isAlarmEnabled)
                    alarmEnabledText
                else
                    alarmDisabledText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Time selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alarmTimeText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { showTimePicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = String.format("%02d:%02d", selectedHour, selectedMinute),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } // End of Column
    } // End of Card

    // Time Picker Dialog
    if (showTimePicker) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ Material TimePicker
            val timePickerState = rememberTimePickerState(
                initialHour = selectedHour,
                initialMinute = selectedMinute,
                is24Hour = true
            )

            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false

                        // Reschedule if alarm is enabled
                        if (isAlarmEnabled) {
                            com.example.isekaikuroshin.utils.AlarmScheduler.scheduleDailyAlarm(
                                context = context,
                                alarmId = alarmId,
                                hourOfDay = selectedHour,
                                minute = selectedMinute,
                                title = title,
                                message = timeToPractice,
                                soundUri = null,
                                targetRoute = "health_hub"
                            )
                        }
                    }) {
                        Text(okText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(cancelText)
                    }
                },
                text = {
                    TimePicker(state = timePickerState)
                }
            )
        }
    }
} // End of DailyAlarmCard

/**
 * G129: Compact Alarm Card (Yan yana 3 sütun için küçük versiyon)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactAlarmCard(
    title: String,
    alarmId: Int,
    defaultHour: Int,
    defaultMinute: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAlarmEnabled by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(defaultHour) }
    var selectedMinute by remember { mutableIntStateOf(defaultMinute) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timeToPractice = rememberLocalizedText("time_to_practice")
    val okText = rememberLocalizedText("ok")
    val cancelText = rememberLocalizedText("cancel")

    Card(
        modifier = modifier
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon
            Text(
                text = title,
                fontSize = 24.sp
            )

            // Time Button
            Button(
                onClick = { showTimePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentPadding = PaddingValues(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Switch
            Switch(
                checked = isAlarmEnabled,
                onCheckedChange = { enabled ->
                    isAlarmEnabled = enabled

                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (!com.example.isekaikuroshin.utils.AlarmScheduler.canScheduleExactAlarms(context)) {
                                com.example.isekaikuroshin.utils.AlarmScheduler.openAlarmPermissionSettings(context)
                                return@Switch
                            }
                        }

                        com.example.isekaikuroshin.utils.AlarmScheduler.scheduleDailyAlarm(
                            context = context,
                            alarmId = alarmId,
                            hourOfDay = selectedHour,
                            minute = selectedMinute,
                            title = title,
                            message = timeToPractice,
                            soundUri = null,
                            targetRoute = "health_hub"
                        )
                    } else {
                        com.example.isekaikuroshin.utils.AlarmScheduler.cancelAlarm(context, alarmId)
                    }
                },
                modifier = Modifier.height(24.dp)
            )
        }
    }

    // Time Picker Dialog (same as DailyAlarmCard)
    if (showTimePicker) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val timePickerState = rememberTimePickerState(
                initialHour = selectedHour,
                initialMinute = selectedMinute,
                is24Hour = true
            )

            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false

                        if (isAlarmEnabled) {
                            com.example.isekaikuroshin.utils.AlarmScheduler.scheduleDailyAlarm(
                                context = context,
                                alarmId = alarmId,
                                hourOfDay = selectedHour,
                                minute = selectedMinute,
                                title = title,
                                message = timeToPractice,
                                soundUri = null,
                                targetRoute = "health_hub"
                            )
                        }
                    }) {
                        Text(okText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(cancelText)
                    }
                },
                text = {
                    TimePicker(state = timePickerState)
                }
            )
        }
    }
} // End of CompactAlarmCard