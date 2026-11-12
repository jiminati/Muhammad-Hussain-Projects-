package com.vlektra.batterydoctor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.model.PackLayout
import com.vlektra.batterydoctor.ui.theme.Eyebrow
import com.vlektra.batterydoctor.ui.theme.Vlk
import kotlin.math.roundToInt

/** How one cell group is doing, for colouring the map. */
enum class CellHealth { GOOD, HIGH, LOW, WEAK, BROKEN, UNKNOWN }

data class CellState(
    val group: Int,
    val millivolts: Int?,
    val health: CellHealth
)

/**
 * The physical picture of the pack.
 *
 * Every group is drawn where it actually sits, including the 3 parallel cells
 * per group on a 36 Ah pack, so a technician reading the screen is looking at
 * the same thing they are holding. The two module bridges are drawn as real
 * joints, because that is where Batch 6 packs fail.
 */
@Composable
fun CellMap(
    layout: PackLayout,
    cells: List<CellState>,
    modifier: Modifier = Modifier,
    showNumbers: Boolean = true,
    onGroupTap: ((CellState) -> Unit)? = null
) {
    val byGroup = remember(cells) { cells.associateBy { it.group } }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        layout.modules.forEachIndexed { moduleIndex, rowRange ->

            if (moduleIndex > 0) {
                BridgeBand(layout, moduleIndex)
            }

            // Module name, only when the pack really has more than one.
            if (layout.modules.size > 1) {
                Text(
                    text = layout.moduleNames.getOrElse(moduleIndex) { "Module" }.uppercase(),
                    style = Eyebrow,
                    color = Vlk.TextFaint,
                    modifier = Modifier.padding(bottom = 6.dp, top = if (moduleIndex > 0) 6.dp else 0.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(14.dp)
                    .padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowRange.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0 until layout.columns).forEach { col ->
                            val group = layout.groupAt(col, row)
                            val state = group?.let { byGroup[it] }
                            CellBlock(
                                state = state,
                                group = group,
                                showNumber = showNumbers,
                                isBridgeEnd = group != null && group in layout.bridgeGroups,
                                modifier = Modifier.weight(1f),
                                onTap = if (state != null && onGroupTap != null) {
                                    { onGroupTap(state) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellBlock(
    state: CellState?,
    group: Int?,
    showNumber: Boolean,
    isBridgeEnd: Boolean,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)?
) {
    val health = state?.health ?: CellHealth.UNKNOWN
    val (fg, bg) = cellColours(health)

    Box(
        modifier = modifier
            .aspectRatio(1.25f)
            .then(
                when (health) {
                    CellHealth.BROKEN -> Modifier.glassPulse(7.dp, fg, periodMs = 1100)
                    CellHealth.WEAK -> Modifier.glass(7.dp, fg, GlassTint.STRONG)
                    CellHealth.LOW, CellHealth.HIGH -> Modifier.glass(7.dp, fg, GlassTint.SUBTLE)
                    else -> Modifier.glass(7.dp)
                }
            )
            .then(if (onTap != null) Modifier.pressable(pressedScale = 0.88f, onClick = onTap) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (showNumber && group != null) {
            Text(
                text = "$group",
                color = if (health == CellHealth.GOOD) Vlk.TextDim else fg,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        // A notch in the corner marks a bridge end, so the joint is findable
        // without relying on colour.
        if (isBridgeEnd) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(3.dp)
                    .background(Vlk.Accent, RoundedCornerShape(999.dp))
            )
        }
    }
}

/** The gap between two modules, with the busbars that cross it. */
@Composable
private fun BridgeBand(layout: PackLayout, moduleIndex: Int) {
    if (layout.bridges.isEmpty()) {
        Spacer(Modifier.height(8.dp))
        return
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0 until layout.columns).forEach { col ->
            val bridge = layout.bridges.firstOrNull { col in it.cols }
            Box(
                Modifier
                    .weight(1f)
                    .height(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bridge != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (bridge.downward) "▼" else "▲",
                            color = Vlk.Accent,
                            fontSize = 9.sp
                        )
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(7.dp)
                                .background(Vlk.Accent.copy(alpha = 0.55f))
                        )
                    }
                }
            }
        }
    }
}

fun cellColours(h: CellHealth): Pair<Color, Color> = when (h) {
    CellHealth.GOOD -> Vlk.Ok to Vlk.Surface
    CellHealth.HIGH -> Vlk.Ok to Vlk.OkBg
    CellHealth.LOW -> Vlk.Watch to Vlk.WatchBg
    CellHealth.WEAK -> Vlk.Shop to Vlk.ShopBg
    CellHealth.BROKEN -> Vlk.Engineer to Vlk.EngineerBg
    CellHealth.UNKNOWN -> Vlk.Idle to Vlk.Surface
}

/** The key under the map, in words a technician uses. */
@Composable
fun CellMapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            CellHealth.GOOD to "Normal",
            CellHealth.LOW to "A bit low",
            CellHealth.WEAK to "Weak",
            CellHealth.BROKEN to "Broken"
        ).forEach { (h, label) ->
            val (fg, bg) = cellColours(h)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(bg, RoundedCornerShape(3.dp))
                        .border(1.dp, fg.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(5.dp))
                Text(label, color = Vlk.TextFaint, fontSize = 10.sp)
            }
        }
    }
}

/** A tap on a cell opens this, and nothing else changes on the screen. */
@Composable
fun CellDetailStrip(selected: CellState?, packMedianMv: Int?) {
    AnimatedVisibility(
        visible = selected != null,
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
    ) {
        val s = selected ?: return@AnimatedVisibility
        val (fg, bg) = cellColours(s.health)
        val delta = if (s.millivolts != null && packMedianMv != null) {
            s.millivolts - packMedianMv
        } else null

        Row(
            Modifier
                .fillMaxWidth()
                .glass(13.dp, fg, GlassTint.STRONG)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Cell ${s.group}",
                    color = Vlk.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    healthWord(s.health),
                    color = fg,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    s.millivolts?.let { "${it / 1000.0}".take(5) + " V" } ?: "—",
                    color = Vlk.Text,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (delta != null) {
                    Text(
                        if (delta >= 0) "+$delta mV vs pack" else "$delta mV vs pack",
                        color = Vlk.TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

fun healthWord(h: CellHealth): String = when (h) {
    CellHealth.GOOD -> "Normal"
    CellHealth.HIGH -> "Highest in the pack"
    CellHealth.LOW -> "A little low"
    CellHealth.WEAK -> "Weak — needs balancing"
    CellHealth.BROKEN -> "Broken connection likely"
    CellHealth.UNKNOWN -> "Not read"
}

/** Work out each group's health from the readings, with the same thresholds the rules use. */
fun cellStatesFrom(cellsMv: List<Int>): List<CellState> {
    if (cellsMv.isEmpty()) return emptyList()
    val sorted = cellsMv.sorted()
    val mid = sorted.size / 2
    val median = if (sorted.size % 2 == 0) {
        (sorted[mid - 1] + sorted[mid]) / 2.0
    } else {
        sorted[mid].toDouble()
    }
    val lo = sorted.first()
    val hi = sorted.last()
    val spread = hi - lo

    return cellsMv.mapIndexed { i, mv ->
        val sag = median - mv
        val health = when {
            sag >= 300 -> CellHealth.BROKEN
            sag >= 100 -> CellHealth.WEAK
            mv == lo && spread >= 30 -> CellHealth.LOW
            mv == hi && spread >= 30 -> CellHealth.HIGH
            else -> CellHealth.GOOD
        }
        CellState(group = i + 1, millivolts = mv, health = health)
    }
}

fun medianMv(cellsMv: List<Int>): Int? {
    if (cellsMv.isEmpty()) return null
    val s = cellsMv.sorted()
    val m = s.size / 2
    return if (s.size % 2 == 0) ((s[m - 1] + s[m]) / 2.0).roundToInt() else s[m]
}
