package com.flix.music.core.data.repository

import com.flix.music.core.model.Lyrics
import com.flix.music.core.model.Track

/** Lyrics of songs. Failures are thrown as `FlixException`. */
interface LyricsRepository {

    /** Null when there are no lyrics for [track]. */
    suspend fun lyrics(track: Track): Lyrics?
}
