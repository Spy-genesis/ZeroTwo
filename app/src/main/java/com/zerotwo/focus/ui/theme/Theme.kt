package com.zerotwo.focus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The single brand accent. Everything else is Material 3 defaults.
// Dynamic (wallpaper) color is deliberately off so the accent stays consistent.
val Accent = Color(0xFF4F46E5)
private val AccentLight = Color(0xFFA5B4FC)

// Status colors for granted / not granted chips.
val Granted = Color(0xFF16A34A)
val NotGranted = Color(0xFFDC2626)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
)

private val DarkColors = darkColorScheme(
    primary = AccentLight,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
)

@Composable
fun ZeroTwoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
