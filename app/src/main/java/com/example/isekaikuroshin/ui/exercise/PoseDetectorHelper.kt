package com.example.isekaikuroshin.ui.exercise

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ML Kit Pose Detection Helper Class - GELİŞTİRİLMİŞ VERSİYON
 *
 * Bu sınıf, Google ML Kit'in Pose Detection özelliğini kullanarak
 * kamera görüntülerinden insan duruşlarını (pose) tespit eder.
 *
 * YENİ ÖZELLİKLER (Web Araştırması Sonuçları):
 * - Frame Throttling: Çok hızlı frame'leri otomatik düşürür
 * - Backpressure Strategy: KEEP_ONLY_LATEST mantığı
 * - Performance Metrics: FPS ve frame drop tracking
 * - Min Resolution Check: 480x360 minimum çözünürlük
 * - Frame Skipping: İşlem süresi uzunsa frame atla
 *
 * Araştırma Kaynakları:
 * - Google ML Kit Documentation: "Use STREAM_MODE for best framerates (30-45 fps)"
 * - CameraX Best Practices: "STRATEGY_KEEP_ONLY_LATEST prevents queue buildup"
 * - MediaPipe FlowLimiter: "max_in_flight: 1 for reduced latency"
 */
class PoseDetectorHelper(context: Context) {

    private val poseDetector: PoseDetector

    // BEST PRACTICE #1: Frame Throttling (Processing flag)
    // Araştırma: "Throttle calls to the detector and drop frames if busy"
    private val isProcessing = AtomicBoolean(false)

    // BEST PRACTICE #2: Performance Metrics
    private var frameCount = 0L
    private var droppedFrameCount = 0L
    private var lastFpsCalculationTime = System.currentTimeMillis()
    private var currentFps = 0f

    // BEST PRACTICE #3: Frame Skip Configuration
    private val minProcessingIntervalMs = 33L  // ~30 FPS max (araştırma: "30-45 fps achievable")
    private var lastProcessingTime = 0L

    // BEST PRACTICE #4: Resolution Validation
    // Araştırma: "Use images with dimensions of at least 480x360 pixels"
    private val minWidth = 480
    private val minHeight = 360

    init {
        // BEST PRACTICE: STREAM_MODE for real-time performance
        // Araştırma: "STREAM_MODE provides best framerates for real-time"
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()

        poseDetector = PoseDetection.getClient(options)

        Log.d(TAG, "✅ PoseDetector başlatıldı (STREAM_MODE) - Optimized for 30-45 FPS")
    }

    /**
     * BEST PRACTICE: Frame Throttling ve Backpressure Management
     *
     * Araştırma bulgularından:
     * - "Drop frames if a new frame becomes available while detector is running"
     * - "Backpressure strategy KEEP_ONLY_LATEST prevents queue buildup"
     * - "FlowLimiterCalculator limits in-flight images to 1"
     */
    @androidx.camera.core.ExperimentalGetImage
    fun detectPoseBlocking(imageProxy: ImageProxy): Pose? {
        // BEST PRACTICE #1: Check if already processing (Frame Throttling)
        if (isProcessing.get()) {
            droppedFrameCount++
            // Log.d(TAG, "⏭️ Frame dropped (detector busy) - Total dropped: $droppedFrameCount")
            return null  // Drop frame if busy
        }

        // BEST PRACTICE #2: Time-based Frame Skipping
        val currentTime = System.currentTimeMillis()
        val timeSinceLastProcess = currentTime - lastProcessingTime
        if (timeSinceLastProcess < minProcessingIntervalMs) {
            droppedFrameCount++
            // Log.d(TAG, "⏭️ Frame skipped (too fast) - Min interval: ${minProcessingIntervalMs}ms")
            return null  // Skip frame (too fast)
        }

        val mediaImage = imageProxy.image ?: run {
            Log.w(TAG, "⚠️ MediaImage null, frame atlandı")
            return null
        }

        // BEST PRACTICE #3: Resolution Validation
        // Araştırma: "Minimum 480x360 pixels for optimal performance"
        if (mediaImage.width < minWidth || mediaImage.height < minHeight) {
            Log.w(TAG, "⚠️ Resolution too low: ${mediaImage.width}x${mediaImage.height} (min: ${minWidth}x${minHeight})")
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        return try {
            // Set processing flag (Backpressure management)
            isProcessing.set(true)
            lastProcessingTime = currentTime

            // Blocking call using CountDownLatch
            var result: Pose? = null
            val latch = java.util.concurrent.CountDownLatch(1)

            poseDetector.process(inputImage)
                .addOnSuccessListener { pose ->
                    result = pose
                    latch.countDown()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Pose detection failed: ${e.message}")
                    latch.countDown()
                }

            // Wait for completion (max 1 second timeout)
            latch.await(1, java.util.concurrent.TimeUnit.SECONDS)

            // Performance tracking
            frameCount++
            updateFpsMetrics()

            // Smart cast için local değişken kullan
            val finalResult = result
            if (finalResult != null && finalResult.allPoseLandmarks.isNotEmpty()) {
                // Log her 30 frame'de bir (spam önleme)
                if (frameCount % 30 == 0L) {
                    Log.d(TAG, "✅ Pose OK | FPS: ${currentFps.toInt()} | Dropped: $droppedFrameCount")
                }
            }

            finalResult
        } catch (e: Exception) {
            Log.e(TAG, "❌ Pose detection hatası: ${e.message}", e)
            null
        } finally {
            // CRITICAL: Release processing flag
            isProcessing.set(false)
        }
    }

    /**
     * BEST PRACTICE: FPS Monitoring
     *
     * Gerçek zamanlı performans takibi
     */
    private fun updateFpsMetrics() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastFpsCalculationTime

        // Her 1 saniyede bir FPS hesapla
        if (elapsedTime >= 1000) {
            currentFps = (frameCount.toFloat() / elapsedTime) * 1000f
            lastFpsCalculationTime = currentTime
            frameCount = 0

            // Performance warning (araştırma: "30-45 fps achievable")
            if (currentFps < 20f) {
                Log.w(TAG, "⚠️ Low FPS detected: ${currentFps.toInt()} FPS (target: 30+ FPS)")
            }
        }
    }

    /**
     * Kaynakları serbest bırak
     */
    fun close() {
        poseDetector.close()
        Log.d(TAG, "🔄 PoseDetector kapatıldı")
        Log.d(TAG, "📊 Final Stats - FPS: ${currentFps.toInt()} | Dropped Frames: $droppedFrameCount")
    }

    /**
     * Performance metrics
     */
    fun getCurrentFps(): Float = currentFps
    fun getDroppedFrameCount(): Long = droppedFrameCount

    /**
     * Reset statistics
     */
    fun resetStats() {
        frameCount = 0
        droppedFrameCount = 0
        lastFpsCalculationTime = System.currentTimeMillis()
        currentFps = 0f
        Log.d(TAG, "📊 Performance stats reset")
    }

    companion object {
        private const val TAG = "PoseDetectorHelper"
    }
}
