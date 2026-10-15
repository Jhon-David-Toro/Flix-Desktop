package com.flix.music.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.common.formatShortFileSize
import com.flix.music.core.designsystem.component.ConfirmDialog
import com.flix.music.core.designsystem.component.FlixTopAppBar
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.DownloadState
import com.flix.music.core.model.DownloadStorage
import com.flix.music.core.model.SongDownload
import com.flix.music.core.model.Track
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.action_cancel
import com.flix.music.resources.download_remove
import com.flix.music.resources.downloads_cancel
import com.flix.music.resources.downloads_empty_body
import com.flix.music.resources.downloads_empty_title
import com.flix.music.resources.downloads_remove
import com.flix.music.resources.downloads_remove_message
import com.flix.music.resources.downloads_remove_title
import com.flix.music.resources.downloads_retry
import com.flix.music.resources.downloads_state_downloaded
import com.flix.music.resources.downloads_state_downloading
import com.flix.music.resources.downloads_state_failed
import com.flix.music.resources.downloads_state_queued
import com.flix.music.resources.downloads_storage
import com.flix.music.resources.downloads_storage_value
import com.flix.music.resources.ic_download
import com.flix.music.resources.ic_download_done_filled
import com.flix.music.resources.ic_refresh
import com.flix.music.resources.ic_schedule
import com.flix.music.resources.profile_downloads
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Every user action on the Descargas screen. Defaults do nothing, which keeps previews short. */
data class DownloadsActions(
    val onBack: () -> Unit = {},
    val onDownloadClick: (SongDownload) -> Unit = {},
    val onRetry: (SongDownload) -> Unit = {},
    val onRemove: (SongDownload) -> Unit = {},
)

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack) {
        DownloadsActions(
            onBack = onBack,
            onDownloadClick = viewModel::onDownloadClick,
            onRetry = viewModel::onRetry,
            onRemove = viewModel::onRemove,
        )
    }
    DownloadsContent(
        uiState = uiState,
        actions = actions,
        nowPlaying = nowPlaying,
        modifier = modifier,
    )
}

/**
 * Descargas (design `downloads`): one row per song with its state on the right, which is
 * also its action (remove, cancel or retry), and the space used at the bottom.
 */
@Composable
fun DownloadsContent(
    uiState: DownloadsUiState,
    actions: DownloadsActions,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    var confirmingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    val content = uiState as? DownloadsUiState.Content

    Column(modifier.fillMaxSize()) {
        FlixTopAppBar(title = stringResource(Res.string.profile_downloads), onBack = actions.onBack)
        when {
            content == null -> Unit
            content.downloads.isEmpty() -> StateView(
                icon = Res.drawable.ic_download,
                title = stringResource(Res.string.downloads_empty_title),
                body = stringResource(Res.string.downloads_empty_body),
            )
            else -> LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = FlixTheme.spacing.xxl)) {
                items(content.downloads, key = { download -> download.track.id }) { download ->
                    DownloadRow(
                        download = download,
                        nowPlaying = nowPlaying,
                        onClick = { actions.onDownloadClick(download) },
                        onAction = {
                            when (download.state) {
                                DownloadState.Downloaded -> confirmingRemoval = download.track.id
                                DownloadState.Failed -> actions.onRetry(download)
                                DownloadState.Queued, DownloadState.Downloading -> actions.onRemove(download)
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "storage") {
                    StorageUsage(
                        storage = content.storage,
                        modifier = Modifier
                            .padding(horizontal = FlixTheme.spacing.gutter)
                            .padding(top = 18.dp),
                    )
                }
            }
        }
    }

    val removing = content?.downloads?.firstOrNull { download -> download.track.id == confirmingRemoval }
    if (removing != null) {
        ConfirmDialog(
            title = stringResource(Res.string.downloads_remove_title),
            message = stringResource(Res.string.downloads_remove_message, removing.track.title),
            confirmLabel = stringResource(Res.string.download_remove),
            dismissLabel = stringResource(Res.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmingRemoval = null
                actions.onRemove(removing)
            },
            onDismiss = { confirmingRemoval = null },
        )
    }
}

@Composable
private fun DownloadRow(
    download: SongDownload,
    nowPlaying: NowPlayingMarker,
    onClick: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackMenu = LocalTrackMenu.current
    val track = download.track
    TrackRow(
        title = track.title,
        subtitle = when (download.state) {
            DownloadState.Downloaded -> stringResource(Res.string.downloads_state_downloaded)
            DownloadState.Downloading -> stringResource(Res.string.downloads_state_downloading, download.percent)
            DownloadState.Queued -> stringResource(Res.string.downloads_state_queued)
            DownloadState.Failed -> stringResource(Res.string.downloads_state_failed)
        },
        subtitleColor = if (download.state == DownloadState.Failed) FlixTheme.colors.error else FlixTheme.colors.textSecondary,
        artworkUrl = track.artworkUrl,
        onClick = onClick,
        onLongClick = { trackMenu.open(track) },
        isCurrent = track.id == nowPlaying.trackId,
        isPlaying = nowPlaying.isPlaying,
        modifier = modifier,
        trailing = { DownloadStateButton(download, onAction) },
    )
}

/** The state icon on the right of a row (design: the five download states). */
@Composable
private fun DownloadStateButton(download: SongDownload, onClick: () -> Unit) {
    val title = download.track.title
    IconButton(onClick = onClick) {
        when (download.state) {
            DownloadState.Downloaded -> Icon(
                painter = painterResource(Res.drawable.ic_download_done_filled),
                contentDescription = stringResource(Res.string.downloads_remove, title),
                tint = FlixTheme.colors.secondary,
            )
            DownloadState.Downloading -> {
                val cancelLabel = stringResource(Res.string.downloads_cancel, title)
                CircularProgressIndicator(
                    progress = { download.percent / 100f },
                    modifier = Modifier
                        .size(PROGRESS_RING_SIZE)
                        .semantics { contentDescription = cancelLabel },
                    color = FlixTheme.colors.primary,
                    trackColor = FlixTheme.colors.surfaceVariant,
                    strokeWidth = PROGRESS_RING_STROKE,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
            }
            DownloadState.Queued -> Icon(
                painter = painterResource(Res.drawable.ic_schedule),
                contentDescription = stringResource(Res.string.downloads_cancel, title),
                tint = FlixTheme.colors.textSecondary,
            )
            DownloadState.Failed -> Icon(
                painter = painterResource(Res.drawable.ic_refresh),
                contentDescription = stringResource(Res.string.downloads_retry, title),
                tint = FlixTheme.colors.error,
            )
        }
    }
}

/** "Almacenamiento · 1,2 GB de 8 GB" over a steel beam (design `.beam`). */
@Composable
private fun StorageUsage(storage: DownloadStorage, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.downloads_storage),
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    Res.string.downloads_storage_value,
                    formatShortFileSize(storage.usedBytes),
                    formatShortFileSize(storage.totalBytes),
                ),
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textSecondary,
            )
        }
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(BEAM_HEIGHT)
                .clip(FlixTheme.shapes.xs)
                .background(FlixTheme.colors.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(storage.usedFraction)
                    .fillMaxHeight()
                    .background(FlixTheme.colors.secondary),
            )
        }
    }
}

private val PROGRESS_RING_SIZE = 22.dp
private val PROGRESS_RING_STROKE = 2.5.dp
private val BEAM_HEIGHT = 3.dp

private fun previewDownload(id: String, title: String, artist: String, state: DownloadState, percent: Int = 0) =
    SongDownload(Track(id, title, listOf(ArtistRef(null, artist)), null, null, null), state, percent)


