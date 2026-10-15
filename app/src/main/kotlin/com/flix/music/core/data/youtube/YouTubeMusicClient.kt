package com.flix.music.core.data.youtube

import com.flix.music.core.model.Track
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Clock
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** YouTube Music answered with an HTTP error, e.g. 429 when it rate-limits. */
class YouTubeMusicException(val statusCode: Int) : IOException("YouTube Music answered HTTP $statusCode")

/**
 * YouTube Music's own web API ("InnerTube"), called from the phone without an
 * account, exactly like ytmusicapi does: search and radios answer anonymously. Blocking;
 * call it off the main thread.
 */
class YouTubeMusicClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val clock: Clock,
) {

    /** Songs for [query], best match first (about 20). */
    fun searchSongs(query: String): List<Track> =
        parseSongSearch(
            post(SEARCH_URL) {
                put("query", query)
                put("params", SONGS_FILTER)
            },
        )

    /** The song's radio: similar songs to keep playing after it (about 50, the song itself first). */
    fun radio(trackId: String): List<Track> =
        parseRadio(
            post(NEXT_URL) {
                put("videoId", trackId)
                put("playlistId", RADIO_PLAYLIST_PREFIX + trackId)
                put("isAudioOnly", true)
            },
        )

    /** Calls an InnerTube endpoint as the YouTube Music website does, with [fields] next to the client context. */
    private fun post(url: String, fields: JsonObjectBuilder.() -> Unit): JsonElement {
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", CLIENT_NAME)
                    put("clientVersion", clientVersion())
                    put("hl", LANGUAGE)
                }
            }
            fields()
        }
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("User-Agent", USER_AGENT)
            .header("Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw YouTubeMusicException(response.code)
            return json.parseToJsonElement(response.body.string())
        }
    }

    /** WEB_REMIX versions follow the date, as the website sends them ("1.20261013.01.00"). */
    private fun clientVersion(): String = "1.${CLIENT_DATE.format(clock.instant().atOffset(ZoneOffset.UTC))}.01.00"

    private companion object {
        const val ORIGIN = "https://music.youtube.com"
        const val SEARCH_URL = "$ORIGIN/youtubei/v1/search?prettyPrint=false"
        const val NEXT_URL = "$ORIGIN/youtubei/v1/next?prettyPrint=false"
        const val RADIO_PLAYLIST_PREFIX = "RDAMVM"
        const val CLIENT_NAME = "WEB_REMIX"
        const val LANGUAGE = "en"
        const val SONGS_FILTER = "EgWKAQIIAWoMEA4QChADEAQQCRAF"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        val CLIENT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
