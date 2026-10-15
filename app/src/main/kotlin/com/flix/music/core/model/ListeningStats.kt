package com.flix.music.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.time.Duration

/** Statistics periods (design `stats`: Semana · Mes · Año). */
enum class StatsPeriod { Week, Month, Year }

/** One bar of "Actividad diaria": a day, or a month in the yearly view. */
data class ActivityBar(val start: LocalDate, val listened: Duration)

/** [artworkUrl] is the cover of one of the artist's songs, since there are no artist photos. */
data class ArtistListening(val artist: ArtistRef, val artworkUrl: String?, val listened: Duration)

data class TrackPlays(val track: Track, val plays: Int)

/**
 * Listening in a period, computed on the device from the play history (design:
 * "Calculado en este dispositivo"). A play counts the song's full length, since only
 * the start of each play is stored.
 */
data class ListeningSummary(
    val period: StatsPeriod,
    val start: LocalDate,
    val listened: Duration,
    val plays: Int,
    val averagePerDay: Duration,
    val activity: List<ActivityBar>,
    val topArtists: List<ArtistListening>,
    val topTracks: List<TrackPlays>,
)

/** First day of the [period] that contains [today]; weeks start on Monday. */
fun StatsPeriod.startOf(today: LocalDate): LocalDate = when (this) {
    StatsPeriod.Week -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    StatsPeriod.Month -> today.withDayOfMonth(1)
    StatsPeriod.Year -> today.withDayOfYear(1)
}

private fun StatsPeriod.endAfter(start: LocalDate): LocalDate = when (this) {
    StatsPeriod.Week -> start.plusWeeks(1)
    StatsPeriod.Month -> start.plusMonths(1)
    StatsPeriod.Year -> start.plusYears(1)
}

fun List<HistoryEntry>.summaryFor(
    period: StatsPeriod,
    today: LocalDate,
    zone: ZoneId,
    rankingSize: Int = DEFAULT_RANKING_SIZE,
): ListeningSummary {
    val start = period.startOf(today)
    val end = period.endAfter(start)
    val plays = filter { entry -> entry.playedAt.atZone(zone).toLocalDate().let { day -> day >= start && day < end } }
    val listened = plays.sumOfLength()
    val elapsedDays = (today.toEpochDay() - start.toEpochDay() + 1).coerceAtLeast(1)
    return ListeningSummary(
        period = period,
        start = start,
        listened = listened,
        plays = plays.size,
        averagePerDay = listened / elapsedDays.toInt(),
        activity = plays.activity(period, start, end, zone),
        topArtists = plays.topArtists(rankingSize),
        topTracks = plays.topTracks(rankingSize),
    )
}

/** What was listened in the month before the one containing [today], for "+3 h vs. agosto". */
fun List<HistoryEntry>.listenedInPreviousMonth(today: LocalDate, zone: ZoneId): Duration {
    val previousMonth = YearMonth.from(today).minusMonths(1)
    return filter { entry -> YearMonth.from(entry.playedAt.atZone(zone)) == previousMonth }.sumOfLength()
}

private fun List<HistoryEntry>.sumOfLength(): Duration =
    fold(Duration.ZERO) { total, entry -> total + (entry.track.duration ?: Duration.ZERO) }

private fun List<HistoryEntry>.activity(
    period: StatsPeriod,
    start: LocalDate,
    end: LocalDate,
    zone: ZoneId,
): List<ActivityBar> {
    val bucketOf: (LocalDate) -> LocalDate =
        if (period == StatsPeriod.Year) { day -> day.withDayOfMonth(1) } else { day -> day }
    val listenedByBucket = groupBy { entry -> bucketOf(entry.playedAt.atZone(zone).toLocalDate()) }
        .mapValues { (_, entries) -> entries.sumOfLength() }
    val buckets = generateSequence(start) { day -> if (period == StatsPeriod.Year) day.plusMonths(1) else day.plusDays(1) }
        .takeWhile { day -> day < end }
    return buckets.map { day -> ActivityBar(day, listenedByBucket[day] ?: Duration.ZERO) }.toList()
}

private fun List<HistoryEntry>.topArtists(limit: Int): List<ArtistListening> =
    filter { entry -> entry.track.artists.isNotEmpty() }
        .groupBy { entry -> entry.track.artists.first().let { artist -> artist.id ?: artist.name } }
        .map { (_, plays) ->
            val firstPlay = plays.first().track
            ArtistListening(firstPlay.artists.first(), firstPlay.artworkUrl, plays.sumOfLength())
        }
        .sortedByDescending { listening -> listening.listened }
        .take(limit)

private fun List<HistoryEntry>.topTracks(limit: Int): List<TrackPlays> =
    groupBy { entry -> entry.track.id }
        .map { (_, plays) -> TrackPlays(plays.first().track, plays.size) }
        .sortedByDescending { trackPlays -> trackPlays.plays }
        .take(limit)

private const val DEFAULT_RANKING_SIZE = 3
