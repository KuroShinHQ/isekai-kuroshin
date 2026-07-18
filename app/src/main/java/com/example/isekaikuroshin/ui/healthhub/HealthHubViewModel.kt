package com.example.isekaikuroshin.ui.healthhub

import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Mass
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.engine.CommandProcessingEngine
import com.example.isekaikuroshin.engine.HealthInsight
import com.example.isekaikuroshin.engine.HybridNLUEngine
import com.example.isekaikuroshin.engine.CommandResult
import com.example.isekaikuroshin.engine.ProactiveHealthCoach
import com.example.isekaikuroshin.engine.mind.LanguageLearningEngine
import com.example.isekaikuroshin.engine.mind.LanguageSessionResponse
import com.example.isekaikuroshin.engine.mind.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class HealthHubViewModel @Inject constructor(
    private val hybridNLUEngine: HybridNLUEngine,
    private val commandProcessingEngine: CommandProcessingEngine,
    val healthConnectManager: HealthConnectManager,
    private val proactiveHealthCoach: ProactiveHealthCoach,
    private val languageLearningEngine: LanguageLearningEngine,
    private val languageProgressTracker: com.example.isekaikuroshin.engine.mind.LanguageProgressTracker,
    private val gameStateManager: com.example.isekaikuroshin.game.GameStateManager,
    // HATA #3 FIX: PersistentDataManager ekle
    private val persistentDataManager: com.example.isekaikuroshin.data.PersistentDataManager,
    // Health Hub Vision Analysis Engine
    private val cameraVisionEngine: com.example.isekaikuroshin.engine.CameraVisionEngine
) : ViewModel() {

    companion object {
        private const val TAG = "HealthHubViewModel"
    }

    // UI State
    private val _uiState = MutableStateFlow<HealthHubUiState>(HealthHubUiState.Loading)
    val uiState: StateFlow<HealthHubUiState> = _uiState.asStateFlow()

    private val _commandResult = mutableStateOf<CommandResult?>(null)
    val commandResult: State<CommandResult?> = _commandResult

    // TODO-HUB-04: Weight History StateFlow
    val weightHistory: StateFlow<List<WeightRecord>> = uiState.map { state ->
        when (state) {
            is HealthHubUiState.Success -> state.weightHistory
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // TODO-HUB-07: Exercise History StateFlows
    val exerciseHistory: StateFlow<List<ExerciseSessionRecord>> = uiState.map { state ->
        when (state) {
            is HealthHubUiState.Success -> state.exerciseHistory
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _weeklyExerciseSummary = MutableStateFlow("")
    val weeklyExerciseSummary: StateFlow<String> = _weeklyExerciseSummary.asStateFlow()

    // TODO-HUB-08: Health Insight StateFlow
    private val _currentHealthInsight = MutableStateFlow<HealthInsight?>(null)
    val currentHealthInsight: StateFlow<HealthInsight?> = _currentHealthInsight.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // TODO-HUB-10: Nutrition History StateFlow
    val nutritionHistory: StateFlow<List<NutritionRecord>> = uiState.map { state ->
        when (state) {
            is HealthHubUiState.Success -> state.nutritionHistory
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // TODO-HUB-10: Öğün türüne göre gruplanmış beslenme verisi
    val groupedNutritionHistory: StateFlow<Map<Int, List<NutritionRecord>>> = nutritionHistory.map { records ->
        records.groupBy { it.mealType }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    // TODO-HUB-12: Language Learning StateFlows
    private val _languageSessionResponse = MutableStateFlow<LanguageSessionResponse?>(null)
    val languageSessionResponse: StateFlow<LanguageSessionResponse?> = _languageSessionResponse.asStateFlow()

    private val _isLanguageLearning = MutableStateFlow(false)
    val isLanguageLearning: StateFlow<Boolean> = _isLanguageLearning.asStateFlow()

    // TODO-HUB-13: Chat History StateFlow
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    // TODO-HUB-15: Language Progress StateFlow
    private val _languageProgress = MutableStateFlow<com.example.isekaikuroshin.engine.mind.LanguageProgressTracker.LanguageProgress?>(null)
    val languageProgress: StateFlow<com.example.isekaikuroshin.engine.mind.LanguageProgressTracker.LanguageProgress?> = _languageProgress.asStateFlow()

    // Health Hub Vision Analysis StateFlows
    private val _isProcessingImage = MutableStateFlow(false)
    val isProcessingImage: StateFlow<Boolean> = _isProcessingImage.asStateFlow()

    private val _pendingImageAnalysis = MutableStateFlow<com.example.isekaikuroshin.data.VisionAnalysisResult?>(null)
    val pendingImageAnalysis: StateFlow<com.example.isekaikuroshin.data.VisionAnalysisResult?> = _pendingImageAnalysis.asStateFlow()

    private val _showConfirmationDialog = MutableStateFlow(false)
    val showConfirmationDialog: StateFlow<Boolean> = _showConfirmationDialog.asStateFlow()

    init {
        Log.d(TAG, "ViewModel başlatılıyor...")
        healthConnectManager.initialize()
        checkPermissionsAndLoadData()
        loadTodaysNutrition()

        // TODO-HUB-15: İlk açılışta dil ilerlemesini yükle
        loadLanguageProgress()
    }

    /**
     * İzinleri kontrol et ve verileri yükle
     */
    fun checkPermissionsAndLoadData() {
        viewModelScope.launch {
            _uiState.value = HealthHubUiState.Loading
            try {
                val hasPermissions = healthConnectManager.checkPermissions()
                if (hasPermissions) {
                    Log.d(TAG, "İzinler zaten verilmiş, veriler yükleniyor")
                    loadAllHealthData()
                } else {
                    Log.w(TAG, "Health Connect izinleri verilmemiş")
                    _uiState.value = HealthHubUiState.PermissionsRequired
                }
            } catch (e: Exception) {
                Log.e(TAG, "İzin kontrolü ve veri yükleme başarısız", e)
                _uiState.value = HealthHubUiState.Error("İzinler kontrol edilirken bir hata oluştu")
            }
        }
    }

    /**
     * Health Connect izinleri için launcher ayarla
     */
    fun setHealthConnectPermissionsLauncher(launcher: ManagedActivityResultLauncher<Set<String>, Set<String>>) {
        healthConnectManager.setPermissionsLauncher(launcher)
    }

    /**
     * İzinleri talep et
     */
    fun requestPermissions() {
        healthConnectManager.requestPermissions()
    }

    /**
     * Tüm sağlık verilerini yükle (kilo ve egzersiz)
     */
    private fun loadAllHealthData() {
        viewModelScope.launch {
            _uiState.value = HealthHubUiState.Loading
            try {
                // Örnek veriler (geçici)
                val weightData = generateDummyWeightData(30)
                val exerciseData = generateDummyExerciseData(7)

                // Bugünün beslenme verilerini oku
                val nutritionData = loadTodaysNutritionData()

                // Haftalık özet hesapla
                calculateWeeklyExerciseSummary(exerciseData)

                _uiState.value = HealthHubUiState.Success(
                    weightHistory = weightData,
                    exerciseHistory = exerciseData,
                    nutritionHistory = nutritionData
                )
                Log.d(TAG, "TODO-HUB-04: Başarılı: Tüm sağlık verileri yüklendi")
                Log.d(TAG, "- ${weightData.size} adet kilo kaydı")
                Log.d(TAG, "- ${exerciseData.size} adet egzersiz kaydı")

            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-04: Sağlık verileri yüklenirken hata", e)
                _uiState.value = HealthHubUiState.Error("Sağlık verileri yüklenemedi")
            }
        }
    }

    /**
     * Gelen komutu NLU ile işle ve ilgili aksiyonu al
     */
    suspend fun processCommand(command: String): CommandResult {
        val commandResult = hybridNLUEngine.parseCommand(command)
        _commandResult.value = commandResult
        commandProcessingEngine.processCommand(commandResult)
        return commandResult
    }

    // ========= DUMMY DATA GENERATION (GEÇİCİ) =========
    // Gerçek Health Connect entegrasyonu tamamlanana kadar kullanılır

    /**
     * Son 30 gün için sahte kilo verisi oluşturur
     */
    private fun generateDummyWeightData(days: Int): List<WeightRecord> {
        val records = mutableListOf<WeightRecord>()
        var currentWeight = 75.0
        val today = LocalDate.now()

        for (i in 0 until days) {
            val date = today.minusDays(i.toLong())
            val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()

            // Her gün için rastgele bir ağırlık değişimi
            currentWeight += Random.nextDouble(-0.3, 0.3)

            records.add(
                WeightRecord(
                    weight = Mass.kilograms(currentWeight),
                    time = instant,
                    zoneOffset = ZoneId.systemDefault().rules.getOffset(instant)
                )
            )
        }
        Log.d(TAG, "TODO-HUB-04: ${records.size} adet sahte kilo verisi oluşturuldu")
        return records.sortedBy { it.time }
    }

    /**
     * Son 7 gün için sahte egzersiz verisi oluşturur
     */
    private fun generateDummyExerciseData(days: Int): List<ExerciseSessionRecord> {
        val records = mutableListOf<ExerciseSessionRecord>()
        val exerciseTypes = listOf(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING
        )
        val today = LocalDate.now()

        for (i in 0 until days) {
            // Bazen egzersiz yapılmamış günleri simüle et
            if (Random.nextBoolean()) continue

            val date = today.minusDays(i.toLong())
            val startTime = date.atTime(Random.nextInt(7, 20), Random.nextInt(0, 59))
                .atZone(ZoneId.systemDefault())
                .toInstant()

            val durationInMinutes = Random.nextInt(20, 90).toLong()
            val endTime = startTime.plus(Duration.ofMinutes(durationInMinutes))
            val exerciseType = exerciseTypes.random()

            records.add(
                ExerciseSessionRecord(
                    exerciseType = exerciseType,
                    title = getExerciseName(exerciseType),
                    startTime = startTime,
                    startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime)
                )
            )
        }
        Log.d(TAG, "TODO-HUB-07: ${records.size} adet sahte egzersiz verisi oluşturuldu")
        return records.sortedBy { it.startTime }
    }

    /**
     * Egzersiz tipine göre okunabilir isim döndürür
     */
    private fun getExerciseName(exerciseType: Int): String {
        return when (exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Koşu"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Yürüyüş"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Ağırlık Antrenmanı"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Bisiklet"
            else -> "Bilinmeyen Egzersiz"
        }
    }

    /**
     * TODO-HUB-07: Haftalık egzersiz özetini hesaplar
     */
    private fun calculateWeeklyExerciseSummary(exerciseData: List<ExerciseSessionRecord>) {
        val totalMinutes = exerciseData.sumOf {
            val duration = java.time.Duration.between(it.startTime, it.endTime)
            duration.toMinutes()
        }
        val activityCount = exerciseData.size

        val summary = if (activityCount > 0) {
            "Bu Hafta: $activityCount Aktivite, Toplam $totalMinutes Dakika"
        } else {
            "Bu hafta için egzersiz kaydı bulunamadı."
        }
        _weeklyExerciseSummary.value = summary
    }

    /**
     * TODO-HUB-07: Egzersiz tipini gösterme için uygun emoji döndürür
     */
    fun getExerciseEmoji(exerciseType: Int): String {
        return when (exerciseType) {
            56 -> "🏃‍♂️" // EXERCISE_TYPE_RUNNING
            79 -> "🚶‍♂️" // EXERCISE_TYPE_WALKING
            8 -> "🚴‍♂️" // EXERCISE_TYPE_BIKING
            71 -> "🏊‍♂️" // EXERCISE_TYPE_SWIMMING
            65 -> "🏋️‍♂️" // EXERCISE_TYPE_STRENGTH_TRAINING
            86 -> "🧘‍♂️" // EXERCISE_TYPE_YOGA
            52 -> "🤸‍♂️" // EXERCISE_TYPE_PILATES
            27 -> "🏈" // EXERCISE_TYPE_FOOTBALL_AMERICAN
            6 -> "🏀" // EXERCISE_TYPE_BASKETBALL
            73 -> "🎾" // EXERCISE_TYPE_TENNIS
            else -> "🏃‍♂️" // Varsayılan
        }
    }

    /**
     * TODO-G121: Egzersiz tipini localized isim olarak döndürür
     * KURAL 9: Hardcoded text kaldırıldı → LanguageManager kullanılıyor
     */
    fun getExerciseTypeName(exerciseType: Int): String {
        val key = when (exerciseType) {
            56 -> "exercise_type_running" // EXERCISE_TYPE_RUNNING
            79 -> "exercise_type_walking" // EXERCISE_TYPE_WALKING
            8 -> "exercise_type_biking" // EXERCISE_TYPE_BIKING
            71 -> "exercise_type_swimming" // EXERCISE_TYPE_SWIMMING
            65 -> "exercise_type_strength" // EXERCISE_TYPE_STRENGTH_TRAINING
            86 -> "exercise_type_yoga" // EXERCISE_TYPE_YOGA
            52 -> "exercise_type_pilates" // EXERCISE_TYPE_PILATES
            27 -> "exercise_type_football" // EXERCISE_TYPE_FOOTBALL_AMERICAN
            6 -> "exercise_type_basketball" // EXERCISE_TYPE_BASKETBALL
            73 -> "exercise_type_tennis" // EXERCISE_TYPE_TENNIS
            else -> "exercise_type_unknown"
        }
        return com.example.isekaikuroshin.data.LanguageManager.getText(key)
    }

    /**
     * TODO-HUB-07: Egzersiz tarihini format olarak döndürür
     */
    fun formatExerciseDate(exerciseRecord: ExerciseSessionRecord): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM")
        return exerciseRecord.startTime.atZone(ZoneId.systemDefault()).format(formatter)
    }

    /**
     * TODO-HUB-07: Egzersiz süresini dakika olarak döndürür
     */
    fun getExerciseDurationInMinutes(exerciseRecord: ExerciseSessionRecord): Long {
        val duration = java.time.Duration.between(exerciseRecord.startTime, exerciseRecord.endTime)
        return duration.toMinutes()
    }

    /**
     * TODO-HUB-08: Proaktif sağlık analizi çalıştır
     * TODO-HUB-11: Gelişmiş çok boyutlu analiz ile güncellendi
     */
    fun runHealthAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                Log.d(TAG, "TODO-HUB-11: Gelişmiş çok boyutlu sağlık analizi başlatılıyor...")

                // TODO-HUB-11: Yeni analyzeWeeklyPatterns() ile aktivite, kilo ve beslenme birlikte analiz ediliyor
                val insight = proactiveHealthCoach.analyzeWeeklyPatterns()
                _currentHealthInsight.value = insight

                if (insight != null) {
                    Log.d(TAG, "TODO-HUB-11: ✅ Sağlık içgörüsü oluşturuldu: ${insight.title}")
                    Log.d(TAG, "TODO-HUB-11:    - Kategori: ${insight.category}")
                    Log.d(TAG, "TODO-HUB-11:    - Önem: ${insight.severity}")
                } else {
                    Log.d(TAG, "TODO-HUB-11: ℹ️ Bu hafta için özel bir içgörü oluşturulmadı")
                }

            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-11: Sağlık analizi başarısız", e)
                _currentHealthInsight.value = null
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * TODO-HUB-08: Mevcut sağlık içgörüsünü temizle
     */
    fun clearHealthInsight() {
        _currentHealthInsight.value = null
    }

    /**
     * TODO-HUB-10: Bugünün beslenme verilerini yükle
     */
    private fun loadTodaysNutrition() {
        viewModelScope.launch {
            try {
                val nutritionData = loadTodaysNutritionData()
                val currentState = _uiState.value
                if (currentState is HealthHubUiState.Success) {
                    _uiState.value = currentState.copy(nutritionHistory = nutritionData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-10: Bugünün beslenme verileri yüklenemedi", e)
            }
        }
    }

    /**
     * TODO-HUB-10: Bugünün beslenme verilerini Health Connect'ten oku
     */
    private suspend fun loadTodaysNutritionData(): List<NutritionRecord> {
        return try {
            val now = Instant.now()
            val startOfDay = now.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
            val endOfDay = startOfDay.plus(Duration.ofDays(1))

            val nutritionRecords = healthConnectManager.readNutritionHistory(startOfDay, endOfDay)
            Log.d(TAG, "TODO-HUB-10: ${nutritionRecords.size} adet beslenme kaydı okundu")
            nutritionRecords
        } catch (e: Exception) {
            Log.e(TAG, "TODO-HUB-10: Beslenme verileri okuma hatası", e)
            emptyList()
        }
    }

    /**
     * TODO-HUB-10: Beslenme verilerini yeniden yükle
     */
    fun refreshNutritionData() {
        loadTodaysNutrition()
    }

    /**
     * TODO-HUB-12: Dil öğrenim oturumu başlat
     * TODO-HUB-13: Chat history ile güncellendi
     * HATA #3 FIX: Oyun ayarlarından tercih edilen dili oku, yoksa sistem diline fallback yap
     *
     * @param language Öğrenilecek dil (öncelik: 1) method parametresi, 2) oyun ayarları, 3) sistem dili)
     * @param level CEFR seviyesi (A1, A2, B1, B2, C1, C2)
     */
    fun startLanguagePractice(language: String? = null, level: String = "A2") {
        viewModelScope.launch {
            _isLanguageLearning.value = true
            try {
                // HATA #3 FIX: Dil önceliği mantığı
                val finalLanguage = when {
                    // 1. Parametre ile gelen dil varsa onu kullan
                    !language.isNullOrBlank() -> language
                    // 2. Oyun ayarlarında tercih edilen dil varsa onu kullan
                    else -> {
                        val preferredLang = persistentDataManager.gameData.value.settingsData.gameSettings.preferredPracticeLanguage
                        if (preferredLang.isNotBlank()) {
                            Log.d(TAG, "HATA #3 FIX: Using preferred language from settings: $preferredLang")
                            preferredLang
                        } else {
                            // 3. Son çare: Sistem dilini kullan
                            val systemLanguage = java.util.Locale.getDefault().displayLanguage
                            Log.d(TAG, "HATA #3 FIX: No preferred language set, falling back to system language: $systemLanguage")
                            systemLanguage
                        }
                    }
                }

                Log.d(TAG, "TODO-HUB-13: Dil pratiği başlatılıyor: $finalLanguage, Seviye: $level")

                // Chat history'yi temizle (yeni oturum)
                _chatHistory.value = emptyList()

                val result = languageLearningEngine.startLanguageSession(finalLanguage, level)

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    _languageSessionResponse.value = response

                    // TODO-HUB-13: İlk AI mesajını chat history'ye ekle
                    // TODO-HUB-14: expectedResponse ile birlikte sakla
                    response?.let {
                        _chatHistory.value = listOf(
                            ChatMessage(
                                text = it.fullResponse,
                                isFromUser = false,
                                expectedResponse = it.expectedResponse
                            )
                        )
                    }

                    Log.d(TAG, "TODO-HUB-13: ✅ Dil oturumu başarılı!")
                    Log.d(TAG, "TODO-HUB-13: İlk mesaj chat history'ye eklendi")
                } else {
                    Log.e(TAG, "TODO-HUB-13: Dil oturumu başarısız: ${result.exceptionOrNull()?.message}")
                    _languageSessionResponse.value = null
                }

            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-13: Dil pratiği hatası", e)
                _languageSessionResponse.value = null
            } finally {
                _isLanguageLearning.value = false
            }
        }
    }

    /**
     * TODO-HUB-12: Dil oturumu yanıtını temizle
     */
    fun clearLanguageSession() {
        _languageSessionResponse.value = null
        _chatHistory.value = emptyList()
    }

    /**
     * TODO-HUB-13: Kullanıcının mesajını gönder ve AI yanıtını al
     * TODO-HUB-14: İlerleme takibi ve stat güncellemesi eklendi
     *
     * @param userReply Kullanıcının mesajı
     */
    fun sendChatMessage(userReply: String) {
        if (userReply.isBlank()) {
            Log.w(TAG, "TODO-HUB-13: Boş mesaj gönderilemez")
            return
        }

        viewModelScope.launch {
            _isSendingMessage.value = true
            try {
                Log.d(TAG, "TODO-HUB-13: Kullanıcı mesajı gönderiliyor: $userReply")

                // 1. Kullanıcının mesajını chat history'ye ekle
                val userMessage = ChatMessage(
                    text = userReply,
                    isFromUser = true
                )

                _chatHistory.value = _chatHistory.value + userMessage
                Log.d(TAG, "TODO-HUB-13: Kullanıcı mesajı chat history'ye eklendi")

                // TODO-HUB-14: Önceki AI mesajından expected response'u al
                val previousAiMessages = _chatHistory.value.filter { !it.isFromUser }
                val lastExpectedResponse = previousAiMessages.lastOrNull()?.expectedResponse

                // 2. Konuşma geçmişini formatla
                val formattedHistory = formatChatHistory(_chatHistory.value)

                // 3. AI'dan yanıt al
                val result = languageLearningEngine.continueSession(
                    chatHistory = formattedHistory,
                    userReply = userReply,
                    language = "İngilizce",
                    level = "A2"
                )

                if (result.isSuccess) {
                    val (aiResponse, expectedResponse) = result.getOrNull()!!

                    // TODO-HUB-14: İlerleme takibini değerlendir
                    Log.d(TAG, "TODO-HUB-14: İlerleme değerlendirmesi başlıyor...")
                    val progressResult = languageProgressTracker.evaluateAndUpdateProgress(
                        language = "İngilizce",
                        expectedResponse = lastExpectedResponse,
                        actualResponse = userReply
                    )

                    if (progressResult.isSuccess) {
                        val (progress, accuracy) = progressResult.getOrNull()!!
                        Log.d(TAG, "TODO-HUB-14: ✅ İlerleme güncellendi")
                        Log.d(TAG, "TODO-HUB-14: Doğruluk: ${(accuracy * 100).toInt()}%")
                        Log.d(TAG, "TODO-HUB-14: Seviye: ${progress.cefrLevel} - ${progress.progressPercent.toInt()}%")

                        // TODO-HUB-15: UI'daki ilerlemeyi güncelle
                        _languageProgress.value = progress
                        Log.d(TAG, "TODO-HUB-15: UI ilerleme verisi güncellendi")
                    } else {
                        Log.e(TAG, "TODO-HUB-14: İlerleme değerlendirmesi başarısız: ${progressResult.exceptionOrNull()?.message}")
                    }

                    // 4. AI'ın yanıtını chat history'ye ekle (expectedResponse'u sakla)
                    val aiMessage = ChatMessage(
                        text = aiResponse,
                        isFromUser = false,
                        expectedResponse = expectedResponse
                    )

                    _chatHistory.value = _chatHistory.value + aiMessage

                    Log.d(TAG, "TODO-HUB-13: ✅ AI yanıtı başarıyla alındı ve eklendi")
                    Log.d(TAG, "TODO-HUB-13: Toplam mesaj sayısı: ${_chatHistory.value.size}")
                } else {
                    Log.e(TAG, "TODO-HUB-13: AI yanıt alamadı: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-13: Mesaj gönderme hatası", e)
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    /**
     * TODO-HUB-13: Chat history'yi formatlanmış string'e dönüştür
     */
    private fun formatChatHistory(messages: List<ChatMessage>): String {
        return messages.joinToString("\n\n") { message ->
            val speaker = if (message.isFromUser) "Öğrenci" else "Eğitmen"
            "$speaker: ${message.text}"
        }
    }

    /**
     * TODO-HUB-15: Dil öğrenim ilerlemesini yükle
     */
    private fun loadLanguageProgress() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "TODO-HUB-15: Dil ilerlemesi yükleniyor...")
                val progress = languageProgressTracker.getProgressStats("İngilizce")

                if (progress != null) {
                    _languageProgress.value = progress
                    Log.d(TAG, "TODO-HUB-15: ✅ İlerleme yüklendi: ${progress.cefrLevel} - ${progress.progressPercent.toInt()}%")
                } else {
                    Log.d(TAG, "TODO-HUB-15: Henüz ilerleme kaydı yok")
                    _languageProgress.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-15: İlerleme yükleme hatası", e)
                _languageProgress.value = null
            }
        }
    }

    /**
     * TODO-HUB-15: İlerlemeyi yeniden yükle (chat sonrası güncelleme için)
     */
    fun refreshLanguageProgress() {
        loadLanguageProgress()
    }

    /**
     * Health Hub: Görüntü kanıtını işle (kan tahlili, yemek, tartı, vb.)
     * @param imageUri Analiz edilecek görüntünün URI'si
     */
    fun processImageEvidence(imageUri: Uri) {
        viewModelScope.launch {
            _isProcessingImage.value = true
            try {
                Log.d(TAG, "Health Hub: Görüntü analizi başlatılıyor...")

                // CameraVisionEngine ile görüntüyü analiz et
                val result = cameraVisionEngine.analyzeImage(imageUri)

                if (result.isSuccess) {
                    val analysisResult = result.getOrNull()
                    _pendingImageAnalysis.value = analysisResult

                    Log.d(TAG, "Health Hub: ✅ Görüntü analizi başarılı")
                    Log.d(TAG, "  - Veri Tipi: ${analysisResult?.dataType}")
                    Log.d(TAG, "  - Güven Skoru: ${analysisResult?.confidence}")

                    // Onay dialogunu göster
                    _showConfirmationDialog.value = true
                } else {
                    Log.e(TAG, "Health Hub: Görüntü analizi başarısız: ${result.exceptionOrNull()?.message}")
                    _pendingImageAnalysis.value = null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Health Hub: Görüntü işleme hatası", e)
                _pendingImageAnalysis.value = null
            } finally {
                _isProcessingImage.value = false
            }
        }
    }

    /**
     * Health Hub: Kullanıcının düzelttiği değeri uygula
     * @param correctedValue Kullanıcının düzelttiği değer
     */
    fun applyUserCorrection(correctedValue: String) {
        viewModelScope.launch {
            try {
                val currentAnalysis = _pendingImageAnalysis.value
                if (currentAnalysis != null) {
                    Log.d(TAG, "Health Hub: Kullanıcı düzeltmesi uygulanıyor: $correctedValue")

                    // Düzeltilmiş analiz sonucu oluştur
                    val correctedAnalysis = currentAnalysis.copy(userCorrected = true)
                    _pendingImageAnalysis.value = correctedAnalysis

                    // TODO: Düzeltilmiş veriyi Health Connect'e kaydet
                    // Bu kısım FAZ 2'de eklenecek

                    Log.d(TAG, "Health Hub: ✅ Kullanıcı düzeltmesi başarıyla uygulandı")
                }

                // Dialog'u kapat
                _showConfirmationDialog.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Health Hub: Düzeltme uygulama hatası", e)
            }
        }
    }

    /**
     * Health Hub: Görüntü analiz sonucunu onayla ve veritabanına kaydet
     */
    fun confirmImageData() {
        viewModelScope.launch {
            try {
                val currentAnalysis = _pendingImageAnalysis.value
                if (currentAnalysis != null) {
                    Log.d(TAG, "Health Hub: Görüntü verisi onaylandı, kaydediliyor...")

                    // TODO: Veriyi Health Connect'e kaydet
                    // Bu kısım FAZ 2'de eklenecek
                    when (currentAnalysis.dataType) {
                        com.example.isekaikuroshin.data.DataType.WEIGHT -> {
                            Log.d(TAG, "Health Hub: Kilo verisi kaydedilecek")
                            // healthConnectManager.writeWeightRecord(...)
                        }
                        com.example.isekaikuroshin.data.DataType.FOOD -> {
                            Log.d(TAG, "Health Hub: Beslenme verisi kaydedilecek")
                            // healthConnectManager.writeNutritionRecord(...)
                        }
                        com.example.isekaikuroshin.data.DataType.BLOOD_TEST -> {
                            Log.d(TAG, "Health Hub: Kan tahlili verisi kaydedilecek")
                        }
                        com.example.isekaikuroshin.data.DataType.FITNESS -> {
                            Log.d(TAG, "Health Hub: Fitness verisi kaydedilecek")
                        }
                        else -> {
                            Log.w(TAG, "Health Hub: Bilinmeyen veri tipi")
                        }
                    }

                    Log.d(TAG, "Health Hub: ✅ Veri başarıyla onaylandı")
                }

                // Dialog'u kapat
                _showConfirmationDialog.value = false
                _pendingImageAnalysis.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Health Hub: Veri onaylama hatası", e)
            }
        }
    }

    /**
     * Health Hub: Onay dialogunu kapat
     */
    fun dismissImageDialog() {
        _showConfirmationDialog.value = false
        _pendingImageAnalysis.value = null
    }

    /**
     * Health Hub: Onay dialogunu kapat (eski isim - deprecate edilecek)
     */
    @Deprecated("Use dismissImageDialog() instead", ReplaceWith("dismissImageDialog()"))
    fun dismissConfirmationDialog() {
        dismissImageDialog()
    }
}

// UI State Sealed Class
sealed class HealthHubUiState {
    object Loading : HealthHubUiState()
    object PermissionsRequired : HealthHubUiState()
    data class Success(
        val weightHistory: List<WeightRecord> = emptyList(),
        val exerciseHistory: List<ExerciseSessionRecord> = emptyList(),
        val nutritionHistory: List<NutritionRecord> = emptyList()
    ) : HealthHubUiState()

    data class Error(val message: String) : HealthHubUiState()
}
