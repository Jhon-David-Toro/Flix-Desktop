package com.flix.music.core.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * On-device library. The schema is exported to `app/schemas`; every new version
 * migrates with an auto-migration so favorites and history are never wiped.
 */
@Database(
    entities = [
        FavoriteTrackEntity::class,
        HistoryEntryEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FollowedArtistEntity::class,
        SavedAlbumEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class FlixDatabase : RoomDatabase() {

    abstract fun favoritesDao(): FavoritesDao

    abstract fun historyDao(): HistoryDao

    abstract fun playlistsDao(): PlaylistsDao

    abstract fun followedArtistsDao(): FollowedArtistsDao

    abstract fun savedAlbumsDao(): SavedAlbumsDao
}
