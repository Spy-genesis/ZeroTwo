package com.zerotwo.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zerotwo.focus.erp.StudentProfileStore
import com.zerotwo.focus.permissions.PermissionChecker
import com.zerotwo.focus.ui.navigation.AppNavHost
import com.zerotwo.focus.ui.navigation.Routes
import com.zerotwo.focus.ui.theme.ZeroTwoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Skip onboarding when everything required is already granted, and sign-in when already signed in.
        val start = when {
            !PermissionChecker.allRequiredGranted(this) -> Routes.ONBOARDING
            StudentProfileStore.get(this).profile.value == null -> Routes.ERP_LOGIN
            else -> Routes.HOME
        }
        setContent {
            ZeroTwoTheme {
                AppNavHost(startDestination = start)
            }
        }
    }
}
