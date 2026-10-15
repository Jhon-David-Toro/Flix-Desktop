package com.flix.music.core.data.repository

import com.flix.music.core.data.database.FavoriteTrackEntity
import com.flix.music.core.data.database.FavoritesDao
import com.flix.music.core.data.database.toStoredTrack
import com.flix.music.core.data.database.toTrack
import com.flix.music.core.model.FavoriteTrack
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class FavoritesRepositoryImpl(
    private val favoritesDao: FavoritesDao,
) : FavoritesRepository {

    override val favorites: Flow<List<FavoriteTrack>> = favoritesDao.observeAll().map { entities ->
        entities.map { entity ->
            FavoriteTrack(entity.track.toTrack(), Instant.ofEpochMilli(entity.addedAtMillis))
        }
    }

    override val favoriteIds: Flow<Set<String>> = favoritesDao.observeIds().map { ids -> ids.toSet() }

    override suspend fun add(track: Track, addedAt: Instant) {
        favoritesDao.upsert(FavoriteTrackEntity(track.toStoredTrack(), addedAt.toEpochMilli()))
    }

    override suspend fun remove(trackId: String) = favoritesDao.delete(trackId)

    override suspend fun toggle(track: Track): Boolean {
        val isFavorite = favoritesDao.exists(track.id)
        if (isFavorite) remove(track.id) else add(track)
        return !isFavorite
    }
}
