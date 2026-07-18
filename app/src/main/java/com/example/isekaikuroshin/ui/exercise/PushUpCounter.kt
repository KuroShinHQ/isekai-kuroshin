package com.example.isekaikuroshin.ui.exercise

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Egzersiz Durumları (State Machine için)
 */
enum class ExerciseState {
    READY,      // Başlangıç pozisyonu
    DOWN,       // Aşağıda (göğüs yerde - şınav için)
    UP,         // Yukarıda (kollar düz)
    TRANSITION, // Geçiş durumu (ara pozisyon)
    INVALID     // Hatalı form
}

/**
 * Form Kalitesi Değerlendirmesi
 */
enum class FormQuality {
    EXCELLENT,  // Mükemmel form
    GOOD,       // İyi form
    NEUTRAL,    // Nötr (ara pozisyon)
    POOR,       // Zayıf form
    INVALID     // Geçersiz
}

/**
 * Şınav Sayma Sonucu
 */
data class PushUpResult(
    val count: Int,
    val feedback: String,
    val formQuality: FormQuality,
    val elbowAngle: Float = 0f,
    val state: ExerciseState = ExerciseState.READY,
    val bodyAlignment: Float = 0f,  // Vücut hizalama skoru (0-100)
    val shoulderDepth: Float = 0f   // Omuz derinliği (yere ne kadar yakın)
)

/**
 * Vücut Hizalama Kontrolü
 */
data class BodyAlignment(
    val isGood: Boolean,
    val score: Float,  // 0-100 arası skor
    val message: String
)

/**
 * ADVANCED PUSH-UP COUNTER (v2.0)
 *
 * GitHub ve MediaPipe best practices uygulanmış gelişmiş şınav sayacı
 *
 * İYİLEŞTİRMELER:
 * 1. ✅ Hysteresis Threshold - Eşik etrafında zıplamayı önler
 * 2. ✅ EMA Smoothing - Daha akıllı gürültü filtreleme
 * 3. ✅ Temporal Validation - Hız bazlı hareket kontrolü
 * 4. ✅ Multi-criteria Form Check - Çoklu form kriteri
 * 5. ✅ Enhanced Body Alignment - Gelişmiş vücut hizalama algoritması
 * 6. ✅ Shoulder Depth Detection - Omuz derinlik kontrolü
 * 7. ✅ Visibility Confidence - Gelişmiş görünürlük kontrolü
 * 8. ✅ OneEuroFilter Integration - VR/AR industry-standard filtering (NEW!)
 * 9. ✅ Velocity Analysis - Momentum/savurma önleme (NEW!)
 */
class PushUpCounter {
    private var state = ExerciseState.READY
    private var pushUpCount = 0

    // ═══ ONE EURO FILTERS (Optimized - Sadece kritik eklemler) ═══
    // OPTIMIZATION: Sadece dirsek için filter (omuzlar zaten stabil)
    private val leftElbowFilter = OneEuroFilter(minCutoff = 1.2f, beta = 0.01f)  // Biraz daha responsive
    private val rightElbowFilter = OneEuroFilter(minCutoff = 1.2f, beta = 0.01f)

    // ═══ MOVEMENT QUALITY ANALYZER (Hız/İvme kontrolü) ═══
    // Dirsek hareketinin hızını analiz ederek momentum/savurma tespiti
    private val movementAnalyzer = MovementQualityAnalyzer()

    // === HYSTERESIS THRESHOLDS (Eşik etrafında zıplamayı önler) ===
    // GitHub best practices: Giriş ve çıkış eşiklerini farklılaştır
    private val elbowDownHysteresis = HysteresisThreshold(
        lowThreshold = 90f,   // Bu açının altına inince DOWN state'e geç
        highThreshold = 110f  // Bu açının üstüne çıkınca DOWN'dan çık
    )

    private val elbowUpHysteresis = HysteresisThreshold(
        lowThreshold = 150f,  // Bu açının altına inince UP'tan çık
        highThreshold = 165f  // Bu açının üstüne çıkınca UP state'e geç
    )

    // === SMOOTHING FILTERS (Gürültü filtreleme) ===
    // Her kritik eklem için ayrı filtre (GitHub best practices)
    private val elbowAngleFilter = SmoothingFilter(windowSize = 5, alpha = 0.3f)
    private val bodyAngleFilter = SmoothingFilter(windowSize = 7, alpha = 0.25f)
    private val shoulderDepthFilter = SmoothingFilter(windowSize = 5, alpha = 0.35f)

    // === TEMPORAL VALIDATORS (Zaman bazlı kontroller) ===
    // MediaPipe best practices: Hareket hızı kontrolü
    private val downPhaseValidator = MovementValidator(
        minDurationMs = 400,   // Minimum 400ms (çok hızlı hareket = geçersiz)
        maxDurationMs = 4000   // Maksimum 4s (çok yavaş = geçersiz)
    )

    private val upPhaseValidator = MovementValidator(
        minDurationMs = 400,
        maxDurationMs = 4000
    )

    // === PEAK-HOLD LOGIC (Tepe noktası bekleme) ===
    // BEST PRACTICE: Tepe ve dip noktalarında kısa süre bekleme zorunluluğu
    // Bu, yarım hareketleri ve sarsıntıları büyük ölçüde önler
    // Araştırma: "Conditions tracked for 24 continuous frames to avoid false positives"
    private val peakHoldDurationMs = 200L  // Tepe noktasında 200ms bekle
    private var peakHoldStartTime = 0L
    private var isHoldingPeak = false

    // === VISIBILITY THRESHOLDS (Görünürlük eşikleri) ===
    // Kritik eklemler için yüksek confidence gerekliliği
    private val MIN_CRITICAL_LIKELIHOOD = 0.7f  // Kritik eklemler için
    private val MIN_SECONDARY_LIKELIHOOD = 0.5f // İkincil eklemler için

    // === FORM QUALITY THRESHOLDS (Form kalitesi eşikleri) ===
    private val EXCELLENT_BODY_ALIGNMENT = 10f  // Vücut hizalama sapması (px)
    private val GOOD_BODY_ALIGNMENT = 30f
    private val POOR_BODY_ALIGNMENT = 60f

    private val EXCELLENT_SHOULDER_DEPTH = 0.8f  // Omuz-dirsek oranı
    private val GOOD_SHOULDER_DEPTH = 0.6f

    // Debug state tracking
    private var lastStateChangeTime = 0L
    private var consecutiveInvalidFrames = 0

    /**
     * MAIN PROCESSING FUNCTION (v2.0 - OneEuroFilter + Velocity Analysis)
     *
     * Pose verisini işler ve şınav sayımını günceller
     * Çoklu kriter ve akıllı filtreleme uygular
     */
    fun processPose(pose: Pose): PushUpResult {
        // ===== STEP 1: LANDMARK EXTRACTION =====
        val landmarks = extractLandmarks(pose)
        if (!landmarks.areValid) {
            return createInvalidResult(landmarks.missingMessage)
        }

        // ═══ STEP 1.5: ONE EURO FILTER APPLICATION (OPTIMIZED!) ═══
        // Sadece dirsek pozisyonlarını filtrele (performance optimization)
        val timestamp = System.currentTimeMillis()

        val filteredLeftElbow = leftElbowFilter.filter(landmarks.leftElbow!!.position, timestamp)
        val filteredRightElbow = rightElbowFilter.filter(landmarks.rightElbow!!.position, timestamp)

        // ===== STEP 2: ANGLE CALCULATIONS WITH FILTERED POSITIONS =====
        // Sol ve sağ dirsek açılarını hesapla (omuz raw, dirsek filtered)
        val leftElbowAngle = calculateAngle(
            landmarks.leftShoulder!!.position,  // Raw omuz pozisyonu (zaten stabil)
            filteredLeftElbow,
            landmarks.leftWrist!!.position
        )
        val rightElbowAngle = calculateAngle(
            landmarks.rightShoulder!!.position,  // Raw omuz pozisyonu
            filteredRightElbow,
            landmarks.rightWrist!!.position
        )

        // Ortalama dirsek açısı (artık çok daha smooth!)
        val rawElbowAngle = (leftElbowAngle + rightElbowAngle) / 2f
        val smoothElbowAngle = elbowAngleFilter.addAngle(rawElbowAngle)

        // ═══ STEP 2.5: VELOCITY ANALYSIS (NEW!) ═══
        // Dirsek hareketinin hızını analiz et (momentum/savurma tespiti)
        val avgElbowPosition = PointF(
            (filteredLeftElbow.x + filteredRightElbow.x) / 2f,
            (filteredLeftElbow.y + filteredRightElbow.y) / 2f
        )
        val movementQuality = movementAnalyzer.analyzeMovement(avgElbowPosition, timestamp)

        // Hareket çok hızlıysa (momentum kullanıyor) veya çok yavaşsa geçersiz
        val isMovementValid = !movementQuality.isTooFast &&
                              movementQuality.qualityScore >= 50f  // Minimum %50 kalite skoru

        // ===== STEP 3: MULTI-CRITERIA FORM VALIDATION =====
        val bodyAlignment = checkAdvancedBodyAlignment(landmarks)
        val shoulderDepth = checkShoulderDepth(landmarks, smoothElbowAngle)

        // Form geçerliliği (tüm kriterler kontrol edilir)
        val isFormValid = bodyAlignment.isGood && shoulderDepth > 0.3f

        if (!isFormValid) {
            consecutiveInvalidFrames++
            if (consecutiveInvalidFrames > 10) {
                // 10 frame üst üste geçersiz form = INVALID state'e geç
                state = ExerciseState.INVALID
            }
        } else {
            consecutiveInvalidFrames = 0
        }

        // ===== STEP 4: STATE MACHINE WITH HYSTERESIS =====
        val previousState = state
        val feedback: String
        val formQuality: FormQuality

        when (state) {
            ExerciseState.READY -> {
                // Başlangıç pozisyonu: Kullanıcı plank pozisyonuna geçmeli
                val isUp = elbowUpHysteresis.check(smoothElbowAngle, highMeansActive = true)

                if (isUp && isFormValid) {
                    state = ExerciseState.UP
                    upPhaseValidator.startPhase()
                    feedback = "✅ Hazır! Şınav yapmaya başlayın"
                    formQuality = FormQuality.GOOD

                    Log.d(TAG, "READY → UP (Açı: ${smoothElbowAngle.toInt()}°)")
                } else {
                    feedback = if (!isFormValid) {
                        "⚠️ ${bodyAlignment.message}"
                    } else {
                        "📌 Plank pozisyonuna geçin (kollar düz)"
                    }
                    formQuality = FormQuality.NEUTRAL
                }
            }

            ExerciseState.UP -> {
                // Yukarı pozisyon: Kullanıcı aşağı inmeli
                val isDown = elbowDownHysteresis.check(smoothElbowAngle, highMeansActive = false)

                if (isDown) {
                    // BEST PRACTICE: Peak-Hold Logic - DOWN pozisyonunda biraz bekle
                    if (checkPeakHold()) {
                        // Aşağı pozisyona geçiş - TEMPORAL VALIDATION uygula
                        if (upPhaseValidator.isValidMovement()) {
                            state = ExerciseState.DOWN
                            downPhaseValidator.startPhase()
                            resetPeakHold()

                            feedback = if (isFormValid) {
                                "👍 İyi! Şimdi yukarı itin"
                            } else {
                                "⚠️ Form: ${bodyAlignment.message}"
                            }
                            formQuality = if (isFormValid) FormQuality.GOOD else FormQuality.POOR

                            Log.d(TAG, "UP → DOWN (Açı: ${smoothElbowAngle.toInt()}°, Süre: ${upPhaseValidator.getElapsedTime()}ms)")
                        } else {
                            // Hareket çok hızlı veya çok yavaş - geçersiz
                            state = ExerciseState.TRANSITION
                            resetPeakHold()
                            feedback = "⚠️ Hareket çok ${if (upPhaseValidator.getElapsedTime() < 400) "hızlı" else "yavaş"}"
                            formQuality = FormQuality.POOR

                            Log.d(TAG, "UP → TRANSITION (Geçersiz süre: ${upPhaseValidator.getElapsedTime()}ms)")
                        }
                    } else {
                        feedback = "⏸️ Aşağıda biraz bekleyin..."
                        formQuality = FormQuality.NEUTRAL
                    }
                } else {
                    resetPeakHold()  // Henüz aşağı inmedi, hold'u sıfırla
                    feedback = "⬇️ Göğsünüzü yere indirin"
                    formQuality = FormQuality.NEUTRAL
                }
            }

            ExerciseState.DOWN -> {
                // Aşağı pozisyon: Kullanıcı yukarı itmeli
                val isUp = elbowUpHysteresis.check(smoothElbowAngle, highMeansActive = true)

                if (isUp) {
                    // BEST PRACTICE: Peak-Hold Logic - UP pozisyonunda biraz bekle
                    if (checkPeakHold()) {
                        // ═══ VELOCITY VALIDATION + TEMPORAL + FORM (3-Layer Validation) ═══
                        val isTemporalValid = downPhaseValidator.isValidMovement()

                        // Yukarı pozisyona geçiş - TÜM KRİTERLER KONTROL EDİLİR
                        if (isTemporalValid && isFormValid && isMovementValid) {
                            // ✅ GEÇERLİ ŞINAV TAMAMLANDI!
                            state = ExerciseState.UP
                            pushUpCount++
                            upPhaseValidator.startPhase()
                            resetPeakHold()

                            feedback = "🎉 Harika! Şınav: $pushUpCount"
                            formQuality = FormQuality.EXCELLENT

                            Log.d(TAG, "✅ DOWN → UP | ŞINAV SAYILDI! #$pushUpCount " +
                                    "(Açı: ${smoothElbowAngle.toInt()}°, Süre: ${downPhaseValidator.getElapsedTime()}ms, " +
                                    "Alignment: ${bodyAlignment.score.toInt()}%, Velocity: ${movementQuality.velocity.toInt()} px/s)")
                        } else {
                            // ❌ Form, tempo veya hareket kalitesi kötü - tekrar sayılmadı
                            state = ExerciseState.TRANSITION
                            resetPeakHold()
                            feedback = when {
                                !isFormValid -> "❌ Form hatalı - tekrar sayılmadı"
                                !isTemporalValid -> "❌ Hareket çok ${if (downPhaseValidator.getElapsedTime() < 400) "hızlı" else "yavaş"}"
                                !isMovementValid -> movementQuality.feedback  // "⚠️ Daha Yavaş Hareket Et"
                                else -> "❌ Geçersiz tekrar"
                            }
                            formQuality = FormQuality.POOR

                            Log.d(TAG, "❌ DOWN → TRANSITION (Form: $isFormValid, Temporal: $isTemporalValid, " +
                                    "Movement: $isMovementValid, Velocity: ${movementQuality.velocity.toInt()} px/s)")
                        }
                    } else {
                        feedback = "⏸️ Yukarıda biraz bekleyin..."
                        formQuality = FormQuality.NEUTRAL
                    }
                } else {
                    resetPeakHold()  // Henüz yukarı çıkmadı, hold'u sıfırla
                    feedback = "⬆️ Yukarı itin"
                    formQuality = FormQuality.NEUTRAL
                }
            }

            ExerciseState.TRANSITION -> {
                // Geçiş durumu: Form düzelene kadar bekle
                val isUp = elbowUpHysteresis.check(smoothElbowAngle, highMeansActive = true)

                if (isUp && isFormValid) {
                    state = ExerciseState.UP
                    upPhaseValidator.startPhase()
                    feedback = "✅ Form düzeltildi! Devam edin"
                    formQuality = FormQuality.GOOD
                } else {
                    feedback = "⏸️ Doğru forma geçin"
                    formQuality = FormQuality.NEUTRAL
                }
            }

            ExerciseState.INVALID -> {
                // Geçersiz form: Kullanıcı formu düzeltmeli
                val isUp = elbowUpHysteresis.check(smoothElbowAngle, highMeansActive = true)

                if (isUp && isFormValid) {
                    state = ExerciseState.UP
                    upPhaseValidator.startPhase()
                    consecutiveInvalidFrames = 0
                    feedback = "✅ Form düzeltildi! Devam edin"
                    formQuality = FormQuality.GOOD

                    Log.d(TAG, "INVALID → UP (Form düzeltildi)")
                } else {
                    feedback = "❌ ${bodyAlignment.message}"
                    formQuality = FormQuality.INVALID
                }
            }
        }

        // State değişimi logla
        if (state != previousState) {
            lastStateChangeTime = System.currentTimeMillis()
        }

        return PushUpResult(
            count = pushUpCount,
            feedback = feedback,
            formQuality = formQuality,
            elbowAngle = smoothElbowAngle,
            state = state,
            bodyAlignment = bodyAlignment.score,
            shoulderDepth = shoulderDepth
        )
    }

    /**
     * ENHANCED LANDMARK EXTRACTION
     *
     * Tüm gerekli eklemleri çıkarır ve görünürlük kontrolü yapar
     * GitHub best practices: Kritik ve ikincil eklemler ayrı confidence ile kontrol edilir
     */
    private fun extractLandmarks(pose: Pose): LandmarkSet {
        // Kritik eklemler (mutlaka görünür olmalı)
        val criticalLandmarks = listOf(
            pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.LEFT_WRIST),
            pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        )

        // İkincil eklemler (form kalitesi için)
        val secondaryLandmarks = listOf(
            pose.getPoseLandmark(PoseLandmark.LEFT_HIP),
            pose.getPoseLandmark(PoseLandmark.RIGHT_HIP),
            pose.getPoseLandmark(PoseLandmark.LEFT_KNEE),
            pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        )

        // Kritik eklemler kontrolü
        val missingCritical = criticalLandmarks.any {
            it == null || it.inFrameLikelihood < MIN_CRITICAL_LIKELIHOOD
        }

        if (missingCritical) {
            return LandmarkSet(
                areValid = false,
                missingMessage = "⚠️ Lütfen kamerada üst vücudunuzun tamamını gösterin"
            )
        }

        // İkincil eklemler kontrolü
        val missingSecondary = secondaryLandmarks.count {
            it == null || it.inFrameLikelihood < MIN_SECONDARY_LIKELIHOOD
        }

        if (missingSecondary > 1) {
            return LandmarkSet(
                areValid = false,
                missingMessage = "⚠️ Lütfen kamerada tüm vücudunuzu gösterin"
            )
        }

        return LandmarkSet(
            areValid = true,
            leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
            rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
            leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
            rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
            leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST),
            rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST),
            leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP),
            rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP),
            leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE),
            rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE),
            leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE),
            rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        )
    }

    /**
     * ADVANCED BODY ALIGNMENT CHECK
     *
     * GitHub/MediaPipe best practices uygulanmış gelişmiş hizalama kontrolü
     *
     * Kontrol Edilen Kriterler:
     * 1. Omuz-Kalça-Diz doğrusallığı (collinearity)
     * 2. Kalça-Ayak hizalaması
     * 3. Omuz-Kalça mesafesi vs yükseklik oranı
     */
    private fun checkAdvancedBodyAlignment(landmarks: LandmarkSet): BodyAlignment {
        val leftShoulder = landmarks.leftShoulder!!
        val leftHip = landmarks.leftHip ?: return BodyAlignment(false, 0f, "Kalça görünmüyor")
        val leftKnee = landmarks.leftKnee ?: return BodyAlignment(false, 0f, "Diz görünmüyor")
        val leftAnkle = landmarks.leftAnkle

        // Kriter 1: Omuz-Kalça-Diz doğrusallığı
        val collinearity = calculateCollinearity(
            leftShoulder.position,
            leftHip.position,
            leftKnee.position
        )

        // Kriter 2: Kalça yüksekliği kontrolü (V-shape önleme)
        val shoulderToHipDist = calculateDistance(leftShoulder.position, leftHip.position)
        val hipHeight = leftHip.position.y - leftShoulder.position.y

        // Kalça omuzdan çok yukarıda mı? (V-shape = hatalı form)
        val isHipTooHigh = hipHeight < -shoulderToHipDist * 0.3f  // 30% threshold

        // Kriter 3: Ayak hizalaması (varsa)
        val isAnkleAligned = if (leftAnkle != null) {
            val ankleToLine = pointToLineDistance(
                leftAnkle.position,
                leftShoulder.position,
                leftKnee.position
            )
            ankleToLine < 100f  // Pixel threshold
        } else {
            true  // Ayak görünmüyorsa atla
        }

        // Skor hesaplama (0-100)
        val alignmentScore = when {
            collinearity < EXCELLENT_BODY_ALIGNMENT && !isHipTooHigh && isAnkleAligned -> 100f
            collinearity < GOOD_BODY_ALIGNMENT && !isHipTooHigh -> 75f
            collinearity < POOR_BODY_ALIGNMENT -> 50f
            else -> 25f
        }

        val isGood = alignmentScore >= 50f

        val message = when {
            isHipTooHigh -> "Kalçanızı indirin (V-shape)"
            collinearity > POOR_BODY_ALIGNMENT -> "Vücudunuzu düz tutun"
            !isAnkleAligned -> "Bacaklarınızı düzleştirin"
            alignmentScore >= 75f -> "Mükemmel form!"
            else -> "İyi form"
        }

        // Smoothing uygula
        val smoothScore = bodyAngleFilter.addAngle(alignmentScore)

        return BodyAlignment(isGood, smoothScore, message)
    }

    /**
     * SHOULDER DEPTH CHECK
     *
     * MediaPipe fitness AI tutorial'dan alınan teknik
     * Omuzun ne kadar aşağı indiğini kontrol eder
     */
    private fun checkShoulderDepth(landmarks: LandmarkSet, elbowAngle: Float): Float {
        val leftShoulder = landmarks.leftShoulder!!
        val leftElbow = landmarks.leftElbow!!

        // Omuz-dirsek mesafesi
        val shoulderToElbow = calculateDistance(leftShoulder.position, leftElbow.position)

        // Omuz-dirsek Y farkı (omuz ne kadar aşağıda)
        val shoulderDepthY = leftElbow.position.y - leftShoulder.position.y

        // Normalize edilmiş derinlik (0-1 arası)
        val normalizedDepth = if (shoulderToElbow > 0) {
            (shoulderDepthY / shoulderToElbow).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Smoothing uygula
        return shoulderDepthFilter.addAngle(normalizedDepth)
    }

    /**
     * BEST PRACTICE: Peak-Hold Check
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
     * Helper: Geçersiz sonuç oluştur
     */
    private fun createInvalidResult(message: String): PushUpResult {
        return PushUpResult(
            count = pushUpCount,
            feedback = message,
            formQuality = FormQuality.INVALID,
            state = state
        )
    }

    /**
     * Sayacı sıfırla (v2.0 - OneEuroFilter + MovementAnalyzer dahil)
     */
    fun reset() {
        state = ExerciseState.READY
        pushUpCount = 0

        // ═══ ONE EURO FILTERS RESET (OPTIMIZED!) ═══
        leftElbowFilter.reset()
        rightElbowFilter.reset()

        // ═══ MOVEMENT ANALYZER RESET (NEW!) ═══
        movementAnalyzer.reset()

        // Tüm eski filtreleri sıfırla
        elbowAngleFilter.reset()
        bodyAngleFilter.reset()
        shoulderDepthFilter.reset()

        // Hysteresis'leri sıfırla
        elbowDownHysteresis.reset()
        elbowUpHysteresis.reset()

        // Validator'ları sıfırla
        downPhaseValidator.reset()
        upPhaseValidator.reset()

        // Peak-hold'u sıfırla
        resetPeakHold()

        consecutiveInvalidFrames = 0

        Log.d(TAG, "🔄 Şınav sayacı sıfırlandı (v2.0 - Filters + Analyzer reset)")
    }

    fun getCount(): Int = pushUpCount
    fun getState(): ExerciseState = state

    /**
     * Landmark data holder
     */
    private data class LandmarkSet(
        val areValid: Boolean,
        val missingMessage: String = "",
        val leftShoulder: PoseLandmark? = null,
        val rightShoulder: PoseLandmark? = null,
        val leftElbow: PoseLandmark? = null,
        val rightElbow: PoseLandmark? = null,
        val leftWrist: PoseLandmark? = null,
        val rightWrist: PoseLandmark? = null,
        val leftHip: PoseLandmark? = null,
        val rightHip: PoseLandmark? = null,
        val leftKnee: PoseLandmark? = null,
        val rightKnee: PoseLandmark? = null,
        val leftAnkle: PoseLandmark? = null,
        val rightAnkle: PoseLandmark? = null
    )

    companion object {
        private const val TAG = "PushUpCounter"
    }
}
