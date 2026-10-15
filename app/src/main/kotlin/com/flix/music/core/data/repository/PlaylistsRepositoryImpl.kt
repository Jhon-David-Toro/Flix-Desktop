package com.flix.music.core.data.repository

import com.flix.music.core.data.database.PlaylistEntity
import com.flix.music.core.data.database.PlaylistWithTracks
import com.flix.music.core.data.database.PlaylistsDao
import com.flix.music.core.data.database.toStoredTrack
import com.flix.music.core.data.database.toTrack
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

class PlaylistsRepositoryImpl(
    private val playlistsDao: PlaylistsDao,
    private val clock: Clock,
) : PlaylistsRepository {

    override val playlists: Flow<List<Playlist>> =
        playlistsDao.observeAll().map { playlists -> playlists.map { playlist -> playlist.toPlaylist() } }

    override fun playlist(playlistId: Long): Flow<Playlist?> =
        playlistsDao.observe(playlistId).map { playlist -> playlist?.toPlaylist() }

    override suspend fun create(name: String, description: String?, firstTrack: Track?): Long {
        val now = clock.millis()
        val playlistId = playlistsDao.insert(
            PlaylistEntity(name = name.trim(), description = description.normalized(), updatedAtMillis = now),
        )
        if (firstTrack != null) playlistsDao.appendTrack(playlistId, firstTrack.toStoredTrack(), now)
        return playlistId
    }

    override suspend fun addTrack(playlistId: Long, track: Track): Boolean =
        playlistsDao.appendTrack(playlistId, track.toStoredTrack(), clock.millis())

    override suspend fun save(playlistId: Long, name: String, description: String?, tracks: List<Track>) {
        playlistsDao.replace(
            playlistId = playlistId,
            name = name.trim(),
            description = description.normalized(),
            tracks = tracks.map { track -> track.toStoredTrack() },
            updatedAtMillis = clock.millis(),
        )
    }

    override suspend fun delete(playlistId: Long) = playlistsDao.delete(playlistId)
}

private fun String?.normalized(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

private fun PlaylistWithTracks.toPlaylist() = Playlist(
    id = playlist.playlistId,
    name = playlist.name,
    description = playlist.description,
    tracks = tracks.sortedBy { track -> track.position }.map { track -> track.track.toTrack() },
    updatedAt = Instant.ofEpochMilli(playlist.updatedAtMillis),
)
