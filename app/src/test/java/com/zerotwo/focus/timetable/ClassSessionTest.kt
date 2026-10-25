package com.zerotwo.focus.timetable

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class ClassSessionTest {

    private fun session(start: String, end: String) =
        ClassSession(DayOfWeek.MONDAY, LocalTime.parse(start), LocalTime.parse(end), "Test")

    @Test
    fun normalClass_startInclusive_endExclusive() {
        val s = session("09:00", "10:00")
        assertFalse(s.isInSession(LocalTime.parse("08:59")))
        assertTrue(s.isInSession(LocalTime.parse("09:00")))
        assertTrue(s.isInSession(LocalTime.parse("09:59")))
        assertFalse(s.isInSession(LocalTime.parse("10:00")))
        assertFalse(s.isOver(LocalTime.parse("09:30")))
        assertTrue(s.isOver(LocalTime.parse("10:00")))
    }

    @Test
    fun lateDemoClass_crossesMidnight() {
        val s = session("23:30", "00:30")
        assertTrue(s.isInSession(LocalTime.parse("23:45")))
        assertTrue(s.isInSession(LocalTime.parse("00:10")))
        assertFalse(s.isInSession(LocalTime.parse("00:30")))
        assertFalse(s.isInSession(LocalTime.parse("12:00")))
        assertFalse(s.isOver(LocalTime.parse("00:10")))
    }
}
