package com.flix.music.feature.home

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlaybackStatus
import com.flix.music.core.player.PlayerState
import com.flix.music.testing.FakeFavoritesRepository
import com.flix.music.testing.FakeHistoryRepository
import com.flix.music.testing.FakeMusicRepository
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.FakePlaylistsRepository
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val metallica = ArtistRef("m", "Metallica")
    private val fadeToBlack = Track("fade", "Fade To Black", listOf(metallica), null, null, null)
    private val one = Track("one", "One", listOf(metallica), null, null, null)

    private val historyRepository = FakeHistoryRepository(
        listOf(HistoryEntry(1, fadeToBlack, Instant.parse("2026-09-29T20:00:00Z"))),
    )
    private val musicRepository = FakeMusicRepository().apply { tracks = listOf(fadeToBlack, one) }
    private val playerController = FakePlayerController()

    private val viewModel by lazy {
        HomeViewModel(
            historyRepository,
            FakeFavoritesRepository(),
            FakePlaylistsRepository(),
            musicRepository,
            playerController,
            Clock.fixed(Instant.parse("2026-09-29T22:00:00Z"), ZoneOffset.UTC),
        )
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `sin escuchas es el primer uso`() = runTest {
        historyRepository.clear()
        collectUiState()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFirstRun)
    }

    @Test
    fun `seguir escuchando usa lo que está en el reproductor con lo que falta`() = runTest {
        playerController.setState(
            PlayerState(status = PlaybackStatus.Paused, current = one, position = 100.seconds, duration = 400.seconds),
        )
        collectUiState()
        advanceUntilIdle()

        val resume = viewModel.uiState.value.resume!!
        assertEquals(one, resume.track)
        assertEquals(300.seconds, resume.remaining)
        assertEquals(0.25f, resume.progress)
    }

    @Test
    fun `con el reproductor vacío reanuda lo último del historial`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onResume()

        assertEquals(
            FakePlayerController.PlayRequest(listOf(fadeToBlack), 0, PlaybackSource.History, false),
            playerController.lastPlayRequest,
        )
    }

    @Test
    fun `más de carga las canciones del artista más escuchado`() = runTest {
        collectUiState()
        advanceUntilIdle()

        assertEquals(metallica, viewModel.uiState.value.topArtist)
        assertEquals(RemoteSection.Loaded(listOf(fadeToBlack, one)), viewModel.uiState.value.moreFromArtist)
    }

    @Test
    fun `si falla solo esa sección muestra el error y se puede reintentar`() = runTest {
        musicRepository.error = FlixError.NoConnection
        collectUiState()
        advanceUntilIdle()
        assertEquals(RemoteSection.Failed, viewModel.uiState.value.moreFromArtist)

        musicRepository.error = null
        viewModel.onRetryMoreFromArtist()
        advanceUntilIdle()
        assertEquals(RemoteSection.Loaded(listOf(fadeToBlack, one)), viewModel.uiState.value.moreFromArtist)
    }
}
