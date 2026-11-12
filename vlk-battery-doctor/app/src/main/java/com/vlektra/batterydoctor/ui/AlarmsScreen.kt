package com.vlektra.batterydoctor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.vlektra.batterydoctor.ui.theme.Eyebrow
import com.vlektra.batterydoctor.ui.theme.Vlk

/**
 * Every alarm the BMS has stored, in its own window.
 *
 * The raw log is hundreds of lines and most of it is the pack going to sleep
 * and waking up. This screen answers the question a technician actually has:
 * which alarms fired, how many times, and when was the last one. The full list
 * is there underneath for anyone who wants it.
 */
@Composable
fun AlarmsScreen(state: AppState) {
    if (state.history.isEmpty()) {
        EmptyState(
            "No alarm history",
            "Connect to a battery. The log is read automatically."
        )
        return
    }

    var expanded by remember { mutableStateOf<EventCategory?>(null) }
    var showRaw by remember { mutableStateOf(false) }

    val groups = remember(state.history) {
        state.history
            .filter { it.category != EventCategory.POWER }
            .groupBy { it.category }
            .map { (cat, list) ->
                AlarmGroup(
                    category = cat,
                    label = alarmLabel(cat),
                    plain = alarmPlain(cat),
                    count = list.size,
                    latest = list.firstOrNull()?.timestamp ?: "",
                    earliest = list.lastOrNull()?.timestamp ?: "",
                    severity = alarmTint(cat),
                    events = list
                )
            }
            .sortedWith(compareByDescending<AlarmGroup> { it.severity.weight }.thenByDescending { it.count })
    }

    val powerCount = remember(state.history) {
        state.history.count { it.category == EventCategory.POWER }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))

        Column {
            Text(
                "Alarm history",
                style = MaterialTheme.typography.displaySmall,
                color = Vlk.Text
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (groups.isEmpty())
                    "$powerCount routine entries and no alarms. This pack has a clean log."
                else
                    "${groups.sumOf { it.count }} alarms across ${groups.size} kinds, " +
                        "plus $powerCount routine power entries.",
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (groups.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("WHAT HAPPENED, AND WHEN", style = Eyebrow, color = Vlk.TextFaint)
                FaultTimeline(days = remember(state.history) { buildTimeline(state.history) })
            }
        }

        if (groups.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(15.dp, Vlk.Ok, GlassTint.STRONG)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("Nothing has gone wrong", color = Vlk.Ok, fontWeight = FontWeight.Bold)
                Text(
                    "The only entries are the pack waking and sleeping, which is normal.",
                    color = Vlk.TextDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            // Severity bar: the shape of the log at a glance.
            AlarmBar(groups)

            Text("BY KIND", style = Eyebrow, color = Vlk.TextFaint)
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                groups.forEach { g ->
                    AlarmGroupCard(
                        group = g,
                        open = expanded == g.category,
                        onToggle = {
                            expanded = if (expanded == g.category) null else g.category
                        }
                    )
                }
            }
        }

        // Raw log, folded away
        Row(
            Modifier
                .fillMaxWidth()
                .glass(11.dp)
                .pressable { showRaw = !showRaw }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Full log · ${state.history.size} entries",
                color = Vlk.Text,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (showRaw) "Hide" else "Show",
                color = Vlk.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            )
        }

        AnimatedVisibility(
            visible = showRaw,
            enter = fadeIn(tween(160)) + expandVertically(tween(240)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(200))
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(11.dp)
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                state.history.forEach { e ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            e.timestamp,
                            color = Vlk.TextFaint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            modifier = Modifier.width(104.dp)
                        )
                        Text(
                            e.name,
                            color = if (e.category == EventCategory.POWER)
                                Vlk.TextFaint else Vlk.TextDim,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------

private data class AlarmGroup(
    val category: EventCategory,
    val label: String,
    val plain: String,
    val count: Int,
    val latest: String,
    val earliest: String,
    val severity: AlarmTint,
    val events: List<HistoryEvent>
)

private enum class AlarmTint(val weight: Int, val colour: Color, val back: Color, val word: String) {
    SERIOUS(3, Vlk.Engineer, Vlk.EngineerBg, "Serious"),
    REPAIR(2, Vlk.Shop, Vlk.ShopBg, "Needs repair"),
    MINOR(1, Vlk.Watch, Vlk.WatchBg, "Minor"),
    INFO(0, Vlk.TextDim, Vlk.SurfaceHi, "Information")
}

@Composable
private fun AlarmBar(groups: List<AlarmGroup>) {
    val total = groups.sumOf { it.count }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(9.dp)
        ) {
            groups.forEach { g ->
                val share = g.count.toFloat() / total
                val grown by animateFloatAsState(
                    targetValue = share,
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label = "alarmShare"
                )
                Box(
                    Modifier
                        .weight(grown.coerceAtLeast(0.02f))
                        .fillMaxWidth()
                        .height(9.dp)
                        .padding(end = 2.dp)
                        .background(g.severity.colour, RoundedCornerShape(3.dp))
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlarmTint.entries
                .filter { t -> groups.any { it.severity == t } }
                .forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(t.colour, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(t.word, color = Vlk.TextFaint, fontSize = 10.sp)
                    }
                }
        }
    }
}

@Composable
private fun AlarmGroupCard(group: AlarmGroup, open: Boolean, onToggle: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(13.dp)
            .pressable(onClick = onToggle)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(group.severity.back, RoundedCornerShape(10.dp))
                    .border(1.dp, group.severity.colour.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${group.count}",
                    color = group.severity.colour,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    group.label,
                    color = Vlk.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    group.severity.word,
                    color = group.severity.colour,
                    style = Eyebrow
                )
            }
            Text(
                if (open) "−" else "+",
                color = Vlk.TextDim,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            group.plain,
            color = Vlk.TextDim,
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text("LAST SEEN", style = Eyebrow, color = Vlk.TextFaint)
                Text(
                    group.latest,
                    color = Vlk.TextDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp
                )
            }
            if (group.count > 1) {
                Column {
                    Text("FIRST SEEN", style = Eyebrow, color = Vlk.TextFaint)
                    Text(
                        group.earliest,
                        color = Vlk.TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(150)) + expandVertically(tween(220)),
            exit = fadeOut(tween(110)) + shrinkVertically(tween(180))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Vlk.Line)
                )
                Spacer(Modifier.height(3.dp))
                group.events.take(30).forEach { e ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            e.timestamp,
                            color = Vlk.TextFaint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.width(108.dp)
                        )
                        Text(
                            e.name,
                            color = Vlk.TextDim,
                            fontSize = 10.5.sp
                        )
                    }
                }
                if (group.events.size > 30) {
                    Text(
                        "+ ${group.events.size - 30} more",
                        color = Vlk.TextFaint,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// Plain words for each alarm family
// ----------------------------------------------------------------------

private fun alarmLabel(c: EventCategory): String = when (c) {
    EventCategory.SHORT_CIRCUIT -> "Short circuit protection"
    EventCategory.VOLTAGE_SENSE -> "Voltage reading failed"
    EventCategory.TEMP_SENSE -> "Temperature sensor failed"
    EventCategory.TEMP_DIFFERENCE -> "Uneven temperature"
    EventCategory.CELL_UNDERVOLTAGE -> "Cell ran too low"
    EventCategory.CELL_OVERVOLTAGE -> "Cell went too high"
    EventCategory.OVERCURRENT -> "Too much current"
    EventCategory.CHARGE_TEMP_HIGH -> "Charged while hot"
    EventCategory.CHARGE_TEMP_LOW -> "Charged while cold"
    EventCategory.SOC_LOW -> "Battery nearly empty"
    EventCategory.POWER -> "Power on and sleep"
    EventCategory.OTHER -> "Other alarms"
}

private fun alarmPlain(c: EventCategory): String = when (c) {
    EventCategory.SHORT_CIRCUIT ->
        "The BMS cut the power because current spiked. Something in the wiring " +
            "or the controller caused it."
    EventCategory.VOLTAGE_SENSE ->
        "The BMS could not read one or more cells. This points at the thin sense " +
            "wires, not at the cells themselves."
    EventCategory.TEMP_SENSE ->
        "A temperature sensor stopped reporting. It is usually loose or broken."
    EventCategory.TEMP_DIFFERENCE ->
        "One part of the pack ran much hotter than another."
    EventCategory.CELL_UNDERVOLTAGE ->
        "The bike was run until a cell hit its lower limit. Normal once in a " +
            "while, wearing if it keeps happening."
    EventCategory.CELL_OVERVOLTAGE ->
        "A cell went above its charging limit."
    EventCategory.OVERCURRENT ->
        "The bike drew more current than the pack is rated for."
    EventCategory.CHARGE_TEMP_HIGH ->
        "Charging started while the pack was still hot. This shortens cell life."
    EventCategory.CHARGE_TEMP_LOW ->
        "Charging happened below freezing, which permanently damages LFP cells."
    EventCategory.SOC_LOW ->
        "The charge level dropped to the warning point."
    EventCategory.POWER ->
        "The pack waking up and going to sleep. Normal housekeeping."
    EventCategory.OTHER ->
        "Alarms that do not fall into the common groups."
}

private fun alarmTint(c: EventCategory): AlarmTint = when (c) {
    EventCategory.SHORT_CIRCUIT, EventCategory.VOLTAGE_SENSE -> AlarmTint.SERIOUS
    EventCategory.TEMP_SENSE, EventCategory.OVERCURRENT,
    EventCategory.CELL_OVERVOLTAGE, EventCategory.TEMP_DIFFERENCE -> AlarmTint.REPAIR
    EventCategory.CELL_UNDERVOLTAGE, EventCategory.CHARGE_TEMP_HIGH,
    EventCategory.CHARGE_TEMP_LOW, EventCategory.SOC_LOW -> AlarmTint.MINOR
    else -> AlarmTint.INFO
}
