package com.flix.music.core.player

import com.flix.music.core.common.Log
import com.flix.music.core.data.database.StoredTrack
import com.flix.music.core.data.database.toStoredTrack
import com.flix.music.core.data.database.toTrack
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Where the music was when Flix closed: the queue, the song and the second it was at. */
@Serializable
data class PlayerSession(
    val tracks: List<StoredTrack>,
    val currentIndex: Int,
    val positionMillis: Long,
    val repeatMode: String,
    val unshuffledOrder: List<Int>? = null,
    val sourceKind: String? = null,
    val sourceName: String? = null,
) {
    val position: Duration get() = positionMillis.milliseconds

    fun toSnapshot(): PlayQueue.Snapshot = PlayQueue.Snapshot(
        tracks = tracks.map(StoredTrack::toTrack),
        currentIndex = currentIndex,
        repeatMode = RepeatMode.entries.firstOrNull { mode -> mode.name == repeatMode } ?: RepeatMode.Off,
        unshuffledOrder = unshuffledOrder,
    )

    /** The "Reproduciendo desde …" it had. */
    fun source(): PlaybackSource? {
        val name = sourceName.orEmpty()
        return when (sourceKind) {
            "search" -> PlaybackSource.Search(name)
            "genre" -> PlaybackSource.Genre(name)
            "favorites" -> PlaybackSource.Favorites
            "history" -> PlaybackSource.History
            "downloads" -> PlaybackSource.Downloads
            "playlist" -> PlaybackSource.Playlist(name)
            "artist" -> PlaybackSource.Artist(name)
            "album" -> PlaybackSource.Album(name)
            "song" -> PlaybackSource.Song(name)
            else -> null
        }
    }

    companion object {
        fun of(snapshot: PlayQueue.Snapshot, position: Duration, source: PlaybackSource?): PlayerSession {
            val (kind, name) = when (source) {
                is PlaybackSource.Search -> "search" to source.query
                is PlaybackSource.Genre -> "genre" to source.name
                PlaybackSource.Favorites -> "favorites" to null
                PlaybackSource.History -> "history" to null
                PlaybackSource.Downloads -> "downloads" to null
                is PlaybackSource.Playlist -> "playlist" to source.name
                is PlaybackSource.Artist -> "artist" to source.name
                is PlaybackSource.Album -> "album" to source.name
                is PlaybackSource.Song -> "song" to source.title
                null -> null to null
            }
            return PlayerSession(
                tracks = snapshot.tracks.map { track -> track.toStoredTrack() },
                currentIndex = snapshot.currentIndex,
                positionMillis = position.inWholeMilliseconds,
                repeatMode = snapshot.repeatMode.name,
                unshuffledOrder = snapshot.unshuffledOrder,
                sourceKind = kind,
                sourceName = name,
            )
        }
    }
}

/** Keeps [PlayerSession] in `session.json`; a missing or broken file means nothing to restore. */
class PlayerSessionStore(private val file: File, private val json: Json) {

    fun read(): PlayerSession? = runCatching { json.decodeFromString<PlayerSession>(file.readText()) }.getOrNull()

    fun write(session: PlayerSession?) {
        runCatching {
            if (session == null) {
                file.delete()
            } else {
                val temporary = File(file.parentFile, "${file.name}.tmp")
                temporary.writeText(json.encodeToString(session))
                file.delete()
                temporary.renameTo(file)
            }
        }.onFailure { failure -> Log.w("PlayerSession", "Could not save the session: ${failure.message}") }
    }
}
