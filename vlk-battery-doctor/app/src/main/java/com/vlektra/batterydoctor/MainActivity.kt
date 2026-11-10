package com.vlektra.batterydoctor

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vlektra.batterydoctor.ui.Shell
import com.vlektra.batterydoctor.ui.theme.Vlk
import com.vlektra.batterydoctor.ui.theme.VlkTheme
import androidx.compose.foundation.layout.Box

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestBluetoothPermissions()
        setContent {
            VlkTheme {
                Box(Modifier.fillMaxSize().background(Vlk.Ink)) {
                    App()
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(needed)
    }
}

@Composable
private fun App() {
    val state: AppState = viewModel()
    Shell(state)
}
