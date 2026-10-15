package com.flix.music.core.player

import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * The app's single entry point to playback. Screens and ViewModels depend on this
 * interface, never on Media3, so they can be tested with a fake.
 */
interface PlayerController {

    val state: StateFlow<PlayerState>

    val events: Flow<PlayerEvent>

    /** Replaces the queue with [tracks] and starts playing [tracks]`[startIndex]`. */
    fun play(
        tracks: List<Track>,
        startIndex: Int,
        source: PlaybackSource,
        shuffle: Boolean = false,
    )

    /** Inserts [track] right after the current one, or plays it when nothing is queued. */
    fun playNext(track: Track)

    /** Appends [track] to the end of the queue, or plays it when nothing is queued. */
    fun addToQueue(track: Track)

    /** Jumps to the queued track at [index] (Cola: tapping a row). */
    fun skipToQueueItem(index: Int)

    fun removeQueueItem(index: Int)

    /** Puts [track] back at [index] ("Deshacer" after removing it from the queue). */
    fun restoreQueueItem(index: Int, track: Track)

    fun moveQueueItem(fromIndex: Int, toIndex: Int)

    /** "Limpiar": removes everything except the current track. */
    fun clearUpNext()

    fun togglePlayPause()

    fun seekTo(position: Duration)

    fun skipToNext()

    fun skipToPrevious()

    fun toggleShuffle()

    /** Off → All → One → Off. */
    fun cycleRepeatMode()

    /** Retries the current track after an error, or restarts the queue once it ended. */
    fun retry()
}
