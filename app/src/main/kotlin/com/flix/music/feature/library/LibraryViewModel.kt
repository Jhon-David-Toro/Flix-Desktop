package com.flix.music.feature.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.data.repository.FollowedArtistsRepository
import com.flix.music.core.data.repository.HistoryRepository
import com.flix.music.core.data.repository.LibraryLayoutRepository
import com.flix.music.core.data.repository.PlaylistsRepository
import com.flix.music.core.data.repository.SavedAlbumsRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.Album
import com.flix.music.core.model.Artist
import com.flix.music.core.model.FavoriteTrack
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.SortOrder
import com.flix.music.core.model.Track
import com.flix.music.core.model.countPlayedOn
import com.flix.music.core.model.matching
import com.flix.music.core.model.sortedFor
import com.flix.music.core.model.sortedPlaylistsFor
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * Library filters (design `library`): Canciones = favorites, Álbumes = saved,
 * Artistas = followed. [supportsGrid] is false for songs, which are always rows.
 */
enum class LibraryFilter(val supportsGrid: Boolean) {
    Playlists(supportsGrid = true),
    Songs(supportsGrid = false),
    Albums(supportsGrid = true),
    Artists(supportsGrid = true),
}

/** Every collection is already sorted and narrowed by [query]. */
data class LibraryUiState(
    val favoriteCount: Int = 0,
    val playedTodayCount: Int = 0,
    val hasHistory: Boolean = false,
    val filter: LibraryFilter = LibraryFilter.Playlists,
    val sortOrder: SortOrder = SortOrder.Recent,
    val query: String = "",
    val isGrid: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val songs: List<FavoriteTrack> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
) {
    /** Whether the selected filter has anything before searching, to tell "empty" from "no match". */
    val isFilterEmpty: Boolean
        get() = query.isBlank() && when (filter) {
            LibraryFilter.Playlists -> playlists.isEmpty()
            LibraryFilter.Songs -> songs.isEmpty()
            LibraryFilter.Albums -> albums.isEmpty()
            LibraryFilter.Artists -> artists.isEmpty()
        }
}

/** Everything stored on the device that the Library shows. */
private data class LibraryCollections(
    val favorites: List<FavoriteTrack>,
    val history: List<HistoryEntry>,
    val playlists: List<Playlist>,
    val albums: List<Album>,
    val artists: List<Artist>,
)

/** How the user is looking at the Library right now. */
private data class LibraryView(
    val filter: LibraryFilter,
    val sortOrder: SortOrder,
    val query: String,
    val gridFilters: Set<String>,
)

/**
 * Library tab: pinned Favorites and History, filter chips, sort, list/grid (remembered
 * per filter) and a search that narrows the selected collection.
 */
class LibraryViewModel(
    favoritesRepository: FavoritesRepository,
    historyRepository: HistoryRepository,
    private val playlistsRepository: PlaylistsRepository,
    savedAlbumsRepository: SavedAlbumsRepository,
    followedArtistsRepository: FollowedArtistsRepository,
    private val libraryLayoutRepository: LibraryLayoutRepository,
    private val playerController: PlayerController,
    private val clock: Clock,
) : ViewModel() {

    /** Field text as snapshot state (no lag while typing); [searchText] drives the filtering. */
    var query by mutableStateOf("")
        private set

    private val searchText = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.Playlists)
    private val sortOrder = MutableStateFlow(SortOrder.Recent)

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    private val collections = combine(
        favoritesRepository.favorites,
        historyRepository.history,
        playlistsRepository.playlists,
        savedAlbumsRepository.albums,
        followedArtistsRepository.artists,
        ::LibraryCollections,
    )

    private val view = combine(filter, sortOrder, searchText, libraryLayoutRepository.gridFilters, ::LibraryView)

    val uiState: StateFlow<LibraryUiState> = combine(collections, view) { stored, current ->
        val text = current.query.trim()
        LibraryUiState(
            favoriteCount = stored.favorites.size,
            playedTodayCount = stored.history.countPlayedOn(LocalDate.now(clock), clock.zone),
            hasHistory = stored.history.isNotEmpty(),
            filter = current.filter,
            sortOrder = current.sortOrder,
            query = current.query,
            isGrid = current.filter.supportsGrid && current.filter.name in current.gridFilters,
            playlists = stored.playlists.sortedPlaylistsFor(current.sortOrder).filter { it.name.containsText(text) },
            songs = stored.favorites.sortedFor(current.sortOrder).matching(text),
            albums = stored.albums.sortedByName(current.sortOrder) { it.name }
                .filter { it.name.containsText(text) || it.artistName.containsText(text) },
            artists = stored.artists.sortedByName(current.sortOrder) { it.name }.filter { it.name.containsText(text) },
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, LibraryUiState())

    fun onFilterChange(newFilter: LibraryFilter) {
        filter.value = newFilter
    }

    fun onSortOrderChange(order: SortOrder) {
        sortOrder.value = order
    }

    fun onQueryChange(newQuery: String) {
        query = newQuery
        searchText.value = newQuery
    }

    fun onToggleLayout() {
        val state = uiState.value
        if (!state.filter.supportsGrid) return
        viewModelScope.launch { libraryLayoutRepository.setGrid(state.filter.name, grid = !state.isGrid) }
    }

    /** "Canciones = favoritas" (design): plays the saved songs from [track] in the shown order. */
    fun onSongClick(track: Track) {
        val songs = uiState.value.songs.map { it.track }
        val startIndex = songs.indexOfFirst { it.id == track.id }
        if (startIndex >= 0) playerController.play(songs, startIndex, PlaybackSource.Favorites)
    }

    fun onCreatePlaylist(name: String, description: String) {
        viewModelScope.launch { playlistsRepository.create(name, description) }
    }
}

private fun String.containsText(text: String): Boolean = text.isEmpty() || contains(text, ignoreCase = true)

/** Repositories emit most recent first, so "Recientes" keeps their order. */
private fun <Item> List<Item>.sortedByName(order: SortOrder, name: (Item) -> String): List<Item> = when (order) {
    SortOrder.Recent -> this
    SortOrder.Alphabetical -> sortedBy { item -> name(item).lowercase() }
}
