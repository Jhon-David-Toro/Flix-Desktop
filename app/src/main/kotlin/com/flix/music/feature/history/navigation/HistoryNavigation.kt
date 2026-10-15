package com.flix.music.feature.history.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.history.HistoryScreen
import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

fun NavController.navigateToHistory() = navigateFromResumed(HistoryRoute)

fun NavGraphBuilder.historyScreen(onBack: () -> Unit, onBrowseMusic: () -> Unit) {
    composable<HistoryRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        HistoryScreen(onBack = onBack, onBrowseMusic = onBrowseMusic)
    }
}
