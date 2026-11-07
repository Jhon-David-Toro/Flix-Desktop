package com.flix.music.core.player

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoplayTest {

    private fun track(id: String) = Track(id, "Canción $id", listOf(ArtistRef(null, "Artista")), null, null, null)

    @Test
    fun `añade solo lo que no está en la cola, sin repetir`() {
        val radio = listOf(track("a"), track("b"), track("a"), track("c"))

        assertEquals(listOf("b", "c"), autoplayAdditions(radio, queued = listOf(track("a"))).map { song -> song.id })
    }

    @Test
    fun `añade como mucho un lote cada vez`() {
        val radio = (1..40).map { number -> track("$number") }

        assertEquals(AUTOPLAY_BATCH_SIZE, autoplayAdditions(radio, queued = emptyList()).size)
    }

    @Test
    fun `no añade otra versión de una canción ya en la cola`() {
        val live = Track("x", "Canción a (Live)", listOf(ArtistRef(null, "Artista")), null, null, null)

        assertEquals(listOf("b"), autoplayAdditions(listOf(live, track("b")), queued = listOf(track("a"))).map { song -> song.id })
    }
}
