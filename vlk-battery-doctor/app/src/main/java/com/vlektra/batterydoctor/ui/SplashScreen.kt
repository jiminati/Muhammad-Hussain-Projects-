package com.vlektra.batterydoctor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.ui.theme.Eyebrow
import com.vlektra.batterydoctor.ui.theme.Vlk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The launch sequence.
 *
 * VLEKTRA arrives first and holds the screen alone, because it is the company
 * mark. It then shrinks and lifts to make room, Battery Doctor takes the stage
 * beneath it, and the credit settles at the foot of the screen — present, not
 * shouting.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {

    val markScale = remember { Animatable(1.22f) }
    val markAlpha = remember { Animatable(0f) }
    val markLift = remember { Animatable(0f) }
    val boltDraw = remember { Animatable(0f) }

    val titleAlpha = remember { Animatable(0f) }
    val titleRise = remember { Animatable(26f) }
    val ruleWidth = remember { Animatable(0f) }

    val creditAlpha = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. the mark arrives
        launch { markAlpha.animateTo(1f, tween(620, easing = FastOutSlowInEasing)) }
        launch { markScale.animateTo(1f, tween(760, easing = FastOutSlowInEasing)) }
        launch { glow.animateTo(1f, tween(900, easing = LinearEasing)) }
        launch {
            delay(240)
            boltDraw.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        }

        delay(1120)

        // 2. it shrinks and lifts out of the way
        launch { markScale.animateTo(0.44f, tween(620, easing = FastOutSlowInEasing)) }
        launch { markLift.animateTo(-54f, tween(620, easing = FastOutSlowInEasing)) }
        launch { glow.animateTo(0.35f, tween(620)) }

        delay(330)

        // 3. the product name takes the stage
        launch { titleAlpha.animateTo(1f, tween(560, easing = FastOutSlowInEasing)) }
        launch { titleRise.animateTo(0f, tween(620, easing = FastOutSlowInEasing)) }
        launch {
            delay(200)
            ruleWidth.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        }

        delay(430)

        // 4. the credit settles
        creditAlpha.animateTo(1f, tween(640))

        delay(900)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Vlk.Ink),
        contentAlignment = Alignment.Center
    ) {
        // A soft pool of brand light behind the mark, so the black is not flat.
        Box(
            Modifier
                .fillMaxSize()
                .alpha(glow.value)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Vlk.AccentGlow, Color.Transparent),
                        radius = 760f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ---- VLEKTRA ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .offset(y = markLift.value.dp)
                    .scale(markScale.value)
                    .alpha(markAlpha.value)
            ) {
                Bolt(
                    progress = boltDraw.value,
                    modifier = Modifier.size(width = 26.dp, height = 40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "VLEKTRA",
                    color = Vlk.Text,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp,
                    letterSpacing = 3.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // ---- hairline that draws itself ----
            Box(
                Modifier
                    .width((188 * ruleWidth.value).dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Vlk.Accent, Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(16.dp))

            // ---- BATTERY DOCTOR ----
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = titleRise.value.dp)
                    .alpha(titleAlpha.value)
            ) {
                Text(
                    text = "BATTERY",
                    color = Vlk.Text,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 31.sp,
                    letterSpacing = 6.sp
                )
                Text(
                    text = "DOCTOR",
                    color = Vlk.Accent,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 31.sp,
                    letterSpacing = 6.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "PACK DIAGNOSIS FOR THE FIELD",
                    style = Eyebrow,
                    color = Vlk.TextFaint
                )
            }
        }

        // ---- credit, at the foot ----
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 46.dp)
                .alpha(creditAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(22.dp)
                    .height(1.dp)
                    .background(Vlk.Line)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "developed by",
                style = MaterialTheme.typography.bodySmall,
                color = Vlk.TextFaint
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "S E N",
                color = Vlk.TextDim,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 4.sp
            )
        }
    }
}

/**
 * The bolt beside the wordmark, drawn stroke-first so it writes itself in.
 * Generic enough to swap for the real Vlektra mark when the asset arrives.
 */
@Composable
private fun Bolt(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.62f, 0f)
            lineTo(w * 0.08f, h * 0.55f)
            lineTo(w * 0.44f, h * 0.55f)
            lineTo(w * 0.34f, h)
            lineTo(w * 0.94f, h * 0.41f)
            lineTo(w * 0.56f, h * 0.41f)
            close()
        }

        // Fill rises with the draw so the glyph fades in as it is traced.
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(Vlk.Accent, Vlk.Accent.copy(alpha = 0.55f))
            ),
            alpha = (progress * progress).coerceIn(0f, 1f)
        )
        drawPath(
            path = path,
            color = Vlk.Accent,
            style = Stroke(width = 2.2f, cap = StrokeCap.Round),
            alpha = progress.coerceIn(0f, 1f)
        )
    }
}
