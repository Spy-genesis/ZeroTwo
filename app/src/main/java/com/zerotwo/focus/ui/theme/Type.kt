package com.zerotwo.focus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zerotwo.focus.R

/** Headings: wide techno face. Callers uppercase heading strings; Compose has no text-transform. */
val OrbitronFamily = FontFamily(
    Font(R.font.orbitron_semibold, FontWeight.SemiBold),
    Font(R.font.orbitron_bold, FontWeight.Bold),
    Font(R.font.orbitron_black, FontWeight.Black),
)

/** Body text. */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Numbers: screen time, ranks, clock times. */
val MonoFamily = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
)

private fun heading(size: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = OrbitronFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * 1.25).sp,
    letterSpacing = tracking.em,
)

private fun body(size: Int, weight: FontWeight, lineHeight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.em,
)

val HudTypography = Typography(
    displayLarge = heading(48, FontWeight.Black, 0.08),
    displayMedium = heading(40, FontWeight.Black, 0.08),
    displaySmall = heading(32, FontWeight.Bold, 0.08),
    headlineLarge = heading(28, FontWeight.Bold, 0.12),
    headlineMedium = heading(24, FontWeight.Bold, 0.12),
    headlineSmall = heading(20, FontWeight.Bold, 0.1),
    titleLarge = heading(18, FontWeight.Bold, 0.12),
    titleMedium = heading(14, FontWeight.SemiBold, 0.12),
    titleSmall = body(14, FontWeight.SemiBold, 20, 0.02),
    bodyLarge = body(16, FontWeight.Normal, 24),
    bodyMedium = body(14, FontWeight.Normal, 20),
    bodySmall = body(12, FontWeight.Normal, 16),
    labelLarge = body(14, FontWeight.SemiBold, 20, 0.06),
    labelMedium = body(12, FontWeight.Medium, 16, 0.06),
    labelSmall = body(11, FontWeight.Medium, 16, 0.06),
)

/** Big readouts (hero screen-time number). */
val HudNumberLarge = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp)

/** Inline numbers: durations, ranks, clock times. */
val HudNumber = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)
