package com.flix.music.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackArtworkTest {

    private fun track(artworkUrl: String?) = Track("id", "Fade To Black", emptyList(), null, null, artworkUrl)

    @Test
    fun `la portada grande pide 1200 px al CDN de YouTube`() {
        val url = "https://lh3.googleusercontent.com/abc=w544-h544-l90-rj"

        assertEquals("https://lh3.googleusercontent.com/abc=w1200-h1200-l90-rj", track(url).largeArtworkUrl)
    }

    @Test
    fun `las miniaturas de video y la falta de portada no cambian`() {
        val video = "https://i.ytimg.com/vi/abc/hqdefault.jpg"

        assertEquals(video, track(video).largeArtworkUrl)
        assertNull(track(null).largeArtworkUrl)
    }
}
