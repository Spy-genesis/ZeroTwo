package com.zerotwo.focus.blocker

import android.content.Context
import com.zerotwo.focus.calendar.CalendarEvent
import com.zerotwo.focus.calendar.CalendarMonitor
import com.zerotwo.focus.calendar.CalendarRules
import com.zerotwo.focus.calendar.CalendarSettings
import com.zerotwo.focus.config.DemoConfig
import com.zerotwo.focus.timetable.ClassSession
import com.zerotwo.focus.timetable.TimetableRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Something coming up later today that will start blocking, for "Next: …" lines. */
data class NextBlock(val title: String, val start: LocalTime)

sealed interface BlockStatus {
    val isBlocking: Boolean

    /** A timetable class (or the demo class) is in session: blocking is forced on. */
    data class InClass(val session: ClassSession, val isDemo: Boolean) : BlockStatus {
        override val isBlocking get() = true
    }

    /** A calendar event in a selected calendar is happening now: blocking is forced on. */
    data class InEvent(val event: CalendarEvent) : BlockStatus {
        override val isBlocking get() = true
        val end: LocalTime get() = Instant.ofEpochMilli(event.endMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    }

    /** Nothing scheduled, but the user switched blocking on. */
    data object Manual : BlockStatus {
        override val isBlocking get() = true
    }

    data class Off(val next: NextBlock?) : BlockStatus {
        override val isBlocking get() = false
    }
}

/** A class or event is running, so the user can't loosen blocking right now. */
val BlockStatus.isInSession: Boolean get() = this is BlockStatus.InClass || this is BlockStatus.InEvent

/**
 * The single rule for "should social apps be blocked right now, and why":
 * 1. Demo mode → the fake class.
 * 2. Calendar set up (permission + at least one selected calendar) → a matching event happening now.
 *    Otherwise (no permission / nothing selected) → the mock timetable.
 * 3. The manual switch.
 */
object BlockPolicy {

    fun evaluate(context: Context, nowMillis: Long = System.currentTimeMillis()): BlockStatus {
        val state = BlockerSettings.get(context).state.value
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)

        if (state.isDemoActive(nowMillis)) {
            val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(state.demoStartedAtMillis), zone)
            val session = ClassSession(
                day = start.dayOfWeek,
                start = start.toLocalTime(),
                end = start.toLocalTime().plusMinutes(DemoConfig.DEMO_DURATION_MINUTES),
                course = DemoConfig.DEMO_COURSE_NAME,
            )
            return BlockStatus.InClass(session, isDemo = true)
        }

        val calendar = CalendarMonitor.current(context, nowMillis)
        val filter = CalendarSettings.get(context).filter.value
        val next: NextBlock?
        if (CalendarRules.isCalendarActive(calendar, filter)) {
            CalendarRules.activeEvent(calendar.events, nowMillis, filter)?.let { return BlockStatus.InEvent(it) }
            next = CalendarRules.nextEventToday(calendar.events, nowMillis, filter, zone)?.let {
                NextBlock(it.title, Instant.ofEpochMilli(it.beginMillis).atZone(zone).toLocalTime())
            }
        } else {
            val timetable = TimetableRepository.get(context)
            timetable.currentSession(now.dayOfWeek, now.toLocalTime())?.let {
                return BlockStatus.InClass(it, isDemo = false)
            }
            next = timetable.nextSession(now.dayOfWeek, now.toLocalTime())?.let { NextBlock(it.course, it.start) }
        }

        if (state.manualBlockingEnabled) return BlockStatus.Manual
        return BlockStatus.Off(next)
    }

    /**
     * Re-evaluates whenever blocker or calendar settings change, when the calendar cache refreshes,
     * and every [tickMillis], so start/end times are picked up.
     */
    fun statusFlow(context: Context, tickMillis: Long = 15_000L): Flow<BlockStatus> {
        val ticker = flow {
            while (true) {
                emit(Unit)
                delay(tickMillis)
            }
        }
        return combine(
            BlockerSettings.get(context).state,
            CalendarSettings.get(context).filter,
            CalendarMonitor.snapshot,
            ticker,
        ) { _, _, _, _ -> evaluate(context) }
    }
}
