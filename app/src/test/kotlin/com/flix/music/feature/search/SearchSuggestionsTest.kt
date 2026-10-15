package com.flix.music.feature.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchSuggestionsTest {

    @Test
    fun `la distancia de edición cuenta cambios de una letra`() {
        assertEquals(0, editDistance("metal", "metal"))
        assertEquals(1, editDistance("blak", "black"))
        assertEquals(3, editDistance("fdae to blak", "fade to black"))
    }

    @Test
    fun `las sugerencias al escribir contienen el texto y excluyen la búsqueda exacta`() {
        val recents = listOf("fade to black", "Fade", "eminem killshot")

        assertEquals(listOf("fade to black"), typingSuggestions("fade", recents))
    }

    @Test
    fun `quizás buscabas propone solo recientes parecidas, la más cercana primero`() {
        val recents = listOf("metallica", "fade to blck", "fade to black")

        assertEquals(
            listOf("fade to blck", "fade to black"),
            similarRecentSearches("fade to blak", recents),
        )
    }
}
