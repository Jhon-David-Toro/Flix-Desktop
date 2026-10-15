package com.flix.music.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.CreatePlaylistDialog
import com.flix.music.core.designsystem.component.FavoritesArtwork
import com.flix.music.core.designsystem.component.FlixCard
import com.flix.music.core.designsystem.component.FlixFilterChip
import com.flix.music.core.designsystem.component.FlixSnackbarVisuals
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.PlaylistArtwork
import com.flix.music.core.designsystem.component.SearchField
import com.flix.music.core.designsystem.component.SortOrderChip
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.handCursor
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.designsystem.theme.ThemeMode
import com.flix.music.core.model.Album
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.Artist
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FavoriteTrack
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.SortOrder
import com.flix.music.core.model.Track
import com.flix.music.core.ui.LocalDetailNavigator
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.action_browse_music
import com.flix.music.resources.favorites_empty_body
import com.flix.music.resources.favorites_empty_title
import com.flix.music.resources.favorites_title
import com.flix.music.resources.history_title
import com.flix.music.resources.ic_add
import com.flix.music.resources.ic_album
import com.flix.music.resources.ic_check_circle_filled
import com.flix.music.resources.ic_close
import com.flix.music.resources.ic_favorite
import com.flix.music.resources.ic_grid_view
import com.flix.music.resources.ic_history
import com.flix.music.resources.ic_person
import com.flix.music.resources.ic_queue_music
import com.flix.music.resources.ic_search
import com.flix.music.resources.ic_search_off
import com.flix.music.resources.ic_view_list
import com.flix.music.resources.library_album_caption
import com.flix.music.resources.library_filter_albums
import com.flix.music.resources.library_filter_artists
import com.flix.music.resources.library_filter_playlists
import com.flix.music.resources.library_filter_songs
import com.flix.music.resources.library_history_empty
import com.flix.music.resources.library_history_today
import com.flix.music.resources.library_no_albums_body
import com.flix.music.resources.library_no_albums_title
import com.flix.music.resources.library_no_artists_body
import com.flix.music.resources.library_no_artists_title
import com.flix.music.resources.library_no_match_body
import com.flix.music.resources.library_no_match_title
import com.flix.music.resources.library_no_playlists_body
import com.flix.music.resources.library_no_playlists_title
import com.flix.music.resources.library_playlist_count
import com.flix.music.resources.library_playlist_empty
import com.flix.music.resources.library_search
import com.flix.music.resources.library_search_close
import com.flix.music.resources.library_search_placeholder
import com.flix.music.resources.library_show_grid
import com.flix.music.resources.library_show_list
import com.flix.music.resources.playlist_create_action
import com.flix.music.resources.playlist_create_title
import com.flix.music.resources.playlist_created
import com.flix.music.resources.search_clear
import com.flix.music.resources.songs_count
import com.flix.music.resources.tab_library
import com.flix.music.resources.type_artist
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.time.Instant

/** Every user action on the Library tab. Defaults do nothing, which keeps previews short. */
data class LibraryActions(
    val onOpenFavorites: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onBrowseMusic: () -> Unit = {},
    val onFilterChange: (LibraryFilter) -> Unit = {},
    val onSortOrderChange: (SortOrder) -> Unit = {},
    val onSongClick: (Track) -> Unit = {},
    val onSongMore: (Track) -> Unit = {},
    val onOpenPlaylist: (Long) -> Unit = {},
    val onCreatePlaylist: (name: String, description: String) -> Unit = { _, _ -> },
    val onQueryChange: (String) -> Unit = {},
    val onToggleLayout: () -> Unit = {},
    val onOpenAlbum: (Album) -> Unit = {},
    val onOpenArtist: (Artist) -> Unit = {},
)

@Composable
fun LibraryScreen(
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlaylist: (playlistId: Long) -> Unit,
    onBrowseMusic: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val trackMenu = LocalTrackMenu.current
    val detailNavigator = LocalDetailNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val createdMessage = stringResource(Res.string.playlist_created)
    val actions = remember(viewModel, onOpenFavorites, onOpenHistory, onOpenPlaylist, onBrowseMusic, trackMenu, detailNavigator) {
        LibraryActions(
            onOpenFavorites = onOpenFavorites,
            onOpenHistory = onOpenHistory,
            onBrowseMusic = onBrowseMusic,
            onFilterChange = viewModel::onFilterChange,
            onSortOrderChange = viewModel::onSortOrderChange,
            onSongClick = viewModel::onSongClick,
            onSongMore = { track -> trackMenu.open(track) },
            onOpenPlaylist = onOpenPlaylist,
            onCreatePlaylist = { name, description ->
                viewModel.onCreatePlaylist(name, description)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        FlixSnackbarVisuals(createdMessage, Res.drawable.ic_check_circle_filled, success = true),
                    )
                }
            },
            onQueryChange = viewModel::onQueryChange,
            onToggleLayout = viewModel::onToggleLayout,
            onOpenAlbum = { album ->
                detailNavigator.openAlbum(AlbumRef(album.id, album.name), ArtistRef(null, album.artistName))
            },
            onOpenArtist = { artist -> detailNavigator.openArtist(ArtistRef(artist.id, artist.name)) },
        )
    }
    LibraryContent(
        uiState = uiState,
        query = viewModel.query,
        actions = actions,
        nowPlaying = nowPlaying,
        modifier = modifier,
    )
}

@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    query: String = uiState.query,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    var creatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    val closeSearch = {
        searching = false
        actions.onQueryChange("")
    }
    if (creatingPlaylist) {
        CreatePlaylistDialog(
            onCreate = { name, description ->
                creatingPlaylist = false
                actions.onCreatePlaylist(name, description)
            },
            onDismiss = { creatingPlaylist = false },
        )
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl),
    ) {
        item(key = "title") {
            Row(
                modifier = Modifier.padding(start = FlixTheme.spacing.gutter, end = FlixTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.tab_library),
                    style = FlixTheme.typography.headline,
                    color = FlixTheme.colors.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = FlixTheme.spacing.md)
                        .semantics { heading() },
                )
                IconButton(onClick = { if (searching) closeSearch() else searching = true }) {
                    Icon(
                        painter = painterResource(if (searching) Res.drawable.ic_close else Res.drawable.ic_search),
                        contentDescription = stringResource(
                            if (searching) Res.string.library_search_close else Res.string.library_search,
                        ),
                        tint = FlixTheme.colors.textPrimary,
                    )
                }
                IconButton(onClick = { creatingPlaylist = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = stringResource(Res.string.playlist_create_title),
                        tint = FlixTheme.colors.textPrimary,
                    )
                }
            }
        }
        if (searching) {
            item(key = "search") {
                SearchField(
                    query = query,
                    onQueryChange = actions.onQueryChange,
                    onClear = { actions.onQueryChange("") },
                    placeholder = stringResource(Res.string.library_search_placeholder),
                    clearLabel = stringResource(Res.string.search_clear),
                    modifier = Modifier
                        .padding(horizontal = FlixTheme.spacing.gutter)
                        .padding(bottom = FlixTheme.spacing.md),
                )
            }
        }
        item(key = "pinned") { PinnedCollections(uiState, actions) }
        item(key = "filters") {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = FlixTheme.spacing.gutter)
                    .padding(top = FlixTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
            ) {
                LibraryFilter.entries.forEach { libraryFilter ->
                    FlixFilterChip(
                        label = stringResource(libraryFilter.label()),
                        selected = libraryFilter == uiState.filter,
                        onClick = { actions.onFilterChange(libraryFilter) },
                    )
                }
            }
        }
        if (uiState.isFilterEmpty) {
            item(key = "empty-${uiState.filter}") {
                FilterEmptyState(
                    filter = uiState.filter,
                    onCreatePlaylist = { creatingPlaylist = true },
                    onBrowseMusic = actions.onBrowseMusic,
                )
            }
            return@LazyColumn
        }
        item(key = "controls") { LibraryControls(uiState, actions) }
        when (uiState.filter) {
            LibraryFilter.Playlists -> collectionItems(
                items = uiState.playlists,
                isGrid = uiState.isGrid,
                key = { playlist -> "playlist-${playlist.id}" },
                row = { playlist, rowModifier ->
                    PlaylistRow(playlist, onClick = { actions.onOpenPlaylist(playlist.id) }, modifier = rowModifier)
                },
                cell = { playlist, cellModifier ->
                    GridCell(
                        title = playlist.name,
                        caption = playlistCaption(playlist),
                        onClick = { actions.onOpenPlaylist(playlist.id) },
                        modifier = cellModifier,
                    ) { artworkModifier ->
                        BoxWithConstraints(artworkModifier) {
                            PlaylistArtwork(
                                coverUrls = playlist.coverUrls,
                                size = maxWidth,
                                cornerRadius = GRID_RADIUS,
                                stacked = false,
                            )
                        }
                    }
                },
            )
            LibraryFilter.Songs -> items(uiState.songs, key = { it.track.id }) { favorite ->
                TrackRow(
                    title = favorite.track.title,
                    subtitle = favorite.track.artistNames,
                    artworkUrl = favorite.track.artworkUrl,
                    onClick = { actions.onSongClick(favorite.track) },
                    onMoreClick = { actions.onSongMore(favorite.track) },
                    isCurrent = favorite.track.id == nowPlaying.trackId,
                    isPlaying = nowPlaying.isPlaying,
                    modifier = Modifier.animateItem(),
                )
            }
            LibraryFilter.Albums -> collectionItems(
                items = uiState.albums,
                isGrid = uiState.isGrid,
                key = { album -> "album-${album.id}" },
                row = { album, rowModifier ->
                    CollectionRow(
                        title = album.name,
                        caption = stringResource(Res.string.library_album_caption, album.artistName),
                        artworkUrl = album.artworkUrl,
                        circular = false,
                        onClick = { actions.onOpenAlbum(album) },
                        modifier = rowModifier,
                    )
                },
                cell = { album, cellModifier ->
                    GridCell(
                        title = album.name,
                        caption = album.artistName,
                        onClick = { actions.onOpenAlbum(album) },
                        modifier = cellModifier,
                    ) { artworkModifier -> GridArtwork(album.artworkUrl, circular = false, modifier = artworkModifier) }
                },
            )
            LibraryFilter.Artists -> collectionItems(
                items = uiState.artists,
                isGrid = uiState.isGrid,
                key = { artist -> "artist-${artist.id}" },
                row = { artist, rowModifier ->
                    CollectionRow(
                        title = artist.name,
                        caption = stringResource(Res.string.type_artist),
                        artworkUrl = artist.artworkUrl,
                        circular = true,
                        onClick = { actions.onOpenArtist(artist) },
                        modifier = rowModifier,
                    )
                },
                cell = { artist, cellModifier ->
                    GridCell(
                        title = artist.name,
                        caption = stringResource(Res.string.type_artist),
                        onClick = { actions.onOpenArtist(artist) },
                        modifier = cellModifier,
                        centered = true,
                    ) { artworkModifier -> GridArtwork(artist.artworkUrl, circular = true, modifier = artworkModifier) }
                },
            )
        }
        if (uiState.query.isNotBlank() && uiState.isSelectionEmpty()) {
            item(key = "no-match") {
                StateView(
                    icon = Res.drawable.ic_search_off,
                    title = stringResource(Res.string.library_no_match_title, uiState.query.trim()),
                    body = stringResource(Res.string.library_no_match_body),
                )
            }
        }
    }
}

private fun LibraryUiState.isSelectionEmpty(): Boolean = when (filter) {
    LibraryFilter.Playlists -> playlists.isEmpty()
    LibraryFilter.Songs -> songs.isEmpty()
    LibraryFilter.Albums -> albums.isEmpty()
    LibraryFilter.Artists -> artists.isEmpty()
}

/**
 * Rows, or a 2-column grid (design `librarygrid`: gap 16dp vertical, 12dp horizontal).
 * The grid is built from rows of two so it lives inside the same scrolling list.
 */
private fun <Item> LazyListScope.collectionItems(
    items: List<Item>,
    isGrid: Boolean,
    key: (Item) -> String,
    row: @Composable (Item, Modifier) -> Unit,
    cell: @Composable (Item, Modifier) -> Unit,
) {
    if (!isGrid) {
        items(items, key = key) { item -> row(item, Modifier.animateItem()) }
        return
    }
    items(items.chunked(GRID_COLUMNS), key = { pair -> "grid-" + key(pair.first()) }) { pair ->
        Row(
            modifier = Modifier
                .padding(horizontal = FlixTheme.spacing.gutter)
                .padding(bottom = FlixTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
        ) {
            pair.forEach { item -> cell(item, Modifier.weight(1f)) }
            repeat(GRID_COLUMNS - pair.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** "⇅ Recientes" on the left and the list/grid toggle on the right. */
@Composable
private fun LibraryControls(uiState: LibraryUiState, actions: LibraryActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = FlixTheme.spacing.gutter, end = FlixTheme.spacing.xs)
            .padding(top = FlixTheme.spacing.md, bottom = FlixTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortOrderChip(order = uiState.sortOrder, onOrderChange = actions.onSortOrderChange)
        Spacer(Modifier.weight(1f))
        if (uiState.filter.supportsGrid) {
            IconButton(onClick = actions.onToggleLayout) {
                Icon(
                    painter = painterResource(if (uiState.isGrid) Res.drawable.ic_view_list else Res.drawable.ic_grid_view),
                    contentDescription = stringResource(
                        if (uiState.isGrid) Res.string.library_show_list else Res.string.library_show_grid,
                    ),
                    tint = if (uiState.isGrid) FlixTheme.colors.textPrimary else FlixTheme.colors.textSecondary,
                )
            }
        }
    }
}

/** Empty state for each filter (design "Empty states" and "vacío por filtro"). */
@Composable
private fun FilterEmptyState(filter: LibraryFilter, onCreatePlaylist: () -> Unit, onBrowseMusic: () -> Unit) {
    when (filter) {
        LibraryFilter.Playlists -> StateView(
            icon = Res.drawable.ic_queue_music,
            title = stringResource(Res.string.library_no_playlists_title),
            body = stringResource(Res.string.library_no_playlists_body),
            actionLabel = stringResource(Res.string.playlist_create_action),
            onAction = onCreatePlaylist,
        )
        LibraryFilter.Songs -> StateView(
            icon = Res.drawable.ic_favorite,
            title = stringResource(Res.string.favorites_empty_title),
            body = stringResource(Res.string.favorites_empty_body),
            actionLabel = stringResource(Res.string.action_browse_music),
            onAction = onBrowseMusic,
        )
        LibraryFilter.Albums -> StateView(
            icon = Res.drawable.ic_album,
            title = stringResource(Res.string.library_no_albums_title),
            body = stringResource(Res.string.library_no_albums_body),
            actionLabel = stringResource(Res.string.action_browse_music),
            onAction = onBrowseMusic,
        )
        LibraryFilter.Artists -> StateView(
            icon = Res.drawable.ic_person,
            title = stringResource(Res.string.library_no_artists_title),
            body = stringResource(Res.string.library_no_artists_body),
            actionLabel = stringResource(Res.string.action_browse_music),
            onAction = onBrowseMusic,
        )
    }
}

/** `.row` for albums and artists: 48dp cover (circle for artists), name and caption. */
@Composable
private fun CollectionRow(
    title: String,
    caption: String,
    artworkUrl: String?,
    circular: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FlixTheme.spacing.rowHeight)
            .handCursor().clickable(onClick = onClick)
            .padding(horizontal = FlixTheme.spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(if (circular) FlixTheme.shapes.circle else FlixTheme.shapes.sm)
                .background(FlixTheme.colors.surfaceVariant),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = FlixTheme.typography.subtitle,
                color = FlixTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(caption, style = FlixTheme.typography.bodySmall, color = FlixTheme.colors.textSecondary)
        }
    }
}

/** Grid cell: square cover (`.afull`), then title (12sp semibold) and caption. */
@Composable
private fun GridCell(
    title: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    artwork: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = modifier
            .clip(FlixTheme.shapes.md)
            .handCursor().clickable(onClick = onClick),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        artwork(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Text(
            text = title,
            style = FlixTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = FlixTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = caption,
            style = FlixTheme.typography.caption,
            color = FlixTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GridArtwork(artworkUrl: String?, circular: Boolean, modifier: Modifier) {
    AsyncImage(
        model = artworkUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(if (circular) FlixTheme.shapes.circle else RoundedCornerShape(GRID_RADIUS))
            .background(FlixTheme.colors.surfaceVariant),
    )
}

@Composable
private fun playlistCaption(playlist: Playlist): String =
    if (playlist.tracks.isEmpty()) {
        stringResource(Res.string.library_playlist_empty)
    } else {
        pluralStringResource(Res.plurals.library_playlist_count, playlist.tracks.size, playlist.tracks.size)
    }

/** The two pinned collections (design `library`): Favoritos and Historial as `.card` tiles. */
@Composable
private fun PinnedCollections(uiState: LibraryUiState, actions: LibraryActions) {
    Row(
        modifier = Modifier.padding(horizontal = FlixTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
    ) {
        PinnedTile(
            title = stringResource(Res.string.favorites_title),
            caption = pluralStringResource(Res.plurals.songs_count, uiState.favoriteCount, uiState.favoriteCount),
            onClick = actions.onOpenFavorites,
            modifier = Modifier.weight(1f),
            artwork = {
                FavoritesArtwork(size = TILE_ARTWORK_SIZE, iconSize = TILE_ICON_SIZE, shape = FlixTheme.shapes.sm)
            },
        )
        PinnedTile(
            title = stringResource(Res.string.history_title),
            caption = if (uiState.hasHistory) {
                stringResource(Res.string.library_history_today, uiState.playedTodayCount)
            } else {
                stringResource(Res.string.library_history_empty)
            },
            onClick = actions.onOpenHistory,
            modifier = Modifier.weight(1f),
            artwork = {
                Box(
                    modifier = Modifier
                        .size(TILE_ARTWORK_SIZE)
                        .clip(FlixTheme.shapes.sm)
                        .background(FlixTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_history),
                        contentDescription = null,
                        tint = FlixTheme.colors.textPrimary,
                        modifier = Modifier.size(TILE_ICON_SIZE),
                    )
                }
            },
        )
    }
}

/** Playlist row (design `library`): stacked cover, name and "Playlist · N canciones". */
@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val caption = playlistCaption(playlist)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FlixTheme.spacing.rowHeight)
            .handCursor().clickable(onClick = onClick)
            .padding(horizontal = FlixTheme.spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        PlaylistArtwork(coverUrls = playlist.coverUrls, size = 48.dp, cornerRadius = 8.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = FlixTheme.typography.subtitle,
                color = FlixTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(caption, style = FlixTheme.typography.bodySmall, color = FlixTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun PinnedTile(
    title: String,
    caption: String,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlixCard(modifier = modifier, shape = FlixTheme.shapes.md, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlixTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
        ) {
            artwork()
            Column {
                Text(title, style = FlixTheme.typography.label, color = FlixTheme.colors.textPrimary)
                Text(caption, style = FlixTheme.typography.caption, color = FlixTheme.colors.textSecondary)
            }
        }
    }
}

private fun LibraryFilter.label(): StringResource = when (this) {
    LibraryFilter.Playlists -> Res.string.library_filter_playlists
    LibraryFilter.Songs -> Res.string.library_filter_songs
    LibraryFilter.Albums -> Res.string.library_filter_albums
    LibraryFilter.Artists -> Res.string.library_filter_artists
}

private const val GRID_COLUMNS = 2
private val GRID_RADIUS = 16.dp

private val TILE_ARTWORK_SIZE = 48.dp
private val TILE_ICON_SIZE = 24.dp

private val previewSongs = listOf(
    Track("1", "Fade To Black", listOf(ArtistRef(null, "Metallica")), null, null, null),
    Track("2", "Killshot", listOf(ArtistRef(null, "Eminem")), null, null, null),
).map { track -> FavoriteTrack(track, Instant.EPOCH) }

private val previewPlaylists = listOf(
    Playlist(1, "Noches de ruta", null, previewSongs.map { it.track }, Instant.EPOCH),
    Playlist(2, "Para correr", null, emptyList(), Instant.EPOCH),
)


