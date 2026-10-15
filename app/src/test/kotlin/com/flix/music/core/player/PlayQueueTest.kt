package com.flix.music.core.player

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlayQueueTest {

    private fun track(id: String) = Track(id, "Canción $id", listOf(ArtistRef(null, "Artista")), null, null, null)

    private val songs = listOf("a", "b", "c", "d").map(::track)

    private fun PlayQueue.ids(): List<String> = (0 until size).map { index -> get(index).id }

    @Test
    fun `empieza en la canción elegida y la cola muestra lo que sigue`() {
        val queue = PlayQueue().apply { replace(songs, startIndex = 1, shuffle = false) }

        assertEquals("b", queue.current?.id)
        assertEquals(listOf("c", "d"), queue.upNext.map { item -> item.track.id })
        assertEquals(listOf(2, 3), queue.upNext.map(QueueItem::index))
    }

    @Test
    fun `al final se para salvo con repetir todo, que vuelve al principio`() {
        val queue = PlayQueue().apply { replace(songs, startIndex = 3, shuffle = false) }
        assertNull(queue.nextIndex(userSkip = false))
        assertFalse(queue.hasNext)

        queue.repeatMode = RepeatMode.All

        assertEquals(0, queue.nextIndex(userSkip = false))
    }

    @Test
    fun `repetir una repite al terminar, pero saltar pasa a la siguiente`() {
        val queue = PlayQueue().apply {
            replace(songs, startIndex = 0, shuffle = false)
            repeatMode = RepeatMode.One
        }

        assertEquals(0, queue.nextIndex(userSkip = false))
        assertEquals(1, queue.nextIndex(userSkip = true))
    }

    @Test
    fun `reproducir siguiente entra justo después de la actual`() {
        val queue = PlayQueue().apply { replace(songs, startIndex = 1, shuffle = false) }

        queue.playNext(track("x"))

        assertEquals(listOf("a", "b", "x", "c", "d"), queue.ids())
        assertEquals("b", queue.current?.id)
    }

    @Test
    fun `quitar y deshacer deja la cola como estaba`() {
        val queue = PlayQueue().apply { replace(songs, startIndex = 2, shuffle = false) }

        queue.remove(0)
        assertEquals("c", queue.current?.id)
        queue.restore(0, track("a"))

        assertEquals(listOf("a", "b", "c", "d"), queue.ids())
        assertEquals("c", queue.current?.id)
    }

    @Test
    fun `no se puede quitar la canción que suena`() {
        val queue = PlayQueue().apply { replace(songs, startIndex = 1, shuffle = false) }

        queue.remove(1)

        assertEquals(4, queue.size)
    }

    @Test
    fun `mover una canción mantiene la que suena`() {
        val queue = PlayQueue().apply { replace(songs, startIndex = 1, shuffle = false) }

        queue.move(fromIndex = 3, toIndex = 0)

        assertEquals(listOf("d", "a", "b", "c"), queue.ids())
        assertEquals("b", queue.current?.id)
    }

    @Test
    fun `aleatorio deja la actual primero y al quitarlo vuelve el orden original`() {
        val queue = PlayQueue(Random(7)).apply { replace(songs, startIndex = 2, shuffle = false) }

        queue.setShuffle(true)
        assertEquals("c", queue.current?.id)
        assertEquals(0, queue.currentIndex)
        assertEquals(songs.map(Track::id).toSet(), queue.ids().toSet())
        queue.add(track("x"))
        queue.setShuffle(false)

        assertEquals(listOf("a", "b", "c", "d", "x"), queue.ids())
        assertEquals("c", queue.current?.id)
    }

    @Test
    fun `limpiar deja solo la canción actual`() {
        val queue = PlayQueue().apply { replace(songs, startIndex = 2, shuffle = false) }

        queue.clearUpNext()

        assertEquals(listOf("c"), queue.ids())
        assertTrue(queue.upNext.isEmpty())
    }
}
