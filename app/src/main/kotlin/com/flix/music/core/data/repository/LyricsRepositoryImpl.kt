package com.flix.music.core.data.repository

import com.flix.music.core.data.lyrics.LrclibClient
import com.flix.music.core.data.lyrics.SavedLyricsStore
import com.flix.music.core.data.network.catalogCall
import com.flix.music.core.model.Lyrics
import com.flix.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Lyrics saved with a download first, so they show offline; otherwise LRCLIB,
 * remembering each answer (also "no lyrics") for the session.
 */
class LyricsRepositoryImpl(
    private val lrclibClient: LrclibClient,
    private val savedLyricsStore: SavedLyricsStore,
) : LyricsRepository {

    private class Answer(val lyrics: Lyrics?)

    private val answers = ConcurrentHashMap<String, Answer>()

    override suspend fun lyrics(track: Track): Lyrics? {
        withContext(Dispatchers.IO) { savedLyricsStore.read(track.id) }?.let { saved -> return saved }
        answers[track.id]?.let { answer -> return answer.lyrics }
        val lyrics = catalogCall { lrclibClient.find(track) }
        answers[track.id] = Answer(lyrics)
        return lyrics
    }
}
