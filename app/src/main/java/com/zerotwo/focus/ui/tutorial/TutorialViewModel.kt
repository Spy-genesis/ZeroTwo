package com.zerotwo.focus.ui.tutorial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.blocker.BlockedActivity
import com.zerotwo.focus.blocker.BlockerSettings
import com.zerotwo.focus.calendar.CalendarMonitor
import com.zerotwo.focus.calendar.CalendarSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TutorialStep { WELCOME, DEMO, CALENDAR, CONTROLS, PROGRESS, DONE }

data class TutorialUiState(
    val step: TutorialStep = TutorialStep.WELCOME,
    val demoRunning: Boolean = false,
    /** The block screen appeared after the demo started: the user has seen ZeroTwo work. */
    val sawBlockScreen: Boolean = false,
    val calendarAccess: Boolean = false,
    val calendarsSelected: Int = 0,
)

class TutorialViewModel(application: Application) : AndroidViewModel(application) {

    private val blocker = BlockerSettings.get(application)
    private val step = MutableStateFlow(TutorialStep.WELCOME)

    val uiState: StateFlow<TutorialUiState> = combine(
        step,
        blocker.state,
        BlockedActivity.lastShownAtMillis,
        CalendarMonitor.snapshot,
        CalendarSettings.get(application).filter,
    ) { step, blockerState, lastBlockShown, calendar, filter ->
        val demoRunning = blockerState.isDemoActive(System.currentTimeMillis())
        TutorialUiState(
            step = step,
            demoRunning = demoRunning,
            sawBlockScreen = demoRunning && lastBlockShown >= blockerState.demoStartedAtMillis,
            calendarAccess = calendar.hasPermission,
            calendarsSelected = calendar.calendars.count { it.id in filter.selectedCalendarIds },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TutorialUiState())

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) { CalendarMonitor.refresh(getApplication()) }
    }

    fun startDemo() = blocker.setDemoActive(true)

    fun next() {
        // Leaving the demo step ends the demo, so apps aren't still blocked when the user starts for real.
        if (step.value == TutorialStep.DEMO && uiState.value.demoRunning) blocker.setDemoActive(false)
        step.update { current -> TutorialStep.entries.getOrElse(current.ordinal + 1) { current } }
    }

    fun back() = step.update { current -> TutorialStep.entries.getOrElse(current.ordinal - 1) { current } }

    fun finish() = TutorialProgress.markSeen(getApplication())
}
