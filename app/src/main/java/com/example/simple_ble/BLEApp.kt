// BLEApp.kt (can be in the same file as MainActivity.kt or separate)
package com.example.simple_ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun BLEApp(bleViewModel: BleViewModel) {
    val context = LocalContext.current

    // Request location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                bleViewModel.startScan()
            } else {
                // Handle permission denial
                Log.e("BLEApp", "Location permission denied.")
            }
        }
    )

    // Request permission on first launch
    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                bleViewModel.startScan()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // UI Components
    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize()
    ) {
        if (bleViewModel.isScanning.value) { // Access .value
            Text(text = "Scanning for BLE devices...", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            if (bleViewModel.scanResults.isNotEmpty()) { // Access .value
                Text(text = "Discovered Devices:", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(bleViewModel.scanResults) { scannedDevice -> // Access .value
                        DeviceItem(device = scannedDevice.device, rssi = scannedDevice.rssi) {
                            bleViewModel.connectToDevice(scannedDevice.device)
                        }
                        Divider()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    bleViewModel.startScan()
                }) {
                    Text("Rescan")
                }
            } else {
                Text(text = "No devices found.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    bleViewModel.startScan()
                }) {
                    Text("Start Scanning")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (bleViewModel.isConnected.value) { // Access .value
            Text(text = "Connected to device.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            if (bleViewModel.receivedData.value != null) { // Access .value
                Text(text = "Received Data: ${bleViewModel.receivedData.value}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = "Reading data...", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun DeviceItem(device: BluetoothDevice, rssi: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(text = device.name ?: "Unnamed Device", style = MaterialTheme.typography.titleMedium)
        Text(text = device.address, style = MaterialTheme.typography.bodyMedium)
        Text(text = "RSSI: $rssi dBm", style = MaterialTheme.typography.bodySmall)
    }
}