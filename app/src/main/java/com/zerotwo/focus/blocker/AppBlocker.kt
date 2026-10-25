package com.zerotwo.focus.blocker

import android.content.Context
import android.os.SystemClock

/**
 * Single entry point for "this app just came to the foreground".
 * Both detectors (accessibility events and the UsageStats poller) report here,
 * so the debounce stops them from launching the block screen twice.
 */
object AppBlocker {

    private const val DEBOUNCE_MS = 1500L

    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    @Synchronized
    fun onForegroundApp(context: Context, packageName: String) {
        if (packageName == context.packageName) return
        val app = BlockerSettings.get(context).blockedAppFor(packageName) ?: return

        val now = SystemClock.elapsedRealtime()
        if (packageName == lastBlockedPackage && now - lastBlockedAt < DEBOUNCE_MS) return
        lastBlockedPackage = packageName
        lastBlockedAt = now

        BlockedActivity.launch(context, app.label)
    }
}
