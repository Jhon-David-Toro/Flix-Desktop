package com.flix.music.feature.search

import com.flix.music.resources.Res
import com.flix.music.resources.ic_bolt
import com.flix.music.resources.ic_dark_mode
import com.flix.music.resources.ic_directions_run
import com.flix.music.resources.ic_psychology
import com.flix.music.resources.mood_energy
import com.flix.music.resources.mood_focus
import com.flix.music.resources.mood_night
import com.flix.music.resources.mood_workout
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource


/** Mood chips from the design ("Según tu ánimo"). Each one runs a curated search [query]. */
enum class SearchMood(
    val label: StringResource,
    val icon: DrawableResource,
    val query: String,
) {
    Energy(Res.string.mood_energy, Res.drawable.ic_bolt, "música energética"),
    Night(Res.string.mood_night, Res.drawable.ic_dark_mode, "música para la noche"),
    Focus(Res.string.mood_focus, Res.drawable.ic_psychology, "música para concentrarse"),
    Workout(Res.string.mood_workout, Res.drawable.ic_directions_run, "música para entrenar"),
}
