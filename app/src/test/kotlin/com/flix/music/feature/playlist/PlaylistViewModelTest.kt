package com.flix.music.feature.playlist

import androidx.lifecycle.SavedStateHandle
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackSource
import com.flix.music.testing.FakeDownloadsRepository
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
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val midnightCity = Track("1", "Midnight City", listOf(ArtistRef(null, "M83")), null, null, null)
    private val killshot = Track("2", "Killshot", listOf(ArtistRef(null, "Eminem")), null, null, null)

    private val repository = FakePlaylistsRepository(
        listOf(Playlist(7, "Noches de ruta", null, listOf(midnightCity, killshot), Instant.EPOCH)),
    )
    private val playerController = FakePlayerController()
    private val downloadsRepository = FakeDownloadsRepository()
    private val viewModel by lazy {
        PlaylistViewModel(SavedStateHandle(mapOf("playlistId" to 7L)), repository, playerController, downloadsRepository)
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `tocar una canción reproduce la playlist desde ella`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onTrackClick(killshot)

        assertEquals(
            FakePlayerController.PlayRequest(
                tracks = listOf(midnightCity, killshot),
                startIndex = 1,
                source = PlaybackSource.Playlist("Noches de ruta"),
                shuffle = false,
            ),
            playerController.lastPlayRequest,
        )
    }

    @Test
    fun `al eliminarla la pantalla pasa a Missing`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onDelete()
        advanceUntilIdle()

        assertEquals(PlaylistUiState.Missing, viewModel.uiState.value)
    }

    @Test
    fun `descargar la playlist descarga todas sus canciones`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onDownload()

        assertEquals(setOf(midnightCity, killshot), downloadsRepository.current.map { download -> download.track }.toSet())
    }
}
