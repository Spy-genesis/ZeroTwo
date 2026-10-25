package com.zerotwo.focus.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.blocker.BlockPolicy
import com.zerotwo.focus.blocker.BlockStatus
import com.zerotwo.focus.blocker.BlockerService
import com.zerotwo.focus.blocker.BlockerSettings
import com.zerotwo.focus.blocker.BlockerState
import com.zerotwo.focus.calendar.CalendarMonitor
import com.zerotwo.focus.calendar.CalendarRules
import com.zerotwo.focus.calendar.CalendarSettings
import com.zerotwo.focus.cloud.ScreenTimeSync
import com.zerotwo.focus.erp.StudentProfile
import com.zerotwo.focus.erp.StudentProfileStore
import com.zerotwo.focus.permissions.PermissionChecker
import com.zerotwo.focus.screentime.ScreenTimeRepository
import com.zerotwo.focus.timetable.ClassSession
import com.zerotwo.focus.timetable.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** One row of today's schedule, from either the calendar or the timetable. */
data class ScheduleItem(
    val key: String,
    val title: String,
    val start: LocalTime,
    val end: LocalTime,
    val allDay: Boolean,
    val isNow: Boolean,
    val isPast: Boolean,
)

enum class ScheduleSource { CALENDAR, TIMETABLE }

/** Why the calendar isn't driving blocking yet, so Home can nudge the user. */
enum class CalendarHint { NO_PERMISSION, NONE_SELECTED }

data class HomeUiState(
    val status: BlockStatus,
    val blocker: BlockerState,
    val schedule: List<ScheduleItem>,
    val scheduleSource: ScheduleSource,
    val calendarHint: CalendarHint?,
    val permissionsOk: Boolean,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = BlockerSettings.get(application)
    private val timetable = TimetableRepository.get(application)
    private val permissionsOk = MutableStateFlow(true)
    private val profiles = StudentProfileStore.get(application)

    val profile: StateFlow<StudentProfile?> = profiles.profile

    /** Today's total, or null while loading / without usage access. */
    private val _todayMillis = MutableStateFlow<Long?>(null)
    val todayMillis: StateFlow<Long?> = _todayMillis.asStateFlow()

    val uiState: StateFlow<HomeUiState> =
        combine(BlockPolicy.statusFlow(application), settings.state, permissionsOk) { status, blocker, permsOk ->
            buildState(status, blocker, permsOk)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            buildState(BlockPolicy.evaluate(application), settings.state.value, true),
        )

    private fun buildState(status: BlockStatus, blocker: BlockerState, permsOk: Boolean): HomeUiState {
        val nowMillis = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val now = LocalTime.now(zone)
        val snapshot = CalendarMonitor.snapshot.value
        val filter = CalendarSettings.get(getApplication()).filter.value
        val useCalendar = CalendarRules.isCalendarActive(snapshot, filter)
        val demo = (status as? BlockStatus.InClass)?.takeIf { it.isDemo }?.session

        val fromSource = if (useCalendar) {
            CalendarRules.eventsOn(LocalDate.now(zone), snapshot.events, filter, zone).map {
                ScheduleItem(
                    key = "e${it.eventId}-${it.beginMillis}",
                    title = it.title,
                    start = Instant.ofEpochMilli(it.beginMillis).atZone(zone).toLocalTime(),
                    end = Instant.ofEpochMilli(it.endMillis).atZone(zone).toLocalTime(),
                    allDay = it.allDay,
                    isNow = CalendarRules.isActive(it, nowMillis),
                    isPast = it.endMillis <= nowMillis,
                )
            }
        } else {
            timetable.sessionsOn(LocalDate.now(zone).dayOfWeek).map { it.toItem(now) }
        }
        // The demo class shows in either list, so the demo story works with or without a calendar.
        val schedule = (fromSource + listOfNotNull(demo?.toItem(now))).sortedBy { it.start }

        val hint = when {
            useCalendar -> null
            !snapshot.hasPermission -> CalendarHint.NO_PERMISSION
            else -> CalendarHint.NONE_SELECTED
        }
        return HomeUiState(
            status, blocker, schedule,
            if (useCalendar) ScheduleSource.CALENDAR else ScheduleSource.TIMETABLE,
            hint, permsOk,
        )
    }

    private fun ClassSession.toItem(now: LocalTime) = ScheduleItem(
        key = "t$start-$course",
        title = course,
        start = start,
        end = end,
        allDay = false,
        isNow = isInSession(now),
        isPast = isOver(now),
    )

    /** Called on resume: permissions can be revoked from Settings at any time. */
    fun refresh() {
        permissionsOk.value = PermissionChecker.allRequiredGranted(getApplication())
        viewModelScope.launch(Dispatchers.IO) { CalendarMonitor.refresh(getApplication()) }
        viewModelScope.launch {
            _todayMillis.value = withContext(Dispatchers.IO) {
                runCatching { ScreenTimeRepository(getApplication()).lastDays(days = 1)?.last()?.totalMillis }.getOrNull()
            }
        }
    }

    fun ensureServiceRunning() = BlockerService.start(getApplication())

    /** Throttled; the leaderboard forces a fresh upload when opened. */
    fun syncScreenTime() {
        viewModelScope.launch { ScreenTimeSync.sync(getApplication()) }
    }

    fun signOut() = profiles.clear()

    fun setManualBlockingEnabled(enabled: Boolean) = settings.setManualBlockingEnabled(enabled)

    /** @return the new demo state. */
    fun toggleDemo(): Boolean {
        val active = !settings.state.value.isDemoActive(System.currentTimeMillis())
        settings.setDemoActive(active)
        return active
    }
}
