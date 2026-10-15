package com.flix.music.core.player

import com.flix.music.core.model.Track

/** How many radio songs autoplay adds at a time; when they run out, it adds more. */
internal const val AUTOPLAY_BATCH_SIZE = 25

/**
 * The radio songs autoplay appends: those not in the queue yet, each once, in the
 * radio's order, at most [limit].
 */
internal fun autoplayAdditions(radio: List<Track>, queuedIds: Set<String>, limit: Int = AUTOPLAY_BATCH_SIZE): List<Track> =
    radio.asSequence()
        .filterNot { track -> track.id in queuedIds }
        .distinctBy(Track::id)
        .take(limit)
        .toList()
