package com.flix.music.core.data.repository

import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow

/** Play history, stored on the device. */
interface HistoryRepository {

    /** Most recent first, capped at [MAX_HISTORY_ENTRIES]. */
    val history: Flow<List<HistoryEntry>>

    suspend fun record(track: Track)

    suspend fun remove(entryId: Long)

    /** Puts back a removed [entry] with its original id and time ("Deshacer"). */
    suspend fun restore(entry: HistoryEntry)

    suspend fun clear()
}

const val MAX_HISTORY_ENTRIES = 500
