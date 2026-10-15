package com.flix.music.core.model

fun List<Track>.distinctArtists(): List<Artist> =
    flatMap { track ->
        track.artists.mapNotNull { artistRef ->
            artistRef.id?.let { artistId ->
                Artist(id = artistId, name = artistRef.name, artworkUrl = track.artworkUrl)
            }
        }
    }.distinctBy { it.id }

fun List<Track>.distinctAlbums(): List<Album> =
    mapNotNull { track ->
        track.album?.let { albumRef ->
            albumRef.id?.let { albumId ->
                Album(
                    id = albumId,
                    name = albumRef.name,
                    artistName = track.artistNames,
                    artworkUrl = track.artworkUrl,
                )
            }
        }
    }.distinctBy { it.id }

/**
 * Songs by the artist, derived from search results (there is no artist endpoint).
 * Matches the channel id when known, the name otherwise.
 */
fun List<Track>.byArtist(artistId: String?, artistName: String): List<Track> = filter { track ->
    track.artists.any { artist ->
        if (artistId != null && artist.id != null) artist.id == artistId else artist.name.equals(artistName, ignoreCase = true)
    }
}

/**
 * Songs of the album found in search results (there is no album endpoint), so the
 * album screen warns that some may be missing.
 */
fun List<Track>.fromAlbum(albumId: String?, albumName: String): List<Track> = filter { track ->
    val album = track.album ?: return@filter false
    if (albumId != null && album.id != null) album.id == albumId else album.name.equals(albumName, ignoreCase = true)
}
