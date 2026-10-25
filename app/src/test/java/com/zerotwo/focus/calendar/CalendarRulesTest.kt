package com.zerotwo.focus.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarRulesTest {

    // Fixed zone so the tests don't depend on the machine running them.
    private val zone = ZoneId.of("Asia/Karachi")
    private val day = LocalDate.of(2026, 10, 26)

    private fun at(time: String, date: LocalDate = day): Long =
        LocalDateTime.of(date, java.time.LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()

    private fun event(
        title: String,
        begin: Long,
        end: Long,
        calendarId: Long = 1,
        allDay: Boolean = false,
    ) = CalendarEvent(eventId = title.hashCode().toLong(), calendarId, title, begin, end, allDay)

    private val all = CalendarFilter(selectedCalendarIds = setOf(1L, 2L))

    @Test
    fun active_startInclusive_endExclusive() {
        val lecture = event("Lecture", at("09:00"), at("10:00"))
        assertFalse(CalendarRules.isActive(lecture, at("08:59")))
        assertTrue(CalendarRules.isActive(lecture, at("09:00")))
        assertTrue(CalendarRules.isActive(lecture, at("09:59")))
        assertFalse(CalendarRules.isActive(lecture, at("10:00")))
    }

    @Test
    fun eventCrossingMidnight_isActiveOnBothSides() {
        val night = event("Night lab", at("23:00"), at("01:00", day.plusDays(1)))
        val events = listOf(night)
        assertEquals(night, CalendarRules.activeEvent(events, at("23:30"), all))
        assertEquals(night, CalendarRules.activeEvent(events, at("00:30", day.plusDays(1)), all))
        assertNull(CalendarRules.activeEvent(events, at("01:00", day.plusDays(1)), all))
        assertNull(CalendarRules.activeEvent(events, at("22:59"), all))
    }

    @Test
    fun eventCrossingMidnight_showsOnBothDays() {
        val night = event("Night lab", at("23:00"), at("01:00", day.plusDays(1)))
        assertEquals(listOf(night), CalendarRules.eventsOn(day, listOf(night), all, zone))
        assertEquals(listOf(night), CalendarRules.eventsOn(day.plusDays(1), listOf(night), all, zone))
        assertTrue(CalendarRules.eventsOn(day.plusDays(2), listOf(night), all, zone).isEmpty())
    }

    @Test
    fun backToBack_handsOverExactlyAtTheBoundary() {
        val first = event("Data Structures", at("09:00"), at("10:00"))
        val second = event("Discrete Math", at("10:00"), at("11:00"))
        val events = listOf(first, second)
        assertEquals(first, CalendarRules.activeEvent(events, at("09:59"), all))
        assertEquals(second, CalendarRules.activeEvent(events, at("10:00"), all))
        assertEquals(second, CalendarRules.activeEvent(events, at("10:59"), all))
        assertNull(CalendarRules.activeEvent(events, at("11:00"), all))
    }

    @Test
    fun overlapping_mostRecentlyStartedWins() {
        val longBlock = event("Study block", at("08:00"), at("12:00"))
        val lecture = event("Lecture", at("09:00"), at("10:00"))
        assertEquals(lecture, CalendarRules.activeEvent(listOf(longBlock, lecture), at("09:30"), all))
        assertEquals(longBlock, CalendarRules.activeEvent(listOf(longBlock, lecture), at("10:30"), all))
    }

    @Test
    fun onlySelectedCalendarsCount() {
        val other = event("Gym", at("09:00"), at("10:00"), calendarId = 3)
        assertNull(CalendarRules.activeEvent(listOf(other), at("09:30"), all))
        assertNull(CalendarRules.activeEvent(listOf(other), at("09:30"), CalendarFilter()))
    }

    @Test
    fun keyword_isCaseInsensitive_andBlankMeansEverything() {
        val lecture = event("CS-201 LECTURE", at("09:00"), at("10:00"))
        val lunch = event("Lunch with Ali", at("09:00"), at("10:00"))
        val keyword = all.copy(keyword = "  lecture ")
        assertTrue(CalendarRules.counts(lecture, keyword))
        assertFalse(CalendarRules.counts(lunch, keyword))
        assertTrue(CalendarRules.counts(lunch, all.copy(keyword = "")))
    }

    @Test
    fun allDayEvents_ignoredUnlessEnabled() {
        val holiday = event("Holiday", at("00:00"), at("00:00", day.plusDays(1)), allDay = true)
        assertNull(CalendarRules.activeEvent(listOf(holiday), at("12:00"), all))
        assertEquals(holiday, CalendarRules.activeEvent(listOf(holiday), at("12:00"), all.copy(includeAllDay = true)))
    }

    @Test
    fun allDay_reanchoredFromUtcToLocalMidnight() {
        val utcStart = day.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val utcEnd = day.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val (begin, end) = CalendarRules.normalizeAllDay(utcStart, utcEnd, zone)
        assertEquals(at("00:00"), begin)
        assertEquals(at("00:00", day.plusDays(1)), end)
    }

    @Test
    fun nextEventToday_skipsStartedAndTomorrow() {
        val now = at("10:30")
        val running = event("Running", at("10:00"), at("11:00"))
        val later = event("Later", at("13:00"), at("14:00"))
        val tomorrow = event("Tomorrow", at("09:00", day.plusDays(1)), at("10:00", day.plusDays(1)))
        assertEquals(later, CalendarRules.nextEventToday(listOf(tomorrow, running, later), now, all, zone))
        assertNull(CalendarRules.nextEventToday(listOf(running, tomorrow), now, all, zone))
    }

    @Test
    fun calendarActive_needsPermissionAndAnExistingSelectedCalendar() {
        val cals = listOf(DeviceCalendar(1, "Uni", "me@uni", null))
        val snapshot = CalendarSnapshot(true, cals, emptyList(), 1L)
        assertTrue(CalendarRules.isCalendarActive(snapshot, CalendarFilter(setOf(1L))))
        assertFalse(CalendarRules.isCalendarActive(snapshot, CalendarFilter(setOf(9L)))) // deleted calendar
        assertFalse(CalendarRules.isCalendarActive(snapshot, CalendarFilter()))
        assertFalse(CalendarRules.isCalendarActive(CalendarSnapshot.NO_PERMISSION, CalendarFilter(setOf(1L))))
    }
}
