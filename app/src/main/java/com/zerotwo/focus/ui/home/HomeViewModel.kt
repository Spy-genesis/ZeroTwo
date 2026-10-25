package com.zerotwo.focus.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zerotwo.focus.blocker.BlockerService
import com.zerotwo.focus.blocker.BlockerSettings
import com.zerotwo.focus.blocker.BlockerState
import com.zerotwo.focus.permissions.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = BlockerSettings.get(application)

    val blockerState: StateFlow<BlockerState> = settings.state

    private val _permissionsOk = MutableStateFlow(true)
    val permissionsOk: StateFlow<Boolean> = _permissionsOk.asStateFlow()

    /** Called on resume: permissions can be revoked from Settings at any time. */
    fun refresh() {
        _permissionsOk.value = PermissionChecker.allRequiredGranted(getApplication())
    }

    fun setBlockingEnabled(enabled: Boolean) {
        settings.setBlockingEnabled(enabled)
        if (enabled) BlockerService.startIfEnabled(getApplication()) else BlockerService.stop(getApplication())
    }
}
