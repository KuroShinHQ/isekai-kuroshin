package com.example.isekaikuroshin.ui.exercise

import android.os.Trace
import android.util.Log
import kotlin.system.measureNanoTime

/**
 * Performance Telemetry System
 *
 * Orta segment cihazlar (Samsung A34) için optimize edilmiş
 * performans izleme ve telemetri sistemi.
 *
 * ÖZELLİKLER:
 * - Frame processing time tracking
 * - FPS monitoring (real-time)
 * - CPU/GPU usage estimation
 * - Bottleneck detection
 * - Perfetto Trace integration
 */
object PerformanceTelemetry {

    const val TAG = "PerformanceTelemetry"  // public for inline functions

    // Frame timing
    private var lastFrameTime = System.nanoTime()
    private var frameTimeSum = 0L
    private var frameCount = 0
    private var slowFrameCount = 0

    // Target: 33ms (30 FPS) for mid-range devices
    private const val TARGET_FRAME_TIME_MS = 33L
    private const val SLOW_FRAME_THRESHOLD_MS = 50L  // Frame longer than 50ms

    // Moving average for smoother metrics
    private val frameTimesBuffer = BoundedDequeForTelemetry<Long>(maxSize = 60)  // Last 60 frames (2 sec @ 30fps)

    // Performance thresholds
    private const val WARNING_FPS = 20f
    private const val CRITICAL_FPS = 15f

    /**
     * Begin frame processing
     */
    fun beginFrame() {
        Trace.beginSection("ExerciseFrame")
        lastFrameTime = System.nanoTime()
    }

    /**
     * End frame processing and calculate metrics
     */
    fun endFrame() {
        val currentTime = System.nanoTime()
        val frameTimeNs = currentTime - lastFrameTime
        val frameTimeMs = frameTimeNs / 1_000_000L

        // Update metrics
        frameTimeSum += frameTimeMs
        frameCount++

        // Track slow frames
        if (frameTimeMs > SLOW_FRAME_THRESHOLD_MS) {
            slowFrameCount++
            Log.w(TAG, "⚠️ Slow frame detected: ${frameTimeMs}ms (target: ${TARGET_FRAME_TIME_MS}ms)")
        }

        // Add to moving average buffer (automatically removes oldest if full)
        frameTimesBuffer.addLast(frameTimeMs)

        // Log every 60 frames (every ~2 seconds @ 30fps)
        if (frameCount % 60 == 0) {
            logPerformanceMetrics()
        }

        Trace.endSection()
    }

    /**
     * Measure execution time of a code block with Perfetto tracing
     */
    inline fun <T> measurePerformance(sectionName: String, block: () -> T): T {
        Trace.beginSection(sectionName)
        val startTime = System.nanoTime()

        return try {
            block()
        } finally {
            val executionTimeMs = (System.nanoTime() - startTime) / 1_000_000L
            Trace.endSection()

            // Log if execution is slow
            if (executionTimeMs > 16) {  // Longer than 16ms (1 frame @ 60fps)
                Log.w(TAG, "⏱️ Slow operation: $sectionName took ${executionTimeMs}ms")
            }
        }
    }

    /**
     * Calculate current FPS
     */
    fun getCurrentFPS(): Float {
        if (frameTimesBuffer.isEmpty()) return 0f
        val avgFrameTimeMs = frameTimesBuffer.average()
        return if (avgFrameTimeMs > 0) 1000f / avgFrameTimeMs.toFloat() else 0f
    }

    /**
     * Get average frame time (ms)
     */
    fun getAverageFrameTime(): Float {
        return if (frameTimesBuffer.isEmpty()) 0f else frameTimesBuffer.average().toFloat()
    }

    /**
     * Get slow frame percentage
     */
    fun getSlowFramePercentage(): Float {
        return if (frameCount > 0) (slowFrameCount.toFloat() / frameCount) * 100f else 0f
    }

    /**
     * Log comprehensive performance metrics
     */
    private fun logPerformanceMetrics() {
        val currentFps = getCurrentFPS()
        val avgFrameTime = getAverageFrameTime()
        val slowFramePct = getSlowFramePercentage()

        Log.i(TAG, "═══════════════════════════════════════")
        Log.i(TAG, "📊 PERFORMANCE METRICS")
        Log.i(TAG, "───────────────────────────────────────")
        Log.i(TAG, "  FPS: ${currentFps.toInt()} fps")
        Log.i(TAG, "  Avg Frame Time: ${avgFrameTime.toInt()}ms")
        Log.i(TAG, "  Slow Frames: ${slowFramePct.toInt()}%")
        Log.i(TAG, "  Total Frames: $frameCount")
        Log.i(TAG, "═══════════════════════════════════════")

        // Performance warnings
        when {
            currentFps < CRITICAL_FPS -> {
                Log.e(TAG, "🔴 CRITICAL: FPS below ${CRITICAL_FPS}! Device may be struggling.")
            }
            currentFps < WARNING_FPS -> {
                Log.w(TAG, "⚠️ WARNING: FPS below ${WARNING_FPS}. Consider reducing visual effects.")
            }
        }
    }

    /**
     * Reset all metrics
     */
    fun reset() {
        frameTimeSum = 0L
        frameCount = 0
        slowFrameCount = 0
        frameTimesBuffer.clear()
        Log.d(TAG, "📊 Performance metrics reset")
    }

    /**
     * Get performance summary report
     */
    fun getPerformanceReport(): String {
        return buildString {
            appendLine("═══ PERFORMANCE REPORT ═══")
            appendLine("FPS: ${getCurrentFPS().toInt()}")
            appendLine("Avg Frame Time: ${getAverageFrameTime().toInt()}ms")
            appendLine("Slow Frames: ${getSlowFramePercentage().toInt()}%")
            appendLine("Total Frames: $frameCount")
            appendLine("═══════════════════════════")
        }
    }
}

/**
 * Bounded Deque with max capacity (composition pattern)
 */
private class BoundedDequeForTelemetry<T>(private val maxSize: Int) {
    private val deque = ArrayDeque<T>()

    val size: Int get() = deque.size

    fun addLast(element: T) {
        if (deque.size >= maxSize) {
            deque.removeFirst()
        }
        deque.addLast(element)
    }

    fun isEmpty(): Boolean = deque.isEmpty()

    fun average(): Double {
        if (deque.isEmpty()) return 0.0
        @Suppress("UNCHECKED_CAST")
        return (deque as Collection<Number>).map { it.toDouble() }.average()
    }

    fun clear() = deque.clear()
}
