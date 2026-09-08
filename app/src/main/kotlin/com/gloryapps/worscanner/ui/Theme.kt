package com.gloryapps.worscanner.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** What every screen and the strip over the game are drawn in: `Colors` and `Lettering`, as Material reads them. */
@Composable
fun ScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SCHEME, typography = TYPOGRAPHY, shapes = SHAPES, content = content)
}

private val SCHEME = darkColorScheme(
    primary = Colors.accent,
    onPrimary = Colors.onAccent,
    background = Colors.screen,
    onBackground = Colors.text,
    surface = Colors.screen,
    onSurface = Colors.text,
    surfaceContainer = Colors.raised,
    surfaceContainerLowest = Colors.sunken,
    surfaceContainerHigh = Colors.lifted,
    surfaceVariant = Colors.raised,
    onSurfaceVariant = Colors.muted,
    tertiary = Colors.warning,
    outline = Colors.edge,
    outlineVariant = Colors.hairline,
    error = Colors.failure,
    onError = Colors.onAccent,
)

private val TYPOGRAPHY = Typography(
    headlineSmall = Lettering.title,
    titleLarge = Lettering.subtitle,
    titleMedium = Lettering.brand,
    bodyLarge = Lettering.body,
    bodyMedium = Lettering.body,
    bodySmall = Lettering.caption,
    labelLarge = Lettering.action,
    labelMedium = Lettering.caption,
    labelSmall = Lettering.section,
)

private val SHAPES = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(14.dp),
)
