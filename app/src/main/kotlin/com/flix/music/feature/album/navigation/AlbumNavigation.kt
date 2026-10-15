package com.flix.music.feature.album.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.album.AlbumScreen
import kotlinx.serialization.Serializable

/** The album and artist names are what gets searched, since albums are derived from search. */
@Serializable
data class AlbumRoute(
    val albumId: String?,
    val albumName: String,
    val artistName: String,
    val artistId: String?,
)

fun NavController.navigateToAlbum(albumId: String?, albumName: String, artistName: String, artistId: String?) =
    navigateFromResumed(AlbumRoute(albumId, albumName, artistName, artistId))

fun NavGraphBuilder.albumScreen(onBack: () -> Unit) {
    composable<AlbumRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        AlbumScreen(onBack = onBack)
    }
}
