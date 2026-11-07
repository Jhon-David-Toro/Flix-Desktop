package com.flix.music.core.model

import kotlin.time.Duration

data class Track(
    val id: String,
    val title: String,
    val artists: List<ArtistRef>,
    val album: AlbumRef?,
    val duration: Duration?,
    val artworkUrl: String?,
) {

    val artistNames: String
        get() = artists.joinToString(", ") { it.name }
}

data class ArtistRef(
    val id: String?,
    val name: String,
)

data class AlbumRef(
    val id: String?,
    val name: String,
)

/** A song plus whether it can be played now (design: "No disponible" when playability ≠ OK). */
data class SongDetails(val track: Track, val isPlayable: Boolean)

private val ARTWORK_SIZE = Regex("=w[0-9]+-h[0-9]+")

/**
 * The cover for the big player: YouTube's image CDN serves the same picture at any size, so
 * the 544 px list cover becomes 1200 px (sharp on high-density screens). Other hosts, such
 * as video thumbnails, have no size in their address and stay as they are.
 */
val Track.largeArtworkUrl: String? get() = artworkUrl?.replaceFirst(ARTWORK_SIZE, "=w1200-h1200")
