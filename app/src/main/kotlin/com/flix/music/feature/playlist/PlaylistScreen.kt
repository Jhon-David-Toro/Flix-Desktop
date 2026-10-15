package com.flix.music.feature.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.designsystem.component.ConfirmDialog
import com.flix.music.core.designsystem.component.FlixBottomSheet
import com.flix.music.core.designsystem.component.FlixTopAppBar
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.PlayPauseButton
import com.flix.music.core.designsystem.component.PlaylistArtwork
import com.flix.music.core.designsystem.component.SheetQuickAction
import com.flix.music.core.designsystem.component.SheetQuickActions
import com.flix.music.core.designsystem.component.SheetRow
import com.flix.music.core.designsystem.component.SheetTitle
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.util.toReadableLength
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import com.flix.music.core.model.totalDuration
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.core.ui.rememberShare
import com.flix.music.resources.Res
import com.flix.music.resources.action_browse_music
import com.flix.music.resources.action_cancel
import com.flix.music.resources.action_more_options
import com.flix.music.resources.action_share
import com.flix.music.resources.download_playlist_started
import com.flix.music.resources.ic_delete
import com.flix.music.resources.ic_download
import com.flix.music.resources.ic_edit
import com.flix.music.resources.ic_more_vert
import com.flix.music.resources.ic_play_arrow_filled
import com.flix.music.resources.ic_playlist_add
import com.flix.music.resources.ic_share
import com.flix.music.resources.ic_shuffle
import com.flix.music.resources.player_shuffle
import com.flix.music.resources.playlist_delete
import com.flix.music.resources.playlist_delete_confirm
import com.flix.music.resources.playlist_delete_message
import com.flix.music.resources.playlist_delete_title
import com.flix.music.resources.playlist_download
import com.flix.music.resources.playlist_edit
import com.flix.music.resources.playlist_edit_short
import com.flix.music.resources.playlist_empty_body
import com.flix.music.resources.playlist_empty_title
import com.flix.music.resources.playlist_play
import com.flix.music.resources.playlist_summary
import com.flix.music.resources.playlist_summary_empty
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/** Every user action on the Playlist screen. Defaults do nothing, which keeps previews short. */
data class PlaylistActions(
    val onBack: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onBrowseMusic: () -> Unit = {},
    val onPlay: () -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onTrackClick: (Track) -> Unit = {},
    val onTrackMore: (Track) -> Unit = {},
    val onShare: () -> Unit = {},
    val onDownload: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
fun PlaylistScreen(
    onBack: () -> Unit,
    onEdit: (playlistId: Long) -> Unit,
    onBrowseMusic: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val trackMenu = LocalTrackMenu.current
    val share = rememberShare()
    val playlist = (uiState as? PlaylistUiState.Content)?.playlist
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val downloadStartedMessage = stringResource(Res.string.download_playlist_started)

    LaunchedEffect(uiState) {
        if (uiState == PlaylistUiState.Missing) onBack()
    }

    val actions = PlaylistActions(
        onBack = onBack,
        onEdit = { playlist?.let { onEdit(it.id) } },
        onBrowseMusic = onBrowseMusic,
        onPlay = viewModel::onPlay,
        onShuffle = viewModel::onShuffle,
        onTrackClick = viewModel::onTrackClick,
        onTrackMore = { track -> trackMenu.open(track) },
        onShare = { playlist?.let { shared -> share(shared.shareText()) } },
        onDownload = {
            viewModel.onDownload()
            coroutineScope.launch { snackbarHostState.showSnackbar(downloadStartedMessage) }
        },
        onDelete = viewModel::onDelete,
    )
    PlaylistContent(
        uiState = uiState,
        actions = actions,
        nowPlaying = nowPlaying,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistContent(
    uiState: PlaylistUiState,
    actions: PlaylistActions,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val content = uiState as? PlaylistUiState.Content

    Column(modifier.fillMaxSize()) {
        FlixTopAppBar(
            title = "",
            onBack = actions.onBack,
            actions = {
                if (content != null) {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_more_vert),
                            contentDescription = stringResource(Res.string.action_more_options),
                            tint = FlixTheme.colors.textPrimary,
                        )
                    }
                }
            },
        )
        if (content == null) return@Column
        val playlist = content.playlist

        LazyColumn(contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl)) {
            item(key = "header") { PlaylistHeader(content, actions) }
            if (playlist.tracks.isEmpty()) {
                item(key = "empty") {
                    StateView(
                        icon = Res.drawable.ic_playlist_add,
                        title = stringResource(Res.string.playlist_empty_title),
                        body = stringResource(Res.string.playlist_empty_body),
                        actionLabel = stringResource(Res.string.action_browse_music),
                        onAction = actions.onBrowseMusic,
                    )
                }
            }
            items(playlist.tracks, key = { track -> track.id }) { track ->
                TrackRow(
                    title = track.title,
                    subtitle = track.artistNames,
                    artworkUrl = track.artworkUrl,
                    onClick = { actions.onTrackClick(track) },
                    isCurrent = track.id == nowPlaying.trackId,
                    isPlaying = nowPlaying.isPlaying,
                    onMoreClick = { actions.onTrackMore(track) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (menuOpen) {
            PlaylistMenu(
                playlist = playlist,
                actions = actions,
                onRequestDelete = { confirmingDelete = true },
                onDismiss = { menuOpen = false },
            )
        }
        if (confirmingDelete) {
            ConfirmDialog(
                title = stringResource(Res.string.playlist_delete_title, playlist.name),
                message = stringResource(Res.string.playlist_delete_message),
                confirmLabel = stringResource(Res.string.playlist_delete_confirm),
                dismissLabel = stringResource(Res.string.action_cancel),
                destructive = true,
                onConfirm = {
                    confirmingDelete = false
                    actions.onDelete()
                },
                onDismiss = { confirmingDelete = false },
            )
        }
    }
}

/** The playlist as text: its name, then one "Title — Artists" line per song. */
private fun Playlist.shareText(): String {
    val lines = tracks.joinToString("\n") { track -> "${track.title} — ${track.artistNames}" }
    return listOf(name, lines).filter { part -> part.isNotBlank() }.joinToString("\n\n")
}

/** Mosaic cover, name, description, "Creada por ti · N canciones · length" and actions. */
@Composable
private fun PlaylistHeader(content: PlaylistUiState.Content, actions: PlaylistActions) {
    val playlist = content.playlist
    val coverShape = RoundedCornerShape(COVER_RADIUS)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = FlixTheme.spacing.xs, bottom = FlixTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            PlaylistArtwork(
                coverUrls = playlist.coverUrls,
                size = COVER_SIZE,
                cornerRadius = COVER_RADIUS,
                stacked = playlist.coverUrls.isEmpty(),
                placeholderIconSize = 56.dp,
                modifier = if (playlist.coverUrls.isEmpty()) Modifier else Modifier.shadow(8.dp, coverShape),
            )
        }
        Column(Modifier.padding(horizontal = FlixTheme.spacing.gutter)) {
            Text(
                text = playlist.name,
                style = FlixTheme.typography.headline,
                color = FlixTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            playlist.description?.let { description ->
                Text(
                    text = description,
                    style = FlixTheme.typography.caption,
                    color = FlixTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(
                text = playlistSummary(playlist.tracks.size, content.totalDuration.toReadableLength()),
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (playlist.tracks.isNotEmpty()) {
                PlaylistActionsRow(actions)
            }
        }
    }
}

@Composable
private fun playlistSummary(trackCount: Int, length: String): String =
    if (trackCount == 0) {
        stringResource(Res.string.playlist_summary_empty)
    } else {
        pluralStringResource(Res.plurals.playlist_summary, trackCount, trackCount, length)
    }

@Composable
private fun PlaylistActionsRow(actions: PlaylistActions) {
    Row(
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.xs),
    ) {
        IconButton(onClick = actions.onEdit) {
            Icon(
                painter = painterResource(Res.drawable.ic_edit),
                contentDescription = stringResource(Res.string.playlist_edit),
                tint = FlixTheme.colors.textPrimary,
            )
        }
        IconButton(onClick = actions.onShare) {
            Icon(
                painter = painterResource(Res.drawable.ic_share),
                contentDescription = stringResource(Res.string.action_share),
                tint = FlixTheme.colors.textPrimary,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = actions.onShuffle) {
            Icon(
                painter = painterResource(Res.drawable.ic_shuffle),
                contentDescription = stringResource(Res.string.player_shuffle),
                tint = FlixTheme.colors.textPrimary,
            )
        }
        PlayPauseButton(
            icon = Res.drawable.ic_play_arrow_filled,
            contentDescription = stringResource(Res.string.playlist_play),
            onClick = actions.onPlay,
            size = PLAY_BUTTON_SIZE,
            iconSize = PLAY_ICON_SIZE,
        )
    }
}

/** Playlist menu (design "Menú: Playlist"): Reproducir · Aleatorio · Editar, then Descargar, Compartir and Eliminar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistMenu(
    playlist: Playlist,
    actions: PlaylistActions,
    onRequestDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val closeThen: (() -> Unit) -> Unit = { action ->
        coroutineScope.launch {
            sheetState.hide()
            onDismiss()
            action()
        }
    }
    FlixBottomSheet(onDismiss = onDismiss, sheetState = sheetState) {
        SheetTitle(playlist.name)
        SheetQuickActions {
            SheetQuickAction(
                icon = Res.drawable.ic_play_arrow_filled,
                label = stringResource(Res.string.playlist_play),
                onClick = { closeThen(actions.onPlay) },
            )
            SheetQuickAction(
                icon = Res.drawable.ic_shuffle,
                label = stringResource(Res.string.player_shuffle),
                onClick = { closeThen(actions.onShuffle) },
            )
            SheetQuickAction(
                icon = Res.drawable.ic_edit,
                label = stringResource(Res.string.playlist_edit_short),
                onClick = { closeThen(actions.onEdit) },
            )
        }
        SheetRow(
            icon = Res.drawable.ic_download,
            label = stringResource(Res.string.playlist_download),
            onClick = { closeThen(actions.onDownload) },
        )
        SheetRow(
            icon = Res.drawable.ic_share,
            label = stringResource(Res.string.action_share),
            onClick = { closeThen(actions.onShare) },
        )
        SheetRow(
            icon = Res.drawable.ic_delete,
            label = stringResource(Res.string.playlist_delete),
            contentColor = FlixTheme.colors.error,
            iconTint = FlixTheme.colors.error,
            onClick = { closeThen(onRequestDelete) },
        )
    }
}

private val COVER_SIZE = 184.dp
private val COVER_RADIUS = 14.dp
private val PLAY_BUTTON_SIZE = 52.dp
private val PLAY_ICON_SIZE = 28.dp

private val previewTracks = listOf(
    Track("1", "Midnight City", listOf(ArtistRef(null, "M83")), null, 243.seconds, null),
    Track("2", "Killshot", listOf(ArtistRef(null, "Eminem")), null, 253.seconds, null),
    Track("3", "Tití Me Preguntó", listOf(ArtistRef(null, "Bad Bunny")), null, 243.seconds, null),
)


