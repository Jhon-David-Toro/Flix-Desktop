package com.flix.music.feature.player

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import com.flix.music.core.player.QueueItem
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueKeysTest {

    private fun item(index: Int, id: String) =
        QueueItem(index, Track(id, "Canción $id", listOf(ArtistRef(null, "Artista")), null, null, null))

    @Test
    fun `la misma canción dos veces en la cola tiene dos filas distintas`() {
        val keys = queueKeys(listOf(item(1, "a"), item(2, "b"), item(3, "a")))

        assertEquals(listOf("track-a#1", "track-b#1", "track-a#2"), keys)
    }

    @Test
    fun `la clave no depende de la posición en la cola`() {
        val before = queueKeys(listOf(item(1, "a"), item(2, "b")))
        val after = queueKeys(listOf(item(1, "b"), item(2, "a")))

        assertEquals(before.toSet(), after.toSet())
    }
}
