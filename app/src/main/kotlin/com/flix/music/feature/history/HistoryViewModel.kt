package com.flix.music.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.HistoryRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.HistoryDay
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.groupedByDay
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Content(val days: List<HistoryDay>) : HistoryUiState
}

/** History screen: plays grouped by day, replay from an entry, remove with undo, clear all. */
class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val playerController: PlayerController,
    private val clock: Clock,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    val uiState: StateFlow<HistoryUiState> = historyRepository.history
        .map { entries -> HistoryUiState.Content(entries.groupedByDay(LocalDate.now(clock), clock.zone)) }
        .stateIn(viewModelScope, WhileUiSubscribed, HistoryUiState.Loading)

    /** Plays the history from [entry] onwards (design: "Volver a reproducir"). */
    fun onEntryClick(entry: HistoryEntry) {
        val entries = (uiState.value as? HistoryUiState.Content)?.days?.flatMap { it.entries }.orEmpty()
        val startIndex = entries.indexOfFirst { it.id == entry.id }
        if (startIndex < 0) return
        playerController.play(
            tracks = entries.drop(startIndex).map { it.track }.distinctBy { it.id },
            startIndex = 0,
            source = PlaybackSource.History,
        )
    }

    fun onRemove(entry: HistoryEntry) {
        viewModelScope.launch { historyRepository.remove(entry.id) }
    }

    fun onUndoRemove(entry: HistoryEntry) {
        viewModelScope.launch { historyRepository.restore(entry) }
    }

    fun onClearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }
}
