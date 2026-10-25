package com.zerotwo.focus.calendar

/** A calendar on the device (CalendarContract.Calendars). */
data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    /** ARGB color from the provider, or null. */
    val color: Int?,
)

/**
 * One occurrence of an event (CalendarContract.Instances), so recurring events are already expanded.
 * [beginMillis]/[endMillis] are real instants; all-day events are converted to local midnights on read.
 */
data class CalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
)

/** What the user chose on the Calendar screen. */
data class CalendarFilter(
    val selectedCalendarIds: Set<Long> = emptySet(),
    /** Case-insensitive "title contains"; blank means every event counts. */
    val keyword: String = "",
    val includeAllDay: Boolean = false,
)

/** Latest read of the calendar provider. */
data class CalendarSnapshot(
    val hasPermission: Boolean,
    val calendars: List<DeviceCalendar>,
    /** All instances in the read window, every calendar; filtering happens in [CalendarRules]. */
    val events: List<CalendarEvent>,
    val fetchedAtMillis: Long,
) {
    companion object {
        val NO_PERMISSION = CalendarSnapshot(hasPermission = false, emptyList(), emptyList(), 0L)
    }
}
