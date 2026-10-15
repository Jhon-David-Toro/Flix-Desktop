package com.flix.music.feature.library

import com.flix.music.core.data.repository.LibraryLayoutRepository
import com.flix.music.core.model.Album
import com.flix.music.core.model.Artist
import com.flix.music.core.model.SortOrder
import com.flix.music.testing.FakeFavoritesRepository
import com.flix.music.testing.FakeFollowedArtistsRepository
import com.flix.music.testing.FakeHistoryRepository
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.FakePlaylistsRepository
import com.flix.music.testing.FakeSavedAlbumsRepository
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class InMemoryLayoutRepository : LibraryLayoutRepository {
        override val gridFilters = MutableStateFlow(emptySet<String>())

        override suspend fun setGrid(filterKey: String, grid: Boolean) {
            gridFilters.value = if (grid) gridFilters.value + filterKey else gridFilters.value - filterKey
        }
    }

    private val masterOfPuppets = Album("mop", "Master of Puppets", "Metallica", null)
    private val eightMile = Album("8m", "8 Mile", "Eminem", null)
    private val layoutRepository = InMemoryLayoutRepository()

    private val viewModel by lazy {
        LibraryViewModel(
            FakeFavoritesRepository(),
            FakeHistoryRepository(),
            FakePlaylistsRepository(),
            FakeSavedAlbumsRepository(listOf(masterOfPuppets, eightMile)),
            FakeFollowedArtistsRepository(listOf(Artist("m", "Metallica", null))),
            layoutRepository,
            FakePlayerController(),
            Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"), ZoneOffset.UTC),
        )
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `álbumes guardados de más reciente a más antiguo y A–Z por nombre`() = runTest {
        collectUiState()
        viewModel.onFilterChange(LibraryFilter.Albums)
        advanceUntilIdle()
        assertEquals(listOf("mop", "8m"), viewModel.uiState.value.albums.map { it.id })

        viewModel.onSortOrderChange(SortOrder.Alphabetical)
        advanceUntilIdle()
        assertEquals(listOf("8m", "mop"), viewModel.uiState.value.albums.map { it.id })
    }

    @Test
    fun `buscar en la biblioteca acota por nombre del álbum o del artista`() = runTest {
        collectUiState()
        viewModel.onFilterChange(LibraryFilter.Albums)
        viewModel.onQueryChange("eminem")
        advanceUntilIdle()

        assertEquals(listOf("8m"), viewModel.uiState.value.albums.map { it.id })
        assertFalse(viewModel.uiState.value.isFilterEmpty)
    }

    @Test
    fun `la cuadrícula se recuerda por filtro`() = runTest {
        collectUiState()
        viewModel.onFilterChange(LibraryFilter.Albums)
        advanceUntilIdle()

        viewModel.onToggleLayout()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isGrid)

        viewModel.onFilterChange(LibraryFilter.Artists)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isGrid)

        viewModel.onFilterChange(LibraryFilter.Albums)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isGrid)
    }

    @Test
    fun `canciones siempre en lista`() = runTest {
        collectUiState()
        viewModel.onFilterChange(LibraryFilter.Songs)
        advanceUntilIdle()

        viewModel.onToggleLayout()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isGrid)
        assertEquals(emptySet<String>(), layoutRepository.gridFilters.value)
    }
}
