package com.flix.music.core.player.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamUrlsTest {

    @Test
    fun `la URL de la canción y su id van y vuelven`() {
        val url = youTubeWatchUrl("9HZ_tx8aWuA")

        assertEquals("https://www.youtube.com/watch?v=9HZ_tx8aWuA", url)
        assertEquals("9HZ_tx8aWuA", trackIdOfWatchUrl(url))
    }

    @Test
    fun `no toca las URLs de audio ya resueltas`() {
        assertNull(trackIdOfWatchUrl("https://rr2---sn-x.googlevideo.com/videoplayback?id=abc&expire=1"))
        assertNull(trackIdOfWatchUrl("https://www.youtube.com/watch?v=9HZ_tx8aWuA&list=PL1"))
    }

    @Test
    fun `la caducidad sale del parámetro expire con un minuto de margen`() {
        val url = "https://rr2---sn-x.googlevideo.com/videoplayback?ip=1.2.3.4&expire=1790936852&id=o-A"

        assertEquals(1_790_936_852_000L - 60_000L, streamExpiresAtMillis(url, nowMillis = 0))
    }

    @Test
    fun `sin expire se confía en la URL media hora`() {
        assertEquals(1_000L + 30 * 60_000L, streamExpiresAtMillis("https://example.com/audio.mp3", nowMillis = 1_000))
    }
}
