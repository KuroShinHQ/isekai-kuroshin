package com.example.isekaikuroshin.engine.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import android.util.Log

/**
 * Şınav Tekrar Sayacı
 *
 * ÇALIŞMA PRENSİBİ:
 * 1. Dirsek açısını ölç (omuz-dirsek-bilek)
 * 2. Durum makines: DOWN (90°-110°) <-> UP (160°-180°)
 * 3. Tam döngü = 1 tekrar
 *
 * FORM KALİTESİ KONTROLÜ:
 * - Vücut düzlüğü (kalça-omuz-ayak hizalı mı?)
 * - Dirsekler dışarı açık mı? (form hatası)
 * - Hareket aralığı yeterli mi?
 *
 * KALİBRASYON DESTEĞİ:
 * - Kullanıcı "yukarı" ve "aşağı" pozisyonunu kaydedebilir
 * - Sistem bu pozisyonlara göre özel açı aralıkları belirler
 */
class PushUpRepCounter : RepCounterBase() {

    companion object {
        private const val TAG = "PushUpRepCounter"

        // ========================================
        // VARSAYILAN AÇI ARALIĞI (Kalibrasyon yoksa)
        // ========================================

        // Alt pozisyon (Dirsekler bükülmüş)
        private const val DEFAULT_DOWN_ANGLE_MIN = 70f
        private const val DEFAULT_DOWN_ANGLE_MAX = 110f

        // Üst pozisyon (Dirsekler düz)
        private const val DEFAULT_UP_ANGLE_MIN = 150f
        private const val DEFAULT_UP_ANGLE_MAX = 180f

        // ========================================
        // FORM KALİTESİ THRESHOLD'LARI
        // ========================================

        // Vücut hizalama toleransı (derece)
        private const val BODY_ALIGNMENT_TOLERANCE = 15f

        // Hareket aralığı minimum (derece)
        private const val MIN_RANGE_OF_MOTION = 40f
    }

    // ========================================
    // STATE
    // ========================================

    private var currentElbowAngle = 0f
    private var maxAngleThisRep = 0f
    private var minAngleThisRep = 180f

    private var currentFeedback = "Şınav pozisyonuna geçin"
    private var currentFormQuality = 0f

    // Kalibrasyon varsa, bu değerler override edilir
    private var downAngleRange = DEFAULT_DOWN_ANGLE_MIN..DEFAULT_DOWN_ANGLE_MAX
    private var upAngleRange = DEFAULT_UP_ANGLE_MIN..DEFAULT_UP_ANGLE_MAX

    // ========================================
    // KALIBRASYON (Override)
    // ========================================

    override fun setCalibration(calibration: CalibratedPose) {
        super.setCalibration(calibration)

        // Dirsek açılarını çıkar
        val upperElbowAngle = calibration.upperPositionAngles["elbow_angle"] ?: DEFAULT_UP_ANGLE_MIN
        val lowerElbowAngle = calibration.lowerPositionAngles["elbow_angle"] ?: DEFAULT_DOWN_ANGLE_MIN

        // Tolerans ekle (±10°)
        val tolerance = 10f
        upAngleRange = (upperElbowAngle - tolerance)..(upperElbowAngle + tolerance)
        downAngleRange = (lowerElbowAngle - tolerance)..(lowerElbowAngle + tolerance)

        Log.d(TAG, "✅ Calibration set: UP=${upAngleRange}, DOWN=${downAngleRange}")
    }

    // ========================================
    // POSE PROCESSING
    // ========================================

    override fun processPose(pose: Pose): Boolean {
        // ========================================
        // 1. LANDMARK'LARI AL
        // ========================================

        // Sağ kol (öncelik)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        // Sol kol (fallback)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        // Vücut hizalama için
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        // ========================================
        // 2. DİRSEK AÇISINI HESAPLA
        // ========================================

        val rightElbowAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)
        val leftElbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)

        // İki koldan da ölçüm yapabiliyorsak ortalama al
        currentElbowAngle = when {
            rightElbowAngle != null && leftElbowAngle != null -> (rightElbowAngle + leftElbowAngle) / 2f
            rightElbowAngle != null -> rightElbowAngle
            leftElbowAngle != null -> leftElbowAngle
            else -> {
                currentFeedback = "Kollarınızı kameraya gösterin"
                currentFormQuality = 0f
                return false
            }
        }

        // ========================================
        // 3. FORM KALİTESİ HESAPLA
        // ========================================

        currentFormQuality = calculateFormQuality(
            rightShoulder, rightHip, rightAnkle,
            leftShoulder, leftHip, leftAnkle
        )

        // ========================================
        // 4. DURUM MAKİNESİ (Rep Counting)
        // ========================================

        val wasNewRep = updateStateMachine(currentElbowAngle)

        // ========================================
        // 5. FEEDBACK GÜNCELLECurrent
        // ========================================

        updateFeedback(currentElbowAngle, currentFormQuality)

        return wasNewRep
    }

    /**
     * Durum makinesi - Tekrar sayma mantığı
     */
    private fun updateStateMachine(elbowAngle: Float): Boolean {
        var newRep = false

        // Hareket aralığını takip et (bu tekrarda ne kadar hareket yaptı?)
        maxAngleThisRep = maxOf(maxAngleThisRep, elbowAngle)
        minAngleThisRep = minOf(minAngleThisRep, elbowAngle)

        when (currentState) {
            ExerciseState.UNKNOWN -> {
                // Başlangıç - hangi pozisyonda?
                currentState = when {
                    elbowAngle in downAngleRange -> ExerciseState.DOWN
                    elbowAngle in upAngleRange -> ExerciseState.UP
                    else -> ExerciseState.UNKNOWN
                }
                Log.d(TAG, "Initial state: $currentState (angle: ${elbowAngle.toInt()}°)")
            }

            ExerciseState.UP -> {
                // Yukarıdayken, aşağı inmeyi bekle
                if (elbowAngle in downAngleRange && canTransition()) {
                    currentState = ExerciseState.DOWN
                    recordTransition()
                    Log.d(TAG, "Transition: UP -> DOWN (angle: ${elbowAngle.toInt()}°)")
                }
            }

            ExerciseState.DOWN -> {
                // Aşağıdayken, yukarı çıkmayı bekle
                if (elbowAngle in upAngleRange && canTransition()) {
                    // ✅ TEKrar SAYILDI!
                    val rangeOfMotion = maxAngleThisRep - minAngleThisRep

                    // Hareket aralığı yeterli mi kontrol et
                    if (rangeOfMotion >= MIN_RANGE_OF_MOTION) {
                        repCount++
                        newRep = true
                        Log.d(TAG, "✅ REP COUNTED! Total: $repCount (ROM: ${rangeOfMotion.toInt()}°)")
                    } else {
                        Log.w(TAG, "⚠️ Insufficient range of motion: ${rangeOfMotion.toInt()}° (min: $MIN_RANGE_OF_MOTION°)")
                    }

                    currentState = ExerciseState.UP
                    recordTransition()

                    // Reset hareket aralığı
                    maxAngleThisRep = elbowAngle
                    minAngleThisRep = elbowAngle
                }
            }

            ExerciseState.TRANSITIONING -> {
                // Geçiş durumu (şu an kullanılmıyor)
            }
        }

        return newRep
    }

    /**
     * Form kalitesi hesapla (0.0 - 1.0)
     */
    private fun calculateFormQuality(
        rightShoulder: PoseLandmark?,
        rightHip: PoseLandmark?,
        rightAnkle: PoseLandmark?,
        leftShoulder: PoseLandmark?,
        leftHip: PoseLandmark?,
        leftAnkle: PoseLandmark?
    ): Float {
        var qualityScore = 1.0f

        // ========================================
        // VÜCUT HİZALAMASI (Omuz-Kalça-Ayak düz mü?)
        // ========================================

        val rightBodyAngle = calculateAngle(rightShoulder, rightHip, rightAnkle)
        val leftBodyAngle = calculateAngle(leftShoulder, leftHip, leftAnkle)

        if (rightBodyAngle != null) {
            // İdeal: 180° (düz hat)
            val deviation = Math.abs(180f - rightBodyAngle)
            if (deviation > BODY_ALIGNMENT_TOLERANCE) {
                qualityScore -= 0.3f  // -%30
            }
        }

        // ========================================
        // HAREKET ARALIĞI
        // ========================================

        val rangeOfMotion = maxAngleThisRep - minAngleThisRep
        if (rangeOfMotion < MIN_RANGE_OF_MOTION) {
            qualityScore -= 0.4f  // -%40
        }

        return qualityScore.coerceIn(0f, 1f)
    }

    /**
     * Feedback güncelle
     */
    private fun updateFeedback(elbowAngle: Float, formQuality: Float) {
        currentFeedback = when {
            formQuality < 0.5f -> "⚠️ Vücudunuzu düz tutun!"
            currentState == ExerciseState.DOWN -> "⬆️ Yukarı çıkın (${elbowAngle.toInt()}°)"
            currentState == ExerciseState.UP -> "⬇️ Aşağı inin (${elbowAngle.toInt()}°)"
            else -> "Şınav pozisyonuna geçin"
        }
    }

    // ========================================
    // GETTER'LAR
    // ========================================

    override fun getFeedback(): String = currentFeedback

    override fun getFormQuality(): Float = currentFormQuality

    /**
     * Mevcut dirsek açısını al (debug için)
     */
    fun getCurrentElbowAngle(): Float = currentElbowAngle
}
