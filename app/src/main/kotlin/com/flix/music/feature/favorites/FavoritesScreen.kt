package com.flix.music.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.designsystem.component.FavoriteButton
import com.flix.music.core.designsystem.component.FavoritesArtwork
import com.flix.music.core.designsystem.component.FilledTonalIconButton
import com.flix.music.core.designsystem.component.FlixTopAppBar
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.PlayPauseButton
import com.flix.music.core.designsystem.component.SearchField
import com.flix.music.core.designsystem.component.SearchFieldCompactHeight
import com.flix.music.core.designsystem.component.SortOrderChip
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.showUndo
import com.flix.music.core.designsystem.component.util.toClock
import com.flix.music.core.designsystem.component.util.toReadableLength
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FavoriteTrack
import com.flix.music.core.model.SortOrder
import com.flix.music.core.model.Track
import com.flix.music.core.model.totalDuration
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.action_browse_music
import com.flix.music.resources.action_undo
import com.flix.music.resources.favorite_removed
import com.flix.music.resources.favorites_empty_body
import com.flix.music.resources.favorites_empty_title
import com.flix.music.resources.favorites_no_match_body
import com.flix.music.resources.favorites_no_match_title
import com.flix.music.resources.favorites_play_all
import com.flix.music.resources.favorites_search
import com.flix.music.resources.favorites_summary
import com.flix.music.resources.favorites_title
import com.flix.music.resources.ic_favorite
import com.flix.music.resources.ic_play_arrow_filled
import com.flix.music.resources.ic_search_off
import com.flix.music.resources.ic_shuffle
import com.flix.music.resources.player_shuffle
import com.flix.music.resources.search_clear
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/** Every user action on the Favorites screen. Defaults do nothing, which keeps previews short. */
data class FavoritesActions(
    val onBack: () -> Unit = {},
    val onBrowseMusic: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onSortOrderChange: (SortOrder) -> Unit = {},
    val onPlayAll: () -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onTrackClick: (Track) -> Unit = {},
    val onRemove: (FavoriteTrack) -> Unit = {},
    val onUndoRemove: (FavoriteTrack) -> Unit = {},
)

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onBrowseMusic: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortOrder by viewModel.currentSortOrder.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack, onBrowseMusic) {
        FavoritesActions(
            onBack = onBack,
            onBrowseMusic = onBrowseMusic,
            onQueryChange = viewModel::onQueryChange,
            onSortOrderChange = viewModel::onSortOrderChange,
            onPlayAll = viewModel::onPlayAll,
            onShuffle = viewModel::onShuffle,
            onTrackClick = viewModel::onTrackClick,
            onRemove = viewModel::onRemove,
            onUndoRemove = viewModel::onUndoRemove,
        )
    }
    FavoritesContent(
        uiState = uiState,
        query = viewModel.query,
        sortOrder = sortOrder,
        nowPlaying = nowPlaying,
        actions = actions,
        modifier = modifier,
    )
}

@Composable
fun FavoritesContent(
    uiState: FavoritesUiState,
    query: String,
    sortOrder: SortOrder,
    actions: FavoritesActions,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val removedMessage = stringResource(Res.string.favorite_removed)
    val undoLabel = stringResource(Res.string.action_undo)

    Column(modifier.fillMaxSize()) {
        FlixTopAppBar(title = stringResource(Res.string.favorites_title), onBack = actions.onBack)
        val content = uiState as? FavoritesUiState.Content ?: return@Column
        if (content.all.isEmpty()) {
            StateView(
                icon = Res.drawable.ic_favorite,
                title = stringResource(Res.string.favorites_empty_title),
                body = stringResource(Res.string.favorites_empty_body),
                actionLabel = stringResource(Res.string.action_browse_music),
                onAction = actions.onBrowseMusic,
            )
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl)) {
            item(key = "header") { FavoritesHeader(content, actions) }
            item(key = "search") {
                Row(
                    modifier = Modifier
                        .padding(horizontal = FlixTheme.spacing.gutter)
                        .padding(top = FlixTheme.spacing.md, bottom = FlixTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
                ) {
                    SearchField(
                        query = query,
                        onQueryChange = actions.onQueryChange,
                        onClear = { actions.onQueryChange("") },
                        placeholder = stringResource(Res.string.favorites_search),
                        clearLabel = stringResource(Res.string.search_clear),
                        modifier = Modifier.weight(1f),
                        height = SearchFieldCompactHeight,
                    )
                    SortOrderChip(order = sortOrder, onOrderChange = actions.onSortOrderChange)
                }
            }
            if (content.visible.isEmpty()) {
                item(key = "no-match") {
                    StateView(
                        icon = Res.drawable.ic_search_off,
                        title = stringResource(Res.string.favorites_no_match_title, query.trim()),
                        body = stringResource(Res.string.favorites_no_match_body),
                    )
                }
            }
            items(content.visible, key = { it.track.id }) { favorite ->
                val trackMenu = LocalTrackMenu.current
                TrackRow(
                    title = favorite.track.title,
                    subtitle = favorite.track.artistNames,
                    artworkUrl = favorite.track.artworkUrl,
                    onClick = { actions.onTrackClick(favorite.track) },
                    onLongClick = { trackMenu.open(favorite.track) },
                    isCurrent = favorite.track.id == nowPlaying.trackId,
                    isPlaying = nowPlaying.isPlaying,
                    duration = favorite.track.duration?.toClock(),
                    modifier = Modifier.animateItem(),
                    trailing = {
                        FavoriteButton(
                            isFavorite = true,
                            onToggle = {
                                actions.onRemove(favorite)
                                coroutineScope.launch {
                                    snackbarHostState.showUndo(removedMessage, undoLabel) {
                                        actions.onUndoRemove(favorite)
                                    }
                                }
                            },
                            trackTitle = favorite.track.title,
                        )
                    },
                )
            }
        }
    }
}

/** Collection header (design `favorites`): artwork, count · total length, shuffle and play. */
@Composable
private fun FavoritesHeader(content: FavoritesUiState.Content, actions: FavoritesActions) {
    Row(
        modifier = Modifier
            .padding(horizontal = FlixTheme.spacing.gutter)
            .padding(top = FlixTheme.spacing.xs),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.lg),
    ) {
        FavoritesArtwork(
            size = HEADER_ARTWORK_SIZE,
            iconSize = HEADER_ICON_SIZE,
            shape = FlixTheme.shapes.md,
        )
        Column {
            Text(
                text = pluralStringResource(
                    Res.plurals.favorites_summary,
                    content.all.size,
                    content.all.size,
                    content.totalDuration.toReadableLength(),
                ),
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textSecondary,
            )
            Row(
                modifier = Modifier.padding(top = FlixTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = actions.onShuffle,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = FlixTheme.colors.surfaceVariant,
                        contentColor = FlixTheme.colors.textPrimary,
                    ),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_shuffle),
                        contentDescription = stringResource(Res.string.player_shuffle),
                    )
                }
                PlayPauseButton(
                    icon = Res.drawable.ic_play_arrow_filled,
                    contentDescription = stringResource(Res.string.favorites_play_all),
                    onClick = actions.onPlayAll,
                    size = DETAIL_PLAY_BUTTON_SIZE,
                    iconSize = DETAIL_PLAY_ICON_SIZE,
                )
            }
        }
    }
}

private val HEADER_ARTWORK_SIZE = 120.dp
private val HEADER_ICON_SIZE = 52.dp
private val DETAIL_PLAY_BUTTON_SIZE = 52.dp
private val DETAIL_PLAY_ICON_SIZE = 28.dp

private val previewFavorites = listOf(
    Track("1", "Fade To Black", listOf(ArtistRef(null, "Metallica")), null, 417.seconds, null),
    Track("2", "Killshot", listOf(ArtistRef(null, "Eminem")), null, 253.seconds, null),
    Track("3", "Midnight City", listOf(ArtistRef(null, "M83")), null, 243.seconds, null),
).map { track -> FavoriteTrack(track, Instant.EPOCH) }


