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
        requestPermissions()
        setContent {
            VlkTheme {
                Box(Modifier.fillMaxSize().background(Vlk.Ink)) {
                    App()
                }
            }
        }
    }

    private fun requestPermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // Fills the City field on the report; the technician can always type it.
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        permissionLauncher.launch(needed.toTypedArray())
    }
}

@Composable
private fun App() {
    val state: AppState = viewModel()
    Shell(state)
}
