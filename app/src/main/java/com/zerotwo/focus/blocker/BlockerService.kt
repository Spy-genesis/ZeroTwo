package com.zerotwo.focus.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.zerotwo.focus.MainActivity
import com.zerotwo.focus.R
import com.zerotwo.focus.calendar.CalendarMonitor
import com.zerotwo.focus.cloud.ScreenTimeSync
import com.zerotwo.focus.timetable.format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps ZeroTwo alive with a persistent notification (so OEM task killers leave it alone),
 * tracks class start/end, and polls UsageStats as a backup to the accessibility service.
 */
class BlockerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    /** Calendar edits (new event, moved lecture) apply within seconds instead of waiting for the next 60 s refresh. */
    private val calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scope.launch(Dispatchers.IO) { CalendarMonitor.refresh(this@BlockerService) }
        }
    }
    @Volatile private var observingCalendar = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!goForeground(BlockPolicy.evaluate(this))) return START_NOT_STICKY
        if (loopJob == null) loopJob = scope.launch { runLoop() }
        return START_STICKY
    }

    override fun onDestroy() {
        if (observingCalendar) contentResolver.unregisterContentObserver(calendarObserver)
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Every second: re-evaluate the policy (classes start and end on their own),
     * refresh the notification if the status changed, and run the UsageStats backup detector.
     */
    private suspend fun runLoop() {
        val detector = ForegroundAppDetector(this)
        var shownText: Pair<String, String>? = null
        var ticks = 0L
        while (scope.isActive) {
            if (ticks++ % 60 == 0L) {
                // ScreenTimeSync throttles itself to one upload per 15 min; checking once a minute is plenty.
                scope.launch { ScreenTimeSync.sync(this@BlockerService) }
                scope.launch(Dispatchers.IO) { refreshCalendar() }
            }

            val status = BlockPolicy.evaluate(this)
            val text = notificationText(status)
            if (text != shownText) {
                notificationManager().notify(NOTIFICATION_ID, buildNotification(status))
                shownText = text
            }
            // Poll even when not blocking, so a class starting while Instagram is open still gets caught.
            val foreground = detector.currentForegroundApp()
            if (status.isBlocking && foreground != null) AppBlocker.onForegroundApp(this, foreground)
            delay(LOOP_INTERVAL_MS)
        }
    }

    /** Re-reads calendars and (once permission exists) starts watching for changes. */
    @Synchronized
    private fun refreshCalendar() {
        val snapshot = CalendarMonitor.refresh(this)
        if (snapshot.hasPermission && !observingCalendar) {
            try {
                contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, calendarObserver)
                observingCalendar = true
            } catch (e: SecurityException) {
                Log.w(TAG, "Can't observe calendar", e)
            }
        }
    }

    private fun goForeground(status: BlockStatus): Boolean = try {
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(status),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        true
    } catch (e: Exception) {
        // e.g. ForegroundServiceStartNotAllowedException when started from the background.
        Log.w(TAG, "Could not enter foreground", e)
        stopSelf()
        false
    }

    private fun notificationText(status: BlockStatus): Pair<String, String> {
        val count = BlockerSettings.get(this).state.value.selectedApps.size
        val apps = "$count app${if (count == 1) "" else "s"}"
        return when (status) {
            is BlockStatus.InClass ->
                "In ${status.session.course}" to "$apps blocked until ${status.session.end.format(this)}"
            is BlockStatus.InEvent -> "In ${status.event.title}" to "$apps blocked until ${status.end.format(this)}"
            BlockStatus.Manual -> "Blocking is on" to "$apps blocked. Tap to manage."
            is BlockStatus.Off -> "Not blocking" to (
                status.next?.let { "Next: ${it.title} at ${it.start.format(this)}" }
                    ?: "No more classes today"
                )
        }
    }

    private fun buildNotification(status: BlockStatus): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val (title, text) = notificationText(status)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_block)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Blocker status", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shown while ZeroTwo is blocking apps"
            setShowBadge(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)

    companion object {
        private const val TAG = "BlockerService"
        private const val CHANNEL_ID = "blocker_status"
        private const val NOTIFICATION_ID = 1
        private const val LOOP_INTERVAL_MS = 1000L

        /**
         * Starts the always-on scheduler. It runs even with the manual switch off, because a class
         * can start at any moment. Safe to call repeatedly; never throws.
         */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, BlockerService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not start blocker service", e)
            }
        }
    }
}
