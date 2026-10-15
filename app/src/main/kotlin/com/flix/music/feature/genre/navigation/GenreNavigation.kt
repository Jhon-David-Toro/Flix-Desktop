package com.flix.music.feature.genre.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.genre.GenreScreen
import kotlinx.serialization.Serializable

@Serializable
data class GenreRoute(val genreId: String)

fun NavController.navigateToGenre(genreId: String) = navigateFromResumed(GenreRoute(genreId))

fun NavGraphBuilder.genreScreen(onBack: () -> Unit) {
    composable<GenreRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        GenreScreen(onBack = onBack)
    }
}
