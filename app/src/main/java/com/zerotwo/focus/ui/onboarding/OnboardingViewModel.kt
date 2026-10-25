package com.zerotwo.focus.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zerotwo.focus.permissions.AppPermission
import com.zerotwo.focus.permissions.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionStatus(val permission: AppPermission, val granted: Boolean)

data class OnboardingUiState(val statuses: List<PermissionStatus> = emptyList()) {
    val requiredGranted: Boolean get() = statuses.filter { it.permission.required }.all { it.granted }
    val grantedCount: Int get() = statuses.count { it.granted }
}

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Called on every ON_RESUME, i.e. whenever the user comes back from Settings. */
    fun refresh() {
        val app = getApplication<Application>()
        _uiState.value = OnboardingUiState(
            AppPermission.entries.map { PermissionStatus(it, PermissionChecker.isGranted(app, it)) },
        )
    }
}
