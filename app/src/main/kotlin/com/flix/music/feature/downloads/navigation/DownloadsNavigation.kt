package com.flix.music.feature.downloads.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.downloads.DownloadsScreen
import kotlinx.serialization.Serializable

@Serializable
data object DownloadsRoute

fun NavController.navigateToDownloads() = navigateFromResumed(DownloadsRoute)

fun NavGraphBuilder.downloadsScreen(onBack: () -> Unit) {
    composable<DownloadsRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        DownloadsScreen(onBack = onBack)
    }
}
