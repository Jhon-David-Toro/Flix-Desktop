package com.flix.music.core.player

import com.flix.music.core.model.Track

/** How many radio songs autoplay adds at a time; when they run out, it adds more. */
internal const val AUTOPLAY_BATCH_SIZE = 25

/** Same song under another id (live, lyric video, remaster…): same title and first artist. */
internal fun Track.sameSongKey(): String {
    val cleanTitle = title.substringBefore('(').substringBefore('[').substringBefore(" - ").trim().lowercase()
    return "$cleanTitle|${artists.firstOrNull()?.name?.lowercase().orEmpty()}"
}

/**
 * The radio songs autoplay appends: not in the queue yet (by id or as another version of a
 * queued song), each once, in the radio's order, at most [limit].
 */
internal fun autoplayAdditions(radio: List<Track>, queued: List<Track>, limit: Int = AUTOPLAY_BATCH_SIZE): List<Track> {
    val seen = queued.mapTo(HashSet()) { track -> track.sameSongKey() }
    return radio.asSequence()
        .filter { track -> seen.add(track.sameSongKey()) }
        .take(limit)
        .toList()
}
