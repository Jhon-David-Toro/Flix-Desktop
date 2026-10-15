package com.flix.music.core.player

/** Plays one song at a time; the desktop player drives it and reacts to its [AudioSignal]s. */
interface AudioEngine : AutoCloseable {

    /** Replaces whatever plays with [url], from [startSeconds], paused or not. */
    fun load(url: String, startSeconds: Double = 0.0, paused: Boolean = false)

    fun setPaused(paused: Boolean)

    fun seek(seconds: Double)

    fun stop()

    /** 0–100. */
    fun setVolume(percent: Int)
}

/** What the engine reports while it plays, on its own thread. */
sealed interface AudioSignal {
    /** A new file started opening. */
    data object Loading : AudioSignal

    /** The file opened; [Duration] and positions follow. */
    data object Loaded : AudioSignal

    /** Playback (re)started after loading or seeking. */
    data object Restarted : AudioSignal

    data class Position(val seconds: Double) : AudioSignal

    data class Duration(val seconds: Double) : AudioSignal

    data class Paused(val paused: Boolean) : AudioSignal

    /** Waiting for the network mid-song. */
    data class Buffering(val buffering: Boolean) : AudioSignal

    /** The song played to its end. */
    data object Finished : AudioSignal

    data class Failed(val message: String) : AudioSignal
}
