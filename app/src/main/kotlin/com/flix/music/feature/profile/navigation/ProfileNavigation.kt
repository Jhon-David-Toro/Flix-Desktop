package com.flix.music.feature.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.profile.ProfileScreen
import com.flix.music.feature.profile.StatsScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute

@Serializable
data object StatsRoute

fun NavController.navigateToProfile() = navigateFromResumed(ProfileRoute)

fun NavController.navigateToStats() = navigateFromResumed(StatsRoute)

fun NavGraphBuilder.profileScreens(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
) {
    composable<ProfileRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        ProfileScreen(
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            onOpenStats = onOpenStats,
            onOpenHistory = onOpenHistory,
            onOpenDownloads = onOpenDownloads,
        )
    }
    composable<StatsRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        StatsScreen(onBack = onBack)
    }
}
