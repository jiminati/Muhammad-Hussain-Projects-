package com.vlektra.batterydoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.diagnosis.Severity
import com.vlektra.batterydoctor.ui.theme.Vlk

/** Foreground and background colour pair for a severity. */
fun severityColours(s: Severity): Pair<Color, Color> = when (s) {
    Severity.OK -> Vlk.Ok to Vlk.OkBg
    Severity.WATCH -> Vlk.Watch to Vlk.WatchBg
    Severity.WORKSHOP -> Vlk.Shop to Vlk.ShopBg
    Severity.ENGINEER -> Vlk.Engineer to Vlk.EngineerBg
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Vlk.TextDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.4.sp,
        modifier = modifier
    )
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Vlk.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Vlk.Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun Pill(text: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .border(1.dp, fg.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
    }
}

@Composable
fun KeyValue(label: String, value: String, mono: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Vlk.TextDim,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = Vlk.Text,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun Stat(label: String, value: String, accent: Color = Vlk.Text, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Vlk.SurfaceHi, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Vlk.TextDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            letterSpacing = 0.9.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = accent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
