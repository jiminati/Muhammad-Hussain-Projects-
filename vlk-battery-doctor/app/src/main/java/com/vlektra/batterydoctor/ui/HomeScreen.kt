package com.vlektra.batterydoctor.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.Screen
import com.vlektra.batterydoctor.diagnosis.Finding
import com.vlektra.batterydoctor.diagnosis.Severity
import com.vlektra.batterydoctor.model.PackLayouts
import com.vlektra.batterydoctor.ui.theme.Eyebrow
import com.vlektra.batterydoctor.ui.theme.Vlk

/**
 * Base mode. What a technician needs and nothing more: which battery this is,
 * whether it is healthy, where the trouble is, and what to do about it.
 *
 * Numbers live behind the cell map (tap a cell) and in engineering mode.
 */
@Composable
fun HomeScreen(state: AppState) {
    val snapshot = state.snapshot
    val diagnosis = state.diagnosis

    if (snapshot == null || diagnosis == null) {
        EmptyState("No battery connected", "Go to Connect and pick a pack.")
        return
    }

    val layout = remember(snapshot.batteryCode, state.connected?.knownBatch) {
        PackLayouts.forPack(
            batteryCode = snapshot.batteryCode,
            batchHint = state.connected?.knownBatch,
            ratedAh = snapshot.ratedAh
        )
    }
    val cells = remember(snapshot.cellsMv) { cellStatesFrom(snapshot.cellsMv) }
    val median = remember(snapshot.cellsMv) { medianMv(snapshot.cellsMv) }
    var selected by remember { mutableStateOf<CellState?>(null) }

    val (fg, bg) = severityColours(diagnosis.worst)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(6.dp))

        // ---------- the verdict, before anything else ----------
        VerdictHero(
            severity = diagnosis.worst,
            headline = diagnosis.headline,
            modifier = Modifier.riseIn(0)
        )

        // ---------- which battery is this ----------
        Row(
            Modifier
                .fillMaxWidth()
                .riseIn(1)
                .background(Vlk.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, Vlk.Line, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("CONNECTED BATTERY", style = Eyebrow, color = Vlk.TextFaint)
                Spacer(Modifier.height(5.dp))
                Text(
                    state.job.batteryId.ifBlank { snapshot.deviceName },
                    color = Vlk.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    buildString {
                        append(layout.title)
                        state.connected?.knownBatch?.let { append(" · $it") }
                    },
                    color = Vlk.TextDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ---------- live instruments ----------
        LiveStrip(
            snapshot = snapshot,
            tint = fg,
            modifier = Modifier.riseIn(2)
        )

        // ---------- the pack, as it physically is ----------
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("CELL LAYOUT", style = Eyebrow, color = Vlk.TextFaint)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        layout.subtitle,
                        color = Vlk.TextDim,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "tap a cell",
                    color = Vlk.TextFaint,
                    fontSize = 10.sp
                )
            }

            CellMap(
                layout = layout,
                cells = cells,
                onGroupTap = { selected = if (selected?.group == it.group) null else it }
            )

            CellDetailStrip(selected = selected, packMedianMv = median)
            CellMapLegend()

            if (layout.bridges.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Vlk.SurfaceHi, RoundedCornerShape(10.dp))
                        .padding(11.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .size(5.dp)
                            .background(Vlk.Accent, RoundedCornerShape(999.dp))
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Blue dots are the two module joints, at cells 7 and 18. " +
                            "They carry the whole current and take the vibration, " +
                            "so they break first.",
                        color = Vlk.TextDim,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // ---------- problem ----------
        val problems = diagnosis.actionable
        SectionHeading("The problem")
        if (problems.isEmpty()) {
            PlainCard(
                tint = Vlk.Ok,
                title = "Nothing wrong found",
                body = "The cells, the fault log and the settings all check out."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                problems.take(3).forEach { ProblemCard(it) }
                if (problems.size > 3) {
                    Text(
                        "+ ${problems.size - 3} more in the full report",
                        color = Vlk.TextFaint,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // ---------- solution ----------
        SectionHeading("What to do")
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            val steps = problems.ifEmpty { diagnosis.findings }
            steps.take(4).forEachIndexed { i, f -> SolutionStep(i + 1, f) }
        }

        Spacer(Modifier.height(2.dp))

        Button(
            onClick = { state.go(Screen.REPORT) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Vlk.Accent,
                contentColor = Color.White
            )
        ) {
            Text("Make the report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ----------------------------------------------------------------------
// Pieces
// ----------------------------------------------------------------------

@Composable
private fun VerdictHero(
    severity: Severity,
    headline: String,
    modifier: Modifier = Modifier
) {
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "verdictReveal"
    )
    val (fg, bg) = severityColours(severity)

    Column(
        modifier
            .fillMaxWidth()
            .alpha(reveal)
            .offset(y = ((1f - reveal) * 14).dp)
            .background(
                Brush.verticalGradient(listOf(bg, Vlk.Surface)),
                RoundedCornerShape(18.dp)
            )
            .border(1.4.dp, fg.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(fg, RoundedCornerShape(999.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(severity.badge, style = Eyebrow, color = fg)
        }

        Text(
            severity.verdict,
            style = MaterialTheme.typography.displaySmall,
            color = Vlk.Text
        )
        Text(
            severity.verdictUrdu,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(1.dp))
        Text(
            headline,
            color = Vlk.TextDim,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SectionHeading(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            color = Vlk.Text,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Vlk.Line)
        )
    }
}

@Composable
private fun ProblemCard(f: Finding) {
    val (fg, bg) = severityColours(f.severity)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Vlk.Surface, RoundedCornerShape(13.dp))
            .border(1.dp, Vlk.Line, RoundedCornerShape(13.dp))
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(9.dp)
                .background(bg, RoundedCornerShape(3.dp))
                .border(1.5.dp, fg, RoundedCornerShape(3.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                f.brief,
                color = Vlk.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                f.title,
                color = Vlk.TextFaint,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SolutionStep(number: Int, f: Finding) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Vlk.Surface, RoundedCornerShape(13.dp))
            .border(1.dp, Vlk.Line, RoundedCornerShape(13.dp))
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(26.dp)
                .background(Vlk.AccentSoft, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                color = Vlk.Text,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                f.briefAction,
                color = Vlk.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                f.actionUrdu,
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PlainCard(tint: Color, title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Vlk.Surface, RoundedCornerShape(13.dp))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(13.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, color = tint, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
        Text(body, color = Vlk.TextDim, style = MaterialTheme.typography.bodySmall)
    }
}
