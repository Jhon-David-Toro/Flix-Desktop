package com.flix.music.core.data.youtube

import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Reads the songs out of a YouTube Music `search` response (songs filter), with the
 * same rules as ytmusicapi: the second column lists artists, album and duration
 * separated by " • ", where runs linking to `MPRE…` are albums and other links or
 * plain text are artists.
 */
internal fun parseSongSearch(response: JsonElement): List<Track> =
    response.path("contents", "tabbedSearchResultsRenderer", "tabs").items().firstOrNull()
        .path("tabRenderer", "content", "sectionListRenderer", "contents").items()
        .flatMap { section -> section.path("musicShelfRenderer", "contents").items() }
        .mapNotNull { entry -> entry.field("musicResponsiveListItemRenderer")?.toTrack() }

/**
 * Reads the songs of a YouTube Music `next` response for a radio ("RDAMVM" + id),
 * seed song first. Their byline follows the search rules; videos also list views
 * and likes, which are skipped.
 */
internal fun parseRadio(response: JsonElement): List<Track> =
    response.path("contents", "singleColumnMusicWatchNextResultsRenderer", "tabbedRenderer", "watchNextTabbedResultsRenderer", "tabs")
        .items().firstOrNull()
        .path("tabRenderer", "content", "musicQueueRenderer", "content", "playlistPanelRenderer", "contents").items()
        .mapNotNull { entry -> entry.field("playlistPanelVideoRenderer")?.toRadioTrack() }

/** Asks Google's image CDN for the 544px version of a thumbnail, as the design uses. */
internal fun upscaledThumbnail(url: String?): String? = url?.replaceFirst(THUMBNAIL_SIZE, "=w$ARTWORK_SIZE-h$ARTWORK_SIZE")

private fun JsonElement.toTrack(): Track? {
    val id = path("playlistItemData", "videoId").text()
        ?: path("overlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer", "playNavigationEndpoint", "watchEndpoint", "videoId").text()
        ?: return null
    val columns = field("flexColumns").items().map { column ->
        column.path("musicResponsiveListItemFlexColumnRenderer", "text", "runs").items()
    }
    val title = columns.firstOrNull()?.firstOrNull().field("text").text() ?: return null
    val details = parseSongRuns(columns.getOrNull(1).orEmpty())
    val thumbnailUrl = path("thumbnail", "musicThumbnailRenderer", "thumbnail", "thumbnails").items()
        .lastOrNull().field("url").text()
    return Track(
        id = id,
        title = title,
        artists = details.artists,
        album = details.album,
        duration = details.duration,
        artworkUrl = upscaledThumbnail(thumbnailUrl),
    )
}

private fun JsonElement.toRadioTrack(): Track? {
    val id = field("videoId").text() ?: return null
    val title = path("title", "runs").items().firstOrNull().field("text").text() ?: return null
    val details = parseSongRuns(path("longBylineText", "runs").items())
    val duration = path("lengthText", "runs").items().firstOrNull().field("text").text()
        ?.takeIf(DURATION_TEXT::matches)
        ?.let(::parseDuration)
    val thumbnailUrl = path("thumbnail", "thumbnails").items().lastOrNull().field("url").text()
    return Track(
        id = id,
        title = title,
        artists = details.artists,
        album = details.album,
        duration = duration ?: details.duration,
        artworkUrl = upscaledThumbnail(thumbnailUrl),
    )
}

private class SongRuns(val artists: List<ArtistRef>, val album: AlbumRef?, val duration: Duration?)

/** Even runs carry data; odd runs are separators (" • ", " & ", ", "). */
private fun parseSongRuns(runs: List<JsonElement>): SongRuns {
    val artists = mutableListOf<ArtistRef>()
    var album: AlbumRef? = null
    var duration: Duration? = null
    runs.filterIndexed { index, _ -> index % 2 == 0 }.forEach { run ->
        val text = run.field("text").text() ?: return@forEach
        val browseId = run.path("navigationEndpoint", "browseEndpoint", "browseId").text()
        when {
            run.field("navigationEndpoint") != null ->
                if (browseId != null && (browseId.startsWith(ALBUM_ID_PREFIX) || RELEASE_DETAIL in browseId)) {
                    album = AlbumRef(browseId, text)
                } else {
                    artists += ArtistRef(browseId, text)
                }
            DURATION_TEXT.matches(text) -> duration = parseDuration(text)
            YEAR_TEXT.matches(text) || VIEWS_TEXT.matches(text) -> Unit
            else -> artists += ArtistRef(null, text)
        }
    }
    return SongRuns(artists, album, duration)
}

/** "6:58" or "1:02:03" in seconds. */
private fun parseDuration(text: String): Duration =
    text.split(":").fold(0L) { total, part -> total * SECONDS_PER_MINUTE + part.toLong() }.seconds

private fun JsonElement?.field(name: String): JsonElement? = (this as? JsonObject)?.get(name)

private fun JsonElement?.path(vararg names: String): JsonElement? = names.fold(this) { current, name -> current.field(name) }

private fun JsonElement?.items(): List<JsonElement> = (this as? JsonArray).orEmpty()

private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content

private const val ALBUM_ID_PREFIX = "MPRE"
private const val RELEASE_DETAIL = "release_detail"
private const val ARTWORK_SIZE = 544
private const val SECONDS_PER_MINUTE = 60
private val THUMBNAIL_SIZE = Regex("=w\\d+-h\\d+")
private val DURATION_TEXT = Regex("^(\\d+:)*\\d+:\\d+$")
private val YEAR_TEXT = Regex("^\\d{4}$")
private val VIEWS_TEXT = Regex("^[\\d.,]+[KMB]? (plays|views|likes)$")
