package com.flix.music.feature.trackmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.DownloadsRepository
import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.data.repository.PlaylistsRepository
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.SortOrder
import com.flix.music.core.model.Track
import com.flix.music.core.model.sortedPlaylistsFor
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Actions of the song menu and the "Añadir a playlist" picker, shared by every screen. */
class TrackMenuViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val playerController: PlayerController,
    private val downloadsRepository: DownloadsRepository,
) : ViewModel() {

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, WhileUiSubscribed, emptySet())

    /** Songs downloaded or on their way, which the menu offers to remove instead. */
    val downloadIds: StateFlow<Set<String>> = downloadsRepository.downloads
        .map { downloads -> downloads.mapTo(HashSet()) { download -> download.track.id } }
        .stateIn(viewModelScope, WhileUiSubscribed, emptySet())

    /** Most recently changed first, so the playlist you are building is on top. */
    val playlists: StateFlow<List<Playlist>> = playlistsRepository.playlists
        .map { playlists -> playlists.sortedPlaylistsFor(SortOrder.Recent) }
        .stateIn(viewModelScope, WhileUiSubscribed, emptyList())

    fun onPlayNext(track: Track) = playerController.playNext(track)

    fun onAddToQueue(track: Track) = playerController.addToQueue(track)

    fun onToggleFavorite(track: Track) {
        viewModelScope.launch { favoritesRepository.toggle(track) }
    }

    fun onDownload(track: Track) = downloadsRepository.download(listOf(track))

    fun onRemoveDownload(track: Track) = downloadsRepository.remove(track.id)

    /** Returns false when the playlist already had the song. */
    suspend fun addToPlaylist(playlist: Playlist, track: Track): Boolean =
        playlistsRepository.addTrack(playlist.id, track)

    /** Creates the playlist with [track] as its first song and returns its id. */
    suspend fun createPlaylistWith(name: String, description: String, track: Track): Long =
        playlistsRepository.create(name, description, firstTrack = track)
}
