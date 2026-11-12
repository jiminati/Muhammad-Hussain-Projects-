package com.vlektra.batterydoctor.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.Screen
import com.vlektra.batterydoctor.ui.theme.Eyebrow
import com.vlektra.batterydoctor.ui.theme.Vlk

@Composable
fun Shell(state: AppState) {

    if (state.screen == Screen.SPLASH) {
        SplashScreen(onFinished = { state.splashDone() })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Vlk.Ink)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Header(state)

        val enter = screenEnterProgress(state.screen)
        Box(
            Modifier
                .weight(1f)
                .graphicsLayer {
                    alpha = enter
                    translationY = (1f - enter) * 18f
                }
        ) {
            when (state.screen) {
                Screen.SPLASH -> Unit
                Screen.CONNECT -> ConnectScreen(state)
                Screen.HOME -> HomeScreen(state)
                Screen.ALARMS -> AlarmsScreen(state)
                Screen.ENGINEER -> PackScreen(state)
                Screen.PARAMETERS -> ParametersScreen(state)
                Screen.REPORT -> ReportScreen(state)
                Screen.ABOUT -> AboutScreen(state)
            }

            if (state.busy) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Vlk.Ink.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Vlk.Accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.height(16.dp))
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
            .background(
                Brush.verticalGradient(listOf(Vlk.Surface, Vlk.Ink))
            )
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 11.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 3.dp, height = 17.dp)
                        .background(Vlk.Accent, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    "VLEKTRA",
                    color = Vlk.Text,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.6.sp
                )
                Spacer(Modifier.width(8.dp))
                Text("BATTERY DOCTOR", style = Eyebrow, color = Vlk.TextFaint)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.connected != null) {
                    ModeToggle(state)
                    Spacer(Modifier.width(10.dp))
                }
                Box(
                    Modifier
                        .size(26.dp)
                        .background(
                            if (state.screen == Screen.ABOUT) Vlk.AccentSoft else Vlk.SurfaceHi,
                            RoundedCornerShape(8.dp)
                        )
                        .pressable { state.go(Screen.ABOUT) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "i",
                        color = if (state.screen == Screen.ABOUT) Vlk.Accent else Vlk.TextDim,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        state.connected?.let { pack ->
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(Vlk.Ok, RoundedCornerShape(999.dp))
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    listOfNotNull(pack.name, pack.knownChassis, pack.knownBatch)
                        .joinToString("  ·  "),
                    color = Vlk.TextFaint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}

/** Base mode / engineering mode, as a sliding pill. */
@Composable
private fun ModeToggle(state: AppState) {
    val on = state.engineeringMode
    val slide by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "modeSlide"
    )

    Row(
        Modifier
            .background(Vlk.SurfaceHi, RoundedCornerShape(999.dp))
            .pressable {
                state.engineeringMode = !state.engineeringMode
                if (!state.engineeringMode &&
                    (state.screen == Screen.ENGINEER || state.screen == Screen.PARAMETERS)
                ) {
                    state.go(Screen.HOME)
                }
            }
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(16.dp)
                .alpha(1f - slide * 0.65f)
                .background(
                    if (!on) Vlk.Accent else Color.Transparent,
                    RoundedCornerShape(999.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "B",
                color = if (!on) Color.White else Vlk.TextFaint,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(3.dp))
        Box(
            Modifier
                .size(16.dp)
                .alpha(0.35f + slide * 0.65f)
                .background(
                    if (on) Vlk.Watch else Color.Transparent,
                    RoundedCornerShape(999.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "E",
                color = if (on) Vlk.Ink else Vlk.TextFaint,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BottomTabs(state: AppState) {
    val tabs = buildList {
        add(Screen.HOME to "Battery")
        add(Screen.ALARMS to "Alarms")
        if (state.engineeringMode) {
            add(Screen.ENGINEER to "Readings")
            add(Screen.PARAMETERS to "Settings")
        }
        add(Screen.REPORT to "Report")
    }

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Vlk.Line)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(Vlk.Surface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { (screen, label) ->
                val active = state.screen == screen
                val width by animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = tween(220),
                    label = "tabBar"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pressable { state.go(screen) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .height(2.5.dp)
                            .width((20 * width).dp)
                            .background(Vlk.Accent, RoundedCornerShape(2.dp))
                    )
                    Text(
                        label,
                        color = if (active) Vlk.Text else Vlk.TextFaint,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
