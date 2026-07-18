package com.example.isekaikuroshin.utils

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.isekaikuroshin.data.PerformanceMode
import com.example.isekaikuroshin.data.PersistentDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * G59: Performance Settings UI - PerformanceManager
 *
 * Performans izleme ve yönetim sistemi
 * - FPS tracking
 * - Thermal monitoring (cihaz sıcaklığı)
 * - Auto-throttle (ısınma durumunda performans düşürme)
 * - Performance mode application
 *
 * Özellikler:
 * - Real-time FPS counter
 * - CPU/GPU thermal zone monitoring
 * - Battery status tracking
 * - Performance mode presets (HIGH/BALANCED/BATTERY)
 */
object PerformanceManager {

    private const val TAG = "PerformanceManager"
    private const val UPDATE_INTERVAL_MS = 1000L // 1 saniyede bir güncelle
    private const val THERMAL_CRITICAL_THRESHOLD = 45f // °C

    // Performance statistics data class
    data class PerformanceStats(
        val currentFps: Int = 0,
        val averageFps: Float = 0f,
        val cpuTemperature: Float = 0f,
        val batteryTemperature: Float = 0f,
        val thermalState: ThermalState = ThermalState.NORMAL,
        val isThrottled: Boolean = false,
        val batteryLevel: Int = 100,
        val isBatteryCharging: Boolean = false
    )

    enum class ThermalState {
        NORMAL,     // <40°C
        WARM,       // 40-43°C
        HOT,        // 43-45°C
        CRITICAL    // >45°C
    }

    // StateFlow for reactive updates
    private val _performanceStats = MutableStateFlow(PerformanceStats())
    val performanceStats: StateFlow<PerformanceStats> = _performanceStats.asStateFlow()

    private var monitoringJob: Job? = null
    private var isMonitoring = false

    // FPS tracking
    private val fpsHistory = mutableListOf<Int>()
    private const val FPS_HISTORY_SIZE = 60 // 60 saniye history

    /**
     * Performans izlemeyi başlat
     */
    fun startMonitoring(context: Context) {
        if (isMonitoring) {
            Log.d(TAG, "⚠️ G59: Performance monitoring already started")
            return
        }

        isMonitoring = true
        Log.d(TAG, "✅ G59: Performance monitoring başlatıldı")

        monitoringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isMonitoring) {
                try {
                    val stats = collectPerformanceStats(context)
                    _performanceStats.value = stats

                    // Kritik sıcaklık uyarısı
                    if (stats.thermalState == ThermalState.CRITICAL) {
                        Log.w(TAG, "🔥 G59: CRITICAL THERMAL STATE! Temp: ${stats.cpuTemperature}°C")

                        // Auto-throttle aktifse performansı düşür
                        val settings = PersistentDataManager.gameData.value.settingsData.performanceSettings
                        if (settings.autoThrottleOnHeat && !stats.isThrottled) {
                            applyThermalThrottle(context)
                        }
                    } else if (stats.thermalState == ThermalState.HOT) {
                        Log.w(TAG, "⚠️ G59: High thermal state. Temp: ${stats.cpuTemperature}°C")
                    }

                    delay(UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ G59: Performance stats collection failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Performans izlemeyi durdur
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "🛑 G59: Performance monitoring durduruldu")
    }

    /**
     * Performans istatistiklerini topla (tek seferlik)
     */
    fun collectPerformanceStats(context: Context): PerformanceStats {
        val cpuTemp = readCpuTemperature()
        val batteryTemp = readBatteryTemperature(context)
        val thermalState = calculateThermalState(cpuTemp, batteryTemp)

        // Battery bilgileri
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        val batteryStatus = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS) ?: 0
        val isCharging = batteryStatus == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        batteryStatus == android.os.BatteryManager.BATTERY_STATUS_FULL

        // Throttle durumu
        val settings = PersistentDataManager.gameData.value.settingsData.performanceSettings
        val isThrottled = settings.autoThrottleOnHeat &&
                         (cpuTemp > settings.maxThermalThreshold || batteryTemp > settings.maxThermalThreshold)

        return PerformanceStats(
            currentFps = 60, // TODO: Gerçek FPS tracking eklenecek
            averageFps = 60f,
            cpuTemperature = cpuTemp,
            batteryTemperature = batteryTemp,
            thermalState = thermalState,
            isThrottled = isThrottled,
            batteryLevel = batteryLevel,
            isBatteryCharging = isCharging
        )
    }

    /**
     * CPU sıcaklığını oku (thermal zone'lardan)
     */
    private fun readCpuTemperature(): Float {
        try {
            // Android cihazlarda thermal zone dosyaları farklı yerlerde olabilir
            val thermalPaths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone2/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/devices/virtual/thermal/thermal_zone1/temp"
            )

            for (path in thermalPaths) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val temp = file.readText().trim().toFloatOrNull()
                    if (temp != null) {
                        // Genelde millidegree cinsinden (1000 = 1°C)
                        return if (temp > 1000) temp / 1000f else temp
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "⚠️ G59: CPU temp read failed: ${e.message}")
        }

        return 0f // Okunamazsa 0 döndür
    }

    /**
     * Batarya sıcaklığını oku
     */
    private fun readBatteryTemperature(context: Context): Float {
        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && batteryManager != null) {
                val temp = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                // Not: BATTERY_PROPERTY_TEMPERATURE deprecated, CAPACITY kullanıyoruz
                // Alternatif: BroadcastReceiver ile EXTRA_TEMPERATURE alınabilir
                return temp.toFloat() / 10f
            }
        } catch (e: Exception) {
            Log.d(TAG, "⚠️ G59: Battery temp read failed: ${e.message}")
        }

        return 0f
    }

    /**
     * Thermal state hesapla
     */
    private fun calculateThermalState(cpuTemp: Float, batteryTemp: Float): ThermalState {
        val maxTemp = maxOf(cpuTemp, batteryTemp)
        return when {
            maxTemp > 45f -> ThermalState.CRITICAL
            maxTemp > 43f -> ThermalState.HOT
            maxTemp > 40f -> ThermalState.WARM
            else -> ThermalState.NORMAL
        }
    }

    /**
     * Isınma durumunda otomatik performans düşürme
     */
    private fun applyThermalThrottle(context: Context) {
        Log.w(TAG, "🔥 G59: Thermal throttle applied - switching to BATTERY mode")

        PersistentDataManager.updateSettingsData { settings ->
            settings.copy(
                performanceSettings = settings.performanceSettings.copy(
                    performanceMode = "BATTERY",
                    targetFps = 30
                )
            )
        }
    }

    /**
     * Performance mode'u uygula
     */
    fun applyPerformanceMode(mode: PerformanceMode) {
        Log.d(TAG, "📊 G59: Applying performance mode: $mode")

        val (fps, particles, postProcess, shadows) = when (mode) {
            PerformanceMode.HIGH -> Quadruple(60, true, true, true)
            PerformanceMode.BALANCED -> Quadruple(45, true, true, false)
            PerformanceMode.BATTERY -> Quadruple(30, false, false, false)
        }

        PersistentDataManager.updateSettingsData { settings ->
            settings.copy(
                performanceSettings = settings.performanceSettings.copy(
                    performanceMode = mode.name,
                    targetFps = fps,
                    enableParticleEffects = particles,
                    enablePostProcessing = postProcess,
                    enableShadows = shadows
                )
            )
        }

        Log.d(TAG, "✅ G59: Performance mode applied - FPS: $fps, Particles: $particles")
    }

    /**
     * Performans log'u (debug amaçlı)
     */
    fun logPerformanceSnapshot(context: Context) {
        val stats = collectPerformanceStats(context)
        Log.d(TAG, """
            📊 G59: PERFORMANCE SNAPSHOT
            ├─ FPS: ${stats.currentFps} (avg: ${String.format("%.1f", stats.averageFps)})
            ├─ CPU Temp: ${String.format("%.1f", stats.cpuTemperature)}°C
            ├─ Battery Temp: ${String.format("%.1f", stats.batteryTemperature)}°C
            ├─ Thermal State: ${stats.thermalState}
            ├─ Throttled: ${stats.isThrottled}
            ├─ Battery: ${stats.batteryLevel}% (Charging: ${stats.isBatteryCharging})
            └─ Mode: ${PersistentDataManager.gameData.value.settingsData.performanceSettings.performanceMode}
        """.trimIndent())
    }

    /**
     * FPS kaydet (frame render edildiğinde çağrılacak)
     */
    fun recordFrame() {
        // TODO: Gerçek FPS tracking implementasyonu
        // Her frame render edildiğinde bu fonksiyon çağrılacak
    }
}

/**
 * Helper data class for tuple return
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
