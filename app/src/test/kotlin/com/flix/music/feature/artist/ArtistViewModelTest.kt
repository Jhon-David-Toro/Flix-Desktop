package com.flix.music.feature.artist

import androidx.lifecycle.SavedStateHandle
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.Track
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val metallica = ArtistRef("UCmetallica", "Metallica")
    private val sandman = Track("1", "Enter Sandman", listOf(metallica), AlbumRef("MPREblack", "Metallica"), null, "black")
    private val fade = Track("2", "Fade To Black", listOf(metallica), AlbumRef("MPRErtl", "Ride the Lightning"), null, "rtl")
    private val cover = Track("3", "Enter Sandman", listOf(ArtistRef("UCother", "Otro")), null, null, null)

    private val musicRepository = FakeMusicRepository().apply { tracks = listOf(sandman, cover, fade) }
    private val followedArtists = FakeFollowedArtistsRepository()
    private val viewModel by lazy {
        ArtistViewModel(
            SavedStateHandle(mapOf("artistId" to "UCmetallica", "artistName" to "Metallica")),
            musicRepository,
            followedArtists,
            FakePlayerController(),
        )
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun content() = viewModel.uiState.value as ArtistUiState.Content

    @Test
    fun `populares y álbumes salen de las canciones del artista`() = runTest {
        collectUiState()
        advanceUntilIdle()

        assertEquals(listOf(sandman, fade), content().popular)
        assertEquals(listOf("MPREblack", "MPRErtl"), content().albums.map { it.id })
        assertEquals("black", content().heroArtworkUrl)
    }

    @Test
    fun `seguir y dejar de seguir`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onToggleFollow()
        advanceUntilIdle()
        assertTrue(content().isFollowing)

        viewModel.onToggleFollow()
        advanceUntilIdle()
        assertEquals(emptyList<String>(), followedArtists.current.map { it.id })
    }

    @Test
    fun `un fallo de red muestra el error con el nombre`() = runTest {
        musicRepository.error = FlixError.Timeout
        collectUiState()
        advanceUntilIdle()

        assertEquals(ArtistUiState.Error("Metallica", FlixError.Timeout), viewModel.uiState.value)
    }
}
