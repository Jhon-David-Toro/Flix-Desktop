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

        assertEquals(listOf("b", "c"), autoplayAdditions(radio, queuedIds = setOf("a")).map { song -> song.id })
    }

    @Test
    fun `añade como mucho un lote cada vez`() {
        val radio = (1..40).map { number -> track("$number") }

        assertEquals(AUTOPLAY_BATCH_SIZE, autoplayAdditions(radio, queuedIds = emptySet()).size)
    }
}
