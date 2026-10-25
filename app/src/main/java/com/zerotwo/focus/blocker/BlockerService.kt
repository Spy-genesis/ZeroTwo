package com.zerotwo.focus.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.zerotwo.focus.MainActivity
import com.zerotwo.focus.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps ZeroTwo alive with a persistent notification (so OEM task killers leave it alone)
 * and polls UsageStats as a backup to the accessibility service.
 */
class BlockerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = BlockerSettings.get(this)
        if (!settings.state.value.blockingEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!goForeground(settings.state.value)) return START_NOT_STICKY

        if (pollJob == null) {
            pollJob = scope.launch { pollForegroundApp() }
            // Keep the notification text in sync with the blocked-app toggles.
            scope.launch {
                settings.state.collectLatest { state ->
                    if (state.blockingEnabled) notificationManager().notify(NOTIFICATION_ID, buildNotification(state))
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun pollForegroundApp() {
        val detector = ForegroundAppDetector(this)
        while (scope.isActive) {
            detector.currentForegroundApp()?.let { AppBlocker.onForegroundApp(this, it) }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun goForeground(state: BlockerState): Boolean = try {
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(state),
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

    private fun buildNotification(state: BlockerState): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val count = state.selectedApps.size
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_block)
            .setContentTitle("ZeroTwo is blocking social apps")
            .setContentText("$count app${if (count == 1) "" else "s"} blocked. Tap to manage.")
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
        private const val POLL_INTERVAL_MS = 1000L

        /** Starts the service if blocking is on. Safe to call repeatedly; never throws. */
        fun startIfEnabled(context: Context) {
            if (!BlockerSettings.get(context).state.value.blockingEnabled) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, BlockerService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not start blocker service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BlockerService::class.java))
        }
    }
}
