package com.flix.music.core.player.stream

/**
 * Turns a YouTube id into a direct audio URL resolved on this phone, so playback
 * comes from the phone's own network instead of the server's datacenter IP, which
 * YouTube answers with "Sign in to confirm you're not a bot".
 */
interface StreamResolver {

    /**
     * The direct audio URL, or null when YouTube would not give one. Blocking, about
     * 1–3 s the first time, cached until the URL expires.
     */
    fun resolve(trackId: String): String?

    /** Drops a cached URL that googlevideo rejected, so the next attempt resolves again. */
    fun forget(trackId: String)
}
