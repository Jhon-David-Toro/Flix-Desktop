package com.flix.music.feature.song.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.song.SongScreen
import kotlinx.serialization.Serializable

@Serializable
data class SongRoute(val trackId: String)

fun NavController.navigateToSong(trackId: String) = navigateFromResumed(SongRoute(trackId))

fun NavGraphBuilder.songScreen(onBack: () -> Unit) {
    composable<SongRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        SongScreen(onBack = onBack)
    }
}
