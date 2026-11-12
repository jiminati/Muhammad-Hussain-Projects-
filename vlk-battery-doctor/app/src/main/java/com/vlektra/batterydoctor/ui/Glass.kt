package com.vlektra.batterydoctor.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vlektra.batterydoctor.ui.theme.Vlk
import kotlin.math.cos
import kotlin.math.sin

/**
 * The glass surface system.
 *
 * Panels are translucent sheets with the light falling off down their height,
 * floating over a slow field of colour. The colour of that field is the
 * verdict: a healthy pack bathes the screen in green, a broken one in red. So
 * the severity is readable from across a workshop, before a word is read.
 *
 * No backdrop blur is used. Android only offers it from API 31, and a soft
 * gradient field needs no blurring to look soft — which keeps the whole look
 * identical on every phone a technician might carry.
 */

/** How strongly a panel is tinted by its status colour. */
enum class GlassTint { NONE, SUBTLE, STRONG }

/**
 * A frosted panel: a translucent fill that fades down the height, with a
 * bright top edge that fades out the same way, as light would.
 */
fun Modifier.glass(
    radius: Dp = 18.dp,
    tint: Color? = null,
    strength: GlassTint = GlassTint.SUBTLE
): Modifier = composed {
    val shape = RoundedCornerShape(radius)

    val topFill: Color
    val bottomFill: Color
    if (tint == null || strength == GlassTint.NONE) {
        topFill = Color.White.copy(alpha = 0.085f)
        bottomFill = Color.White.copy(alpha = 0.022f)
    } else if (strength == GlassTint.SUBTLE) {
        topFill = tint.copy(alpha = 0.13f)
        bottomFill = tint.copy(alpha = 0.028f)
    } else {
        topFill = tint.copy(alpha = 0.26f)
        bottomFill = tint.copy(alpha = 0.05f)
    }

    val edgeTop = (tint ?: Color.White).copy(
        alpha = if (strength == GlassTint.STRONG) 0.55f else 0.26f
    )
    val edgeBottom = (tint ?: Color.White).copy(alpha = 0.035f)

    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(topFill, bottomFill)))
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(listOf(edgeTop, edgeBottom)),
            shape = shape
        )
}

/** A glass panel that breathes, for anything that needs attention. */
fun Modifier.glassPulse(
    radius: Dp = 18.dp,
    tint: Color,
    active: Boolean = true,
    periodMs: Int = 1500
): Modifier = composed {
    val shape = RoundedCornerShape(radius)

    val pulse by rememberInfiniteTransition(label = "glassPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glassPulseValue"
    )
    val p = if (active) pulse else 0f

    this
        .shadow(
            elevation = (4 + p * 16).dp,
            shape = shape,
            ambientColor = tint,
            spotColor = tint
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = 0.16f + p * 0.12f),
                    tint.copy(alpha = 0.03f)
                )
            )
        )
        .border(
            width = 1.4.dp,
            brush = Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = 0.5f + p * 0.45f),
                    tint.copy(alpha = 0.06f)
                )
            ),
            shape = shape
        )
}

/** A slow highlight that travels across a panel, the way light crosses glass. */
fun Modifier.sheen(periodMs: Int = 5200): Modifier = composed {
    val phase by rememberInfiniteTransition(label = "sheen").animateFloat(
        initialValue = -0.45f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheenPhase"
    )

    drawWithContent {
        drawContent()
        val w = size.width
        val band = w * 0.42f
        val x = phase * (w + band) - band
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.055f),
                    Color.Transparent
                ),
                startX = x,
                endX = x + band
            )
        )
    }
}

/** A ready-made glass card, since most panels want the same padding and spacing. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    tint: Color? = null,
    strength: GlassTint = GlassTint.NONE,
    padding: Dp = 14.dp,
    gap: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glass(radius, tint, strength)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content
    )
}

/**
 * The field of colour everything floats on.
 *
 * Three soft pools drift on slow, mismatched orbits so the screen is never
 * quite still, and [severity] decides their hue. Alpha is kept low: this is
 * atmosphere, and text has to stay readable over it.
 */
@Composable
fun AmbientBackdrop(
    severity: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    val drift by rememberInfiniteTransition(label = "ambient").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(34_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientDrift"
    )

    // Settle into a new verdict colour rather than snapping to it.
    val r by animateFloatAsState(severity.red, tween(900), label = "ambR")
    val g by animateFloatAsState(severity.green, tween(900), label = "ambG")
    val b by animateFloatAsState(severity.blue, tween(900), label = "ambB")
    val hue = Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        fun pool(cx: Float, cy: Float, rad: Float, colour: Color, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colour.copy(alpha = alpha * intensity), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = rad
                ),
                radius = rad,
                center = Offset(cx, cy)
            )
        }

        // The verdict's own pool, high and to one side.
        pool(
            cx = w * (0.26f + 0.10f * cos(drift)),
            cy = h * (0.16f + 0.05f * sin(drift * 0.8f)),
            rad = w * 0.95f,
            colour = hue,
            alpha = 0.20f
        )
        // Brand light, low and opposite, on a slower orbit.
        pool(
            cx = w * (0.84f + 0.09f * cos(drift * 0.62f + 2.1f)),
            cy = h * (0.70f + 0.07f * sin(drift * 0.55f)),
            rad = w * 0.80f,
            colour = Vlk.Accent,
            alpha = 0.13f
        )
        // A cool pool at the foot to stop the bottom going flat black.
        pool(
            cx = w * (0.44f + 0.07f * sin(drift * 0.4f)),
            cy = h * 1.02f,
            rad = w * 0.72f,
            colour = Vlk.SurfaceTop,
            alpha = 0.55f
        )
    }
}
