package com.flix.music.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** One line of lyrics; [start] is set only for synced lyrics. */
data class LyricLine(val start: Duration?, val text: String)

/** Lyrics of a song, synced to the music (design `lyricssync`) or plain. */
data class Lyrics(val lines: List<LyricLine>) {

    val isSynced: Boolean get() = lines.isNotEmpty() && lines.all { line -> line.start != null }

    /** The line being sung at [position], or null before the first one (or for plain lyrics). */
    fun activeLineIndex(position: Duration): Int? {
        if (!isSynced) return null
        return lines.indexOfLast { line -> (line.start ?: Duration.INFINITE) <= position }.takeIf { index -> index >= 0 }
    }

    companion object {

        /**
         * Reads LRC ("[01:23.45] text"). Lines with several stamps repeat; metadata tags
         * such as "[ar: …]" and empty lines are dropped.
         */
        fun fromLrc(lrc: String): Lyrics = Lyrics(
            lrc.lineSequence()
                .flatMap { rawLine ->
                    val stamps = LRC_STAMP.findAll(rawLine).toList()
                    val text = rawLine.replace(LRC_TAG, "").trim()
                    stamps.map { stamp -> LyricLine(stamp.toDuration(), text) }
                }
                .filter { line -> line.text.isNotEmpty() }
                .sortedBy { line -> line.start }
                .toList(),
        )

        fun fromPlainText(text: String): Lyrics = Lyrics(
            text.lines().map(String::trim).filter(String::isNotEmpty).map { line -> LyricLine(null, line) },
        )

        private fun MatchResult.toDuration(): Duration {
            val (minutes, seconds, fraction) = destructured
            val fractionMillis = fraction.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            return (minutes.toLong() * 60_000 + seconds.toLong() * 1_000 + fractionMillis).milliseconds
        }

        private val LRC_STAMP = Regex("\\[(\\d+):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
        private val LRC_TAG = Regex("\\[[^]]*]")
    }
}
