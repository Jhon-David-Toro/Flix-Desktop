package com.flix.music.core.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "favorite_tracks", primaryKeys = ["trackId"])
data class FavoriteTrackEntity(
    @Embedded val track: StoredTrack,
    val addedAtMillis: Long,
)

@Entity(tableName = "history", indices = [Index("playedAtMillis")])
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    @Embedded val track: StoredTrack,
    val playedAtMillis: Long,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String,
    val description: String?,
    val updatedAtMillis: Long,
)

/** A song inside a playlist. The primary key keeps each song at most once per playlist. */
@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val position: Int,
    @Embedded val track: StoredTrack,
)

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(parentColumn = "playlistId", entityColumn = "playlistId")
    val tracks: List<PlaylistTrackEntity>,
)

@Entity(tableName = "followed_artists")
data class FollowedArtistEntity(
    @PrimaryKey val artistId: String,
    val name: String,
    val artworkUrl: String?,
    val followedAtMillis: Long,
)

@Entity(tableName = "saved_albums")
data class SavedAlbumEntity(
    @PrimaryKey val albumId: String,
    val name: String,
    val artistName: String,
    val artworkUrl: String?,
    val savedAtMillis: Long,
)
