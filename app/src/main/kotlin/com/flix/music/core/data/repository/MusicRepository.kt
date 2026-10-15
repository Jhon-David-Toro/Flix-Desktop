package com.flix.music.core.data.repository

import com.flix.music.core.model.SongDetails
import com.flix.music.core.model.Track

/**
 * Music catalog, read from YouTube Music directly on the phone (no backend).
 * Every failure is thrown as a `FlixException`.
 */
interface MusicRepository {

    suspend fun search(query: String): List<Track>

    /** A song already seen in a search this session, so its detail can open instantly. */
    fun cachedTrack(trackId: String): Track?

    suspend fun song(trackId: String): SongDetails

    /** Songs like [track] to keep playing after it (YouTube Music's radio), without [track] itself. */
    suspend fun radio(track: Track): List<Track>
}
