package com.example.isekaikuroshin.utils

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * G60c: Memory Optimization Utility
 * Target: <300MB RAM idle, <500MB peak on Samsung A34s
 *
 * Features:
 * - Bitmap optimization (downsampling, RGB_565)
 * - Cache management
 * - Memory threshold monitoring
 * - Garbage collection utilities
 */
object MemoryOptimizer {

    private const val TAG = "MemoryOptimizer"

    // Memory thresholds (MB)
    private const val LOW_MEMORY_THRESHOLD = 200
    private const val CRITICAL_MEMORY_THRESHOLD = 100
    private const val MAX_CACHE_SIZE_MB = 50

    /**
     * Check if device is in low memory state
     */
    fun isLowMemory(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val isLow = availableMemoryMB < LOW_MEMORY_THRESHOLD

        if (isLow) {
            Log.w(TAG, "⚠️ G60c: Low memory detected - Available: ${availableMemoryMB}MB")
        }

        return isLow
    }

    /**
     * Get available memory in MB
     */
    fun getAvailableMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    /**
     * Calculate optimal Bitmap sample size for memory-efficient loading
     * G60c optimization: Prevents OOM errors on low-end devices
     *
     * @param originalWidth Original image width
     * @param originalHeight Original image height
     * @param reqWidth Required width
     * @param reqHeight Required height
     * @return Sample size (power of 2)
     */
    fun calculateInSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1

        if (originalHeight > reqHeight || originalWidth > reqWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            // Calculate largest inSampleSize that keeps both dimensions larger than requested
            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        Log.d(TAG, "✅ G60c: Calculated sample size: $inSampleSize for ${originalWidth}x${originalHeight} → ${reqWidth}x${reqHeight}")
        return inSampleSize
    }

    /**
     * Get recommended Bitmap config based on memory state
     * G60c: RGB_565 uses 50% less memory than ARGB_8888
     */
    fun getRecommendedBitmapConfig(context: Context, needsAlpha: Boolean = false): Bitmap.Config {
        return if (isLowMemory(context) && !needsAlpha) {
            Log.d(TAG, "⚡ G60c: Using RGB_565 for memory optimization")
            Bitmap.Config.RGB_565  // 2 bytes per pixel (50% memory saving)
        } else if (needsAlpha) {
            Bitmap.Config.ARGB_8888  // 4 bytes per pixel (full quality with alpha)
        } else {
            Bitmap.Config.ARGB_8888
        }
    }

    /**
     * Calculate cache directory size
     */
    fun getCacheSizeMB(context: Context): Long {
        val cacheDir = context.cacheDir
        return getFolderSize(cacheDir) / (1024 * 1024)
    }

    /**
     * Recursively calculate folder size
     */
    private fun getFolderSize(directory: File): Long {
        var size = 0L
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getFolderSize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    /**
     * Clear cache if it exceeds threshold
     * G60c: Automatic cache management
     */
    suspend fun trimCacheIfNeeded(context: Context): Boolean = withContext(Dispatchers.IO) {
        val cacheSizeMB = getCacheSizeMB(context)

        if (cacheSizeMB > MAX_CACHE_SIZE_MB) {
            Log.w(TAG, "🗑️ G60c: Cache size ${cacheSizeMB}MB exceeds ${MAX_CACHE_SIZE_MB}MB - clearing...")
            clearCache(context)
            Log.i(TAG, "✅ G60c: Cache cleared - new size: ${getCacheSizeMB(context)}MB")
            true
        } else {
            Log.d(TAG, "✓ G60c: Cache size OK: ${cacheSizeMB}MB")
            false
        }
    }

    /**
     * Clear all cache
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        context.cacheDir.deleteRecursively()
        Log.i(TAG, "✅ G60c: All cache cleared")
    }

    /**
     * Force garbage collection (use sparingly!)
     * G60c: Emergency memory cleanup
     */
    fun forceGC() {
        Log.d(TAG, "🔄 G60c: Forcing garbage collection...")
        System.gc()
        System.runFinalization()
        Log.d(TAG, "✅ G60c: GC completed")
    }

    /**
     * Get memory usage report
     */
    fun getMemoryReport(context: Context): MemoryReport {
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        return MemoryReport(
            heapUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
            heapMaxMB = runtime.maxMemory() / (1024 * 1024),
            availableSystemMB = memoryInfo.availMem / (1024 * 1024),
            totalSystemMB = memoryInfo.totalMem / (1024 * 1024),
            cacheSizeMB = getCacheSizeMB(context),
            isLowMemory = memoryInfo.lowMemory
        )
    }

    /**
     * Memory optimization strategy based on current state
     */
    fun getOptimizationStrategy(context: Context): OptimizationStrategy {
        val availableMB = getAvailableMemoryMB(context)

        return when {
            availableMB < CRITICAL_MEMORY_THRESHOLD -> {
                Log.e(TAG, "🔥 G60c: CRITICAL MEMORY! Available: ${availableMB}MB")
                OptimizationStrategy.CRITICAL
            }
            availableMB < LOW_MEMORY_THRESHOLD -> {
                Log.w(TAG, "⚠️ G60c: LOW MEMORY! Available: ${availableMB}MB")
                OptimizationStrategy.AGGRESSIVE
            }
            else -> {
                OptimizationStrategy.NORMAL
            }
        }
    }

    /**
     * Apply optimization strategy
     */
    suspend fun applyOptimization(context: Context, strategy: OptimizationStrategy) {
        when (strategy) {
            OptimizationStrategy.CRITICAL -> {
                Log.w(TAG, "🔥 G60c: Applying CRITICAL optimization...")
                clearCache(context)
                forceGC()
                // TODO: Reduce graphics quality, disable particles, lower FPS
            }
            OptimizationStrategy.AGGRESSIVE -> {
                Log.w(TAG, "⚠️ G60c: Applying AGGRESSIVE optimization...")
                trimCacheIfNeeded(context)
                forceGC()
                // TODO: Reduce some graphics settings
            }
            OptimizationStrategy.NORMAL -> {
                Log.d(TAG, "✓ G60c: Normal memory state - no optimization needed")
                trimCacheIfNeeded(context)
            }
        }
    }

    /**
     * Log detailed memory status
     */
    fun logMemoryStatus(context: Context) {
        val report = getMemoryReport(context)
        val strategy = getOptimizationStrategy(context)

        Log.i(TAG, """
            📊 G60c: MEMORY STATUS REPORT
            ├─ Heap: ${report.heapUsedMB}MB / ${report.heapMaxMB}MB
            ├─ System Available: ${report.availableSystemMB}MB / ${report.totalSystemMB}MB
            ├─ Cache Size: ${report.cacheSizeMB}MB
            ├─ Low Memory Flag: ${report.isLowMemory}
            └─ Strategy: $strategy
        """.trimIndent())
    }
}

/**
 * Memory usage report data class
 */
data class MemoryReport(
    val heapUsedMB: Long,
    val heapMaxMB: Long,
    val availableSystemMB: Long,
    val totalSystemMB: Long,
    val cacheSizeMB: Long,
    val isLowMemory: Boolean
)

/**
 * Memory optimization strategies
 */
enum class OptimizationStrategy {
    NORMAL,      // >200MB available - no action needed
    AGGRESSIVE,  // 100-200MB available - reduce cache, force GC
    CRITICAL     // <100MB available - clear cache, force GC, reduce graphics
}
