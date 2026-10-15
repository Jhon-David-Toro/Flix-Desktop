package com.flix.music.core.designsystem.component.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkTintTest {

    private val red = 0xFFC1121F.toInt()
    private val blue = 0xFF1D4FB8.toInt()

    @Test
    fun `el color dominante es el del grupo más numeroso`() {
        val pixels = IntArray(70) { red } + IntArray(30) { blue }

        assertEquals(red, dominantColorOf(pixels))
    }

    @Test
    fun `ignora los píxeles transparentes`() {
        assertNull(dominantColorOf(IntArray(10) { 0x00FFFFFF }))
    }

    @Test
    fun `en oscuro limita la luminosidad y conserva el tono`() {
        val clamped = 0xFF9FD3FF.toInt().clampLightness(isDark = true)
        val red = clamped shr 16 and 0xFF
        val green = clamped shr 8 and 0xFF
        val blue = clamped and 0xFF

        assertTrue("debe oscurecerse", green < 0xD3)
        assertTrue("sigue siendo azulado", blue > red)
    }
}
