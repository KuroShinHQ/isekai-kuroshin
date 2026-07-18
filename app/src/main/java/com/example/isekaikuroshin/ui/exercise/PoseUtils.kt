package com.example.isekaikuroshin.ui.exercise

import android.graphics.PointF
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.abs

/**
 * Pose Estimation Yardımcı Fonksiyonlar
 *
 * Bu dosya, pose detection için gerekli matematiksel hesaplamaları içerir.
 */

/**
 * İki vektör arasındaki açıyı hesaplar (derece cinsinden)
 *
 * @param p1 İlk nokta (örn: omuz)
 * @param p2 Orta nokta (örn: dirsek) - köşe noktası
 * @param p3 Üçüncü nokta (örn: bilek)
 * @return Açı (0-180 derece arası)
 */
fun calculateAngle(p1: PointF, p2: PointF, p3: PointF): Float {
    // Vektörleri hesapla
    val vector1 = PointF(p1.x - p2.x, p1.y - p2.y)
    val vector2 = PointF(p3.x - p2.x, p3.y - p2.y)

    // İç çarpım (dot product)
    val dotProduct = vector1.x * vector2.x + vector1.y * vector2.y

    // Vektör uzunlukları
    val magnitude1 = sqrt(vector1.x * vector1.x + vector1.y * vector1.y)
    val magnitude2 = sqrt(vector2.x * vector2.x + vector2.y * vector2.y)

    // Sıfıra bölme kontrolü
    if (magnitude1 == 0f || magnitude2 == 0f) {
        return 0f
    }

    // Açıyı hesapla
    val cosAngle = (dotProduct / (magnitude1 * magnitude2)).coerceIn(-1f, 1f)
    val angleRad = acos(cosAngle)

    // Radyandan dereceye çevir
    return Math.toDegrees(angleRad.toDouble()).toFloat()
}

/**
 * İki nokta arasındaki mesafeyi hesaplar
 */
fun calculateDistance(p1: PointF, p2: PointF): Float {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    return sqrt(dx * dx + dy * dy)
}

/**
 * Üç nokta arasındaki doğrusallık (collinearity) hesaplar
 * Düz bir çizgi oluşturup oluşturmadıklarını kontrol eder
 *
 * @return 0'a yakın değer = çizgi düz, yüksek değer = eğri/bükük
 */
fun calculateCollinearity(p1: PointF, p2: PointF, p3: PointF): Float {
    // Üçgenin alanını hesapla (düz çizgide alan = 0)
    val area = abs(
        p1.x * (p2.y - p3.y) +
        p2.x * (p3.y - p1.y) +
        p3.x * (p1.y - p2.y)
    ) / 2f

    // Normalize et (uzaklıkla böl)
    val distance = calculateDistance(p1, p3)
    return if (distance > 0) area / distance else 0f
}

/**
 * Bir noktanın iki nokta arasındaki çizgiye olan dikey uzaklığını hesaplar
 * Vücut hizalaması kontrolü için kullanılır
 */
fun pointToLineDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Float {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y

    if (dx == 0f && dy == 0f) return calculateDistance(point, lineStart)

    val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / (dx * dx + dy * dy)

    val projectedPoint = if (t < 0) {
        lineStart
    } else if (t > 1) {
        lineEnd
    } else {
        PointF(lineStart.x + t * dx, lineStart.y + t * dy)
    }

    return calculateDistance(point, projectedPoint)
}

/**
 * Gelişmiş Hareketli Ortalama Filtresi (Exponential Moving Average)
 *
 * GitHub best practices'den alınan smoothing algoritması
 * Ani değişimleri filtrelerken gerçek hareketlere duyarlı kalır
 */
class SmoothingFilter(
    private val windowSize: Int = 5,
    private val alpha: Float = 0.3f  // EMA weight (0-1, düşük = daha smooth)
) {
    private val angleHistory = mutableListOf<Float>()
    private var emaValue: Float? = null

    /**
     * Simple Moving Average (SMA) ile başla
     */
    fun addAngle(angle: Float): Float {
        angleHistory.add(angle)
        if (angleHistory.size > windowSize) {
            angleHistory.removeAt(0)
        }

        // EMA (Exponential Moving Average) hesapla
        emaValue = if (emaValue == null) {
            // İlk değer: basit ortalama
            angleHistory.average().toFloat()
        } else {
            // EMA formülü: EMA_new = alpha * value + (1 - alpha) * EMA_old
            alpha * angle + (1 - alpha) * emaValue!!
        }

        return emaValue!!
    }

    /**
     * Filtre geçmişini temizle
     */
    fun reset() {
        angleHistory.clear()
        emaValue = null
    }

    /**
     * Mevcut filtered değeri döndür
     */
    fun getCurrentValue(): Float? = emaValue
}

/**
 * Hysteresis (Eşik Farklılaştırma) Sınıfı
 *
 * Eşik değerleri etrafında zıplama (bouncing) önler
 * GitHub best practices'den alınan teknik
 */
class HysteresisThreshold(
    private val lowThreshold: Float,
    private val highThreshold: Float
) {
    private var currentState: Boolean = false

    /**
     * Değeri kontrol et ve durum döndür
     *
     * @param value Kontrol edilecek değer
     * @param highMeansActive true ise yüksek değer = aktif (UP state)
     * @return true ise threshold geçildi
     */
    fun check(value: Float, highMeansActive: Boolean = true): Boolean {
        currentState = if (highMeansActive) {
            // Yüksek değer = aktif (örn: kol yukarıda)
            if (value > highThreshold) true
            else if (value < lowThreshold) false
            else currentState  // Ara değerlerde mevcut durumu koru
        } else {
            // Düşük değer = aktif (örn: kol aşağıda)
            if (value < lowThreshold) true
            else if (value > highThreshold) false
            else currentState
        }
        return currentState
    }

    fun reset() {
        currentState = false
    }

    fun getCurrentState(): Boolean = currentState
}

/**
 * Temporal (Zaman Bazlı) Hareket Validator
 *
 * Çok hızlı veya çok yavaş hareketleri filtreler
 * MediaPipe best practices'den alınan yaklaşım
 */
class MovementValidator(
    private val minDurationMs: Long = 300,  // Minimum tekrar süresi (ms)
    private val maxDurationMs: Long = 5000  // Maksimum tekrar süresi (ms)
) {
    private var phaseStartTime: Long = 0

    /**
     * Hareket fazını başlat
     */
    fun startPhase() {
        phaseStartTime = System.currentTimeMillis()
    }

    /**
     * Hareketin geçerli bir sürede tamamlanıp tamamlanmadığını kontrol et
     *
     * @return true ise hareket geçerli sürede tamamlandı
     */
    fun isValidMovement(): Boolean {
        if (phaseStartTime == 0L) return false

        val elapsed = System.currentTimeMillis() - phaseStartTime
        return elapsed in minDurationMs..maxDurationMs
    }

    /**
     * Geçen süreyi ms cinsinden döndür
     */
    fun getElapsedTime(): Long {
        return if (phaseStartTime == 0L) 0
        else System.currentTimeMillis() - phaseStartTime
    }

    fun reset() {
        phaseStartTime = 0
    }
}
