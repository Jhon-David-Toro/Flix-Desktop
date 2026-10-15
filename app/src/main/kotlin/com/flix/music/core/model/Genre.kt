package com.flix.music.core.model

/**
 * A curated genre. Each one is backed by a search [query] and has a fixed
 * gradient (ARGB) from the design spec, so its tile always looks the same.
 */
data class Genre(
    val id: String,
    val name: String,
    val query: String,
    val gradientStart: Long,
    val gradientEnd: Long,
)

/** Genres shown in Search, in design order. */
object Genres {

    val all: List<Genre> = listOf(
        Genre("rock", "Rock", "rock", 0xFF7A2E1F, 0xFFD9623B),
        Genre("metal", "Metal", "metal", 0xFF15171C, 0xFF5B6270),
        Genre("hip-hop", "Hip-Hop", "hip hop", 0xFF3B2A5C, 0xFFC46BD6),
        Genre("pop", "Pop", "pop", 0xFFB8325E, 0xFFFF9AB8),
        Genre("electronic", "Electrónica", "electronic", 0xFF0B3D91, 0xFF16C2D5),
        Genre("jazz", "Jazz", "jazz", 0xFF1F4E3D, 0xFFC9A45C),
        Genre("latin", "Latina", "música latina", 0xFFB3470F, 0xFFFFC35C),
        Genre("indie", "Indie", "indie", 0xFF3F5A36, 0xFFA8C686),
        Genre("rnb", "R&B", "r&b", 0xFF4A1F3D, 0xFFD07A8F),
        Genre("classical", "Clásica", "música clásica", 0xFF4B4238, 0xFFD8C7A8),
    )

    fun byId(genreId: String): Genre? = all.firstOrNull { it.id == genreId }
}
