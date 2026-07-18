// TODO-D4.1: Real BLE Manager - Android GATT Client (Telefon → ESP32)
package com.example.isekaikuroshin.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * ✅ GÖREV D4.1: BLE Manager - Real Android GATT Client
 *
 * Röntgen Raporu Protokolü:
 * - Command TX (Write): Telefon → ESP32 (5 bytes: [CMD_ID:1][PARAM:4])
 * - Telemetry RX (Notify): ESP32 → Telefon (String: "P1:80,P2:90,T:50,S:95,M:REFLEKS")
 *
 * GATT UUIDs (ESP32 ile sync olmalı):
 * - Service: 4fafc201-1fb5-459e-8fcc-c5c9c331914b
 * - Command TX (Write): beb5483e-36e1-4688-b7f5-ea07361b26a8
 * - Telemetry RX (Notify): beb5483e-36e1-4688-b7f5-ea07361b26a9
 *
 * Permissions (AndroidManifest.xml):
 * - BLUETOOTH, BLUETOOTH_ADMIN (API < 31)
 * - BLUETOOTH_CONNECT, BLUETOOTH_SCAN (API ≥ 31)
 * - ACCESS_FINE_LOCATION (BLE scan için gerekli)
 */
@SuppressLint("MissingPermission")
object BleManager {
    private const val TAG = "BleManager"

    // GATT UUIDs (ESP32 ile sync - ESP-001'de tanımlanacak)
    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val COMMAND_TX_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val TELEMETRY_RX_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")

    // ✅ APP-006: Device name filter KALDIRILDI
    // Kök Neden: ESP32 advertising packet'inde isim truncate ediliyor
    // Çözüm: Sadece SERVICE_UUID ile filtrele (yukarıda tanımlı)

    // Android BLE objects
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var telemetryCharacteristic: BluetoothGattCharacteristic? = null

    // State flows
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastCommand = MutableStateFlow<DroneCommand?>(null)
    val lastCommand: StateFlow<DroneCommand?> = _lastCommand.asStateFlow()

    private val _telemetryData = MutableStateFlow<String?>(null)
    val telemetryData: StateFlow<String?> = _telemetryData.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String>("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    /**
     * Initialize BLE Manager with Android Context
     */
    fun initialize(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Log.d(TAG, "🔧 BLE Manager initialized")
    }

    /**
     * ✅ D4.1: BLE Scan & Connect to ESP32
     *
     * 1. Scan for BLE devices
     * 2. Filter by device name (contains "Isekai" or "Kolluk")
     * 3. Connect to GATT server
     * 4. Discover services & characteristics
     */
    fun connect(context: Context) {
        if (bluetoothAdapter == null) {
            initialize(context)
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "❌ Bluetooth is disabled or not available")
            _connectionStatus.value = "Bluetooth Disabled"
            return
        }

        Log.d(TAG, "🔍 Starting BLE scan for ESP32 (Kolluk)...")
        _connectionStatus.value = "Scanning..."

        // ✅ APP-006: Scan filter SERVICE_UUID ile (İsim yerine UUID kullan)
        // Kök Neden: ESP32 advertising packet'inde isim truncate ediliyor,
        // sadece SERVICE_UUID güvenilir şekilde yayınlanıyor
        val serviceUuid = ParcelUuid.fromString(SERVICE_UUID.toString())

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(serviceUuid) // ✅ İsim yerine SERVICE_UUID ile filtrele
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanner = bluetoothAdapter!!.bluetoothLeScanner
        scanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)

        Log.d(TAG, "📡 BLE scan started (filter: SERVICE_UUID = $SERVICE_UUID)")
        Log.d(TAG, "   ℹ️ Device name is NOT used for filtering (may be truncated in advertising packet)")
    }

    /**
     * BLE Scan Callback - Device bulunduğunda
     * ✅ APP-006: SERVICE_UUID ile filtreliyoruz, isim kontrolü kaldırıldı
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val device = it.device
                val deviceName = device.name ?: "Unknown"

                Log.d(TAG, "📱 Device found: $deviceName (${device.address})")

                // ✅ APP-006: SERVICE_UUID filtresi zaten uygulandı, direkt bağlan
                // İsim kontrolüne gerek yok (advertising packet'te truncate olabilir)
                Log.d(TAG, "✅ Target device found! (matched by SERVICE_UUID)")
                Log.d(TAG, "   Device Name: $deviceName")
                Log.d(TAG, "   MAC Address: ${device.address}")

                // Stop scan
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)

                // Connect to GATT
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "❌ BLE scan failed: $errorCode")
            _connectionStatus.value = "Scan Failed"
        }
    }

    /**
     * Connect to GATT Server (ESP32)
     */
    private fun connectToDevice(device: BluetoothDevice) {
        _connectionStatus.value = "Connecting..."

        // GATT connect
        bluetoothGatt = device.connectGatt(null, false, gattCallback)
        Log.d(TAG, "🔗 Connecting to GATT server: ${device.name}")
    }

    /**
     * GATT Callback - Connection events
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "✅ GATT connected! Discovering services...")
                    _connectionStatus.value = "Connected (Discovering...)"
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "❌ GATT disconnected")
                    _connectionStatus.value = "Disconnected"
                    _isConnected.value = false
                    bluetoothGatt = null
                    commandCharacteristic = null
                    telemetryCharacteristic = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "🔍 Services discovered! Looking for Kolluk service...")

                // Find our service
                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    Log.d(TAG, "✅ Kolluk service found!")

                    // Command TX (Write)
                    commandCharacteristic = service.getCharacteristic(COMMAND_TX_UUID)
                    if (commandCharacteristic != null) {
                        Log.d(TAG, "✅ Command TX characteristic found (Write)")
                    } else {
                        Log.e(TAG, "❌ Command TX characteristic NOT FOUND!")
                    }

                    // Telemetry RX (Notify)
                    telemetryCharacteristic = service.getCharacteristic(TELEMETRY_RX_UUID)
                    if (telemetryCharacteristic != null) {
                        Log.d(TAG, "✅ Telemetry RX characteristic found (Notify)")

                        // Enable notifications
                        gatt.setCharacteristicNotification(telemetryCharacteristic, true)

                        // Write descriptor (enable notify on ESP32 side)
                        val descriptor = telemetryCharacteristic!!.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Client Characteristic Configuration
                        )
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)

                        Log.d(TAG, "📡 Telemetry notifications enabled")
                    } else {
                        Log.e(TAG, "❌ Telemetry RX characteristic NOT FOUND!")
                    }

                    // Connection complete!
                    _isConnected.value = true
                    _connectionStatus.value = "Connected (Ready)"
                    Log.d(TAG, "🎉 BLE setup complete! Ready to send commands.")

                } else {
                    Log.e(TAG, "❌ Kolluk service NOT FOUND! UUID: $SERVICE_UUID")
                    _connectionStatus.value = "Service Not Found"
                    gatt?.disconnect()
                }
            } else {
                Log.e(TAG, "❌ Service discovery failed: $status")
                _connectionStatus.value = "Discovery Failed"
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            // ✅ GÖREV D4.1: Telemetry RX (Notify) - ESP32 → Telefon
            if (characteristic?.uuid == TELEMETRY_RX_UUID) {
                val data = characteristic?.value ?: return
                val telemetryString = String(data, Charsets.UTF_8)

                Log.d(TAG, "📥 Telemetry received: $telemetryString")
                _telemetryData.value = telemetryString

                // Parse and update DroneState
                val droneState = com.example.isekaikuroshin.ble.TelemetryParser.parse(telemetryString)
                if (droneState != null) {
                    com.example.isekaikuroshin.data.DroneStateManager.updateState(droneState)
                } else {
                    Log.w(TAG, "⚠️ Failed to parse telemetry: $telemetryString")
                }
            }
        }
    }

    /**
     * ✅ GÖREV D4.1: Send Command to ESP32 (Command TX - Write)
     *
     * Röntgen Protokol: [CMD_ID:1byte][PARAM:4bytes] (5 bytes total)
     */
    fun sendCommand(command: DroneCommand, param: Int = 0): DroneCommandResult {
        if (!_isConnected.value || commandCharacteristic == null) {
            Log.e(TAG, "❌ Cannot send command: Not connected or characteristic not found")
            return DroneCommandResult.NotConnected
        }

        // Encode command
        val packet = command.encode(param)

        Log.d(TAG, "📤 Sending command: $command (0x${command.commandId.toString(16).uppercase()})")
        Log.d(TAG, "   Description: ${command.description}")
        Log.d(TAG, "   Encoded: ${packet.joinToString(" ") { "0x%02X".format(it) }}")

        // Write to GATT characteristic
        commandCharacteristic!!.value = packet
        val success = bluetoothGatt?.writeCharacteristic(commandCharacteristic) ?: false

        return if (success) {
            _lastCommand.value = command
            Log.d(TAG, "✅ Command write queued successfully")
            DroneCommandResult.Success
        } else {
            Log.e(TAG, "❌ Command write failed")
            DroneCommandResult.Error("GATT write failed")
        }
    }

    /**
     * Disconnect from ESP32
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting from ESP32...")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _isConnected.value = false
        _connectionStatus.value = "Disconnected"
        _lastCommand.value = null
        _telemetryData.value = null
        Log.d(TAG, "❌ Disconnected")
    }

    /**
     * Check connection status
     */
    fun checkConnection(): Boolean {
        return _isConnected.value
    }
}
