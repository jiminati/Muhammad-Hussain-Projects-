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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.model.AccessTier
import com.vlektra.batterydoctor.ui.theme.Vlk
import androidx.compose.ui.text.font.FontFamily

@Composable
fun ParametersScreen(state: AppState) {
    val p = state.parameters
    if (p == null) {
        EmptyState("No parameters read", "Connect to a pack first.")
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
            Text("Parameters", style = MaterialTheme.typography.headlineSmall, color = Vlk.Text)
            Text(
                "Read from the BMS. Writing is disabled until the protocol is confirmed.",
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // The two settings that cause most false complaints
        Card {
            SectionLabel("Technician tier")
            ParamRow(
                "Capacity Learning",
                if (p.capacityLearning) "ON" else "OFF",
                AccessTier.TECHNICIAN,
                flagged = p.capacityLearning
            )
            ParamRow(
                "Active Balancing",
                if (p.activeBalancing) "ON" else "OFF",
                AccessTier.TECHNICIAN,
                flagged = !p.activeBalancing
            )
            ParamRow(
                "Balancing start",
                "${p.balanceStartMv} mV",
                AccessTier.TECHNICIAN
            )
            ParamRow(
                "Balancing start diff",
                "${p.balanceStartDiffMv} mV",
                AccessTier.TECHNICIAN
            )
            ParamRow("Balance current", "${p.balanceCurrentA} A", AccessTier.TECHNICIAN)
        }

        Card {
            SectionLabel("Engineer tier · voltage")
            ParamRow("Cell overvoltage", "${p.cellOverVoltageMv} mV", AccessTier.ENGINEER)
            ParamRow("Cell undervoltage", "${p.cellUnderVoltageMv} mV", AccessTier.ENGINEER)
            ParamRow("Pack overvoltage", "${p.packOverVoltageV} V", AccessTier.ENGINEER)
            ParamRow("Pack undervoltage", "${p.packUnderVoltageV} V", AccessTier.ENGINEER)
            ParamRow("Voltage difference", "${p.voltageDiffProtectMv} mV", AccessTier.ENGINEER)
        }

        Card {
            SectionLabel("Engineer tier · current")
            ParamRow("Charge overcurrent", "${p.chargeOverCurrentA} A", AccessTier.ENGINEER)
            ParamRow("Discharge overcurrent", "${p.dischargeOverCurrentA} A", AccessTier.ENGINEER)
            ParamRow("Short circuit trigger", "${p.shortCircuitTriggerA} A", AccessTier.ENGINEER)
        }

        Card {
            SectionLabel("Engineer tier · temperature")
            ParamRow("Charge high temp", "${p.chargeHighTempC} °C", AccessTier.ENGINEER)
            ParamRow("Charge low temp", "${p.chargeLowTempC} °C", AccessTier.ENGINEER)
            ParamRow("Discharge high temp", "${p.dischargeHighTempC} °C", AccessTier.ENGINEER)
            ParamRow("Discharge low temp", "${p.dischargeLowTempC} °C", AccessTier.ENGINEER)
            ParamRow("Temp difference", "${p.tempDifferenceC} °C", AccessTier.ENGINEER)
            ParamRow(
                "NTC disconnect protection",
                if (p.ntcDisconnectProtectionDisabled) "DISABLED" else "Enabled",
                AccessTier.ENGINEER,
                flagged = p.ntcDisconnectProtectionDisabled
            )
        }

        Card {
            SectionLabel("Locked · set at manufacture")
            ParamRow("Rated capacity", "${p.ratedCapacityAh} Ah", AccessTier.LOCKED)
            ParamRow("Cell strings", "${p.cellStrings}", AccessTier.LOCKED)
            ParamRow("SOC 0% point", "${p.soc0Mv} mV", AccessTier.LOCKED)
            ParamRow("SOC 100% point", "${p.soc100Mv} mV", AccessTier.LOCKED)
            ParamRow("Sleep time", "${p.sleepTimeS} s", AccessTier.LOCKED)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(Vlk.WatchBg, RoundedCornerShape(12.dp))
                .border(1.dp, Vlk.Watch.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(13.dp)
        ) {
            Text(
                "Writing is switched off in this build. When it is enabled, technician " +
                    "settings apply directly, engineer settings ask for a PIN, and every " +
                    "change is logged with who changed it, when, and from what to what.",
                color = Vlk.Watch,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = { },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = Vlk.SurfaceHi,
                disabledContentColor = Vlk.TextDim
            )
        ) {
            Text("Apply VLK standard profile", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ParamRow(
    label: String,
    value: String,
    tier: AccessTier,
    flagged: Boolean = false
) {
    val tierColour = when (tier) {
        AccessTier.TECHNICIAN -> Vlk.Ok
        AccessTier.ENGINEER -> Vlk.Watch
        AccessTier.LOCKED -> Vlk.TextDim
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier
                    .height(13.dp)
                    .width(2.5.dp)
                    .background(tierColour, RoundedCornerShape(2.dp))
            )
            Text(
                label,
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            value,
            color = if (flagged) Vlk.Shop else Vlk.Text,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp
        )
    }
}
