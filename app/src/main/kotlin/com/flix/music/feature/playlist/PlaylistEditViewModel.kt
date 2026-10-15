package com.flix.music.feature.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.PlaylistsRepository
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Edit playlist (design `pledit`). Changes stay in a draft until "Guardar": name,
 * description, removed songs and the new order. Leaving with changes asks first.
 * The draft lives in snapshot state so text fields never lag behind typing.
 */
class PlaylistEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val playlistsRepository: PlaylistsRepository,
) : ViewModel() {

    private val playlistId: Long =
        requireNotNull(savedStateHandle.get<Long>(PlaylistViewModel.PLAYLIST_ID_ARGUMENT))

    private var original: Playlist? by mutableStateOf(null)

    var name by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    val tracks = mutableStateListOf<Track>()

    val isLoaded: Boolean get() = original != null

    /** True once saved or deleted; the screen closes. */
    var isFinished by mutableStateOf(false)
        private set

    val canSave: Boolean get() = name.isNotBlank()

    val hasChanges: Boolean
        get() {
            val playlist = original ?: return false
            return name != playlist.name ||
                description != playlist.description.orEmpty() ||
                tracks.toList() != playlist.tracks
        }

    init {
        viewModelScope.launch {
            val playlist = playlistsRepository.playlist(playlistId).first()
            if (playlist == null) {
                isFinished = true
                return@launch
            }
            name = playlist.name
            description = playlist.description.orEmpty()
            tracks.addAll(playlist.tracks)
            original = playlist
        }
    }

    fun onNameChange(newName: String) {
        name = newName.take(Playlist.MAX_NAME_LENGTH)
    }

    fun onDescriptionChange(newDescription: String) {
        description = newDescription
    }

    /** Removes [track] from the draft and returns where it was, so "Deshacer" can put it back. */
    fun onRemove(track: Track): Int {
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index >= 0) tracks.removeAt(index)
        return index
    }

    fun onUndoRemove(track: Track, index: Int) {
        if (tracks.none { it.id == track.id }) tracks.add(index.coerceIn(0, tracks.size), track)
    }

    fun onMove(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in tracks.indices || toIndex !in tracks.indices || fromIndex == toIndex) return
        tracks.add(toIndex, tracks.removeAt(fromIndex))
    }

    fun onSave() {
        if (!canSave) return
        viewModelScope.launch {
            playlistsRepository.save(playlistId, name, description, tracks.toList())
            isFinished = true
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            playlistsRepository.delete(playlistId)
            isFinished = true
        }
    }
}
