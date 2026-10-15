package com.flix.music.core.data.youtube

import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Uses a real YouTube Music `next` response for the radio of "Fade To Black", trimmed to three songs. */
class YouTubeMusicRadioParserTest {

    private val response = Json.parseToJsonElement(
        requireNotNull(javaClass.getResource("/youtube_music/next_radio.json")).readText(),
    )

    private val radio = parseRadio(response)

    @Test
    fun `la radio empieza por la canción semilla y sigue con parecidas`() {
        assertEquals(listOf("9HZ_tx8aWuA", "E0ozmU9cJDg", "m7nwbJLO9qo"), radio.map { track -> track.id })
    }

    @Test
    fun `lee título, artista, álbum y duración, sin el año`() {
        val paranoid = radio[2]

        assertEquals("Paranoid (2009 Remaster)", paranoid.title)
        assertEquals(listOf(ArtistRef("UCLCELUuoHbkUxZ9EMHTYebg", "Black Sabbath")), paranoid.artists)
        assertEquals(AlbumRef("MPREb_nVJN3OahC9r", "Paranoid"), paranoid.album)
        assertEquals(168.seconds, paranoid.duration)
    }

    @Test
    fun `una respuesta sin cola no da canciones`() {
        assertEquals(emptyList<Any>(), parseRadio(JsonObject(emptyMap())))
    }
}
