package com.flix.music.core.player.stream

import com.flix.music.core.data.youtube.NewPipeSetup
import com.flix.music.core.player.AudioSignal
import com.flix.music.core.player.mpv.MpvEngine
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Clock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Plays a real song through [AudioServer] with mpv, muted, and seeks inside it. Needs
 * the internet and libmpv, so it only runs with `-PliveTests=true`.
 */
class AudioServerLiveTest {

    @Test
    fun `mpv reproduce y salta con precisión dentro de una canción del servidor local`() {
        assumeTrue(System.getProperty("flix.liveTests") == "true")
        val okHttpClient = OkHttpClient()
        val server = AudioServer(okHttpClient, NewPipeStreamResolver(NewPipeSetup(okHttpClient), Clock.systemUTC()))
        val library = File(System.getProperty("compose.application.resources.dir"), "libmpv-2.dll")
        val restarted = CountDownLatch(1)
        var position = 0.0
        var duration = 0.0
        val started = System.nanoTime()
        val engine = MpvEngine(library) { signal ->
            when (signal) {
                is AudioSignal.Position -> position = signal.seconds
                is AudioSignal.Duration -> duration = signal.seconds
                AudioSignal.Restarted -> restarted.countDown()
                is AudioSignal.Failed -> println("fallo: ${signal.message}")
                else -> Unit
            }
        }
        engine.setVolume(0)
        engine.load(server.urlOf("9HZ_tx8aWuA"))

        assertTrue("no empezó a sonar", restarted.await(30, TimeUnit.SECONDS))
        println("sonando a los ${(System.nanoTime() - started) / 1_000_000} ms, duración $duration s")
        Thread.sleep(2_000)
        val afterTwoSeconds = position
        val seekStarted = System.nanoTime()
        engine.seek(300.0)
        Thread.sleep(1_500)
        println("tras 2 s: $afterTwoSeconds · tras saltar a 300 s: $position (salto en ${(System.nanoTime() - seekStarted) / 1_000_000} ms)")
        engine.close()
        server.stop()

        assertTrue(afterTwoSeconds > 1.0)
        assertTrue(position in 300.0..302.5)
    }
}
