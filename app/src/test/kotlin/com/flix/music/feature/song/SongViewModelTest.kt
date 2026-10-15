package com.flix.music.feature.song

import androidx.lifecycle.SavedStateHandle
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.SongDetails
import com.flix.music.core.model.Track
import com.flix.music.testing.FakeFavoritesRepository
import com.flix.music.testing.FakeFollowedArtistsRepository
import com.flix.music.testing.FakeMusicRepository
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val metallica = ArtistRef("UCmetallica", "Metallica")
    private val fadeToBlack = Track("fade", "Fade To Black", listOf(metallica), null, null, null)
    private val one = Track("one", "One", listOf(metallica), null, null, null)
    private val cover = Track("cover", "Fade To Black", listOf(ArtistRef("UCother", "Apocalyptica")), null, null, null)

    private val musicRepository = FakeMusicRepository().apply { tracks = listOf(fadeToBlack, one, cover) }
    private val followedArtists = FakeFollowedArtistsRepository()
    private val playerController = FakePlayerController()
    private val viewModel by lazy {
        SongViewModel(
            SavedStateHandle(mapOf("trackId" to "fade")),
            musicRepository,
            FakeFavoritesRepository(),
            followedArtists,
            playerController,
        )
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `abre al instante con la canción que ya venía de la búsqueda`() = runTest {
        musicRepository.responseDelayMillis = 5_000

        assertEquals(fadeToBlack, viewModel.uiState.value.track)
    }

    @Test
    fun `más como esta son otras canciones del mismo artista`() = runTest {
        collectUiState()
        advanceUntilIdle()

        assertEquals(listOf(one), viewModel.uiState.value.moreLikeThis)
    }

    @Test
    fun `si no se puede reproducir el botón no hace nada`() = runTest {
        musicRepository.songDetails = SongDetails(fadeToBlack, isPlayable = false)
        collectUiState()
        advanceUntilIdle()

        viewModel.onPlay()

        assertNull(playerController.lastPlayRequest)
    }

    @Test
    fun `sin datos previos un fallo de red muestra el error`() = runTest {
        musicRepository.tracks = emptyList()
        musicRepository.error = FlixError.NoConnection
        collectUiState()
        advanceUntilIdle()

        assertEquals(FlixError.NoConnection, viewModel.uiState.value.error)
    }

    @Test
    fun `seguir al artista lo guarda en la biblioteca`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onToggleFollow()
        advanceUntilIdle()

        assertEquals(listOf("UCmetallica"), followedArtists.current.map { it.id })
        assertEquals(true, viewModel.uiState.value.isFollowingArtist)
    }
}
