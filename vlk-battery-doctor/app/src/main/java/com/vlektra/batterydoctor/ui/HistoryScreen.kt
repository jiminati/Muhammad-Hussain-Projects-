package com.vlektra.batterydoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.model.EventCategory
import com.vlektra.batterydoctor.model.HistoryEvent
import com.vlektra.batterydoctor.ui.theme.Vlk

@Composable
fun HistoryScreen(state: AppState) {
    if (state.history.isEmpty()) {
        EmptyState(
            "No history read",
            "Connect to a pack. The fault log is read automatically."
        )
        return
    }

    var hideRoutine by remember { mutableStateOf(true) }

    val shown = remember(state.history, hideRoutine) {
        if (hideRoutine) state.history.filter { it.category != EventCategory.POWER }
        else state.history
    }

    val counts = remember(state.history) {
        state.history.groupingBy { it.category }.eachCount()
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
            Text("Fault log", style = MaterialTheme.typography.headlineSmall, color = Vlk.Text)
            Text(
                "${state.history.size} entries, newest first",
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // What matters, counted
        Card {
            SectionLabel("Summary")
            val interesting = listOf(
                EventCategory.SHORT_CIRCUIT to "Short circuit",
                EventCategory.VOLTAGE_SENSE to "Voltage sampling",
                EventCategory.TEMP_SENSE to "Temp sampling",
                EventCategory.TEMP_DIFFERENCE to "Temp difference",
                EventCategory.CELL_UNDERVOLTAGE to "Cell undervoltage",
                EventCategory.CELL_OVERVOLTAGE to "Cell overvoltage",
                EventCategory.OVERCURRENT to "Overcurrent",
                EventCategory.CHARGE_TEMP_HIGH to "Charged hot",
                EventCategory.CHARGE_TEMP_LOW to "Charged cold",
                EventCategory.SOC_LOW to "SOC low"
            ).filter { (counts[it.first] ?: 0) > 0 }

            if (interesting.isEmpty()) {
                Text(
                    "Only routine power-on and sleep entries. Nothing to investigate.",
                    color = Vlk.Ok,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                interesting.forEach { (cat, label) ->
                    KeyValue(label, "${counts[cat]}")
                }
            }
            KeyValue("Power on / sleep", "${counts[EventCategory.POWER] ?: 0}")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Vlk.Surface, RoundedCornerShape(11.dp))
                .border(1.dp, Vlk.Line, RoundedCornerShape(11.dp))
                .clickable { hideRoutine = !hideRoutine }
                .padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (hideRoutine) "Hiding power on / sleep entries"
                else "Showing every entry",
                color = Vlk.Text,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (hideRoutine) "Show all" else "Hide routine",
                color = Vlk.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            )
        }

        SectionLabel("Entries")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shown.forEach { EventRow(it) }
        }
    }
}

@Composable
private fun EventRow(e: HistoryEvent) {
    val colour = when (e.category) {
        EventCategory.SHORT_CIRCUIT -> Vlk.Engineer
        EventCategory.VOLTAGE_SENSE -> Vlk.Engineer
        EventCategory.TEMP_SENSE, EventCategory.TEMP_DIFFERENCE -> Vlk.Shop
        EventCategory.CELL_UNDERVOLTAGE, EventCategory.CELL_OVERVOLTAGE -> Vlk.Watch
        EventCategory.OVERCURRENT -> Vlk.Shop
        EventCategory.POWER -> Vlk.TextDim
        else -> Vlk.TextDim
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Vlk.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, Vlk.Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(colour, RoundedCornerShape(999.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(
                e.name,
                color = if (e.category == EventCategory.POWER) Vlk.TextDim else Vlk.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                e.timestamp,
                color = Vlk.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
        if (e.level > 0) {
            Pill("L${e.level}", colour, Vlk.SurfaceHi)
        }
    }
}
