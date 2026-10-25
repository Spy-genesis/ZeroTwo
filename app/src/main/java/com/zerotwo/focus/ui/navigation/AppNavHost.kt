package com.zerotwo.focus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zerotwo.focus.erp.StudentProfileStore
import com.zerotwo.focus.ui.blockedapps.BlockedAppsScreen
import com.zerotwo.focus.ui.calendar.CalendarSettingsScreen
import com.zerotwo.focus.ui.erp.ErpLoginScreen
import com.zerotwo.focus.ui.home.HomeScreen
import com.zerotwo.focus.ui.leaderboard.LeaderboardScreen
import com.zerotwo.focus.ui.onboarding.OnboardingScreen
import com.zerotwo.focus.ui.screentime.ScreenTimeScreen
import com.zerotwo.focus.ui.tutorial.TutorialProgress
import com.zerotwo.focus.ui.tutorial.TutorialScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val ERP_LOGIN = "erp_login"
    const val HOME = "home"
    const val BLOCKED_APPS = "blocked_apps"
    const val SCREEN_TIME = "screen_time"
    const val LEADERBOARD = "leaderboard"
    const val CALENDAR = "calendar"
    const val TUTORIAL = "tutorial"
}

/** Replaces the whole back stack, so Back can't return to onboarding or sign-in. */
private fun NavHostController.navigateClearingStack(route: String) =
    navigate(route) { popUpTo(graph.id) { inclusive = true } }

@Composable
fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val profiles = StudentProfileStore.get(context)
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    navController.navigateClearingStack(
                        when {
                            profiles.profile.value == null -> Routes.ERP_LOGIN
                            !TutorialProgress.isSeen(context) -> Routes.TUTORIAL
                            else -> Routes.HOME
                        },
                    )
                },
            )
        }
        composable(Routes.ERP_LOGIN) {
            ErpLoginScreen(
                onSignedIn = {
                    navController.navigateClearingStack(
                        if (TutorialProgress.isSeen(context)) Routes.HOME else Routes.TUTORIAL,
                    )
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenBlockedApps = { navController.navigate(Routes.BLOCKED_APPS) },
                onOpenScreenTime = { navController.navigate(Routes.SCREEN_TIME) },
                onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                onOpenTutorial = { navController.navigate(Routes.TUTORIAL) },
                onReviewPermissions = { navController.navigate(Routes.ONBOARDING) },
                onSignedOut = { navController.navigateClearingStack(Routes.ERP_LOGIN) },
            )
        }
        composable(Routes.BLOCKED_APPS) {
            BlockedAppsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SCREEN_TIME) {
            ScreenTimeScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TUTORIAL) {
            TutorialScreen(
                onFinished = { navController.navigateClearingStack(Routes.HOME) },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
            )
        }
        composable(Routes.CALENDAR) {
            CalendarSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
