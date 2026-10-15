package com.flix.music.feature.home

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Genres
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class HomeSectionsTest {

    private val metallica = ArtistRef("m", "Metallica")
    private val eminem = ArtistRef("e", "Eminem")

    private fun play(id: Long, trackId: String, artist: ArtistRef) =
        HistoryEntry(id, Track(trackId, "Canción $trackId", listOf(artist), null, null, null), Instant.EPOCH)

    @Test
    fun `el saludo cambia con la hora`() {
        assertEquals(Greeting.Morning, greetingAt(LocalTime.of(8, 0)))
        assertEquals(Greeting.Afternoon, greetingAt(LocalTime.of(15, 30)))
        assertEquals(Greeting.Night, greetingAt(LocalTime.of(23, 10)))
        assertEquals(Greeting.Night, greetingAt(LocalTime.of(2, 0)))
    }

    @Test
    fun `escuchado recientemente muestra cada canción una vez`() {
        val history = listOf(play(3, "a", metallica), play(2, "b", eminem), play(1, "a", metallica))

        assertEquals(listOf("a", "b"), history.recentTracks().map { it.id })
    }

    @Test
    fun `más de es el artista con más escuchas y en empate el más reciente`() {
        val mostPlayed = listOf(play(3, "a", eminem), play(2, "b", metallica), play(1, "c", metallica))
        assertEquals(metallica, mostPlayed.mostPlayedArtist())

        val tie = listOf(play(2, "a", eminem), play(1, "b", metallica))
        assertEquals(eminem, tie.mostPlayedArtist())
    }

    @Test
    fun `descubrir sugiere dos géneros que cambian cada día`() {
        val today = Genres.all.forDiscovery(LocalDate.of(2026, 9, 29))
        val tomorrow = Genres.all.forDiscovery(LocalDate.of(2026, 9, 30))

        assertEquals(2, today.size)
        assertNotEquals(today, tomorrow)
    }
}
