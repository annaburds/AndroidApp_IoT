// BleViewModel.kt
package com.example.simple_ble

import android.Manifest
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Data class to represent a scanned BLE device along with its RSSI.
 */
data class ScannedDevice(
    val device: BluetoothDevice,
    val rssi: Int
)

/**
 * ViewModel to manage BLE operations.
 */
class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BLEViewModel"

    // UUID Constants
    val CONSOLE_SERVICE_UUID: UUID = UUID.fromString("E11D2E00-04AB-4DA5-B66A-EECB738F90F3")
    val READ_CHAR_UUID: UUID = UUID.fromString("E11D2E01-04AB-4DA5-B66A-EECB738F90F3")

    // BLE Components
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    // State Lists
//    val scanResults = mutableStateListOf<ScannedDevice>()
    val scanResults: SnapshotStateList<ScannedDevice> = mutableStateListOf()
    var isScanning = mutableStateOf(false)
    var isConnected = mutableStateOf(false)
    var receivedData = mutableStateOf<String?>(null)

    // Get Bluetooth Adapter
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = application.getSystemService(Application.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    /**
     * Start scanning for BLE devices.
     */
    fun startScan() {
        if (isScanning.value) return

        bluetoothAdapter?.bluetoothLeScanner?.let { scanner ->
            // Clear previous scan results
            scanResults.clear()

            // Define scan settings
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()

            // No scan filters to discover all devices
            val scanFilters = emptyList<ScanFilter>()

            // Define scan callback
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    super.onScanResult(callbackType, result)
                    result?.let { scanResult ->
                        val device = scanResult.device
                        val rssi = scanResult.rssi
                        Log.i(TAG, "Found device: ${device.name ?: "Unnamed"} - ${device.address} (RSSI: $rssi)")
                        // Avoid duplicates
                        if (!scanResults.any { it.device.address == device.address }) {
                            scanResults.add(ScannedDevice(device, rssi))
                        }
                    }
                }

                override fun onBatchScanResults(results: List<ScanResult?>?) {
                    super.onBatchScanResults(results)
                    results?.forEach { scanResult ->
                        scanResult?.let {
                            val device = it.device
                            val rssi = it.rssi
                            Log.i(TAG, "Found device (batch): ${device.name ?: "Unnamed"} - ${device.address} (RSSI: $rssi)")
                            if (!scanResults.any { scanned -> scanned.device.address == device.address }) {
                                scanResults.add(ScannedDevice(device, rssi))
                            }
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "Scan failed with error: $errorCode")
                }
            }

            // Start scanning
            scanner.startScan(scanFilters, scanSettings, scanCallback)
            isScanning.value = true
            Log.i(TAG, "Started scanning for BLE devices.")

            // Stop scanning after 10 seconds
            handler.postDelayed({
                scanner.stopScan(scanCallback)
                isScanning.value = false
                Log.i(TAG, "Stopped scanning for BLE devices.")
            }, 10000)
        }
    }

    /**
     * Connect to a selected BLE device.
     */
    fun connectToDevice(device: BluetoothDevice) {
        if (isConnected.value) return

        bluetoothGatt = device.connectGatt(getApplication(), false, gattCallback)
        Log.i(TAG, "Connecting to device: ${device.address}")
    }

    /**
     * Disconnect and close GATT connection.
     */
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        isConnected.value = false
        receivedData.value = null
        Log.i(TAG, "Disconnected from device.")
    }

    /**
     * BluetoothGattCallback to handle GATT events.
     */
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server.")
                    isConnected.value = true
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server.")
                    isConnected.value = false
                    receivedData.value = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(CONSOLE_SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(READ_CHAR_UUID)
                    if (characteristic != null) {
                        val properties = characteristic.properties
//                        if ((properties and BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                            // Read the characteristic
//                            val result = gatt.readCharacteristic(characteristic)
//                            Log.w(TAG, "Read characteristic initiated: $result")
//                        }
                        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            // Enable notifications
                            enableNotifications(gatt, characteristic)
                            Log.w(TAG, "test")
                        }
                    } else {
                        Log.w(TAG, "Characteristic not found")
                    }
                } else {
                    Log.w(TAG, "Service not found")
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }


        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                Log.w(TAG, "Characteristic read: ${data?.let { String(it) }}")
                receivedData.value = data?.let { String(it) } ?: "No Data"
            } else {
                Log.w(TAG, "Characteristic read failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            val data = characteristic.value
            val value = data[0].toInt()
            val val_str = "$value"

            Log.w(TAG, "Characteristic changed: ${val_str}}")
            receivedData.value = val_str
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Descriptor write successful for ${descriptor.characteristic.uuid}")
            } else {
                Log.w(TAG, "Descriptor write failed with status: $status")
            }
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor != null) {
            // Enable local notifications
            gatt.setCharacteristicNotification(characteristic, true)

            // Enable remote notifications
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            Log.w(TAG, "Enabling notifications for ${characteristic.uuid}")
        } else {
            Log.w(TAG, "Descriptor $cccdUuid not found for characteristic ${characteristic.uuid}")
        }
    }

}
