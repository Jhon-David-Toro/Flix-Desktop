package com.flix.music.feature.genre

import androidx.lifecycle.SavedStateHandle
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.Track
import com.flix.music.core.model.distinctAlbums
import com.flix.music.core.model.distinctArtists
import com.flix.music.core.player.PlaybackSource
import com.flix.music.testing.FakeMusicRepository
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GenreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMusicRepository()
    private val playerController = FakePlayerController()

    private val masterOfPuppets =
        Track("1", "Master of Puppets", listOf(ArtistRef("m", "Metallica")), AlbumRef("mop", "Master of Puppets"), null, null)
    private val raining =
        Track("2", "Raining Blood", listOf(ArtistRef("s", "Slayer")), AlbumRef("rib", "Reign in Blood"), null, null)

    private fun genreViewModel(genreId: String = "metal") =
        GenreViewModel(SavedStateHandle(mapOf("genreId" to genreId)), repository, playerController)

    @Test
    fun `reproducir pone todo el género en la cola desde la primera canción`() = runTest {
        repository.tracks = listOf(masterOfPuppets, raining)
        val viewModel = genreViewModel()
        advanceUntilIdle()

        viewModel.onPlay()

        assertEquals(
            FakePlayerController.PlayRequest(
                tracks = listOf(masterOfPuppets, raining),
                startIndex = 0,
                source = PlaybackSource.Genre("Metal"),
                shuffle = false,
            ),
            playerController.lastPlayRequest,
        )
    }

    @Test
    fun `aleatorio activa el modo aleatorio`() = runTest {
        repository.tracks = listOf(masterOfPuppets, raining)
        val viewModel = genreViewModel()
        advanceUntilIdle()

        viewModel.onShuffle()

        assertEquals(true, playerController.lastPlayRequest?.shuffle)
    }

    @Test
    fun `busca con la consulta curada del género y deriva artistas y álbumes`() = runTest {
        repository.tracks = listOf(masterOfPuppets)

        val viewModel = genreViewModel()
        advanceUntilIdle()

        assertEquals("metal", repository.lastQuery)
        assertEquals(
            GenreUiState.Content(
                tracks = listOf(masterOfPuppets),
                artists = listOf(masterOfPuppets).distinctArtists(),
                albums = listOf(masterOfPuppets).distinctAlbums(),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `sin canciones muestra el estado vacío`() = runTest {
        val viewModel = genreViewModel(genreId = "jazz")
        advanceUntilIdle()

        assertEquals(GenreUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `reintentar después de un error carga el contenido`() = runTest {
        repository.error = FlixError.NoConnection
        val viewModel = genreViewModel()
        advanceUntilIdle()
        assertEquals(GenreUiState.Error(FlixError.NoConnection), viewModel.uiState.value)

        repository.error = null
        repository.tracks = listOf(masterOfPuppets)
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(listOf(masterOfPuppets), (viewModel.uiState.value as GenreUiState.Content).tracks)
    }
}
