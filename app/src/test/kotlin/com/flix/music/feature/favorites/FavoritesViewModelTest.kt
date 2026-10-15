package com.flix.music.feature.favorites

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FavoriteTrack
import com.flix.music.core.model.SortOrder
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackSource
import com.flix.music.testing.FakeFavoritesRepository
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val killshot = FavoriteTrack(
        Track("1", "Killshot", listOf(ArtistRef(null, "Eminem")), null, null, null),
        Instant.parse("2026-09-28T10:00:00Z"),
    )
    private val fadeToBlack = FavoriteTrack(
        Track("2", "Fade To Black", listOf(ArtistRef(null, "Metallica")), null, null, null),
        Instant.parse("2026-09-27T10:00:00Z"),
    )

    private val repository = FakeFavoritesRepository(listOf(killshot, fadeToBlack))
    private val playerController = FakePlayerController()
    private val viewModel by lazy { FavoritesViewModel(repository, playerController) }

    private fun kotlinx.coroutines.test.TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun visibleIds() = (viewModel.uiState.value as FavoritesUiState.Content).visible.map { it.track.id }

    @Test
    fun `muestra los favoritos del más reciente al más antiguo`() = runTest {
        collectUiState()
        advanceUntilIdle()

        assertEquals(listOf("1", "2"), visibleIds())
    }

    @Test
    fun `buscar y ordenar A–Z filtran la lista visible`() = runTest {
        collectUiState()
        viewModel.onSortOrderChange(SortOrder.Alphabetical)
        advanceUntilIdle()
        assertEquals(listOf("2", "1"), visibleIds())

        viewModel.onQueryChange("eminem")
        advanceUntilIdle()
        assertEquals(listOf("1"), visibleIds())
    }

    @Test
    fun `reproducir todo empieza por el primero con origen Favoritos`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onPlayAll()

        assertEquals(
            FakePlayerController.PlayRequest(
                tracks = listOf(killshot.track, fadeToBlack.track),
                startIndex = 0,
                source = PlaybackSource.Favorites,
                shuffle = false,
            ),
            playerController.lastPlayRequest,
        )
    }

    @Test
    fun `deshacer devuelve el favorito a su posición original`() = runTest {
        collectUiState()
        advanceUntilIdle()

        viewModel.onRemove(killshot)
        advanceUntilIdle()
        assertEquals(listOf("2"), visibleIds())

        viewModel.onUndoRemove(killshot)
        advanceUntilIdle()
        assertEquals(listOf("1", "2"), visibleIds())
    }
}
