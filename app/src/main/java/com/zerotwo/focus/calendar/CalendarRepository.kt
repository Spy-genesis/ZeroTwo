package com.zerotwo.focus.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId

/** Reads the device calendar provider. Never throws: a missing permission gives [CalendarSnapshot.NO_PERMISSION]. */
class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun read(nowMillis: Long = System.currentTimeMillis()): CalendarSnapshot {
        if (!hasPermission()) return CalendarSnapshot.NO_PERMISSION
        return try {
            CalendarSnapshot(hasPermission = true, calendars(), instances(), nowMillis)
        } catch (e: SecurityException) {
            CalendarSnapshot.NO_PERMISSION // revoked between the check and the query
        } catch (e: Exception) {
            Log.w(TAG, "Calendar read failed", e)
            CalendarSnapshot(hasPermission = true, emptyList(), emptyList(), nowMillis)
        }
    }

    private fun calendars(): List<DeviceCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val result = mutableListOf<DeviceCalendar>()
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                result += DeviceCalendar(
                    id = c.getLong(0),
                    displayName = c.getString(1) ?: "Calendar",
                    accountName = c.getString(2) ?: "",
                    color = if (c.isNull(3)) null else c.getInt(3),
                )
            }
        }
        return result.sortedWith(compareBy({ it.accountName }, { it.displayName }))
    }

    /**
     * Instances from the start of yesterday to the end of tomorrow: covers "active now" (including events
     * that began yesterday) and "later today". Instances that merely overlap the window are included too.
     */
    private fun instances(): List<CalendarEvent> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val from = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from)
            ContentUris.appendId(it, to)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.STATUS,
        )
        val result = mutableListOf<CalendarEvent>()
        context.contentResolver.query(uri, projection, null, null, CalendarContract.Instances.BEGIN)?.use { c ->
            while (c.moveToNext()) {
                if (!c.isNull(6) && c.getInt(6) == CalendarContract.Events.STATUS_CANCELED) continue
                val allDay = c.getInt(5) == 1
                val (begin, end) = if (allDay) {
                    CalendarRules.normalizeAllDay(c.getLong(3), c.getLong(4), zone)
                } else {
                    c.getLong(3) to c.getLong(4)
                }
                if (end <= begin) continue
                result += CalendarEvent(
                    eventId = c.getLong(0),
                    calendarId = c.getLong(1),
                    title = c.getString(2)?.takeIf { it.isNotBlank() } ?: "(No title)",
                    beginMillis = begin,
                    endMillis = end,
                    allDay = allDay,
                )
            }
        }
        return result
    }

    private companion object {
        const val TAG = "CalendarRepository"
    }
}
