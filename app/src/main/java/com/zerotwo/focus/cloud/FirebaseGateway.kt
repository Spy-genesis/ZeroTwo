package com.zerotwo.focus.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.zerotwo.focus.erp.StudentProfile
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.LocalDate

data class LeaderboardEntry(
    val studentId: String,
    val totalMillis: Long,
    val socialMillis: Long,
)

/**
 * All Firebase access. Auth is anonymous: the student's identity comes from the
 * (mocked) ERP login, Firebase only needs *some* signed-in user to satisfy the rules.
 *
 * Firestore layout — one flat collection so the leaderboard needs no custom index:
 *   dailyScreenTime/{studentId}_{yyyy-MM-dd}
 *     studentId, department, batch, date, totalMillis, socialMillis, uid, updatedAt
 */
object FirebaseGateway {

    private const val COLLECTION = "dailyScreenTime"
    private const val TIMEOUT_MS = 15_000L

    /** False when the build had no google-services.json. */
    fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private suspend fun ensureSignedIn(): String {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        return withTimeout(TIMEOUT_MS) { auth.signInAnonymously().await() }.user?.uid
            ?: error("Anonymous sign-in returned no user")
    }

    suspend fun uploadDay(profile: StudentProfile, date: LocalDate, totalMillis: Long, socialMillis: Long) {
        val uid = ensureSignedIn()
        val day = date.toString()
        val data = mapOf(
            "studentId" to profile.studentId,
            "department" to profile.department,
            "batch" to profile.batch,
            "date" to day,
            "totalMillis" to totalMillis,
            "socialMillis" to socialMillis,
            "uid" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        withTimeout(TIMEOUT_MS) {
            FirebaseFirestore.getInstance().collection(COLLECTION)
                .document("${profile.studentId}_$day")
                .set(data)
                .await()
        }
    }

    /** Everyone in the same department + batch for [date], lowest screen time first. */
    suspend fun leaderboard(profile: StudentProfile, date: LocalDate): List<LeaderboardEntry> {
        ensureSignedIn()
        val snapshot = withTimeout(TIMEOUT_MS) {
            FirebaseFirestore.getInstance().collection(COLLECTION)
                .whereEqualTo("department", profile.department)
                .whereEqualTo("batch", profile.batch)
                .whereEqualTo("date", date.toString())
                .get()
                .await()
        }
        return snapshot.documents.mapNotNull { doc ->
            LeaderboardEntry(
                studentId = doc.getString("studentId") ?: return@mapNotNull null,
                totalMillis = doc.getLong("totalMillis") ?: return@mapNotNull null,
                socialMillis = doc.getLong("socialMillis") ?: 0L,
            )
        }.sortedWith(compareBy({ it.totalMillis }, { it.studentId }))
    }
}
