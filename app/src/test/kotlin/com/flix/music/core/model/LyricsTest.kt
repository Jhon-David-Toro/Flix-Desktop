package com.flix.music.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LyricsTest {

    private val lrc = """
        [ar: Artista]
        [00:12.50] primera línea
        [00:15.00][01:15.00] estribillo
        [00:20.7] tercera
        [00:25.00]
    """.trimIndent()

    @Test
    fun `lee las marcas de tiempo, repite estribillos y descarta etiquetas y líneas vacías`() {
        val lyrics = Lyrics.fromLrc(lrc)

        assertEquals(
            listOf(
                LyricLine(12_500.milliseconds, "primera línea"),
                LyricLine(15.seconds, "estribillo"),
                LyricLine(20_700.milliseconds, "tercera"),
                LyricLine(75.seconds, "estribillo"),
            ),
            lyrics.lines,
        )
        assertTrue(lyrics.isSynced)
    }

    @Test
    fun `la línea activa es la última que ya empezó`() {
        val lyrics = Lyrics.fromLrc(lrc)

        assertNull(lyrics.activeLineIndex(5.seconds))
        assertEquals(0, lyrics.activeLineIndex(13.seconds))
        assertEquals(2, lyrics.activeLineIndex(30.seconds))
        assertEquals(3, lyrics.activeLineIndex(80.seconds))
    }

    @Test
    fun `la letra sin tiempos no resalta ninguna línea`() {
        val lyrics = Lyrics.fromPlainText("uno\n\n dos ")

        assertEquals(listOf("uno", "dos"), lyrics.lines.map { line -> line.text })
        assertFalse(lyrics.isSynced)
        assertNull(lyrics.activeLineIndex(10.seconds))
    }
}
