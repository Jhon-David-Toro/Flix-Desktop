package com.flix.music.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FileSizesTest {

    private val spanish = Locale.forLanguageTag("es")

    @Test
    fun `tamaños cortos como en Android`() {
        assertEquals("512 B", formatShortFileSize(512, spanish))
        assertEquals("1,2 GB", formatShortFileSize(1_200_000_000, spanish))
        assertEquals("350 MB", formatShortFileSize(350_000_000, spanish))
        assertEquals("8 GB", formatShortFileSize(8_000_000_000, spanish))
    }
}
