package com.ora.rescue

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** User-selectable appearance preference stored in SharedPreferences. */
enum class ThemePreference { LIGHT, DARK, SYSTEM }

val OraFontFamily = FontFamily(Font(R.font.inter))

private val LightColors = lightColorScheme(
    primary = Color(0xFF5546C6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE5FF),
    onPrimaryContainer = Color(0xFF382A85),
    secondary = Color(0xFF3D56A3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EBFF),
    onSecondaryContainer = Color(0xFF253D81),
    tertiary = Color(0xFF226747),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3F5EA),
    onTertiaryContainer = Color(0xFF164E35),
    background = Color(0xFFF2F0FF),
    onBackground = Color(0xFF22233F),
    surface = Color(0xFFFCFAFF),
    onSurface = Color(0xFF22233F),
    surfaceVariant = Color(0xFFEDE9FA),
    onSurfaceVariant = Color(0xFF5F5775),
    surfaceContainer = Color(0xFFF0EBFF),
    surfaceContainerHigh = Color(0xFFEAE4FA),
    outline = Color(0xFF817895),
    outlineVariant = Color(0xFFD8D0EB),
    inverseSurface = Color(0xFF2F2B43),
    inverseOnSurface = Color(0xFFF4EFFD),
    inversePrimary = Color(0xFFC9BFFF),
    error = Color(0xFFAA2847),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5EC),
    onErrorContainer = Color(0xFF831F39)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC4B7FF),
    onPrimary = Color(0xFF211650),
    primaryContainer = Color(0xFF38306B),
    onPrimaryContainer = Color(0xFFE6DDFF),
    secondary = Color(0xFFB1C5FF),
    onSecondary = Color(0xFF13285A),
    secondaryContainer = Color(0xFF28365C),
    onSecondaryContainer = Color(0xFFDCE5FF),
    tertiary = Color(0xFF9EDDBB),
    onTertiary = Color(0xFF0C3323),
    tertiaryContainer = Color(0xFF203C32),
    onTertiaryContainer = Color(0xFFB9F0CF),
    background = Color(0xFF10101F),
    onBackground = Color(0xFFF0EDF9),
    surface = Color(0xFF1B1930),
    onSurface = Color(0xFFF0EDF9),
    surfaceVariant = Color(0xFF26223E),
    onSurfaceVariant = Color(0xFFC8BFDA),
    surfaceContainer = Color(0xFF211D37),
    surfaceContainerHigh = Color(0xFF2B2644),
    outline = Color(0xFF9A90AD),
    outlineVariant = Color(0xFF49415E),
    inverseSurface = Color(0xFFEBE5F8),
    inverseOnSurface = Color(0xFF2A253D),
    inversePrimary = Color(0xFF5144BA),
    error = Color(0xFFFFADC1),
    onError = Color(0xFF570C29),
    errorContainer = Color(0xFF542539),
    onErrorContainer = Color(0xFFFFD9E3)
)

internal data class OraAccentColors(
    val banner: Brush,
    val onBanner: Color,
    val avatar: Brush,
    val warning: Color,
    val warningContainer: Color,
    val dark: Boolean
)

private val LightAccents = OraAccentColors(
    banner = Brush.linearGradient(listOf(Color(0xFF4157BD), Color(0xFF6C43BA))),
    onBanner = Color.White,
    avatar = Brush.linearGradient(listOf(Color(0xFFC8DCFF), Color(0xFFC8B0FF))),
    warning = Color(0xFF874D14),
    warningContainer = Color(0xFFFFEFDC),
    dark = false
)

private val DarkAccents = OraAccentColors(
    banner = Brush.linearGradient(listOf(Color(0xFF292E62), Color(0xFF492F70))),
    onBanner = Color(0xFFF3EDFF),
    avatar = Brush.linearGradient(listOf(Color(0xFF304675), Color(0xFF563A7D))),
    warning = Color(0xFFFFC484),
    warningContainer = Color(0xFF442D21),
    dark = true
)

internal val LocalOraAccents = staticCompositionLocalOf { LightAccents }

// ─── Shapes ───────────────────────────────────────────────────────────────────

val OraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ─── Typography ───────────────────────────────────────────────────────────────

/**
 * iOS-inspired type scale using Inter.
 * Body copy: 16sp / 1.5 line height. Secondary labels: 13–14sp.
 * Headings: 24–28sp semibold. Actions: semibold 15sp.
 * Avoids tiny faint text — minimum useful size is labelSmall at 11sp.
 */
private fun buildOraTypography(): Typography {
    fun style(size: Int, weight: FontWeight, lineHeight: Double = size * 1.35) = TextStyle(
        fontFamily = OraFontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp
    )
    return Typography(
        displayLarge  = style(32, FontWeight.Bold,     lineHeight = 40.0),
        displayMedium = style(28, FontWeight.Bold,     lineHeight = 36.0),
        displaySmall  = style(24, FontWeight.SemiBold, lineHeight = 32.0),
        headlineLarge  = style(26, FontWeight.SemiBold, lineHeight = 34.0),
        headlineMedium = style(22, FontWeight.SemiBold, lineHeight = 30.0),
        headlineSmall  = style(18, FontWeight.SemiBold, lineHeight = 26.0),
        titleLarge  = style(17, FontWeight.SemiBold, lineHeight = 24.0),
        titleMedium = style(15, FontWeight.SemiBold, lineHeight = 22.0),
        titleSmall  = style(14, FontWeight.Medium,   lineHeight = 20.0),
        bodyLarge  = style(16, FontWeight.Normal, lineHeight = 24.0),
        bodyMedium = style(14, FontWeight.Normal, lineHeight = 22.0),
        bodySmall  = style(13, FontWeight.Normal, lineHeight = 20.0),
        labelLarge  = style(14, FontWeight.SemiBold, lineHeight = 20.0),
        labelMedium = style(13, FontWeight.Medium,   lineHeight = 18.0),
        labelSmall  = style(11, FontWeight.Medium,   lineHeight = 16.0)
    )
}

private val OraTypography = buildOraTypography()

// ─── Theme Composable ─────────────────────────────────────────────────────────

@Composable
fun OraRescueTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themePreference) {
        ThemePreference.DARK   -> true
        ThemePreference.LIGHT  -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalOraAccents provides if (useDark) DarkAccents else LightAccents) {
        MaterialTheme(
            typography = OraTypography,
            colorScheme = if (useDark) DarkColors else LightColors,
            shapes = OraShapes,
            content = content
        )
    }
}
