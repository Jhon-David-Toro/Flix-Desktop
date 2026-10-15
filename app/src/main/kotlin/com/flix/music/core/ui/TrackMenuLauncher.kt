package com.flix.music.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.flix.music.core.model.Track

/**
 * Opens the song menu (design "Menú de canción") from any screen. The app shell hosts
 * the sheets, so screens only need the track. [onRemoveFromHistory] adds
 * "Quitar del historial", which only applies inside Historial.
 */
interface TrackMenuLauncher {

    fun open(track: Track, onRemoveFromHistory: (() -> Unit)? = null)

    /** Goes straight to the "Añadir a playlist" picker (e.g. the song screen's button). */
    fun addToPlaylist(track: Track)
}

/** Defaults to doing nothing so previews and tests need no host. */
val LocalTrackMenu = staticCompositionLocalOf<TrackMenuLauncher> {
    object : TrackMenuLauncher {
        override fun open(track: Track, onRemoveFromHistory: (() -> Unit)?) = Unit
        override fun addToPlaylist(track: Track) = Unit
    }
}
