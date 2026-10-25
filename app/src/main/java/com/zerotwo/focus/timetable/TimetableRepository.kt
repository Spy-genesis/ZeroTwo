package com.zerotwo.focus.timetable

import android.content.Context
import android.util.Log
import com.zerotwo.focus.config.TimetableConfig
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Reads the mock timetable from assets once per process.
 * Integration-ready, mocked for demo: swap [load] for an ERP API call later.
 * A missing or malformed file yields an empty timetable rather than a crash; bad entries are skipped.
 */
class TimetableRepository private constructor(context: Context) {

    val sessions: List<ClassSession> = load(context)

    fun sessionsOn(day: DayOfWeek): List<ClassSession> = sessions.filter { it.day == day }

    fun currentSession(day: DayOfWeek, time: LocalTime): ClassSession? =
        sessionsOn(day).firstOrNull { it.isInSession(time) }

    fun nextSession(day: DayOfWeek, time: LocalTime): ClassSession? =
        sessionsOn(day).firstOrNull { it.start.isAfter(time) }

    private fun load(context: Context): List<ClassSession> {
        val json = try {
            context.assets.open(TimetableConfig.TIMETABLE_ASSET).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Timetable asset missing", e)
            return emptyList()
        }
        val classes = try {
            JSONObject(json).getJSONArray("classes")
        } catch (e: Exception) {
            Log.w(TAG, "Timetable JSON is malformed", e)
            return emptyList()
        }
        return (0 until classes.length()).mapNotNull { i ->
            try {
                val entry = classes.getJSONObject(i)
                ClassSession(
                    day = DayOfWeek.valueOf(entry.getString("day").trim().uppercase()),
                    start = LocalTime.parse(entry.getString("start")),
                    end = LocalTime.parse(entry.getString("end")),
                    course = entry.getString("course"),
                ).takeIf { it.end.isAfter(it.start) }
                    ?: run { Log.w(TAG, "Skipping class $i: end is not after start"); null }
            } catch (e: Exception) {
                Log.w(TAG, "Skipping malformed class $i", e)
                null
            }
        }.sortedWith(compareBy({ it.day }, { it.start }))
    }

    companion object {
        private const val TAG = "TimetableRepository"

        @Volatile private var instance: TimetableRepository? = null

        fun get(context: Context): TimetableRepository =
            instance ?: synchronized(this) {
                instance ?: TimetableRepository(context.applicationContext).also { instance = it }
            }
    }
}
