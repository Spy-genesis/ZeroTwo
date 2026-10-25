package com.zerotwo.focus.calendar

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cached calendar state. The block check runs every second and on every app switch, so it reads this
 * cache instead of the provider. Refreshed every 60 s by BlockerService, on calendar changes
 * (ContentObserver), when the UI resumes, and lazily if the cache is older than [MAX_AGE_MS].
 */
object CalendarMonitor {

    private const val MAX_AGE_MS = 60_000L

    private val _snapshot = MutableStateFlow(CalendarSnapshot.NO_PERMISSION)
    val snapshot: StateFlow<CalendarSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    fun refresh(context: Context): CalendarSnapshot =
        CalendarRepository(context.applicationContext).read().also { _snapshot.value = it }

    /** The cached snapshot, re-read first if it's stale or the permission changed since. */
    fun current(context: Context, nowMillis: Long = System.currentTimeMillis()): CalendarSnapshot {
        val cached = _snapshot.value
        val permissionNow = CalendarRepository(context.applicationContext).hasPermission()
        val stale = nowMillis - cached.fetchedAtMillis > MAX_AGE_MS
        return if (stale || cached.hasPermission != permissionNow) refresh(context) else cached
    }
}
