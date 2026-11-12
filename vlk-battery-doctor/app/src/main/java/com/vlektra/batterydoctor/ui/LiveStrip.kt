package com.vlektra.batterydoctor.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.model.PackSnapshot
import com.vlektra.batterydoctor.ui.theme.Eyebrow
import com.vlektra.batterydoctor.ui.theme.Vlk
import androidx.compose.material3.Text
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The live instruments: charge, current, and how far apart the cells are.
 *
 * All three are drawn rather than written. Current animates in the direction
 * it is actually flowing, so a technician can see at a glance whether the pack
 * is charging or being drained and roughly how hard.
 */
@Composable
fun LiveStrip(
    snapshot: PackSnapshot,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val charging = snapshot.current > 0.5
    val resting = abs(snapshot.current) <= 0.5
    val currentColour = when {
        resting -> Vlk.TextDim
        charging -> Vlk.Ok
        else -> Vlk.Accent
    }

    Column(
        modifier
            .fillMaxWidth()
            .glass(16.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            SocRing(snapshot.socPercent.toFloat(), tint)

            Spacer(Modifier.width(16.dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- current, with direction ---
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "%.1f".format(abs(liveNumber(snapshot.current.toFloat()))),
                            color = currentColour,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "A",
                            color = Vlk.TextDim,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        FlowTag(resting, charging, currentColour)
                    }
                    CurrentFlow(
                        amps = snapshot.current,
                        colour = currentColour,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Vlk.Line)
        )

        SpreadStrip(snapshot)
    }
}

@Composable
private fun FlowTag(resting: Boolean, charging: Boolean, colour: Color) {
    val label = when {
        resting -> "RESTING"
        charging -> "CHARGING"
        else -> "IN USE"
    }
    Box(
        Modifier
            .background(colour.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = Eyebrow, color = colour)
    }
}

/** Charge, as an arc that fills. */
@Composable
private fun SocRing(percent: Float, tint: Color) {
    val shown = animatedNumber(percent.coerceIn(0f, 100f))
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val box = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = Vlk.SurfaceTop,
                startAngle = 130f, sweepAngle = 280f, useCenter = false,
                topLeft = topLeft, size = box,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = tint,
                startAngle = 130f, sweepAngle = 280f * (shown / 100f), useCenter = false,
                topLeft = topLeft, size = box,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${shown.roundToInt()}",
                color = Vlk.Text,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp
            )
            Text("CHARGE", style = Eyebrow, color = Vlk.TextFaint)
        }
    }
}

/**
 * Current as moving light. Dots travel out of the battery when the bike is
 * running and into it when it is charging, faster the harder it is working.
 */
@Composable
private fun CurrentFlow(amps: Double, colour: Color, modifier: Modifier = Modifier) {
    val magnitude = abs(amps)
    val resting = magnitude <= 0.5
    // A gentle floor and ceiling so 2 A still moves and 150 A does not strobe.
    val periodMs = if (resting) 3000 else (2400 - (magnitude.coerceAtMost(60.0) * 28)).toInt()

    val phase by rememberInfiniteTransition(label = "flow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs.coerceAtLeast(500), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowPhase"
    )

    Canvas(modifier) {
        val midY = size.height / 2
        val track = 2.dp.toPx()

        drawLine(
            color = Vlk.SurfaceTop,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = track,
            cap = StrokeCap.Round
        )

        if (resting) return@Canvas

        // Positive current is charging, so the light runs into the pack: right to left.
        val toLeft = amps > 0
        val dots = 7
        val spacing = size.width / dots

        repeat(dots) { i ->
            val raw = (phase + i.toFloat() / dots) % 1f
            val x = if (toLeft) size.width - raw * size.width else raw * size.width
            // Fade in at the start of the run and out at the end.
            val edge = (1f - kotlin.math.abs(raw - 0.5f) * 2f).coerceIn(0f, 1f)
            drawCircle(
                color = colour.copy(alpha = 0.25f + 0.75f * edge),
                radius = 2.6.dp.toPx(),
                center = Offset(x, midY)
            )
        }
        // A short comet tail on the leading dot.
        val lead = if (toLeft) size.width - phase * size.width else phase * size.width
        drawLine(
            color = colour.copy(alpha = 0.5f),
            start = Offset(lead, midY),
            end = Offset(if (toLeft) lead + spacing * 0.6f else lead - spacing * 0.6f, midY),
            strokeWidth = track,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Every cell as a tick on a voltage axis.
 *
 * A healthy pack is one tight cluster. A broken joint is a single tick sitting
 * on its own at the far left, which needs no explaining.
 */
@Composable
private fun SpreadStrip(snapshot: PackSnapshot) {
    val cells = snapshot.cellsMv
    if (cells.isEmpty()) return

    val lo = cells.min()
    val hi = cells.max()
    val spread = hi - lo
    val spreadColour = when {
        spread >= 100 -> Vlk.Shop
        spread >= 50 -> Vlk.Watch
        else -> Vlk.Ok
    }
    val median = medianMv(cells) ?: lo

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("CELL SPREAD", style = Eyebrow, color = Vlk.TextFaint)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${animatedNumber(spread.toFloat()).roundToInt()}",
                    color = spreadColour,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "mV",
                    color = Vlk.TextDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            val pad = 2.dp.toPx()
            val w = size.width - pad * 2
            val baseY = size.height - 9.dp.toPx()
            val range = (hi - lo).coerceAtLeast(1)

            // axis
            drawLine(
                color = Vlk.SurfaceTop,
                start = Offset(pad, baseY),
                end = Offset(pad + w, baseY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // the bulk of the pack, shaded, so an outlier reads as outside it
            val q = cells.sorted()
            val p10 = q[(q.size * 0.12f).toInt().coerceIn(0, q.size - 1)]
            val p90 = q[(q.size * 0.88f).toInt().coerceIn(0, q.size - 1)]
            val x10 = pad + w * (p10 - lo).toFloat() / range
            val x90 = pad + w * (p90 - lo).toFloat() / range
            drawRoundRect(
                color = Vlk.Ok.copy(alpha = 0.13f),
                topLeft = Offset(x10, baseY - 20.dp.toPx()),
                size = Size((x90 - x10).coerceAtLeast(3.dp.toPx()), 20.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )

            cells.forEach { mv ->
                val x = pad + w * (mv - lo).toFloat() / range
                val sag = median - mv
                val c = when {
                    sag >= 300 -> Vlk.Engineer
                    sag >= 100 -> Vlk.Shop
                    else -> Vlk.Ok
                }
                val tall = if (sag >= 100) 22.dp.toPx() else 14.dp.toPx()
                drawLine(
                    color = c.copy(alpha = if (sag >= 100) 1f else 0.75f),
                    start = Offset(x, baseY),
                    end = Offset(x, baseY - tall),
                    strokeWidth = if (sag >= 100) 3.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$lo mV",
                color = if (median - lo >= 100) Vlk.Engineer else Vlk.TextFaint,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp
            )
            Text(
                "$hi mV",
                color = Vlk.TextFaint,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp
            )
        }
    }
}
