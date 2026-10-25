package com.zerotwo.focus.permissions

/**
 * The special (Settings-granted) permissions the blocker needs.
 * None of these can be requested with a runtime dialog; the user has to flip a switch in Settings.
 */
enum class AppPermission(
    val title: String,
    val why: String,
    val required: Boolean,
) {
    USAGE_ACCESS(
        title = "Usage access",
        why = "Measures your screen time per app for the leaderboard.",
        required = true,
    ),
    OVERLAY(
        title = "Display over other apps",
        why = "Shows the block screen on top of a social app during class.",
        required = true,
    ),
    ACCESSIBILITY(
        title = "Accessibility service",
        why = "Detects which app you just opened so it can be blocked instantly.",
        required = true,
    ),
    BATTERY_OPTIMIZATION(
        title = "Ignore battery optimization",
        why = "Stops Android from killing the blocker in the background. Recommended.",
        required = false,
    ),
}
