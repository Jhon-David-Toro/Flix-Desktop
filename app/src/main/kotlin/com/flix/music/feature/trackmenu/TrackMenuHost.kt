package com.flix.music.feature.trackmenu

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.designsystem.component.AddToPlaylistSheet
import com.flix.music.core.designsystem.component.CreatePlaylistDialog
import com.flix.music.core.designsystem.component.FlixBottomSheet
import com.flix.music.core.designsystem.component.FlixSnackbarVisuals
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.SheetHeader
import com.flix.music.core.designsystem.component.SheetQuickAction
import com.flix.music.core.designsystem.component.SheetQuickActions
import com.flix.music.core.designsystem.component.SheetRow
import com.flix.music.core.designsystem.theme.FlixMotion
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.Track
import com.flix.music.core.ui.LocalDetailNavigator
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.TrackMenuLauncher
import com.flix.music.core.ui.flixViewModel
import com.flix.music.core.ui.invoke
import com.flix.music.core.ui.rememberShare
import com.flix.music.core.ui.rememberStrings
import com.flix.music.resources.Res
import com.flix.music.resources.action_share
import com.flix.music.resources.action_view
import com.flix.music.resources.add_to_playlist
import com.flix.music.resources.add_to_playlist_done
import com.flix.music.resources.download
import com.flix.music.resources.download_remove
import com.flix.music.resources.download_removed
import com.flix.music.resources.download_started
import com.flix.music.resources.favorite_added
import com.flix.music.resources.favorite_removed
import com.flix.music.resources.history_remove_entry
import com.flix.music.resources.ic_add_to_queue
import com.flix.music.resources.ic_album
import com.flix.music.resources.ic_check_circle_filled
import com.flix.music.resources.ic_delete
import com.flix.music.resources.ic_download
import com.flix.music.resources.ic_download_done_filled
import com.flix.music.resources.ic_favorite
import com.flix.music.resources.ic_favorite_filled
import com.flix.music.resources.ic_person
import com.flix.music.resources.ic_playlist_add
import com.flix.music.resources.ic_playlist_play
import com.flix.music.resources.ic_share
import com.flix.music.resources.track_add_to_queue
import com.flix.music.resources.track_add_to_queue_done
import com.flix.music.resources.track_favorite
import com.flix.music.resources.track_go_to_album
import com.flix.music.resources.track_go_to_artist
import com.flix.music.resources.track_play_next
import com.flix.music.resources.track_play_next_done
import com.flix.music.resources.track_share_text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Which part of the song menu flow is on screen. */
private sealed interface TrackMenuStep {
    val track: Track

    data class Actions(override val track: Track, val onRemoveFromHistory: (() -> Unit)?) : TrackMenuStep
    data class PickPlaylist(override val track: Track) : TrackMenuStep
    data class CreatePlaylist(override val track: Track) : TrackMenuStep
}

/**
 * Hosts the song menu for the whole app: provides [LocalTrackMenu] to [content] and
 * shows the menu sheet, the "Añadir a playlist" picker and the "Nueva playlist" dialog.
 * "Ver" on the confirmation snackbar calls [onOpenPlaylist].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuHost(
    onOpenPlaylist: (playlistId: Long) -> Unit,
    viewModel: TrackMenuViewModel = flixViewModel(),
    content: @Composable () -> Unit,
) {
    var step by remember { mutableStateOf<TrackMenuStep?>(null) }
    val launcher = remember {
        object : TrackMenuLauncher {
            override fun open(track: Track, onRemoveFromHistory: (() -> Unit)?) {
                step = TrackMenuStep.Actions(track, onRemoveFromHistory)
            }

            override fun addToPlaylist(track: Track) {
                step = TrackMenuStep.PickPlaylist(track)
            }
        }
    }
    val detailNavigator = LocalDetailNavigator.current
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val downloadIds by viewModel.downloadIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val share = rememberShare()
    val text = rememberStrings(
        Res.string.action_view,
        Res.string.add_to_playlist_done,
        Res.string.download_removed,
        Res.string.download_started,
        Res.string.favorite_added,
        Res.string.favorite_removed,
        Res.string.track_add_to_queue_done,
        Res.string.track_play_next_done,
        Res.string.track_share_text,
    )
    val coroutineScope = rememberCoroutineScope()

    fun confirmAdded(playlistName: String, playlistId: Long) {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                FlixSnackbarVisuals(
                    message = text(Res.string.add_to_playlist_done, playlistName),
                    icon = Res.drawable.ic_check_circle_filled,
                    actionLabel = text(Res.string.action_view),
                ),
            )
            if (result == SnackbarResult.ActionPerformed) onOpenPlaylist(playlistId)
        }
    }

    fun confirm(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    CompositionLocalProvider(LocalTrackMenu provides launcher, content = content)

    when (val currentStep = step) {
        null -> Unit
        is TrackMenuStep.Actions -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val track = currentStep.track
            val isFavorite = track.id in favoriteIds

            fun closeThen(action: () -> Unit) {
                coroutineScope.launch {
                    sheetState.hide()
                    step = null
                    action()
                }
            }

            FlixBottomSheet(onDismiss = { step = null }, sheetState = sheetState) {
                SheetHeader(
                    title = track.title,
                    subtitle = listOfNotNull(track.artistNames, track.album?.name)
                        .filter { part -> part.isNotBlank() }
                        .joinToString(" · "),
                    artworkUrl = track.artworkUrl,
                )
                SheetQuickActions {
                    SheetQuickAction(
                        icon = Res.drawable.ic_playlist_play,
                        label = stringResource(Res.string.track_play_next),
                        onClick = {
                            closeThen {
                                viewModel.onPlayNext(track)
                                confirm(text(Res.string.track_play_next_done))
                            }
                        },
                    )
                    SheetQuickAction(
                        icon = Res.drawable.ic_add_to_queue,
                        label = stringResource(Res.string.track_add_to_queue),
                        onClick = {
                            closeThen {
                                viewModel.onAddToQueue(track)
                                confirm(text(Res.string.track_add_to_queue_done))
                            }
                        },
                    )
                    SheetQuickAction(
                        icon = if (isFavorite) Res.drawable.ic_favorite_filled else Res.drawable.ic_favorite,
                        label = stringResource(Res.string.track_favorite),
                        iconTint = if (isFavorite) FlixTheme.colors.primaryContent else FlixTheme.colors.textPrimary,
                        onClick = {
                            closeThen {
                                viewModel.onToggleFavorite(track)
                                confirm(
                                    text(
                                        if (isFavorite) Res.string.favorite_removed else Res.string.favorite_added,
                                    ),
                                )
                            }
                        },
                    )
                }
                SheetRow(
                    icon = Res.drawable.ic_playlist_add,
                    label = stringResource(Res.string.add_to_playlist),
                    onClick = { closeThen { step = TrackMenuStep.PickPlaylist(track) } },
                )
                if (track.id in downloadIds) {
                    SheetRow(
                        icon = Res.drawable.ic_download_done_filled,
                        label = stringResource(Res.string.download_remove),
                        onClick = {
                            closeThen {
                                viewModel.onRemoveDownload(track)
                                confirm(text(Res.string.download_removed))
                            }
                        },
                    )
                } else {
                    SheetRow(
                        icon = Res.drawable.ic_download,
                        label = stringResource(Res.string.download),
                        onClick = {
                            closeThen {
                                viewModel.onDownload(track)
                                confirm(text(Res.string.download_started))
                            }
                        },
                    )
                }
                track.artists.firstOrNull()?.let { artist ->
                    SheetRow(
                        icon = Res.drawable.ic_person,
                        label = stringResource(Res.string.track_go_to_artist),
                        onClick = { closeThen { detailNavigator.openArtist(artist) } },
                    )
                }
                track.album?.let { album ->
                    SheetRow(
                        icon = Res.drawable.ic_album,
                        label = stringResource(Res.string.track_go_to_album),
                        onClick = { closeThen { detailNavigator.openAlbum(album, track.artists.firstOrNull()) } },
                    )
                }
                SheetRow(
                    icon = Res.drawable.ic_share,
                    label = stringResource(Res.string.action_share),
                    onClick = { closeThen { share(text(Res.string.track_share_text, track.title, track.artistNames, track.id)) } },
                )
                currentStep.onRemoveFromHistory?.let { removeFromHistory ->
                    SheetRow(
                        icon = Res.drawable.ic_delete,
                        label = stringResource(Res.string.history_remove_entry),
                        onClick = { closeThen(removeFromHistory) },
                    )
                }
            }
        }
        is TrackMenuStep.PickPlaylist -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var pickedPlaylistId by remember { mutableStateOf<Long?>(null) }
            AddToPlaylistSheet(
                trackId = currentStep.track.id,
                playlists = playlists,
                pickedPlaylistId = pickedPlaylistId,
                sheetState = sheetState,
                onDismiss = { step = null },
                onCreateNew = {
                    coroutineScope.launch {
                        sheetState.hide()
                        step = TrackMenuStep.CreatePlaylist(currentStep.track)
                    }
                },
                onPick = { playlist ->
                    pickedPlaylistId = playlist.id
                    coroutineScope.launch {
                        val added = viewModel.addToPlaylist(playlist, currentStep.track)
                        delay(PICK_CONFIRMATION_MILLIS)
                        sheetState.hide()
                        step = null
                        if (added) confirmAdded(playlist.name, playlist.id)
                    }
                },
            )
        }
        is TrackMenuStep.CreatePlaylist -> CreatePlaylistDialog(
            onDismiss = { step = null },
            onCreate = { name, description ->
                step = null
                coroutineScope.launch {
                    val playlistId = viewModel.createPlaylistWith(name, description, currentStep.track)
                    confirmAdded(name.trim(), playlistId)
                }
            },
        )
    }
}

/** Check shown on the picked playlist before closing (design: fast + 300 ms). */
private const val PICK_CONFIRMATION_MILLIS = FlixMotion.FAST + 300L
