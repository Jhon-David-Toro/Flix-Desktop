package com.flix.music.feature.playlist

import androidx.lifecycle.SavedStateHandle
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import com.flix.music.testing.FakePlaylistsRepository
import com.flix.music.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun track(id: String, title: String) = Track(id, title, listOf(ArtistRef(null, "Artista")), null, null, null)

    private val midnightCity = track("1", "Midnight City")
    private val killshot = track("2", "Killshot")
    private val everlong = track("3", "Everlong")

    private val repository = FakePlaylistsRepository(
        listOf(Playlist(7, "Noches de ruta", null, listOf(midnightCity, killshot, everlong), Instant.EPOCH)),
    )
    private val viewModel by lazy {
        PlaylistEditViewModel(SavedStateHandle(mapOf("playlistId" to 7L)), repository)
    }

    /** Builds the ViewModel and lets it read the playlist into the draft. */
    private fun TestScope.loadDraft() {
        check(!viewModel.isFinished)
        advanceUntilIdle()
    }

    @Test
    fun `los cambios quedan en borrador hasta guardar`() = runTest {
        loadDraft()
        assertFalse(viewModel.hasChanges)

        viewModel.onNameChange("Noches de carretera")
        viewModel.onMove(fromIndex = 2, toIndex = 0)
        viewModel.onRemove(killshot)

        assertTrue(viewModel.hasChanges)
        assertEquals(listOf(midnightCity, killshot, everlong), repository.current.single().tracks)

        viewModel.onSave()
        advanceUntilIdle()

        val saved = repository.current.single()
        assertEquals("Noches de carretera", saved.name)
        assertEquals(listOf(everlong, midnightCity), saved.tracks)
        assertTrue(viewModel.isFinished)
    }

    @Test
    fun `deshacer devuelve la canción quitada a su sitio`() = runTest {
        loadDraft()

        val index = viewModel.onRemove(killshot)
        viewModel.onUndoRemove(killshot, index)

        assertEquals(listOf(midnightCity, killshot, everlong), viewModel.tracks.toList())
        assertFalse(viewModel.hasChanges)
    }

    @Test
    fun `sin nombre no se puede guardar`() = runTest {
        loadDraft()

        viewModel.onNameChange("   ")
        viewModel.onSave()
        advanceUntilIdle()

        assertFalse(viewModel.canSave)
        assertEquals("Noches de ruta", repository.current.single().name)
    }

    @Test
    fun `el nombre se corta en 60 caracteres`() = runTest {
        loadDraft()

        viewModel.onNameChange("x".repeat(80))

        assertEquals(Playlist.MAX_NAME_LENGTH, viewModel.name.length)
    }

    @Test
    fun `eliminar borra la playlist y cierra`() = runTest {
        loadDraft()

        viewModel.onDelete()
        advanceUntilIdle()

        assertEquals(emptyList<Playlist>(), repository.current)
        assertTrue(viewModel.isFinished)
    }
}
