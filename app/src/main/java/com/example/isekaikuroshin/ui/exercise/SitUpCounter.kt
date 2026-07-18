package com.example.isekaikuroshin.ui.exercise

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Mekik (Sit-up) Sayma Algoritması - v2.0
 *
 * Bu sınıf, PushUpCounter ile aynı gelişmiş teknikleri kullanarak
 * mekik hareketlerini doğru şekilde sayar ve form kontrolü yapar.
 *
 * YENİ ÖZELLIKLER (v2.0):
 * - OneEuroFilter: Industry-standard VR/AR filtering (jitter reduction)
 * - MovementQualityAnalyzer: Velocity/acceleration validation (momentum detection)
 * - 3-Layer Validation: Form + Temporal + Movement
 *
 * Araştırma Kaynakları:
 * - MediaPipe Pose Documentation (Google AI)
 * - ML Kit Pose Detection Best Practices
 * - LearnOpenCV Fitness Trainer Tutorials
 * - GitHub: GYM-pose-estimation-using-mediapipe
 * - "1€ Filter: A Simple Speed-based Low-pass Filter" (CHI 2012)
 *
 * Teknik Özellikler:
 * - Hysteresis Thresholding (zıplama önleme)
 * - OneEuroFilter Smoothing (adaptive filtering)
 * - Temporal Validation (zaman bazlı doğrulama)
 * - Velocity/Acceleration Analysis (momentum detection)
 * - Hip Stability Check (kalça stabilite kontrolü)
 * - Peak-Hold Logic (tepe noktası bekleme)
 */

/**
 * Mekik Sonucu
 */
data class SitUpResult(
    val count: Int,
    val feedback: String,
    val formQuality: FormQuality,
    val torsoAngle: Float = 0f,
    val state: ExerciseState = ExerciseState.READY,
    val hipStability: Float = 0f  // Kalça stabilite skoru (0-100)
)

/**
 * Kalça Stabilite Kontrolü
 */
data class HipStability(
    val isStable: Boolean,
    val score: Float,  // 0-100 arası skor
    val message: String
)

/**
 * Kritik Landmark Seti (Mekik için)
 */
data class SitUpLandmarkSet(
    val leftShoulder: PoseLandmark,
    val rightShoulder: PoseLandmark,
    val leftHip: PoseLandmark,
    val rightHip: PoseLandmark,
    val leftKnee: PoseLandmark,
    val rightKnee: PoseLandmark,
    val leftAnkle: PoseLandmark?,
    val rightAnkle: PoseLandmark?
)

class SitUpCounter {

    // Durum takibi
    private var state = ExerciseState.READY
    private var sitUpCount = 0
    private var feedback = "Mekik yapmaya hazırlanın"
    private var formQuality = FormQuality.NEUTRAL

    // ═══════════════════════════════════════════════════════════════════
    // YENİ v2.0: ONE EURO FILTER (OPTIMIZED - Sadece gerekli eklemler)
    // ═══════════════════════════════════════════════════════════════════
    // OPTIMIZATION: Sadece omuz için filter (torso açısı hesabında kritik)
    // Kalça ve diz zaten yerde, daha az noise var
    private val leftShoulderFilter = OneEuroFilter(minCutoff = 1.5f, beta = 0.01f)
    private val rightShoulderFilter = OneEuroFilter(minCutoff = 1.5f, beta = 0.01f)

    // ═══════════════════════════════════════════════════════════════════
    // YENİ v2.0: MOVEMENT QUALITY ANALYZER (Momentum Detection)
    // ═══════════════════════════════════════════════════════════════════
    // Omuz hareketini analiz et (torso'nun yukarı/aşağı hareketi için)
    private val movementAnalyzer = MovementQualityAnalyzer()

    // BEST PRACTICE #1: Hysteresis Thresholds (Zıplama önleme)
    // DOWN (sırt yerde): 140°-160° arası
    // UP (oturma pozisyonu): 60°-80° arası
    private val torsoDownHysteresis = HysteresisThreshold(
        lowThreshold = 140f,   // Bu açıdan küçükse DOWN değil
        highThreshold = 160f   // Bu açıdan büyükse kesinlikle DOWN
    )

    private val torsoUpHysteresis = HysteresisThreshold(
        lowThreshold = 60f,    // Bu açıdan küçükse kesinlikle UP
        highThreshold = 80f    // Bu açıdan büyükse UP değil
    )

    // BEST PRACTICE #2: Exponential Moving Average (EMA) Smoothing
    // Not: OneEuroFilter eklediğimiz için EMA'yı daha hafif kullanıyoruz
    private val torsoAngleFilter = SmoothingFilter(windowSize = 3, alpha = 0.5f)

    // BEST PRACTICE #3: Temporal Validation (Zaman bazlı doğrulama)
    // Mekik hareketleri genellikle şınavdan biraz daha yavaştır
    private val upPhaseValidator = MovementValidator(
        minDurationMs = 400,   // Minimum 400ms (çok hızlı sarsıntıları önler)
        maxDurationMs = 4000   // Maksimum 4s (statik pozları önler)
    )

    private val downPhaseValidator = MovementValidator(
        minDurationMs = 400,
        maxDurationMs = 4000
    )

    // BEST PRACTICE #4: Peak-Hold Logic
    // Tepe ve dip noktalarında kısa süre bekleme zorunluluğu
    private val peakHoldDurationMs = 200L
    private var peakHoldStartTime = 0L
    private var isHoldingPeak = false

    // Kalça pozisyonu takibi (Hip Stability)
    private var initialHipPosition: PointF? = null
    private val maxHipMovement = 150f  // Kalça max 150 piksel hareket edebilir

    // Form validation
    private var consecutiveInvalidFrames = 0
    private val maxInvalidFrames = 10

    // State değişim takibi
    private var lastStateChangeTime = System.currentTimeMillis()

    companion object {
        private const val TAG = "SitUpCounter"

        // Landmark confidence thresholds
        private const val CRITICAL_LANDMARK_CONFIDENCE = 0.7f
        private const val SECONDARY_LANDMARK_CONFIDENCE = 0.5f
    }

    /**
     * Pose'dan kritik landmark'ları çıkar (Mekik için)
     *
     * Kritik eklemler: Omuz, Kalça, Diz (torso açısı hesabı için)
     * Opsiyonel eklemler: Ayak bileği (stabilite kontrolü için)
     */
    private fun extractLandmarks(pose: Pose): SitUpLandmarkSet? {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        // Kritik landmark'lar eksikse veya güven düşükse null döndür
        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null) {
            return null
        }

        // BEST PRACTICE: Confidence-based filtering
        if (leftShoulder.inFrameLikelihood < CRITICAL_LANDMARK_CONFIDENCE ||
            rightShoulder.inFrameLikelihood < CRITICAL_LANDMARK_CONFIDENCE ||
            leftHip.inFrameLikelihood < CRITICAL_LANDMARK_CONFIDENCE ||
            rightHip.inFrameLikelihood < CRITICAL_LANDMARK_CONFIDENCE ||
            leftKnee.inFrameLikelihood < CRITICAL_LANDMARK_CONFIDENCE ||
            rightKnee.inFrameLikelihood < CRITICAL_LANDMARK_CONFIDENCE) {
            return null
        }

        // Ayak bileği opsiyonel (düşük confidence kabul edilir)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.takeIf {
            it.inFrameLikelihood >= SECONDARY_LANDMARK_CONFIDENCE
        }
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.takeIf {
            it.inFrameLikelihood >= SECONDARY_LANDMARK_CONFIDENCE
        }

        return SitUpLandmarkSet(
            leftShoulder, rightShoulder,
            leftHip, rightHip,
            leftKnee, rightKnee,
            leftAnkle, rightAnkle
        )
    }

    /**
     * Torso (Gövde) Açısını Hesapla
     *
     * Omuz-Kalça-Diz arasındaki açı
     * - Sırt yerde: ~180° (düz uzanmış)
     * - Oturma pozisyonu: ~60-80° (bükülü)
     */
    private fun calculateTorsoAngle(landmarks: SitUpLandmarkSet): Float {
        // Sol taraf kullan (daha stabil genellikle)
        val shoulder = landmarks.leftShoulder.position
        val hip = landmarks.leftHip.position
        val knee = landmarks.leftKnee.position

        return calculateAngle(shoulder, hip, knee)
    }

    /**
     * BEST PRACTICE: Hip Stability Check
     *
     * Mekik sırasında kalça pozisyonu büyük ölçüde sabit kalmalıdır.
     * Kalça çok hareket ediyorsa, form bozuk demektir.
     *
     * Araştırma: ML Kit Pose Detection - "Z coordinate helps determine
     * whether parts of body are in front or behind hips"
     */
    private fun checkHipStability(landmarks: SitUpLandmarkSet): HipStability {
        val currentHip = PointF(
            (landmarks.leftHip.position.x + landmarks.rightHip.position.x) / 2f,
            (landmarks.leftHip.position.y + landmarks.rightHip.position.y) / 2f
        )

        // İlk pozisyonu kaydet
        if (initialHipPosition == null) {
            initialHipPosition = currentHip
            return HipStability(true, 100f, "Başlangıç pozisyonu")
        }

        // Kalça hareketini hesapla
        val hipMovement = calculateDistance(
            initialHipPosition!!,
            currentHip
        )

        // Stabilite skorunu hesapla (0-100)
        val stabilityScore = ((1f - (hipMovement / maxHipMovement).coerceIn(0f, 1f)) * 100f)

        val isStable = hipMovement < maxHipMovement
        val message = when {
            stabilityScore >= 80f -> "Kalça stabil ✓"
            stabilityScore >= 60f -> "Kalça hafif hareket ediyor"
            stabilityScore >= 40f -> "Kalça fazla hareket ediyor!"
            else -> "Kalça çok dengesiz!"
        }

        return HipStability(isStable, stabilityScore, message)
    }

    /**
     * BEST PRACTICE: Peak-Hold Logic
     *
     * Tepe noktalarında (UP ve DOWN pozisyonlarında) kısa bir süre
     * beklemesi gerekir. Bu, yarım hareketleri ve sarsıntıları önler.
     *
     * Araştırma: "Conditions tracked for 24 continuous frames to avoid
     * false positives" (LearnOpenCV)
     */
    private fun checkPeakHold(): Boolean {
        if (!isHoldingPeak) {
            // Tepe noktasına yeni ulaştık
            peakHoldStartTime = System.currentTimeMillis()
            isHoldingPeak = true
            return false  // Henüz yeterince beklemedik
        }

        val elapsed = System.currentTimeMillis() - peakHoldStartTime
        return elapsed >= peakHoldDurationMs
    }

    /**
     * Peak-hold'u sıfırla (state değişimi sonrası)
     */
    private fun resetPeakHold() {
        isHoldingPeak = false
        peakHoldStartTime = 0L
    }

    /**
     * Ana İşleme Fonksiyonu - v2.0
     */
    fun processPose(pose: Pose): SitUpResult {
        val previousState = state

        // STEP 1: Landmark'ları çıkar
        val landmarks = extractLandmarks(pose)

        if (landmarks == null) {
            feedback = "⚠️ Tam görünün (omuz, kalça, diz)"
            formQuality = FormQuality.INVALID
            consecutiveInvalidFrames++

            if (consecutiveInvalidFrames > maxInvalidFrames) {
                state = ExerciseState.INVALID
            }

            return SitUpResult(
                count = sitUpCount,
                feedback = feedback,
                formQuality = formQuality,
                state = state
            )
        }

        // Landmark'lar geçerli - invalid frame counter'ı sıfırla
        consecutiveInvalidFrames = 0

        // ═══════════════════════════════════════════════════════════════════
        // STEP 1.5: ONE EURO FILTER APPLICATION (OPTIMIZED v2.0!)
        // ═══════════════════════════════════════════════════════════════════
        val timestamp = System.currentTimeMillis()

        // Sadece omuz eklemlerini filtrele (performance optimization)
        val filteredLeftShoulder = leftShoulderFilter.filter(landmarks.leftShoulder.position, timestamp)
        val filteredRightShoulder = rightShoulderFilter.filter(landmarks.rightShoulder.position, timestamp)

        // STEP 2: Torso açısını hesapla (omuz filtered, kalça/diz raw)
        val rawTorsoAngle = calculateAngle(
            filteredLeftShoulder,
            landmarks.leftHip.position,  // Raw kalça (zaten yerde, stabil)
            landmarks.leftKnee.position  // Raw diz (zaten yerde, stabil)
        )
        val smoothTorsoAngle = torsoAngleFilter.addAngle(rawTorsoAngle)

        // ═══════════════════════════════════════════════════════════════════
        // STEP 2.5: VELOCITY ANALYSIS (YENİ v2.0!)
        // ═══════════════════════════════════════════════════════════════════
        // Omuz hareketini analiz et (mekikte gövde hareketi için ana gösterge)
        val avgShoulderPosition = PointF(
            (filteredLeftShoulder.x + filteredRightShoulder.x) / 2f,
            (filteredLeftShoulder.y + filteredRightShoulder.y) / 2f
        )

        val movementQuality = movementAnalyzer.analyzeMovement(avgShoulderPosition, timestamp)

        // Movement validation (momentum kontrolü)
        val isMovementValid = !movementQuality.isTooFast && movementQuality.qualityScore >= 50f

        // STEP 3: Hip Stability Check
        val hipStability = checkHipStability(landmarks)
        val isFormValid = hipStability.isStable

        // STEP 4: State Machine ile durum yönetimi
        when (state) {
            ExerciseState.READY -> {
                // Başlangıç: Kullanıcı sırt üstü yatmalı
                val isDown = torsoDownHysteresis.check(smoothTorsoAngle, highMeansActive = true)

                if (isDown && isFormValid) {
                    state = ExerciseState.DOWN
                    downPhaseValidator.startPhase()
                    feedback = "✅ Hazır! Şimdi yukarı doğru kalkın"
                    formQuality = FormQuality.GOOD

                    Log.d(TAG, "READY → DOWN (Açı: ${smoothTorsoAngle.toInt()}°)")
                } else {
                    feedback = if (!isFormValid) {
                        "⚠️ ${hipStability.message}"
                    } else {
                        "Sırt üstü uzanın"
                    }
                    formQuality = FormQuality.NEUTRAL
                }
            }

            ExerciseState.DOWN -> {
                // Aşağı pozisyon (sırt yerde): Kullanıcı oturma pozisyonuna geçmeli
                val isUp = torsoUpHysteresis.check(smoothTorsoAngle, highMeansActive = false)

                if (isUp) {
                    // BEST PRACTICE: Peak-Hold Logic - UP pozisyonunda biraz bekle
                    if (checkPeakHold()) {
                        // ═══════════════════════════════════════════════════════════
                        // 3-LAYER VALIDATION (YENİ v2.0!)
                        // Layer 1: Temporal (timing)
                        // Layer 2: Form (hip stability)
                        // Layer 3: Movement (velocity/acceleration)
                        // ═══════════════════════════════════════════════════════════
                        val isTemporalValid = downPhaseValidator.isValidMovement()

                        if (isTemporalValid && isFormValid && isMovementValid) {
                            state = ExerciseState.UP
                            upPhaseValidator.startPhase()
                            resetPeakHold()

                            feedback = "👍 İyi! Şimdi aşağı inin"
                            formQuality = FormQuality.GOOD

                            Log.d(TAG, "DOWN → UP (Açı: ${smoothTorsoAngle.toInt()}°, " +
                                    "Süre: ${downPhaseValidator.getElapsedTime()}ms, " +
                                    "Velocity: ${movementQuality.velocity.toInt()} px/s)")
                        } else {
                            // Hareket geçersiz
                            state = ExerciseState.TRANSITION
                            resetPeakHold()
                            feedback = when {
                                !isFormValid -> "⚠️ ${hipStability.message}"
                                !isTemporalValid -> "⚠️ Hareket çok ${if (downPhaseValidator.getElapsedTime() < 400) "hızlı" else "yavaş"}"
                                !isMovementValid -> movementQuality.feedback  // "⚠️ Daha Yavaş Hareket Et"
                                else -> "⚠️ Form hatalı"
                            }
                            formQuality = FormQuality.POOR

                            Log.d(TAG, "DOWN → TRANSITION (Geçersiz - Form: $isFormValid, " +
                                    "Temporal: $isTemporalValid, Movement: $isMovementValid)")
                        }
                    } else {
                        feedback = "⏸️ Yukarıda biraz bekleyin..."
                        formQuality = FormQuality.NEUTRAL
                    }
                } else {
                    resetPeakHold()  // Henüz yukarı çıkmadı, hold'u sıfırla
                    feedback = "⬆️ Yukarı doğru kalkın"
                    formQuality = FormQuality.NEUTRAL
                }
            }

            ExerciseState.UP -> {
                // Yukarı pozisyon (oturma): Kullanıcı tekrar aşağı inmeli
                val isDown = torsoDownHysteresis.check(smoothTorsoAngle, highMeansActive = true)

                if (isDown) {
                    // BEST PRACTICE: Peak-Hold Logic - DOWN pozisyonunda biraz bekle
                    if (checkPeakHold()) {
                        // ═══════════════════════════════════════════════════════════
                        // 3-LAYER VALIDATION (YENİ v2.0!)
                        // ═══════════════════════════════════════════════════════════
                        val isTemporalValid = upPhaseValidator.isValidMovement()

                        if (isTemporalValid && isFormValid && isMovementValid) {
                            // ✅ GEÇERLİ MEKİK TAMAMLANDI!
                            state = ExerciseState.DOWN
                            sitUpCount++
                            downPhaseValidator.startPhase()
                            resetPeakHold()

                            feedback = "🎉 Harika! Mekik: $sitUpCount"
                            formQuality = FormQuality.EXCELLENT

                            Log.d(TAG, "✅ UP → DOWN | MEKİK SAYILDI! #$sitUpCount " +
                                    "(Açı: ${smoothTorsoAngle.toInt()}°, " +
                                    "Süre: ${upPhaseValidator.getElapsedTime()}ms, " +
                                    "Stabilite: ${hipStability.score.toInt()}%, " +
                                    "Velocity: ${movementQuality.velocity.toInt()} px/s)")
                        } else {
                            // Form kötü veya hareket geçersiz
                            state = ExerciseState.TRANSITION
                            resetPeakHold()
                            feedback = when {
                                !isFormValid -> "❌ Form hatalı - tekrar sayılmadı"
                                !isTemporalValid -> "❌ Hareket çok ${if (upPhaseValidator.getElapsedTime() < 400) "hızlı" else "yavaş"}"
                                !isMovementValid -> movementQuality.feedback
                                else -> "❌ Geçersiz tekrar"
                            }
                            formQuality = FormQuality.POOR

                            Log.d(TAG, "UP → TRANSITION (Form: $isFormValid, " +
                                    "Temporal: $isTemporalValid, Movement: $isMovementValid, " +
                                    "Süre: ${upPhaseValidator.getElapsedTime()}ms)")
                        }
                    } else {
                        feedback = "⏸️ Aşağıda biraz bekleyin..."
                        formQuality = FormQuality.NEUTRAL
                    }
                } else {
                    resetPeakHold()  // Henüz aşağı inmedi, hold'u sıfırla
                    feedback = "⬇️ Aşağı inin"
                    formQuality = FormQuality.NEUTRAL
                }
            }

            ExerciseState.TRANSITION -> {
                // Geçiş durumu: Form düzelene kadar bekle
                val isDown = torsoDownHysteresis.check(smoothTorsoAngle, highMeansActive = true)

                if (isDown && isFormValid && isMovementValid) {
                    state = ExerciseState.DOWN
                    downPhaseValidator.startPhase()
                    resetPeakHold()
                    feedback = "✅ Form düzeltildi! Devam edin"
                    formQuality = FormQuality.GOOD
                } else {
                    feedback = "⏸️ Doğru forma geçin"
                    formQuality = FormQuality.NEUTRAL
                }
            }

            ExerciseState.INVALID -> {
                // Geçersiz form: Kullanıcı formu düzeltmeli
                val isDown = torsoDownHysteresis.check(smoothTorsoAngle, highMeansActive = true)

                if (isDown && isFormValid && isMovementValid) {
                    state = ExerciseState.DOWN
                    downPhaseValidator.startPhase()
                    resetPeakHold()
                    consecutiveInvalidFrames = 0
                    feedback = "✅ Form düzeltildi! Devam edin"
                    formQuality = FormQuality.GOOD

                    Log.d(TAG, "INVALID → DOWN (Form düzeltildi)")
                } else {
                    feedback = "❌ ${hipStability.message}"
                    formQuality = FormQuality.INVALID
                }
            }
        }

        // State değişimi logla
        if (state != previousState) {
            lastStateChangeTime = System.currentTimeMillis()
        }

        return SitUpResult(
            count = sitUpCount,
            feedback = feedback,
            formQuality = formQuality,
            torsoAngle = smoothTorsoAngle,
            state = state,
            hipStability = hipStability.score
        )
    }

    /**
     * Sayaçları ve filtreleri sıfırla
     */
    fun reset() {
        state = ExerciseState.READY
        sitUpCount = 0
        feedback = "Mekik yapmaya hazırlanın"
        formQuality = FormQuality.NEUTRAL

        // Hysteresis ve EMA sıfırla
        torsoDownHysteresis.reset()
        torsoUpHysteresis.reset()
        torsoAngleFilter.reset()
        upPhaseValidator.reset()
        downPhaseValidator.reset()

        // ═══ YENİ v2.0: OneEuroFilter ve MovementAnalyzer sıfırla (OPTIMIZED) ═══
        leftShoulderFilter.reset()
        rightShoulderFilter.reset()
        movementAnalyzer.reset()

        initialHipPosition = null
        consecutiveInvalidFrames = 0
        resetPeakHold()

        Log.d(TAG, "🔄 SitUpCounter v2.0 sıfırlandı")
    }

    fun getCount() = sitUpCount
    fun getState() = state
    fun getFeedback() = feedback
    fun getFormQuality() = formQuality
}
