package com.example.isekaikuroshin.utils

import kotlin.math.abs

/**
 * One Euro Filter - Real-time Smoothing Algorithm
 *
 * Orijinal Paper: "1€ Filter: A Simple Speed-based Low-pass Filter for Noisy Input in Interactive Systems"
 * (Géry Casiez, Nicolas Roussel, Daniel Vogel - CHI 2012)
 *
 * Bu filtre, düşük hızda titremeleri azaltır, yüksek hızda ise gecikmeyi minimize eder.
 * UI tracking için endüstri standardıdır (kullanılan yerler: iOS, Android, WebXR).
 *
 * Parametreler:
 * - minCutoff: Düşük hızda filtering strength (daha düşük = daha smooth, daha fazla lag)
 * - beta: Hız民感度 (daha yüksek = hıza daha duyarlı)
 * - derivativeCutoff: Türev hesaplama için cutoff
 *
 * Kullanım:
 * ```
 * val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.007)
 * val smoothed = filter.filter(rawValue, timestamp)
 * ```
 */
class OneEuroFilter(
    private val minCutoff: Double = 1.0,      // Hz - smooth ne kadar güçlü (önerilen: 0.5-2.0)
    private val beta: Double = 0.007,         // Hız민감度 (önerilen: 0.001-0.01)
    private val derivativeCutoff: Double = 1.0 // Hz - derivative için
) {
    private var lastValue: Double? = null
    private var lastDerivative: Double = 0.0
    private var lastTimestamp: Long = 0L

    /**
     * Tek bir float değeri filtreler
     *
     * @param value Ham (noisy) değer
     * @param timestamp Milisaniye cinsinden zaman damgası
     * @return Yumuşatılmış değer
     */
    fun filter(value: Float, timestamp: Long = System.currentTimeMillis()): Float {
        return filter(value.toDouble(), timestamp).toFloat()
    }

    /**
     * Tek bir double değeri filtreler
     */
    fun filter(value: Double, timestamp: Long = System.currentTimeMillis()): Double {
        // İlk değer - filtre yok
        if (lastValue == null || lastTimestamp == 0L) {
            lastValue = value
            lastTimestamp = timestamp
            return value
        }

        // Zaman farkı hesapla (saniye cinsinden)
        val dt = (timestamp - lastTimestamp) / 1000.0
        if (dt <= 0.0) {
            // Zaman farkı yok veya negatif - son değeri kullan
            return lastValue!!
        }

        lastTimestamp = timestamp

        // 1. Türev hesapla (değişim hızı)
        val derivative = (value - lastValue!!) / dt

        // 2. Türevi smooth et (düşük geçişli filtre)
        val smoothedDerivative = lowPassFilter(
            derivative,
            lastDerivative,
            alpha(dt, derivativeCutoff)
        )
        lastDerivative = smoothedDerivative

        // 3. Hıza dayalı cutoff hesapla
        val cutoff = minCutoff + beta * abs(smoothedDerivative)

        // 4. Ana değeri smooth et
        val smoothedValue = lowPassFilter(
            value,
            lastValue!!,
            alpha(dt, cutoff)
        )

        lastValue = smoothedValue
        return smoothedValue
    }

    /**
     * Düşük geçişli filtre (exponential smoothing)
     */
    private fun lowPassFilter(current: Double, previous: Double, alpha: Double): Double {
        return alpha * current + (1.0 - alpha) * previous
    }

    /**
     * Alpha değeri hesapla (cutoff frequency'den)
     */
    private fun alpha(dt: Double, cutoff: Double): Double {
        val tau = 1.0 / (2.0 * Math.PI * cutoff)
        return 1.0 / (1.0 + tau / dt)
    }

    /**
     * Filtreyi sıfırla (yeni tracking başladığında)
     */
    fun reset() {
        lastValue = null
        lastDerivative = 0.0
        lastTimestamp = 0L
    }
}

/**
 * OneEuroFilter for 2D Points (x, y)
 *
 * Her eksen için ayrı filtre kullanır
 */
class OneEuroFilter2D(
    minCutoff: Double = 1.0,
    beta: Double = 0.007,
    derivativeCutoff: Double = 1.0
) {
    private val xFilter = OneEuroFilter(minCutoff, beta, derivativeCutoff)
    private val yFilter = OneEuroFilter(minCutoff, beta, derivativeCutoff)

    /**
     * 2D noktayı filtreler
     */
    fun filter(x: Float, y: Float, timestamp: Long = System.currentTimeMillis()): Pair<Float, Float> {
        val smoothX = xFilter.filter(x, timestamp)
        val smoothY = yFilter.filter(y, timestamp)
        return Pair(smoothX, smoothY)
    }

    /**
     * Filtreyi sıfırla
     */
    fun reset() {
        xFilter.reset()
        yFilter.reset()
    }
}

/**
 * Simpler EMA (Exponential Moving Average) Filter
 *
 * OneEuroFilter'dan daha basit ama hızlanırken gecikme olur.
 * Parametre tuning daha kolay.
 *
 * @param alpha Smoothing faktörü (0.0-1.0)
 *   - 0.1 = çok smooth, yüksek gecikme
 *   - 0.5 = orta
 *   - 0.9 = az smooth, düşük gecikme
 */
class EMAFilter(private val alpha: Float = 0.3f) {
    private var lastValue: Float? = null

    fun filter(value: Float): Float {
        if (lastValue == null) {
            lastValue = value
            return value
        }

        val smoothed = alpha * value + (1f - alpha) * lastValue!!
        lastValue = smoothed
        return smoothed
    }

    fun reset() {
        lastValue = null
    }
}

/**
 * EMAFilter for 2D Points
 */
class EMAFilter2D(alpha: Float = 0.3f) {
    private val xFilter = EMAFilter(alpha)
    private val yFilter = EMAFilter(alpha)

    fun filter(x: Float, y: Float): Pair<Float, Float> {
        return Pair(xFilter.filter(x), yFilter.filter(y))
    }

    fun reset() {
        xFilter.reset()
        yFilter.reset()
    }
}
