package com.vlektra.batterydoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.Screen
import com.vlektra.batterydoctor.ui.theme.Vlk

@Composable
fun Shell(state: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Vlk.Ink)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Header(state)

        Box(Modifier.weight(1f)) {
            when (state.screen) {
                Screen.CONNECT -> ConnectScreen(state)
                Screen.PACK -> PackScreen(state)
                Screen.DIAGNOSIS -> DiagnosisScreen(state)
                Screen.HISTORY -> HistoryScreen(state)
                Screen.PARAMETERS -> ParametersScreen(state)
                Screen.REPORT -> ReportScreen(state)
                Screen.ABOUT -> AboutScreen(state)
            }

            if (state.busy) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Vlk.Ink.copy(alpha = 0.86f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Vlk.Accent, strokeWidth = 2.5.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            state.busyLabel,
                            color = Vlk.Text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (state.connected != null) BottomTabs(state)
    }
}

@Composable
private fun Header(state: AppState) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Vlk.Surface)
            .padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "VLK",
                    color = Vlk.Text,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    letterSpacing = (-0.8).sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "BATTERY DOCTOR",
                    color = Vlk.TextDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.6.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (state.connected != null) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(Vlk.Ok, RoundedCornerShape(999.dp))
                    )
                }
                Text(
                    "i",
                    color = if (state.screen == Screen.ABOUT) Vlk.Accent else Vlk.TextDim,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { state.go(Screen.ABOUT) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        state.connected?.let { pack ->
            Spacer(Modifier.height(3.dp))
            Text(
                listOfNotNull(pack.name, pack.knownChassis, pack.knownBatch)
                    .joinToString(" · "),
                color = Vlk.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp
            )
        }
    }
}

@Composable
private fun BottomTabs(state: AppState) {
    val tabs = listOf(
        Screen.PACK to "Pack",
        Screen.DIAGNOSIS to "Diagnosis",
        Screen.HISTORY to "Log",
        Screen.PARAMETERS to "Settings",
        Screen.REPORT to "Report"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .background(Vlk.Surface)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { (screen, label) ->
            val active = state.screen == screen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { state.go(screen) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .height(2.5.dp)
                        .width(if (active) 18.dp else 0.dp)
                        .background(
                            if (active) Vlk.Accent else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                )
                Text(
                    label,
                    color = if (active) Vlk.Text else Vlk.TextDim,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
