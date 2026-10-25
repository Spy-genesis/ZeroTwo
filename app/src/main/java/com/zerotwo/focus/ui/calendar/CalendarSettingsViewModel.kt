package com.zerotwo.focus.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.calendar.CalendarEvent
import com.zerotwo.focus.calendar.CalendarFilter
import com.zerotwo.focus.calendar.CalendarMonitor
import com.zerotwo.focus.calendar.CalendarRules
import com.zerotwo.focus.calendar.CalendarSettings
import com.zerotwo.focus.calendar.DeviceCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class CalendarSettingsUiState(
    val loaded: Boolean,
    val hasPermission: Boolean,
    val calendars: List<DeviceCalendar>,
    val filter: CalendarFilter,
    /** Today's events that would block with the current choices: a live preview of the settings. */
    val matchingToday: List<CalendarEvent>,
)

class CalendarSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = CalendarSettings.get(application)

    val uiState: StateFlow<CalendarSettingsUiState> =
        combine(CalendarMonitor.snapshot, settings.filter) { snapshot, filter ->
            CalendarSettingsUiState(
                loaded = snapshot.fetchedAtMillis > 0 || !snapshot.hasPermission,
                hasPermission = snapshot.hasPermission,
                calendars = snapshot.calendars,
                filter = filter,
                matchingToday = CalendarRules.eventsOn(LocalDate.now(), snapshot.events, filter, ZoneId.systemDefault()),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CalendarSettingsUiState(false, false, emptyList(), settings.filter.value, emptyList()),
        )

    /** On resume: the permission or the calendars may have changed in another app. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) { CalendarMonitor.refresh(getApplication()) }
    }

    fun setCalendarSelected(id: Long, selected: Boolean) = settings.setCalendarSelected(id, selected)
    fun setKeyword(keyword: String) = settings.setKeyword(keyword)
    fun setIncludeAllDay(include: Boolean) = settings.setIncludeAllDay(include)
}
