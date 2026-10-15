package com.flix.music.feature.playlist

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.ConfirmDialog
import com.flix.music.core.designsystem.component.FlixPrimaryButton
import com.flix.music.core.designsystem.component.FlixTextField
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.PlaylistArtwork
import com.flix.music.core.designsystem.component.SwipeToRemove
import com.flix.music.core.designsystem.component.TextButton
import com.flix.music.core.designsystem.component.showUndo
import com.flix.music.core.designsystem.component.util.ReorderState
import com.flix.music.core.designsystem.component.util.draggedRow
import com.flix.music.core.designsystem.component.util.rememberReorderState
import com.flix.music.core.designsystem.component.util.reorderHandle
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.action_cancel
import com.flix.music.resources.action_close
import com.flix.music.resources.action_move_down
import com.flix.music.resources.action_move_up
import com.flix.music.resources.action_save
import com.flix.music.resources.action_undo
import com.flix.music.resources.ic_close
import com.flix.music.resources.ic_delete
import com.flix.music.resources.ic_do_not_disturb_on_filled
import com.flix.music.resources.ic_drag_indicator
import com.flix.music.resources.playlist_delete
import com.flix.music.resources.playlist_delete_confirm
import com.flix.music.resources.playlist_delete_message
import com.flix.music.resources.playlist_delete_title
import com.flix.music.resources.playlist_description_placeholder
import com.flix.music.resources.playlist_discard_confirm
import com.flix.music.resources.playlist_discard_message
import com.flix.music.resources.playlist_discard_title
import com.flix.music.resources.playlist_edit
import com.flix.music.resources.playlist_field_description
import com.flix.music.resources.playlist_field_name
import com.flix.music.resources.playlist_keep_editing
import com.flix.music.resources.playlist_name_counter
import com.flix.music.resources.playlist_remove_track
import com.flix.music.resources.playlist_remove_track_named
import com.flix.music.resources.playlist_track_removed
import com.flix.music.resources.songs_count
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Every user action on the Edit playlist screen. Defaults do nothing, which keeps previews short. */
data class PlaylistEditActions(
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onRemove: (Track) -> Int = { -1 },
    val onUndoRemove: (Track, Int) -> Unit = { _, _ -> },
    val onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    val onDelete: () -> Unit = {},
)

/** What the form shows: the draft plus whether it can be saved or would be lost. */
data class PlaylistEditForm(
    val name: String,
    val description: String,
    val tracks: List<Track>,
    val canSave: Boolean,
    val hasChanges: Boolean,
)

@Composable
fun PlaylistEditScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistEditViewModel = flixViewModel(),
) {
    LaunchedEffect(viewModel.isFinished) {
        if (viewModel.isFinished) onClose()
    }
    if (!viewModel.isLoaded) return

    val actions = remember(viewModel, onClose) {
        PlaylistEditActions(
            onClose = onClose,
            onSave = viewModel::onSave,
            onNameChange = viewModel::onNameChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onRemove = viewModel::onRemove,
            onUndoRemove = viewModel::onUndoRemove,
            onMove = viewModel::onMove,
            onDelete = viewModel::onDelete,
        )
    }
    PlaylistEditContent(
        form = PlaylistEditForm(
            name = viewModel.name,
            description = viewModel.description,
            tracks = viewModel.tracks,
            canSave = viewModel.canSave,
            hasChanges = viewModel.hasChanges,
        ),
        actions = actions,
        modifier = modifier,
    )
}

@Composable
fun PlaylistEditContent(
    form: PlaylistEditForm,
    actions: PlaylistEditActions,
    modifier: Modifier = Modifier,
) {
    var confirmingDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderState(listState) { key -> key is String && key !in NON_TRACK_KEYS }
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val removedMessage = stringResource(Res.string.playlist_track_removed)
    val undoLabel = stringResource(Res.string.action_undo)
    val requestClose = { if (form.hasChanges) confirmingDiscard = true else actions.onClose() }

    fun removeWithUndo(track: Track) {
        val index = actions.onRemove(track)
        if (index < 0) return
        coroutineScope.launch {
            snackbarHostState.showUndo(removedMessage, undoLabel) { actions.onUndoRemove(track, index) }
        }
    }


    Column(modifier.fillMaxSize()) {
        EditTopBar(canSave = form.canSave, onClose = requestClose, onSave = actions.onSave)
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl)) {
            item(key = "form") { EditForm(form, actions) }
            item(key = "count") {
                Text(
                    text = pluralStringResource(Res.plurals.songs_count, form.tracks.size, form.tracks.size).uppercase(),
                    style = FlixTheme.typography.overline,
                    color = FlixTheme.colors.textSecondary,
                    modifier = Modifier
                        .padding(horizontal = FlixTheme.spacing.gutter)
                        .padding(top = FlixTheme.spacing.xxl, bottom = FlixTheme.spacing.sm),
                )
            }
            itemsIndexed(form.tracks, key = { _, track -> track.id }) { index, track ->
                val isDragged = reorder.draggedKey == track.id
                SwipeToRemove(
                    onRemove = { removeWithUndo(track) },
                    modifier = if (isDragged) Modifier.draggedRow(reorder, track.id) else Modifier.animateItem(),
                ) {
                    EditTrackRow(
                        track = track,
                        index = index,
                        lastIndex = form.tracks.lastIndex,
                        isDragged = isDragged,
                        onRemove = { removeWithUndo(track) },
                        onMove = actions.onMove,
                        reorder = reorder,
                        tracks = form.tracks,
                    )
                }
            }
            item(key = "delete") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { confirmingDelete = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_delete),
                            contentDescription = null,
                            tint = FlixTheme.colors.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(Res.string.playlist_delete),
                            style = FlixTheme.typography.label,
                            color = FlixTheme.colors.error,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }

    if (confirmingDiscard) {
        ConfirmDialog(
            title = stringResource(Res.string.playlist_discard_title),
            message = stringResource(Res.string.playlist_discard_message),
            confirmLabel = stringResource(Res.string.playlist_discard_confirm),
            dismissLabel = stringResource(Res.string.playlist_keep_editing),
            destructive = true,
            onConfirm = {
                confirmingDiscard = false
                actions.onClose()
            },
            onDismiss = { confirmingDiscard = false },
        )
    }
    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.playlist_delete_title, form.name),
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

/** `close` · "Editar playlist" · small "Guardar" button. */
@Composable
private fun EditTopBar(canSave: Boolean, onClose: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = FlixTheme.spacing.xs, end = FlixTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(Res.drawable.ic_close),
                contentDescription = stringResource(Res.string.action_close),
                tint = FlixTheme.colors.textPrimary,
            )
        }
        Text(
            text = stringResource(Res.string.playlist_edit),
            style = FlixTheme.typography.title,
            color = FlixTheme.colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        FlixPrimaryButton(
            text = stringResource(Res.string.action_save),
            onClick = onSave,
            enabled = canSave,
            compact = true,
        )
    }
}

@Composable
private fun EditForm(form: PlaylistEditForm, actions: PlaylistEditActions) {
    Column(Modifier.padding(horizontal = FlixTheme.spacing.gutter)) {
        PlaylistArtwork(
            coverUrls = form.tracks.mapNotNull { track -> track.artworkUrl }.distinct().let { urls ->
                if (urls.size >= 4) urls.take(4) else urls.take(1)
            },
            size = 84.dp,
            cornerRadius = 10.dp,
            stacked = false,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        FlixTextField(
            value = form.name,
            onValueChange = actions.onNameChange,
            label = stringResource(Res.string.playlist_field_name),
            helper = stringResource(Res.string.playlist_name_counter, form.name.length, Playlist.MAX_NAME_LENGTH),
            maxLength = Playlist.MAX_NAME_LENGTH,
        )
        FlixTextField(
            value = form.description,
            onValueChange = actions.onDescriptionChange,
            label = stringResource(Res.string.playlist_field_description),
            placeholder = stringResource(Res.string.playlist_description_placeholder),
            singleLine = false,
            modifier = Modifier.padding(top = FlixTheme.spacing.md),
        )
    }
}

/**
 * Song row in edit mode: ⊖ removes it, the handle reorders it by dragging. Screen
 * readers get "Mover arriba / abajo" and "Quitar" instead of the gestures.
 */
@Composable
private fun EditTrackRow(
    track: Track,
    index: Int,
    lastIndex: Int,
    isDragged: Boolean,
    onRemove: () -> Unit,
    onMove: (Int, Int) -> Unit,
    reorder: ReorderState,
    tracks: List<Track>,
) {
    val moveUpLabel = stringResource(Res.string.action_move_up)
    val moveDownLabel = stringResource(Res.string.action_move_down)
    val removeLabel = stringResource(Res.string.playlist_remove_track)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FlixTheme.spacing.rowHeight)
            .then(if (isDragged) Modifier.shadow(6.dp, FlixTheme.shapes.md) else Modifier)
            .background(if (isDragged) FlixTheme.colors.surfaceElevated else FlixTheme.colors.background)
            .semantics(mergeDescendants = true) {
                customActions = buildList {
                    if (index > 0) add(CustomAccessibilityAction(moveUpLabel) { onMove(index, index - 1); true })
                    if (index < lastIndex) add(CustomAccessibilityAction(moveDownLabel) { onMove(index, index + 1); true })
                    add(CustomAccessibilityAction(removeLabel) { onRemove(); true })
                }
            }
            .padding(start = FlixTheme.spacing.xs, end = FlixTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(Res.drawable.ic_do_not_disturb_on_filled),
                contentDescription = stringResource(Res.string.playlist_remove_track_named, track.title),
                tint = FlixTheme.colors.error,
            )
        }
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(FlixTheme.shapes.xs)
                .background(FlixTheme.colors.surfaceVariant),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = FlixTheme.spacing.itemGap),
        ) {
            Text(
                text = track.title,
                style = FlixTheme.typography.subtitle,
                color = FlixTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistNames,
                style = FlixTheme.typography.bodySmall,
                color = FlixTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(FlixTheme.spacing.touchMin)
                .clearAndSetSemantics { contentDescription = "" }
                .reorderHandle(reorder, track.id) { fromKey, toKey ->
                    onMove(tracks.indexOfFirst { it.id == fromKey }, tracks.indexOfFirst { it.id == toKey })
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_drag_indicator),
                contentDescription = null,
                tint = if (isDragged) FlixTheme.colors.textPrimary else FlixTheme.colors.textSecondary,
            )
        }
    }
}

private val NON_TRACK_KEYS = setOf("form", "count", "delete")


private val previewTracks = listOf(
    Track("1", "Midnight City", listOf(ArtistRef(null, "M83")), null, null, null),
    Track("2", "Killshot", listOf(ArtistRef(null, "Eminem")), null, null, null),
    Track("3", "Tití Me Preguntó", listOf(ArtistRef(null, "Bad Bunny")), null, null, null),
)

