package com.ora.rescue

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** User-selectable appearance preference stored in SharedPreferences. */
enum class ThemePreference { LIGHT, DARK, SYSTEM }

// ─── Light Palette ────────────────────────────────────────────────────────────
val OraRose            = Color(0xFF7763FF)   // Primary brand indigo/violet
val OraRoseDark        = Color(0xFF40316D)   // Deep plum for strong emphasis
val OraRoseLight       = Color(0xFFEAE5FF)   // Soft lavender container
val OraPlumInk         = Color(0xFF22233F)   // High-contrast primary text
val OraPlumMuted       = Color(0xFF7A7591)   // Secondary text
val OraCreamPaper      = Color(0xFFF2F0FF)   // Icy lavender background
val OraCreamSurface    = Color(0xFFFFFFFF)   // Crisp card surface
val OraCreamVariant    = Color(0xFFEDE9FD)   // Soft tinted surface
val OraSage            = Color(0xFF6154BD)   // Completion / steady progress
val OraSageLight       = Color(0xFFE8E4FF)   // Light sage container
val OraBorder          = Color(0xFFD0CAEC)   // Soft outline border
val OraBorderLight     = Color(0xFFE4DFF5)   // Subtle divider
val OraAmber           = Color(0xFF8D5716)   // Amber for unallocated work
val OraAmberLight      = Color(0xFFFDF1E2)   // Light amber background

// ─── Dark Palette ─────────────────────────────────────────────────────────────
val OraDarkBackground       = Color(0xFF0F0E1D)   // Deep navy/charcoal
val OraDarkSurface          = Color(0xFF18172E)   // Slightly lighter surface layer
val OraDarkSurfaceVariant   = Color(0xFF211F3A)   // Card/elevated surface
val OraDarkPrimary          = Color(0xFF9D8FFF)   // Brighter violet for dark bg
val OraDarkOnPrimary        = Color(0xFF1A1234)
val OraDarkPrimaryContainer = Color(0xFF302770)
val OraDarkOnPrimaryContainer = Color(0xFFC5B8FF)
val OraDarkOnBackground     = Color(0xFFE8E6F4)   // Near-white primary text
val OraDarkOnSurface        = Color(0xFFE8E6F4)
val OraDarkOnSurfaceVariant = Color(0xFFB0ACCC)   // Readable muted label
val OraDarkOutline          = Color(0xFF4A4570)
val OraDarkOutlineVariant   = Color(0xFF302D55)
val OraDarkSecondary        = Color(0xFF8E7FD4)
val OraDarkSageGreen        = Color(0xFF7DC48A)   // Softer success green for dark
val OraDarkSageContainer    = Color(0xFF152D1E)
val OraDarkAmber            = Color(0xFFFFB86C)   // Amber for dark mode
val OraDarkAmberContainer   = Color(0xFF2A1800)
val OraDarkError            = Color(0xFFFF6B7A)

/**
 * Inter is bundled under the OFL license (preview/OFL.txt).
 * The browser font stack is: -apple-system, BlinkMacSystemFont, 'SF Pro Text', Inter, 'Segoe UI', sans-serif
 * — which uses Apple's SF system font where available; Inter is the licensed fallback for Windows/Android.
 * SF Pro font files are NOT redistributed here.
 */
val OraFontFamily = FontFamily(Font(R.font.inter))

// ─── Color Schemes ────────────────────────────────────────────────────────────

private val OraLightColorScheme = lightColorScheme(
    primary = OraRose,
    onPrimary = Color.White,
    primaryContainer = OraRoseLight,
    onPrimaryContainer = OraRoseDark,
    secondary = Color(0xFF7060A8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6DEFF),
    onSecondaryContainer = Color(0xFF30264D),
    tertiary = OraSage,
    onTertiary = Color.White,
    tertiaryContainer = OraSageLight,
    onTertiaryContainer = Color(0xFF2E2453),
    background = OraCreamPaper,
    onBackground = OraPlumInk,
    surface = OraCreamPaper,
    onSurface = OraPlumInk,
    surfaceVariant = OraCreamVariant,
    onSurfaceVariant = OraPlumMuted,
    outline = OraBorder,
    outlineVariant = OraBorderLight,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val OraDarkColorScheme = darkColorScheme(
    primary = OraDarkPrimary,
    onPrimary = OraDarkOnPrimary,
    primaryContainer = OraDarkPrimaryContainer,
    onPrimaryContainer = OraDarkOnPrimaryContainer,
    secondary = OraDarkSecondary,
    onSecondary = Color(0xFF1A1234),
    secondaryContainer = Color(0xFF2D2650),
    onSecondaryContainer = Color(0xFFCFC3FF),
    tertiary = OraDarkSageGreen,
    onTertiary = Color(0xFF0A1F12),
    tertiaryContainer = OraDarkSageContainer,
    onTertiaryContainer = Color(0xFFB0E8BE),
    background = OraDarkBackground,
    onBackground = OraDarkOnBackground,
    surface = OraDarkSurface,
    onSurface = OraDarkOnSurface,
    surfaceVariant = OraDarkSurfaceVariant,
    onSurfaceVariant = OraDarkOnSurfaceVariant,
    outline = OraDarkOutline,
    outlineVariant = OraDarkOutlineVariant,
    error = OraDarkError,
    onError = Color(0xFF2D000A),
    errorContainer = Color(0xFF5C1120),
    onErrorContainer = Color(0xFFFFB3BC)
)

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
    MaterialTheme(
        typography = buildOraTypography(),
        colorScheme = if (useDark) OraDarkColorScheme else OraLightColorScheme,
        shapes = OraShapes,
        content = content
    )
}
