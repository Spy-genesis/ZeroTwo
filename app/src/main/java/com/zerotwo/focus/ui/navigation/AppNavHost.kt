package com.zerotwo.focus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zerotwo.focus.ui.blockedapps.BlockedAppsScreen
import com.zerotwo.focus.ui.home.HomeScreen
import com.zerotwo.focus.ui.onboarding.OnboardingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val BLOCKED_APPS = "blocked_apps"
}

@Composable
fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenBlockedApps = { navController.navigate(Routes.BLOCKED_APPS) },
                onReviewPermissions = { navController.navigate(Routes.ONBOARDING) },
            )
        }
        composable(Routes.BLOCKED_APPS) {
            BlockedAppsScreen(onBack = { navController.popBackStack() })
        }
    }
}
