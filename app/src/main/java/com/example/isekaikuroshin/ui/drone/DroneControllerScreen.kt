package com.example.isekaikuroshin.ui.drone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.isekaikuroshin.api.WeatherApi
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.google.android.gms.location.LocationServices
import com.example.isekaikuroshin.ble.TelemetryParser
import com.example.isekaikuroshin.data.DroneStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun DroneControllerScreen() {
    val screenTitle = rememberLocalizedText("nav_drone_controller")
    val context = LocalContext.current
    val activity = context as? Activity

    // 🔄 Ekran Yönlendirme State
    var isLandscapeMode by remember { mutableStateOf(false) }

    // ✅ APP-004/005: Bluetooth + Location Runtime Permission State
    var hasBluetoothPermission by remember { mutableStateOf(false) }

    // ✅ APP-007: GPS/Location Services State
    var isGpsEnabled by remember { mutableStateOf(false) }

    // Check Bluetooth + Location permissions
    LaunchedEffect(Unit) {
        hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 ve altı: Sadece ACCESS_FINE_LOCATION (BLE scan için zorunlu)
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        // ✅ APP-007: Check GPS/Location Services
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                       locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // ✅ APP-004/005: Bluetooth + Location Permission Launcher
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: 3 izin gerekli
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        } else {
            // Android 11 ve altı: Sadece konum izni gerekli
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }

        if (hasBluetoothPermission) {
            android.util.Log.d("DroneController", "✅ All permissions granted! (Bluetooth + Location)")
        } else {
            android.util.Log.e("DroneController", "❌ Permissions denied!")
            // Log hangi izinlerin reddedildiğini
            permissions.forEach { (permission, granted) ->
                if (!granted) {
                    android.util.Log.e("DroneController", "   → Denied: $permission")
                }
            }
        }
    }

    // ✅ GÖREV: Gerçek BLE bağlantısını dinle (Mock kaldırıldı)
    // BleManager otomatik olarak telemetri verisini DroneStateManager'a aktarıyor

    // Ekran kapandığında orientation'ı serbest bırak
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Scroll state (landscape modda gerekli)
    val scrollState = rememberScrollState()

    // ✅ APP-003: BoxWithConstraints ile responsive layout (KURAL: Ekran yan çevrildiğinde özel görünüm)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isWideScreen = maxWidth > maxHeight
        val contentPadding: Dp = if (isWideScreen) 12.dp else 16.dp
        val spacingLarge: Dp = if (isWideScreen) 8.dp else 16.dp
        val spacingMedium: Dp = if (isWideScreen) 6.dp else 12.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header + Orientation Toggle Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = screenTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // 🔄 Yatay/Dikey Ekran Butonu
            IconButton(
                onClick = {
                    isLandscapeMode = !isLandscapeMode

                    // ✅ FIX: Orientation'ı SADECE buton tıklandığında değiştir
                    activity?.requestedOrientation = if (isLandscapeMode) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }

                    android.util.Log.d("DroneController", "🔄 Orientation toggled: ${if (isLandscapeMode) "Landscape" else "Portrait"}")
                }
            ) {
                Icon(
                    imageVector = if (isLandscapeMode) Icons.Default.ScreenRotation else Icons.Default.ScreenLockRotation,
                    contentDescription = rememberLocalizedText("ORIENTATION_TOGGLE"),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ Canlı Bağlantı Durumu ve Hata Göstergeleri
        ConnectionStatusBar()

        Spacer(modifier = Modifier.height(8.dp))

        // TODO-D4.0: Geçici BLE Bağlantı Butonları (ESP32 test için)
        TemporaryBleConnectionButtons(
            context = context,
            hasBluetoothPermission = hasBluetoothPermission,
            isGpsEnabled = isGpsEnabled,
            onRequestPermission = {
                // ✅ APP-005: Android sürümüne göre gerekli izinleri iste
                val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+: Bluetooth + Location izinleri
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    // Android 11 ve altı: Sadece Location izni (BLE scan için zorunlu)
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }

                android.util.Log.d("DroneController", "📋 Requesting ${requiredPermissions.size} permissions...")
                bluetoothPermissionLauncher.launch(requiredPermissions)
            }
        )

        Spacer(modifier = Modifier.height(spacingLarge))

        // Landscape modda kompakt layout (isWideScreen veya isLandscapeMode)
        if (isWideScreen || isLandscapeMode) {
            // Yatay modda: Yan yana düzen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sol kolon: Telemetri + Weather
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryPanel()
                    WeatherRiskPanel()
                }

                // Sağ kolon: Komut Butonları
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    MockControlButtons()
                }
            }
        } else {
            // Dikey modda: Normal düzen
            WeatherRiskPanel()
            Spacer(modifier = Modifier.height(spacingLarge))
            TelemetryPanel()
            Spacer(modifier = Modifier.height(spacingLarge))
            MockControlButtons()
        }
    }
    } // BoxWithConstraints kapanışı
}

@Composable
private fun MockStatusPanel() {
    val statusTitle = rememberLocalizedText("drone_status_indicators")
    val batteryLabel = rememberLocalizedText("drone_battery")
    val signalLabel = rememberLocalizedText("drone_signal")
    val modeLabel = rememberLocalizedText("drone_mode")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📡 $statusTitle",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• $batteryLabel: -- %\n• $signalLabel: -- %\n• $modeLabel: --",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * ✅ GÖREV D1: Veri Gönderme - Komut Butonları (Telefon → ESP32)
 * 7 kontrol butonu + Mod değiştirici + 4 takla butonu
 */
@Composable
private fun MockControlButtons() {
    val controlTitle = rememberLocalizedText("drone_control_buttons")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎮 $controlTitle",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🛡️ Güvenli Başlatma + 🪂 Akıllı İniş
        // 🛡️ PRIMARY: Kritik Güvenlik Kontrolleri
        Text(
            text = rememberLocalizedText("drone_status_security"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DroneCommandButton(
                textKey = "SAFE_ARM",
                icon = Icons.Default.FlightTakeoff,
                command = com.example.isekaikuroshin.ble.DroneCommand.SAFE_ARM,
                priority = ButtonPriority.PRIMARY
            )
            DroneCommandButton(
                textKey = "SMART_LANDING",
                icon = Icons.Default.FlightLand,
                command = com.example.isekaikuroshin.ble.DroneCommand.SMART_LANDING,
                priority = ButtonPriority.PRIMARY
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ⚙️ SECONDARY: Uçuş Modları
        Text(
            text = rememberLocalizedText("drone_status_flight_modes"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DroneCommandButton(
                textKey = "CRUISE_TOGGLE",
                icon = Icons.Default.Lock,
                command = com.example.isekaikuroshin.ble.DroneCommand.CRUISE_TOGGLE,
                priority = ButtonPriority.SECONDARY
            )
            DroneFlightModeToggle()
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🎯 TERTIARY: Makro Komutlar
        Text(
            text = rememberLocalizedText("drone_status_macro_commands"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DroneCommandButton(
                textKey = "FLIP_FORWARD",
                icon = Icons.Default.ArrowUpward,
                command = com.example.isekaikuroshin.ble.DroneCommand.FLIP_FORWARD
            )
            DroneCommandButton(
                textKey = "FLIP_BACKWARD",
                icon = Icons.Default.ArrowDownward,
                command = com.example.isekaikuroshin.ble.DroneCommand.FLIP_BACKWARD
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DroneCommandButton(
                textKey = "FLIP_LEFT",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                command = com.example.isekaikuroshin.ble.DroneCommand.FLIP_LEFT
            )
            DroneCommandButton(
                textKey = "FLIP_RIGHT",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                command = com.example.isekaikuroshin.ble.DroneCommand.FLIP_RIGHT
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🧭 Pusula Kalibrasyonu + 🤲 Akıllı Yakalama
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DroneCommandButton(
                textKey = "COMPASS_CALIBRATION",
                icon = Icons.Default.Explore,
                command = com.example.isekaikuroshin.ble.DroneCommand.COMPASS_CALIBRATION
            )
            DroneCommandButton(
                textKey = "SMART_CATCH",
                icon = Icons.Default.PanTool,
                command = com.example.isekaikuroshin.ble.DroneCommand.SMART_CATCH
            )
        }
    }
}

/**
 * ✅ GÖREV D1: Drone Komut Butonu
 * ✅ GÖREV D4.3: VFX - Görsel Geri Bildirim
 * - BLE komut gönderir (şimdilik mock)
 * - Material3 Button ripple effects
 * - State-based color changes (KURAL 10: MaterialTheme.colorScheme)
 * - Scale animation on press
 * - KURAL 9: rememberLocalizedText() kullan (hardcoded text YOK)
 */
enum class ButtonPriority { PRIMARY, SECONDARY, TERTIARY }

@Composable
private fun DroneCommandButton(
    textKey: String,
    icon: ImageVector,
    command: com.example.isekaikuroshin.ble.DroneCommand,
    priority: ButtonPriority = ButtonPriority.TERTIARY
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val buttonText = rememberLocalizedText(textKey)

    // ⚡ KURAL 14: Performance - remember ile animation state cache
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    // Priority'ye göre stil
    val (buttonWidth, iconSize, textStyle, buttonColors) = when (priority) {
        ButtonPriority.PRIMARY -> {
            Tuple4(
                180.dp,
                28.dp,
                MaterialTheme.typography.labelLarge,
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        ButtonPriority.SECONDARY -> {
            Tuple4(
                160.dp,
                24.dp,
                MaterialTheme.typography.labelMedium,
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        ButtonPriority.TERTIARY -> {
            Tuple4(
                150.dp,
                20.dp,
                MaterialTheme.typography.labelSmall,
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }

    // Button onClick handler
    val onClickHandler: () -> Unit = {
        isPressed = true
        val result = com.example.isekaikuroshin.ble.BleManager.sendCommand(command)
        android.util.Log.d("DroneController", "📤 Command: $command → $result")
        coroutineScope.launch {
            delay(100)
            isPressed = false
        }
    }

    when (priority) {
        ButtonPriority.SECONDARY -> {
            OutlinedButton(
                onClick = onClickHandler,
                modifier = Modifier.width(buttonWidth).scale(scale),
                shape = RoundedCornerShape(12.dp),
                colors = buttonColors
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = buttonText,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buttonText,
                        style = textStyle
                    )
                }
            }
        }
        else -> {
            Button(
                onClick = onClickHandler,
                modifier = Modifier.width(buttonWidth).scale(scale),
                shape = RoundedCornerShape(12.dp),
                colors = buttonColors
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = buttonText,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buttonText,
                        style = textStyle
                    )
                }
            }
        }
    }
}

// Helper data class
private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/**
 * ✅ GÖREV D1: Uçuş Modu Değiştirici (Refleks ⇄ Sinematik)
 */
@Composable
private fun DroneFlightModeToggle() {
    var isReflexMode by remember { mutableStateOf(true) }

    val currentMode = if (isReflexMode) "MODE_REFLEX" else "MODE_CINEMATIC"
    val currentIcon = if (isReflexMode) Icons.Default.FlashOn else Icons.Default.Videocam
    val command = if (isReflexMode)
        com.example.isekaikuroshin.ble.DroneCommand.MODE_REFLEX
    else
        com.example.isekaikuroshin.ble.DroneCommand.MODE_CINEMATIC

    Button(
        onClick = {
            // TODO-D1: BLE komut gönder (şimdilik mock)
            val result = com.example.isekaikuroshin.ble.BleManager.sendCommand(command)
            android.util.Log.d("DroneController", "📤 Mode: $command → $result")

            // Modu değiştir
            isReflexMode = !isReflexMode
        },
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = currentIcon,
                contentDescription = rememberLocalizedText(currentMode),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rememberLocalizedText(currentMode),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * ✅ GÖREV D3: Çevresel Risk Paneli
 * - GPS ile konum al
 * - OpenWeatherMap API ile hava durumu çek
 * - Yağmur riski % ve Rüzgar hızı göster
 * - KURAL 9: rememberLocalizedText() kullan
 * - KURAL 10: MaterialTheme.colorScheme kullan
 */
@Composable
private fun WeatherRiskPanel() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val panelTitle = rememberLocalizedText("drone_weather_risk")
    val rainLabel = rememberLocalizedText("drone_rain_chance")
    val windLabel = rememberLocalizedText("drone_wind_speed")

    var rainChance by remember { mutableStateOf<Int?>(null) }
    var windSpeed by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Check location permission
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Fetch weather data
            coroutineScope.launch {
                fetchWeatherData(context) { rain, wind, error ->
                    rainChance = rain
                    windSpeed = wind
                    errorMessage = error
                    isLoading = false
                }
            }
        }
    }

    // Risk durumu belirleme (sadece gerçek risk varsa kırmızı)
    val isHighRisk = (rainChance ?: 0) > 70 || (windSpeed ?: 0.0) > 30.0
    val cardColor = if (isHighRisk) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isHighRisk) Icons.Default.Warning else Icons.Default.Cloud,
                    contentDescription = panelTitle,
                    tint = if (isHighRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = panelTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasLocationPermission) {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(rememberLocalizedText("drone_enable_gps"))
                }
            } else if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "$rainLabel:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (rainChance != null) "$rainChance%" else "--",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isHighRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = "$windLabel:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (windSpeed != null) "${String.format("%.1f", windSpeed)} km/h" else "--",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isHighRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Refresh button
                TextButton(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            fetchWeatherData(context) { rain, wind, error ->
                                rainChance = rain
                                windSpeed = wind
                                errorMessage = error
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(rememberLocalizedText("drone_refresh"))
                }
            }
        }
    }
}

/**
 * Fetch weather data using GPS + OpenWeatherMap API
 */
private suspend fun fetchWeatherData(
    context: Context,
    onResult: (rainChance: Int?, windSpeed: Double?, error: String?) -> Unit
) {
    try {
        // Get location
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(null, null, "Location permission denied")
            return
        }

        val location = fusedLocationClient.lastLocation.await()

        if (location == null) {
            onResult(null, null, "Unable to get location")
            return
        }

        android.util.Log.d("DroneController", "📍 Location: ${location.latitude}, ${location.longitude}")

        // TODO-D3: Replace with real API key
        if (WeatherApi.API_KEY == "YOUR_API_KEY_HERE") {
            // Mock data for demo
            onResult(25, 12.5, null)
            android.util.Log.d("DroneController", "🌦️ Using mock weather data (API key not configured)")
            return
        }

        // Fetch weather from API
        val weather = WeatherApi.service.getCurrentWeather(
            latitude = location.latitude,
            longitude = location.longitude,
            apiKey = WeatherApi.API_KEY
        )

        val rainChance = if (weather.rain != null) {
            ((weather.rain.`1h` ?: 0.0) * 10).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val windSpeedKmh = weather.wind.speed * 3.6 // m/s to km/h

        android.util.Log.d("DroneController", "🌦️ Weather: Rain=$rainChance%, Wind=${String.format("%.1f", windSpeedKmh)} km/h")

        onResult(rainChance, windSpeedKmh, null)

    } catch (e: Exception) {
        android.util.Log.e("DroneController", "❌ Weather fetch error", e)
        onResult(null, null, "Weather fetch failed: ${e.message}")
    }
}

/**
 * ✅ GÖREV D4.0: Geçici BLE Bağlantı Butonları
 * ESP32 ile manuel test için (D4.1 tamamlanınca kaldırılacak)
 *
 * KURAL 9: rememberLocalizedText() kullanılıyor
 * KURAL 10: MaterialTheme.colorScheme kullanılıyor
 */
@Composable
private fun TemporaryBleConnectionButtons(
    context: Context,
    hasBluetoothPermission: Boolean,
    isGpsEnabled: Boolean,
    onRequestPermission: () -> Unit
) {
    // BLE bağlantı durumunu izle
    val isConnected by com.example.isekaikuroshin.ble.BleManager.isConnected.collectAsState()

    // ✅ APP-007: GPS warning dialog state
    var showGpsWarning by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bağlan Butonu
        Button(
            onClick = {
                // ✅ APP-004: Runtime permission check BEFORE connecting
                if (!hasBluetoothPermission) {
                    android.util.Log.w("DroneController", "⚠️ Bluetooth permission not granted, requesting...")
                    onRequestPermission()
                    return@Button
                }

                // ✅ APP-007: GPS/Location Services check BEFORE connecting
                if (!isGpsEnabled) {
                    android.util.Log.w("DroneController", "⚠️ GPS/Location Services disabled, showing warning...")
                    showGpsWarning = true
                    return@Button
                }

                android.util.Log.d("DroneController", "🔗 [BAŞLADI] BLE Bağlantı butonu basıldı")
                try {
                    com.example.isekaikuroshin.ble.BleManager.connect(context)
                    android.util.Log.d("DroneController", "✅ [BAŞARILI] BLE connect() çağrıldı")
                } catch (e: SecurityException) {
                    android.util.Log.e("DroneController", "❌ [HATA] SecurityException: ${e.message}")
                    android.util.Log.e("DroneController", "   → Bluetooth izinleri eksik olabilir")
                } catch (e: IllegalStateException) {
                    android.util.Log.e("DroneController", "❌ [HATA] IllegalStateException: ${e.message}")
                    android.util.Log.e("DroneController", "   → BLE durumu geçersiz")
                } catch (e: Exception) {
                    android.util.Log.e("DroneController", "❌ [HATA] Beklenmeyen hata: ${e.javaClass.simpleName}")
                    android.util.Log.e("DroneController", "   → Mesaj: ${e.message}")
                    android.util.Log.e("DroneController", "   → Stack trace:", e)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !isConnected,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(rememberLocalizedText("drone_connect"))
        }

        // Kes Butonu
        Button(
            onClick = {
                android.util.Log.d("DroneController", "🔌 [BAŞLADI] BLE Kesme butonu basıldı")
                try {
                    com.example.isekaikuroshin.ble.BleManager.disconnect()
                    android.util.Log.d("DroneController", "✅ [BAŞARILI] BLE disconnect() çağrıldı")
                } catch (e: Exception) {
                    android.util.Log.e("DroneController", "❌ [HATA] Disconnect hatası: ${e.message}", e)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = isConnected,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(rememberLocalizedText("drone_disconnect"))
        }
    }

    // Bağlantı durumu göstergesi
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Durum ikonu
        Icon(
            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        // Durum metni
        Text(
            text = rememberLocalizedText(if (isConnected) "drone_ble_connected_mock" else "drone_ble_disconnected"),
            style = MaterialTheme.typography.bodySmall,
            color = if (isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }

    // ✅ APP-007: GPS Warning Dialog
    if (showGpsWarning) {
        AlertDialog(
            onDismissRequest = { showGpsWarning = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = rememberLocalizedText("drone_gps_disabled_title"),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = rememberLocalizedText("drone_gps_disabled_message"),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Open Location Settings
                        try {
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            context.startActivity(intent)
                            showGpsWarning = false
                        } catch (e: Exception) {
                            android.util.Log.e("DroneController", "❌ Failed to open GPS settings: ${e.message}")
                        }
                    }
                ) {
                    Text(rememberLocalizedText("drone_gps_open_settings"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGpsWarning = false }
                ) {
                    Text(rememberLocalizedText("drone_gps_dismiss"))
                }
            }
        )
    }
}

// ============================================================================
// ✅ CANLI BAĞLANTI DURUMU VE HATA GÖSTERGELERİ
// ============================================================================

/**
 * Bağlantı Durumu Barı
 * - 🟢 Bağlı / 🔴 Bağlantı Aranıyor
 * - Timeout durumu (2 saniye)
 * - Sensör hata mesajları
 */
@Composable
private fun ConnectionStatusBar() {
    val droneState by DroneStateManager.state.collectAsState()

    // Durum belirleme
    val statusText: String
    val statusColor: Color
    val statusIcon: ImageVector

    when {
        // Sensör hatası var
        droneState.sensorStatus != com.example.isekaikuroshin.data.SensorStatus.OK -> {
            statusText = droneState.sensorStatus.toUserMessage()
            statusColor = MaterialTheme.colorScheme.error
            statusIcon = Icons.Default.Error
        }
        // Timeout (2 saniyedir veri gelmedi)
        droneState.isTimedOut() && droneState.isConnected -> {
            statusText = rememberLocalizedText("drone_status_timeout")
            statusColor = MaterialTheme.colorScheme.tertiary
            statusIcon = Icons.Default.WarningAmber
        }
        // Bağlı ve veri geliyor
        droneState.isConnected && !droneState.isTimedOut() -> {
            statusText = rememberLocalizedText("drone_status_connected")
            statusColor = MaterialTheme.colorScheme.tertiary
            statusIcon = Icons.Default.CheckCircle
        }
        // Bağlantı yok (alarm değil, bilgi)
        else -> {
            statusText = rememberLocalizedText("drone_status_searching")
            statusColor = MaterialTheme.colorScheme.primary
            statusIcon = Icons.Default.BluetoothSearching
        }
    }

    // UI
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // İkon (animasyonlu)
            val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (droneState.isConnected && droneState.isDataValid()) 1f else 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "status_icon_scale"
            )

            Icon(
                imageVector = statusIcon,
                contentDescription = "Connection Status",
                tint = statusColor,
                modifier = Modifier.scale(scale)
            )

            // Durum metni
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                modifier = Modifier.weight(1f)
            )

            // Son güncelleme zamanı (sadece veri geldiğinde)
            if (droneState.lastUpdateTimestamp > 0) {
                val timeSinceUpdate = (System.currentTimeMillis() - droneState.lastUpdateTimestamp) / 1000
                Text(
                    text = "${timeSinceUpdate}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Mock telemetri simülatörü kaldırıldı - Gerçek BLE verisi kullanılıyor
