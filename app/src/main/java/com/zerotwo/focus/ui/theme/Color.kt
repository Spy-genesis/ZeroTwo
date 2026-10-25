package com.zerotwo.focus.ui.theme

import androidx.compose.ui.graphics.Color

// ZeroTwo HUD palette: dark mecha cockpit, crimson + pink. Every color in the app comes from here.

// Base
val HudBackground = Color(0xFF0B0508)      // near-black, red tint
val HudSurface = Color(0xFF1A0A10)         // cards: deep wine
val HudSurfaceElevated = Color(0xFF26101A) // raised cards, menus, tracks

// Accents
val HudCrimson = Color(0xFFE0143C)  // primary
val HudHotPink = Color(0xFFFF4D8D)  // secondary
val HudPaleRose = Color(0xFFFFE3EC) // highlight / accent text

// States
val HudBlocked = Color(0xFFFF2D2D)  // error + blocked
val HudMint = Color(0xFF5CF2B0)     // success, use sparingly

// Text
val HudTextPrimary = Color(0xFFFFE3EC)
val HudTextSecondary = Color(0xFFB98A99)

// Derived tones (containers, lines) — same hues, stepped darker/lighter
val HudSurfaceLowest = Color(0xFF070305)
val HudSurfaceLow = Color(0xFF120609)
val HudSurfaceHighest = Color(0xFF301421)
val HudSurfaceBright = Color(0xFF3A1A27)
val HudCrimsonContainer = Color(0xFF4A0A1C)
val HudPinkContainer = Color(0xFF3D0F24)
val HudBlockedContainer = Color(0xFF3D0A0D)
val HudOutline = Color(0xFF6E3446)
val HudOutlineVariant = Color(0xFF3A1A26)
