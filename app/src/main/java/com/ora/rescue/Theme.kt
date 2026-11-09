package com.ora.rescue

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Cohesive Material 3 color system for Ora Rescue.
 * Built around warm rose, calming deep plum ink, soft warm-cream paper, and gentle sage green.
 * Uses dark text on light surfaces. Full accessibility verification on a device is still required.
 */
val OraRose = Color(0xFF8C3F5A)             // Primary brand rose (accessible contrast > 4.5:1)
val OraRoseDark = Color(0xFF4A1828)         // Deep plum rose for strong emphasis
val OraRoseLight = Color(0xFFFBECEF)        // Soft warm rose container
val OraPlumInk = Color(0xFF2B1D25)          // High-contrast primary text (deep plum charcoal)
val OraPlumMuted = Color(0xFF6E5B65)        // Secondary / supporting text
val OraCreamPaper = Color(0xFFFFF9F5)       // Calming warm-cream background
val OraCreamSurface = Color(0xFFFFFFFF)     // Crisp card surface
val OraCreamVariant = Color(0xFFF5EBE9)     // Soft tinted surface
val OraSage = Color(0xFF38684C)             // Sage green for completion & steady progress
val OraSageLight = Color(0xFFE2EFE7)        // Light sage container
val OraBorder = Color(0xFFD6C3CC)           // Soft outline border
val OraBorderLight = Color(0xFFECE0E6)      // Subtle divider border
val OraAmber = Color(0xFF8D5716)            // Calm amber for unallocated work notices
val OraAmberLight = Color(0xFFFDF1E2)       // Light amber banner background

private val OraColorScheme = lightColorScheme(
    primary = OraRose,
    onPrimary = Color.White,
    primaryContainer = OraRoseLight,
    onPrimaryContainer = OraRoseDark,
    secondary = Color(0xFF664E5A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4DFE7),
    onSecondaryContainer = Color(0xFF22111A),
    tertiary = OraSage,
    onTertiary = Color.White,
    tertiaryContainer = OraSageLight,
    onTertiaryContainer = Color(0xFF0F2618),
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

val OraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OraRescueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OraColorScheme,
        shapes = OraShapes,
        content = content
    )
}

