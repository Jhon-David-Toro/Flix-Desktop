package com.flix.music.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PlaylistTest {

    private fun playlistWithCovers(vararg covers: String?) = Playlist(
        id = 1,
        name = "Noches de ruta",
        description = null,
        tracks = covers.mapIndexed { index, cover ->
            Track(index.toString(), "Canción $index", listOf(ArtistRef(null, "Artista")), null, null, cover)
        },
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `con 4 portadas distintas o más la portada es un mosaico de las 4 primeras`() {
        val playlist = playlistWithCovers("a", "b", "a", null, "c", "d", "e")

        assertEquals(listOf("a", "b", "c", "d"), playlist.coverUrls)
    }

    @Test
    fun `con menos de 4 portadas usa solo la primera`() {
        assertEquals(listOf("a"), playlistWithCovers(null, "a", "b").coverUrls)
    }

    @Test
    fun `sin portadas no hay mosaico`() {
        assertEquals(emptyList<String>(), playlistWithCovers(null).coverUrls)
    }
}
