package com.flix.music.core.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.log
import kotlin.math.pow

/**
 * "1,2 GB", "350 MB": the short sizes Android's `Formatter.formatShortFileSize` gives,
 * in the user's decimal style; one decimal under 10 of a unit, none above.
 */
fun formatShortFileSize(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes < UNIT) return "$bytes B"
    val exponent = log(bytes.toDouble(), UNIT.toDouble()).toInt().coerceAtMost(UNITS.length)
    val value = bytes / UNIT.toDouble().pow(exponent)
    val pattern = if (value < SHOW_DECIMAL_BELOW) "0.#" else "0"
    return "${DecimalFormat(pattern, DecimalFormatSymbols(locale)).format(value)} ${UNITS[exponent - 1]}B"
}

private const val UNIT = 1000L
private const val UNITS = "kMGT"
private const val SHOW_DECIMAL_BELOW = 10.0
