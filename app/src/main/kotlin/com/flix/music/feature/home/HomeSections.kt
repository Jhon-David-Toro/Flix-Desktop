package com.flix.music.feature.home

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Genre
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Track
import java.time.LocalDate
import java.time.LocalTime

/** The greeting in the Home title, by time of day. */
enum class Greeting { Morning, Afternoon, Night }

internal fun greetingAt(time: LocalTime): Greeting = when (time.hour) {
    in MORNING_START until AFTERNOON_START -> Greeting.Morning
    in AFTERNOON_START until NIGHT_START -> Greeting.Afternoon
    else -> Greeting.Night
}

/** "Escuchado recientemente": each song once, most recent play first. */
internal fun List<HistoryEntry>.recentTracks(): List<Track> =
    map { entry -> entry.track }.distinctBy { track -> track.id }.take(RECENT_COUNT)

/**
 * The artist behind "Más de …": the one with the most plays, so the section is titled
 * with its real reason (design: never "Hecho para ti"). Ties go to the most recent play.
 */
internal fun List<HistoryEntry>.mostPlayedArtist(): ArtistRef? {
    val artists = mapNotNull { entry -> entry.track.artists.firstOrNull() }
    if (artists.isEmpty()) return null
    val playsByArtist = artists.groupingBy { artist -> artist.id ?: artist.name }.eachCount()
    val topCount = playsByArtist.values.max()
    return artists.first { artist -> playsByArtist.getValue(artist.id ?: artist.name) == topCount }
}

/** "Descubrir": two genres that change every day, so Home does not always suggest the same. */
internal fun List<Genre>.forDiscovery(date: LocalDate): List<Genre> {
    if (size <= DISCOVER_COUNT) return this
    val start = (date.dayOfYear * DISCOVER_COUNT) % size
    return List(DISCOVER_COUNT) { offset -> this[(start + offset) % size] }
}

private const val MORNING_START = 6
private const val AFTERNOON_START = 12
private const val NIGHT_START = 20
private const val RECENT_COUNT = 10
private const val DISCOVER_COUNT = 2
