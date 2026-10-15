package com.flix.music.core.data.database

import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class StoredTrackTest {

    @Test
    fun `una canción guardada vuelve igual, con todos sus artistas y su álbum`() {
        val track = Track(
            id = "abc123def45",
            title = "Contigo Na Más - Remix",
            artists = listOf(ArtistRef("UC1", "Luar La L"), ArtistRef(null, "Dei V")),
            album = AlbumRef("MPRE1", "Contigo Na Más"),
            duration = 467.seconds,
            artworkUrl = "https://img/cover.jpg",
        )

        assertEquals(track, track.toStoredTrack().toTrack())
    }

    @Test
    fun `una canción sin álbum ni duración también se guarda`() {
        val track = Track("abc123def45", "Single", listOf(ArtistRef(null, "Artista")), null, null, null)

        assertEquals(track, track.toStoredTrack().toTrack())
    }
}
