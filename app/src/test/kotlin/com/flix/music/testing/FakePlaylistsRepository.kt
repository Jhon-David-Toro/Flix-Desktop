package com.flix.music.testing

import com.flix.music.core.data.repository.PlaylistsRepository
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** In-memory playlists with the same rules as Room: no duplicates, deleting removes it. */
class FakePlaylistsRepository(initial: List<Playlist> = emptyList()) : PlaylistsRepository {

    private val stored = MutableStateFlow(initial)

    val current: List<Playlist> get() = stored.value

    override val playlists: Flow<List<Playlist>> = stored

    override fun playlist(playlistId: Long): Flow<Playlist?> =
        stored.map { playlists -> playlists.firstOrNull { it.id == playlistId } }

    override suspend fun create(name: String, description: String?, firstTrack: Track?): Long {
        val playlistId = (stored.value.maxOfOrNull { it.id } ?: 0) + 1
        val playlist = Playlist(playlistId, name.trim(), description?.takeIf { it.isNotBlank() }, listOfNotNull(firstTrack), Instant.now())
        stored.value = stored.value + playlist
        return playlistId
    }

    override suspend fun addTrack(playlistId: Long, track: Track): Boolean {
        val playlist = stored.value.first { it.id == playlistId }
        if (playlist.contains(track.id)) return false
        replace(playlist.copy(tracks = playlist.tracks + track))
        return true
    }

    override suspend fun save(playlistId: Long, name: String, description: String?, tracks: List<Track>) {
        val playlist = stored.value.first { it.id == playlistId }
        replace(playlist.copy(name = name.trim(), description = description?.takeIf { it.isNotBlank() }, tracks = tracks))
    }

    override suspend fun delete(playlistId: Long) {
        stored.value = stored.value.filterNot { it.id == playlistId }
    }

    private fun replace(playlist: Playlist) {
        stored.value = stored.value.map { existing -> if (existing.id == playlist.id) playlist else existing }
    }
}
