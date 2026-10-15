package com.flix.music.navigation

import com.flix.music.core.navigation.TopLevelRoute
import com.flix.music.feature.home.navigation.HomeRoute
import com.flix.music.feature.library.navigation.LibraryGraph
import com.flix.music.feature.search.navigation.SearchGraph
import com.flix.music.resources.Res
import com.flix.music.resources.ic_home
import com.flix.music.resources.ic_home_filled
import com.flix.music.resources.ic_library_music
import com.flix.music.resources.ic_library_music_filled
import com.flix.music.resources.ic_search
import com.flix.music.resources.tab_home
import com.flix.music.resources.tab_library
import com.flix.music.resources.tab_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class TopLevelDestination (
    val route: TopLevelRoute,
    val label: StringResource,
    val icon: DrawableResource,
    val selectedIcon: DrawableResource,
) {
    Home(HomeRoute, Res.string.tab_home, Res.drawable.ic_home, Res.drawable.ic_home_filled),
    Search(SearchGraph, Res.string.tab_search, Res.drawable.ic_search, Res.drawable.ic_search),
    Library(LibraryGraph, Res.string.tab_library, Res.drawable.ic_library_music, Res.drawable.ic_library_music_filled)
}