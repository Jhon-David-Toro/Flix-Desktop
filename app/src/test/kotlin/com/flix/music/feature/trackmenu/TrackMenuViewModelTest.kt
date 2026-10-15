package com.flix.music.feature.trackmenu

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import com.flix.music.testing.FakeDownloadsRepository
import com.flix.music.testing.FakeFavoritesRepository
import com.flix.music.testing.FakePlayerController
import com.flix.music.testing.FakePlaylistsRepository
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TrackMenuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fadeToBlack = Track("1", "Fade To Black", listOf(ArtistRef(null, "Metallica")), null, null, null)
    private val nochesDeRuta = Playlist(7, "Noches de ruta", null, emptyList(), Instant.EPOCH)
    private val playlistsRepository = FakePlaylistsRepository(listOf(nochesDeRuta))
    private val playerController = FakePlayerController()
    private val viewModel by lazy {
        TrackMenuViewModel(FakeFavoritesRepository(), playlistsRepository, playerController, FakeDownloadsRepository())
    }

    @Test
    fun `añadir dos veces a la misma playlist no la duplica`() = runTest {
        assertTrue(viewModel.addToPlaylist(nochesDeRuta, fadeToBlack))
        assertFalse(viewModel.addToPlaylist(nochesDeRuta, fadeToBlack))

        assertEquals(listOf(fadeToBlack), playlistsRepository.current.single().tracks)
    }

    @Test
    fun `crear desde el menú añade la canción a la nueva playlist`() = runTest {
        val playlistId = viewModel.createPlaylistWith("Para correr", "", fadeToBlack)

        val created = playlistsRepository.current.single { it.id == playlistId }
        assertEquals("Para correr", created.name)
        assertEquals(listOf(fadeToBlack), created.tracks)
    }

    @Test
    fun `reproducir siguiente y añadir a la cola llegan al reproductor`() {
        viewModel.onPlayNext(fadeToBlack)
        viewModel.onAddToQueue(fadeToBlack)

        assertEquals(listOf(fadeToBlack), playerController.playNextRequests)
        assertEquals(listOf(fadeToBlack), playerController.queueRequests)
    }
}
