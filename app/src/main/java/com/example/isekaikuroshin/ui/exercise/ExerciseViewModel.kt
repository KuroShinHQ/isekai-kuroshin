package com.example.isekaikuroshin.ui.exercise

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.game.GameStateManager
import com.example.isekaikuroshin.utils.GameLogger
import com.google.mlkit.vision.pose.Pose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Egzersiz Türü Enum
 */
enum class ExerciseTypeSelection {
    PUSH_UP,    // Şınav
    SIT_UP      // Mekik
}

/**
 * Initialization State (Başlatma Durumu)
 */
sealed class InitializationState {
    object Loading : InitializationState()  // Yükleniyor
    object Ready : InitializationState()    // Hazır
    data class Error(val message: String) : InitializationState()  // Hata
}

/**
 * Gelişmiş Exercise ViewModel
 *
 * Bu ViewModel, egzersiz ekranının state yönetimini,
 * pose detection sonuçlarının işlenmesini,
 * egzersiz türü seçimini ve oyunlaştırma ödüllerinin hesaplanmasını sağlar.
 *
 * YENİ ÖZELLİKLER:
 * - Şınav ve Mekik arasında seçim
 * - Dinamik sayaç değiştirme
 * - Her egzersiz türü için ayrı istatistik takibi
 */
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    companion object {
        private const val TAG = "ExerciseViewModel"
    }

    // ═══ LAZY INITIALIZATION (Performans Optimizasyonu) ═══
    // CRITICAL: Ağır nesneleri baştan yüklemek yerine arka planda yükle

    // Initialization state
    private val _initializationState = MutableStateFlow<InitializationState>(InitializationState.Loading)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    // YENİ: Egzersiz türü seçimi
    private val _selectedExerciseType = MutableStateFlow(ExerciseTypeSelection.PUSH_UP)
    val selectedExerciseType: StateFlow<ExerciseTypeSelection> = _selectedExerciseType.asStateFlow()

    // Sayaçlar (LAZY - arka planda yüklenecek)
    private var pushUpCounter: PushUpCounter? = null
    private var sitUpCounter: SitUpCounter? = null

    // Gamification Engine (LAZY)
    private var gamificationEngine: GamificationEngine? = null

    // Form kalitesi geçmişi (ortalama hesaplamak için)
    private val formQualityHistory = mutableListOf<FormQuality>()

    // Egzersiz başlangıç zamanı
    private var exerciseStartTime = 0L

    // UI State Flow'ları
    private val _exerciseCount = MutableStateFlow(0)
    val exerciseCount: StateFlow<Int> = _exerciseCount.asStateFlow()

    private val _feedback = MutableStateFlow("Başlamak için egzersiz seçin")
    val feedback: StateFlow<String> = _feedback.asStateFlow()

    private val _formQuality = MutableStateFlow(FormQuality.NEUTRAL)
    val formQuality: StateFlow<FormQuality> = _formQuality.asStateFlow()

    private val _currentAngle = MutableStateFlow(0f)
    val currentAngle: StateFlow<Float> = _currentAngle.asStateFlow()

    private val _exerciseState = MutableStateFlow(ExerciseState.READY)
    val exerciseState: StateFlow<ExerciseState> = _exerciseState.asStateFlow()

    // YENİ: Form skoru (0-100 arası)
    private val _formScore = MutableStateFlow(0f)
    val formScore: StateFlow<Float> = _formScore.asStateFlow()

    // YENİ: Tekrar başarı efekti trigger (geçerli tekrar sayıldığında true olur)
    private val _repSuccessTrigger = MutableStateFlow(false)
    val repSuccessTrigger: StateFlow<Boolean> = _repSuccessTrigger.asStateFlow()

    // Önceki tekrar sayısı (yeni tekrar algılama için)
    private var previousRepCount = 0

    // Ödül hesaplama state'leri
    private val _isCalculatingReward = MutableStateFlow(false)
    val isCalculatingReward: StateFlow<Boolean> = _isCalculatingReward.asStateFlow()

    private val _workoutReward = MutableStateFlow<WorkoutReward?>(null)
    val workoutReward: StateFlow<WorkoutReward?> = _workoutReward.asStateFlow()

    init {
        exerciseStartTime = System.currentTimeMillis()
        // CRITICAL: Ağır nesneleri arka planda başlat
        initializeExerciseComponents()
    }

    /**
     * LAZY INITIALIZATION: Ağır bileşenleri arka planda yükle
     *
     * PERFORMANS OPTİMİZASYONU:
     * - Ana thread'i bloke etme
     * - Dispatchers.IO kullanarak arka planda çalıştır
     * - Loading state ile kullanıcıya feedback göster
     */
    private fun initializeExerciseComponents() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                GameLogger.logSystem("[EXERCISE_VM] INITIALIZATION_START")
                val startTime = System.currentTimeMillis()

                // Ağır nesneleri arka planda oluştur
                val pushUpCounterTemp = PushUpCounter()
                GameLogger.logSystem("[EXERCISE_VM] PushUpCounter initialized (${System.currentTimeMillis() - startTime}ms)")

                val sitUpCounterTemp = SitUpCounter()
                GameLogger.logSystem("[EXERCISE_VM] SitUpCounter initialized (${System.currentTimeMillis() - startTime}ms)")

                val gamificationEngineTemp = GamificationEngine()
                GameLogger.logSystem("[EXERCISE_VM] GamificationEngine initialized (${System.currentTimeMillis() - startTime}ms)")

                // Ana thread'de ata
                withContext(Dispatchers.Main) {
                    pushUpCounter = pushUpCounterTemp
                    sitUpCounter = sitUpCounterTemp
                    gamificationEngine = gamificationEngineTemp

                    _initializationState.value = InitializationState.Ready

                    val totalTime = System.currentTimeMillis() - startTime
                    GameLogger.logSystem("[EXERCISE_VM] INITIALIZATION_COMPLETE: ${totalTime}ms")
                    Log.d(TAG, "✅ Egzersiz bileşenleri hazır (${totalTime}ms)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Initialization error: ${e.message}", e)
                GameLogger.logError("EXERCISE_VM", "INITIALIZATION_FAILED: ${e.message}")
                withContext(Dispatchers.Main) {
                    _initializationState.value = InitializationState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * YENİ: Egzersiz türünü değiştir
     */
    fun selectExerciseType(type: ExerciseTypeSelection) {
        _selectedExerciseType.value = type

        // Sayaçları sıfırla
        resetExercise()

        // Başlangıç feedback'i güncelle
        _feedback.value = when (type) {
            ExerciseTypeSelection.PUSH_UP -> "Başlamak için plank pozisyonuna geçin"
            ExerciseTypeSelection.SIT_UP -> "Sırt üstü uzanın"
        }

        Log.d(TAG, "🎯 Egzersiz türü seçildi: ${type.name}")
    }

    /**
     * Pose verisini işler ve UI state'i günceller
     *
     * BEST PRACTICE: Seçilen egzersiz türüne göre doğru sayacı kullan
     *
     * PERFORMANCE PROFILER: Her işlem adımının süresini ölçer
     */
    fun processPose(pose: Pose?) {
        // CRITICAL: Başlatma tamamlanmadan işleme yapma
        if (_initializationState.value !is InitializationState.Ready) {
            return
        }

        if (pose == null) {
            _feedback.value = "⏳ Pose bekleniyor..."
            return
        }

        // ═══ PERFORMANCE PROFILER START ═══
        val profilerStart = System.currentTimeMillis()
        val stepTimes = mutableMapOf<String, Long>()
        var lastStepTime = profilerStart

        // Seçilen egzersiz türüne göre işle
        when (_selectedExerciseType.value) {
            ExerciseTypeSelection.PUSH_UP -> {
                val counter = pushUpCounter ?: return

                // PROFILER: Counter processing
                val result = counter.processPose(pose)
                stepTimes["CounterProcessing"] = System.currentTimeMillis() - lastStepTime
                lastStepTime = System.currentTimeMillis()

                // YENİ: Tekrar sayısı arttıysa başarı efekti tetikle
                if (result.count > previousRepCount) {
                    _repSuccessTrigger.value = true
                    previousRepCount = result.count

                    // 2 saniye sonra efekti kapat (coroutine ile)
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2000)
                        _repSuccessTrigger.value = false
                    }
                }
                stepTimes["TriggerCheck"] = System.currentTimeMillis() - lastStepTime
                lastStepTime = System.currentTimeMillis()

                // UI state'leri güncelle
                _exerciseCount.value = result.count
                _feedback.value = result.feedback
                _formQuality.value = result.formQuality
                _currentAngle.value = result.elbowAngle
                _exerciseState.value = result.state
                _formScore.value = result.bodyAlignment  // YENİ: Form skoru (0-100)

                stepTimes["UIUpdate"] = System.currentTimeMillis() - lastStepTime
                lastStepTime = System.currentTimeMillis()

                // Form kalitesi geçmişine ekle
                if (result.state == ExerciseState.UP || result.state == ExerciseState.DOWN) {
                    formQualityHistory.add(result.formQuality)
                    if (formQualityHistory.size > 100) {
                        formQualityHistory.removeAt(0)
                    }
                }
                stepTimes["HistoryUpdate"] = System.currentTimeMillis() - lastStepTime
            }

            ExerciseTypeSelection.SIT_UP -> {
                val counter = sitUpCounter ?: return

                // PROFILER: Counter processing
                val result = counter.processPose(pose)
                stepTimes["CounterProcessing"] = System.currentTimeMillis() - lastStepTime
                lastStepTime = System.currentTimeMillis()

                // UI state'leri güncelle
                _exerciseCount.value = result.count
                _feedback.value = result.feedback
                _formQuality.value = result.formQuality
                _currentAngle.value = result.torsoAngle
                _exerciseState.value = result.state

                stepTimes["UIUpdate"] = System.currentTimeMillis() - lastStepTime
                lastStepTime = System.currentTimeMillis()

                // Form kalitesi geçmişine ekle
                if (result.state == ExerciseState.UP || result.state == ExerciseState.DOWN) {
                    formQualityHistory.add(result.formQuality)
                    if (formQualityHistory.size > 100) {
                        formQualityHistory.removeAt(0)
                    }
                }
                stepTimes["HistoryUpdate"] = System.currentTimeMillis() - lastStepTime
            }
        }

        // ═══ PERFORMANCE PROFILER LOG ═══
        val totalTime = System.currentTimeMillis() - profilerStart

        // Her 30 frame'de bir profiler raporu göster (spam önleme)
        if (totalTime > 30 || (_exerciseCount.value > 0 && _exerciseCount.value % 5 == 0)) {
            val counterTime = stepTimes["CounterProcessing"] ?: 0L
            val triggerTime = stepTimes["TriggerCheck"] ?: 0L
            val uiTime = stepTimes["UIUpdate"] ?: 0L
            val historyTime = stepTimes["HistoryUpdate"] ?: 0L

            GameLogger.logSystem(
                "[PROFILER] Counter: ${counterTime}ms | Trigger: ${triggerTime}ms | " +
                "UI: ${uiTime}ms | History: ${historyTime}ms | TOTAL: ${totalTime}ms"
            )

            // Performans uyarısı (> 50ms ise)
            if (totalTime > 50) {
                Log.w(TAG, "⚠️ PERFORMANCE WARNING: processPose took ${totalTime}ms (target: <33ms)")
            }
        }
    }

    /**
     * Egzersizi bitir ve ödülleri hesapla
     */
    fun finishExercise(onComplete: (WorkoutReward) -> Unit) {
        Log.d(TAG, "📍 finishExercise() fonksiyonu çağrıldı")
        viewModelScope.launch {
            try {
                Log.d(TAG, "📍 finishExercise coroutine başladı")
                _isCalculatingReward.value = true

                val currentCount = getCurrentCount()
                // TODO-G121: KURAL 9 - Hardcoded text kaldırıldı
                val exerciseTypeName = when (_selectedExerciseType.value) {
                    ExerciseTypeSelection.PUSH_UP -> com.example.isekaikuroshin.data.LanguageManager.getText("exercise_type_push_up")
                    ExerciseTypeSelection.SIT_UP -> com.example.isekaikuroshin.data.LanguageManager.getText("exercise_type_sit_up")
                }

                Log.d(TAG, "🏁 $exerciseTypeName bitiyor... Toplam tekrar: $currentCount")

                // Egzersiz süresi
                val durationMs = System.currentTimeMillis() - exerciseStartTime

                // Ortalama form kalitesi
                val engine = gamificationEngine ?: return@launch
                val avgFormQuality = engine.calculateAverageFormQuality(formQualityHistory)
                Log.d(TAG, "📊 Ortalama form kalitesi: ${(avgFormQuality * 100).toInt()}%")

                // Ödülleri hesapla (egzersiz türüne göre)
                val exerciseType = when (_selectedExerciseType.value) {
                    ExerciseTypeSelection.PUSH_UP -> ExerciseType.PUSH_UP
                    ExerciseTypeSelection.SIT_UP -> ExerciseType.SIT_UP
                }

                val reward = engine.calculateReward(
                    exerciseType = exerciseType,
                    repCount = currentCount,
                    duration = durationMs,
                    formQualityAvg = avgFormQuality
                )

                _workoutReward.value = reward

                Log.d(TAG, "🎁 Ödüller hesaplandı:")
                Log.d(TAG, "  - XP: ${reward.experience}")
                Log.d(TAG, "  - Kalori: ${reward.calories}")
                Log.d(TAG, "  - Stat artışları: ${reward.stats}")

                // GameStateManager'a stat artışlarını uygula
                applyRewardsToGameState(reward)

                // G83: Log exercise completion to Journal
                com.example.isekaikuroshin.utils.EventLogger.logExercise(
                    exerciseType = exerciseTypeName,
                    reps = currentCount,
                    calories = reward.calories.toInt()
                )

                // TODO-G122: Callback'i çağır
                Log.d(TAG, "📍 Callback çağrılıyor...")
                onComplete(reward)
                Log.d(TAG, "✅ Callback çağrıldı!")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Ödül hesaplama hatası: ${e.message}", e)
            } finally {
                _isCalculatingReward.value = false
            }
        }
    }

    /**
     * Ödülleri oyun durumuna uygular
     */
    private suspend fun applyRewardsToGameState(reward: WorkoutReward) {
        try {
            val currentState = gameStateManager.gameState.value
            if (currentState == null) {
                Log.w(TAG, "⚠️ Game state yok, ödüller uygulanamadı")
                return
            }

            val currentPlayerState = currentState.playerState

            // Stat artışlarını uygula
            val updatedPlayerState = currentPlayerState.copy(
                // Stat artışları
                strength = currentPlayerState.strength + (reward.stats[StatType.STRENGTH] ?: 0f),
                agility = currentPlayerState.agility + (reward.stats[StatType.AGILITY] ?: 0f),
                vitality = currentPlayerState.vitality + (reward.stats[StatType.VITALITY] ?: 0f)
            )

            // Güncellenmiş state'i kaydet
            val updatedGameState = currentState.copy(playerState = updatedPlayerState)
            gameStateManager.saveGameState(updatedGameState)

            Log.d(TAG, "✅ Stat artışları oyun durumuna uygulandı")
            Log.d(TAG, "  - Güç: ${currentPlayerState.strength} -> ${updatedPlayerState.strength}")
            Log.d(TAG, "  - Dayanıklılık: ${currentPlayerState.vitality} -> ${updatedPlayerState.vitality}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Oyun durumu güncelleme hatası: ${e.message}", e)
        }
    }

    /**
     * Egzersizi sıfırla
     */
    fun resetExercise() {
        // Her iki sayacı da sıfırla
        pushUpCounter?.reset()
        sitUpCounter?.reset()

        formQualityHistory.clear()
        exerciseStartTime = System.currentTimeMillis()
        previousRepCount = 0  // YENİ: Tekrar sayısını sıfırla

        _exerciseCount.value = 0
        _feedback.value = when (_selectedExerciseType.value) {
            ExerciseTypeSelection.PUSH_UP -> "Başlamak için plank pozisyonuna geçin"
            ExerciseTypeSelection.SIT_UP -> "Sırt üstü uzanın"
        }
        _formQuality.value = FormQuality.NEUTRAL
        _currentAngle.value = 0f
        _exerciseState.value = ExerciseState.READY
        _formScore.value = 0f  // YENİ: Form skorunu sıfırla
        _repSuccessTrigger.value = false  // YENİ: Başarı efektini sıfırla
        _workoutReward.value = null

        Log.d(TAG, "🔄 Egzersiz sıfırlandı")
    }

    /**
     * Mevcut sayıyı al (seçilen egzersiz türüne göre)
     */
    fun getCurrentCount(): Int {
        return when (_selectedExerciseType.value) {
            ExerciseTypeSelection.PUSH_UP -> pushUpCounter?.getCount() ?: 0
            ExerciseTypeSelection.SIT_UP -> sitUpCounter?.getCount() ?: 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}
