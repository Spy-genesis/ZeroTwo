package com.zerotwo.focus.cloud

import android.content.Context
import android.util.Log
import com.zerotwo.focus.erp.StudentProfileStore
import com.zerotwo.focus.screentime.ScreenTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Pushes today's and yesterday's totals to Firestore (yesterday so its final number lands
 * after midnight). Called from Home, the leaderboard, and every 15 min by BlockerService.
 */
object ScreenTimeSync {

    private const val TAG = "ScreenTimeSync"
    private const val MIN_INTERVAL_MS = 15 * 60_000L

    private val mutex = Mutex()
    @Volatile private var lastSuccessAt = 0L

    sealed interface Result {
        data object Uploaded : Result
        data object Skipped : Result
        data class Failed(val message: String) : Result
    }

    /** Never throws. [force] ignores the 15-minute throttle. */
    suspend fun sync(context: Context, force: Boolean = false): Result = withContext(Dispatchers.IO) {
        mutex.withLock {
            val app = context.applicationContext
            val profile = StudentProfileStore.get(app).profile.value ?: return@withLock Result.Skipped
            if (!FirebaseGateway.isConfigured(app)) return@withLock Result.Failed("Firebase isn't set up in this build.")
            if (!force && System.currentTimeMillis() - lastSuccessAt < MIN_INTERVAL_MS) return@withLock Result.Skipped

            val days = ScreenTimeRepository(app).lastDays(days = 2)
                ?: return@withLock Result.Failed("Usage access is off, so there's no screen time to upload.")
            try {
                for (day in days) {
                    FirebaseGateway.uploadDay(
                        profile,
                        day.date,
                        totalMillis = day.totalMillis,
                        socialMillis = day.apps.filter { it.isSocial }.sumOf { it.millis },
                    )
                }
                lastSuccessAt = System.currentTimeMillis()
                Result.Uploaded
            } catch (e: Exception) {
                Log.w(TAG, "Upload failed", e)
                Result.Failed(friendlyMessage(e))
            }
        }
    }

    fun friendlyMessage(e: Throwable): String = when {
        e is kotlinx.coroutines.TimeoutCancellationException -> "No connection to the server. Will retry."
        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
            "Server refused the upload. Check the Firestore rules."
        e.message?.contains("ADMIN_ONLY_OPERATION") == true || e.message?.contains("CONFIGURATION_NOT_FOUND") == true ->
            "Anonymous sign-in is disabled in the Firebase console."
        else -> "Couldn't reach the server: ${e.message ?: e::class.java.simpleName}"
    }
}
