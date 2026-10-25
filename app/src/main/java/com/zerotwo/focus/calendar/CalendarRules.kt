package com.zerotwo.focus.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Pure decisions about calendar events. No Android types, so it's unit-tested on the JVM. */
object CalendarRules {

    /** Calendar blocking applies only once the user can read calendars and has picked one that still exists. */
    fun isCalendarActive(snapshot: CalendarSnapshot, filter: CalendarFilter): Boolean =
        snapshot.hasPermission && snapshot.calendars.any { it.id in filter.selectedCalendarIds }

    fun counts(event: CalendarEvent, filter: CalendarFilter): Boolean {
        if (event.calendarId !in filter.selectedCalendarIds) return false
        if (event.allDay && !filter.includeAllDay) return false
        val keyword = filter.keyword.trim()
        return keyword.isEmpty() || event.title.contains(keyword, ignoreCase = true)
    }

    /** Start inclusive, end exclusive, so back-to-back events hand over exactly at the boundary. */
    fun isActive(event: CalendarEvent, nowMillis: Long): Boolean =
        nowMillis >= event.beginMillis && nowMillis < event.endMillis

    /**
     * The event to block for right now, or null. When events overlap, the one that started most
     * recently wins, so the block screen names what the student just walked into.
     */
    fun activeEvent(events: List<CalendarEvent>, nowMillis: Long, filter: CalendarFilter): CalendarEvent? =
        events.filter { counts(it, filter) && isActive(it, nowMillis) }
            .maxWithOrNull(compareBy<CalendarEvent> { it.beginMillis }.thenBy { it.endMillis })

    /** Matching events that overlap [day] in [zone], in start order (includes ones already over). */
    fun eventsOn(day: LocalDate, events: List<CalendarEvent>, filter: CalendarFilter, zone: ZoneId): List<CalendarEvent> {
        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return events.filter { counts(it, filter) && it.beginMillis < dayEnd && it.endMillis > dayStart }
            .sortedWith(compareBy({ it.beginMillis }, { it.endMillis }))
    }

    /** Next matching event that starts later today, or null. */
    fun nextEventToday(events: List<CalendarEvent>, nowMillis: Long, filter: CalendarFilter, zone: ZoneId): CalendarEvent? {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return eventsOn(today, events, filter, zone).firstOrNull { it.beginMillis > nowMillis }
    }

    /**
     * The provider stores all-day events as UTC midnights. Re-anchor them to local midnights,
     * otherwise a Monday all-day event would start at 05:00 Monday in Pakistan (UTC+5).
     */
    fun normalizeAllDay(beginUtcMillis: Long, endUtcMillis: Long, zone: ZoneId): Pair<Long, Long> {
        fun localMidnight(utcMillis: Long) =
            Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        return localMidnight(beginUtcMillis) to localMidnight(endUtcMillis)
    }
}
