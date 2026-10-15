package com.flix.music.core.player

import com.flix.music.core.model.Track
import kotlin.time.Duration

/**
 * Playback states from the design ("Estados de reproducción"), mapped from Media3's
 * `Player.STATE_*` + `playWhenReady` + `PlaybackException`.
 * Offline and Unavailable arrive with connectivity and playability data.
 */
enum class PlaybackStatus { Idle, Loading, Buffering, Playing, Paused, Ended, Error }

enum class RepeatMode { Off, All, One }

/** Where the current queue came from, shown as "Reproduciendo desde …". */
sealed interface PlaybackSource {
    data class Search(val query: String) : PlaybackSource
    data class Genre(val name: String) : PlaybackSource
    data object Favorites : PlaybackSource
    data object History : PlaybackSource
    data object Downloads : PlaybackSource
    data class Playlist(val name: String) : PlaybackSource
    data class Artist(val name: String) : PlaybackSource
    data class Album(val name: String) : PlaybackSource
    data class Song(val title: String) : PlaybackSource
}

/** A queued track and its position in the player's queue, which queue actions address. */
data class QueueItem(val index: Int, val track: Track)

data class PlayerState(
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val current: Track? = null,
    val next: Track? = null,
    /** What plays after [current], in play order (shuffle included). */
    val upNext: List<QueueItem> = emptyList(),
    val source: PlaybackSource? = null,
    val position: Duration = Duration.ZERO,
    val bufferedPosition: Duration = Duration.ZERO,
    val duration: Duration? = null,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
) {
    val isActive: Boolean get() = status == PlaybackStatus.Playing || status == PlaybackStatus.Buffering
}

/** One-off events the UI shows as a snackbar. */
sealed interface PlayerEvent {
    data class TrackSkipped(val title: String) : PlayerEvent
}
