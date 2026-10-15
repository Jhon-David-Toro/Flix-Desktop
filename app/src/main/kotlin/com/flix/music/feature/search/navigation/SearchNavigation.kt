package com.flix.music.feature.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.flix.music.core.model.Genre
import com.flix.music.core.navigation.TopLevelRoute
import com.flix.music.feature.search.SearchScreen
import kotlinx.serialization.Serializable

/** The Search tab: a nested graph so its detail screens keep the tab selected. */
@Serializable
data object SearchGraph : TopLevelRoute

@Serializable
data object SearchRoute

/**
 * Registers the Search tab. [nestedDestinations] adds the detail screens reachable
 * from Search (genre, and later artist, album and song).
 */
fun NavGraphBuilder.searchGraph(
    onGenreClick: (Genre) -> Unit,
    nestedDestinations: NavGraphBuilder.() -> Unit,
) {
    navigation<SearchGraph>(startDestination = SearchRoute) {
        composable<SearchRoute> {
            SearchScreen(onGenreClick = onGenreClick)
        }
        nestedDestinations()
    }
}
