package com.flix.music.core.player.mpv

import com.flix.music.core.player.mpv.Libmpv.Companion.END_FILE_REASON_EOF
import com.flix.music.core.player.mpv.Libmpv.Companion.END_FILE_REASON_ERROR
import com.flix.music.core.player.mpv.Libmpv.Companion.EVENT_END_FILE
import com.flix.music.core.player.mpv.Libmpv.Companion.EVENT_FILE_LOADED
import com.flix.music.core.player.mpv.Libmpv.Companion.EVENT_PLAYBACK_RESTART
import com.flix.music.core.player.mpv.Libmpv.Companion.EVENT_PROPERTY_CHANGE
import com.flix.music.core.player.mpv.Libmpv.Companion.EVENT_SHUTDOWN
import com.flix.music.core.player.mpv.Libmpv.Companion.EVENT_START_FILE
import com.flix.music.core.player.mpv.Libmpv.Companion.FORMAT_DOUBLE
import com.flix.music.core.player.mpv.Libmpv.Companion.FORMAT_FLAG
import com.sun.jna.Pointer
import java.io.File
import java.util.Locale

/** What mpv reports while it plays, delivered on mpv's own event thread. */
sealed interface MpvSignal {
    /** A new file started opening. */
    data object Loading : MpvSignal

    /** The file opened; [MpvSignal.Duration] and positions follow. */
    data object Loaded : MpvSignal

    /** Playback (re)started after loading or seeking. */
    data object Restarted : MpvSignal

    data class Position(val seconds: Double) : MpvSignal

    data class Duration(val seconds: Double) : MpvSignal

    data class Paused(val paused: Boolean) : MpvSignal

    /** Waiting for the network mid-song. */
    data class Buffering(val buffering: Boolean) : MpvSignal

    /** The song played to its end. */
    data object Finished : MpvSignal

    data class Failed(val message: String) : MpvSignal
}

/**
 * One libmpv instance set up for music: no video, no yt-dlp, the whole song kept in
 * memory once loaded (so seeking back is instant), and named "Flix" in the Windows
 * volume mixer. Commands can be called from any thread; [onSignal] runs on mpv's.
 */
class MpvEngine(libraryFile: File, private val onSignal: (MpvSignal) -> Unit) : AutoCloseable {

    private val mpv = Libmpv.load(libraryFile)
    private val handle: Pointer = requireNotNull(mpv.mpv_create()) { "libmpv could not create a player" }

    @Volatile
    private var running = true

    init {
        OPTIONS.forEach { (name, value) -> mpv.mpv_set_option_string(handle, name, value) }
        check(mpv.mpv_initialize(handle) >= 0) { "libmpv could not start" }
        OBSERVED.forEachIndexed { index, (name, format) -> mpv.mpv_observe_property(handle, index.toLong(), name, format) }
        Thread(::pumpEvents, "flix-mpv-events").apply { isDaemon = true }.start()
    }

    /** Replaces whatever plays with [url], from [startSeconds], paused or not. */
    fun load(url: String, startSeconds: Double = 0.0, paused: Boolean = false) {
        val options = "start=${format(startSeconds)},pause=${if (paused) "yes" else "no"}"
        command("loadfile", url, "replace", "-1", options)
    }

    fun setPaused(paused: Boolean) {
        mpv.mpv_set_property_string(handle, "pause", if (paused) "yes" else "no")
    }

    fun seek(seconds: Double) = command("seek", format(seconds), "absolute+exact")

    fun stop() = command("stop")

    /** 0–100. */
    fun setVolume(percent: Int) {
        mpv.mpv_set_property_string(handle, "volume", percent.coerceIn(0, MAX_VOLUME).toString())
    }

    override fun close() {
        running = false
        mpv.mpv_terminate_destroy(handle)
    }

    private fun command(vararg args: String) {
        val result = mpv.mpv_command(handle, arrayOf(*args, null))
        if (result < 0) onSignal(MpvSignal.Failed("${args.first()}: ${mpv.mpv_error_string(result)}"))
    }

    private fun pumpEvents() {
        while (running) {
            val event = MpvEvent(mpv.mpv_wait_event(handle, WAIT_FOREVER))
            when (event.event_id) {
                EVENT_SHUTDOWN -> return
                EVENT_START_FILE -> onSignal(MpvSignal.Loading)
                EVENT_FILE_LOADED -> onSignal(MpvSignal.Loaded)
                EVENT_PLAYBACK_RESTART -> onSignal(MpvSignal.Restarted)
                EVENT_END_FILE -> event.data?.let(::MpvEventEndFile)?.let(::onEndFile)
                EVENT_PROPERTY_CHANGE -> event.data?.let(::MpvEventProperty)?.let(::onProperty)
            }
        }
    }

    private fun onEndFile(endFile: MpvEventEndFile) {
        when (endFile.reason) {
            END_FILE_REASON_EOF -> onSignal(MpvSignal.Finished)
            END_FILE_REASON_ERROR -> onSignal(MpvSignal.Failed(mpv.mpv_error_string(endFile.error)))
        }
    }

    private fun onProperty(property: MpvEventProperty) {
        val data = property.data ?: return
        when (property.name) {
            "time-pos" -> onSignal(MpvSignal.Position(data.getDouble(0)))
            "duration" -> onSignal(MpvSignal.Duration(data.getDouble(0)))
            "pause" -> onSignal(MpvSignal.Paused(data.getInt(0) != 0))
            "paused-for-cache" -> onSignal(MpvSignal.Buffering(data.getInt(0) != 0))
        }
    }

    private fun format(seconds: Double): String = String.format(Locale.ROOT, "%.3f", seconds)

    private companion object {
        const val WAIT_FOREVER = -1.0
        const val MAX_VOLUME = 100
        val OPTIONS = listOf(
            "video" to "no",
            "audio-display" to "no",
            "ytdl" to "no",
            "terminal" to "no",
            "idle" to "yes",
            "keep-open" to "no",
            "cache" to "yes",
            "demuxer-max-bytes" to "64MiB",
            "demuxer-max-back-bytes" to "64MiB",
            "demuxer-readahead-secs" to "900",
            "audio-client-name" to "Flix",
            "gapless-audio" to "weak",
        )
        val OBSERVED = listOf(
            "time-pos" to FORMAT_DOUBLE,
            "duration" to FORMAT_DOUBLE,
            "pause" to FORMAT_FLAG,
            "paused-for-cache" to FORMAT_FLAG,
        )
    }
}
