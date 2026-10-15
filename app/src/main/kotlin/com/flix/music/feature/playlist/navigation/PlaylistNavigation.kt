package com.flix.music.feature.playlist.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.playlist.PlaylistEditScreen
import com.flix.music.feature.playlist.PlaylistScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistRoute(val playlistId: Long)

@Serializable
data class PlaylistEditRoute(val playlistId: Long)

fun NavController.navigateToPlaylist(playlistId: Long) = navigateFromResumed(PlaylistRoute(playlistId))

fun NavController.navigateToPlaylistEdit(playlistId: Long) = navigateFromResumed(PlaylistEditRoute(playlistId))

fun NavGraphBuilder.playlistScreens(
    onBack: () -> Unit,
    onEdit: (playlistId: Long) -> Unit,
    onBrowseMusic: () -> Unit,
) {
    composable<PlaylistRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        PlaylistScreen(onBack = onBack, onEdit = onEdit, onBrowseMusic = onBrowseMusic)
    }
    composable<PlaylistEditRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        PlaylistEditScreen(onClose = onBack)
    }
}
