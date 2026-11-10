package com.vlektra.batterydoctor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.ui.theme.Vlk

@Composable
fun AboutScreen(state: AppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Text("About", style = MaterialTheme.typography.headlineSmall, color = Vlk.Text)

        Card {
            SectionLabel("Build")
            KeyValue("Version", "0.1.0")
            KeyValue("Data source", state.source.sourceLabel)
            KeyValue("Live hardware", if (state.source.isLive) "yes" else "no")
        }

        Card {
            SectionLabel("What this app decides")
            Text(
                "It reads a DALY BMS, applies Vlektra's own field rules to the cell " +
                    "voltages, the fault log and the configuration, and gives one verdict " +
                    "with the steps that follow from it.",
                color = Vlk.Text.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "A verdict is a starting point, not a replacement for the engineer's " +
                    "judgement. Every finding shows the numbers it came from so it can be " +
                    "checked.",
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Card {
            SectionLabel("Rules in this build")
            listOf(
                "Cell 7 or 18 collapsed on Batch 6 → busbar break, repair line",
                "Any cell 300 mV below median → connection or dead cell",
                "Spread above 100 mV → overnight balancing at 2900 mV",
                "Lowest cell under 2800 mV → supervised recovery charge",
                "3 or more voltage sampling faults → sense harness",
                "Short circuit protection fired → engineer, check wiring",
                "Capacity Learning on → the commonest false range complaint",
                "Active balancing off → imbalance will grow every cycle"
            ).forEach {
                Text("— $it", color = Vlk.TextDim, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "VLEKTRA R&D",
            color = Vlk.TextDim,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Developed by Sen",
            color = Vlk.Accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
