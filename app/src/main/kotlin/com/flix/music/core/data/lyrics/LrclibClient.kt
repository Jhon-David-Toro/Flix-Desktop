package com.flix.music.core.data.lyrics

import com.flix.music.core.model.Lyrics
import com.flix.music.core.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.math.abs

/** LRCLIB answered with an HTTP error other than "not found". */
class LrclibException(val statusCode: Int) : IOException("LRCLIB answered HTTP $statusCode")

@Serializable
internal data class LrclibTrack(
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)

/**
 * Lyrics from LRCLIB (lrclib.net), free and without an account. Synced lyrics are
 * preferred. Each [lyricsQueries] variant is tried with an exact lookup, then with
 * a search that only accepts songs within [MAX_DURATION_DIFFERENCE_SECONDS] of the
 * track, so a different song with the same title is not shown. Blocking.
 */
class LrclibClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {

    /** Null when LRCLIB has no lyrics for [track] (or it is an instrumental). */
    fun find(track: Track): Lyrics? {
        for (query in lyricsQueries(track)) {
            exactMatch(query, track)?.toLyrics()?.let { lyrics -> return lyrics }
        }
        for (query in lyricsQueries(track)) {
            search(query).firstOrNull { candidate -> candidate.matchesLengthOf(track) }?.toLyrics()?.let { lyrics -> return lyrics }
        }
        return null
    }

    private fun exactMatch(query: LyricsQuery, track: Track): LrclibTrack? {
        val url = API.newBuilder()
            .addPathSegment("get")
            .addQueryParameter("track_name", query.title)
            .addQueryParameter("artist_name", query.artist)
            .apply { track.album?.name?.let { albumName -> addQueryParameter("album_name", albumName) } }
            .apply { track.duration?.let { duration -> addQueryParameter("duration", duration.inWholeSeconds.toString()) } }
            .build()
        return get(url)?.let { body -> json.decodeFromString<LrclibTrack>(body) }
    }

    private fun search(query: LyricsQuery): List<LrclibTrack> {
        val url = API.newBuilder()
            .addPathSegment("search")
            .addQueryParameter("track_name", query.title)
            .apply { if (query.artist.isNotBlank()) addQueryParameter("artist_name", query.artist) }
            .build()
        return get(url)?.let { body -> json.decodeFromString<List<LrclibTrack>>(body) }.orEmpty()
    }

    private fun get(url: HttpUrl): String? {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == NOT_FOUND) return null
            if (!response.isSuccessful) throw LrclibException(response.code)
            return response.body.string()
        }
    }

    private fun LrclibTrack.matchesLengthOf(track: Track): Boolean {
        val trackSeconds = track.duration?.inWholeSeconds ?: return true
        val candidateSeconds = duration ?: return false
        return abs(candidateSeconds - trackSeconds) <= MAX_DURATION_DIFFERENCE_SECONDS
    }

    private fun LrclibTrack.toLyrics(): Lyrics? {
        if (instrumental) return null
        syncedLyrics?.takeIf(String::isNotBlank)?.let { lrc -> return Lyrics.fromLrc(lrc).takeIf { lyrics -> lyrics.lines.isNotEmpty() } }
        return plainLyrics?.takeIf(String::isNotBlank)?.let(Lyrics::fromPlainText)
    }

    private companion object {
        val API = "https://lrclib.net/api/".toHttpUrl()
        const val USER_AGENT = "Flix Music (Android; personal player)"
        const val NOT_FOUND = 404
        const val MAX_DURATION_DIFFERENCE_SECONDS = 15
    }
}
