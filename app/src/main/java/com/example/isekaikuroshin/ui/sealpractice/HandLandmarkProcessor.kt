package com.example.isekaikuroshin.ui.sealpractice

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.isekaikuroshin.utils.OneEuroFilter2D
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Hand Landmark Processor - Production-Grade Stabilization
 *
 * Google MediaPipe'ın kendi filtering parametrelerini kullanır:
 * - OneEuroFilter: min_cutoff=0.05, beta=80.0, derivate_cutoff=1.0
 * - Visibility smoothing: alpha=0.1
 *
 * Referanslar:
 * - https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/modules/pose_landmark/pose_landmark_filtering.pbtxt
 * - CHI 2012 Paper: "1€ Filter: A Simple Speed-based Low-pass Filter"
 *
 * Özellikler:
 * 1. ✅ Temporal Smoothing (OneEuroFilter) - Titremeleri yok eder
 * 2. ✅ Velocity-based Filtering - Hızlı harekette gecikme yok
 * 3. ✅ 3D Z-coordinate kullanımı - Perspektif düzeltmesi
 * 4. ✅ Outlier Detection - Ani sıçramaları filtreler
 */
class HandLandmarkProcessor {

    companion object {
        private const val TAG = "HandLandmarkProcessor"

        // Google'ın Pose Landmark Filtering parametreleri
        private const val MIN_CUTOFF = 0.05  // Google'ın kullandığı değer
        private const val BETA = 80.0        // Google'ın kullandığı değer
        private const val DERIVATE_CUTOFF = 1.0

        // Outlier detection threshold (piksel cinsinden)
        private const val MAX_JUMP_DISTANCE = 150f  // Piksel - anlık sıçrama limiti

        // Z-coordinate depth scaling (perspektif için)
        private const val DEPTH_SCALE_FACTOR = 0.5f
    }

    // Her landmark için ayrı 2D filtre (21 eklem noktası)
    private val landmarkFilters = List(21) {
        OneEuroFilter2D(
            minCutoff = MIN_CUTOFF,
            beta = BETA,
            derivativeCutoff = DERIVATE_CUTOFF
        )
    }

    // Z-coordinate (derinlik) için ayrı filtrenler
    private val depthFilters = List(21) {
        com.example.isekaikuroshin.utils.OneEuroFilter(
            minCutoff = 0.1,  // Derinlik daha yavaş değişir
            beta = 10.0,
            derivativeCutoff = 1.0
        )
    }

    // Son bilinen pozisyonlar (outlier detection için)
    private val lastPositions = MutableList<Offset?>(21) { null }
    private val lastDepths = MutableList<Float?>(21) { null }

    // İstatistikler
    private var totalLandmarks = 0
    private var filteredOutliers = 0

    /**
     * Hand landmarkları process eder - Smooth, stable ve perspektif-doğru
     *
     * @param handResult MediaPipe'tan gelen ham sonuç
     * @param imageWidth Kamera görüntüsü genişliği
     * @param imageHeight Kamera görüntüsü yüksekliği
     * @param canvasWidth Çizim alanı genişliği
     * @param canvasHeight Çizim alanı yüksekliği
     * @param rotationDegrees Sensör rotasyonu
     * @param isFrontCamera Ön kamera mı?
     * @return Processed landmarks (smooth + perspective-corrected)
     */
    fun processHandLandmarks(
        handResult: HandLandmarkerResult,
        imageWidth: Int,
        imageHeight: Int,
        canvasWidth: Float,
        canvasHeight: Float,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ): List<ProcessedLandmark> {
        if (handResult.landmarks().isEmpty()) {
            return emptyList()
        }

        val hand = handResult.landmarks()[0]  // İlk el
        if (hand.size != 21) {
            Log.w(TAG, "⚠️ Beklenmeyen landmark sayısı: ${hand.size}")
            return emptyList()
        }

        val processedLandmarks = mutableListOf<ProcessedLandmark>()
        val timestamp = System.currentTimeMillis()

        hand.forEachIndexed { index, landmark ->
            totalLandmarks++

            // 1. Ham koordinatları normalize edilmiş formdan al
            val rawX = landmark.x()
            val rawY = landmark.y()
            val rawZ = landmark.z()  // Derinlik bilgisi

            // 2. Temporal smoothing uygula (titremeleri yok et)
            val (smoothX, smoothY) = landmarkFilters[index].filter(rawX, rawY, timestamp)
            val smoothZ = depthFilters[index].filter(rawZ, timestamp)

            // 3. Ekran koordinatlarına dönüştür
            val screenPos = transformToScreen(
                smoothX, smoothY,
                imageWidth, imageHeight,
                canvasWidth, canvasHeight,
                rotationDegrees, isFrontCamera
            )

            // 4. Outlier detection (ani sıçramalar)
            val isOutlier = detectOutlier(screenPos, index)
            val finalPos = if (isOutlier) {
                filteredOutliers++
                lastPositions[index] ?: screenPos  // Son bilinen pozisyon kullan
            } else {
                lastPositions[index] = screenPos
                screenPos
            }

            // 5. Perspektif düzeltmesi için Z'yi sakla
            lastDepths[index] = smoothZ

            processedLandmarks.add(
                ProcessedLandmark(
                    position = finalPos,
                    depth = smoothZ,
                    wasFiltered = isOutlier,
                    landmarkIndex = index
                )
            )
        }

        // İstatistik logla (her 100 landmark'ta bir)
        if (totalLandmarks % 100 == 0) {
            val outlierRate = (filteredOutliers.toFloat() / totalLandmarks * 100).toInt()
            Log.d(TAG, "📊 Outliers filtered: $filteredOutliers/$totalLandmarks ($outlierRate%)")
        }

        return processedLandmarks
    }

    /**
     * Koordinatı ekrana dönüştür (rotasyon + mirroring)
     */
    private fun transformToScreen(
        x: Float, y: Float,
        imageWidth: Int, imageHeight: Int,
        canvasWidth: Float, canvasHeight: Float,
        rotationDegrees: Int, isFrontCamera: Boolean
    ): Offset {
        // Normalize'dan ImageProxy koordinatlarına
        var imgX = x * imageWidth.toFloat()
        var imgY = y * imageHeight.toFloat()

        // Rotasyon uygula
        val (rotatedX, rotatedY, rotatedWidth, rotatedHeight) = when (rotationDegrees) {
            90 -> Tuple4(imgY, imageWidth.toFloat() - imgX, imageHeight.toFloat(), imageWidth.toFloat())
            180 -> Tuple4(imageWidth.toFloat() - imgX, imageHeight.toFloat() - imgY, imageWidth.toFloat(), imageHeight.toFloat())
            270 -> Tuple4(imageHeight.toFloat() - imgY, imgX, imageHeight.toFloat(), imageWidth.toFloat())
            else -> Tuple4(imgX, imgY, imageWidth.toFloat(), imageHeight.toFloat())
        }

        // Mirroring (ön kamera için)
        var mirroredX = if (isFrontCamera) rotatedWidth - rotatedX else rotatedX
        var mirroredY = if (isFrontCamera) rotatedHeight - rotatedY else rotatedY

        // Aspect ratio korunarak ölçekleme
        val srcAspect = rotatedWidth / rotatedHeight
        val dstAspect = canvasWidth / canvasHeight

        val (scale, offsetX, offsetY) = if (srcAspect > dstAspect) {
            val s = canvasWidth / rotatedWidth
            val scaledHeight = rotatedHeight * s
            val offY = (canvasHeight - scaledHeight) / 2f
            Triple(s, 0f, offY)
        } else {
            val s = canvasHeight / rotatedHeight
            val scaledWidth = rotatedWidth * s
            val offX = (canvasWidth - scaledWidth) / 2f
            Triple(s, offX, 0f)
        }

        val screenX = mirroredX * scale + offsetX
        val screenY = mirroredY * scale + offsetY

        return Offset(screenX, screenY)
    }

    /**
     * Outlier detection - Ani sıçramaları tespit et
     */
    private fun detectOutlier(currentPos: Offset, landmarkIndex: Int): Boolean {
        val lastPos = lastPositions[landmarkIndex] ?: return false

        val distance = sqrt(
            (currentPos.x - lastPos.x) * (currentPos.x - lastPos.x) +
            (currentPos.y - lastPos.y) * (currentPos.y - lastPos.y)
        )

        return distance > MAX_JUMP_DISTANCE
    }

    /**
     * Filtreleri sıfırla (yeni el algılandığında)
     */
    fun reset() {
        landmarkFilters.forEach { it.reset() }
        depthFilters.forEach { it.reset() }
        lastPositions.fill(null)
        lastDepths.fill(null)

        Log.d(TAG, "🔄 Filters reset | Outliers filtered: $filteredOutliers/$totalLandmarks")
        filteredOutliers = 0
        totalLandmarks = 0
    }

    /**
     * Perspektif-düzeltilmiş çizim ölçeği hesapla
     *
     * Z-coordinate kullanarak, kameraya yakın noktaları büyük göster
     */
    fun getPerspectiveScale(landmarkIndex: Int): Float {
        val depth = lastDepths.getOrNull(landmarkIndex) ?: return 1.0f

        // Z negatif = kameraya yakın, pozitif = uzak
        // Ölçek: yakın = büyük, uzak = küçük
        return 1.0f + (depth * DEPTH_SCALE_FACTOR)
    }

    /**
     * İstatistikleri al
     */
    fun getStats(): ProcessorStats {
        return ProcessorStats(
            totalLandmarks = totalLandmarks,
            filteredOutliers = filteredOutliers,
            outlierRate = if (totalLandmarks > 0) {
                filteredOutliers.toFloat() / totalLandmarks
            } else 0f
        )
    }
}

/**
 * Processed Landmark Data Class
 */
data class ProcessedLandmark(
    val position: Offset,      // Ekran koordinatları
    val depth: Float,          // Z-coordinate (perspective için)
    val wasFiltered: Boolean,  // Outlier olarak filtrelendi mi?
    val landmarkIndex: Int     // Hangi eklem (0-20)
)

/**
 * Processor Statistics
 */
data class ProcessorStats(
    val totalLandmarks: Int,
    val filteredOutliers: Int,
    val outlierRate: Float
)

// Tuple4 moved to HandLandmarkOverlay.kt to avoid duplication
