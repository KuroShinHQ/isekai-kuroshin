// TODO-D2: Telemetri UI - Batarya, Gaz ve Durum Göstergeleri
// TODO-D4.2: DroneState entegrasyonu
package com.example.isekaikuroshin.ui.drone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.isekaikuroshin.data.DroneStateManager
import com.example.isekaikuroshin.data.FlightMode
import com.example.isekaikuroshin.data.LanguageManager

/**
 * Localization helper
 * KURAL 9: Always use LanguageManager for text
 */
@Composable
private fun rememberLocalizedText(key: String): String {
    val language by LanguageManager.currentLanguage.collectAsState()
    return LanguageManager.getText(key)
}

/**
 * TODO-D2: Telemetri Paneli
 * TODO-D4.2: DroneState ile reactive güncelleme
 *
 * İçerik:
 * - Çift batarya göstergesi (Kolluk 18650 + Drone Matek F4)
 * - Gaz "Mana Barı" (Hall sensör 0-100%)
 * - Durum göstergeleri (BLE, ELRS, Mod)
 *
 * Data Flow: ESP32 → BLE → Parser → DroneState → UI
 */
@Composable
fun TelemetryPanel() {
    // ✅ GÖREV D4.2: DroneState'ten veri al (reactive)
    val droneState by DroneStateManager.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Başlık
        Text(
            text = rememberLocalizedText("TELEMETRY"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Çift Batarya Göstergeleri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kolluk Bataryası (18650)
            BatteryIndicator(
                label = rememberLocalizedText("GAUNTLET_BATTERY"),
                percentage = droneState.gauntletBattery, // Real-time data!
                modifier = Modifier.weight(1f)
            )

            // Drone Bataryası (Matek F4)
            BatteryIndicator(
                label = rememberLocalizedText("DRONE_BATTERY"),
                percentage = droneState.droneBattery, // Real-time data!
                modifier = Modifier.weight(1f)
            )
        }

        // Gaz "Mana Barı" (Hall Sensör)
        ThrottleManaBar(
            percentage = droneState.throttle // Real-time data!
        )

        // Durum Göstergeleri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BLE Bağlantı Durumu
            StatusIndicator(
                label = rememberLocalizedText("BLE_STATUS"),
                value = rememberLocalizedText(
                    if (droneState.isConnected) "CONNECTED" else "DISCONNECTED"
                ),
                isConnected = droneState.isConnected
            )

            // ELRS Sinyal Gücü
            StatusIndicator(
                label = rememberLocalizedText("ELRS_SIGNAL"),
                value = "${droneState.signalRssi}%", // Real-time data!
                isConnected = droneState.isConnected
            )

            // Uçuş Modu
            StatusIndicator(
                label = rememberLocalizedText("FLIGHT_MODE"),
                value = rememberLocalizedText(
                    when (droneState.flightMode) {
                        FlightMode.REFLEX -> "MODE_REFLEX"
                        FlightMode.CINEMATIC -> "MODE_CINEMATIC"
                    }
                ),
                isConnected = droneState.isConnected
            )
        }
    }
}

/**
 * Batarya Göstergesi
 * @param label Batarya etiketi (Kolluk/Drone)
 * @param percentage Batarya yüzdesi (0-100)
 */
@Composable
fun BatteryIndicator(
    label: String,
    percentage: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Etiket
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Progress Bar (KURAL 10: MaterialTheme.colorScheme kullan)
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                percentage > 50 -> MaterialTheme.colorScheme.tertiary // Yeşil
                percentage > 20 -> MaterialTheme.colorScheme.secondary // Turuncu/Sarı
                else -> MaterialTheme.colorScheme.error // Kırmızı
            },
            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )

        // Yüzde Metni
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Gaz "Mana Barı" (Hall Sensör)
 * @param percentage Gaz yüzdesi (0-100)
 *
 * Renk Gradyanı:
 * - Kırmızı (0-33%)
 * - Sarı (34-66%)
 * - Yeşil (67-100%)
 */
@Composable
fun ThrottleManaBar(percentage: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Etiket
        Text(
            text = rememberLocalizedText("THROTTLE_MANA"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Gradyan Progress Bar (KURAL 10: MaterialTheme.colorScheme kullan)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.error, // Kırmızı
                                MaterialTheme.colorScheme.secondary, // Sarı
                                MaterialTheme.colorScheme.tertiary  // Yeşil
                            )
                        )
                    )
            )
        }

        // Yüzde Metni
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Durum Göstergesi (BLE, ELRS, Mod)
 * @param label Etiket
 * @param value Değer
 * @param isConnected Bağlantı durumu (renk için)
 */
@Composable
fun StatusIndicator(
    label: String,
    value: String,
    isConnected: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Etiket
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        // Değer (KURAL 10: MaterialTheme.colorScheme kullan)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isConnected) {
                MaterialTheme.colorScheme.tertiary // Yeşil (bağlı)
            } else {
                MaterialTheme.colorScheme.error // Kırmızı (kopuk)
            }
        )
    }
}
