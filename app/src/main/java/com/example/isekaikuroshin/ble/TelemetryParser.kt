// TODO-D4.2: Telemetri Packet Parser
package com.example.isekaikuroshin.ble

import android.util.Log
import com.example.isekaikuroshin.data.DroneState
import com.example.isekaikuroshin.data.FlightMode

/**
 * Telemetri Packet Parser
 *
 * ESP32'den gelen telemetri string'ini ayrıştırır ve DroneState'e çevirir.
 *
 * Packet Format:
 * "P1:80,P2:90,T:50,S:95,M:REFLEKS"
 *
 * P1 = Gauntlet Battery (%)
 * P2 = Drone Battery (%)
 * T  = Throttle (%)
 * S  = Signal RSSI (%)
 * M  = Mode (REFLEKS/SINEMATIK)
 *
 * Örnek:
 * ```kotlin
 * val state = TelemetryParser.parse("P1:85,P2:72,T:45,S:92,M:REFLEKS")
 * // → DroneState(gauntletBattery=85, droneBattery=72, throttle=45, ...)
 * ```
 */
object TelemetryParser {
    private const val TAG = "TelemetryParser"

    /**
     * Parse telemetri packet
     *
     * @param packet Raw telemetri string (örn: "P1:80,P2:90,T:50,S:95,M:REFLEKS")
     * @return DroneState veya null (parse hatası durumunda)
     */
    fun parse(packet: String): DroneState? {
        return try {
            // Hata mesajı kontrolü (ESP32'den gelen hata)
            if (packet.startsWith("ERROR:") || packet.startsWith("HATA:")) {
                return parseError(packet)
            }

            // Packet'i key-value pair'lere ayır
            val parts = packet.split(",")
            val data = mutableMapOf<String, String>()

            for (part in parts) {
                val keyValue = part.split(":")
                if (keyValue.size == 2) {
                    data[keyValue[0].trim()] = keyValue[1].trim()
                }
            }

            // Değerleri parse et
            val gauntletBattery = data["P1"]?.toIntOrNull() ?: 0
            val droneBattery = data["P2"]?.toIntOrNull() ?: 0
            val throttle = data["T"]?.toIntOrNull() ?: 0
            val signalRssi = data["S"]?.toIntOrNull() ?: 0
            val flightMode = data["M"]?.let { FlightMode.fromString(it) } ?: FlightMode.REFLEX

            // Validation: 0-100 aralığında olmalı
            if (!isValid(gauntletBattery, droneBattery, throttle, signalRssi)) {
                Log.w(TAG, "⚠️ Invalid telemetry values: $packet")
                return null
            }

            DroneState(
                gauntletBattery = gauntletBattery,
                droneBattery = droneBattery,
                throttle = throttle,
                signalRssi = signalRssi,
                flightMode = flightMode,
                isConnected = true,
                sensorStatus = com.example.isekaikuroshin.data.SensorStatus.OK,
                errorMessage = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Parse error: ${e.message}", e)
            null
        }
    }

    /**
     * Hata mesajını parse et
     * Örnek: "ERROR:MPU" -> MPU6050 hatası
     */
    private fun parseError(packet: String): DroneState {
        val errorType = packet.substringAfter(":").trim()

        val sensorStatus = when (errorType.uppercase()) {
            "MPU", "MPU6050" -> com.example.isekaikuroshin.data.SensorStatus.MPU6050_ERROR
            "ADC", "THROTTLE" -> com.example.isekaikuroshin.data.SensorStatus.ADC_ERROR
            "BLE" -> com.example.isekaikuroshin.data.SensorStatus.BLE_ERROR
            else -> com.example.isekaikuroshin.data.SensorStatus.UNKNOWN_ERROR
        }

        Log.w(TAG, "⚠️ Hardware error received: $errorType -> $sensorStatus")

        return DroneState(
            isConnected = true,
            sensorStatus = sensorStatus,
            errorMessage = sensorStatus.toUserMessage()
        )
    }

    /**
     * Validation: Değerler 0-100 aralığında mı?
     */
    private fun isValid(vararg values: Int): Boolean {
        return values.all { it in 0..100 }
    }

    /**
     * CRC Check (Opsiyonel - ESP32'de implement edilirse)
     *
     * Packet format CRC ile: "P1:80,P2:90,T:50,S:95,M:REFLEKS|CRC:AB12"
     *
     * TODO-D4.1: ESP32 firmware'de CRC eklenirse bu fonksiyonu implement et
     */
    fun parseWithCrc(packet: String): DroneState? {
        // CRC check implementasyonu
        // Şimdilik sadece CRC'yi ignore edip normal parse yap
        val parts = packet.split("|")
        val dataPacket = parts.getOrNull(0) ?: return null
        val crcPart = parts.getOrNull(1)

        if (crcPart != null) {
            // TODO: CRC validation
            Log.d(TAG, "📦 CRC found: $crcPart (validation not implemented yet)")
        }

        return parse(dataPacket)
    }

    // Mock packet generator kaldırıldı - Gerçek BLE verisi kullanılıyor
}

/**
 * Parse result wrapper (hata yönetimi için)
 */
sealed class ParseResult {
    data class Success(val state: DroneState) : ParseResult()
    data class Error(val message: String) : ParseResult()
}

/**
 * Extension: String → DroneState (helper)
 */
fun String.toDroneState(): DroneState? {
    return TelemetryParser.parse(this)
}
