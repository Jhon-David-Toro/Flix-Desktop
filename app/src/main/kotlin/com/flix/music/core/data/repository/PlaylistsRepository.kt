package com.flix.music.core.data.repository

import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow

/** Playlists created on the device. */
interface PlaylistsRepository {

    val playlists: Flow<List<Playlist>>

    /** Emits null once the playlist is deleted. */
    fun playlist(playlistId: Long): Flow<Playlist?>

    /** Creates a playlist, with [firstTrack] when it was created from "Añadir a playlist". Returns its id. */
    suspend fun create(name: String, description: String?, firstTrack: Track? = null): Long

    /** Appends [track]. Returns false when the playlist already had it. */
    suspend fun addTrack(playlistId: Long, track: Track): Boolean

    /** Saves the edit form: name, description and [tracks] in their new order. */
    suspend fun save(playlistId: Long, name: String, description: String?, tracks: List<Track>)

    suspend fun delete(playlistId: Long)
}
