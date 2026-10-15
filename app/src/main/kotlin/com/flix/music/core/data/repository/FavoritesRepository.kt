package com.flix.music.core.data.repository

import com.flix.music.core.model.FavoriteTrack
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Favorite songs, stored on the device. */
interface FavoritesRepository {

    /** Most recently added first. */
    val favorites: Flow<List<FavoriteTrack>>

    val favoriteIds: Flow<Set<String>>

    /** Adds [track]; [addedAt] lets "undo" restore a removed favorite in its original place. */
    suspend fun add(track: Track, addedAt: Instant = Instant.now())

    suspend fun remove(trackId: String)

    /** Adds [track] if it is not a favorite, removes it otherwise. Returns whether it is now a favorite. */
    suspend fun toggle(track: Track): Boolean
}
