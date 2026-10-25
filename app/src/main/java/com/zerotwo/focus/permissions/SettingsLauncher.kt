package com.zerotwo.focus.permissions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Opens the Settings screen where the user can grant a permission.
 * OEM ROMs don't all support every deep link, so each permission has a chain of
 * fallbacks, ending at the app's own details page and then the Settings root.
 */
object SettingsLauncher {

    /** @return true if some Settings screen was opened. */
    fun open(context: Context, permission: AppPermission): Boolean {
        val candidates = intentsFor(context, permission) + appDetailsIntent(context) + Intent(Settings.ACTION_SETTINGS)
        return candidates.any { tryStart(context, it) }
    }

    /** App info page; on Android 13+ this is where "Allow restricted settings" lives for sideloaded APKs. */
    fun openAppDetails(context: Context): Boolean = tryStart(context, appDetailsIntent(context))

    @SuppressLint("BatteryLife") // Direct request is fine for a sideloaded hackathon build.
    private fun intentsFor(context: Context, permission: AppPermission): List<Intent> {
        val pkg = packageUri(context)
        return when (permission) {
            AppPermission.USAGE_ACCESS -> listOf(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, pkg), // jumps straight to our app on some devices
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            )
            AppPermission.OVERLAY -> listOf(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg),
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
            )
            AppPermission.ACCESSIBILITY -> listOf(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            )
            AppPermission.BATTERY_OPTIMIZATION -> listOf(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg),
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
        }
    }

    private fun appDetailsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        // ActivityNotFoundException or SecurityException on locked-down ROMs: try the next fallback.
        false
    }
}
