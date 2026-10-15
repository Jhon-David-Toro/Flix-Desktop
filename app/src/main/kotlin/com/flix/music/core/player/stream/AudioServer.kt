package com.flix.music.core.player.stream

import com.flix.music.core.common.Log
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * A small HTTP server on 127.0.0.1 that the desktop player reads songs from, the PC's
 * counterpart of the phone's `ResolvingDataSource`. `/track/<id>` resolves the song
 * with [streamResolver] and relays googlevideo's audio, forwarding the player's
 * `Range` so seeking works, and asking for `bytes=0-` when the player sends none,
 * since googlevideo throttles requests without a Range. A URL googlevideo rejects is
 * resolved again once.
 */
class AudioServer(
    private val okHttpClient: OkHttpClient,
    private val streamResolver: StreamResolver,
) {

    private val server: HttpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
        createContext(TRACK_PATH) { exchange -> exchange.use(::serveTrack) }
        executor = Executors.newCachedThreadPool { runnable -> Thread(runnable, "flix-audio").apply { isDaemon = true } }
        start()
    }

    /** Where the player reads [trackId] from. JavaFX picks the format by the extension. */
    fun urlOf(trackId: String): String = "http://127.0.0.1:${server.address.port}$TRACK_PATH$trackId$AUDIO_EXTENSION"

    fun stop() = server.stop(0)

    private fun serveTrack(exchange: HttpExchange) {
        val trackId = exchange.requestURI.path.removePrefix(TRACK_PATH).removeSuffix(AUDIO_EXTENSION)
        if (!TRACK_ID.matches(trackId)) return exchange.sendResponseHeaders(NOT_FOUND, NO_BODY)
        val playerRange = exchange.requestHeaders.getFirst(RANGE_HEADER)
        try {
            relay(exchange, trackId, playerRange, retryOnRejection = true)
        } catch (exception: IOException) {
            Log.w(TAG, "Could not serve $trackId: ${exception.message}")
        }
    }

    /**
     * A player asking for the whole file (no Range) gets a plain 200, as players expect,
     * although googlevideo is asked for `bytes=0-`.
     */
    private fun relay(exchange: HttpExchange, trackId: String, playerRange: String?, retryOnRejection: Boolean) {
        val directUrl = streamResolver.resolve(trackId) ?: return exchange.sendResponseHeaders(BAD_GATEWAY, NO_BODY)
        val request = Request.Builder().url(directUrl).header(RANGE_HEADER, playerRange ?: FULL_RANGE).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == FORBIDDEN && retryOnRejection) {
                streamResolver.forget(trackId)
                return relay(exchange, trackId, playerRange, retryOnRejection = false)
            }
            if (!response.isSuccessful) return exchange.sendResponseHeaders(BAD_GATEWAY, NO_BODY)
            response.header(CONTENT_TYPE)?.let { value -> exchange.responseHeaders.set(CONTENT_TYPE, value) }
            if (playerRange != null) response.header(CONTENT_RANGE)?.let { value -> exchange.responseHeaders.set(CONTENT_RANGE, value) }
            exchange.responseHeaders.set("Accept-Ranges", "bytes")
            val status = if (playerRange == null) OK else response.code
            if (exchange.requestMethod == HEAD) {
                totalLengthOf(response)?.let { total -> exchange.responseHeaders.set(CONTENT_LENGTH, total.toString()) }
                return exchange.sendResponseHeaders(status, NO_BODY)
            }
            exchange.sendResponseHeaders(status, response.body.contentLength().takeIf { length -> length >= 0 } ?: CHUNKED)
            exchange.responseBody.use { output -> response.body.byteStream().copyTo(output) }
        }
    }

    /** The whole file's size, which JavaFX reads from HEAD to know it can seek. */
    private fun totalLengthOf(response: okhttp3.Response): Long? =
        response.header(CONTENT_RANGE)?.substringAfterLast('/')?.toLongOrNull()
            ?: response.body.contentLength().takeIf { length -> length >= 0 }

    private companion object {
        const val TAG = "AudioServer"
        const val TRACK_PATH = "/track/"
        const val AUDIO_EXTENSION = ".m4a"
        const val RANGE_HEADER = "Range"
        const val FULL_RANGE = "bytes=0-"
        const val HEAD = "HEAD"
        const val CONTENT_TYPE = "Content-Type"
        const val CONTENT_RANGE = "Content-Range"
        const val CONTENT_LENGTH = "Content-Length"
        const val OK = 200
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val BAD_GATEWAY = 502
        const val NO_BODY = -1L
        const val CHUNKED = 0L
        val TRACK_ID = Regex("^[A-Za-z0-9_-]{11}$")
    }
}
