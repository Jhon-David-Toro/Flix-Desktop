package com.flix.music.feature.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.DownloadsRepository
import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.DownloadAllState
import com.flix.music.core.model.FavoriteTrack
import com.flix.music.core.model.SortOrder
import com.flix.music.core.model.Track
import com.flix.music.core.model.downloadAllState
import com.flix.music.core.model.matching
import com.flix.music.core.model.sortedFor
import com.flix.music.core.model.totalDuration
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

sealed interface FavoritesUiState {

    data object Loading : FavoritesUiState

    /** [visible] is [all] filtered by the search text and sorted. */
    data class Content(
        val all: List<FavoriteTrack>,
        val visible: List<FavoriteTrack>,
        val totalDuration: Duration,
        val downloadState: DownloadAllState = DownloadAllState.None,
    ) : FavoritesUiState
}

/** Favorites screen: play all / shuffle, local search, sort, and remove with undo. */
class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val playerController: PlayerController,
    private val downloadsRepository: DownloadsRepository,
) : ViewModel() {

    /** Field text as snapshot state (no lag while typing); [searchText] drives the filtering. */
    var query by mutableStateOf("")
        private set

    private val searchText = MutableStateFlow("")

    private val sortOrder = MutableStateFlow(SortOrder.Recent)

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    val uiState: StateFlow<FavoritesUiState> = combine(
        favoritesRepository.favorites,
        searchText,
        sortOrder,
        downloadsRepository.downloads,
    ) { favorites, text, order, downloads ->
        val tracks = favorites.map { it.track }
        FavoritesUiState.Content(
            all = favorites,
            visible = favorites.sortedFor(order).matching(text),
            totalDuration = tracks.totalDuration(),
            downloadState = tracks.downloadAllState(downloads),
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, FavoritesUiState.Loading)

    val currentSortOrder: StateFlow<SortOrder> = sortOrder

    fun onQueryChange(newQuery: String) {
        query = newQuery
        searchText.value = newQuery
    }

    fun onSortOrderChange(order: SortOrder) {
        sortOrder.value = order
    }

    /** Downloads every favorite (not only what the search shows); songs already saved are skipped. */
    fun onDownloadAll() = downloadsRepository.download((uiState.value as? FavoritesUiState.Content)?.all?.map { it.track }.orEmpty())

    fun onPlayAll() = playVisible(startIndex = 0, shuffle = false)

    fun onShuffle() {
        val tracks = visibleTracks()
        if (tracks.isNotEmpty()) playVisible(startIndex = tracks.indices.random(), shuffle = true)
    }

    fun onTrackClick(track: Track) {
        val startIndex = visibleTracks().indexOfFirst { it.id == track.id }
        if (startIndex >= 0) playVisible(startIndex = startIndex, shuffle = false)
    }

    fun onRemove(favorite: FavoriteTrack) {
        viewModelScope.launch { favoritesRepository.remove(favorite.track.id) }
    }

    /** "Deshacer": puts the favorite back in its original position. */
    fun onUndoRemove(favorite: FavoriteTrack) {
        viewModelScope.launch { favoritesRepository.add(favorite.track, favorite.addedAt) }
    }

    private fun visibleTracks(): List<Track> =
        (uiState.value as? FavoritesUiState.Content)?.visible?.map { it.track }.orEmpty()

    private fun playVisible(startIndex: Int, shuffle: Boolean) {
        val tracks = visibleTracks()
        if (tracks.isEmpty()) return
        playerController.play(tracks, startIndex, PlaybackSource.Favorites, shuffle)
    }
}
