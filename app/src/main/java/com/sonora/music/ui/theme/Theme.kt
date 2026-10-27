package com.sonora.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object SonoraColors {
    val Green = Color(0xFF1ED760)
    val GreenDark = Color(0xFF169C46)
    val Background = Color(0xFF0B0B0F)
    val Surface = Color(0xFF151519)
    val SurfaceHigh = Color(0xFF222228)
    val SurfaceHighest = Color(0xFF2E2E36)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB3B3B8)
    val TextTertiary = Color(0xFF7A7A82)

    /** Accent colors used for tiles and generated covers. */
    val Accents = listOf(
        Color(0xFFE13300), Color(0xFF1E3264), Color(0xFF8D67AB), Color(0xFFE8115B),
        Color(0xFF148A08), Color(0xFFBA5D07), Color(0xFF509BF5), Color(0xFF27856A),
        Color(0xFFAF2896), Color(0xFF503750), Color(0xFFDC148C), Color(0xFF0D73EC),
    )

    fun accentFor(seed: Any): Color = Accents[(seed.hashCode() and Int.MAX_VALUE) % Accents.size]
}

private val ColorScheme = darkColorScheme(
    primary = SonoraColors.Green,
    onPrimary = Color.Black,
    secondary = SonoraColors.Green,
    onSecondary = Color.Black,
    background = SonoraColors.Background,
    onBackground = SonoraColors.TextPrimary,
    surface = SonoraColors.Surface,
    onSurface = SonoraColors.TextPrimary,
    surfaceVariant = SonoraColors.SurfaceHigh,
    onSurfaceVariant = SonoraColors.TextSecondary,
    surfaceContainer = SonoraColors.SurfaceHigh,
    surfaceContainerHigh = SonoraColors.SurfaceHigh,
    surfaceContainerHighest = SonoraColors.SurfaceHighest,
    surfaceContainerLow = SonoraColors.Surface,
    outline = SonoraColors.TextTertiary,
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.02).em),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.02).em),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).em),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.08.em),
    )
}

@Composable
fun SonoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = AppTypography, content = content)
}
