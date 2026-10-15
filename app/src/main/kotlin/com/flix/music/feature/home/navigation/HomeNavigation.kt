package com.flix.music.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.model.Genre
import com.flix.music.core.navigation.TopLevelRoute
import com.flix.music.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : TopLevelRoute

fun NavGraphBuilder.homeScreen(
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenGenre: (Genre) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylist: (playlistId: Long) -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(
            onOpenProfile = onOpenProfile,
            onOpenHistory = onOpenHistory,
            onOpenSearch = onOpenSearch,
            onOpenGenre = onOpenGenre,
            onOpenLibrary = onOpenLibrary,
            onOpenFavorites = onOpenFavorites,
            onOpenPlaylist = onOpenPlaylist,
        )
    }
}