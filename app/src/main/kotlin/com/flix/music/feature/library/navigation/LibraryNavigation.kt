package com.flix.music.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.flix.music.core.navigation.TopLevelRoute
import com.flix.music.feature.library.LibraryScreen
import kotlinx.serialization.Serializable

/** The Library tab: a nested graph so Favorites, History and playlists keep the tab selected. */
@Serializable
data object LibraryGraph : TopLevelRoute

@Serializable
data object LibraryRoute

fun NavGraphBuilder.libraryGraph(
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlaylist: (playlistId: Long) -> Unit,
    onBrowseMusic: () -> Unit,
    nestedDestinations: NavGraphBuilder.() -> Unit,
) {
    navigation<LibraryGraph>(startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(
                onOpenFavorites = onOpenFavorites,
                onOpenHistory = onOpenHistory,
                onOpenPlaylist = onOpenPlaylist,
                onBrowseMusic = onBrowseMusic,
            )
        }
        nestedDestinations()
    }
}
