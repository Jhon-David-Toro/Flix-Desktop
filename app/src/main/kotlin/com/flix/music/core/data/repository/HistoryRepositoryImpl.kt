package com.flix.music.core.data.repository

import com.flix.music.core.data.database.HistoryDao
import com.flix.music.core.data.database.HistoryEntryEntity
import com.flix.music.core.data.database.toStoredTrack
import com.flix.music.core.data.database.toTrack
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
) : HistoryRepository {

    override val history: Flow<List<HistoryEntry>> = historyDao.observeRecent(MAX_HISTORY_ENTRIES).map { entities ->
        entities.map { entity ->
            HistoryEntry(entity.entryId, entity.track.toTrack(), Instant.ofEpochMilli(entity.playedAtMillis))
        }
    }

    override suspend fun record(track: Track) {
        historyDao.insert(HistoryEntryEntity(track = track.toStoredTrack(), playedAtMillis = Instant.now().toEpochMilli()))
    }

    override suspend fun remove(entryId: Long) = historyDao.delete(entryId)

    override suspend fun restore(entry: HistoryEntry) {
        historyDao.insert(
            HistoryEntryEntity(
                entryId = entry.id,
                track = entry.track.toStoredTrack(),
                playedAtMillis = entry.playedAt.toEpochMilli(),
            ),
        )
    }

    override suspend fun clear() = historyDao.clear()
}
