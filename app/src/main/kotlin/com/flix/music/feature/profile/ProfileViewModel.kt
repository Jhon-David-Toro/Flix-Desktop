package com.flix.music.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.data.repository.FollowedArtistsRepository
import com.flix.music.core.data.repository.HistoryRepository
import com.flix.music.core.data.repository.PlaylistsRepository
import com.flix.music.core.model.ListeningSummary
import com.flix.music.core.model.StatsPeriod
import com.flix.music.core.model.listenedInPreviousMonth
import com.flix.music.core.model.summaryFor
import com.flix.music.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration

/** [month] and [week] are null until the history loads. */
data class ProfileUiState(
    val playlistCount: Int = 0,
    val favoriteCount: Int = 0,
    val artistCount: Int = 0,
    val today: LocalDate = LocalDate.MIN,
    val month: ListeningSummary? = null,
    val week: ListeningSummary? = null,
    val previousMonthListened: Duration = Duration.ZERO,
) {
    val hasListening: Boolean get() = (month?.plays ?: 0) > 0
}

/** Local profile (design `profile`): library counts and "Este mes", computed on the device. */
class ProfileViewModel(
    historyRepository: HistoryRepository,
    playlistsRepository: PlaylistsRepository,
    favoritesRepository: FavoritesRepository,
    followedArtistsRepository: FollowedArtistsRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        historyRepository.history,
        playlistsRepository.playlists,
        favoritesRepository.favorites,
        followedArtistsRepository.artists,
    ) { history, playlists, favorites, artists ->
        val today = LocalDate.now(clock)
        ProfileUiState(
            playlistCount = playlists.size,
            favoriteCount = favorites.size,
            artistCount = artists.size,
            today = today,
            month = history.summaryFor(StatsPeriod.Month, today, clock.zone, rankingSize = 1),
            week = history.summaryFor(StatsPeriod.Week, today, clock.zone, rankingSize = 0),
            previousMonthListened = history.listenedInPreviousMonth(today, clock.zone),
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, ProfileUiState())
}
