package com.vlektra.batterydoctor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * VLK brand palette.
 *
 * CHANGE THESE FOUR VALUES to match Vlektra's official brand colours and the
 * whole app follows. Nothing else in the codebase hard-codes a brand colour.
 */
object Vlk {
    val Ink = Color(0xFF070A0F)        // page background
    val Surface = Color(0xFF111722)    // cards
    val SurfaceHi = Color(0xFF1A2231)  // raised chips, inputs
    val Line = Color(0xFF26303F)       // hairlines
    val Accent = Color(0xFF2E7BFF)     // VLK action blue
    val AccentDim = Color(0xFF1B4FA8)

    val Text = Color(0xFFEAF0F8)
    val TextDim = Color(0xFF8E9BAF)

    // Verdict colours. Lightness differs as well as hue so they are
    // distinguishable without relying on colour alone.
    val Ok = Color(0xFF3ED48B)
    val OkBg = Color(0xFF0E2A1D)
    val Watch = Color(0xFFF0B13C)
    val WatchBg = Color(0xFF2E2310)
    val Shop = Color(0xFFFF8352)
    val ShopBg = Color(0xFF301A11)
    val Engineer = Color(0xFFFF5F6D)
    val EngineerBg = Color(0xFF320F15)
}

private val VlkDarkScheme = darkColorScheme(
    primary = Vlk.Accent,
    onPrimary = Color.White,
    primaryContainer = Vlk.AccentDim,
    onPrimaryContainer = Color.White,
    background = Vlk.Ink,
    onBackground = Vlk.Text,
    surface = Vlk.Surface,
    onSurface = Vlk.Text,
    surfaceVariant = Vlk.SurfaceHi,
    onSurfaceVariant = Vlk.TextDim,
    outline = Vlk.Line,
    error = Vlk.Engineer,
    onError = Color.White
)

private val VlkTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp
    )
)

/** Monospace style for anything with digits that must line up. */
val MonoNumber = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp
)

@Composable
fun VlkTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // The app is deliberately single-theme: technicians work in bright
    // sunlight and in dim workshops, and one high-contrast dark scheme
    // reads reliably in both.
    MaterialTheme(
        colorScheme = VlkDarkScheme,
        typography = VlkTypography,
        content = content
    )
}
