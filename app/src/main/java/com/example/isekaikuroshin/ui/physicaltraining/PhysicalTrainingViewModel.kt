package com.example.isekaikuroshin.ui.physicaltraining

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.PoseTemplate
import com.example.isekaikuroshin.data.PoseTemplateRepository
import com.example.isekaikuroshin.engine.pose.*
import com.google.mlkit.vision.pose.Pose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

/**
 * ViewModel for Physical Training Screen
 *
 * Kadim Mühür sisteminden öğrenilen state management ve
 * tracking loss detection tekniklerini kullanır.
 *
 * SORUMLULUKLAR:
 * - Egzersiz türü seçimi
 * - PoseEstimationEngine yönetimi
 * - Kalman filtreli stabilizasyon
 * - Tekrar sayma
 * - Kalibrasyon yönetimi
 */
@HiltViewModel
class PhysicalTrainingViewModel @Inject constructor(
    private val poseTemplateRepository: PoseTemplateRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PhysicalTrainingVM"
    }

    // ========================================
    // UI STATE
    // ========================================

    private val _uiState = MutableStateFlow(PhysicalTrainingUiState())
    val uiState: StateFlow<PhysicalTrainingUiState> = _uiState.asStateFlow()

    // ========================================
    // POSE ESTIMATION ENGINE
    // ========================================

    private var poseEngine: PoseEstimationEngine? = null
    private var currentRepCounter: RepCounterBase? = null

    // ========================================
    // INITIALIZATION
    // ========================================

    init {
        Log.d(TAG, "PhysicalTrainingViewModel initialized")
    }

    /**
     * Egzersiz türünü seç ve engine'i başlat
     */
    fun selectExercise(exerciseType: PoseEstimationEngine.ExerciseType) {
        viewModelScope.launch {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Selecting exercise: $exerciseType")
            Log.d(TAG, "========================================")

            // Engine oluştur
            poseEngine = PoseEstimationEngine(exerciseType)

            // Uygun RepCounter oluştur
            currentRepCounter = when (exerciseType) {
                PoseEstimationEngine.ExerciseType.PUSH_UP -> PushUpRepCounter()
                PoseEstimationEngine.ExerciseType.SIT_UP -> SitUpRepCounter()
                PoseEstimationEngine.ExerciseType.ROPE_SKIPPING -> RopeSkippingRepCounter()
            }

            // RepCounter'ı engine'e bağla
            poseEngine?.setRepCounter(currentRepCounter!!)

            // Kalibrasyon varsa yükle
            loadCalibration(exerciseType)

            // UI state güncelle
            _uiState.update {
                it.copy(
                    selectedExercise = exerciseType,
                    isSessionActive = true,
                    repCount = 0,
                    feedback = "Hazır! Egzersize başlayın",
                    formQuality = 0f
                )
            }

            Log.d(TAG, "✅ Exercise selected and engine initialized")
        }
    }

    /**
     * Kalibrasyon yükle (varsa)
     */
    private suspend fun loadCalibration(exerciseType: PoseEstimationEngine.ExerciseType) {
        val exerciseTypeString = exerciseType.name
        val template = poseTemplateRepository.getActiveTemplate(exerciseTypeString)

        if (template != null) {
            Log.d(TAG, "✅ Calibration found for $exerciseTypeString")

            // RepCounter'a kalibrasyon uygula
            val calibratedPose = RepCounterBase.CalibratedPose(
                upperPositionAngles = template.upperPosition.angles,
                lowerPositionAngles = template.lowerPosition.angles
            )
            currentRepCounter?.setCalibration(calibratedPose)

            _uiState.update { it.copy(hasCalibration = true) }
        } else {
            Log.d(TAG, "⚠️ No calibration found for $exerciseTypeString, using defaults")
            _uiState.update { it.copy(hasCalibration = false) }
        }
    }

    // ========================================
    // POSE PROCESSING
    // ========================================

    /**
     * Pose'u işle - Her frame'de çağrılır
     *
     * NOT: Ham pose verisi UI'a GÖNDERİLMİYOR - çizim için doğrudan
     * CameraPreview'dan alınan ham pose kullanılıyor (flickering önleme)
     *
     * Kalman filtresi sadece engine içinde tekrar sayma doğruluğu için kullanılıyor
     */
    fun processPose(pose: Pose?) {
        val engine = poseEngine
        if (engine == null) {
            Log.w(TAG, "⚠️ Engine not initialized, skipping pose processing")
            return
        }

        // Pose'u engine'e gönder (Kalman filtresi SADECE burada, tekrar sayma için)
        val result = engine.processPose(pose)

        // UI state güncelle (tekrar sayma verileri + normalized landmarks)
        _uiState.update { currentState ->
            currentState.copy(
                repCount = result.repCount,
                feedback = result.feedback,
                formQuality = result.formQuality,
                isTracking = result.isTracking,
                isNewRep = result.isNewRep,
                normalizedLandmarks = result.normalizedLandmarks,  // ⚡ YENİ: Projeksiyon için
                relevantLandmarkTypes = result.relevantLandmarkTypes  // ⚡ BÖLÜM A: Filtreleme için
            )
        }

        // Yeni tekrar sayıldıysa log
        if (result.isNewRep) {
            Log.d(TAG, "🎉 NEW REP! Total: ${result.repCount}")
        }

        // Debug log (her 30 frame'de bir)
        if (result.repCount % 30 == 0 && result.isTracking) {
            Log.d(TAG, "Tracking: ${result.isTracking}, Reps: ${result.repCount}, Form: ${(result.formQuality * 100).toInt()}%")
        }
    }

    /**
     * Tracking kaybedildiğinde çağrılır
     */
    fun onTrackingLost() {
        poseEngine?.onTrackingLost()
    }

    // ========================================
    // KALIBRASYON
    // ========================================

    /**
     * Kalibrasyon başlat - Egzersize özel talimatlar
     */
    fun startCalibration() {
        val exerciseType = _uiState.value.selectedExercise
        val upperMessage = when (exerciseType) {
            PoseEstimationEngine.ExerciseType.PUSH_UP ->
                "🔼 BAŞLANGIÇ POZİSYONU\nKollar düz, vücut yukarıda"
            PoseEstimationEngine.ExerciseType.SIT_UP ->
                "🔼 BAŞLANGIÇ POZİSYONU\nSırtınız yerde, eller kulak hizasında"
            PoseEstimationEngine.ExerciseType.ROPE_SKIPPING ->
                "🔼 BAŞLANGIÇ POZİSYONU\nAyaklar yere basmış, dik duruş"
            null -> "Üst pozisyona geçin"
        }

        _uiState.update {
            it.copy(
                isCalibrating = true,
                calibrationStep = CalibrationStep.UPPER_POSITION,
                calibrationMessage = upperMessage
            )
        }
        Log.d(TAG, "🎯 Calibration started for $exerciseType")
    }

    /**
     * Mevcut pozisyonu kaydet (kalibrasyon için) - Egzersize özel talimatlar
     */
    fun captureCalibrationPose(pose: Pose) {
        val state = _uiState.value
        val exerciseType = state.selectedExercise ?: return

        when (state.calibrationStep) {
            CalibrationStep.UPPER_POSITION -> {
                // Üst pozisyonu kaydet
                val angles = extractAngles(pose, exerciseType)

                val lowerMessage = when (exerciseType) {
                    PoseEstimationEngine.ExerciseType.PUSH_UP ->
                        "🔽 BİTİŞ POZİSYONU\nKollar bükülmüş, göğüs yere yakın"
                    PoseEstimationEngine.ExerciseType.SIT_UP ->
                        "🔽 BİTİŞ POZİSYONU\nGövde yukarıda, dizlere doğru kıvrılmış"
                    PoseEstimationEngine.ExerciseType.ROPE_SKIPPING ->
                        "🔽 BİTİŞ POZİSYONU\nZıplamanın en üst noktası"
                    else -> "Alt pozisyona geçin"
                }

                _uiState.update {
                    it.copy(
                        calibrationUpperAngles = angles,
                        calibrationStep = CalibrationStep.LOWER_POSITION,
                        calibrationMessage = lowerMessage
                    )
                }
                Log.d(TAG, "✅ Upper position captured: $angles")
            }

            CalibrationStep.LOWER_POSITION -> {
                // Alt pozisyonu kaydet
                val angles = extractAngles(pose, exerciseType)
                _uiState.update {
                    it.copy(
                        calibrationLowerAngles = angles,
                        calibrationStep = CalibrationStep.COMPLETE,
                        calibrationMessage = "✅ Kalibrasyon Tamamlandı!\nArtık egzersize başlayabilirsiniz"
                    )
                }
                Log.d(TAG, "✅ Lower position captured: $angles")

                // Kalibrasyon verisini kaydet
                saveCalibration()
            }

            CalibrationStep.COMPLETE -> {
                // Zaten tamamlanmış
            }

            null -> {
                // Kalibrasyon aktif değil
            }
        }
    }

    /**
     * Pose'dan önemli açıları çıkar
     */
    private fun extractAngles(pose: Pose, exerciseType: PoseEstimationEngine.ExerciseType): Map<String, Float> {
        return when (exerciseType) {
            PoseEstimationEngine.ExerciseType.PUSH_UP -> {
                // Şınav: Dirsek açısı
                val rightShoulder = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER)
                val rightElbow = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ELBOW)
                val rightWrist = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST)

                val elbowAngle = calculateAngle(rightShoulder, rightElbow, rightWrist) ?: 0f
                mapOf("elbow_angle" to elbowAngle)
            }

            PoseEstimationEngine.ExerciseType.SIT_UP -> {
                // Mekik: Gövde açısı
                val rightShoulder = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER)
                val rightHip = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP)
                val rightKnee = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE)

                val torsoAngle = calculateAngle(rightShoulder, rightHip, rightKnee) ?: 0f
                mapOf("torso_angle" to torsoAngle)
            }

            PoseEstimationEngine.ExerciseType.ROPE_SKIPPING -> {
                // İp atlama: Ayak Y pozisyonu
                val rightAnkle = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE)
                val ankleY = rightAnkle?.position?.y ?: 0f
                mapOf("ankle_y" to ankleY)
            }
        }
    }

    /**
     * Açı hesapla (RepCounterBase'den kopyalandı)
     */
    private fun calculateAngle(
        a: com.google.mlkit.vision.pose.PoseLandmark?,
        b: com.google.mlkit.vision.pose.PoseLandmark?,
        c: com.google.mlkit.vision.pose.PoseLandmark?
    ): Float? {
        if (a == null || b == null || c == null) return null

        val ax = a.position.x
        val ay = a.position.y
        val bx = b.position.x
        val by = b.position.y
        val cx = c.position.x
        val cy = c.position.y

        val ba = Pair(ax - bx, ay - by)
        val bc = Pair(cx - bx, cy - by)

        val dot = ba.first * bc.first + ba.second * bc.second
        val magBA = kotlin.math.sqrt(ba.first * ba.first + ba.second * ba.second)
        val magBC = kotlin.math.sqrt(bc.first * bc.first + bc.second * bc.second)

        val cosAngle = dot / (magBA * magBC)
        val angleRad = kotlin.math.acos(cosAngle.coerceIn(-1f, 1f))
        return Math.toDegrees(angleRad.toDouble()).toFloat()
    }

    /**
     * Kalibrasyonu kaydet
     */
    private fun saveCalibration() {
        viewModelScope.launch {
            val state = _uiState.value
            val exerciseType = state.selectedExercise ?: return@launch
            val upperAngles = state.calibrationUpperAngles ?: return@launch
            val lowerAngles = state.calibrationLowerAngles ?: return@launch

            val template = PoseTemplate(
                id = "user_default_${exerciseType.name.lowercase()}",
                exerciseType = exerciseType.name,
                userId = "default",
                upperPosition = com.example.isekaikuroshin.data.PosePosition(angles = upperAngles),
                lowerPosition = com.example.isekaikuroshin.data.PosePosition(angles = lowerAngles)
            )

            poseTemplateRepository.saveTemplate(template)

            // RepCounter'a uygula
            val calibratedPose = RepCounterBase.CalibratedPose(
                upperPositionAngles = upperAngles,
                lowerPositionAngles = lowerAngles
            )
            currentRepCounter?.setCalibration(calibratedPose)

            _uiState.update { it.copy(hasCalibration = true) }

            Log.d(TAG, "✅ Calibration saved!")
        }
    }

    /**
     * Kalibrasyonu iptal et
     */
    fun cancelCalibration() {
        _uiState.update {
            it.copy(
                isCalibrating = false,
                calibrationStep = null,
                calibrationMessage = "",
                calibrationUpperAngles = null,
                calibrationLowerAngles = null
            )
        }
        Log.d(TAG, "❌ Calibration cancelled")
    }

    /**
     * Kalibrasyonu kapat (tamamlandıktan sonra)
     */
    fun finishCalibration() {
        _uiState.update {
            it.copy(
                isCalibrating = false,
                calibrationStep = null,
                calibrationMessage = ""
            )
        }
    }

    // ========================================
    // SESSION MANAGEMENT
    // ========================================

    /**
     * Egzersizi sıfırla
     */
    fun resetExercise() {
        currentRepCounter?.reset()
        poseEngine?.resetState()

        _uiState.update {
            it.copy(
                repCount = 0,
                feedback = "Sıfırlandı! Tekrar başlayın",
                formQuality = 0f,
                isNewRep = false
            )
        }

        Log.d(TAG, "🔄 Exercise reset")
    }

    /**
     * Egzersizi bitir
     */
    fun finishExercise() {
        val finalRepCount = _uiState.value.repCount
        val exerciseType = _uiState.value.selectedExercise

        _uiState.update {
            it.copy(
                isSessionActive = false,
                sessionComplete = true,
                finalRepCount = finalRepCount
            )
        }

        Log.d(TAG, "🏁 Exercise finished! Total reps: $finalRepCount")

        // G83: Log exercise activity to Journal
        if (exerciseType != null && finalRepCount > 0) {
            val exerciseName = when (exerciseType) {
                PoseEstimationEngine.ExerciseType.PUSH_UP -> "Push-ups"
                else -> exerciseType.toString()
            }

            // Tahmini kalori: Her tekrar yaklaşık 0.5 kcal
            val estimatedCalories = (finalRepCount * 0.5).toInt()

            com.example.isekaikuroshin.utils.EventLogger.logExercise(
                exerciseType = exerciseName,
                reps = finalRepCount,
                calories = estimatedCalories
            )

            Log.d(TAG, "✅ G83: Exercise logged - $exerciseName x$finalRepCount ($estimatedCalories kcal)")
        }
    }

    // ========================================
    // CLEANUP
    // ========================================

    override fun onCleared() {
        super.onCleared()

        Log.d(TAG, "========================================")
        Log.d(TAG, "PhysicalTrainingViewModel.onCleared() - Cleanup starting")
        Log.d(TAG, "========================================")

        poseEngine?.cleanup()
        poseEngine = null
        currentRepCounter = null

        Log.d(TAG, "✅ Cleanup complete")
    }
}

// ========================================
// UI STATE
// ========================================

/**
 * UI State for Physical Training
 */
data class PhysicalTrainingUiState(
    // Egzersiz seçimi
    val selectedExercise: PoseEstimationEngine.ExerciseType? = null,
    val isSessionActive: Boolean = false,

    // Tekrar sayma
    val repCount: Int = 0,
    val feedback: String = "Egzersiz seçin",
    val formQuality: Float = 0f,
    val isTracking: Boolean = false,
    val isNewRep: Boolean = false,  // Yeni tekrar animasyonu için

    // ⚡ YENİ: Normalize edilmiş landmark'lar (projeksiyon için)
    // İKİLİ VERİ SİSTEMİ:
    // - Ham pose: Doğrudan kameradan alınır (konum için)
    // - Normalized landmarks: PoseEstimationEngine'den gelir (şekil için)
    val normalizedLandmarks: List<NormalizedPoint> = emptyList(),

    // ⚡ BÖLÜM A: Egzersize özel landmark filtreleme
    val relevantLandmarkTypes: Set<Int> = emptySet(),

    // Kalibrasyon
    val hasCalibration: Boolean = false,
    val isCalibrating: Boolean = false,
    val calibrationStep: CalibrationStep? = null,
    val calibrationMessage: String = "",
    val calibrationUpperAngles: Map<String, Float>? = null,
    val calibrationLowerAngles: Map<String, Float>? = null,

    // Session sonucu
    val sessionComplete: Boolean = false,
    val finalRepCount: Int = 0
)

/**
 * Kalibrasyon adımları
 */
enum class CalibrationStep {
    UPPER_POSITION,  // Üst pozisyon (örn: kollar uzanmış)
    LOWER_POSITION,  // Alt pozisyon (örn: kollar bükülmüş)
    COMPLETE         // Tamamlandı
}
