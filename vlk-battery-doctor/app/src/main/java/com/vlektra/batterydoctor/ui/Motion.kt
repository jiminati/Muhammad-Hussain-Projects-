package com.vlektra.batterydoctor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * The app's motion language, in one place.
 *
 * Three rules: a tap always moves something, numbers count rather than appear,
 * and content arrives from slightly below. Everything is short — under 600 ms —
 * because a technician taps through this while holding a battery.
 */

/**
 * Makes anything tappable press in under the finger and spring back.
 * Use instead of a bare [clickable] on cards, chips and tiles.
 */
fun Modifier.pressable(
    pressedScale: Float = 0.965f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactions = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactions) {
        interactions.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> pressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactions,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Content that lifts into place. [index] staggers a list so rows arrive in
 * sequence rather than all at once.
 */
fun Modifier.riseIn(
    index: Int = 0,
    distance: Dp = 12.dp,
    perItemDelayMs: Int = 45
): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * perItemDelayMs).toLong())
        shown = true
    }
    val t by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "riseIn"
    )
    val px = with(LocalDensity.current) { distance.toPx() }

    this
        .alpha(t)
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, ((1f - t) * px).roundToInt())
            }
        }
}

/** A number that counts up to its value instead of snapping to it. */
@Composable
fun animatedNumber(target: Float, durationMs: Int = 760): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(target, tween(durationMs, easing = FastOutSlowInEasing))
    }
    return anim.value
}

/** Same, for a value that should settle quickly when it changes live. */
@Composable
fun liveNumber(target: Float): Float {
    val v by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "liveNumber"
    )
    return v
}

/** Fades and slides the body when the screen changes, so navigation has weight. */
@Composable
fun screenEnterProgress(key: Any?): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(key) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }
    return anim.value
}
