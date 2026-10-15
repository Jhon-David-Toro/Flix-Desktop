package com.flix.music.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.HistoryRepository
import com.flix.music.core.model.ListeningSummary
import com.flix.music.core.model.StatsPeriod
import com.flix.music.core.model.summaryFor
import com.flix.music.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.Month,
    val today: LocalDate = LocalDate.MIN,
    val summary: ListeningSummary? = null,
)

/** Estadísticas (design `stats`): week, month or year of listening from the play history. */
class StatsViewModel(
    historyRepository: HistoryRepository,
    clock: Clock,
) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.Month)

    val uiState: StateFlow<StatsUiState> = combine(historyRepository.history, period) { history, selectedPeriod ->
        val today = LocalDate.now(clock)
        StatsUiState(selectedPeriod, today, history.summaryFor(selectedPeriod, today, clock.zone))
    }.stateIn(viewModelScope, WhileUiSubscribed, StatsUiState())

    fun onPeriodChange(newPeriod: StatsPeriod) {
        period.value = newPeriod
    }
}
