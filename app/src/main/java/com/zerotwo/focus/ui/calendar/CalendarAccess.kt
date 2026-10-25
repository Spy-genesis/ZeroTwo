package com.zerotwo.focus.ui.calendar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.zerotwo.focus.calendar.CalendarMonitor
import com.zerotwo.focus.permissions.AppPermission
import com.zerotwo.focus.permissions.PermissionChecker
import com.zerotwo.focus.permissions.SettingsLauncher

/**
 * Returns an action that asks for calendar access. If it's already granted, or Android won't show the
 * dialog any more ("don't ask again"), it opens the app's settings page instead. [onResult] gets the new state.
 */
@Composable
fun rememberCalendarAccessRequest(onResult: (granted: Boolean) -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context.findActivity()
        if (!granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALENDAR)
        ) {
            SettingsLauncher.open(context, AppPermission.CALENDAR)
        }
        CalendarMonitor.refresh(context)
        onResult(granted)
    }
    return {
        if (PermissionChecker.isGranted(context, AppPermission.CALENDAR)) {
            SettingsLauncher.open(context, AppPermission.CALENDAR)
        } else {
            launcher.launch(Manifest.permission.READ_CALENDAR)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
