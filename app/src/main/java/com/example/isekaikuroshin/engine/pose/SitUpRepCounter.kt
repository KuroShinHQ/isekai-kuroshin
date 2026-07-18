package com.example.isekaikuroshin.engine.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import android.util.Log

/**
 * Mekik Tekrar Sayacı
 *
 * ÇALIŞMA PRENSİBİ:
 * 1. Gövde açısını ölç (omuz-kalça-diz)
 * 2. Durum makinesi: DOWN (sırt yerde, 120°-180°) <-> UP (oturmuş, 40°-90°)
 * 3. Tam döngü = 1 tekrar
 *
 * FORM KALİTESİ KONTROLÜ:
 * - Ayaklar yerde mi?
 * - Eller kafanın arkasında mı?
 * - Hareket aralığı yeterli mi?
 *
 * KALİBRASYON DESTEĞİ:
 * - Kullanıcı "yatar" ve "oturur" pozisyonunu kaydedebilir
 */
class SitUpRepCounter : RepCounterBase() {

    companion object {
        private const val TAG = "SitUpRepCounter"

        // ========================================
        // VARSAYILAN AÇI ARALIĞI (Kalibrasyon yoksa)
        // ========================================

        // Alt pozisyon (Sırt yerde)
        private const val DEFAULT_DOWN_ANGLE_MIN = 140f
        private const val DEFAULT_DOWN_ANGLE_MAX = 180f

        // Üst pozisyon (Oturmuş)
        private const val DEFAULT_UP_ANGLE_MIN = 40f
        private const val DEFAULT_UP_ANGLE_MAX = 90f

        // ========================================
        // FORM KALİTESİ
        // ========================================

        private const val MIN_RANGE_OF_MOTION = 60f
    }

    // ========================================
    // STATE
    // ========================================

    private var currentTorsoAngle = 0f
    private var maxAngleThisRep = 0f
    private var minAngleThisRep = 180f

    private var currentFeedback = "Mekik pozisyonuna geçin (sırtüstü yatın)"
    private var currentFormQuality = 0f

    // Kalibrasyon
    private var downAngleRange = DEFAULT_DOWN_ANGLE_MIN..DEFAULT_DOWN_ANGLE_MAX
    private var upAngleRange = DEFAULT_UP_ANGLE_MIN..DEFAULT_UP_ANGLE_MAX

    // ========================================
    // KALIBRASYON
    // ========================================

    override fun setCalibration(calibration: CalibratedPose) {
        super.setCalibration(calibration)

        val upperTorsoAngle = calibration.upperPositionAngles["torso_angle"] ?: DEFAULT_UP_ANGLE_MIN
        val lowerTorsoAngle = calibration.lowerPositionAngles["torso_angle"] ?: DEFAULT_DOWN_ANGLE_MIN

        val tolerance = 15f
        upAngleRange = (upperTorsoAngle - tolerance)..(upperTorsoAngle + tolerance)
        downAngleRange = (lowerTorsoAngle - tolerance)..(lowerTorsoAngle + tolerance)

        Log.d(TAG, "✅ Calibration set: UP=${upAngleRange}, DOWN=${downAngleRange}")
    }

    // ========================================
    // POSE PROCESSING
    // ========================================

    override fun processPose(pose: Pose): Boolean {
        // ========================================
        // 1. LANDMARK'LARI AL
        // ========================================

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)

        // Ayaklar (form kontrolü için)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        // ========================================
        // 2. GÖVDE AÇISINI HESAPLA (Omuz-Kalça-Diz)
        // ========================================

        val rightTorsoAngle = calculateAngle(rightShoulder, rightHip, rightKnee)
        val leftTorsoAngle = calculateAngle(leftShoulder, leftHip, leftKnee)

        currentTorsoAngle = when {
            rightTorsoAngle != null && leftTorsoAngle != null -> (rightTorsoAngle + leftTorsoAngle) / 2f
            rightTorsoAngle != null -> rightTorsoAngle
            leftTorsoAngle != null -> leftTorsoAngle
            else -> {
                currentFeedback = "Vücudunuzu kameraya gösterin (yan pozisyon)"
                currentFormQuality = 0f
                return false
            }
        }

        // ========================================
        // 3. FORM KALİTESİ
        // ========================================

        currentFormQuality = calculateFormQuality(rightAnkle, leftAnkle, rightKnee, leftKnee)

        // ========================================
        // 4. DURUM MAKİNESİ
        // ========================================

        val wasNewRep = updateStateMachine(currentTorsoAngle)

        // ========================================
        // 5. FEEDBACK
        // ========================================

        updateFeedback(currentTorsoAngle, currentFormQuality)

        return wasNewRep
    }

    /**
     * Durum makinesi
     */
    private fun updateStateMachine(torsoAngle: Float): Boolean {
        var newRep = false

        maxAngleThisRep = maxOf(maxAngleThisRep, torsoAngle)
        minAngleThisRep = minOf(minAngleThisRep, torsoAngle)

        when (currentState) {
            ExerciseState.UNKNOWN -> {
                currentState = when {
                    torsoAngle in downAngleRange -> ExerciseState.DOWN
                    torsoAngle in upAngleRange -> ExerciseState.UP
                    else -> ExerciseState.UNKNOWN
                }
                Log.d(TAG, "Initial state: $currentState (angle: ${torsoAngle.toInt()}°)")
            }

            ExerciseState.DOWN -> {
                // Sırttayken, oturmayı bekle
                if (torsoAngle in upAngleRange && canTransition()) {
                    currentState = ExerciseState.UP
                    recordTransition()
                    Log.d(TAG, "Transition: DOWN -> UP (angle: ${torsoAngle.toInt()}°)")
                }
            }

            ExerciseState.UP -> {
                // Oturmuşken, yatmayı bekle
                if (torsoAngle in downAngleRange && canTransition()) {
                    val rangeOfMotion = maxAngleThisRep - minAngleThisRep

                    if (rangeOfMotion >= MIN_RANGE_OF_MOTION) {
                        repCount++
                        newRep = true
                        Log.d(TAG, "✅ REP COUNTED! Total: $repCount (ROM: ${rangeOfMotion.toInt()}°)")
                    } else {
                        Log.w(TAG, "⚠️ Insufficient ROM: ${rangeOfMotion.toInt()}°")
                    }

                    currentState = ExerciseState.DOWN
                    recordTransition()

                    maxAngleThisRep = torsoAngle
                    minAngleThisRep = torsoAngle
                }
            }

            ExerciseState.TRANSITIONING -> {}
        }

        return newRep
    }

    /**
     * Form kalitesi
     */
    private fun calculateFormQuality(
        rightAnkle: PoseLandmark?,
        leftAnkle: PoseLandmark?,
        rightKnee: PoseLandmark?,
        leftKnee: PoseLandmark?
    ): Float {
        var qualityScore = 1.0f

        // Ayaklar yerde mi kontrol et (Y pozisyonu büyük olmalı - ekranın altında)
        if (rightAnkle != null && leftAnkle != null && rightKnee != null && leftKnee != null) {
            val avgAnkleY = (rightAnkle.position.y + leftAnkle.position.y) / 2f
            val avgKneeY = (rightKnee.position.y + leftKnee.position.y) / 2f

            // Ayaklar dizlerden daha aşağıda olmalı
            if (avgAnkleY < avgKneeY) {
                qualityScore -= 0.3f  // Ayaklar havada
            }
        }

        // Hareket aralığı
        val rangeOfMotion = maxAngleThisRep - minAngleThisRep
        if (rangeOfMotion < MIN_RANGE_OF_MOTION) {
            qualityScore -= 0.4f
        }

        return qualityScore.coerceIn(0f, 1f)
    }

    /**
     * Feedback
     */
    private fun updateFeedback(torsoAngle: Float, formQuality: Float) {
        currentFeedback = when {
            formQuality < 0.5f -> "⚠️ Ayaklarınızı yerde tutun!"
            currentState == ExerciseState.DOWN -> "⬆️ Oturun (${torsoAngle.toInt()}°)"
            currentState == ExerciseState.UP -> "⬇️ Yatın (${torsoAngle.toInt()}°)"
            else -> "Yan dönün, kamerayı görün"
        }
    }

    // ========================================
    // GETTER'LAR
    // ========================================

    override fun getFeedback(): String = currentFeedback

    override fun getFormQuality(): Float = currentFormQuality

    fun getCurrentTorsoAngle(): Float = currentTorsoAngle
}
