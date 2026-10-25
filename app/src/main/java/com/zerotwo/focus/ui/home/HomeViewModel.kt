package com.zerotwo.focus.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.blocker.BlockPolicy
import com.zerotwo.focus.blocker.BlockStatus
import com.zerotwo.focus.blocker.BlockerService
import com.zerotwo.focus.blocker.BlockerSettings
import com.zerotwo.focus.blocker.BlockerState
import com.zerotwo.focus.permissions.PermissionChecker
import com.zerotwo.focus.timetable.ClassSession
import com.zerotwo.focus.timetable.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime

data class HomeUiState(
    val status: BlockStatus,
    val blocker: BlockerState,
    val todaysClasses: List<ClassSession>,
    val now: LocalTime,
    val permissionsOk: Boolean,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = BlockerSettings.get(application)
    private val timetable = TimetableRepository.get(application)
    private val permissionsOk = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> =
        combine(BlockPolicy.statusFlow(application), settings.state, permissionsOk) { status, blocker, permsOk ->
            buildState(status, blocker, permsOk)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            buildState(BlockPolicy.evaluate(application), settings.state.value, true),
        )

    private fun buildState(status: BlockStatus, blocker: BlockerState, permsOk: Boolean): HomeUiState {
        val demo = (status as? BlockStatus.InClass)?.takeIf { it.isDemo }?.session
        val today = (timetable.sessionsOn(LocalDate.now().dayOfWeek) + listOfNotNull(demo)).sortedBy { it.start }
        return HomeUiState(status, blocker, today, LocalTime.now(), permsOk)
    }

    /** Called on resume: permissions can be revoked from Settings at any time. */
    fun refresh() {
        permissionsOk.value = PermissionChecker.allRequiredGranted(getApplication())
    }

    fun ensureServiceRunning() = BlockerService.start(getApplication())

    fun setManualBlockingEnabled(enabled: Boolean) = settings.setManualBlockingEnabled(enabled)

    /** @return the new demo state. */
    fun toggleDemo(): Boolean {
        val active = !settings.state.value.isDemoActive(System.currentTimeMillis())
        settings.setDemoActive(active)
        return active
    }
}
