package com.flix.music.core.player

import com.flix.music.core.model.Track
import com.flix.music.core.player.stream.StreamResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [PlayerController] for the desktop: the queue lives in [PlayQueue] and one song at a
 * time plays on an [AudioEngine] (mpv), read from [urlOf] (the local audio server).
 * Works like the phone's: the next songs are resolved ahead, a failed song is retried
 * once with a freshly resolved URL and then skipped with [PlayerEvent.TrackSkipped],
 * and the last one stays in [PlaybackStatus.Error] with a retry action. [onFirstPlay]
 * runs once per song when it really starts playing (the history). Everything runs on
 * [scope]'s thread; engine signals are handed over to it.
 */
class DesktopPlayerController(
    engineFactory: ((AudioSignal) -> Unit) -> AudioEngine,
    private val urlOf: (trackId: String) -> String,
    private val streamResolver: StreamResolver,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val queue: PlayQueue = PlayQueue(),
    private val onFirstPlay: (Track) -> Unit = {},
) : PlayerController {

    private val engine: AudioEngine = engineFactory { signal -> scope.launch { onSignal(signal) } }

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 1)
    override val events: Flow<PlayerEvent> = _events.asSharedFlow()

    private var source: PlaybackSource? = null
    private var position: Duration = Duration.ZERO
    private var duration: Duration? = null
    private var paused = false
    private var loading = false
    private var buffering = false
    private var ended = false
    private var failed = false
    private var retriedCurrent = false
    private var publishedPosition: Duration = Duration.ZERO
    private var recordedCurrent = false

    /** A restored session waits, paused, until play: nothing is fetched at launch. */
    private var waitingToLoad = false

    override fun play(tracks: List<Track>, startIndex: Int, source: PlaybackSource, shuffle: Boolean) {
        if (tracks.isEmpty()) return
        this.source = source
        queue.replace(tracks, startIndex, shuffle)
        startCurrent()
    }

    override fun playNext(track: Track) = enqueue(track) { queue.playNext(track) }

    override fun addToQueue(track: Track) = enqueue(track) { queue.add(track) }

    /** Appends songs without touching what plays, e.g. autoplay's radio. */
    fun appendToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val queueHadEnded = ended
        val firstNewIndex = queue.size
        queue.addAll(tracks)
        if (queueHadEnded) {
            queue.moveTo(firstNewIndex)
            startCurrent()
        } else {
            resolveUpcoming()
            publish()
        }
    }

    override fun skipToQueueItem(index: Int) {
        if (index !in 0 until queue.size) return
        queue.moveTo(index)
        startCurrent()
    }

    override fun removeQueueItem(index: Int) = changeQueue { queue.remove(index) }

    override fun restoreQueueItem(index: Int, track: Track) = changeQueue { queue.restore(index, track) }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) = changeQueue { queue.move(fromIndex, toIndex) }

    override fun clearUpNext() = changeQueue { queue.clearUpNext() }

    override fun togglePlayPause() {
        if (queue.current == null) return
        if (ended || failed) return retry()
        if (waitingToLoad) return startCurrent(from = position)
        paused = !paused
        engine.setPaused(paused)
        publish()
    }

    override fun seekTo(position: Duration) {
        if (queue.current == null) return
        this.position = position
        if (waitingToLoad) return publish()
        engine.seek(position.inWholeMilliseconds / MILLIS_PER_SECOND)
        publish()
    }

    override fun skipToNext() {
        queue.nextIndex(userSkip = true)?.let(::skipToQueueItem)
    }

    /** Back to the start of the song after [RESTART_THRESHOLD], otherwise the previous song. */
    override fun skipToPrevious() {
        val previous = queue.previousIndex()
        if (position > RESTART_THRESHOLD || previous == null) seekTo(Duration.ZERO) else skipToQueueItem(previous)
    }

    override fun toggleShuffle() = changeQueue { queue.setShuffle(!queue.shuffleEnabled) }

    override fun cycleRepeatMode() = changeQueue {
        queue.repeatMode = when (queue.repeatMode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
    }

    /** After the queue ended it starts over; after an error the song resumes where it failed. */
    override fun retry() {
        val current = queue.current ?: return
        if (ended) {
            queue.moveTo(0)
            return startCurrent()
        }
        streamResolver.forget(current.id)
        retriedCurrent = false
        startCurrent(from = position)
    }

    fun queuedTrackIds(): Set<String> = queue.trackIds

    /** Where the music is now, to pick it up after Flix restarts; null with an empty queue. */
    fun session(): PlayerSession? = queue.snapshot()?.let { snapshot -> PlayerSession.of(snapshot, position, source) }

    /** Puts [session] back paused at its second, without loading the song until play. */
    fun restore(session: PlayerSession) {
        val snapshot = session.toSnapshot()
        if (snapshot.tracks.isEmpty()) return
        queue.restore(snapshot)
        source = session.source()
        position = session.position
        duration = queue.current?.duration
        paused = true
        loading = false
        ended = false
        failed = false
        recordedCurrent = true
        waitingToLoad = true
        publish()
    }

    /** Shuffle and repeat as they were last time ("Recordar aleatorio y repetir"). */
    fun restoreModes(shuffle: Boolean, repeatMode: RepeatMode) = changeQueue {
        queue.repeatMode = repeatMode
        queue.setShuffle(shuffle)
    }

    fun setVolume(percent: Int) = engine.setVolume(percent)

    fun release() = engine.close()

    private fun enqueue(track: Track, insert: () -> Unit) {
        if (queue.size == 0) {
            source = null
            queue.replace(listOf(track), startIndex = 0, shuffle = false)
            return startCurrent()
        }
        changeQueue(insert)
    }

    private fun changeQueue(change: () -> Unit) {
        change()
        resolveUpcoming()
        publish()
    }

    private fun startCurrent(from: Duration = Duration.ZERO) {
        val current = queue.current ?: return
        if (from == Duration.ZERO) recordedCurrent = false
        waitingToLoad = false
        position = from
        publishedPosition = from
        duration = current.duration
        paused = false
        loading = true
        buffering = false
        ended = false
        failed = false
        engine.load(urlOf(current.id), startSeconds = from.inWholeMilliseconds / MILLIS_PER_SECOND)
        resolveUpcoming()
        publish()
    }

    private fun onSignal(signal: AudioSignal) {
        when (signal) {
            AudioSignal.Loading, AudioSignal.Loaded -> Unit
            AudioSignal.Restarted -> {
                loading = false
                buffering = false
            }
            is AudioSignal.Position -> {
                position = signal.seconds.seconds
                if (abs((position - publishedPosition).inWholeMilliseconds) < POSITION_STEP_MILLIS) return
            }
            is AudioSignal.Duration -> duration = signal.seconds.seconds
            is AudioSignal.Paused -> paused = signal.paused
            is AudioSignal.Buffering -> buffering = signal.buffering
            AudioSignal.Finished -> return onFinished()
            is AudioSignal.Failed -> return onFailed()
        }
        publish()
    }

    private fun onFinished() {
        val next = queue.nextIndex(userSkip = false)
        if (next != null) {
            retriedCurrent = false
            queue.moveTo(next)
            return startCurrent()
        }
        ended = true
        position = duration ?: position
        publish()
    }

    private fun onFailed() {
        val current = queue.current ?: return
        when {
            !retriedCurrent -> {
                retriedCurrent = true
                streamResolver.forget(current.id)
                startCurrent(from = position)
            }
            queue.hasNext -> {
                retriedCurrent = false
                _events.tryEmit(PlayerEvent.TrackSkipped(current.title))
                skipToNext()
            }
            else -> {
                failed = true
                loading = false
                publish()
            }
        }
    }

    /** Resolves the next [PREFETCH_COUNT] songs on the network, so skipping starts at once. */
    private fun resolveUpcoming() {
        queue.upNext.take(PREFETCH_COUNT).forEach { item ->
            scope.launch(ioDispatcher) { streamResolver.resolve(item.track.id) }
        }
    }

    private fun publish() {
        val current = queue.current
        publishedPosition = position
        val newState = PlayerState(
            status = when {
                current == null -> PlaybackStatus.Idle
                failed -> PlaybackStatus.Error
                ended -> PlaybackStatus.Ended
                paused -> PlaybackStatus.Paused
                loading -> PlaybackStatus.Loading
                buffering -> PlaybackStatus.Buffering
                else -> PlaybackStatus.Playing
            },
            current = current,
            next = queue.nextIndex(userSkip = true)?.let(queue::get),
            upNext = queue.upNext,
            source = source,
            position = position,
            bufferedPosition = position,
            duration = duration,
            shuffleEnabled = queue.shuffleEnabled,
            repeatMode = queue.repeatMode,
            hasPrevious = queue.hasPrevious,
            hasNext = queue.hasNext,
        )
        _state.value = newState
        if (current != null && newState.status == PlaybackStatus.Playing && !recordedCurrent) {
            recordedCurrent = true
            onFirstPlay(current)
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000.0
        const val POSITION_STEP_MILLIS = 250L
        const val PREFETCH_COUNT = 2
        val RESTART_THRESHOLD = 3.seconds
    }
}
