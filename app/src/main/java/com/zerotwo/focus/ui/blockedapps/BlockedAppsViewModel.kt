package com.zerotwo.focus.ui.blockedapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zerotwo.focus.blocker.BlockerSettings
import com.zerotwo.focus.blocker.BlockerState
import com.zerotwo.focus.config.BlockableApp
import kotlinx.coroutines.flow.StateFlow

class BlockedAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = BlockerSettings.get(application)

    val state: StateFlow<BlockerState> = settings.state

    fun setAppSelected(app: BlockableApp, selected: Boolean) = settings.setAppSelected(app, selected)
}
