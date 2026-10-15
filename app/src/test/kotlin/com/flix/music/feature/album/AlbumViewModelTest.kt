package com.flix.music.feature.album

import androidx.lifecycle.SavedStateHandle
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackSource
import com.flix.music.testing.FakeMusicRepository
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.FakeSavedAlbumsRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val metallica = ArtistRef("UCmetallica", "Metallica")
    private val rideTheLightning = AlbumRef("MPRErtl", "Ride the Lightning")
    private val fight = Track("1", "Fight Fire with Fire", listOf(metallica), rideTheLightning, null, "cover")
    private val fade = Track("2", "Fade To Black", listOf(metallica), rideTheLightning, null, "cover")
    private val live = Track("3", "Fade To Black (Live)", listOf(metallica), AlbumRef("MPRElive", "Live"), null, null)

    private val musicRepository = FakeMusicRepository().apply { tracks = listOf(fight, live, fade) }
    private val savedAlbums = FakeSavedAlbumsRepository()
    private val playerController = FakePlayerController()
    private val viewModel by lazy {
        AlbumViewModel(
            SavedStateHandle(
                mapOf(
                    "albumId" to "MPRErtl",
                    "albumName" to "Ride the Lightning",
                    "artistName" to "Metallica",
                    "artistId" to "UCmetallica",
                ),
            ),
            musicRepository,
            savedAlbums,
            playerController,
        )
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun content() = viewModel.uiState.value as AlbumUiState.Content

    @Test
    fun `busca por álbum y artista y se queda solo con las canciones de ese álbum`() = runTest {
        collectUiState()
        advanceUntilIdle()

        assertEquals("Ride the Lightning Metallica", musicRepository.lastQuery)
        assertEquals(listOf(fight, fade), content().tracks)
    }

    @Test
    fun `el play pone el álbum completo en la cola`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onPlay()

        assertEquals(
            FakePlayerController.PlayRequest(listOf(fight, fade), 0, PlaybackSource.Album("Ride the Lightning"), false),
            playerController.lastPlayRequest,
        )
    }

    @Test
    fun `el corazón guarda el álbum en la biblioteca`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onToggleSaved()
        advanceUntilIdle()

        assertEquals(listOf("MPRErtl"), savedAlbums.current.map { it.id })
        assertEquals(true, content().isSaved)
    }
}
