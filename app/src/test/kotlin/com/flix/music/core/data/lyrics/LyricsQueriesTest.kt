package com.flix.music.core.data.lyrics

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsQueriesTest {

    private fun track(title: String, artist: String) = Track("id", title, listOf(ArtistRef(null, artist)), null, null, null)

    @Test
    fun `una canción limpia se busca tal cual`() {
        assertEquals(listOf(LyricsQuery("Fade To Black", "Metallica")), lyricsQueries(track("Fade To Black", "Metallica")))
    }

    @Test
    fun `quita los adornos de YouTube y el feat`() {
        val queries = lyricsQueries(track("Fade To Black (Remastered) [Official Audio] feat. Alguien", "Metallica"))

        assertEquals(LyricsQuery("Fade To Black", "Metallica"), queries.last())
    }

    @Test
    fun `separa artista y título en los vídeos tipo Artista - Canción`() {
        val queries = lyricsQueries(track("Luis Fonsi - Despacito ft. Daddy Yankee", "LuisFonsiVEVO"))

        assertEquals(LyricsQuery("Despacito", "Luis Fonsi"), queries.last())
    }

    @Test
    fun `quita el sufijo Topic de los canales automáticos`() {
        assertEquals("Bad Bunny", lyricsQueries(track("Tití Me Preguntó", "Bad Bunny - Topic")).first().artist)
    }
}
