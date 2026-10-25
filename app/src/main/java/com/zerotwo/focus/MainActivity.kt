package com.zerotwo.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zerotwo.focus.blocker.BlockerService
import com.zerotwo.focus.permissions.PermissionChecker
import com.zerotwo.focus.ui.navigation.AppNavHost
import com.zerotwo.focus.ui.navigation.Routes
import com.zerotwo.focus.ui.theme.ZeroTwoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Bring the blocker back if the OS killed it while blocking was on.
        BlockerService.startIfEnabled(this)
        // Skip onboarding when everything required is already granted (e.g. on relaunch).
        val start = if (PermissionChecker.allRequiredGranted(this)) Routes.HOME else Routes.ONBOARDING
        setContent {
            ZeroTwoTheme {
                AppNavHost(startDestination = start)
            }
        }
    }
}
