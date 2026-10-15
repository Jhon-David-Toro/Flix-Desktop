package com.flix.music.testing

import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.data.repository.HistoryRepository
import com.flix.music.core.model.FavoriteTrack
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** In-memory favorites, most recent first like the Room query. */
class FakeFavoritesRepository(initial: List<FavoriteTrack> = emptyList()) : FavoritesRepository {

    private val stored = MutableStateFlow(initial.sortedByDescending { it.addedAt })

    override val favorites: Flow<List<FavoriteTrack>> = stored

    override val favoriteIds: Flow<Set<String>> = stored.map { favorites -> favorites.map { it.track.id }.toSet() }

    val current: List<FavoriteTrack> get() = stored.value

    override suspend fun add(track: Track, addedAt: Instant) {
        stored.value = (stored.value.filterNot { it.track.id == track.id } + FavoriteTrack(track, addedAt))
            .sortedByDescending { it.addedAt }
    }

    override suspend fun remove(trackId: String) {
        stored.value = stored.value.filterNot { it.track.id == trackId }
    }

    override suspend fun toggle(track: Track): Boolean {
        val isFavorite = stored.value.any { it.track.id == track.id }
        if (isFavorite) remove(track.id) else add(track, Instant.now())
        return !isFavorite
    }
}

/** In-memory history, most recent first like the Room query. */
class FakeHistoryRepository(initial: List<HistoryEntry> = emptyList()) : HistoryRepository {

    private val stored = MutableStateFlow(initial.sortedByDescending { it.playedAt })

    override val history: Flow<List<HistoryEntry>> = stored

    val current: List<HistoryEntry> get() = stored.value

    override suspend fun record(track: Track) {
        val nextId = (stored.value.maxOfOrNull { it.id } ?: 0) + 1
        stored.value = listOf(HistoryEntry(nextId, track, Instant.now())) + stored.value
    }

    override suspend fun remove(entryId: Long) {
        stored.value = stored.value.filterNot { it.id == entryId }
    }

    override suspend fun restore(entry: HistoryEntry) {
        stored.value = (stored.value + entry).sortedByDescending { it.playedAt }
    }

    override suspend fun clear() {
        stored.value = emptyList()
    }
}
