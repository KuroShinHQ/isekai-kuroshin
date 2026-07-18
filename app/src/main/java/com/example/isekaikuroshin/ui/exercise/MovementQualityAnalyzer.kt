package com.example.isekaikuroshin.ui.exercise

import android.graphics.PointF
import android.util.Log
import kotlin.math.sqrt

/**
 * MOVEMENT QUALITY ANALYZER
 *
 * Hareket kalitesini hız (velocity) ve ivme (acceleration) analiziyle değerlendirir.
 * Kullanıcının "savurma" (momentum) yapmasını veya çok yavaş hareket etmesini engeller.
 *
 * KAYNAK:
 * - "Rep Counting with Temporal Convolutional Networks" (AI Fitness Papers)
 * - MediaPipe Fitness AI Solutions
 * - Biomechanics research on proper exercise form
 *
 * KULLANIM:
 * ```kotlin
 * val analyzer = MovementQualityAnalyzer()
 * val quality = analyzer.analyzeMovement(currentPos, timestamp)
 * if (quality.isTooFast) {
 *     feedback = "⚠️ Daha Yavaş Hareket Et"
 * }
 * ```
 */
class MovementQualityAnalyzer {

    // ═══ VELOCİTY THRESHOLDS (Hız Eşikleri) ═══
    // Biomechanics research: Sağlıklı şınav 1-2 saniye down, 1-2 saniye up
    // Dirsek maksimum 100 cm/s'den hızlı hareket ederse = momentum kullanıyor

    companion object {
        private const val TAG = "MovementQualityAnalyzer"

        // ═══ PERFORMANCE OPTIMIZATION (FPS İyileştirmesi) ═══
        // Pahalı hesaplamaları her frame yerine her N frame'de bir yap
        private const val ANALYSIS_FRAME_INTERVAL = 5  // Her 5 frame'de bir analiz et (optimized)

        // Hız eşikleri (pixel/second cinsinden)
        // Not: 100 px/s ≈ gerçek dünyada 30-40 cm/s (kamera mesafesine bağlı)
        private const val VELOCITY_TOO_FAST_THRESHOLD = 800f   // > 800 px/s = çok hızlı (savurma)
        private const val VELOCITY_TOO_SLOW_THRESHOLD = 20f    // < 20 px/s = çok yavaş (duraksama)
        private const val VELOCITY_OPTIMAL_MIN = 50f           // Optimal minimum hız
        private const val VELOCITY_OPTIMAL_MAX = 500f          // Optimal maksimum hız

        // İvme eşikleri (pixel/second² cinsinden)
        private const val ACCELERATION_TOO_HIGH_THRESHOLD = 1500f  // Ani hareketler
        private const val JERK_THRESHOLD = 3000f  // Jerk (ivmenin türevi) - yaralanma riski

        // Minimum süre eşikleri (hareket analizi için)
        private const val MIN_TIME_DELTA_MS = 16L  // Minimum 16ms (60 FPS)
    }

    // Movement history (son pozisyonlar ve zamanları)
    private data class MovementFrame(
        val position: PointF,
        val timestamp: Long,
        val velocity: PointF? = null,
        val acceleration: PointF? = null
    )

    private val movementHistory = BoundedDeque<MovementFrame>(maxCapacity = 10)

    // Frame counter for skipping (performans optimizasyonu)
    private var frameCounter = 0

    // Son analiz sonucu (cache - frame skip sırasında döndürülür)
    private var lastQualityResult: MovementQuality? = null

    /**
     * Hareket kalitesi sonucu
     */
    data class MovementQuality(
        val velocity: Float,              // Hız büyüklüğü (px/s)
        val acceleration: Float,          // İvme büyüklüğü (px/s²)
        val isTooFast: Boolean,           // Çok hızlı hareket?
        val isTooSlow: Boolean,           // Çok yavaş hareket?
        val isOptimal: Boolean,           // Optimal hız aralığında?
        val hasHighAcceleration: Boolean, // Yüksek ivme (ani hareket)?
        val qualityScore: Float,          // 0-100 arası kalite skoru
        val feedback: String              // Kullanıcı için geri bildirim
    )

    /**
     * ANA FONKSİYON: Hareket kalitesini analiz et
     *
     * PERFORMANS OPTİMİZASYONU:
     * - Her 3 frame'de bir tam analiz yapar
     * - Diğer frame'lerde cached sonucu döndürür
     * - FPS'i ~13'ten ~30+'a çıkarır
     *
     * @param currentPosition Mevcut eklem pozisyonu (örn: dirsek)
     * @param timestamp Zaman damgası (milliseconds)
     * @return MovementQuality (hız, ivme, kalite skoru vb.)
     */
    fun analyzeMovement(currentPosition: PointF, timestamp: Long): MovementQuality {
        frameCounter++

        // ═══ FRAME SKIPPING (Performans Optimizasyonu) ═══
        // Her N frame'de bir tam analiz yap, diğerlerinde cached sonuç döndür
        if (frameCounter % ANALYSIS_FRAME_INTERVAL != 0 && lastQualityResult != null) {
            // Frame skip - son sonucu döndür (hesaplama yapma)
            return lastQualityResult!!
        }

        // İlk frame - analiz yapılamaz
        if (movementHistory.isEmpty()) {
            val initialFrame = MovementFrame(currentPosition, timestamp)
            movementHistory.addLast(initialFrame)
            val neutralQuality = createNeutralQuality()
            lastQualityResult = neutralQuality
            return neutralQuality
        }

        val previousFrame = movementHistory.last()

        // Zaman farkı çok küçükse (< 16ms) atla
        val timeDelta = timestamp - previousFrame.timestamp
        if (timeDelta < MIN_TIME_DELTA_MS) {
            return createNeutralQuality()
        }

        // ═══ STEP 1: VELOCİTY HESAPLAMA (Hız) ═══
        val velocity = calculateVelocity(
            previousFrame.position,
            currentPosition,
            timeDelta
        )

        val velocityMagnitude = magnitude(velocity)

        // ═══ STEP 2: ACCELERATION HESAPLAMA (İvme) ═══
        val acceleration = if (previousFrame.velocity != null) {
            calculateAcceleration(
                previousFrame.velocity,
                velocity,
                timeDelta
            )
        } else {
            PointF(0f, 0f)
        }

        val accelerationMagnitude = magnitude(acceleration)

        // Yeni frame'i history'ye ekle
        val newFrame = MovementFrame(
            position = currentPosition,
            timestamp = timestamp,
            velocity = velocity,
            acceleration = acceleration
        )
        movementHistory.addLast(newFrame)

        // History boyutunu sınırla (son 10 frame)
        if (movementHistory.size > 10) {
            movementHistory.removeFirst()
        }

        // ═══ STEP 3: KALİTE DEĞERLENDİRMESİ ═══
        val isTooFast = velocityMagnitude > VELOCITY_TOO_FAST_THRESHOLD
        val isTooSlow = velocityMagnitude < VELOCITY_TOO_SLOW_THRESHOLD
        val isOptimal = velocityMagnitude in VELOCITY_OPTIMAL_MIN..VELOCITY_OPTIMAL_MAX
        val hasHighAcceleration = accelerationMagnitude > ACCELERATION_TOO_HIGH_THRESHOLD

        // Kalite skoru (0-100)
        val qualityScore = calculateQualityScore(
            velocityMagnitude,
            accelerationMagnitude,
            isOptimal,
            hasHighAcceleration
        )

        // Kullanıcı geri bildirimi
        val feedback = generateFeedback(isTooFast, isTooSlow, hasHighAcceleration)

        // Debug log (sadece her 30 frame'de bir - log spam önleme)
        if ((isTooFast || hasHighAcceleration) && frameCounter % 30 == 0) {
            Log.d(TAG, "⚠️ Movement Issue: velocity=${velocityMagnitude.toInt()} px/s, " +
                    "accel=${accelerationMagnitude.toInt()} px/s², feedback='$feedback'")
        }

        val result = MovementQuality(
            velocity = velocityMagnitude,
            acceleration = accelerationMagnitude,
            isTooFast = isTooFast,
            isTooSlow = isTooSlow,
            isOptimal = isOptimal,
            hasHighAcceleration = hasHighAcceleration,
            qualityScore = qualityScore,
            feedback = feedback
        )

        // Cache sonucu (sonraki frame skip'lerde kullanılacak)
        lastQualityResult = result
        return result
    }

    /**
     * Hız hesaplama (velocity = Δposition / Δtime)
     */
    private fun calculateVelocity(
        previousPos: PointF,
        currentPos: PointF,
        timeDeltaMs: Long
    ): PointF {
        val timeDeltaSec = timeDeltaMs / 1000f

        return PointF(
            (currentPos.x - previousPos.x) / timeDeltaSec,  // vx (px/s)
            (currentPos.y - previousPos.y) / timeDeltaSec   // vy (px/s)
        )
    }

    /**
     * İvme hesaplama (acceleration = Δvelocity / Δtime)
     */
    private fun calculateAcceleration(
        previousVelocity: PointF,
        currentVelocity: PointF,
        timeDeltaMs: Long
    ): PointF {
        val timeDeltaSec = timeDeltaMs / 1000f

        return PointF(
            (currentVelocity.x - previousVelocity.x) / timeDeltaSec,  // ax (px/s²)
            (currentVelocity.y - previousVelocity.y) / timeDeltaSec   // ay (px/s²)
        )
    }

    /**
     * Vektör büyüklüğü (magnitude)
     */
    private fun magnitude(vector: PointF): Float {
        return sqrt(vector.x * vector.x + vector.y * vector.y)
    }

    /**
     * Kalite skoru hesaplama (0-100 arası)
     *
     * Skorlama mantığı:
     * - Optimal hız aralığı: 100 puan
     * - Hız optimal ama yüksek ivme: 70 puan (ani hareket)
     * - Çok hızlı: 30 puan (momentum kullanıyor)
     * - Çok yavaş: 50 puan (tempo kaybı)
     */
    private fun calculateQualityScore(
        velocity: Float,
        acceleration: Float,
        isOptimal: Boolean,
        hasHighAcceleration: Boolean
    ): Float {
        return when {
            // Mükemmel: Optimal hız + düşük ivme
            isOptimal && !hasHighAcceleration -> 100f

            // İyi: Optimal hız ama biraz yüksek ivme
            isOptimal && hasHighAcceleration -> 70f

            // Orta: Hız biraz yüksek ama kabul edilebilir
            velocity < VELOCITY_TOO_FAST_THRESHOLD * 1.2f -> 60f

            // Kötü: Çok hızlı (momentum)
            velocity > VELOCITY_TOO_FAST_THRESHOLD -> 30f

            // Zayıf: Çok yavaş (tempo kaybı)
            velocity < VELOCITY_TOO_SLOW_THRESHOLD -> 50f

            else -> 75f  // Default: iyi
        }
    }

    /**
     * Kullanıcıya geri bildirim mesajı oluştur
     */
    private fun generateFeedback(
        isTooFast: Boolean,
        isTooSlow: Boolean,
        hasHighAcceleration: Boolean
    ): String {
        return when {
            isTooFast -> "⚠️ Daha Yavaş Hareket Et (Momentum Kullanma)"
            hasHighAcceleration -> "⚠️ Ani Hareketlerden Kaçın"
            isTooSlow -> "💪 Tempoyu Koru"
            else -> "✅ Mükemmel Tempo!"
        }
    }

    /**
     * Nötr kalite (başlangıç veya yetersiz veri)
     */
    private fun createNeutralQuality(): MovementQuality {
        return MovementQuality(
            velocity = 0f,
            acceleration = 0f,
            isTooFast = false,
            isTooSlow = false,
            isOptimal = true,
            hasHighAcceleration = false,
            qualityScore = 75f,
            feedback = "Başlangıç..."
        )
    }

    /**
     * Analyzer'ı sıfırla (yeni egzersiz başladığında)
     */
    fun reset() {
        movementHistory.clear()
        frameCounter = 0
        lastQualityResult = null
        Log.d(TAG, "🔄 Movement analyzer reset")
    }

    /**
     * Ortalama hızı hesapla (son N frame)
     */
    fun getAverageVelocity(): Float {
        if (movementHistory.size < 2) return 0f

        val velocities = movementHistory
            .mapNotNull { it.velocity }
            .map { magnitude(it) }

        return if (velocities.isNotEmpty()) {
            velocities.average().toFloat()
        } else {
            0f
        }
    }

    /**
     * Hareket smooth mu kontrol et (düşük ivme değişimi = smooth)
     */
    fun isMovementSmooth(): Boolean {
        if (movementHistory.size < 3) return true

        val accelerations = movementHistory
            .mapNotNull { it.acceleration }
            .map { magnitude(it) }

        if (accelerations.isEmpty()) return true

        // Acceleration variance (varyans) düşükse = smooth
        val avgAccel = accelerations.average()
        val variance = accelerations.map { (it - avgAccel) * (it - avgAccel) }.average()

        return variance < 500f  // Düşük varyans = smooth hareket
    }
}

/**
 * Circular buffer with max capacity (composition pattern)
 */
private class BoundedDeque<T>(private val maxCapacity: Int) {
    private val deque = ArrayDeque<T>()

    val size: Int get() = deque.size

    fun addLast(element: T) {
        if (deque.size >= maxCapacity) {
            deque.removeFirst()
        }
        deque.addLast(element)
    }

    fun last(): T = deque.last()

    fun isEmpty(): Boolean = deque.isEmpty()

    fun clear() = deque.clear()

    fun removeFirst() = deque.removeFirst()

    fun <R> mapNotNull(transform: (T) -> R?): List<R> = deque.mapNotNull(transform)
}
