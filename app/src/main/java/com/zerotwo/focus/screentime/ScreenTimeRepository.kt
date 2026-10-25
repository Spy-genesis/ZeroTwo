package com.zerotwo.focus.screentime

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.zerotwo.focus.config.BlockedAppsConfig
import com.zerotwo.focus.permissions.AppPermission
import com.zerotwo.focus.permissions.PermissionChecker
import java.time.LocalDate
import java.time.ZoneId

data class AppUsage(
    val packageName: String,
    val label: String,
    val millis: Long,
    /** True if the app is in the blocked-apps config (counts as "social"). */
    val isSocial: Boolean,
)

data class DailyUsage(
    val date: LocalDate,
    /** Sorted by time spent, largest first. */
    val apps: List<AppUsage>,
) {
    val totalMillis: Long get() = apps.sumOf { it.millis }
}

/**
 * Per-app, per-day foreground time built from raw UsageStats events.
 * Events are used instead of queryUsageStats(INTERVAL_DAILY), whose buckets don't line up
 * with local midnight on many devices. Sessions crossing midnight are split between days.
 */
class ScreenTimeRepository(private val context: Context) {

    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val zone = ZoneId.systemDefault()

    /** Oldest day first, today last. Returns null without Usage Access. */
    fun lastDays(days: Int = 7, nowMillis: Long = System.currentTimeMillis()): List<DailyUsage>? {
        // Without access, queryEvents usually returns an empty result instead of throwing.
        if (!PermissionChecker.isGranted(context, AppPermission.USAGE_ACCESS)) return null
        val today = LocalDate.now(zone)
        val firstDay = today.minusDays(days - 1L)
        val windowStart = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()

        // Start reading a day early so an app already open at the window's first midnight is counted;
        // add() clips everything before windowStart.
        val events = try {
            usageStats.queryEvents(windowStart - LOOKBEHIND_MS, nowMillis)
        } catch (e: SecurityException) {
            return null
        } ?: return null

        val perDay = HashMap<LocalDate, HashMap<String, Long>>()
        fun add(pkg: String, from: Long, to: Long) {
            var start = maxOf(from, windowStart)
            while (start < to) {
                val day = java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
                val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = minOf(to, dayEnd)
                perDay.getOrPut(day) { HashMap() }.merge(pkg, end - start, Long::plus)
                start = end
            }
        }

        // Package -> when it came to the foreground.
        val open = HashMap<String, Long>()
        fun close(pkg: String, at: Long) = open.remove(pkg)?.let { add(pkg, it, at) }
        fun closeAll(at: Long) = open.keys.toList().forEach { close(it, at) }

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val at = event.timeStamp
            @Suppress("DEPRECATION") // Old names for ACTIVITY_RESUMED / ACTIVITY_PAUSED, which need API 29.
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // Only one app is in front; closing the others also covers missed "paused" events.
                    open.keys.filter { it != pkg }.forEach { close(it, at) }
                    open.putIfAbsent(pkg, at)
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> close(pkg, at)
                EVENT_SCREEN_NON_INTERACTIVE, EVENT_DEVICE_SHUTDOWN -> closeAll(at)
            }
        }
        closeAll(nowMillis) // whatever is still open is in use right now

        val excluded = excludedPackages()
        val socialPackages = BlockedAppsConfig.DEFAULT_BLOCKED_APPS.flatMap { it.packages }.toSet()
        return (0 until days).map { offset ->
            val date = firstDay.plusDays(offset.toLong())
            val apps = perDay[date].orEmpty()
                .filter { (pkg, ms) -> pkg !in excluded && ms >= MIN_APP_MILLIS }
                .map { (pkg, ms) -> AppUsage(pkg, labelFor(pkg), ms, pkg in socialPackages) }
                .sortedByDescending { it.millis }
            DailyUsage(date, apps)
        }
    }

    /** Launchers and System UI are "being on the phone", not app use (Digital Wellbeing skips them too). */
    private fun excludedPackages(): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launchers = try {
            context.packageManager.queryIntentActivities(home, PackageManager.MATCH_ALL)
                .map { it.activityInfo.packageName }
        } catch (e: Exception) {
            emptyList()
        }
        return launchers.toSet() + "com.android.systemui"
    }

    private val labels = HashMap<String, String>()

    private fun labelFor(pkg: String): String = labels.getOrPut(pkg) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg // uninstalled since, or hidden by package visibility
        }
    }

    private companion object {
        // Literal values of SCREEN_NON_INTERACTIVE (API 28) and DEVICE_SHUTDOWN (API 29);
        // on older Android these events simply never appear.
        const val EVENT_SCREEN_NON_INTERACTIVE = 16
        const val EVENT_DEVICE_SHUTDOWN = 26
        const val MIN_APP_MILLIS = 60_000L
        const val LOOKBEHIND_MS = 24 * 60 * 60_000L
    }
}

/** "2h 05m", "45m", "<1m". */
fun formatDuration(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        minutes < 1 -> "<1m"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${"%02d".format(minutes % 60)}m"
    }
}
