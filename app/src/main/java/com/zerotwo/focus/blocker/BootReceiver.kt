package com.zerotwo.focus.blocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zerotwo.focus.permissions.PermissionChecker

/** Restarts the blocker after a reboot or an app update, so it doesn't wait for the app to be opened. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        // Before onboarding is finished there's nothing useful to run.
        if (!PermissionChecker.allRequiredGranted(context)) return
        BlockerService.start(context)
    }
}
