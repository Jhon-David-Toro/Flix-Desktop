package com.flix.music.feature.favorites.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.favorites.FavoritesScreen
import kotlinx.serialization.Serializable

@Serializable
data object FavoritesRoute

fun NavController.navigateToFavorites() = navigateFromResumed(FavoritesRoute)

fun NavGraphBuilder.favoritesScreen(onBack: () -> Unit, onBrowseMusic: () -> Unit) {
    composable<FavoritesRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        FavoritesScreen(onBack = onBack, onBrowseMusic = onBrowseMusic)
    }
}
