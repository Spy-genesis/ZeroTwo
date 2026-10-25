package com.zerotwo.focus.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.cloud.FirebaseGateway
import com.zerotwo.focus.cloud.LeaderboardEntry
import com.zerotwo.focus.cloud.ScreenTimeSync
import com.zerotwo.focus.erp.StudentProfile
import com.zerotwo.focus.erp.StudentProfileStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class LeaderboardDay(val label: String, val daysAgo: Long) {
    TODAY("Today", 0),
    YESTERDAY("Yesterday", 1),
}

sealed interface LeaderboardContent {
    data object Loading : LeaderboardContent
    data object NotConfigured : LeaderboardContent
    data class Error(val message: String) : LeaderboardContent
    data class Loaded(val entries: List<LeaderboardEntry>, val uploadWarning: String?) : LeaderboardContent
}

data class LeaderboardUiState(
    val profile: StudentProfile?,
    val day: LeaderboardDay = LeaderboardDay.TODAY,
    val content: LeaderboardContent = LeaderboardContent.Loading,
)

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val profiles = StudentProfileStore.get(application)

    private val _uiState = MutableStateFlow(LeaderboardUiState(profiles.profile.value))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun selectDay(day: LeaderboardDay) {
        if (day == _uiState.value.day) return
        _uiState.update { it.copy(day = day) }
        refresh()
    }

    fun refresh() {
        val app = getApplication<Application>()
        val profile = profiles.profile.value ?: return
        loadJob?.cancel()
        _uiState.update { it.copy(profile = profile, content = LeaderboardContent.Loading) }
        loadJob = viewModelScope.launch {
            if (!FirebaseGateway.isConfigured(app)) {
                _uiState.update { it.copy(content = LeaderboardContent.NotConfigured) }
                return@launch
            }
            // Upload first so you always see yourself with up-to-date numbers.
            val upload = ScreenTimeSync.sync(app, force = true)
            val content = try {
                val date = LocalDate.now().minusDays(_uiState.value.day.daysAgo)
                LeaderboardContent.Loaded(
                    FirebaseGateway.leaderboard(profile, date),
                    uploadWarning = (upload as? ScreenTimeSync.Result.Failed)?.message,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                LeaderboardContent.Error(ScreenTimeSync.friendlyMessage(e))
            }
            _uiState.update { it.copy(content = content) }
        }
    }

}
