package com.zerotwo.focus.blocker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Fallback foreground-app detector using UsageStatsManager, for when the accessibility
 * service is off or gets killed. Requires Usage Access; returns null without it.
 */
class ForegroundAppDetector(context: Context) {

    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Remembered between polls: if nothing switched in the query window, the last app is still on top.
    private var lastForeground: String? = null

    fun currentForegroundApp(): String? {
        val now = System.currentTimeMillis()
        val events = try {
            usageStats.queryEvents(now - LOOKBACK_MS, now)
        } catch (e: SecurityException) {
            return null
        } ?: return lastForeground

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION") // Same value as ACTIVITY_RESUMED, which needs API 29.
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForeground = event.packageName
            }
        }
        return lastForeground
    }

    private companion object {
        const val LOOKBACK_MS = 10_000L
    }
}
