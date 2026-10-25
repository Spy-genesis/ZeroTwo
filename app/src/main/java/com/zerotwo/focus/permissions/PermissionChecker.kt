package com.zerotwo.focus.permissions

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import com.zerotwo.focus.blocker.BlockerAccessibilityService

/** Reads the current grant state of each [AppPermission]. Never throws. */
object PermissionChecker {

    fun isGranted(context: Context, permission: AppPermission): Boolean = runCatching {
        when (permission) {
            AppPermission.USAGE_ACCESS -> hasUsageAccess(context)
            AppPermission.OVERLAY -> Settings.canDrawOverlays(context)
            AppPermission.ACCESSIBILITY -> isAccessibilityServiceEnabled(context)
            AppPermission.BATTERY_OPTIMIZATION -> isIgnoringBatteryOptimizations(context)
        }
    }.getOrDefault(false)

    fun allRequiredGranted(context: Context): Boolean =
        AppPermission.entries.filter { it.required }.all { isGranted(context, it) }

    private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        // MODE_DEFAULT means "fall back to the manifest permission", which some OEMs use.
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, BlockerAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }
}
