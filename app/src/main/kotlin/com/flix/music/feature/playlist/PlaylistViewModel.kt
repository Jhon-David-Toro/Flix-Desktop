package com.flix.music.feature.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.DownloadsRepository
import com.flix.music.core.data.repository.PlaylistsRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import com.flix.music.core.model.totalDuration
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

sealed interface PlaylistUiState {

    data object Loading : PlaylistUiState

    data class Content(val playlist: Playlist, val totalDuration: Duration) : PlaylistUiState

    /** The playlist was deleted (from this screen or elsewhere); the screen closes. */
    data object Missing : PlaylistUiState
}

/** Playlist detail (design `playlist`): play, shuffle, play from a song, download, share and delete. */
class PlaylistViewModel(
    savedStateHandle: SavedStateHandle,
    private val playlistsRepository: PlaylistsRepository,
    private val playerController: PlayerController,
    private val downloadsRepository: DownloadsRepository,
) : ViewModel() {

    /** Read by name instead of `toRoute()` so plain JVM tests can pass a [SavedStateHandle]. */
    private val playlistId: Long = requireNotNull(savedStateHandle.get<Long>(PLAYLIST_ID_ARGUMENT))

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    val uiState: StateFlow<PlaylistUiState> = playlistsRepository.playlist(playlistId)
        .map { playlist ->
            if (playlist == null) {
                PlaylistUiState.Missing
            } else {
                PlaylistUiState.Content(playlist, playlist.tracks.totalDuration())
            }
        }
        .stateIn(viewModelScope, WhileUiSubscribed, PlaylistUiState.Loading)

    fun onPlay() = playFrom(startIndex = 0, shuffle = false)

    fun onShuffle() {
        val tracks = tracks()
        if (tracks.isNotEmpty()) playFrom(startIndex = tracks.indices.random(), shuffle = true)
    }

    fun onTrackClick(track: Track) {
        val startIndex = tracks().indexOfFirst { it.id == track.id }
        if (startIndex >= 0) playFrom(startIndex, shuffle = false)
    }

    fun onDownload() = downloadsRepository.download(tracks())

    fun onDelete() {
        viewModelScope.launch { playlistsRepository.delete(playlistId) }
    }

    private fun content(): PlaylistUiState.Content? = uiState.value as? PlaylistUiState.Content

    private fun tracks(): List<Track> = content()?.playlist?.tracks.orEmpty()

    private fun playFrom(startIndex: Int, shuffle: Boolean) {
        val playlist = content()?.playlist ?: return
        if (playlist.tracks.isEmpty()) return
        playerController.play(playlist.tracks, startIndex, PlaybackSource.Playlist(playlist.name), shuffle)
    }

    internal companion object {
        const val PLAYLIST_ID_ARGUMENT = "playlistId"
    }
}
