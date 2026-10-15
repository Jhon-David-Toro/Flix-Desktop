package com.flix.music.core.designsystem.component.util

import kotlin.time.Duration

/** Track length / position as a clock: 417 s → "6:57". */
fun Duration.toClock(): String =
    "%d:%02d".format(inWholeMinutes, inWholeSeconds % 60)

/** Length of a collection as in the design: "9 h 12 min", or "12 min" under an hour. */
fun Duration.toReadableLength(): String {
    val hours = inWholeHours
    val minutes = inWholeMinutes % 60
    return if (hours > 0) "$hours h $minutes min" else "$minutes min"
}
