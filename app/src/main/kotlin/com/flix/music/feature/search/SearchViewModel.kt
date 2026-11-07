package com.flix.music.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.data.repository.RecentSearchesRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.Track
import com.flix.music.core.model.distinctAlbums
import com.flix.music.core.model.distinctArtists
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Search screen logic: debounced search while typing, immediate search for explicit
 * actions (recent, mood, suggestion, retry), and recent searches saved on success.
 */
class SearchViewModel(
    private val repository: MusicRepository,
    private val recentSearchesRepository: RecentSearchesRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    /** Field text. Snapshot state (not a Flow) so the text field never lags or jumps. */
    var query by mutableStateOf("")
        private set

    var filter by mutableStateOf(SearchFilter.All)
        private set

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val recentSearches: StateFlow<List<String>> = recentSearchesRepository.recentSearches
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        query = newQuery
        filter = SearchFilter.All
        searchJob?.cancel()

        val text = newQuery.trim()
        if (text.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        if (!isShowingResults()) {
            _uiState.value = SearchUiState.Typing(typingSuggestions(text, recentSearches.value))
        }
        if (text.length < MIN_QUERY_LENGTH) return

        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            search(text)
        }
    }

    /** Searches [text] right away, without debounce, and puts it in the field. */
    fun onSearchNow(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return
        query = trimmedText
        filter = SearchFilter.All
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search(trimmedText) }
    }

    fun onClear() = onQueryChange("")

    fun onRetry() = onSearchNow(query)

    fun onFilterChange(newFilter: SearchFilter) {
        filter = newFilter
    }

    fun onRemoveRecent(recentSearch: String) {
        viewModelScope.launch { recentSearchesRepository.remove(recentSearch) }
    }

    fun onClearRecents() {
        viewModelScope.launch { recentSearchesRepository.clear() }
    }

    /**
     * Plays only [track]: the results are usually versions of the same song, so queueing
     * them repeats it; the music goes on with its radio (autoplay) instead.
     */
    fun onTrackClick(track: Track) {
        val results = currentResults() ?: return
        if ((results.tracks + results.videos).none { it.id == track.id }) return
        playerController.play(
            tracks = listOf(track),
            startIndex = 0,
            source = PlaybackSource.Search(query.trim()),
        )
    }

    private fun currentResults(): SearchUiState.Results? = when (val state = _uiState.value) {
        is SearchUiState.Results -> state
        is SearchUiState.Loading -> state.previous
        else -> null
    }

    private fun isShowingResults(): Boolean = currentResults() != null

    private suspend fun search(searchQuery: String) {
        _uiState.value = SearchUiState.Loading(previous = currentResults())
        _uiState.value = try {
            val (songs, allVideos) = coroutineScope {
                val videosRequest = async { try { repository.searchVideos(searchQuery) } catch (exception: FlixException) { emptyList() } }
                repository.search(searchQuery) to videosRequest.await()
            }
            val videos = allVideos.filterNot { video -> songs.any { song -> song.id == video.id } }
            // Without songs, the videos take their place so the results always lead with something.
            val tracks = songs.ifEmpty { videos }
            if (tracks.isEmpty()) {
                SearchUiState.Empty(
                    query = searchQuery,
                    suggestions = similarRecentSearches(searchQuery, recentSearches.value),
                )
            } else {
                recentSearchesRepository.add(searchQuery)
                SearchUiState.Results(
                    tracks = tracks,
                    artists = tracks.distinctArtists(),
                    albums = tracks.distinctAlbums(),
                    videos = if (songs.isEmpty()) emptyList() else videos,
                )
            }
        } catch (exception: FlixException) {
            SearchUiState.Error(exception.error)
        }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val DEBOUNCE_MS = 300L
    }
}
