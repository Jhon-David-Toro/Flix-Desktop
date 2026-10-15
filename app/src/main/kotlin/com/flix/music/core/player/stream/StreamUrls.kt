package com.flix.music.core.player.stream

private const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v="
private val WATCH_VIDEO_ID = Regex("^https://www\\.youtube\\.com/watch\\?v=([A-Za-z0-9_-]{11})$")
private val EXPIRE_PARAMETER = Regex("[?&]expire=(\\d+)")

/** The URI a queue item carries: the song's YouTube page, resolved to audio right before loading. */
internal fun youTubeWatchUrl(trackId: String): String = YOUTUBE_WATCH_URL + trackId

/** The YouTube id of a [youTubeWatchUrl], or null for any other URL (e.g. googlevideo). */
internal fun trackIdOfWatchUrl(uri: String): String? = WATCH_VIDEO_ID.find(uri)?.groupValues?.get(1)

/**
 * When a direct stream URL stops being usable. googlevideo states it in the `expire`
 * parameter (Unix seconds); a minute is kept as margin so a song never starts on a
 * URL about to die. URLs without it are trusted for [FALLBACK_LIFETIME_MILLIS].
 */
internal fun streamExpiresAtMillis(url: String, nowMillis: Long): Long {
    val expireSeconds = EXPIRE_PARAMETER.find(url)?.groupValues?.get(1)?.toLongOrNull()
    return if (expireSeconds != null) expireSeconds * 1_000 - EXPIRY_MARGIN_MILLIS else nowMillis + FALLBACK_LIFETIME_MILLIS
}

private const val EXPIRY_MARGIN_MILLIS = 60_000L
private const val FALLBACK_LIFETIME_MILLIS = 30 * 60_000L
