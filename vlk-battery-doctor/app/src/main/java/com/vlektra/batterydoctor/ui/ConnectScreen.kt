package com.vlektra.batterydoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.vlektra.batterydoctor.model.DiscoveredPack
import com.vlektra.batterydoctor.ui.theme.Vlk

@Composable
fun ConnectScreen(state: AppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        Text(
            text = "Connect to a pack",
            style = MaterialTheme.typography.displaySmall,
            color = Vlk.Text
        )
        Text(
            text = "Turn the bike key on, then pick the battery. " +
                "The BMS name starts with JHB or DL.",
            style = MaterialTheme.typography.bodyMedium,
            color = Vlk.TextDim
        )

        // Source switch
        Card {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (state.useSimulator) "Demo packs" else "Live Bluetooth",
                        color = Vlk.Text,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (state.useSimulator)
                            "Three real Vlektra packs, no hardware needed"
                        else
                            "Reads a real BMS — needs the DALY protocol",
                        color = Vlk.TextDim,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = !state.useSimulator,
                    onCheckedChange = { state.setSimulator(!it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Vlk.Accent,
                        uncheckedThumbColor = Vlk.TextDim,
                        uncheckedTrackColor = Vlk.SurfaceHi
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(
                if (state.found.isEmpty()) "Nearby" else "Nearby · ${state.found.size} found"
            )
            if (state.scanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.6.dp,
                        color = Vlk.Accent
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Scanning",
                        color = Vlk.Accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (state.found.isEmpty() && !state.scanning) {
            Card {
                Text(
                    text = "No packs yet",
                    color = Vlk.Text,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Tap Scan below. Stand within a few metres of the bike " +
                        "with the key turned on.",
                    color = Vlk.TextDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        state.found.forEach { pack ->
            PackRow(pack) { state.connectAndRead(pack) }
        }

        state.error?.let { msg ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Vlk.EngineerBg, RoundedCornerShape(12.dp))
                    .border(1.dp, Vlk.Engineer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(13.dp)
            ) {
                Text(msg, color = Vlk.Engineer, style = MaterialTheme.typography.bodySmall)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(Vlk.Accent.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                .padding(13.dp)
        ) {
            Text(
                text = "Reading is always safe. Nothing is written to the BMS until " +
                    "you open Parameters and confirm.",
                color = Vlk.Text.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = { state.startScan() },
            enabled = !state.scanning && !state.busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Vlk.Accent,
                contentColor = Color.White,
                disabledContainerColor = Vlk.SurfaceHi,
                disabledContentColor = Vlk.TextDim
            )
        ) {
            Text(
                if (state.scanning) "Scanning…" else "Scan for packs",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun PackRow(pack: DiscoveredPack, onClick: () -> Unit) {
    val strong = pack.rssi > -65
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Vlk.Surface, RoundedCornerShape(14.dp))
            .border(
                width = if (strong) 1.5.dp else 1.dp,
                color = if (strong) Vlk.Accent else Vlk.Line,
                shape = RoundedCornerShape(14.dp)
            )
            .pressable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(
                    if (strong) Vlk.Accent else Vlk.SurfaceHi,
                    RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "VLK",
                color = if (strong) Color.White else Vlk.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = pack.name,
                color = Vlk.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
            )
            Text(
                text = listOfNotNull(
                    pack.knownChassis,
                    pack.knownBatch
                ).joinToString(" · ").ifBlank { "Not in the pack register" },
                color = Vlk.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            SignalBars(pack.rssi)
            Spacer(Modifier.height(4.dp))
            Text(
                "${pack.rssi}",
                color = Vlk.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SignalBars(rssi: Int) {
    val level = when {
        rssi > -60 -> 4
        rssi > -70 -> 3
        rssi > -80 -> 2
        else -> 1
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..4).forEach { i ->
            Box(
                Modifier
                    .width(3.dp)
                    .height((5 + i * 3).dp)
                    .background(
                        if (i <= level) Vlk.Accent else Vlk.Line,
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
