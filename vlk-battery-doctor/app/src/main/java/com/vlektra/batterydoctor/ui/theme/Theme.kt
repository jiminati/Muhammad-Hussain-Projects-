package com.vlektra.batterydoctor.ui.theme

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
 * VLK brand palette and type.
 *
 * CHANGE THE FOUR BRAND VALUES to Vlektra's official colours and the whole app
 * follows — nothing else hard-codes a brand colour.
 *
 * The status colours are deliberately separate from the brand accent. In this
 * app colour carries meaning: the cell map, the verdict and the alarm list all
 * read from the same four. They differ in lightness as well as hue, and every
 * place they appear also carries a number or a word, so the screen still works
 * for someone who cannot tell them apart.
 */
object Vlk {
    // --- brand ---
    val Accent = Color(0xFF3D7BFF)
    val AccentSoft = Color(0xFF1E3A6E)
    val AccentGlow = Color(0x333D7BFF)

    // --- ground ---
    val Ink = Color(0xFF05070B)
    val Surface = Color(0xFF0D121B)
    val SurfaceHi = Color(0xFF151D29)
    val SurfaceTop = Color(0xFF1D2736)
    val Line = Color(0xFF202A39)
    val LineSoft = Color(0xFF161F2B)

    // --- text ---
    val Text = Color(0xFFEDF2F9)
    val TextDim = Color(0xFF8795AA)
    val TextFaint = Color(0xFF5A6678)

    // --- status ---
    val Ok = Color(0xFF34D399)
    val OkBg = Color(0xFF0C2A20)
    val Watch = Color(0xFFFBBF24)
    val WatchBg = Color(0xFF2E2208)
    val Shop = Color(0xFFF97316)
    val ShopBg = Color(0xFF301803)
    val Engineer = Color(0xFFF43F5E)
    val EngineerBg = Color(0xFF330a16)

    val Idle = Color(0xFF3A4555)
}

private val Scheme = darkColorScheme(
    primary = Vlk.Accent,
    onPrimary = Color.White,
    primaryContainer = Vlk.AccentSoft,
    onPrimaryContainer = Vlk.Text,
    secondary = Vlk.Ok,
    background = Vlk.Ink,
    onBackground = Vlk.Text,
    surface = Vlk.Surface,
    onSurface = Vlk.Text,
    surfaceVariant = Vlk.SurfaceHi,
    onSurfaceVariant = Vlk.TextDim,
    outline = Vlk.Line,
    outlineVariant = Vlk.LineSoft,
    error = Vlk.Engineer,
    onError = Color.White
)

private val Sans = FontFamily.SansSerif
private val Mono = FontFamily.Monospace

private val VlkTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Black,
        fontSize = 40.sp, letterSpacing = (-1.4).sp, lineHeight = 44.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Black,
        fontSize = 32.sp, letterSpacing = (-1.0).sp, lineHeight = 36.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.ExtraBold,
        fontSize = 25.sp, letterSpacing = (-0.7).sp, lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 19.sp, letterSpacing = (-0.4).sp, lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp, lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Medium,
        fontSize = 9.5.sp, letterSpacing = 1.5.sp
    )
)

/** Digits that line up in a column. */
val Numeral = TextStyle(
    fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 14.sp
)

/** The small uppercase monospace caption used for eyebrows. */
val Eyebrow = TextStyle(
    fontFamily = Mono, fontWeight = FontWeight.Medium,
    fontSize = 9.5.sp, letterSpacing = 1.6.sp
)

@Composable
fun VlkTheme(content: @Composable () -> Unit) {
    // Deliberately single-theme. Technicians work in bright sun and in dim
    // workshops, and one high-contrast dark scheme reads in both.
    MaterialTheme(
        colorScheme = Scheme,
        typography = VlkTypography,
        content = content
    )
}
