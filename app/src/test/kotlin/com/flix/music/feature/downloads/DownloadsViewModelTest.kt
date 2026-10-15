package com.flix.music.feature.downloads

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.DownloadState
import com.flix.music.core.model.SongDownload
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackSource
import com.flix.music.testing.FakeDownloadsRepository
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
class DownloadsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fadeToBlack = Track("1", "Fade To Black", listOf(ArtistRef(null, "Metallica")), null, null, null)
    private val killshot = Track("2", "Killshot", listOf(ArtistRef(null, "Eminem")), null, null, null)
    private val midnightCity = Track("3", "Midnight City", listOf(ArtistRef(null, "M83")), null, null, null)
    private val downloadsRepository = FakeDownloadsRepository(
        listOf(
            SongDownload(fadeToBlack, DownloadState.Downloaded, 100),
            SongDownload(killshot, DownloadState.Downloading, 45),
            SongDownload(midnightCity, DownloadState.Downloaded, 100),
        ),
    )
    private val playerController = FakePlayerController()
    private val viewModel by lazy { DownloadsViewModel(downloadsRepository, playerController) }

    @Test
    fun `tocar una descarga reproduce solo las canciones ya descargadas`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDownloadClick(SongDownload(midnightCity, DownloadState.Downloaded, 100))

        assertEquals(
            FakePlayerController.PlayRequest(listOf(fadeToBlack, midnightCity), 1, PlaybackSource.Downloads, false),
            playerController.lastPlayRequest,
        )
    }

    @Test
    fun `reintentar una descarga fallida la vuelve a poner en marcha`() = runTest {
        downloadsRepository.setDownloads(listOf(SongDownload(killshot, DownloadState.Failed)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onRetry(SongDownload(killshot, DownloadState.Failed))
        advanceUntilIdle()

        assertEquals(listOf(DownloadState.Downloaded), downloadsRepository.current.map { download -> download.state })
    }
}
