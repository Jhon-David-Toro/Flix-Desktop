package com.flix.music.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.designsystem.component.ConfirmDialog
import com.flix.music.core.designsystem.component.FlixTopAppBar
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.SwipeToRemove
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.showUndo
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.HistoryDay
import com.flix.music.core.model.HistoryDayLabel
import com.flix.music.core.model.HistoryEntry
import com.flix.music.core.model.Track
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.action_cancel
import com.flix.music.resources.action_explore_genres
import com.flix.music.resources.action_more_options
import com.flix.music.resources.action_undo
import com.flix.music.resources.history_clear
import com.flix.music.resources.history_clear_confirm
import com.flix.music.resources.history_clear_message
import com.flix.music.resources.history_clear_title
import com.flix.music.resources.history_empty_body
import com.flix.music.resources.history_empty_title
import com.flix.music.resources.history_entry_removed
import com.flix.music.resources.history_remove_entry
import com.flix.music.resources.history_title
import com.flix.music.resources.history_today
import com.flix.music.resources.history_yesterday
import com.flix.music.resources.ic_history
import com.flix.music.resources.ic_more_vert
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Every user action on the History screen. Defaults do nothing, which keeps previews short. */
data class HistoryActions(
    val onBack: () -> Unit = {},
    val onBrowseMusic: () -> Unit = {},
    val onEntryClick: (HistoryEntry) -> Unit = {},
    val onRemove: (HistoryEntry) -> Unit = {},
    val onUndoRemove: (HistoryEntry) -> Unit = {},
    val onClearHistory: () -> Unit = {},
)

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onBrowseMusic: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack, onBrowseMusic) {
        HistoryActions(
            onBack = onBack,
            onBrowseMusic = onBrowseMusic,
            onEntryClick = viewModel::onEntryClick,
            onRemove = viewModel::onRemove,
            onUndoRemove = viewModel::onUndoRemove,
            onClearHistory = viewModel::onClearHistory,
        )
    }
    HistoryContent(
        uiState = uiState,
        actions = actions,
        nowPlaying = nowPlaying,
        modifier = modifier,
    )
}

@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    actions: HistoryActions,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    val days = (uiState as? HistoryUiState.Content)?.days
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val removedMessage = stringResource(Res.string.history_entry_removed)
    val undoLabel = stringResource(Res.string.action_undo)
    val removeLabel = stringResource(Res.string.history_remove_entry)

    val removeWithUndo: (HistoryEntry) -> Unit = { entry ->
        actions.onRemove(entry)
        coroutineScope.launch {
            snackbarHostState.showUndo(removedMessage, undoLabel) { actions.onUndoRemove(entry) }
        }
    }

    Column(modifier.fillMaxSize()) {
        FlixTopAppBar(
            title = stringResource(Res.string.history_title),
            onBack = actions.onBack,
            actions = {
                if (!days.isNullOrEmpty()) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_more_vert),
                                contentDescription = stringResource(Res.string.action_more_options),
                                tint = FlixTheme.colors.textPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = FlixTheme.colors.surfaceElevated,
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(Res.string.history_clear), color = FlixTheme.colors.textPrimary)
                                },
                                onClick = {
                                    menuExpanded = false
                                    confirmingClear = true
                                },
                            )
                        }
                    }
                }
            },
        )

        when {
            days == null -> Unit
            days.isEmpty() -> StateView(
                icon = Res.drawable.ic_history,
                title = stringResource(Res.string.history_empty_title),
                body = stringResource(Res.string.history_empty_body),
                actionLabel = stringResource(Res.string.action_explore_genres),
                onAction = actions.onBrowseMusic,
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl)) {
                days.forEach { day ->
                    stickyHeader(key = "day-${day.entries.first().id}") { DayHeader(day.label) }
                    items(day.entries, key = { it.id }) { entry ->
                        val trackMenu = LocalTrackMenu.current
                        SwipeToRemove(
                            onRemove = { removeWithUndo(entry) },
                            modifier = Modifier.animateItem(),
                        ) {
                            TrackRow(
                                title = entry.track.title,
                                subtitle = entry.track.artistNames,
                                artworkUrl = entry.track.artworkUrl,
                                onClick = { actions.onEntryClick(entry) },
                                onLongClick = { trackMenu.open(entry.track) { removeWithUndo(entry) } },
                                isCurrent = entry.track.id == nowPlaying.trackId,
                                isPlaying = nowPlaying.isPlaying,
                                modifier = Modifier
                                    .background(FlixTheme.colors.background)
                                    .semantics {
                                        customActions = listOf(
                                            CustomAccessibilityAction(removeLabel) {
                                                removeWithUndo(entry)
                                                true
                                            },
                                        )
                                    },
                                trailing = {
                                    Text(
                                        text = entry.playedAt.formatTime(Locale.getDefault()),
                                        style = FlixTheme.typography.caption,
                                        color = FlixTheme.colors.textTertiary,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmingClear) {
        ConfirmDialog(
            title = stringResource(Res.string.history_clear_title),
            message = stringResource(Res.string.history_clear_message),
            confirmLabel = stringResource(Res.string.history_clear_confirm),
            dismissLabel = stringResource(Res.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmingClear = false
                actions.onClearHistory()
            },
            onDismiss = { confirmingClear = false },
        )
    }
}

/** Sticky day header (design: overline "HOY", "AYER", "LUNES, 21 SEP"). */
@Composable
private fun DayHeader(label: HistoryDayLabel) {
    val text = when (label) {
        HistoryDayLabel.Today -> stringResource(Res.string.history_today)
        HistoryDayLabel.Yesterday -> stringResource(Res.string.history_yesterday)
        is HistoryDayLabel.Date -> {
            val locale = Locale.getDefault()
            val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern(DAY_PATTERN, locale) }
            label.date.format(dayFormatter)
        }
    }
    Text(
        text = text.uppercase(),
        style = FlixTheme.typography.overline,
        color = FlixTheme.colors.textSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .background(FlixTheme.colors.background)
            .padding(horizontal = FlixTheme.spacing.gutter)
            .padding(top = FlixTheme.spacing.lg, bottom = FlixTheme.spacing.xs)
            .semantics { heading() },
    )
}

/** Time of day of a play in the user's format, e.g. "9:40 p. m.". */
private fun Instant.formatTime(locale: Locale): String =
    atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FORMATTER.withLocale(locale))

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private const val DAY_PATTERN = "EEEE, d MMM"

private fun previewEntry(id: Long, title: String, artist: String) =
    HistoryEntry(id, Track(id.toString(), title, listOf(ArtistRef(null, artist)), null, null, null), Instant.EPOCH)


