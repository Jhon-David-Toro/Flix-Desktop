package com.flix.music.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes

class ListeningStatsTest {

    private val today = LocalDate.of(2026, 9, 30)
    private val metallica = ArtistRef("m", "Metallica")
    private val eminem = ArtistRef("e", "Eminem")

    private fun play(id: Long, trackId: String, artist: ArtistRef, minutes: Int, day: LocalDate) = HistoryEntry(
        id = id,
        track = Track(trackId, "Canción $trackId", listOf(artist), null, minutes.minutes, null),
        playedAt = day.atTime(12, 0).toInstant(ZoneOffset.UTC),
    )

    private val history = listOf(
        play(1, "fade", metallica, 7, today),
        play(2, "fade", metallica, 7, today.minusDays(1)),
        play(3, "kill", eminem, 4, today.minusDays(1)),
        play(4, "one", metallica, 8, LocalDate.of(2026, 8, 20)),
    )

    @Test
    fun `el mes suma lo escuchado solo dentro del mes`() {
        val month = history.summaryFor(StatsPeriod.Month, today, ZoneOffset.UTC)

        assertEquals(18.minutes, month.listened)
        assertEquals(3, month.plays)
        assertEquals(30, month.activity.size)
        assertEquals(7.minutes, month.activity.last().listened)
    }

    @Test
    fun `rankings por tiempo para artistas y por veces para canciones`() {
        val month = history.summaryFor(StatsPeriod.Month, today, ZoneOffset.UTC)

        assertEquals(listOf(metallica, eminem), month.topArtists.map { it.artist })
        assertEquals(14.minutes, month.topArtists.first().listened)
        assertEquals(TrackPlays(history.first().track, 2), month.topTracks.first())
    }

    @Test
    fun `la semana empieza el lunes y el año agrupa por meses`() {
        val week = history.summaryFor(StatsPeriod.Week, today, ZoneOffset.UTC)
        val year = history.summaryFor(StatsPeriod.Year, today, ZoneOffset.UTC)

        assertEquals(LocalDate.of(2026, 9, 28), week.start)
        assertEquals(7, week.activity.size)
        assertEquals(12, year.activity.size)
        assertEquals(8.minutes, year.activity[7].listened)
    }

    @Test
    fun `el mes anterior sirve para la comparación`() {
        assertEquals(8.minutes, history.listenedInPreviousMonth(today, ZoneOffset.UTC))
    }
}
