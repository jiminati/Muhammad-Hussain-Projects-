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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.vlektra.batterydoctor.diagnosis.RuleEngine
import com.vlektra.batterydoctor.model.PackSnapshot
import com.vlektra.batterydoctor.ui.theme.Vlk
import kotlin.math.roundToInt

@Composable
fun PackScreen(state: AppState) {
    val s = state.snapshot
    if (s == null) {
        EmptyState("No pack connected", "Go back and connect to a battery first.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        Column {
            Text(s.deviceName, style = MaterialTheme.typography.headlineSmall, color = Vlk.Text)
            Text(
                "${s.batteryCode} · ${s.cellCount}S · ${"%.0f".format(s.ratedAh)} Ah",
                color = Vlk.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        // Headline numbers
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Stat("Pack", "${"%.1f".format(s.packVoltage)} V", Vlk.Text, Modifier.weight(1f))
            Stat(
                "Current",
                "${"%.1f".format(s.current)} A",
                if (s.underLoad) Vlk.Accent else Vlk.Text,
                Modifier.weight(1f)
            )
            Stat(
                "Spread",
                "${s.deltaMv} mV",
                when {
                    s.deltaMv >= 100 -> Vlk.Shop
                    s.deltaMv >= 50 -> Vlk.Watch
                    else -> Vlk.Ok
                },
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Stat("SOC (BMS)", "${"%.0f".format(s.socPercent)} %", Vlk.Text, Modifier.weight(1f))
            Stat(
                "SOC (OCV)",
                "${RuleEngine.socFromMillivolts(s.averageMv).roundToInt()} %",
                Vlk.TextDim,
                Modifier.weight(1f)
            )
            Stat("Cycles", "${s.cycles}", Vlk.Text, Modifier.weight(1f))
        }

        if (!s.underLoad) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Vlk.WatchBg, RoundedCornerShape(11.dp))
                    .border(1.dp, Vlk.Watch.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "Pack is at rest. A weak cell often looks normal until the bike is " +
                        "running — read again under load if you can.",
                    color = Vlk.Watch,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        SectionLabel("Cell voltages")
        CellGrid(s)

        Card {
            SectionLabel("Temperatures")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                s.tempsC.forEachIndexed { i, t ->
                    Stat("T${i + 1}", "$t °C", Vlk.Text, Modifier.weight(1f))
                }
            }
            KeyValue("MOSFET", "${s.mosTempC} °C")
            KeyValue("Spread", "${s.tempSpreadC} °C")
        }

        Card {
            SectionLabel("Board")
            KeyValue("Charge MOS", if (s.chargeMosOn) "ON" else "OFF")
            KeyValue("Discharge MOS", if (s.dischargeMosOn) "ON" else "OFF")
            KeyValue("Balancing", if (s.balancingActive) "ACTIVE" else "IDLE")
            KeyValue("BMS serial", s.bmsSerial)
            KeyValue("Firmware", s.firmware, mono = true)
            KeyValue("Total usage", s.totalUsage)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { state.refresh() },
                enabled = !state.busy,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Re-read", color = Vlk.Text, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { state.go(Screen.ALARMS) },
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Vlk.Accent,
                    contentColor = Color.White
                )
            ) {
                Text("See alarms", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun CellGrid(s: PackSnapshot) {
    val lo = s.minCellMv
    val hi = s.maxCellMv
    val median = s.medianMv

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        s.cellsMv.chunked(4).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { colIndex, mv ->
                    val number = rowIndex * 4 + colIndex + 1
                    val collapsed = (median - mv) >= RuleEngine.Thresholds.COLLAPSE_MV
                    val isLow = mv == lo && s.deltaMv >= 20
                    val isHigh = mv == hi && s.deltaMv >= 20

                    val (border, fill) = when {
                        collapsed -> Vlk.Engineer to Vlk.EngineerBg
                        isLow -> Vlk.Shop to Vlk.ShopBg
                        isHigh -> Vlk.Ok to Vlk.Surface
                        else -> Vlk.Line to Vlk.Surface
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(fill, RoundedCornerShape(9.dp))
                            .border(1.dp, border, RoundedCornerShape(9.dp))
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "$number",
                            color = Vlk.TextDim,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                        Text(
                            "$mv",
                            color = if (collapsed || isLow) border else Vlk.Text,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        if (collapsed) {
                            Text(
                                "BUSBAR?",
                                color = Vlk.Engineer,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Vlk.Text)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = Vlk.TextDim
        )
    }
}
