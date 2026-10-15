package com.flix.music.feature.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.data.repository.SavedAlbumsRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.Album
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.Track
import com.flix.music.core.model.fromAlbum
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

sealed interface AlbumUiState {

    val name: String
    val artistName: String

    data class Loading(override val name: String, override val artistName: String) : AlbumUiState

    data class Error(override val name: String, override val artistName: String, val error: FlixError) : AlbumUiState

    /** [canSave] is false when the album has no browse id, so it cannot be kept in the Library. */
    data class Content(
        override val name: String,
        override val artistName: String,
        val artworkUrl: String?,
        val tracks: List<Track>,
        val totalDuration: Duration,
        val canSave: Boolean,
        val isSaved: Boolean,
    ) : AlbumUiState
}

/**
 * Album (design `album`): the songs found for the album in a search by album and artist
 * (there is no album endpoint), which is why the screen shows an info banner.
 */
class AlbumViewModel(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val savedAlbumsRepository: SavedAlbumsRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val albumId: String? = savedStateHandle.get<String>(ALBUM_ID_ARGUMENT)
    private val albumName: String = requireNotNull(savedStateHandle.get<String>(ALBUM_NAME_ARGUMENT))
    private val artistName: String = savedStateHandle.get<String>(ARTIST_NAME_ARGUMENT).orEmpty()

    /** Where the artist name under the title links to. */
    val artist = ArtistRef(savedStateHandle.get<String>(ARTIST_ID_ARGUMENT), artistName)

    private val loaded = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading(albumName, artistName))

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    val uiState: StateFlow<AlbumUiState> = combine(loaded, savedAlbumsRepository.albums) { state, savedAlbums ->
        if (state is AlbumUiState.Content) state.copy(isSaved = savedAlbums.any { it.id == albumId }) else state
    }.stateIn(viewModelScope, WhileUiSubscribed, loaded.value)

    init {
        load()
    }

    fun onRetry() = load()

    /** "El play del álbum pone la cola completa del álbum." */
    fun onPlay() = playFrom(startIndex = 0, shuffle = false)

    fun onShuffle() {
        val tracks = tracks()
        if (tracks.isNotEmpty()) playFrom(startIndex = tracks.indices.random(), shuffle = true)
    }

    fun onTrackClick(track: Track) {
        val startIndex = tracks().indexOfFirst { it.id == track.id }
        if (startIndex >= 0) playFrom(startIndex, shuffle = false)
    }

    fun onToggleSaved() {
        val content = uiState.value as? AlbumUiState.Content ?: return
        val id = albumId ?: return
        viewModelScope.launch {
            if (content.isSaved) {
                savedAlbumsRepository.remove(id)
            } else {
                savedAlbumsRepository.save(Album(id, albumName, content.artistName, content.artworkUrl))
            }
        }
    }

    private fun load() {
        loaded.value = AlbumUiState.Loading(albumName, artistName)
        viewModelScope.launch {
            loaded.value = try {
                val query = listOf(albumName, artistName).filter { it.isNotBlank() }.joinToString(" ")
                val tracks = musicRepository.search(query).fromAlbum(albumId, albumName)
                AlbumUiState.Content(
                    name = albumName,
                    artistName = artistName.ifBlank { tracks.firstOrNull()?.artistNames.orEmpty() },
                    artworkUrl = tracks.firstOrNull()?.artworkUrl,
                    tracks = tracks,
                    totalDuration = tracks.totalDuration(),
                    canSave = albumId != null,
                    isSaved = false,
                )
            } catch (exception: FlixException) {
                AlbumUiState.Error(albumName, artistName, exception.error)
            }
        }
    }

    private fun tracks(): List<Track> = (loaded.value as? AlbumUiState.Content)?.tracks.orEmpty()

    private fun playFrom(startIndex: Int, shuffle: Boolean) {
        val tracks = tracks()
        if (tracks.isEmpty()) return
        playerController.play(tracks, startIndex, PlaybackSource.Album(albumName), shuffle)
    }

    internal companion object {
        const val ALBUM_ID_ARGUMENT = "albumId"
        const val ALBUM_NAME_ARGUMENT = "albumName"
        const val ARTIST_NAME_ARGUMENT = "artistName"
        const val ARTIST_ID_ARGUMENT = "artistId"
    }
}
