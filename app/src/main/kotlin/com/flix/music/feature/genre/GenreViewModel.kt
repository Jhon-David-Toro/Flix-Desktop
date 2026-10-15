package com.flix.music.feature.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.Genre
import com.flix.music.core.model.Genres
import com.flix.music.core.model.Track
import com.flix.music.core.model.distinctAlbums
import com.flix.music.core.model.distinctArtists
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.nowPlaying
import com.flix.music.feature.genre.navigation.GenreRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads a genre's popular songs through its curated search query and derives its
 * artists and albums from them (design: "Cada género = búsqueda curada").
 */
class GenreViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    /**
     * Reads the [GenreRoute.genreId] argument by name instead of `toRoute()`, so plain
     * JVM tests can build the ViewModel with `SavedStateHandle(mapOf("genreId" to ...))`.
     */
    val genre: Genre = requireNotNull(
        Genres.byId(requireNotNull(savedStateHandle.get<String>(GENRE_ID_ARGUMENT))),
    )

    private val _uiState = MutableStateFlow<GenreUiState>(GenreUiState.Loading)
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onPlay() = playGenre(startIndex = 0, shuffle = false)

    /** "Aleatorio": shuffle on, starting from a random song. */
    fun onShuffle() {
        val tracks = contentTracks() ?: return
        playGenre(startIndex = tracks.indices.random(), shuffle = true)
    }

    fun onTrackClick(track: Track) {
        val startIndex = contentTracks()?.indexOfFirst { it.id == track.id } ?: return
        if (startIndex >= 0) playGenre(startIndex = startIndex, shuffle = false)
    }

    private fun playGenre(startIndex: Int, shuffle: Boolean) {
        val tracks = contentTracks() ?: return
        playerController.play(
            tracks = tracks,
            startIndex = startIndex,
            source = PlaybackSource.Genre(genre.name),
            shuffle = shuffle,
        )
    }

    private fun contentTracks(): List<Track>? = (_uiState.value as? GenreUiState.Content)?.tracks

    private fun load() {
        viewModelScope.launch {
            _uiState.value = GenreUiState.Loading
            _uiState.value = try {
                val tracks = repository.search(genre.query)
                if (tracks.isEmpty()) {
                    GenreUiState.Empty
                } else {
                    GenreUiState.Content(
                        tracks = tracks,
                        artists = tracks.distinctArtists(),
                        albums = tracks.distinctAlbums(),
                    )
                }
            } catch (exception: FlixException) {
                GenreUiState.Error(exception.error)
            }
        }
    }

    private companion object {
        const val GENRE_ID_ARGUMENT = "genreId"
    }
}
