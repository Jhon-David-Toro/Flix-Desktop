package com.flix.music.core.player

import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import com.flix.music.core.player.stream.StreamResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopPlayerControllerTest {

    private fun track(id: String) = Track(id, "Canción $id", listOf(ArtistRef(null, "Artista")), null, null, null)

    private val songs = listOf("a", "b", "c").map(::track)

    private class FakeEngine(val emit: (AudioSignal) -> Unit) : AudioEngine {
        val loads = mutableListOf<Pair<String, Double>>()
        var pauseRequested = false
        var seekedTo: Double? = null

        override fun load(url: String, startSeconds: Double, paused: Boolean) {
            loads += url to startSeconds
        }

        override fun setPaused(paused: Boolean) {
            pauseRequested = paused
        }

        override fun seek(seconds: Double) {
            seekedTo = seconds
        }

        override fun stop() = Unit

        override fun setVolume(percent: Int) = Unit

        override fun close() = Unit
    }

    private class FakeResolver : StreamResolver {
        val resolved = mutableListOf<String>()
        val forgotten = mutableListOf<String>()

        override fun resolve(trackId: String): String {
            resolved += trackId
            return "https://audio/$trackId"
        }

        override fun forget(trackId: String) {
            forgotten += trackId
        }
    }

    private lateinit var engine: FakeEngine
    private val resolver = FakeResolver()

    private fun TestScope.controller() = DesktopPlayerController(
        engineFactory = { emit -> FakeEngine(emit).also { engine = it } },
        urlOf = { trackId -> "local/$trackId" },
        streamResolver = resolver,
        scope = this,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private fun TestScope.signal(signal: AudioSignal) {
        engine.emit(signal)
        advanceUntilIdle()
    }

    @Test
    fun `reproducir carga la canción y resuelve por adelantado las dos siguientes`() = runTest {
        val player = controller()

        player.play(songs, startIndex = 0, source = PlaybackSource.Favorites)
        advanceUntilIdle()

        assertEquals("local/a" to 0.0, engine.loads.single())
        assertEquals(PlaybackStatus.Loading, player.state.value.status)
        assertEquals(listOf("b", "c"), resolver.resolved)

        signal(AudioSignal.Restarted)
        assertEquals(PlaybackStatus.Playing, player.state.value.status)
    }

    @Test
    fun `al terminar una canción pasa a la siguiente y al final de la cola se detiene`() = runTest {
        val player = controller()
        player.play(songs, startIndex = 1, source = PlaybackSource.Favorites)

        signal(AudioSignal.Finished)
        assertEquals("c", player.state.value.current?.id)

        signal(AudioSignal.Finished)
        assertEquals(PlaybackStatus.Ended, player.state.value.status)
        assertEquals(listOf("local/b", "local/c"), engine.loads.map { load -> load.first })
    }

    @Test
    fun `un fallo se reintenta una vez donde iba y luego se salta con aviso`() = runTest {
        val player = controller()
        player.play(songs, startIndex = 0, source = PlaybackSource.Favorites)
        signal(AudioSignal.Position(42.0))
        val skipped = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            assertEquals(PlayerEvent.TrackSkipped("Canción a"), player.events.first())
        }

        signal(AudioSignal.Failed("403"))
        assertEquals("local/a" to 42.0, engine.loads.last())
        assertEquals(listOf("a"), resolver.forgotten)

        signal(AudioSignal.Failed("403"))
        assertEquals("b", player.state.value.current?.id)
        assertTrue(skipped.isCompleted)
    }

    @Test
    fun `si falla la última canción queda en error y reintentar la retoma`() = runTest {
        val player = controller()
        player.play(listOf(track("a")), startIndex = 0, source = PlaybackSource.Favorites)
        signal(AudioSignal.Failed("x"))
        signal(AudioSignal.Failed("x"))
        assertEquals(PlaybackStatus.Error, player.state.value.status)

        player.togglePlayPause()

        assertEquals(PlaybackStatus.Loading, player.state.value.status)
        assertEquals(3, engine.loads.size)
    }

    @Test
    fun `anterior vuelve al principio si ya sonó un rato, si no va a la anterior`() = runTest {
        val player = controller()
        player.play(songs, startIndex = 1, source = PlaybackSource.Favorites)
        signal(AudioSignal.Position(10.0))

        player.skipToPrevious()
        assertEquals(0.0, engine.seekedTo)
        assertEquals("b", player.state.value.current?.id)

        signal(AudioSignal.Position(1.0))
        player.skipToPrevious()
        assertEquals("a", player.state.value.current?.id)
    }

    @Test
    fun `pausar y saltar a un momento llegan al motor`() = runTest {
        val player = controller()
        player.play(songs, startIndex = 0, source = PlaybackSource.Favorites)
        signal(AudioSignal.Restarted)

        player.togglePlayPause()
        player.seekTo(90.seconds)

        assertTrue(engine.pauseRequested)
        assertEquals(90.0, engine.seekedTo)
        assertEquals(PlaybackStatus.Paused, player.state.value.status)
        assertEquals(90.seconds, player.state.value.position)
    }

    @Test
    fun `canciones añadidas con la cola terminada empiezan a sonar`() = runTest {
        val player = controller()
        player.play(listOf(track("a")), startIndex = 0, source = PlaybackSource.Favorites)
        signal(AudioSignal.Finished)

        player.appendToQueue(listOf(track("r1"), track("r2")))
        advanceUntilIdle()

        assertEquals("r1", player.state.value.current?.id)
        assertEquals("local/r1", engine.loads.last().first)
    }
}
