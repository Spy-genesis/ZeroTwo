package com.zerotwo.focus.calendar

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The Calendar screen's choices, in SharedPreferences. Nothing is selected until the user picks. */
class CalendarSettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("calendar", Context.MODE_PRIVATE)

    private val _filter = MutableStateFlow(
        CalendarFilter(
            selectedCalendarIds = prefs.getStringSet(KEY_SELECTED, emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet(),
            keyword = prefs.getString(KEY_KEYWORD, "")!!,
            includeAllDay = prefs.getBoolean(KEY_ALL_DAY, false),
        ),
    )
    val filter: StateFlow<CalendarFilter> = _filter.asStateFlow()

    fun setCalendarSelected(calendarId: Long, selected: Boolean) {
        val ids = _filter.value.selectedCalendarIds.let { if (selected) it + calendarId else it - calendarId }
        prefs.edit().putStringSet(KEY_SELECTED, ids.map(Long::toString).toSet()).apply()
        _filter.value = _filter.value.copy(selectedCalendarIds = ids)
    }

    fun setKeyword(keyword: String) {
        prefs.edit().putString(KEY_KEYWORD, keyword).apply()
        _filter.value = _filter.value.copy(keyword = keyword)
    }

    fun setIncludeAllDay(include: Boolean) {
        prefs.edit().putBoolean(KEY_ALL_DAY, include).apply()
        _filter.value = _filter.value.copy(includeAllDay = include)
    }

    companion object {
        private const val KEY_SELECTED = "selected_calendar_ids"
        private const val KEY_KEYWORD = "keyword"
        private const val KEY_ALL_DAY = "include_all_day"

        @Volatile private var instance: CalendarSettings? = null

        fun get(context: Context): CalendarSettings =
            instance ?: synchronized(this) {
                instance ?: CalendarSettings(context.applicationContext).also { instance = it }
            }
    }
}
