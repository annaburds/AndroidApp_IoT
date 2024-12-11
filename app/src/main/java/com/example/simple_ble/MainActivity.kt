// MainActivity.kt
package com.example.simple_ble

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.simple_ble.ui.theme.Simple_bleTheme

class MainActivity : ComponentActivity() {

    private val bleViewModel: BleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewModelSingleton.bleViewModel = bleViewModel

        setContent {
            Simple_bleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BLEApp(bleViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleViewModel.disconnect()
    }
}
