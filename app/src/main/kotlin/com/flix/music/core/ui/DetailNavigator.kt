package com.flix.music.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef

/**
 * Opens the song, artist and album screens from anywhere (design Navigation: they
 * stack inside the current tab). The app shell provides it; the Full Player collapses
 * before navigating.
 */
interface DetailNavigator {

    fun openSong(trackId: String)

    fun openArtist(artist: ArtistRef)

    /** [artist] helps find the album's songs, since albums are derived from search. */
    fun openAlbum(album: AlbumRef, artist: ArtistRef?)
}

/** Defaults to doing nothing so previews and tests need no NavHost. */
val LocalDetailNavigator = staticCompositionLocalOf<DetailNavigator> {
    object : DetailNavigator {
        override fun openSong(trackId: String) = Unit
        override fun openArtist(artist: ArtistRef) = Unit
        override fun openAlbum(album: AlbumRef, artist: ArtistRef?) = Unit
    }
}
