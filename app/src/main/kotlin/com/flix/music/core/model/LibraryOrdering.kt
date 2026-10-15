package com.flix.music.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration

/** Library sort orders (design: "Orden: Recientes, A–Z"). */
enum class SortOrder { Recent, Alphabetical }

/** Favorites in [order]. The list is expected most-recent-first, as the repository emits it. */
fun List<FavoriteTrack>.sortedFor(order: SortOrder): List<FavoriteTrack> = when (order) {
    SortOrder.Recent -> this
    SortOrder.Alphabetical -> sortedBy { favorite -> favorite.track.title.lowercase() }
}

/** Favorites whose title or artists contain [query], ignoring case. Blank matches all. */
fun List<FavoriteTrack>.matching(query: String): List<FavoriteTrack> {
    val text = query.trim()
    if (text.isEmpty()) return this
    return filter { favorite ->
        favorite.track.title.contains(text, ignoreCase = true) ||
            favorite.track.artistNames.contains(text, ignoreCase = true)
    }
}

/** Sum of the known durations; songs without duration are skipped. */
fun List<Track>.totalDuration(): Duration =
    mapNotNull { track -> track.duration }.fold(Duration.ZERO) { total, duration -> total + duration }

/** History section header: "Hoy", "Ayer" or a date (design: "fecha agrupada"). */
sealed interface HistoryDayLabel {
    data object Today : HistoryDayLabel
    data object Yesterday : HistoryDayLabel
    data class Date(val date: LocalDate) : HistoryDayLabel
}

data class HistoryDay(val label: HistoryDayLabel, val entries: List<HistoryEntry>)

/** Groups [this] most-recent-first history into days, keeping the order inside each day. */
fun List<HistoryEntry>.groupedByDay(today: LocalDate, zone: ZoneId): List<HistoryDay> =
    groupBy { entry -> entry.playedAt.toLocalDate(zone) }
        .map { (date, entries) ->
            val label = when (date) {
                today -> HistoryDayLabel.Today
                today.minusDays(1) -> HistoryDayLabel.Yesterday
                else -> HistoryDayLabel.Date(date)
            }
            HistoryDay(label, entries)
        }

fun List<HistoryEntry>.countPlayedOn(date: LocalDate, zone: ZoneId): Int =
    count { entry -> entry.playedAt.toLocalDate(zone) == date }

private fun Instant.toLocalDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

/** Playlists in [order]; "Recientes" is the most recently changed first. */
fun List<Playlist>.sortedPlaylistsFor(order: SortOrder): List<Playlist> = when (order) {
    SortOrder.Recent -> sortedByDescending { playlist -> playlist.updatedAt }
    SortOrder.Alphabetical -> sortedBy { playlist -> playlist.name.lowercase() }
}
