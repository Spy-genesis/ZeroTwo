package com.zerotwo.focus.blocker

import android.content.Context
import com.zerotwo.focus.config.TimetableConfig
import com.zerotwo.focus.timetable.ClassSession
import com.zerotwo.focus.timetable.TimetableRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

sealed interface BlockStatus {
    val isBlocking: Boolean

    /** A class is in session: blocking is forced on regardless of the manual switch. */
    data class InClass(val session: ClassSession, val isDemo: Boolean) : BlockStatus {
        override val isBlocking get() = true
    }

    /** No class, but the user switched blocking on. */
    data object Manual : BlockStatus {
        override val isBlocking get() = true
    }

    data class Off(val nextClassToday: ClassSession?) : BlockStatus {
        override val isBlocking get() = false
    }
}

/** The single rule for "should social apps be blocked right now, and why". */
object BlockPolicy {

    fun evaluate(context: Context, nowMillis: Long = System.currentTimeMillis()): BlockStatus {
        val state = BlockerSettings.get(context).state.value
        val timetable = TimetableRepository.get(context)
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())

        if (state.isDemoActive(nowMillis)) {
            val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(state.demoStartedAtMillis), ZoneId.systemDefault())
            val session = ClassSession(
                day = start.dayOfWeek,
                start = start.toLocalTime(),
                end = start.toLocalTime().plusMinutes(TimetableConfig.DEMO_DURATION_MINUTES),
                course = TimetableConfig.DEMO_COURSE_NAME,
            )
            return BlockStatus.InClass(session, isDemo = true)
        }

        timetable.currentSession(now.dayOfWeek, now.toLocalTime())?.let {
            return BlockStatus.InClass(it, isDemo = false)
        }
        if (state.manualBlockingEnabled) return BlockStatus.Manual
        return BlockStatus.Off(timetable.nextSession(now.dayOfWeek, now.toLocalTime()))
    }

    /** Re-evaluates whenever settings change and every [tickMillis], so class start/end is picked up. */
    fun statusFlow(context: Context, tickMillis: Long = 15_000L): Flow<BlockStatus> {
        val ticker = flow {
            while (true) {
                emit(Unit)
                delay(tickMillis)
            }
        }
        return combine(BlockerSettings.get(context).state, ticker) { _, _ -> evaluate(context) }
    }
}
