package com.zerotwo.focus.blocker

import android.content.Context
import com.zerotwo.focus.config.BlockableApp
import com.zerotwo.focus.config.BlockedAppsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockerState(
    val blockingEnabled: Boolean,
    /** IDs of config apps the user switched off. Storing the opt-outs means new config apps start blocked. */
    val disabledAppIds: Set<String>,
) {
    fun isAppSelected(app: BlockableApp) = app.id !in disabledAppIds

    val selectedApps: List<BlockableApp>
        get() = BlockedAppsConfig.DEFAULT_BLOCKED_APPS.filter(::isAppSelected)
}

/**
 * Process-wide blocker settings backed by SharedPreferences.
 * The UI observes [state]; the services read [state].value on every check.
 */
class BlockerSettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("blocker", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        BlockerState(
            blockingEnabled = prefs.getBoolean(KEY_ENABLED, false),
            disabledAppIds = prefs.getStringSet(KEY_DISABLED, emptySet())!!.toSet(),
        ),
    )
    val state: StateFlow<BlockerState> = _state.asStateFlow()

    fun setBlockingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = _state.value.copy(blockingEnabled = enabled)
    }

    fun setAppSelected(app: BlockableApp, selected: Boolean) {
        val disabled = _state.value.disabledAppIds.let { if (selected) it - app.id else it + app.id }
        prefs.edit().putStringSet(KEY_DISABLED, disabled).apply()
        _state.value = _state.value.copy(disabledAppIds = disabled)
    }

    /** The app to block for [packageName], or null if it shouldn't be blocked right now. */
    fun blockedAppFor(packageName: String): BlockableApp? {
        val current = _state.value
        if (!current.blockingEnabled) return null
        return current.selectedApps.firstOrNull { packageName in it.packages }
    }

    companion object {
        private const val KEY_ENABLED = "blocking_enabled"
        private const val KEY_DISABLED = "disabled_app_ids"

        @Volatile private var instance: BlockerSettings? = null

        fun get(context: Context): BlockerSettings =
            instance ?: synchronized(this) {
                instance ?: BlockerSettings(context.applicationContext).also { instance = it }
            }
    }
}
