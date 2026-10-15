package com.flix.music.feature.search

import com.flix.music.core.model.Album
import com.flix.music.core.model.Artist
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.Track

/** Every state of the Search screen below the field, one per design mockup. */
sealed interface SearchUiState {

    /** Empty field: the discovery hub (recent searches, moods, genres). */
    data object Idle : SearchUiState

    /** Typing before the first results: matching recent searches. */
    data class Typing(val suggestions: List<String>) : SearchUiState

    /** Searching. [previous] results stay on screen dimmed, if there were any. */
    data class Loading(val previous: Results? = null) : SearchUiState

    /** [tracks] is never empty; artists and albums are derived from it. */
    data class Results(
        val tracks: List<Track>,
        val artists: List<Artist>,
        val albums: List<Album>,
    ) : SearchUiState

    /** No results for [query]; [suggestions] are similarly spelled recent searches. */
    data class Empty(
        val query: String,
        val suggestions: List<String> = emptyList(),
    ) : SearchUiState

    data class Error(val error: FlixError) : SearchUiState
}
