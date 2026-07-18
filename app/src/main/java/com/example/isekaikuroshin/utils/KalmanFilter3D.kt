package com.example.isekaikuroshin.utils

import kotlin.math.sqrt

/**
 * Kalman Filter for 3D Point Tracking
 *
 * Bu sınıf, 3D uzaydaki bir noktanın (x, y, z) konumunu ve hızını (vx, vy, vz)
 * tahmin etmek ve ölçümlerle güncellemek için Kalman Filtresi algoritmasını uygular.
 *
 * KULLANIM ALANI:
 * - MediaPipe hand landmark'larının smooth tracking'i
 * - Oklüzyon (occlusion) durumunda eksik landmark'ları tahmin etme
 * - Jitter (titreme) azaltma
 *
 * ARAŞTIRMA KAYNAKLARI:
 * - "Gesture Recognition Improvement of Mediapipe Model Based on Historical Trajectory
 *   Assist Tracking, Kalman Filtering and Smooth Filtering" (ACM 2024)
 * - "Automatic Facial Landmark Tracking in Video Sequences Using Kalman Filter
 *   Assisted Active Shape Models" (Springer)
 * - MediaPipe resmi dokümantasyonu
 *
 * ALGORİTMA:
 * 1. PREDICT: Önceki durum ve hız bilgisine göre yeni pozisyonu tahmin et
 * 2. UPDATE: Kameradan gelen gerçek ölçümle tahmini düzelt
 * 3. OCCLUSION HANDLING: Ölçüm yoksa predict edilen değeri kullanmaya devam et
 *
 * @param processNoise Process noise (Q) - Sistemin belirsizliği (varsayılan: 0.001)
 * @param measurementNoise Measurement noise (R) - Sensör belirsizliği (varsayılan: 0.1)
 */
class KalmanFilter3D(
    private val processNoise: Float = 0.001f,      // Q: Sistem gürültüsü (düşük = smooth, yüksek = responsive)
    private val measurementNoise: Float = 0.1f     // R: Ölçüm gürültüsü (düşük = ölçüme güven, yüksek = tahmine güven)
) {
    // ========================================
    // STATE VECTOR (Durum Vektörü)
    // ========================================
    // 6D state: [x, y, z, vx, vy, vz]
    // x, y, z: Konum
    // vx, vy, vz: Hız (velocity)

    private var x = 0f      // X pozisyonu
    private var y = 0f      // Y pozisyonu
    private var z = 0f      // Z pozisyonu
    private var vx = 0f     // X hızı
    private var vy = 0f     // Y hızı
    private var vz = 0f     // Z hızı

    // ========================================
    // COVARIANCE MATRIX (Kovaryans Matrisi)
    // ========================================
    // P: State estimate covariance (6x6 matrix)
    // Basitleştirilmiş diagonal form kullanıyoruz (full matrix yerine)
    private var px = 1f     // Kovaryans X
    private var py = 1f     // Kovaryans Y
    private var pz = 1f     // Kovaryans Z
    private var pvx = 1f    // Kovaryans VX
    private var pvy = 1f    // Kovaryans VY
    private var pvz = 1f    // Kovaryans VZ

    // ========================================
    // INITIALIZATION FLAG
    // ========================================
    private var initialized = false

    /**
     * PREDICT STEP (Tahmin Adımı)
     *
     * Önceki durum ve hız bilgisine göre yeni pozisyonu tahmin eder.
     * Bu fonksiyon her frame'de çağrılır, ölçüm olsun olmasın.
     *
     * @param dt Delta time (saniye cinsinden) - Genellikle 1/30 veya 1/60 (30fps veya 60fps)
     * @return Tahmin edilen pozisyon (x, y, z)
     */
    fun predict(dt: Float = 1f / 30f): Triple<Float, Float, Float> {
        if (!initialized) {
            // İlk tahmin - henüz veri yok, sıfır döndür
            return Triple(0f, 0f, 0f)
        }

        // ========================================
        // STATE PREDICTION (Durum Tahmini)
        // ========================================
        // x_k = x_{k-1} + vx * dt
        // y_k = y_{k-1} + vy * dt
        // z_k = z_{k-1} + vz * dt
        // vx, vy, vz sabit kalır (basit model)

        x += vx * dt
        y += vy * dt
        z += vz * dt

        // ========================================
        // COVARIANCE PREDICTION (Kovaryans Tahmini)
        // ========================================
        // P_k = P_{k-1} + Q (process noise eklenir)

        px += processNoise
        py += processNoise
        pz += processNoise
        pvx += processNoise
        pvy += processNoise
        pvz += processNoise

        return Triple(x, y, z)
    }

    /**
     * UPDATE STEP (Güncelleme Adımı)
     *
     * Kameradan gelen gerçek ölçümle tahmini düzeltir.
     * Bu fonksiyon sadece geçerli ölçüm olduğunda çağrılır.
     *
     * @param measurementX Ölçülen X koordinatı
     * @param measurementY Ölçülen Y koordinatı
     * @param measurementZ Ölçülen Z koordinatı
     * @param dt Delta time (saniye cinsinden)
     */
    fun update(measurementX: Float, measurementY: Float, measurementZ: Float, dt: Float = 1f / 30f) {
        if (!initialized) {
            // İlk ölçüm - state'i initialize et
            x = measurementX
            y = measurementY
            z = measurementZ
            vx = 0f
            vy = 0f
            vz = 0f
            initialized = true
            return
        }

        // ========================================
        // KALMAN GAIN (Kalman Kazancı)
        // ========================================
        // K = P / (P + R)
        // Ölçümün tahmini ne kadar "düzeltmesi" gerektiğini belirler

        val kx = px / (px + measurementNoise)
        val ky = py / (py + measurementNoise)
        val kz = pz / (pz + measurementNoise)

        // ========================================
        // STATE UPDATE (Durum Güncelleme)
        // ========================================
        // x_k = x_k + K * (measurement - x_k)
        // "Innovation" (yenilik): measurement - x_k

        val innovationX = measurementX - x
        val innovationY = measurementY - y
        val innovationZ = measurementZ - z

        x += kx * innovationX
        y += ky * innovationY
        z += kz * innovationZ

        // ========================================
        // VELOCITY UPDATE (Hız Güncelleme)
        // ========================================
        // Basit hız tahmini: innovation / dt
        // Smooth yapmak için önceki hızla karıştır

        val alpha = 0.7f  // Smoothing factor (0.7 = %70 yeni, %30 eski)

        if (dt > 0f) {
            vx = alpha * (innovationX / dt) + (1f - alpha) * vx
            vy = alpha * (innovationY / dt) + (1f - alpha) * vy
            vz = alpha * (innovationZ / dt) + (1f - alpha) * vz
        }

        // ========================================
        // COVARIANCE UPDATE (Kovaryans Güncelleme)
        // ========================================
        // P_k = (1 - K) * P_k

        px *= (1f - kx)
        py *= (1f - ky)
        pz *= (1f - kz)

        // Kovaryansın çok küçük olmasını engelle (numerical stability)
        px = px.coerceAtLeast(0.001f)
        py = py.coerceAtLeast(0.001f)
        pz = pz.coerceAtLeast(0.001f)
    }

    /**
     * RESET (Sıfırla)
     *
     * Filtrenin tüm iç durumunu sıfırlar.
     * El ekrandan tamamen kaybolup geri geldiğinde çağrılır.
     *
     * SEBEP: Eski hız bilgisi artık geçersiz olduğu için,
     * filtre eski hıza göre hatalı tahminler yapabilir.
     */
    fun reset() {
        x = 0f
        y = 0f
        z = 0f
        vx = 0f
        vy = 0f
        vz = 0f
        px = 1f
        py = 1f
        pz = 1f
        pvx = 1f
        pvy = 1f
        pvz = 1f
        initialized = false
    }

    /**
     * GET STATE (Durumu Al)
     *
     * Filtrenin mevcut tahminini döndürür.
     *
     * @return (x, y, z, vx, vy, vz)
     */
    fun getState(): KalmanState {
        return KalmanState(
            x = x,
            y = y,
            z = z,
            vx = vx,
            vy = vy,
            vz = vz,
            confidence = getConfidence()
        )
    }

    /**
     * GET CONFIDENCE (Güven Skoru)
     *
     * Filtrenin tahmininin ne kadar güvenilir olduğunu döndürür.
     * Düşük kovaryans = yüksek güven
     *
     * @return 0.0-1.0 arası güven skoru
     */
    private fun getConfidence(): Float {
        // Toplam kovaryans (pozisyon için)
        val totalCovariance = px + py + pz

        // Normalize et (0-1 arası)
        // Düşük kovaryans (örn. 0.01) → yüksek güven (0.99)
        // Yüksek kovaryans (örn. 10.0) → düşük güven (0.1)

        return 1f / (1f + totalCovariance)
    }

    /**
     * IS INITIALIZED
     *
     * Filtrenin initialize edilip edilmediğini kontrol eder.
     *
     * @return true ise en az bir update() çağrısı yapılmış
     */
    fun isInitialized(): Boolean = initialized
}

/**
 * Kalman Filter State Data Class
 *
 * Filtrenin mevcut durumunu temsil eder.
 */
data class KalmanState(
    val x: Float,
    val y: Float,
    val z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val confidence: Float  // 0.0-1.0 güven skoru
)
