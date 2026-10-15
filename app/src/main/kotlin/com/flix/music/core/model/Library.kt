package com.flix.music.core.model

import java.time.Instant

/** A favorite song and when it was added (used for the "Recientes" order and for undo). */
data class FavoriteTrack(
    val track: Track,
    val addedAt: Instant,
)

/** One play in the history. The same song appears once per play. */
data class HistoryEntry(
    val id: Long,
    val track: Track,
    val playedAt: Instant,
)

/**
 * A playlist created on this device ("Creada por ti"). [tracks] keep the user's order.
 * [updatedAt] drives the "Recientes" order in the Library.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val description: String?,
    val tracks: List<Track>,
    val updatedAt: Instant,
) {
    /** Automatic cover (design): the first 4 distinct covers as a mosaic, or 1 if there are fewer. */
    val coverUrls: List<String>
        get() = tracks.mapNotNull { track -> track.artworkUrl }.distinct().let { urls ->
            if (urls.size >= MOSAIC_SIZE) urls.take(MOSAIC_SIZE) else urls.take(1)
        }

    fun contains(trackId: String): Boolean = tracks.any { track -> track.id == trackId }

    companion object {
        /** Name limit shown as "11 / 60" in the create and edit forms. */
        const val MAX_NAME_LENGTH = 60
        private const val MOSAIC_SIZE = 4
    }
}
