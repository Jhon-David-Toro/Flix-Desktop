package com.flix.music.feature.search

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackSource
import com.flix.music.testing.FakeMusicRepository
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.FakeRecentSearchesRepository
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMusicRepository()
    private val recentSearchesRepository = FakeRecentSearchesRepository()
    private val playerController = FakePlayerController()

    /**
     * Built lazily so it is created after [mainDispatcherRule] installs the test
     * dispatcher: the ViewModel starts collecting recent searches in its constructor.
     */
    private val viewModel by lazy { SearchViewModel(repository, recentSearchesRepository, playerController) }

    private val fadeToBlack = Track("1", "Fade To Black", listOf(ArtistRef(null, "Metallica")), null, null, null)
    private val fadeToBlackResults =
        SearchUiState.Results(listOf(fadeToBlack), artists = emptyList(), albums = emptyList())

    @Test
    fun `escribir rápido hace una sola búsqueda`() = runTest {
        repository.tracks = listOf(fadeToBlack)

        viewModel.onQueryChange("fa")
        viewModel.onQueryChange("fad")
        viewModel.onQueryChange("fade")
        advanceTimeBy(299.milliseconds)
        assertEquals(0, repository.searchCount)

        advanceUntilIdle()
        assertEquals(1, repository.searchCount)
        assertEquals(fadeToBlackResults, viewModel.uiState.value)
    }

    @Test
    fun `menos de dos letras no busca y muestra sugerencias`() = runTest {
        viewModel.onQueryChange("f")
        advanceUntilIdle()

        assertEquals(0, repository.searchCount)
        assertEquals(SearchUiState.Typing(emptyList()), viewModel.uiState.value)
    }

    @Test
    fun `al escribir sugiere las búsquedas recientes que coinciden`() = runTest {
        val recents = FakeRecentSearchesRepository(listOf("fade to black", "eminem killshot"))
        val typingViewModel = SearchViewModel(repository, recents, playerController)
        runCurrent()

        typingViewModel.onQueryChange("fade")

        assertEquals(SearchUiState.Typing(listOf("fade to black")), typingViewModel.uiState.value)
    }

    @Test
    fun `sin resultados muestra el estado vacío con recientes parecidas`() = runTest {
        val recents = FakeRecentSearchesRepository(listOf("fade to black", "metallica"))
        val emptyViewModel = SearchViewModel(repository, recents, playerController)
        repository.tracks = emptyList()

        emptyViewModel.onQueryChange("fdae to blak")
        advanceUntilIdle()

        assertEquals(
            SearchUiState.Empty("fdae to blak", suggestions = listOf("fade to black")),
            emptyViewModel.uiState.value,
        )
    }

    @Test
    fun `una búsqueda con resultados se guarda en recientes`() = runTest {
        repository.tracks = listOf(fadeToBlack)

        viewModel.onQueryChange("fade to black")
        advanceUntilIdle()

        assertEquals(listOf("fade to black"), viewModel.recentSearches.value)
    }

    @Test
    fun `buscar ya no espera el debounce y pone el texto en el campo`() = runTest {
        repository.tracks = listOf(fadeToBlack)

        viewModel.onSearchNow("metallica")
        runCurrent()

        assertEquals("metallica", viewModel.query)
        assertEquals(1, repository.searchCount)
        assertEquals("metallica", repository.lastQuery)
    }

    @Test
    fun `si el repository falla muestra el error`() = runTest {
        repository.error = FlixError.NoConnection

        viewModel.onQueryChange("fade")
        advanceUntilIdle()

        assertEquals(SearchUiState.Error(FlixError.NoConnection), viewModel.uiState.value)
    }

    @Test
    fun `reintentar después de un error muestra los resultados`() = runTest {
        repository.error = FlixError.NoConnection
        viewModel.onQueryChange("fade")
        advanceUntilIdle()

        repository.error = null
        repository.tracks = listOf(fadeToBlack)
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(fadeToBlackResults, viewModel.uiState.value)
    }

    @Test
    fun `mientras carga una búsqueda nueva, conserva los resultados anteriores`() = runTest {
        repository.tracks = listOf(fadeToBlack)
        viewModel.onQueryChange("fade")
        advanceUntilIdle()

        repository.responseDelayMillis = 1_000
        viewModel.onQueryChange("fade to")
        advanceTimeBy(301.milliseconds)

        assertEquals(SearchUiState.Loading(previous = fadeToBlackResults), viewModel.uiState.value)
    }

    @Test
    fun `una búsqueda nueva vuelve al filtro Todo`() = runTest {
        viewModel.onFilterChange(SearchFilter.Artists)

        viewModel.onQueryChange("fade")

        assertEquals(SearchFilter.All, viewModel.filter)
    }

    @Test
    fun `quitar y borrar recientes`() = runTest {
        val recents = FakeRecentSearchesRepository(listOf("fade to black", "metallica"))
        val recentsViewModel = SearchViewModel(repository, recents, playerController)
        runCurrent()

        recentsViewModel.onRemoveRecent("fade to black")
        runCurrent()
        assertEquals(listOf("metallica"), recentsViewModel.recentSearches.value)

        recentsViewModel.onClearRecents()
        runCurrent()
        assertEquals(emptyList<String>(), recentsViewModel.recentSearches.value)
    }

    @Test
    fun `tocar una canción reproduce desde ella hasta el final de los resultados`() = runTest {
        val killshot = Track("2", "Killshot", listOf(ArtistRef(null, "Eminem")), null, null, null)
        val lately = Track("3", "Lately", listOf(ArtistRef(null, "Luar La L")), null, null, null)
        repository.tracks = listOf(fadeToBlack, killshot, lately)
        viewModel.onQueryChange("fade")
        advanceUntilIdle()

        viewModel.onTrackClick(killshot)

        assertEquals(
            FakePlayerController.PlayRequest(
                tracks = listOf(killshot, lately),
                startIndex = 0,
                source = PlaybackSource.Search("fade"),
                shuffle = false,
            ),
            playerController.lastPlayRequest,
        )
    }

    @Test
    fun `borrar vuelve al reposo`() = runTest {
        repository.tracks = listOf(fadeToBlack)
        viewModel.onQueryChange("fade")
        advanceUntilIdle()

        viewModel.onClear()

        assertEquals("", viewModel.query)
        assertEquals(SearchUiState.Idle, viewModel.uiState.value)
    }
}
