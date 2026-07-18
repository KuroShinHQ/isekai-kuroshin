package com.example.isekaikuroshin.ui.sealpractice

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MediaPipe Hands Integration for Kadim Mühür Teknikleri
 *
 * BEST PRACTICES (Web Araştırması Sonuçlarından):
 * - LIVE_STREAM mode for real-time (30-45 FPS target)
 * - GPU Delegate for 1.5x speedup (real devices)
 * - CPU Delegate for emulator compatibility
 * - Frame throttling to prevent queue buildup
 * - 21-point hand landmarks per hand
 *
 * Bu sınıf, PoseDetectorHelper'ın aynı pattern'lerini kullanır:
 * - Frame throttling (AtomicBoolean)
 * - Time-based frame skipping
 * - FPS metrics tracking
 * - Dropped frame counting
 *
 * @param context Android context
 * @param isEmulator If true, forces CPU delegate. If false, tries GPU then CPU fallback
 */
class HandDetectorHelper(context: Context, private val isEmulator: Boolean = false) {

    companion object {
        private const val TAG = "HandDetectorHelper"

        init {
            try {
                System.loadLibrary("mediapipe_tasks_vision_jni")
                Log.d(TAG, "✅ Native library loaded explicitly")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ Failed to load native library explicitly: ${e.message}", e)
                // Fallback - MediaPipe might load it automatically
            }
        }
    }

    private var handLandmarker: HandLandmarker? = null

    // Frame throttling (PoseDetectorHelper pattern)
    private val isProcessing = AtomicBoolean(false)
    private val minProcessingIntervalMs = 33L  // ~30 FPS max
    private var lastProcessingTime = 0L

    // Performance metrics
    private var frameCount = 0L
    private var droppedFrameCount = 0L
    private var currentFps = 0f
    private var lastFpsCalculationTime = System.currentTimeMillis()

    // Result channel (Flow pattern for async results)
    private val _results = Channel<HandLandmarkerResult>(Channel.CONFLATED)
    val results: Flow<HandLandmarkerResult> = _results.receiveAsFlow()

    init {
        handLandmarker = if (isEmulator) {
            // EMULATOR MODE: Force CPU delegate for stability
            Log.w(TAG, "🤖 EMULATOR MODE: Forcing CPU delegate for MediaPipe compatibility")
            initializeHandLandmarker(context, useGpu = false)
                ?: throw RuntimeException("❌ HandLandmarker initialization failed with CPU on emulator")
        } else {
            // REAL DEVICE: Try GPU first, fallback to CPU if needed
            Log.d(TAG, "📱 REAL DEVICE: Trying GPU delegate, will fallback to CPU if needed")
            initializeHandLandmarker(context, useGpu = true)
                ?: initializeHandLandmarker(context, useGpu = false)
                ?: throw RuntimeException("❌ HandLandmarker initialization failed with both GPU and CPU")
        }
    }

    /**
     * HandLandmarker'ı initialize et (GPU veya CPU ile)
     *
     * @return HandLandmarker instance veya null (başarısız olursa)
     */
    private fun initializeHandLandmarker(context: Context, useGpu: Boolean): HandLandmarker? {
        return try {
            val delegate = if (useGpu) Delegate.GPU else Delegate.CPU
            Log.d(TAG, "🔄 Attempting to initialize with ${if (useGpu) "GPU" else "CPU"} delegate...")

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .setDelegate(delegate)
                .build()

            // Emülatör için çok düşük threshold değerleri (kamera kalitesi düşük)
            val detectionConfidence = if (isEmulator) 0.3f else 0.6f
            val presenceConfidence = if (isEmulator) 0.3f else 0.6f
            val trackingConfidence = if (isEmulator) 0.3f else 0.7f

            Log.d(TAG, "📊 Confidence thresholds: detection=$detectionConfidence, presence=$presenceConfidence, tracking=$trackingConfidence")

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2) // ÇİFT EL DESTEĞİ AKTIF
                .setMinHandDetectionConfidence(detectionConfidence)
                .setMinHandPresenceConfidence(presenceConfidence)
                .setMinTrackingConfidence(trackingConfidence)
                .setResultListener { result, inputImage ->
                    val handCount = result.landmarks().size

                    // ⚡⚡⚡ CRITICAL LOG for double hand debugging
                    Log.e("HandDetector", "🖐️🖐️🖐️ DETECTION RESULT: ${handCount} hand(s) detected")

                    if (handCount > 0) {
                        Log.d(TAG, "   ✅ Hand 0 has ${result.landmarks()[0].size} landmarks")
                    }
                    if (handCount > 1) {
                        Log.e("HandDetector", "   ✅✅ DOUBLE HAND! Hand 1 has ${result.landmarks()[1].size} landmarks")
                    }

                    _results.trySend(result)
                    frameCount++
                    updateFpsMetrics()
                }
                .setErrorListener { error ->
                    Log.e(TAG, "❌ Hand detection error: ${error.message}")
                }
                .build()

            val landmarker = HandLandmarker.createFromOptions(context, options)
            Log.d(TAG, "✅ HandLandmarker initialized (${if (useGpu) "GPU" else "CPU"} delegate, confidence: 0.6/0.6/0.7)")
            landmarker

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize with ${if (useGpu) "GPU" else "CPU"}: ${e.message}", e)
            null  // Return null, fallback denenir
        }
    }

    /**
     * Process camera frame
     *
     * Returns false if frame is dropped (throttling)
     */
    fun detectHands(bitmap: Bitmap, timestampMs: Long): Boolean {
        // Frame throttling #1: Check if already processing
        if (isProcessing.get()) {
            droppedFrameCount++
            return false  // Drop frame if busy
        }

        // Frame throttling #2: Time-based frame skipping
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessingTime < minProcessingIntervalMs) {
            droppedFrameCount++
            return false  // Skip frame (too fast)
        }

        return try {
            isProcessing.set(true)
            lastProcessingTime = currentTime

            val mpImage = BitmapImageBuilder(bitmap).build()
            handLandmarker?.detectAsync(mpImage, timestampMs)

            true  // Frame processed
        } catch (e: Exception) {
            Log.e(TAG, "❌ Detection failed: ${e.message}", e)
            false
        } finally {
            isProcessing.set(false)
        }
    }

    /**
     * Update FPS metrics
     *
     * Her 1 saniyede bir FPS hesapla ve performance warning göster
     */
    private fun updateFpsMetrics() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastFpsCalculationTime

        if (elapsedTime >= 1000) {
            currentFps = (frameCount.toFloat() / elapsedTime) * 1000f

            // Performance warning
            if (currentFps < 20f) {
                Log.w(TAG, "⚠️ Low FPS: ${currentFps.toInt()} (target: 30+) | Dropped: $droppedFrameCount")
            } else {
                // Her saniye FPS ve dropped frame sayısını log
                Log.d(TAG, "📊 FPS: ${currentFps.toInt()} | Dropped: $droppedFrameCount")
            }

            lastFpsCalculationTime = currentTime
            frameCount = 0
        }
    }

    /**
     * Get current FPS
     */
    fun getCurrentFps(): Float = currentFps

    /**
     * Get dropped frame count
     */
    fun getDroppedFrameCount(): Long = droppedFrameCount

    /**
     * Get processing rate (0.0 - 1.0)
     */
    fun getProcessingRate(): Float {
        val totalFrames = frameCount + droppedFrameCount
        return if (totalFrames > 0) {
            frameCount.toFloat() / totalFrames.toFloat()
        } else {
            0f
        }
    }

    /**
     * Get comprehensive performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            currentFps = currentFps,
            totalFramesProcessed = frameCount,
            droppedFrames = droppedFrameCount,
            processingRate = getProcessingRate(),
            isPerformanceGood = currentFps >= 25f && getProcessingRate() >= 0.8f
        )
    }

    /**
     * Set number of hands to detect (1 or 2)
     *
     * Note: This requires re-initializing the HandLandmarker, which is not
     * currently supported at runtime. The number of hands is set during
     * initialization and cannot be changed dynamically.
     */
    fun setNumHands(numHands: Int) {
        Log.w(TAG, "⚠️ setNumHands($numHands) called, but dynamic hand count change is not supported")
        Log.w(TAG, "   HandLandmarker is initialized with 2 hands by default")
        Log.w(TAG, "   To change hand count, recreate HandDetectorHelper with new configuration")
    }

    /**
     * Reset stats (örn: yeni session başladığında)
     */
    fun resetStats() {
        frameCount = 0
        droppedFrameCount = 0
        lastFpsCalculationTime = System.currentTimeMillis()
        currentFps = 0f
        Log.d(TAG, "📊 Performance stats reset")
    }

    /**
     * Close hand landmarker (lifecycle cleanup)
     */
    fun close() {
        handLandmarker?.close()
        handLandmarker = null
        Log.d(TAG, "🔄 HandLandmarker closed | Final FPS: ${currentFps.toInt()} | Dropped: $droppedFrameCount")
    }
}

/**
 * Performance Metrics Data Class
 */
data class PerformanceMetrics(
    val currentFps: Float,
    val totalFramesProcessed: Long,
    val droppedFrames: Long,
    val processingRate: Float,  // 0.0 - 1.0 (yüzde olarak dropped frame oranı)
    val isPerformanceGood: Boolean
) {
    fun getPerformanceStatus(): String {
        return when {
            currentFps >= 30f && processingRate >= 0.9f -> "Mükemmel"
            currentFps >= 25f && processingRate >= 0.8f -> "İyi"
            currentFps >= 20f && processingRate >= 0.7f -> "Orta"
            else -> "Düşük"
        }
    }
}
