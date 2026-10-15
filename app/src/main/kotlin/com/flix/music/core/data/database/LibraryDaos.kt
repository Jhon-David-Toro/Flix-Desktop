package com.flix.music.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {

    @Query("SELECT * FROM favorite_tracks ORDER BY addedAtMillis DESC")
    fun observeAll(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT trackId FROM favorite_tracks")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE trackId = :trackId)")
    suspend fun exists(trackId: String): Boolean

    @Upsert
    suspend fun upsert(favorite: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE trackId = :trackId")
    suspend fun delete(trackId: String)
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY playedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntryEntity>>

    /** REPLACE lets "undo" re-insert a removed entry with its original id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntryEntity): Long

    @Query("DELETE FROM history WHERE entryId = :entryId")
    suspend fun delete(entryId: Long)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface PlaylistsDao {

    @Transaction
    @Query("SELECT * FROM playlists")
    fun observeAll(): Flow<List<PlaylistWithTracks>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    fun observe(playlistId: Long): Flow<PlaylistWithTracks?>

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, description = :description, updatedAtMillis = :updatedAtMillis WHERE playlistId = :playlistId")
    suspend fun update(playlistId: Long, name: String, description: String?, updatedAtMillis: Long)

    @Query("UPDATE playlists SET updatedAtMillis = :updatedAtMillis WHERE playlistId = :playlistId")
    suspend fun touch(playlistId: Long, updatedAtMillis: Long)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun delete(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    /** Returns -1 when the song is already in the playlist (design: "no duplican"). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(track: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deleteTracks(playlistId: Long)

    @Insert
    suspend fun insertTracks(tracks: List<PlaylistTrackEntity>)

    /** Appends [track] at the end. Returns false when it was already there. */
    @Transaction
    suspend fun appendTrack(playlistId: Long, track: StoredTrack, updatedAtMillis: Long): Boolean {
        val added = insertTrack(PlaylistTrackEntity(playlistId, nextPosition(playlistId), track)) != -1L
        if (added) touch(playlistId, updatedAtMillis)
        return added
    }

    /** Saves the edit form: name, description and the whole track list in its new order. */
    @Transaction
    suspend fun replace(playlistId: Long, name: String, description: String?, tracks: List<StoredTrack>, updatedAtMillis: Long) {
        update(playlistId, name, description, updatedAtMillis)
        deleteTracks(playlistId)
        insertTracks(tracks.mapIndexed { position, track -> PlaylistTrackEntity(playlistId, position, track) })
    }
}

@Dao
interface FollowedArtistsDao {

    @Query("SELECT * FROM followed_artists ORDER BY followedAtMillis DESC")
    fun observeAll(): Flow<List<FollowedArtistEntity>>

    @Upsert
    suspend fun upsert(artist: FollowedArtistEntity)

    @Query("DELETE FROM followed_artists WHERE artistId = :artistId")
    suspend fun delete(artistId: String)
}

@Dao
interface SavedAlbumsDao {

    @Query("SELECT * FROM saved_albums ORDER BY savedAtMillis DESC")
    fun observeAll(): Flow<List<SavedAlbumEntity>>

    @Upsert
    suspend fun upsert(album: SavedAlbumEntity)

    @Query("DELETE FROM saved_albums WHERE albumId = :albumId")
    suspend fun delete(albumId: String)
}
