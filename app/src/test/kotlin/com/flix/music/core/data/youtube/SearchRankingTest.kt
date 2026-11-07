package com.flix.music.core.data.youtube

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRankingTest {

    private fun track(id: String) = Track(id, "Canción $id", listOf(ArtistRef(null, "Artista")), null, null, null)

    @Test
    fun `los videos van después de las canciones y sin repetir`() {
        val songs = listOf(track("a"), track("b"))
        val videos = listOf(track("live"), track("a"), track("remix"))

        assertEquals(listOf("a", "b", "live", "remix"), rankedSearchResults(songs, videos).map { it.id })
    }
}
