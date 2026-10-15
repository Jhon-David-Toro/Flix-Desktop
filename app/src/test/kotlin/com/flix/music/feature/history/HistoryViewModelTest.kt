package com.flix.music.feature.history

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.HistoryDayLabel
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackSource
import com.flix.music.testing.FakeHistoryRepository
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun entry(id: Long, title: String, playedAt: String) =
        HistoryEntry(id, Track(id.toString(), title, listOf(ArtistRef(null, "Artista")), null, null, null), Instant.parse(playedAt))

    private val fadeToBlack = entry(1, "Fade To Black", "2026-09-28T21:40:00Z")
    private val killshot = entry(2, "Killshot", "2026-09-28T19:20:00Z")
    private val lately = entry(3, "Lately", "2026-09-27T23:02:00Z")

    private val repository = FakeHistoryRepository(listOf(fadeToBlack, killshot, lately))
    private val playerController = FakePlayerController()
    private val clock = Clock.fixed(Instant.parse("2026-09-28T23:00:00Z"), ZoneOffset.UTC)
    private val viewModel by lazy { HistoryViewModel(repository, playerController, clock) }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun days() = (viewModel.uiState.value as HistoryUiState.Content).days

    @Test
    fun `agrupa las escuchas en hoy y ayer`() = runTest {
        collectUiState()
        advanceUntilIdle()

        assertEquals(listOf(HistoryDayLabel.Today, HistoryDayLabel.Yesterday), days().map { it.label })
    }

    @Test
    fun `tocar una entrada reproduce desde ella con origen Historial`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onEntryClick(killshot)

        assertEquals(
            FakePlayerController.PlayRequest(
                tracks = listOf(killshot.track, lately.track),
                startIndex = 0,
                source = PlaybackSource.History,
                shuffle = false,
            ),
            playerController.lastPlayRequest,
        )
    }

    @Test
    fun `quitar y deshacer deja el historial como estaba`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onRemove(killshot)
        advanceUntilIdle()
        assertEquals(listOf(1L, 3L), repository.current.map { it.id })

        viewModel.onUndoRemove(killshot)
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L, 3L), repository.current.map { it.id })
    }

    @Test
    fun `borrar historial lo vacía`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onClearHistory()
        advanceUntilIdle()

        assertEquals(emptyList<HistoryEntry>(), repository.current)
    }
}
