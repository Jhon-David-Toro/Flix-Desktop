package com.flix.music.core.data.youtube

import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Uses a real YouTube Music search response ("fade to black metallica", songs filter), trimmed to three songs. */
class YouTubeMusicParserTest {

    private val response = Json.parseToJsonElement(
        requireNotNull(javaClass.getResource("/youtube_music/search_songs.json")).readText(),
    )

    private val songs = parseSongSearch(response)

    @Test
    fun `lee id, título, artista, álbum y duración de cada canción`() {
        val fadeToBlack = songs.first()

        assertEquals("9HZ_tx8aWuA", fadeToBlack.id)
        assertEquals("Fade To Black", fadeToBlack.title)
        assertEquals(listOf(ArtistRef("UCGexNm_Kw4rdQjLxmpb2EKw", "Metallica")), fadeToBlack.artists)
        assertEquals(AlbumRef("MPREb_vbNohrhu6ae", "Ride The Lightning"), fadeToBlack.album)
        assertEquals(418.seconds, fadeToBlack.duration)
    }

    @Test
    fun `varios artistas, también los que no tienen canal`() {
        val promNight = songs[1]
        assertEquals(listOf("Paul Zaza", "Carl Zittrer"), promNight.artists.map { artist -> artist.name })
        assertTrue(promNight.artists.first().id?.startsWith("UC") == true)
        assertEquals(null, promNight.artists.last().id)
        assertEquals(listOf("Pickin' On Series", "Iron Horse"), songs[2].artists.map { artist -> artist.name })
    }

    @Test
    fun `pide la carátula a 544 px`() {
        assertTrue(songs.all { song -> song.artworkUrl?.contains("=w544-h544") == true })
    }

    @Test
    fun `una respuesta sin resultados da una lista vacía`() {
        assertEquals(emptyList<Any>(), parseSongSearch(JsonObject(emptyMap())))
    }
}
