package com.flix.music.core.data.database

import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Snapshot of a [Track] embedded in library tables, so favorites and history render
 * offline without asking the server. Artists are stored as a JSON list.
 */
@Serializable
data class StoredTrack(
    val trackId: String,
    val title: String,
    val artistsJson: String,
    val albumId: String?,
    val albumName: String?,
    val durationSeconds: Long?,
    val artworkUrl: String?,
)

@Serializable
private data class StoredArtist(val id: String?, val name: String)

internal fun Track.toStoredTrack(): StoredTrack = StoredTrack(
    trackId = id,
    title = title,
    artistsJson = Json.encodeToString(artists.map { artist -> StoredArtist(artist.id, artist.name) }),
    albumId = album?.id,
    albumName = album?.name,
    durationSeconds = duration?.inWholeSeconds,
    artworkUrl = artworkUrl,
)

internal fun StoredTrack.toTrack(): Track = Track(
    id = trackId,
    title = title,
    artists = Json.decodeFromString<List<StoredArtist>>(artistsJson).map { artist -> ArtistRef(artist.id, artist.name) },
    album = albumName?.let { name -> AlbumRef(albumId, name) },
    duration = durationSeconds?.seconds,
    artworkUrl = artworkUrl,
)
