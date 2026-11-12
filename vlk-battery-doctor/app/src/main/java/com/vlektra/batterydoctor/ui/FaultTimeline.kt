package com.vlektra.batterydoctor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vlektra.batterydoctor.model.EventCategory
import com.vlektra.batterydoctor.model.HistoryEvent
import com.vlektra.batterydoctor.ui.theme.Eyebrow
import com.vlektra.batterydoctor.ui.theme.Vlk

/**
 * What happened, and when — as a picture.
 *
 * One row per day. Each alarm is a dot, coloured by how serious it is, so the
 * shape of a bad day is visible before a single word is read. The words that
 * remain are a few: "Ran flat, then short circuit".
 */

data class DayOfFaults(
    val day: String,
    val events: List<HistoryEvent>,
    val alarms: List<HistoryEvent>
) {
    val worst: EventCategory? = alarms.maxByOrNull { severityWeight(it.category) }?.category
    val summary: String = summarise(alarms)
    val tint: Color = when (severityWeight(worst)) {
        3 -> Vlk.Engineer
        2 -> Vlk.Shop
        1 -> Vlk.Watch
        else -> Vlk.Idle
    }
}

fun buildTimeline(history: List<HistoryEvent>): List<DayOfFaults> =
    history
        .groupBy { it.timestamp.take(10) }
        .map { (day, events) ->
            DayOfFaults(
                day = day,
                events = events,
                alarms = events.filter { it.category != EventCategory.POWER }
            )
        }
        .sortedByDescending { it.day }

@Composable
fun FaultTimeline(
    days: List<DayOfFaults>,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxWidth()) {
        days.forEachIndexed { index, d ->
            DayRow(
                day = d,
                first = index == 0,
                last = index == days.lastIndex,
                open = open == d.day,
                index = index,
                onToggle = { open = if (open == d.day) null else d.day }
            )
        }
    }
}

@Composable
private fun DayRow(
    day: DayOfFaults,
    first: Boolean,
    last: Boolean,
    open: Boolean,
    index: Int,
    onToggle: () -> Unit
) {
    val quiet = day.alarms.isEmpty()

    Row(
        Modifier
            .fillMaxWidth()
            .riseIn(index)
            .pressable(onClick = onToggle)
    ) {
        // --- the spine ---
        Column(
            Modifier.width(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(1.5.dp)
                    .height(if (first) 10.dp else 14.dp)
                    .background(if (first) Color.Transparent else Vlk.Line)
            )
            Box(
                Modifier
                    .size(if (quiet) 7.dp else 11.dp)
                    .background(day.tint, RoundedCornerShape(999.dp))
            )
            Box(
                Modifier
                    .width(1.5.dp)
                    .weight(1f)
                    .background(if (last) Color.Transparent else Vlk.Line)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(
            Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    prettyDay(day.day),
                    color = if (quiet) Vlk.TextFaint else Vlk.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (!quiet) {
                    Text(
                        "${day.alarms.size}",
                        color = day.tint,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (quiet) {
                Text(
                    "Nothing wrong",
                    color = Vlk.TextFaint,
                    fontSize = 12.sp
                )
            } else {
                DotCluster(day.alarms)
                Text(
                    day.summary,
                    color = Vlk.TextDim,
                    fontSize = 12.5.sp
                )
            }

            AnimatedVisibility(
                visible = open && !quiet,
                enter = fadeIn(tween(140)) + expandVertically(tween(200)),
                exit = fadeOut(tween(100)) + shrinkVertically(tween(170))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Vlk.LineSoft, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    day.alarms.take(24).forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .background(
                                        categoryColour(e.category),
                                        RoundedCornerShape(999.dp)
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                e.timestamp.takeLast(8),
                                color = Vlk.TextFaint,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                shortName(e.category),
                                color = Vlk.TextDim,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (day.alarms.size > 24) {
                        Text(
                            "+ ${day.alarms.size - 24} more",
                            color = Vlk.TextFaint,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/** One dot per alarm, so a bad day looks like a bad day. */
@Composable
private fun DotCluster(alarms: List<HistoryEvent>) {
    val shown = alarms.take(22)
    val extra = alarms.size - shown.size

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        shown
            .sortedByDescending { severityWeight(it.category) }
            .forEach { e ->
                Box(
                    Modifier
                        .size(7.dp)
                        .background(categoryColour(e.category), RoundedCornerShape(999.dp))
                )
            }
        if (extra > 0) {
            Spacer(Modifier.width(2.dp))
            Text(
                "+$extra",
                color = Vlk.TextFaint,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp
            )
        }
    }
}

// ----------------------------------------------------------------------

private fun severityWeight(c: EventCategory?): Int = when (c) {
    EventCategory.SHORT_CIRCUIT, EventCategory.VOLTAGE_SENSE -> 3
    EventCategory.TEMP_SENSE, EventCategory.OVERCURRENT,
    EventCategory.CELL_OVERVOLTAGE, EventCategory.TEMP_DIFFERENCE -> 2
    EventCategory.CELL_UNDERVOLTAGE, EventCategory.CHARGE_TEMP_HIGH,
    EventCategory.CHARGE_TEMP_LOW, EventCategory.SOC_LOW -> 1
    else -> 0
}

fun categoryColour(c: EventCategory): Color = when (severityWeight(c)) {
    3 -> Vlk.Engineer
    2 -> Vlk.Shop
    1 -> Vlk.Watch
    else -> Vlk.Idle
}

/** Two or three words. Nothing longer belongs on a timeline. */
fun shortName(c: EventCategory): String = when (c) {
    EventCategory.SHORT_CIRCUIT -> "Short circuit"
    EventCategory.VOLTAGE_SENSE -> "Cell not read"
    EventCategory.TEMP_SENSE -> "Sensor failed"
    EventCategory.TEMP_DIFFERENCE -> "Uneven heat"
    EventCategory.CELL_UNDERVOLTAGE -> "Ran flat"
    EventCategory.CELL_OVERVOLTAGE -> "Overcharged"
    EventCategory.OVERCURRENT -> "Too much current"
    EventCategory.CHARGE_TEMP_HIGH -> "Charged hot"
    EventCategory.CHARGE_TEMP_LOW -> "Charged cold"
    EventCategory.SOC_LOW -> "Nearly empty"
    EventCategory.POWER -> "Normal use"
    EventCategory.OTHER -> "Other alarm"
}

/** The day in a handful of words, worst thing first. */
private fun summarise(alarms: List<HistoryEvent>): String {
    if (alarms.isEmpty()) return "Nothing wrong"
    val kinds = alarms
        .map { it.category }
        .distinct()
        .sortedByDescending { severityWeight(it) }
        .take(3)
        .map { shortName(it) }
    return kinds.joinToString(", ")
}

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** 2026-11-03 becomes 3 Nov. */
fun prettyDay(iso: String): String {
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val month = parts[1].toIntOrNull() ?: return iso
    val day = parts[2].toIntOrNull() ?: return iso
    return "$day ${MONTHS.getOrElse(month - 1) { parts[1] }}"
}
