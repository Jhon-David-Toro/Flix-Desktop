package com.flix.music.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSearchesTest {

    @Test
    fun `la búsqueda nueva va primero`() {
        assertEquals(listOf("metallica", "bad bunny"), listOf("bad bunny").withRecent("metallica"))
    }

    @Test
    fun `repetir una búsqueda la sube sin duplicarla, ignorando mayúsculas`() {
        assertEquals(
            listOf("Metallica", "bad bunny"),
            listOf("bad bunny", "metallica").withRecent("Metallica"),
        )
    }

    @Test
    fun `nunca guarda más del máximo`() {
        val full = (1..MAX_RECENT_SEARCHES).map { number -> "búsqueda $number" }

        val updated = full.withRecent("nueva")

        assertEquals(MAX_RECENT_SEARCHES, updated.size)
        assertEquals("nueva", updated.first())
    }
}
