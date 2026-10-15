package com.flix.music.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LibraryOrderingTest {

    private fun track(id: String, title: String, artist: String = "Artista", seconds: Int? = null) =
        Track(id, title, listOf(ArtistRef(null, artist)), null, seconds?.seconds, null)

    private val recentFirst = listOf(
        FavoriteTrack(track("1", "Killshot", "Eminem"), Instant.parse("2026-09-28T10:00:00Z")),
        FavoriteTrack(track("2", "fade to black", "Metallica"), Instant.parse("2026-09-27T10:00:00Z")),
    )

    @Test
    fun `A–Z ordena por título sin distinguir mayúsculas y Recientes conserva el orden`() {
        assertEquals(listOf("2", "1"), recentFirst.sortedFor(SortOrder.Alphabetical).map { it.track.id })
        assertEquals(listOf("1", "2"), recentFirst.sortedFor(SortOrder.Recent).map { it.track.id })
    }

    @Test
    fun `la búsqueda local encuentra por título o artista`() {
        assertEquals(listOf("2"), recentFirst.matching("METALLICA").map { it.track.id })
        assertEquals(listOf("1"), recentFirst.matching("kill").map { it.track.id })
        assertEquals(2, recentFirst.matching("  ").size)
    }

    @Test
    fun `la duración total suma solo las canciones con duración conocida`() {
        val tracks = listOf(track("1", "a", seconds = 300), track("2", "b", seconds = 120), track("3", "c"))

        assertEquals(7.minutes, tracks.totalDuration())
    }

    @Test
    fun `el historial se agrupa en hoy, ayer y fechas anteriores`() {
        val today = LocalDate.of(2026, 9, 28)
        fun entry(id: Long, playedAt: String) = HistoryEntry(id, track(id.toString(), "t$id"), Instant.parse(playedAt))
        val history = listOf(
            entry(1, "2026-09-28T21:40:00Z"),
            entry(2, "2026-09-28T19:20:00Z"),
            entry(3, "2026-09-27T23:02:00Z"),
            entry(4, "2026-09-21T08:30:00Z"),
        )

        val days = history.groupedByDay(today, ZoneOffset.UTC)

        assertEquals(
            listOf(HistoryDayLabel.Today, HistoryDayLabel.Yesterday, HistoryDayLabel.Date(LocalDate.of(2026, 9, 21))),
            days.map { it.label },
        )
        assertEquals(listOf(1L, 2L), days.first().entries.map { it.id })
        assertEquals(2, history.countPlayedOn(today, ZoneOffset.UTC))
    }
}
