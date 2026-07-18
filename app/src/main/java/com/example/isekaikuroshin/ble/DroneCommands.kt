// TODO-D1: Drone Komutları - Telefon → ESP32 (Kolluk)
package com.example.isekaikuroshin.ble

/**
 * Drone Komut Enum'ları
 *
 * BLE Komut Protokolü (Planlanan):
 * - Format: [CMD_ID:1byte][PARAM:4bytes]
 * - ESP32'ye (Kolluk) gönderilecek komutlar
 *
 * NOT: Gerçek BLE entegrasyonu D4.1'de yapılacak
 * Şimdilik enum tanımları ve mock implementation
 */
enum class DroneCommand(val commandId: Byte, val description: String) {
    // 🛡️ Güvenli Başlatma - Motorları güvenle başlat
    SAFE_ARM(0x01, "Safe Arm - Start motors safely with countdown"),

    // 🪂 Akıllı İniş - HC-SR04 sensör ile zemin algılama
    SMART_LANDING(0x02, "Smart Landing - Ground detection with HC-SR04"),

    // 🔒 Gaz Kilidi Aç/Kapat - Hall sensör verisini dondur
    CRUISE_TOGGLE(0x03, "Cruise Mode Toggle - Lock/unlock throttle"),

    // ⚡ Refleks Modu - MPU6050 hassasiyet artır
    MODE_REFLEX(0x04, "Reflex Mode - High sensitivity MPU6050"),

    // 🎬 Sinematik Modu - MPU6050 yumuşatılmış
    MODE_CINEMATIC(0x05, "Cinematic Mode - Smoothed MPU6050 data"),

    // 🔄 Takla İleri
    FLIP_FORWARD(0x06, "Flip Forward - Auto flip macro"),

    // 🔄 Takla Geri
    FLIP_BACKWARD(0x07, "Flip Backward - Auto flip macro"),

    // 🔄 Takla Sağ
    FLIP_RIGHT(0x08, "Flip Right - Auto flip macro"),

    // 🔄 Takla Sol
    FLIP_LEFT(0x09, "Flip Left - Auto flip macro"),

    // 🧭 Pusula Kalibrasyonu - HMC5883L sıfırlama
    COMPASS_CALIBRATION(0x0A, "Compass Calibration - Reset HMC5883L"),

    // 🤲 Akıllı Yakalama - HC-SR04'ü "el altında dur" moduna al
    SMART_CATCH(0x0B, "Smart Catch - Hand detection landing mode");

    /**
     * Komutu BLE packet formatına encode eder
     *
     * Format: [CMD_ID:1byte][PARAM:4bytes]
     * Örnek: SAFE_ARM → [0x01, 0x00, 0x00, 0x00, 0x00]
     */
    fun encode(param: Int = 0): ByteArray {
        return byteArrayOf(
            commandId,
            (param shr 24).toByte(),
            (param shr 16).toByte(),
            (param shr 8).toByte(),
            param.toByte()
        )
    }
}

/**
 * Drone Komut Sonucu
 */
sealed class DroneCommandResult {
    object Success : DroneCommandResult()
    data class Error(val message: String) : DroneCommandResult()
    object NotConnected : DroneCommandResult()
}
