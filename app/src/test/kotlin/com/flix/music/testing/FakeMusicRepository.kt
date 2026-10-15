package com.flix.music.testing

import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.SongDetails
import com.flix.music.core.model.Track
import kotlinx.coroutines.delay

/** In-memory [MusicRepository] whose answer, failure and latency are set by each test. */
class FakeMusicRepository : MusicRepository {

    var tracks: List<Track> = emptyList()
    var error: FlixError? = null

    /** Simulated server latency, on the test's virtual clock. */
    var responseDelayMillis = 0L

    var searchCount = 0
        private set

    var lastQuery: String? = null
        private set

    override suspend fun search(query: String): List<Track> {
        searchCount++
        lastQuery = query
        delay(responseDelayMillis)
        error?.let { throw FlixException(it) }
        return tracks
    }

    var songDetails: SongDetails? = null

    override fun cachedTrack(trackId: String): Track? = tracks.firstOrNull { it.id == trackId }

    override suspend fun song(trackId: String): SongDetails {
        delay(responseDelayMillis)
        error?.let { throw FlixException(it) }
        return songDetails ?: SongDetails(tracks.first { it.id == trackId }, isPlayable = true)
    }

    var radioTracks: List<Track> = emptyList()

    override suspend fun radio(track: Track): List<Track> {
        delay(responseDelayMillis)
        error?.let { throw FlixException(it) }
        return radioTracks
    }
}
