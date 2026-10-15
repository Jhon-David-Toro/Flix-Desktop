package com.flix.music.feature.artist.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.artist.ArtistScreen
import kotlinx.serialization.Serializable

/** [artistName] is what gets searched; [artistId] narrows the results to that channel. */
@Serializable
data class ArtistRoute(val artistId: String?, val artistName: String)

fun NavController.navigateToArtist(artistId: String?, artistName: String) =
    navigateFromResumed(ArtistRoute(artistId, artistName))

fun NavGraphBuilder.artistScreen(onBack: () -> Unit) {
    composable<ArtistRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        ArtistScreen(onBack = onBack)
    }
}
