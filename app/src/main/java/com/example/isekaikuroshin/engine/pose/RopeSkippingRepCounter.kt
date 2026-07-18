package com.example.isekaikuroshin.engine.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import android.util.Log

/**
 * İp Atlama Tekrar Sayacı
 *
 * ⚡ BÖLÜM B: AÇI TABANLI TEKRAR SAYMA (2024 Araştırma: PMC9760008, PMC9139829)
 *
 * ÇALIŞMA PRENSİBİ (Yeni):
 * 1. Kalça, diz ve ayak bileği açılarını ölç
 * 2. Araştırma bulguları:
 *    - Kalça açısı: 13.4° - 35.3° (fleksiyon aralığı)
 *    - Diz açısı: 13.6° - 67.9° (fleksiyon aralığı)
 *    - Ayak bileği hareketi de önemli
 * 3. Sıçrama algılama: Açılar daraldı (fleksiyon) -> genişledi (ekstansiyon)
 * 4. EMA smoothing ile titreme önleme (alpha = 0.5)
 * 5. Her tam sıçrama = 1 tekrar
 *
 * FORM KALİTESİ KONTROLÜ:
 * - Açı değişimi yeterli mi?
 * - İki ayak eşit mi?
 * - Ritimli mi?
 *
 * KALİBRASYON DESTEĞİ:
 * - Kullanıcı "yerde" ve "havada" pozisyonlarını kaydedebilir
 */
class RopeSkippingRepCounter : RepCounterBase() {

    companion object {
        private const val TAG = "RopeSkippingRepCounter"

        // ========================================
        // ⚡ BÖLÜM B: AÇI TABANLI THRESHOLD'LAR (Araştırma: PMC9760008, PMC9139829)
        // ========================================

        // Kalça açısı (Gövde-Kalça-Diz)
        // Araştırma: 13.4° - 35.3° fleksiyon aralığı
        private const val HIP_ANGLE_FLEXION_MIN = 155f   // Düz duruş (180° - 25° = ~155°)
        private const val HIP_ANGLE_FLEXION_MAX = 175f   // Hafif fleksiyon (180° - 5° = ~175°)

        // Diz açısı (Kalça-Diz-Ayak)
        // Araştırma: 13.6° - 67.9° fleksiyon aralığı
        private const val KNEE_ANGLE_EXTENSION_MIN = 160f  // Neredeyse düz (zıplarken)
        private const val KNEE_ANGLE_FLEXION_MAX = 140f    // Bükülme (yere değerken)

        // Minimum açı değişimi (fleksiyon aralığı)
        private const val MIN_ANGLE_CHANGE = 15f  // En az 15° değişim olmalı

        // ⚡ BÖLÜM C.2: EMA Smoothing Alpha
        private const val ANGLE_SMOOTHING_ALPHA = 0.5f  // Daha güçlü smoothing

        // Maksimum hava süresi (milisaniye) - çok uzun = hata
        private const val MAX_AIRTIME_MS = 800L

        // Minimum yer temas süresi (milisaniye)
        private const val MIN_GROUND_TIME_MS = 100L
    }

    // ========================================
    // STATE
    // ========================================

    enum class JumpState {
        EXTENDED,       // Bacaklar düz (zıplamanın zirvesi)
        FLEXED,         // Bacaklar bükülmüş (yere değme)
        TRANSITIONING   // Geçiş
    }

    private var jumpState: JumpState = JumpState.FLEXED

    // ⚡ BÖLÜM B: Açı takibi
    private var currentKneeAngle = 180f
    private var currentHipAngle = 180f
    private var maxKneeAngleThisJump = 0f    // Tam ekstansiyon (zıplarken)
    private var minKneeAngleThisJump = 180f  // Tam fleksiyon (yere değerken)

    // ⚡ BÖLÜM C.2: EMA Smoothing
    private var smoothedKneeAngle: Float? = null
    private var smoothedHipAngle: Float? = null

    private var jumpStartTime = 0L
    private var landingTime = 0L

    private var currentFeedback = "İp atlama pozisyonuna geçin"
    private var currentFormQuality = 0f

    // Kalibrasyon (açı tabanlı)
    private var calibratedExtensionAngle: Float? = null
    private var calibratedFlexionAngle: Float? = null

    // ========================================
    // KALIBRASYON
    // ========================================

    override fun setCalibration(calibration: CalibratedPose) {
        super.setCalibration(calibration)

        // ⚡ BÖLÜM B: Açı tabanlı kalibrasyon
        calibratedExtensionAngle = calibration.upperPositionAngles["knee_angle"]  // Zıplarken (düz bacak)
        calibratedFlexionAngle = calibration.lowerPositionAngles["knee_angle"]    // Yere değerken (bükülü bacak)

        if (calibratedExtensionAngle != null && calibratedFlexionAngle != null) {
            Log.d(TAG, "✅ Calibration set: Extension=${calibratedExtensionAngle}°, Flexion=${calibratedFlexionAngle}°")
        }
    }

    // ========================================
    // POSE PROCESSING
    // ========================================

    override fun processPose(pose: Pose): Boolean {
        // ========================================
        // 1. ⚡ BÖLÜM B: LANDMARK'LARI AL (Sadece ilgili eklemler)
        // ========================================

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        // Gövde için (kalça açısı hesabı)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)

        if (rightHip == null || rightKnee == null || rightAnkle == null ||
            leftHip == null || leftKnee == null || leftAnkle == null) {
            currentFeedback = "Bacaklarınızı kameraya gösterin"
            currentFormQuality = 0f
            return false
        }

        // ========================================
        // 2. ⚡ BÖLÜM B: AÇILARI HESAPLA
        // ========================================

        // Diz açısı (Kalça-Diz-Ayak)
        val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle) ?: 180f
        val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle) ?: 180f
        val rawKneeAngle = (rightKneeAngle + leftKneeAngle) / 2f

        // Kalça açısı (Gövde-Kalça-Diz) - opsiyonel, daha iyi form kontrolü için
        val rightHipAngle = if (rightShoulder != null) {
            calculateAngle(rightShoulder, rightHip, rightKnee) ?: 180f
        } else 180f
        val leftHipAngle = if (leftShoulder != null) {
            calculateAngle(leftShoulder, leftHip, leftKnee) ?: 180f
        } else 180f
        val rawHipAngle = (rightHipAngle + leftHipAngle) / 2f

        // ========================================
        // 3. ⚡ BÖLÜM C.2: EMA SMOOTHING (Titreme önleme)
        // ========================================

        currentKneeAngle = if (smoothedKneeAngle == null) {
            smoothedKneeAngle = rawKneeAngle
            rawKneeAngle
        } else {
            val smoothed = smoothedKneeAngle!! * (1 - ANGLE_SMOOTHING_ALPHA) + rawKneeAngle * ANGLE_SMOOTHING_ALPHA
            smoothedKneeAngle = smoothed
            smoothed
        }

        currentHipAngle = if (smoothedHipAngle == null) {
            smoothedHipAngle = rawHipAngle
            rawHipAngle
        } else {
            val smoothed = smoothedHipAngle!! * (1 - ANGLE_SMOOTHING_ALPHA) + rawHipAngle * ANGLE_SMOOTHING_ALPHA
            smoothedHipAngle = smoothed
            smoothed
        }

        // ========================================
        // 4. ⚡ BÖLÜM B: SIÇRAMA ALGILA (Açı tabanlı)
        // ========================================

        val wasNewRep = detectJumpByAngle(currentKneeAngle)

        // ========================================
        // 5. FORM KALİTESİ
        // ========================================

        currentFormQuality = calculateFormQuality(rightAnkle, leftAnkle)

        // ========================================
        // 6. FEEDBACK
        // ========================================

        updateFeedback()

        return wasNewRep
    }

    /**
     * ⚡ BÖLÜM B: Açı tabanlı sıçrama algılama mantığı
     *
     * Araştırma bulgularına göre:
     * - Zıplarken bacaklar düz = büyük diz açısı (160°+)
     * - Yere değerken bacaklar bükülür = küçük diz açısı (140°-)
     * - Döngü: FLEXED (bükülü) -> EXTENDED (düz) -> FLEXED
     */
    private fun detectJumpByAngle(kneeAngle: Float): Boolean {
        var newRep = false
        val currentTime = System.currentTimeMillis()

        // Hareket aralığını takip et
        maxKneeAngleThisJump = maxOf(maxKneeAngleThisJump, kneeAngle)
        minKneeAngleThisJump = minOf(minKneeAngleThisJump, kneeAngle)

        when (jumpState) {
            JumpState.FLEXED -> {
                // Bacaklar bükülü (yerdeyken) - ekstansiyonu bekle
                if (kneeAngle >= KNEE_ANGLE_EXTENSION_MIN && canTransition()) {
                    jumpState = JumpState.EXTENDED
                    jumpStartTime = currentTime
                    recordTransition()
                    Log.d(TAG, "🚀 Jump started! Knee angle: ${kneeAngle.toInt()}°")
                }
            }

            JumpState.EXTENDED -> {
                // Bacaklar düz (zıplarken) - fleksiyonu bekle (yere dönüş)
                if (kneeAngle <= KNEE_ANGLE_FLEXION_MAX && canTransition()) {
                    jumpState = JumpState.FLEXED
                    landingTime = currentTime

                    val airTime = landingTime - jumpStartTime
                    val angleChange = maxKneeAngleThisJump - minKneeAngleThisJump

                    Log.d(TAG, "🛬 Landing! Angle change: ${angleChange.toInt()}°, Air time: ${airTime}ms")

                    // ✅ TEKRAR SAYILDI! (Açı değişimi yeterli mi kontrol et)
                    if (angleChange >= MIN_ANGLE_CHANGE) {
                        repCount++
                        newRep = true
                        recordTransition()
                        Log.d(TAG, "✅ REP COUNTED! Total: $repCount (Angle ROM: ${angleChange.toInt()}°)")
                    } else {
                        Log.w(TAG, "⚠️ Insufficient angle change: ${angleChange.toInt()}° (min: $MIN_ANGLE_CHANGE°)")
                    }

                    // Reset hareket aralığı
                    maxKneeAngleThisJump = kneeAngle
                    minKneeAngleThisJump = kneeAngle
                }

                // Hava süresini kontrol et (güvenlik)
                val airTime = currentTime - jumpStartTime
                if (airTime > MAX_AIRTIME_MS) {
                    Log.w(TAG, "⚠️ Air time too long, resetting")
                    jumpState = JumpState.FLEXED
                }
            }

            JumpState.TRANSITIONING -> {
                // Geçiş durumu (şu an kullanılmıyor)
            }
        }

        return newRep
    }

    /**
     * Form kalitesi
     */
    private fun calculateFormQuality(rightAnkle: PoseLandmark, leftAnkle: PoseLandmark): Float {
        var qualityScore = 1.0f

        // ========================================
        // İKİ AYAK EŞİT Mİ?
        // ========================================

        val ankleHeightDiff = Math.abs(rightAnkle.position.y - leftAnkle.position.y)
        if (ankleHeightDiff > 0.05f) {  // %5'ten fazla fark
            qualityScore -= 0.3f  // Tek ayakla atladı
        }

        // ========================================
        // ⚡ BÖLÜM B: AÇI DEĞİŞİMİ YETERLİ Mİ?
        // ========================================

        val angleChange = maxKneeAngleThisJump - minKneeAngleThisJump
        if (angleChange < MIN_ANGLE_CHANGE * 1.5f) {  // İdeal: minChange'in 1.5 katı
            qualityScore -= 0.2f  // Çok az hareket
        }

        return qualityScore.coerceIn(0f, 1f)
    }

    /**
     * Feedback
     */
    private fun updateFeedback() {
        currentFeedback = when (jumpState) {
            JumpState.FLEXED -> "⏸️ Zıplayın! (Diz: ${currentKneeAngle.toInt()}°)"
            JumpState.EXTENDED -> "🚀 Havada! (Diz: ${currentKneeAngle.toInt()}°)"
            JumpState.TRANSITIONING -> "🔄 Geçiş"
        }

        // Form uyarısı ekle
        if (currentFormQuality < 0.6f) {
            currentFeedback += " ⚠️ İki ayakla atlayın!"
        }
    }

    // ========================================
    // GETTER'LAR
    // ========================================

    override fun getFeedback(): String = currentFeedback

    override fun getFormQuality(): Float = currentFormQuality

    fun getCurrentJumpState(): JumpState = jumpState

    fun getCurrentKneeAngle(): Float = currentKneeAngle

    fun getCurrentHipAngle(): Float = currentHipAngle

    /**
     * Reset override - açı state'lerini de sıfırla
     */
    override fun reset() {
        super.reset()
        jumpState = JumpState.FLEXED
        currentKneeAngle = 180f
        currentHipAngle = 180f
        maxKneeAngleThisJump = 0f
        minKneeAngleThisJump = 180f
        smoothedKneeAngle = null
        smoothedHipAngle = null
        jumpStartTime = 0L
        landingTime = 0L
    }
}
