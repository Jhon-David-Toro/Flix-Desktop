package com.flix.music.feature.search

import com.flix.music.resources.Res
import com.flix.music.resources.search_filter_all
import com.flix.music.resources.search_section_albums
import com.flix.music.resources.search_section_artists
import com.flix.music.resources.search_section_songs
import org.jetbrains.compose.resources.StringResource


enum class SearchFilter(val label: StringResource) {
    All(Res.string.search_filter_all),
    Songs(Res.string.search_section_songs),
    Artists(Res.string.search_section_artists),
    Albums(Res.string.search_section_albums),
}