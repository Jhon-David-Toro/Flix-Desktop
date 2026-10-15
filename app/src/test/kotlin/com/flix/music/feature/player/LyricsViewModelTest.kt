package com.flix.music.feature.player

import com.flix.music.core.data.repository.LyricsRepository
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.Lyrics
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackStatus
import com.flix.music.core.player.PlayerState
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fadeToBlack = Track("1", "Fade To Black", listOf(ArtistRef("m", "Metallica")), null, null, null)
    private val lyrics = Lyrics.fromLrc("[00:10]Life it seems will fade away")
    private val lyricsRepository = object : LyricsRepository {
        var failure: FlixError? = null
        var requests = 0

        override suspend fun lyrics(track: Track): Lyrics? {
            requests++
            failure?.let { error -> throw FlixException(error) }
            return lyrics.takeIf { track.id == fadeToBlack.id }
        }
    }
    private val playerController = FakePlayerController().apply {
        setState(PlayerState(status = PlaybackStatus.Playing, current = fadeToBlack))
    }
    private val viewModel by lazy { LyricsViewModel(lyricsRepository, playerController) }

    @Test
    fun `muestra la letra de la canción que suena y no la vuelve a pedir al avanzar`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(LyricsUiState.Ready(lyrics), viewModel.uiState.value)

        playerController.setState(playerController.state.value.copy(position = lyrics.lines.first().start!!))
        advanceUntilIdle()

        assertEquals(1, lyricsRepository.requests)
    }

    @Test
    fun `una canción sin letra queda como no disponible`() = runTest {
        playerController.setState(PlayerState(current = fadeToBlack.copy(id = "2")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(LyricsUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun `reintentar tras un fallo vuelve a pedir la letra`() = runTest {
        lyricsRepository.failure = FlixError.NoConnection
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(LyricsUiState.Failed(FlixError.NoConnection), viewModel.uiState.value)

        lyricsRepository.failure = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(LyricsUiState.Ready(lyrics), viewModel.uiState.value)
    }
}
