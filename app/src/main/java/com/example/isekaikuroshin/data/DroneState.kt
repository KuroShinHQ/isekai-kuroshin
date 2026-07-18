// TODO-D4.2: Drone Telemetri Data Model
package com.example.isekaikuroshin.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drone Telemetri Durumu
 *
 * ESP32'den gelen telemetri verisini tutar ve UI'a reactive olarak sunar.
 *
 * Packet Format (Örnek):
 * "P1:80,P2:90,T:50,S:95,M:REFLEKS"
 *
 * P1 = Gauntlet Battery (Kolluk 18650) %
 * P2 = Drone Battery (Matek F4 LiPo) %
 * T  = Throttle (Hall Sensör KY-003) %
 * S  = Signal RSSI (ELRS) %
 * M  = Mode (REFLEKS/SINEMATIK)
 */
data class DroneState(
    val gauntletBattery: Int = 0,  // P1: Kolluk bataryası (0-100%)
    val droneBattery: Int = 0,     // P2: Drone bataryası (0-100%)
    val throttle: Int = 0,         // T: Gaz seviyesi (0-100%)
    val signalRssi: Int = 0,       // S: ELRS sinyal gücü (0-100%)
    val flightMode: FlightMode = FlightMode.REFLEX, // M: Uçuş modu
    val isConnected: Boolean = false, // BLE bağlantı durumu
    val lastUpdateTimestamp: Long = 0L, // Son güncelleme zamanı (ms)
    val errorMessage: String? = null,   // Hata mesajı (null = hata yok)
    val sensorStatus: SensorStatus = SensorStatus.OK // Sensör durumu
) {
    /**
     * Timeout kontrolü: Son güncelleme 2 saniyeden eski mi?
     */
    fun isTimedOut(): Boolean {
        if (lastUpdateTimestamp == 0L) return false // Hiç veri gelmemiş
        return (System.currentTimeMillis() - lastUpdateTimestamp) > 2000
    }

    /**
     * Geçerli veri mi?
     */
    fun isDataValid(): Boolean {
        return isConnected && !isTimedOut() && errorMessage == null
    }
}

/**
 * Sensör Durumu
 * KURAL 11: Metadata-based logic
 */
enum class SensorStatus {
    OK,              // Tüm sensörler çalışıyor
    MPU6050_ERROR,   // MPU6050 (Refleks sensörü) hatası
    ADC_ERROR,       // ADC (Gaz sensörü) hatası
    BLE_ERROR,       // BLE bağlantı hatası
    UNKNOWN_ERROR;   // Bilinmeyen hata

    fun toUserMessage(): String {
        return when (this) {
            OK -> ""
            MPU6050_ERROR -> "⚠️ HATA: KOLLUK GÖZÜ (MPU6050) BAĞLI DEĞİL"
            ADC_ERROR -> "⚠️ HATA: GAZ SENSÖRÜ YANIT VERMİYOR"
            BLE_ERROR -> "⚠️ HATA: BLUETOOTH BAĞLANTISI KESİLDİ"
            UNKNOWN_ERROR -> "⚠️ HATA: BİLİNMEYEN DONANIM SORUNU"
        }
    }
}

/**
 * Uçuş Modları
 * KURAL 11: Metadata-based logic (String karşılaştırması YAPMA)
 */
enum class FlightMode {
    REFLEX,      // Refleks modu (yüksek hassasiyet)
    CINEMATIC;   // Sinematik modu (yumuşatılmış)

    companion object {
        fun fromString(mode: String): FlightMode {
            return when (mode.uppercase()) {
                "REFLEKS", "REFLEX" -> REFLEX
                "SINEMATIK", "CINEMATIC" -> CINEMATIC
                else -> REFLEX // Default
            }
        }
    }
}

/**
 * Drone State Manager
 * Singleton pattern ile global state yönetimi
 */
object DroneStateManager {
    private val _state = MutableStateFlow(DroneState())
    val state: StateFlow<DroneState> = _state.asStateFlow()

    /**
     * Telemetri verisini güncelle
     */
    fun updateState(newState: DroneState) {
        _state.value = newState.copy(lastUpdateTimestamp = System.currentTimeMillis())
    }

    /**
     * Belirli alanları güncelle (partial update)
     */
    fun updateBattery(gauntlet: Int?, drone: Int?) {
        _state.value = _state.value.copy(
            gauntletBattery = gauntlet ?: _state.value.gauntletBattery,
            droneBattery = drone ?: _state.value.droneBattery,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    fun updateThrottle(throttle: Int) {
        _state.value = _state.value.copy(
            throttle = throttle,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    fun updateSignal(rssi: Int) {
        _state.value = _state.value.copy(
            signalRssi = rssi,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    fun updateFlightMode(mode: FlightMode) {
        _state.value = _state.value.copy(
            flightMode = mode,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    fun updateConnectionStatus(isConnected: Boolean) {
        _state.value = _state.value.copy(
            isConnected = isConnected,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Hata mesajı güncelle
     */
    fun updateError(errorMessage: String?) {
        _state.value = _state.value.copy(
            errorMessage = errorMessage,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Sensör durumu güncelle
     */
    fun updateSensorStatus(status: SensorStatus) {
        _state.value = _state.value.copy(
            sensorStatus = status,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * State'i sıfırla (bağlantı koparsa)
     */
    fun reset() {
        _state.value = DroneState()
    }
}
