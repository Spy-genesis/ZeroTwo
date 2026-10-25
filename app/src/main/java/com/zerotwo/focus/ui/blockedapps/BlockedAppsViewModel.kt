package com.zerotwo.focus.ui.blockedapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.blocker.BlockPolicy
import com.zerotwo.focus.blocker.BlockStatus
import com.zerotwo.focus.blocker.BlockerSettings
import com.zerotwo.focus.blocker.BlockerState
import com.zerotwo.focus.config.BlockableApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BlockedAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = BlockerSettings.get(application)

    val state: StateFlow<BlockerState> = settings.state

    /** During class the list is locked, so students can't un-block an app mid-lecture. */
    val locked: StateFlow<Boolean> = BlockPolicy.statusFlow(application)
        .map { it is BlockStatus.InClass }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockPolicy.evaluate(application) is BlockStatus.InClass)

    fun setAppSelected(app: BlockableApp, selected: Boolean) {
        if (locked.value && !selected) return
        settings.setAppSelected(app, selected)
    }
}
