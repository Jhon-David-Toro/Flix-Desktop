package com.flix.music.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.ActivityBars
import com.flix.music.core.designsystem.component.FlixCard
import com.flix.music.core.designsystem.component.FlixTopAppBar
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.ProfileAvatar
import com.flix.music.core.designsystem.component.SettingsChevron
import com.flix.music.core.designsystem.component.SettingsRow
import com.flix.music.core.designsystem.component.TextButton
import com.flix.music.core.designsystem.component.util.toReadableLength
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.StatsPeriod
import com.flix.music.core.model.Track
import com.flix.music.core.model.listenedInPreviousMonth
import com.flix.music.core.model.summaryFor
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.history_title
import com.flix.music.resources.ic_download
import com.flix.music.resources.ic_history
import com.flix.music.resources.ic_settings
import com.flix.music.resources.profile_artists
import com.flix.music.resources.profile_downloads
import com.flix.music.resources.profile_favorites
import com.flix.music.resources.profile_month_comparison
import com.flix.music.resources.profile_month_empty
import com.flix.music.resources.profile_playlists
import com.flix.music.resources.profile_see_stats
import com.flix.music.resources.profile_this_month
import com.flix.music.resources.profile_title
import com.flix.music.resources.profile_top_artist
import com.flix.music.resources.profile_top_song
import com.flix.music.resources.settings_title
import com.flix.music.resources.stats_bars_description
import com.flix.music.resources.stats_computed_locally
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

/** Every user action on the profile screen. Defaults do nothing, which keeps previews short. */
data class ProfileActions(
    val onBack: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenStats: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onOpenDownloads: () -> Unit = {},
)

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        uiState = uiState,
        actions = ProfileActions(onBack, onOpenSettings, onOpenStats, onOpenHistory, onOpenDownloads),
        modifier = modifier,
    )
}

@Composable
fun ProfileContent(uiState: ProfileUiState, actions: ProfileActions, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        FlixTopAppBar(
            title = "",
            onBack = actions.onBack,
            actions = {
                IconButton(onClick = actions.onOpenSettings) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_settings),
                        contentDescription = stringResource(Res.string.settings_title),
                        tint = FlixTheme.colors.textPrimary,
                    )
                }
            },
        )
        LazyColumn(contentPadding = PaddingValues(bottom = FlixTheme.spacing.lg)) {
            item(key = "identity") { ProfileIdentity(uiState) }
            item(key = "month") {
                MonthCard(
                    uiState = uiState,
                    onOpenStats = actions.onOpenStats,
                    modifier = Modifier
                        .padding(horizontal = FlixTheme.spacing.gutter)
                        .padding(top = 18.dp),
                )
            }
            item(key = "links") {
                Column(Modifier.padding(top = 10.dp)) {
                    SettingsRow(
                        title = stringResource(Res.string.history_title),
                        icon = Res.drawable.ic_history,
                        onClick = actions.onOpenHistory,
                    ) { SettingsChevron() }
                    SettingsRow(
                        title = stringResource(Res.string.profile_downloads),
                        icon = Res.drawable.ic_download,
                        onClick = actions.onOpenDownloads,
                    ) { SettingsChevron() }
                    SettingsRow(
                        title = stringResource(Res.string.settings_title),
                        icon = Res.drawable.ic_settings,
                        onClick = actions.onOpenSettings,
                    ) { SettingsChevron() }
                }
            }
        }
    }
}

/** 64dp avatar, "Tu perfil" and "6 playlists · 128 favoritos · 14 artistas". */
@Composable
private fun ProfileIdentity(uiState: ProfileUiState) {
    Row(
        modifier = Modifier.padding(horizontal = FlixTheme.spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileAvatar(size = 64.dp)
        Column {
            Text(
                text = stringResource(Res.string.profile_title),
                style = FlixTheme.typography.titleLarge,
                color = FlixTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = listOf(
                    pluralStringResource(Res.plurals.profile_playlists, uiState.playlistCount, uiState.playlistCount),
                    pluralStringResource(Res.plurals.profile_favorites, uiState.favoriteCount, uiState.favoriteCount),
                    pluralStringResource(Res.plurals.profile_artists, uiState.artistCount, uiState.artistCount),
                ).joinToString(" · "),
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textSecondary,
            )
        }
    }
}

/**
 * "Este mes" (design): total listened, the change against last month, this week's
 * bars with today in Lumen, and the top artist and song.
 */
@Composable
private fun MonthCard(uiState: ProfileUiState, onOpenStats: () -> Unit, modifier: Modifier = Modifier) {
    val month = uiState.month
    FlixCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.profile_this_month).uppercase(),
                    style = FlixTheme.typography.overline,
                    color = FlixTheme.colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(Res.string.stats_computed_locally),
                    style = FlixTheme.typography.caption,
                    color = FlixTheme.colors.textSecondary,
                )
            }
            if (month == null || !uiState.hasListening) {
                Text(
                    text = stringResource(Res.string.profile_month_empty),
                    style = FlixTheme.typography.bodySmall,
                    color = FlixTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = FlixTheme.spacing.md),
                )
                return@Column
            }
            Text(
                text = month.listened.toReadableLength(),
                style = FlixTheme.typography.headline.copy(fontSize = 30.sp, lineHeight = 30.sp, fontFeatureSettings = "tnum"),
                color = FlixTheme.colors.textPrimary,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
            Text(
                text = stringResource(
                    Res.string.profile_month_comparison,
                    (month.listened - uiState.previousMonthListened).toSignedLength(),
                    uiState.today.minusMonths(1).monthName().lowercase(),
                ),
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textSecondary,
            )
            uiState.week?.let { week ->
                ActivityBars(
                    values = week.activity.map { bar -> bar.listened.inWholeMinutes.toFloat() },
                    labels = activityLabels(StatsPeriod.Week, week.activity),
                    highlightedIndex = week.activity.indexOf(uiState.today, StatsPeriod.Week),
                    description = stringResource(Res.string.stats_bars_description),
                    chartHeight = 56.dp,
                    modifier = Modifier.padding(top = FlixTheme.spacing.md),
                )
            }
            HorizontalDivider(color = FlixTheme.colors.divider, modifier = Modifier.padding(vertical = 10.dp))
            month.topArtists.firstOrNull()?.let { top ->
                TopRow(
                    caption = stringResource(Res.string.profile_top_artist),
                    title = top.artist.name,
                    artworkUrl = top.artworkUrl,
                    circular = true,
                )
            }
            month.topTracks.firstOrNull()?.let { top ->
                TopRow(
                    caption = stringResource(Res.string.profile_top_song),
                    title = top.track.title,
                    artworkUrl = top.track.artworkUrl,
                    circular = false,
                )
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onOpenStats) {
                    Text(
                        text = stringResource(Res.string.profile_see_stats),
                        style = FlixTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = FlixTheme.colors.primaryContent,
                    )
                }
            }
        }
    }
}

/** "Artista top" / "Canción top": caption above the name (design puts the label first). */
@Composable
private fun TopRow(caption: String, title: String, artworkUrl: String?, circular: Boolean) {
    Row(
        modifier = Modifier.heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(if (circular) FlixTheme.shapes.circle else FlixTheme.shapes.xs)
                .background(FlixTheme.colors.surfaceVariant),
        )
        Column {
            Text(caption, style = FlixTheme.typography.bodySmall, color = FlixTheme.colors.textSecondary)
            Text(
                text = title,
                style = FlixTheme.typography.subtitle,
                color = FlixTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

