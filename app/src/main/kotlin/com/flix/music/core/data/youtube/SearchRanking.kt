package com.flix.music.core.data.youtube

import com.flix.music.core.model.Track

/**
 * Search results: the songs first, then the videos (live versions, remixes, covers,
 * fan uploads) that are not already among them, so originals stay on top.
 */
fun rankedSearchResults(songs: List<Track>, videos: List<Track>): List<Track> =
    (songs + videos).distinctBy(Track::id)
