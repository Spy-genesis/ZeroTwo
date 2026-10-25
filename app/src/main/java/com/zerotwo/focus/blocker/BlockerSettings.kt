package com.zerotwo.focus.blocker

import android.content.Context
import com.zerotwo.focus.config.BlockableApp
import com.zerotwo.focus.config.BlockedAppsConfig
import com.zerotwo.focus.config.TimetableConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockerState(
    /** The user's on/off switch. Only matters outside class hours; classes always block. */
    val manualBlockingEnabled: Boolean,
    /** IDs of config apps the user switched off. Storing the opt-outs means new config apps start blocked. */
    val disabledAppIds: Set<String>,
    /** When the hidden demo class started (epoch ms), or 0 if demo mode is off. */
    val demoStartedAtMillis: Long,
) {
    fun isAppSelected(app: BlockableApp) = app.id !in disabledAppIds

    val selectedApps: List<BlockableApp>
        get() = BlockedAppsConfig.DEFAULT_BLOCKED_APPS.filter(::isAppSelected)

    fun isDemoActive(nowMillis: Long): Boolean =
        demoStartedAtMillis > 0 && nowMillis < demoStartedAtMillis + TimetableConfig.DEMO_DURATION_MINUTES * 60_000
}

/**
 * Process-wide blocker settings backed by SharedPreferences.
 * The UI observes [state]; the services read [state].value on every check.
 */
class BlockerSettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("blocker", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        BlockerState(
            manualBlockingEnabled = prefs.getBoolean(KEY_MANUAL, false),
            disabledAppIds = prefs.getStringSet(KEY_DISABLED, emptySet())!!.toSet(),
            demoStartedAtMillis = prefs.getLong(KEY_DEMO_STARTED, 0L),
        ),
    )
    val state: StateFlow<BlockerState> = _state.asStateFlow()

    fun setManualBlockingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MANUAL, enabled).apply()
        _state.value = _state.value.copy(manualBlockingEnabled = enabled)
    }

    fun setAppSelected(app: BlockableApp, selected: Boolean) {
        val disabled = _state.value.disabledAppIds.let { if (selected) it - app.id else it + app.id }
        prefs.edit().putStringSet(KEY_DISABLED, disabled).apply()
        _state.value = _state.value.copy(disabledAppIds = disabled)
    }

    fun setDemoActive(active: Boolean) {
        val startedAt = if (active) System.currentTimeMillis() else 0L
        prefs.edit().putLong(KEY_DEMO_STARTED, startedAt).apply()
        _state.value = _state.value.copy(demoStartedAtMillis = startedAt)
    }

    companion object {
        private const val KEY_MANUAL = "blocking_enabled" // Phase 2 key name, kept so the setting survives updates.
        private const val KEY_DISABLED = "disabled_app_ids"
        private const val KEY_DEMO_STARTED = "demo_started_at"

        @Volatile private var instance: BlockerSettings? = null

        fun get(context: Context): BlockerSettings =
            instance ?: synchronized(this) {
                instance ?: BlockerSettings(context.applicationContext).also { instance = it }
            }
    }
}
