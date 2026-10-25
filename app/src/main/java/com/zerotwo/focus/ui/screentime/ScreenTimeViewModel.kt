package com.zerotwo.focus.ui.screentime

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.screentime.DailyUsage
import com.zerotwo.focus.screentime.ScreenTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScreenTimeUiState {
    data object Loading : ScreenTimeUiState
    data object NoPermission : ScreenTimeUiState
    data class Loaded(val days: List<DailyUsage>, val selectedIndex: Int) : ScreenTimeUiState {
        val selected: DailyUsage get() = days[selectedIndex]
        val averageMillis: Long get() = days.sumOf { it.totalMillis } / days.size
    }
}

class ScreenTimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenTimeRepository(application)

    private val _uiState = MutableStateFlow<ScreenTimeUiState>(ScreenTimeUiState.Loading)
    val uiState: StateFlow<ScreenTimeUiState> = _uiState.asStateFlow()

    /** Called on resume, so the numbers are fresh after using other apps. Keeps the selected day. */
    fun refresh() {
        viewModelScope.launch {
            val days = withContext(Dispatchers.IO) { runCatching { repository.lastDays() }.getOrNull() }
            val previous = _uiState.value as? ScreenTimeUiState.Loaded
            _uiState.value = if (days == null) {
                ScreenTimeUiState.NoPermission
            } else {
                ScreenTimeUiState.Loaded(days, previous?.selectedIndex ?: days.lastIndex)
            }
        }
    }

    fun select(index: Int) {
        val current = _uiState.value as? ScreenTimeUiState.Loaded ?: return
        if (index in current.days.indices) _uiState.value = current.copy(selectedIndex = index)
    }
}
