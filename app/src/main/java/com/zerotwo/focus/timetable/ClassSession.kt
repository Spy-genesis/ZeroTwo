package com.zerotwo.focus.timetable

import android.content.Context
import android.text.format.DateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ClassSession(
    val day: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    val course: String,
) {
    /** Timetable classes never cross midnight, but a late-night demo class can, e.g. 23:30–00:30. */
    private val crossesMidnight: Boolean get() = !end.isAfter(start)

    fun isInSession(time: LocalTime): Boolean =
        if (crossesMidnight) !time.isBefore(start) || time.isBefore(end)
        else !time.isBefore(start) && time.isBefore(end)

    fun isOver(time: LocalTime): Boolean = !crossesMidnight && !time.isBefore(end)
}

/** Formats a time using the device's 12h/24h preference. */
fun LocalTime.format(context: Context): String =
    format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"))
