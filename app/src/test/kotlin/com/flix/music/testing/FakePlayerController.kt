package com.flix.music.testing

import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.player.PlayerEvent
import com.flix.music.core.player.PlayerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration

/** Records what screens ask the player to do; no Media3 involved. */
class FakePlayerController : PlayerController {

    data class PlayRequest(
        val tracks: List<Track>,
        val startIndex: Int,
        val source: PlaybackSource,
        val shuffle: Boolean,
    )

    private val mutableState = MutableStateFlow(PlayerState())

    override val state: StateFlow<PlayerState> = mutableState

    fun setState(playerState: PlayerState) {
        mutableState.value = playerState
    }

    override val events: Flow<PlayerEvent> = emptyFlow()

    var lastPlayRequest: PlayRequest? = null
        private set

    override fun play(tracks: List<Track>, startIndex: Int, source: PlaybackSource, shuffle: Boolean) {
        lastPlayRequest = PlayRequest(tracks, startIndex, source, shuffle)
    }

    val playNextRequests = mutableListOf<Track>()

    val queueRequests = mutableListOf<Track>()

    override fun playNext(track: Track) {
        playNextRequests += track
    }

    override fun addToQueue(track: Track) {
        queueRequests += track
    }

    override fun skipToQueueItem(index: Int) = Unit

    override fun removeQueueItem(index: Int) = Unit

    override fun restoreQueueItem(index: Int, track: Track) = Unit

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit

    override fun clearUpNext() = Unit

    override fun togglePlayPause() = Unit

    override fun seekTo(position: Duration) = Unit

    override fun skipToNext() = Unit

    override fun skipToPrevious() = Unit

    override fun toggleShuffle() = Unit

    override fun cycleRepeatMode() = Unit

    override fun retry() = Unit
}
