package com.flix.music.feature.profile

import androidx.compose.runtime.Composable
import com.flix.music.core.model.ActivityBar
import com.flix.music.core.model.StatsPeriod
import com.flix.music.resources.Res
import com.flix.music.resources.stats_month_initials
import com.flix.music.resources.stats_weekday_initials
import org.jetbrains.compose.resources.stringResource
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.time.Duration

/** Ranking length as in the design: "6 h 02", or "45 min" under an hour. */
internal fun Duration.toRankingLength(): String {
    val hours = inWholeHours
    val minutes = inWholeMinutes % MINUTES_PER_HOUR
    return if (hours > 0) "$hours h %02d".format(minutes) else "$minutes min"
}

/** "+3 h" / "−45 min" for the month comparison. */
internal fun Duration.toSignedLength(): String {
    val sign = if (isNegative()) "−" else "+"
    val magnitude = absoluteValue
    return if (magnitude.inWholeHours > 0) "$sign${magnitude.inWholeHours} h" else "$sign${magnitude.inWholeMinutes} min"
}

internal fun LocalDate.monthName(locale: Locale = Locale.getDefault()): String =
    month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }

/** Labels under the bars: L…D for a week, 1 · 5 · 10… for a month, E…D for a year. */
@Composable
internal fun activityLabels(period: StatsPeriod, bars: List<ActivityBar>): List<String> = when (period) {
    StatsPeriod.Week -> stringResource(Res.string.stats_weekday_initials).split(",")
    StatsPeriod.Month -> bars.map { bar ->
        val day = bar.start.dayOfMonth
        if (day == 1 || day % MONTH_LABEL_STEP == 0) day.toString() else ""
    }
    StatsPeriod.Year -> stringResource(Res.string.stats_month_initials).split(",")
}

/** Index of the bar that contains [today], highlighted in Lumen. */
internal fun List<ActivityBar>.indexOf(today: LocalDate, period: StatsPeriod): Int? =
    indexOfFirst { bar ->
        if (period == StatsPeriod.Year) bar.start.month == today.month else bar.start == today
    }.takeIf { it >= 0 }

private const val MINUTES_PER_HOUR = 60
private const val MONTH_LABEL_STEP = 5
