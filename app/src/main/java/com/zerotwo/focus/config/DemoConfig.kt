package com.zerotwo.focus.config

/**
 * DEMO MODE CONFIG. Demo mode is hidden: tap the "ZeroTwo" title on Home [DEMO_UNLOCK_TAPS] times.
 * While it's on, a fake class is in session and the leaderboard adds sample classmates,
 * so the whole story can be shown on one phone, even offline.
 */
object DemoConfig {

    const val DEMO_UNLOCK_TAPS = 5

    /** The fake class starts the moment demo mode is switched on. */
    const val DEMO_COURSE_NAME = "Operating Systems (Demo)"
    const val DEMO_DURATION_MINUTES = 60L

    data class DemoClassmate(val studentId: String, val fullDayMinutes: Long, val socialShare: Double)

    /**
     * Shown on the leaderboard only, never uploaded. Minutes are for a full day;
     * "Today" scales them by how much of the day has passed so they look plausible at any hour.
     */
    val DEMO_CLASSMATES: List<DemoClassmate> = listOf(
        DemoClassmate("24102", fullDayMinutes = 52, socialShare = 0.20),
        DemoClassmate("24117", fullDayMinutes = 95, socialShare = 0.35),
        DemoClassmate("24123", fullDayMinutes = 148, socialShare = 0.50),
        DemoClassmate("24131", fullDayMinutes = 203, socialShare = 0.60),
        DemoClassmate("24138", fullDayMinutes = 262, socialShare = 0.70),
        DemoClassmate("24144", fullDayMinutes = 341, socialShare = 0.75),
    )
}
