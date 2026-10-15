package com.flix.music.feature.genre

import com.flix.music.core.model.Album
import com.flix.music.core.model.Artist
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.Track

sealed interface GenreUiState {

    data object Loading : GenreUiState

    /** [tracks] is never empty; artists and albums are derived from it. */
    data class Content(
        val tracks: List<Track>,
        val artists: List<Artist>,
        val albums: List<Album>,
    ) : GenreUiState

    data object Empty : GenreUiState

    data class Error(val error: FlixError) : GenreUiState
}
