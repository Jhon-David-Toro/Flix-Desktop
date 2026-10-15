package com.flix.music.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.ActivityBars
import com.flix.music.core.designsystem.component.FlixSegmentedControl
import com.flix.music.core.designsystem.component.FlixTopAppBar
import com.flix.music.core.designsystem.component.SectionHeader
import com.flix.music.core.designsystem.component.Segment
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.util.toReadableLength
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.ListeningSummary
import com.flix.music.core.model.StatsPeriod
import com.flix.music.core.model.Track
import com.flix.music.core.model.summaryFor
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.ic_graphic_eq
import com.flix.music.resources.stats_average
import com.flix.music.resources.stats_bars_description
import com.flix.music.resources.stats_daily_activity
import com.flix.music.resources.stats_empty_body
import com.flix.music.resources.stats_empty_title
import com.flix.music.resources.stats_month
import com.flix.music.resources.stats_monthly_activity
import com.flix.music.resources.stats_songs
import com.flix.music.resources.stats_this_week
import com.flix.music.resources.stats_times
import com.flix.music.resources.stats_title
import com.flix.music.resources.stats_top_artists
import com.flix.music.resources.stats_week
import com.flix.music.resources.stats_year
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(uiState = uiState, onBack = onBack, onPeriodChange = viewModel::onPeriodChange, modifier = modifier)
}

/**
 * Estadísticas (design `stats`): period switch, total and daily average, activity
 * bars, and rankings drawn as horizontal bars (lengths compare better than a donut).
 */
@Composable
fun StatsContent(
    uiState: StatsUiState,
    onBack: () -> Unit,
    onPeriodChange: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        FlixTopAppBar(title = stringResource(Res.string.stats_title), onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = FlixTheme.spacing.lg)) {
            item(key = "period") {
                FlixSegmentedControl(
                    segments = listOf(
                        Segment(StatsPeriod.Week, stringResource(Res.string.stats_week)),
                        Segment(StatsPeriod.Month, stringResource(Res.string.stats_month)),
                        Segment(StatsPeriod.Year, stringResource(Res.string.stats_year)),
                    ),
                    selected = uiState.period,
                    onSelect = onPeriodChange,
                    modifier = Modifier.padding(horizontal = FlixTheme.spacing.gutter),
                )
            }
            val summary = uiState.summary ?: return@LazyColumn
            item(key = "total") { StatsTotal(summary, uiState.today) }
            if (summary.plays == 0) {
                item(key = "empty") {
                    StateView(
                        icon = Res.drawable.ic_graphic_eq,
                        title = stringResource(Res.string.stats_empty_title),
                        body = stringResource(Res.string.stats_empty_body),
                    )
                }
                return@LazyColumn
            }
            item(key = "activity") {
                SectionHeader(
                    title = stringResource(
                        if (summary.period == StatsPeriod.Year) Res.string.stats_monthly_activity else Res.string.stats_daily_activity,
                    ),
                    modifier = Modifier.padding(top = SECTION_TOP, bottom = FlixTheme.spacing.sm),
                )
                ActivityBars(
                    values = summary.activity.map { bar -> bar.listened.inWholeMinutes.toFloat() },
                    labels = activityLabels(summary.period, summary.activity),
                    highlightedIndex = summary.activity.indexOf(uiState.today, summary.period),
                    description = stringResource(Res.string.stats_bars_description),
                    modifier = Modifier.padding(horizontal = FlixTheme.spacing.gutter),
                )
            }
            item(key = "artists-header") {
                SectionHeader(title = stringResource(Res.string.stats_top_artists), modifier = Modifier.padding(top = SECTION_TOP))
            }
            val topArtistTime = summary.topArtists.firstOrNull()?.listened?.inWholeSeconds?.coerceAtLeast(1) ?: 1
            itemsIndexed(summary.topArtists, key = { _, listening -> "artist-${listening.artist.id ?: listening.artist.name}" }) { index, listening ->
                RankRow(
                    position = index + 1,
                    title = listening.artist.name,
                    value = listening.listened.toRankingLength(),
                    share = listening.listened.inWholeSeconds.toFloat() / topArtistTime,
                    artworkUrl = listening.artworkUrl,
                    circular = true,
                )
            }
            item(key = "songs-header") {
                SectionHeader(title = stringResource(Res.string.stats_songs), modifier = Modifier.padding(top = SECTION_TOP))
            }
            val topPlays = summary.topTracks.firstOrNull()?.plays?.coerceAtLeast(1) ?: 1
            itemsIndexed(summary.topTracks, key = { _, trackPlays -> "track-${trackPlays.track.id}" }) { index, trackPlays ->
                RankRow(
                    position = index + 1,
                    title = trackPlays.track.title,
                    value = pluralStringResource(Res.plurals.stats_times, trackPlays.plays, trackPlays.plays),
                    share = trackPlays.plays.toFloat() / topPlays,
                    artworkUrl = trackPlays.track.artworkUrl,
                    circular = false,
                )
            }
        }
    }
}

/** "Septiembre" · "23 h 14 min" · "≈ 45 min al día · 312 canciones". */
@Composable
private fun StatsTotal(summary: ListeningSummary, today: LocalDate) {
    val periodName = when (summary.period) {
        StatsPeriod.Week -> stringResource(Res.string.stats_this_week)
        StatsPeriod.Month -> summary.start.monthName()
        StatsPeriod.Year -> today.year.toString()
    }
    Column(
        Modifier
            .padding(horizontal = FlixTheme.spacing.gutter)
            .padding(top = 18.dp, bottom = FlixTheme.spacing.xs),
    ) {
        Text(periodName, style = FlixTheme.typography.caption, color = FlixTheme.colors.textSecondary)
        Text(
            text = summary.listened.toReadableLength(),
            style = FlixTheme.typography.headline.copy(fontSize = 34.sp, lineHeight = 34.sp, fontFeatureSettings = "tnum"),
            color = FlixTheme.colors.textPrimary,
            modifier = Modifier.padding(top = FlixTheme.spacing.xs),
        )
        Text(
            text = pluralStringResource(
                Res.plurals.stats_average,
                summary.plays,
                summary.averagePerDay.toReadableLength(),
                summary.plays,
            ),
            style = FlixTheme.typography.caption,
            color = FlixTheme.colors.textSecondary,
        )
    }
}

/** Ranking row: position, cover, name and a bar whose length is its share of the first. */
@Composable
private fun RankRow(position: Int, title: String, value: String, share: Float, artworkUrl: String?, circular: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(horizontal = FlixTheme.spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) {
            Text(
                text = position.toString(),
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textTertiary,
            )
        }
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(if (circular) FlixTheme.shapes.circle else FlixTheme.shapes.xs)
                .background(FlixTheme.colors.surfaceVariant),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = FlixTheme.typography.subtitle,
                color = FlixTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm)) {
                Box(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(share.coerceIn(MIN_SHARE, 1f))
                            .height(4.dp)
                            .alpha(RANK_BAR_ALPHA)
                            .clip(FlixTheme.shapes.pill)
                            .background(FlixTheme.colors.textSecondary),
                    )
                }
                Text(value, style = FlixTheme.typography.caption, color = FlixTheme.colors.textSecondary)
            }
        }
    }
}

private const val RANK_BAR_ALPHA = 0.55f
private const val MIN_SHARE = 0.04f
private val SECTION_TOP = 22.dp

