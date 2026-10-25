package com.zerotwo.focus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Always dark: the HUD look doesn't have a light variant, and dynamic (wallpaper) color stays off.
private val HudColorScheme = darkColorScheme(
    primary = HudCrimson,
    onPrimary = HudTextPrimary,
    primaryContainer = HudCrimsonContainer,
    onPrimaryContainer = HudPaleRose,
    inversePrimary = HudCrimson,
    secondary = HudHotPink,
    onSecondary = HudBackground,
    secondaryContainer = HudPinkContainer,
    onSecondaryContainer = HudPaleRose,
    tertiary = HudPaleRose,
    onTertiary = HudBackground,
    tertiaryContainer = HudSurfaceElevated,
    onTertiaryContainer = HudPaleRose,
    background = HudBackground,
    onBackground = HudTextPrimary,
    surface = HudSurface,
    onSurface = HudTextPrimary,
    surfaceVariant = HudSurfaceElevated,
    onSurfaceVariant = HudTextSecondary,
    surfaceTint = HudCrimson,
    inverseSurface = HudPaleRose,
    inverseOnSurface = HudSurface,
    error = HudBlocked,
    onError = HudBackground,
    errorContainer = HudBlockedContainer,
    onErrorContainer = HudPaleRose,
    outline = HudOutline,
    outlineVariant = HudOutlineVariant,
    scrim = HudBackground,
    surfaceBright = HudSurfaceBright,
    surfaceDim = HudBackground,
    surfaceContainerLowest = HudSurfaceLowest,
    surfaceContainerLow = HudSurfaceLow,
    surfaceContainer = HudSurface,
    surfaceContainerHigh = HudSurfaceElevated,
    surfaceContainerHighest = HudSurfaceHighest,
)

@Composable
fun ZeroTwoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HudColorScheme,
        typography = HudTypography,
        shapes = HudShapes,
        content = content,
    )
}
