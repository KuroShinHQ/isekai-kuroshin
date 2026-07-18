package com.example.isekaikuroshin.ui.sealpractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.engine.GestureRecognitionEngine
import com.example.isekaikuroshin.engine.GestureResult
import com.example.isekaikuroshin.engine.PerformanceLevel
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Seal Practice Screen
 *
 * Manages:
 * - Available seals for player
 * - Practice session state
 * - Hand detection results processing
 * - Seal mastery updates
 * - Skill unlocking when seal is mastered
 */
@HiltViewModel
class SealPracticeViewModel @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val gestureEngine: GestureRecognitionEngine,
    private val sealRepository: SealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SealPracticeUiState())
    val uiState: StateFlow<SealPracticeUiState> = _uiState.asStateFlow()

    // Battery optimization settings
    private val MAX_SESSION_DURATION_MS = 10 * 60 * 1000L  // 10 dakika max session
    private val IDLE_TIMEOUT_MS = 2 * 60 * 1000L  // 2 dakika idle timeout
    private var lastActivityTime = System.currentTimeMillis()
    private var sessionTimeoutJob: kotlinx.coroutines.Job? = null

    init {
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("SealPracticeViewModel initialized")

        // Initialize default seals if not already done
        gameStateManager.initializeDefaultSeals()

        // CANLİ VERİ SENKRONIZASYONU: GameState'i sürekli dinle
        observeGameStateChanges()
    }

    /**
     * YENI: GameState'i sürekli dinle - Kalibrasyon değişiklikleri anında yansısın!
     *
     * GameState'teki seals listesi her değiştiğinde (kalibrasyon, mastery update vb.)
     * bu fonksiyon otomatik olarak tetiklenir ve UI güncellenir.
     */
    private fun observeGameStateChanges() {
        viewModelScope.launch {
            gameStateManager.gameState.collect { gameState ->
                val playerLevel = gameState.playerState.level
                val unlockedSeals = gameState.unlockedSeals
                val masteredSealIds = unlockedSeals.filter { it.masteryLevel >= 50 }.map { it.id }

                com.example.isekaikuroshin.utils.GameLogger.logSealPractice("🔄 GameState changed! Reloading available seals...")
                com.example.isekaikuroshin.utils.GameLogger.logSealPractice("Total unlocked seals: ${unlockedSeals.size}, Mastered: ${masteredSealIds.size}")

                // Filter seals based on player level and mastery
                val available = unlockedSeals.filter { seal ->
                    seal.isUnlockedFor(playerLevel, masteredSealIds)
                }

                com.example.isekaikuroshin.utils.GameLogger.logSealPractice("Available seals for practice: ${available.size}")
                available.forEach { seal ->
                    com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  • ${seal.nameKey} (Mastery: ${seal.masteryLevel}%, Calibrations: ${seal.templateLandmarksList.size})")
                }

                _uiState.update { it.copy(availableSeals = available) }
            }
        }
    }

    /**
     * DEPRECATED: Artık sürekli dinleme kullanıyoruz, manuel reload gereksiz
     */
    @Deprecated("Use observeGameStateChanges() for real-time sync")
    fun loadAvailableSeals() {
        // Bu fonksiyon artık kullanılmıyor - observeGameStateChanges() otomatik güncelliyor
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("⚠️ loadAvailableSeals() called - but using Flow-based sync now")
    }

    /**
     * Start practice session for a seal
     *
     * YENI: Session sırasında seal'ı canlı takip eder - kalibrasyon güncellemeleri anında yansır!
     */
    fun startPracticeSession(seal: Seal) {
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("========================================")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("PRACTICE SESSION STARTED: ${seal.nameKey}")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Difficulty: ${seal.difficulty}")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Tier: ${seal.tier}")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Current Mastery: ${seal.masteryLevel}%")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Current Calibrations: ${seal.templateLandmarksList.size}")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("========================================")

        // ⚠️ KRİTİK: GestureEngine state'ini temizle (bayat durum önleme)
        gestureEngine.resetState()
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("🔄 GestureEngine state reset for new session")

        _uiState.update {
            it.copy(
                selectedSeal = seal,
                sessionActive = true,
                sessionStartTime = System.currentTimeMillis(),
                attemptCount = 0,
                successCount = 0,
                currentFeedback = "Elinizi kameraya gösterin ve mührü taklit edin",
                showSuccessAnimation = false,
                showMasteryDialog = false
            )
        }

        // Reset activity tracker
        lastActivityTime = System.currentTimeMillis()

        // Start session timeout monitoring (battery optimization)
        startSessionTimeoutMonitoring()

        // YENI: Seçili seal'ı canlı takip et (kalibrasyon güncellemeleri için)
        observeSelectedSealChanges(seal.id)
    }

    /**
     * YENI: Seçili seal'ı canlı takip et
     *
     * Kullanıcı pratik yaparken başka bir sekmede kalibrasyon eklerse,
     * bu değişiklik anında pratik ekranına yansır!
     */
    private fun observeSelectedSealChanges(sealId: String) {
        viewModelScope.launch {
            gameStateManager.gameState.collect { gameState ->
                // Session aktif mi kontrol et
                if (!_uiState.value.sessionActive) return@collect

                // Güncel seal'ı bul
                val updatedSeal = gameState.unlockedSeals.find { it.id == sealId }

                if (updatedSeal != null) {
                    val oldCalibrationCount = _uiState.value.selectedSeal?.templateLandmarksList?.size ?: 0
                    val newCalibrationCount = updatedSeal.templateLandmarksList.size

                    // Kalibrasyon sayısı değiştiyse logla
                    if (newCalibrationCount != oldCalibrationCount) {
                        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("🔄 SEAL UPDATED LIVE! ${updatedSeal.nameKey}: Calibrations $oldCalibrationCount → $newCalibrationCount")
                    }

                    // UI'yi güncelle
                    _uiState.update { it.copy(selectedSeal = updatedSeal) }
                }
            }
        }
    }

    /**
     * Monitor session duration and idle time (Battery Optimization)
     */
    private fun startSessionTimeoutMonitoring() {
        sessionTimeoutJob?.cancel()
        sessionTimeoutJob = viewModelScope.launch {
            while (_uiState.value.sessionActive) {
                kotlinx.coroutines.delay(10000)  // Her 10 saniyede bir kontrol

                val currentTime = System.currentTimeMillis()
                val sessionDuration = currentTime - _uiState.value.sessionStartTime
                val idleTime = currentTime - lastActivityTime

                // Max session duration check
                if (sessionDuration >= MAX_SESSION_DURATION_MS) {
                    endSession()
                    _uiState.update {
                        it.copy(
                            currentFeedback = "⚠️ Maksimum oturum süresi (10 dk) doldu. Batarya tasarrufu için oturum sonlandırıldı."
                        )
                    }
                    break
                }

                // Idle timeout check
                if (idleTime >= IDLE_TIMEOUT_MS) {
                    endSession()
                    _uiState.update {
                        it.copy(
                            currentFeedback = "⚠️ 2 dakika boyunca aktivite tespit edilmedi. Batarya tasarrufu için oturum sonlandırıldı."
                        )
                    }
                    break
                }
            }
        }
    }

    /**
     * Process hand detection result
     *
     * Called from CameraPreview when hand landmarks are detected
     *
     * ⚠️ KRİTİK: Tracking loss detection eklendi - El kaybedilip yeniden
     * tespit edildiğinde GestureEngine'e bilgi verir (smoothing buffer reset)
     */
    fun processHandDetection(result: HandLandmarkerResult) {
        android.util.Log.d("SEAL_PRACTICE_VM", "===== processHandDetection CALLED =====")

        val seal = _uiState.value.selectedSeal
        android.util.Log.d("SEAL_PRACTICE_VM", "Selected seal: ${seal?.nameKey ?: "NULL"}")

        if (seal == null) {
            android.util.Log.e("SEAL_PRACTICE_VM", "❌ No seal selected! Returning early.")
            return
        }

        val sessionActive = _uiState.value.sessionActive
        android.util.Log.d("SEAL_PRACTICE_VM", "Session active: $sessionActive")

        if (!sessionActive) {
            android.util.Log.e("SEAL_PRACTICE_VM", "❌ Session not active! Returning early.")
            return
        }

        // Update activity time (idle detection için)
        lastActivityTime = System.currentTimeMillis()

        // Get first hand (single hand for now, future: support dual hands)
        val allLandmarks = result.landmarks()
        android.util.Log.d("SEAL_PRACTICE_VM", "Total hands detected: ${allLandmarks.size}")

        val handLandmarks = allLandmarks.firstOrNull()

        // ⚠️ KRİTİK: Tracking loss algılama (RE-DETECTION stability)
        if (handLandmarks == null) {
            android.util.Log.w("SEAL_PRACTICE_VM", "⚠️ No hand landmarks found! MediaPipe detected 0 hands.")
            android.util.Log.w("SEAL_PRACTICE_VM", "🔄 Signaling tracking loss to GestureEngine...")
            gestureEngine.onTrackingLost()
            return
        }

        android.util.Log.d("SEAL_PRACTICE_VM", "✅ Hand landmarks found: ${handLandmarks.size} points")
        android.util.Log.d("SEAL_PRACTICE_VM", "Calling gestureEngine.evaluateGesture()...")

        // Convert NormalizedLandmark to NormalizedPoint
        val normalizedPoints = handLandmarks.map { landmark ->
            NormalizedPoint(
                x = landmark.x(),
                y = landmark.y(),
                z = landmark.z()
            )
        }

        viewModelScope.launch {
            val gestureResult = gestureEngine.evaluateGesture(
                normalizedPoints,
                seal
            )

            android.util.Log.d("SEAL_PRACTICE_VM", "✅ gestureEngine.evaluateGesture() returned!")
            android.util.Log.d("SEAL_PRACTICE_VM", "Result accuracy: ${(gestureResult.accuracy * 100).toInt()}%")

            handleGestureResult(gestureResult)
        }
    }

    /**
     * Handle gesture evaluation result
     *
     * Updates UI state and game state based on gesture quality
     */
    private suspend fun handleGestureResult(result: GestureResult) {
        val currentState = _uiState.value
        val seal = currentState.selectedSeal ?: return

        // Update UI state with REAL-TIME accuracy (her frame güncellenir)
        _uiState.update {
            it.copy(
                attemptCount = it.attemptCount + 1,
                successCount = if (result.isSuccess) it.successCount + 1 else it.successCount,
                lastAccuracy = result.accuracy,
                currentAccuracy = result.accuracy,  // Canlı doğruluk skoru
                currentFeedback = result.feedback,
                lastPerformanceLevel = result.performanceLevel,
                showSuccessAnimation = result.performanceLevel == PerformanceLevel.PERFECT,
                bestAccuracyThisSession = maxOf(it.bestAccuracyThisSession, result.accuracy),
                normalizedHandLandmarks = result.debugInfo.liveNormalizedLandmarks  // ⚡ YENİ: Normalize veri overlay için
            )
        }

        // Update seal mastery in game state
        if (result.isSuccess) {
            val masteryGain = when (result.performanceLevel) {
                PerformanceLevel.PERFECT -> 5
                PerformanceLevel.GOOD -> 3
                PerformanceLevel.ACCEPTABLE -> 1
                else -> 0
            }

            // Update mastery
            gameStateManager.updateSealMastery(seal.id, masteryGain)

            // Record practice attempt
            gameStateManager.recordSealPracticeAttempt(
                sealId = seal.id,
                wasSuccessful = true,
                wasPerfect = result.performanceLevel == PerformanceLevel.PERFECT,
                accuracy = result.accuracy
            )

            // Check if seal is mastered (100%)
            val updatedSeal = gameStateManager.getSealById(seal.id)
            if (updatedSeal != null && updatedSeal.masteryLevel >= 100 && seal.masteryLevel < 100) {
                unlockRelatedSkills(updatedSeal)
            }
        } else {
            // Record failed attempt
            gameStateManager.recordSealPracticeAttempt(
                sealId = seal.id,
                wasSuccessful = false,
                wasPerfect = false,
                accuracy = result.accuracy
            )
        }

        // Reset success animation after delay
        if (result.performanceLevel == PerformanceLevel.PERFECT) {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(showSuccessAnimation = false) }
        }
    }

    /**
     * Unlock skills related to mastered seal
     */
    private suspend fun unlockRelatedSkills(seal: Seal) {
        val unlockedSkillNames = mutableListOf<String>()

        // Her related skill ID için skill'i unlock et
        seal.relatedSkillIds.forEach { skillId ->
            try {
                // Skill'i unlock et (GameStateManager'da implementasyon gerekli)
                val skill = gameStateManager.getSkillById(skillId)
                if (skill != null) {
                    gameStateManager.unlockSkill(skillId)
                    unlockedSkillNames.add(skill.name)
                }
            } catch (e: Exception) {
                // Skill bulunamadı veya unlock edilemedi
                android.util.Log.e("SealPracticeVM", "Skill unlock error for $skillId: ${e.message}")
            }
        }

        // Seal mastery badge ver
        val badge = com.example.isekaikuroshin.data.SealMasteryBadges.getBadgeForMasteredSeal(seal.id)
        var badgeText = ""
        if (badge != null) {
            try {
                gameStateManager.grantBadge(badge.id)
                badgeText = "\n\n🏆 Rozet Kazanıldı: ${badge.name}"
            } catch (e: Exception) {
                android.util.Log.e("SealPracticeVM", "Badge add error: ${e.message}")
            }
        }

        // Kullanıcıya bildirim göster
        val skillsText = if (unlockedSkillNames.isNotEmpty()) {
            "\n\nAçılan Yetenekler:\n${unlockedSkillNames.joinToString("\n") { "• $it" }}"
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                showMasteryDialog = true,
                masteryMessage = "🎉 ${seal.nameKey} mührünü tamamen ustalandınız!$skillsText$badgeText"
            )
        }
    }

    /**
     * Dismiss mastery dialog
     */
    fun dismissMasteryDialog() {
        _uiState.update { it.copy(showMasteryDialog = false) }
    }

    /**
     * End practice session
     */
    fun endSession() {
        val state = _uiState.value

        // Cancel timeout monitoring
        sessionTimeoutJob?.cancel()

        // Calculate session stats
        val sessionDuration = (System.currentTimeMillis() - state.sessionStartTime) / 1000 / 60  // minutes
        val successRate = if (state.attemptCount > 0) {
            (state.successCount.toFloat() / state.attemptCount.toFloat() * 100).toInt()
        } else 0

        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("========================================")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("PRACTICE SESSION ENDED: ${state.selectedSeal?.nameKey ?: "Unknown"}")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Duration: ${sessionDuration.toInt()} minutes")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Attempts: ${state.attemptCount}")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Successes: ${state.successCount}")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Success Rate: $successRate%")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("  Best Accuracy: ${(state.bestAccuracyThisSession * 100).toInt()}%")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("========================================")

        // G83: Log seal practice session to Journal
        if (state.attemptCount > 0) {
            com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                "I practiced the ${state.selectedSeal?.nameKey ?: "Unknown"} seal for ${sessionDuration.toInt()} minutes " +
                "with $successRate% success rate (${state.successCount}/${state.attemptCount} attempts)"
            )
        }

        _uiState.update {
            it.copy(
                sessionActive = false,
                sessionSummary = SessionSummary(
                    duration = sessionDuration.toInt(),
                    attempts = state.attemptCount,
                    successes = state.successCount,
                    successRate = successRate,
                    bestAccuracy = state.bestAccuracyThisSession
                )
            )
        }
    }

    /**
     * Reset session (start new practice)
     */
    fun resetSession() {
        _uiState.update {
            it.copy(
                sessionActive = false,
                selectedSeal = null,
                sessionSummary = null,
                attemptCount = 0,
                successCount = 0,
                lastAccuracy = 0f,
                bestAccuracyThisSession = 0f,
                currentFeedback = "",
                showSuccessAnimation = false,
                showMasteryDialog = false
            )
        }
    }

    /**
     * ⚠️ KRİTİK: ViewModel temizleme (Bayat Durum Önleme)
     *
     * Bu fonksiyon, kullanıcı ekrandan ayrıldığında otomatik olarak çağrılır.
     * Tüm aktif session'ları sonlandırır ve GestureEngine state'ini temizler.
     *
     * SEBEP:
     * - Kullanıcı farklı bir mühre geçtiğinde veya ekrandan çıktığında,
     *   önceki session'dan kalan veriler bir sonraki session'a sızmasın.
     * - GestureRecognitionEngine Singleton olduğu için, her ViewModel temizlenirken
     *   engine state'ini de reset etmeliyiz.
     */
    override fun onCleared() {
        super.onCleared()

        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("========================================")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("🔄 SealPracticeViewModel.onCleared() - Cleanup starting")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("========================================")

        // Cancel all active coroutines
        sessionTimeoutJob?.cancel()
        sessionTimeoutJob = null

        // Reset GestureEngine state (Singleton cleanup)
        gestureEngine.resetState()

        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("✅ SealPracticeViewModel cleanup complete")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("   • Session timeout job cancelled")
        com.example.isekaikuroshin.utils.GameLogger.logSealPractice("   • GestureEngine state reset")
    }
}

/**
 * UI State for Seal Practice Screen
 */
data class SealPracticeUiState(
    val availableSeals: List<Seal> = emptyList(),
    val selectedSeal: Seal? = null,
    val sessionActive: Boolean = false,
    val sessionStartTime: Long = 0,

    // Practice metrics
    val attemptCount: Int = 0,
    val successCount: Int = 0,
    val lastAccuracy: Float = 0f,
    val bestAccuracyThisSession: Float = 0f,
    val currentAccuracy: Float = 0f,  // Canlı doğruluk skoru (her frame güncellenir)

    // Feedback
    val currentFeedback: String = "",
    val lastPerformanceLevel: PerformanceLevel? = null,
    val showSuccessAnimation: Boolean = false,

    // ⚡ YENİ: Debug overlay için normalize veri
    val normalizedHandLandmarks: List<com.example.isekaikuroshin.data.NormalizedPoint> = emptyList(),

    // Mastery
    val showMasteryDialog: Boolean = false,
    val masteryMessage: String = "",

    // Session summary
    val sessionSummary: SessionSummary? = null
)

/**
 * Session summary after practice ends
 */
data class SessionSummary(
    val duration: Int,  // minutes
    val attempts: Int,
    val successes: Int,
    val successRate: Int,  // %
    val bestAccuracy: Float
)
