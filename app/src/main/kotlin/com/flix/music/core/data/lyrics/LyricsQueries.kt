package com.flix.music.core.data.lyrics

import com.flix.music.core.model.Track

/** One title/artist pair to look up. */
internal data class LyricsQuery(val title: String, val artist: String)

/**
 * The lookups to try for a YouTube track, most exact first: its title as is, then
 * without the "(Official Video)" / "[Remastered]" decorations and "feat." credits
 * YouTube adds. Titles like "Luis Fonsi - Despacito ft. Daddy Yankee" (videos
 * instead of songs) are also split into artist and title.
 */
internal fun lyricsQueries(track: Track): List<LyricsQuery> {
    val artist = track.artists.firstOrNull()?.name.orEmpty().removeSuffix(TOPIC_SUFFIX).removeSuffix(VEVO_SUFFIX)
    val bareTitle = track.title.replace(DECORATION, "").replace(FEATURING, "").trim(' ', '-', '–', '—')
    val queries = mutableListOf(LyricsQuery(track.title.trim(), artist), LyricsQuery(bareTitle, artist))
    val separator = bareTitle.indexOf(ARTIST_TITLE_SEPARATOR)
    if (separator > 0) {
        val titleArtist = bareTitle.substring(0, separator).trim()
        val titleSong = bareTitle.substring(separator + ARTIST_TITLE_SEPARATOR.length).trim()
        queries += LyricsQuery(titleSong, titleArtist)
    }
    return queries.filter { query -> query.title.isNotBlank() }.distinct()
}

private const val TOPIC_SUFFIX = " - Topic"
private const val VEVO_SUFFIX = "VEVO"
private const val ARTIST_TITLE_SEPARATOR = " - "
private val DECORATION = Regex("\\s*[(\\[][^)\\]]*[)\\]]")
private val FEATURING = Regex("(?i)\\s+(feat\\.?|ft\\.?|featuring)\\s.*$")
