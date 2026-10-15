package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.Playlist
import com.flix.music.resources.Res
import com.flix.music.resources.add_to_playlist
import com.flix.music.resources.add_to_playlist_already_added
import com.flix.music.resources.add_to_playlist_search
import com.flix.music.resources.ic_add
import com.flix.music.resources.ic_check
import com.flix.music.resources.ic_check_circle_filled
import com.flix.music.resources.playlist_create_title
import com.flix.music.resources.playlist_empty_caption
import com.flix.music.resources.search_clear
import com.flix.music.resources.songs_count
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * "Añadir a playlist" (design `sheetpl`): "Nueva playlist" first, then every playlist
 * with its count. Playlists that already have [trackId] say so and cannot be picked
 * again. [pickedPlaylistId] shows the Lumen check on the chosen row before the caller
 * closes the sheet. With more than 8 playlists a search field appears on top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    trackId: String,
    playlists: List<Playlist>,
    onPick: (Playlist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
    pickedPlaylistId: Long? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visiblePlaylists = remember(playlists, query) {
        playlists.filter { playlist -> playlist.name.contains(query.trim(), ignoreCase = true) }
    }

    FlixBottomSheet(onDismiss = onDismiss, sheetState = sheetState) {
        SheetTitle(stringResource(Res.string.add_to_playlist))
        if (playlists.size > SEARCH_THRESHOLD) {
            SearchField(
                query = query,
                onQueryChange = { newQuery -> query = newQuery },
                onClear = { query = "" },
                placeholder = stringResource(Res.string.add_to_playlist_search),
                clearLabel = stringResource(Res.string.search_clear),
                modifier = Modifier
                    .padding(horizontal = FlixTheme.spacing.lg)
                    .padding(bottom = FlixTheme.spacing.sm),
            )
        }
        LazyColumn {
            item(key = "new") {
                SheetRowLayout(onClick = onCreateNew) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(FlixTheme.shapes.sm)
                            .background(FlixTheme.colors.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = null,
                            tint = FlixTheme.colors.textPrimary,
                        )
                    }
                    Text(stringResource(Res.string.playlist_create_title), style = FlixTheme.typography.label)
                }
            }
            items(visiblePlaylists, key = { playlist -> playlist.id }) { playlist ->
                PlaylistPickRow(
                    playlist = playlist,
                    alreadyAdded = playlist.contains(trackId) && playlist.id != pickedPlaylistId,
                    picked = playlist.id == pickedPlaylistId,
                    onClick = { onPick(playlist) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistPickRow(
    playlist: Playlist,
    alreadyAdded: Boolean,
    picked: Boolean,
    onClick: () -> Unit,
) {
    val caption = when {
        alreadyAdded -> stringResource(Res.string.add_to_playlist_already_added)
        playlist.tracks.isEmpty() -> stringResource(Res.string.playlist_empty_caption)
        else -> pluralStringResource(Res.plurals.songs_count, playlist.tracks.size, playlist.tracks.size)
    }
    SheetRowLayout(
        onClick = { if (!alreadyAdded && !picked) onClick() },
        height = PLAYLIST_ROW_HEIGHT,
        modifier = Modifier.semantics { if (alreadyAdded) disabled() },
    ) {
        PlaylistArtwork(coverUrls = playlist.coverUrls, size = 40.dp, cornerRadius = 6.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = FlixTheme.typography.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(caption, style = FlixTheme.typography.caption, color = FlixTheme.colors.textSecondary)
        }
        when {
            picked -> Icon(
                painter = painterResource(Res.drawable.ic_check_circle_filled),
                contentDescription = null,
                tint = FlixTheme.colors.primaryContent,
            )
            alreadyAdded -> Icon(
                painter = painterResource(Res.drawable.ic_check),
                contentDescription = null,
                tint = FlixTheme.colors.textTertiary,
            )
        }
    }
}

private const val SEARCH_THRESHOLD = 8
private val PLAYLIST_ROW_HEIGHT = 56.dp
