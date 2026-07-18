package com.example.isekaikuroshin.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * G58: Memory Profiling Setup
 *
 * Gerçek zamanlı hafıza kullanım izleme ve raporlama sistemi
 * Developer Options ekranında gösterilir
 *
 * Özellikler:
 * - Heap memory (current / max)
 * - Native memory (allocated / free)
 * - PSS (Proportional Set Size) - tüm süreçler arası paylaşılan hafıza
 * - Available system memory
 * - Memory pressure warnings
 */
object MemoryMonitor {

    private const val TAG = "MemoryMonitor"
    private const val UPDATE_INTERVAL_MS = 2000L // 2 saniyede bir güncelle

    // Memory statistics data class
    data class MemoryStats(
        val heapUsedMB: Float = 0f,
        val heapMaxMB: Float = 0f,
        val nativeAllocatedMB: Float = 0f,
        val nativeFreeMB: Float = 0f,
        val pssTotalMB: Float = 0f,
        val availableSystemMemoryMB: Float = 0f,
        val isLowMemory: Boolean = false,
        val memoryPressureLevel: MemoryPressure = MemoryPressure.NORMAL
    )

    enum class MemoryPressure {
        NORMAL,    // <200MB kullanım
        MODERATE,  // 200-300MB kullanım
        HIGH,      // 300-400MB kullanım
        CRITICAL   // >400MB kullanım
    }

    // StateFlow for reactive updates
    private val _memoryStats = MutableStateFlow(MemoryStats())
    val memoryStats: StateFlow<MemoryStats> = _memoryStats.asStateFlow()

    private var monitoringJob: Job? = null
    private var isMonitoring = false

    /**
     * Hafıza izlemeyi başlat (Developer Mode aktifse)
     */
    fun startMonitoring(context: Context) {
        if (isMonitoring) {
            Log.d(TAG, "⚠️ G58: Monitoring already started")
            return
        }

        isMonitoring = true
        Log.d(TAG, "✅ G58: Memory monitoring başlatıldı")

        monitoringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isMonitoring) {
                try {
                    val stats = collectMemoryStats(context)
                    _memoryStats.value = stats

                    // Kritik hafıza durumunda uyar
                    if (stats.memoryPressureLevel == MemoryPressure.CRITICAL) {
                        Log.w(TAG, "🔥 G58: CRITICAL MEMORY PRESSURE! PSS: ${stats.pssTotalMB}MB")
                    } else if (stats.memoryPressureLevel == MemoryPressure.HIGH) {
                        Log.w(TAG, "⚠️ G58: High memory pressure. PSS: ${stats.pssTotalMB}MB")
                    }

                    delay(UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ G58: Memory stats collection failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Hafıza izlemeyi durdur
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "🛑 G58: Memory monitoring durduruldu")
    }

    /**
     * Hafıza istatistiklerini topla (tek seferlik)
     */
    fun collectMemoryStats(context: Context): MemoryStats {
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Heap Memory (Java/Kotlin objects)
        val heapUsedBytes = runtime.totalMemory() - runtime.freeMemory()
        val heapMaxBytes = runtime.maxMemory()

        // Native Memory (JNI/Native code)
        val nativeAllocatedBytes = Debug.getNativeHeapAllocatedSize()
        val nativeFreeBytes = Debug.getNativeHeapFreeSize()

        // PSS (Proportional Set Size) - gerçek kullanım
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        val pssTotalKB = memInfo.totalPss

        // Sistem hafızası
        val memInfoSystem = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfoSystem)
        val availableSystemBytes = memInfoSystem.availMem

        // Memory Pressure hesapla
        val pssTotalMB = pssTotalKB / 1024f
        val pressure = when {
            pssTotalMB > 400f -> MemoryPressure.CRITICAL
            pssTotalMB > 300f -> MemoryPressure.HIGH
            pssTotalMB > 200f -> MemoryPressure.MODERATE
            else -> MemoryPressure.NORMAL
        }

        return MemoryStats(
            heapUsedMB = heapUsedBytes / (1024f * 1024f),
            heapMaxMB = heapMaxBytes / (1024f * 1024f),
            nativeAllocatedMB = nativeAllocatedBytes / (1024f * 1024f),
            nativeFreeMB = nativeFreeBytes / (1024f * 1024f),
            pssTotalMB = pssTotalMB,
            availableSystemMemoryMB = availableSystemBytes / (1024f * 1024f),
            isLowMemory = memInfoSystem.lowMemory,
            memoryPressureLevel = pressure
        )
    }

    /**
     * Garbage Collection'ı manuel tetikle (Debug/Test amaçlı)
     */
    fun forceGarbageCollection() {
        Log.d(TAG, "♻️ G58: Force GC triggered")
        System.gc()
        Runtime.getRuntime().gc()
    }

    /**
     * Hafıza durumu log'u (debug amaçlı)
     */
    fun logMemorySnapshot(context: Context) {
        val stats = collectMemoryStats(context)
        Log.d(TAG, """
            📊 G58: MEMORY SNAPSHOT
            ├─ Heap: ${String.format("%.1f", stats.heapUsedMB)}MB / ${String.format("%.1f", stats.heapMaxMB)}MB
            ├─ Native: ${String.format("%.1f", stats.nativeAllocatedMB)}MB allocated
            ├─ PSS Total: ${String.format("%.1f", stats.pssTotalMB)}MB
            ├─ Available System: ${String.format("%.1f", stats.availableSystemMemoryMB)}MB
            ├─ Low Memory: ${stats.isLowMemory}
            └─ Pressure: ${stats.memoryPressureLevel}
        """.trimIndent())
    }
}
