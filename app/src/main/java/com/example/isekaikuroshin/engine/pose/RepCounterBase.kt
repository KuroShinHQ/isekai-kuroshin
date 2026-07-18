package com.example.isekaikuroshin.engine.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Tekrar Sayacı - Soyut Base Sınıfı
 *
 * Her egzersiz türü için özel bir RepCounter implementasyonu yazılır.
 * Bu sınıf, tüm sayaçların ortak davranışlarını tanımlar.
 *
 * KULLANIM:
 * - PushUpRepCounter: Şınav sayma
 * - SitUpRepCounter: Mekik sayma
 * - RopeSkippingRepCounter: İp atlama sayma
 *
 * TEKRAR SAYMA MANTIGI:
 * - Durum Makinesi: UNKNOWN -> DOWN -> UP -> DOWN (1 tekrar)
 * - Her egzersiz kendi aralıklarını tanımlar (örn: şınav için 90°-160°)
 *
 * KALIBRASYON DESTEĞI:
 * - Kullanıcı "üst" ve "alt" pozisyonları kaydedebilir
 * - Sayaç bu özel pozisyonlara göre çalışır
 */
abstract class RepCounterBase {

    // ========================================
    // STATE (Tekrar Sayma Durumu)
    // ========================================

    /**
     * Egzersiz Durumu
     */
    enum class ExerciseState {
        UNKNOWN,        // Başlangıç durumu veya pose algılanamadı
        DOWN,           // Alt pozisyon (örn: şınav aşağıda)
        UP,             // Üst pozisyon (örn: şınav yukarıda)
        TRANSITIONING   // Geçiş (opsiyonel, smooth geçişler için)
    }

    var currentState: ExerciseState = ExerciseState.UNKNOWN
        protected set
    var repCount: Int = 0
        protected set
    protected var lastTransitionTime: Long = 0

    // Debounce - Hızlı geçişleri engelle (milisaniye)
    protected val MIN_TRANSITION_DELAY_MS = 500L

    // ========================================
    // KALIBRASYON (Opsiyonel)
    // ========================================

    /**
     * Kullanıcının kalibre ettiği pozisyonlar
     * Eğer null ise, varsayılan açı aralıkları kullanılır
     */
    data class CalibratedPose(
        val upperPositionAngles: Map<String, Float>,  // Üst pozisyon (örn: kollar uzanmış)
        val lowerPositionAngles: Map<String, Float>   // Alt pozisyon (örn: kollar bükülmüş)
    )

    protected var calibratedPose: CalibratedPose? = null

    open fun setCalibration(calibration: CalibratedPose) {
        this.calibratedPose = calibration
    }

    fun clearCalibration() {
        this.calibratedPose = null
    }

    // ========================================
    // ABSTRACT METHODS (Her egzersiz implement eder)
    // ========================================

    /**
     * Pose'dan tekrar sayma
     *
     * @param pose ML Kit Pose objesi
     * @return true ise yeni bir tekrar sayıldı
     */
    abstract fun processPose(pose: Pose): Boolean

    /**
     * Anlık feedback metni
     *
     * Kullanıcıya gösterilecek (örn: "Kollarınızı düzleştirin")
     */
    abstract fun getFeedback(): String

    /**
     * Form kalitesi skoru (0.0 - 1.0)
     *
     * Mükemmel form = 1.0, Kötü form = 0.0
     */
    abstract fun getFormQuality(): Float

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * İki landmark arasındaki açıyı hesapla
     *
     * @param a İlk nokta (örn: omuz)
     * @param b Orta nokta (örn: dirsek)
     * @param c Son nokta (örn: bilek)
     * @return Açı (derece cinsinden)
     */
    protected fun calculateAngle(
        a: PoseLandmark?,
        b: PoseLandmark?,
        c: PoseLandmark?
    ): Float? {
        if (a == null || b == null || c == null) return null

        val ax = a.position.x
        val ay = a.position.y
        val bx = b.position.x
        val by = b.position.y
        val cx = c.position.x
        val cy = c.position.y

        // Vektörler: BA ve BC
        val ba = Pair(ax - bx, ay - by)
        val bc = Pair(cx - bx, cy - by)

        // Dot product ve magnitude
        val dot = ba.first * bc.first + ba.second * bc.second
        val magBA = kotlin.math.sqrt(ba.first * ba.first + ba.second * ba.second)
        val magBC = kotlin.math.sqrt(bc.first * bc.first + bc.second * bc.second)

        // Açı hesaplama (radyan -> derece)
        val cosAngle = dot / (magBA * magBC)
        val angleRad = kotlin.math.acos(cosAngle.coerceIn(-1f, 1f))
        return Math.toDegrees(angleRad.toDouble()).toFloat()
    }

    /**
     * Durum geçişi için debounce kontrolü
     */
    protected fun canTransition(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTransitionTime) >= MIN_TRANSITION_DELAY_MS
    }

    /**
     * Durum geçişini kaydet
     */
    protected fun recordTransition() {
        lastTransitionTime = System.currentTimeMillis()
    }

    /**
     * Sayacı sıfırla
     */
    open fun reset() {
        currentState = ExerciseState.UNKNOWN
        repCount = 0
        lastTransitionTime = 0
    }

    /**
     * Vücut parçasının Y eksenindeki pozisyonu
     * (İp atlama gibi dikey hareketler için)
     */
    protected fun getVerticalPosition(landmark: PoseLandmark?): Float? {
        return landmark?.position?.y
    }

    /**
     * İki landmark arasındaki mesafe
     */
    protected fun getDistance(a: PoseLandmark?, b: PoseLandmark?): Float? {
        if (a == null || b == null) return null

        val dx = a.position.x - b.position.x
        val dy = a.position.y - b.position.y
        val dz = a.position3D.z - b.position3D.z

        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}
