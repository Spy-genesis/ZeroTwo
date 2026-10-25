package com.zerotwo.focus.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.blocker.BlockerSettings
import com.zerotwo.focus.cloud.FirebaseGateway
import com.zerotwo.focus.cloud.LeaderboardEntry
import com.zerotwo.focus.cloud.ScreenTimeSync
import com.zerotwo.focus.config.DemoConfig
import com.zerotwo.focus.erp.StudentProfile
import com.zerotwo.focus.erp.StudentProfileStore
import com.zerotwo.focus.screentime.ScreenTimeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

enum class LeaderboardDay(val label: String, val daysAgo: Long) {
    TODAY("Today", 0),
    YESTERDAY("Yesterday", 1),
}

sealed interface LeaderboardContent {
    data object Loading : LeaderboardContent
    data object NotConfigured : LeaderboardContent
    data class Error(val message: String) : LeaderboardContent
    data class Loaded(
        val entries: List<LeaderboardEntry>,
        /** Something to tell the user without hiding the list (upload failed, offline demo, …). */
        val notice: String?,
        val isDemo: Boolean,
    ) : LeaderboardContent
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
        val profile = profiles.profile.value ?: return
        loadJob?.cancel()
        _uiState.update { it.copy(profile = profile, content = LeaderboardContent.Loading) }
        loadJob = viewModelScope.launch {
            val content = load(profile, _uiState.value.day)
            _uiState.update { it.copy(content = content) }
        }
    }

    private suspend fun load(profile: StudentProfile, day: LeaderboardDay): LeaderboardContent {
        val app = getApplication<Application>()
        val date = LocalDate.now().minusDays(day.daysAgo)
        val demo = BlockerSettings.get(app).state.value.isDemoActive(System.currentTimeMillis())

        if (!FirebaseGateway.isConfigured(app)) {
            return if (demo) demoBoard(profile, date, day, remote = null, notice = "Offline demo: Firebase isn't set up in this build.")
            else LeaderboardContent.NotConfigured
        }

        // Upload first so you always see yourself with up-to-date numbers.
        val upload = ScreenTimeSync.sync(app, force = true)
        val uploadNotice = (upload as? ScreenTimeSync.Result.Failed)?.let { "Your latest numbers weren't uploaded: ${it.message}" }
        return try {
            val remote = FirebaseGateway.leaderboard(profile, date)
            if (demo) demoBoard(profile, date, day, remote, uploadNotice)
            else LeaderboardContent.Loaded(remote, uploadNotice, isDemo = false)
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            val message = ScreenTimeSync.friendlyMessage(e)
            if (demo) demoBoard(profile, date, day, remote = null, notice = "Offline demo: $message")
            else LeaderboardContent.Error(message)
        }
    }

    /** Real entries (or just you, from on-device data) plus the sample classmates from DemoConfig. */
    private suspend fun demoBoard(
        profile: StudentProfile,
        date: LocalDate,
        day: LeaderboardDay,
        remote: List<LeaderboardEntry>?,
        notice: String?,
    ): LeaderboardContent {
        val real = remote ?: listOfNotNull(localEntry(profile, date))
        val dayFraction = if (day == LeaderboardDay.TODAY) {
            (LocalTime.now().toSecondOfDay() / (22 * 3600.0)).coerceIn(0.15, 1.0)
        } else {
            1.0
        }
        val taken = real.map { it.studentId }.toSet()
        val classmates = DemoConfig.DEMO_CLASSMATES.filter { it.studentId !in taken }.map {
            val total = (it.fullDayMinutes * dayFraction * 60_000).toLong()
            LeaderboardEntry(it.studentId, total, (total * it.socialShare).toLong(), isDemo = true)
        }
        val entries = (real + classmates).sortedWith(compareBy({ it.totalMillis }, { it.studentId }))
        return LeaderboardContent.Loaded(entries, notice, isDemo = true)
    }

    private suspend fun localEntry(profile: StudentProfile, date: LocalDate): LeaderboardEntry? =
        withContext(Dispatchers.IO) {
            val day = ScreenTimeRepository(getApplication()).lastDays(days = 2)?.firstOrNull { it.date == date }
                ?: return@withContext null
            LeaderboardEntry(profile.studentId, day.totalMillis, day.apps.filter { it.isSocial }.sumOf { it.millis })
        }
}
