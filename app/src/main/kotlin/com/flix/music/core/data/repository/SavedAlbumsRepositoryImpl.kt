package com.flix.music.core.data.repository

import com.flix.music.core.data.database.SavedAlbumEntity
import com.flix.music.core.data.database.SavedAlbumsDao
import com.flix.music.core.model.Album
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

class SavedAlbumsRepositoryImpl(
    private val savedAlbumsDao: SavedAlbumsDao,
    private val clock: Clock,
) : SavedAlbumsRepository {

    override val albums: Flow<List<Album>> = savedAlbumsDao.observeAll().map { entities ->
        entities.map { entity -> Album(entity.albumId, entity.name, entity.artistName, entity.artworkUrl) }
    }

    override suspend fun save(album: Album) {
        savedAlbumsDao.upsert(SavedAlbumEntity(album.id, album.name, album.artistName, album.artworkUrl, clock.millis()))
    }

    override suspend fun remove(albumId: String) = savedAlbumsDao.delete(albumId)
}
