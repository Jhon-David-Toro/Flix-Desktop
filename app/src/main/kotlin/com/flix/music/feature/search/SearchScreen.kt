package com.flix.music.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.designsystem.component.AlbumCarousel
import com.flix.music.core.designsystem.component.ArtistCarousel
import com.flix.music.core.designsystem.component.BestResultCard
import com.flix.music.core.designsystem.component.BestResultCardSkeleton
import com.flix.music.core.designsystem.component.ErrorStateView
import com.flix.music.core.designsystem.component.FlixChip
import com.flix.music.core.designsystem.component.FlixFilterChip
import com.flix.music.core.designsystem.component.GENRE_TILE_COMPACT_HEIGHT
import com.flix.music.core.designsystem.component.GenreTile
import com.flix.music.core.designsystem.component.LoadingHairline
import com.flix.music.core.designsystem.component.MediaCard
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.SearchField
import com.flix.music.core.designsystem.component.SectionHeader
import com.flix.music.core.designsystem.component.SkeletonBox
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.SuggestionRow
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.TrackRowSkeleton
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.designsystem.theme.ThemeMode
import com.flix.music.core.model.Album
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.Artist
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.Genre
import com.flix.music.core.model.Genres
import com.flix.music.core.model.Track
import com.flix.music.core.model.distinctAlbums
import com.flix.music.core.model.distinctArtists
import com.flix.music.core.ui.LocalDetailNavigator
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.ic_close
import com.flix.music.resources.ic_history
import com.flix.music.resources.ic_north_west
import com.flix.music.resources.ic_search
import com.flix.music.resources.ic_search_off
import com.flix.music.resources.loading_results
import com.flix.music.resources.play_item
import com.flix.music.resources.search_best_result
import com.flix.music.resources.search_clear
import com.flix.music.resources.search_clear_recents
import com.flix.music.resources.search_did_you_mean
import com.flix.music.resources.search_empty_body
import com.flix.music.resources.search_empty_title
import com.flix.music.resources.search_error_upstream_body
import com.flix.music.resources.search_error_upstream_title
import com.flix.music.resources.search_genres
import com.flix.music.resources.search_moods
import com.flix.music.resources.search_now
import com.flix.music.resources.search_placeholder
import com.flix.music.resources.search_recent
import com.flix.music.resources.search_remove_recent
import com.flix.music.resources.search_section_albums
import com.flix.music.resources.search_section_artists
import com.flix.music.resources.search_section_songs
import com.flix.music.resources.search_use_suggestion
import com.flix.music.resources.see_all
import com.flix.music.resources.tab_search
import com.flix.music.resources.type_artist
import com.flix.music.resources.type_song
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** Every user action on the Search screen. Defaults do nothing, which keeps previews short. */
data class SearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onClear: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onSearchNow: (String) -> Unit = {},
    val onFilterChange: (SearchFilter) -> Unit = {},
    val onRemoveRecent: (String) -> Unit = {},
    val onClearRecents: () -> Unit = {},
    val onGenreClick: (Genre) -> Unit = {},
    val onTrackClick: (Track) -> Unit = {},
    val onTrackOpen: (Track) -> Unit = {},
    val onArtistClick: (Artist) -> Unit = {},
    val onAlbumClick: (Album) -> Unit = {},
)

@Composable
fun SearchScreen(
    onGenreClick: (Genre) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val detailNavigator = LocalDetailNavigator.current
    val actions = remember(viewModel, onGenreClick, detailNavigator) {
        SearchActions(
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::onClear,
            onRetry = viewModel::onRetry,
            onSearchNow = viewModel::onSearchNow,
            onFilterChange = viewModel::onFilterChange,
            onRemoveRecent = viewModel::onRemoveRecent,
            onClearRecents = viewModel::onClearRecents,
            onGenreClick = onGenreClick,
            onTrackClick = viewModel::onTrackClick,
            onTrackOpen = { track -> detailNavigator.openSong(track.id) },
            onArtistClick = { artist -> detailNavigator.openArtist(ArtistRef(artist.id, artist.name)) },
            onAlbumClick = { album ->
                detailNavigator.openAlbum(AlbumRef(album.id, album.name), ArtistRef(null, album.artistName))
            },
        )
    }

    SearchContent(
        query = viewModel.query,
        uiState = uiState,
        filter = viewModel.filter,
        recentSearches = recentSearches,
        nowPlaying = nowPlaying,
        actions = actions,
        modifier = modifier,
    )
}

@Composable
fun SearchContent(
    query: String,
    uiState: SearchUiState,
    filter: SearchFilter,
    recentSearches: List<String>,
    actions: SearchActions,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    Column(modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.tab_search),
            style = FlixTheme.typography.headline,
            color = FlixTheme.colors.textPrimary,
            modifier = Modifier.padding(
                horizontal = FlixTheme.spacing.gutter,
                vertical = FlixTheme.spacing.md,
            ),
        )
        SearchField(
            query = query,
            onQueryChange = actions.onQueryChange,
            onClear = actions.onClear,
            placeholder = stringResource(Res.string.search_placeholder),
            clearLabel = stringResource(Res.string.search_clear),
            modifier = Modifier.padding(horizontal = FlixTheme.spacing.gutter),
        )
        LoadingHairline(
            visible = uiState is SearchUiState.Loading,
            modifier = Modifier
                .padding(horizontal = FlixTheme.spacing.gutter)
                .offset(y = (-2).dp),
        )

        when (uiState) {
            SearchUiState.Idle -> SearchIdle(recentSearches, actions)
            is SearchUiState.Typing -> SearchTyping(query, uiState.suggestions, actions)
            is SearchUiState.Loading -> SearchLoading(uiState.previous, filter, nowPlaying, actions)
            is SearchUiState.Empty -> SearchEmpty(uiState, actions)
            is SearchUiState.Error -> SearchError(uiState.error, recentSearches, actions)
            is SearchUiState.Results -> SearchResults(uiState, filter, nowPlaying, actions)
        }
    }
}

@Composable
private fun SearchIdle(recentSearches: List<String>, actions: SearchActions) {
    LazyColumn(contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl)) {
        if (recentSearches.isNotEmpty()) {
            item(key = "recents-header") {
                SectionHeader(
                    title = stringResource(Res.string.search_recent),
                    actionLabel = stringResource(Res.string.search_clear_recents),
                    onAction = actions.onClearRecents,
                    modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap),
                )
            }
            items(recentSearches, key = { "recent-$it" }) { recentSearch ->
                SuggestionRow(
                    text = recentSearch,
                    leadingIcon = Res.drawable.ic_history,
                    onClick = { actions.onSearchNow(recentSearch) },
                    trailing = {
                        IconButton(onClick = { actions.onRemoveRecent(recentSearch) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_close),
                                contentDescription = stringResource(Res.string.search_remove_recent, recentSearch),
                                tint = FlixTheme.colors.textSecondary,
                                modifier = Modifier.size(TRAILING_ICON_SIZE),
                            )
                        }
                    },
                )
            }
        }

        item(key = "moods-header") {
            SectionHeader(
                title = stringResource(Res.string.search_moods),
                modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap, bottom = FlixTheme.spacing.md),
            )
        }
        item(key = "moods") {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = FlixTheme.spacing.gutter),
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
            ) {
                SearchMood.entries.forEach { mood ->
                    FlixChip(
                        label = stringResource(mood.label),
                        onClick = { actions.onSearchNow(mood.query) },
                        leadingIcon = mood.icon,
                    )
                }
            }
        }

        item(key = "genres-header") {
            SectionHeader(
                title = stringResource(Res.string.search_genres),
                modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap, bottom = FlixTheme.spacing.md),
            )
        }
        items(Genres.all.chunked(2), key = { pair -> "genres-${pair.first().id}" }) { pair ->
            Row(
                modifier = Modifier.padding(
                    start = FlixTheme.spacing.gutter,
                    end = FlixTheme.spacing.gutter,
                    bottom = FlixTheme.spacing.md,
                ),
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
            ) {
                pair.forEach { genre ->
                    GenreTile(
                        name = genre.name,
                        gradientStart = Color(genre.gradientStart),
                        gradientEnd = Color(genre.gradientEnd),
                        onClick = { actions.onGenreClick(genre) },
                        height = GENRE_TILE_COMPACT_HEIGHT,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTyping(query: String, suggestions: List<String>, actions: SearchActions) {
    Column(Modifier.padding(top = FlixTheme.spacing.sm)) {
        suggestions.forEach { suggestion ->
            SuggestionRow(
                text = suggestion,
                leadingIcon = Res.drawable.ic_history,
                onClick = { actions.onSearchNow(suggestion) },
                trailing = {
                    IconButton(onClick = { actions.onQueryChange(suggestion) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_north_west),
                            contentDescription = stringResource(Res.string.search_use_suggestion, suggestion),
                            tint = FlixTheme.colors.textSecondary,
                            modifier = Modifier.size(TRAILING_ICON_SIZE),
                        )
                    }
                },
            )
        }
        SuggestionRow(
            text = stringResource(Res.string.search_now, query.trim()),
            leadingIcon = Res.drawable.ic_search,
            onClick = { actions.onSearchNow(query) },
        )
    }
}

@Composable
private fun SearchEmpty(state: SearchUiState.Empty, actions: SearchActions) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        StateView(
            icon = Res.drawable.ic_search_off,
            title = stringResource(Res.string.search_empty_title, state.query),
            body = stringResource(Res.string.search_empty_body),
        )
        if (state.suggestions.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.search_did_you_mean).uppercase(),
                style = FlixTheme.typography.overline,
                color = FlixTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FlixTheme.spacing.md),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm, Alignment.CenterHorizontally),
            ) {
                state.suggestions.forEach { suggestion ->
                    FlixChip(label = suggestion, onClick = { actions.onSearchNow(suggestion) })
                }
            }
        }
    }
}

@Composable
private fun SearchError(error: FlixError, recentSearches: List<String>, actions: SearchActions) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ErrorStateView(
            error = error,
            onRetry = actions.onRetry,
            upstreamTitle = Res.string.search_error_upstream_title,
            upstreamBody = Res.string.search_error_upstream_body,
        )
        if (recentSearches.isNotEmpty()) {
            SectionHeader(title = stringResource(Res.string.search_recent))
            recentSearches.forEach { recentSearch ->
                SuggestionRow(
                    text = recentSearch,
                    leadingIcon = Res.drawable.ic_history,
                    onClick = { actions.onSearchNow(recentSearch) },
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = FlixTheme.spacing.gutter)
            .padding(top = FlixTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
    ) {
        SearchFilter.entries.forEach { searchFilter ->
            FlixFilterChip(
                label = stringResource(searchFilter.label),
                selected = searchFilter == selected,
                onClick = { onFilterChange(searchFilter) },
            )
        }
    }
}

@Composable
private fun SearchLoading(
    previous: SearchUiState.Results?,
    filter: SearchFilter,
    nowPlaying: NowPlayingMarker,
    actions: SearchActions,
) {
    if (previous != null) {
        Box(Modifier.alpha(DIMMED_ALPHA)) {
            SearchResults(results = previous, filter = filter, nowPlaying = nowPlaying, actions = actions)
        }
        return
    }
    val loadingLabel = stringResource(Res.string.loading_results)
    Column(Modifier.clearAndSetSemantics { contentDescription = loadingLabel }) {
        FilterChipsRow(
            selected = SearchFilter.All,
            onFilterChange = {},
            modifier = Modifier.alpha(DIMMED_ALPHA),
        )
        BestResultCardSkeleton(Modifier.padding(top = FlixTheme.spacing.lg))
        SkeletonBox(
            Modifier
                .padding(horizontal = FlixTheme.spacing.gutter)
                .padding(top = FlixTheme.spacing.sectionGap, bottom = FlixTheme.spacing.md)
                .fillMaxWidth(SECTION_TITLE_SKELETON_WIDTH)
                .height(12.dp)
        )
        SKELETON_ROW_WIDTHS.forEach { (titleWidth, subtitleWidth) ->
            TrackRowSkeleton(titleWidth = titleWidth, subtitleWidth = subtitleWidth)
        }
    }
}

@Composable
private fun SearchResults(
    results: SearchUiState.Results,
    filter: SearchFilter,
    nowPlaying: NowPlayingMarker,
    actions: SearchActions,
) {
    Column {
        FilterChipsRow(selected = filter, onFilterChange = actions.onFilterChange)
        LazyColumn(
            contentPadding = PaddingValues(top = FlixTheme.spacing.lg, bottom = FlixTheme.spacing.xxl),
        ) {
            when (filter) {
                SearchFilter.All -> allResults(results, nowPlaying, actions)
                SearchFilter.Songs -> trackItems(results.tracks, nowPlaying, actions.onTrackClick)
                SearchFilter.Artists -> cardGrid(results.artists, key = { it.id }) { artist, cardModifier ->
                    MediaCard(
                        title = artist.name,
                        subtitle = stringResource(Res.string.type_artist),
                        artworkUrl = artist.artworkUrl,
                        onClick = { actions.onArtistClick(artist) },
                        circular = true,
                        modifier = cardModifier,
                    )
                }
                SearchFilter.Albums -> cardGrid(results.albums, key = { it.id }) { album, cardModifier ->
                    MediaCard(
                        title = album.name,
                        subtitle = album.artistName,
                        artworkUrl = album.artworkUrl,
                        onClick = { actions.onAlbumClick(album) },
                        modifier = cardModifier,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.allResults(
    results: SearchUiState.Results,
    nowPlaying: NowPlayingMarker,
    actions: SearchActions,
) {
    val bestTrack = results.tracks.first()
    item(key = "best") {
        BestResultCard(
            overline = stringResource(Res.string.search_best_result),
            title = bestTrack.title,
            subtitle = "${stringResource(Res.string.type_song)} · ${bestTrack.artistNames}",
            artworkUrl = bestTrack.artworkUrl,
            playLabel = stringResource(Res.string.play_item, bestTrack.title),
            onClick = { actions.onTrackOpen(bestTrack) },
            onPlay = { actions.onTrackClick(bestTrack) },
        )
    }

    val moreSongs = results.tracks.drop(1).take(SONGS_PREVIEW_COUNT)
    if (moreSongs.isNotEmpty()) {
        item(key = "songs-header") {
            SectionHeader(
                title = stringResource(Res.string.search_section_songs),
                actionLabel = stringResource(Res.string.see_all),
                onAction = { actions.onFilterChange(SearchFilter.Songs) },
                modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap),
            )
        }
        trackItems(moreSongs, nowPlaying, actions.onTrackClick)
    }

    if (results.artists.isNotEmpty()) {
        item(key = "artists-header") {
            SectionHeader(
                title = stringResource(Res.string.search_section_artists),
                modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap, bottom = FlixTheme.spacing.md),
            )
        }
        item(key = "artists") {
            ArtistCarousel(artists = results.artists, onArtistClick = actions.onArtistClick)
        }
    }

    if (results.albums.isNotEmpty()) {
        item(key = "albums-header") {
            SectionHeader(
                title = stringResource(Res.string.search_section_albums),
                modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap, bottom = FlixTheme.spacing.md),
            )
        }
        item(key = "albums") {
            AlbumCarousel(albums = results.albums, onAlbumClick = actions.onAlbumClick)
        }
    }
}

private fun LazyListScope.trackItems(
    tracks: List<Track>,
    nowPlaying: NowPlayingMarker,
    onTrackClick: (Track) -> Unit,
) {
    items(tracks, key = { it.id }) { track ->
        val trackMenu = LocalTrackMenu.current
        TrackRow(
            title = track.title,
            subtitle = listOfNotNull(track.artistNames, track.album?.name)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            artworkUrl = track.artworkUrl,
            onClick = { onTrackClick(track) },
            isCurrent = track.id == nowPlaying.trackId,
            isPlaying = nowPlaying.isPlaying,
            onMoreClick = { trackMenu.open(track) },
        )
    }
}

/** Two-column grid inside a LazyColumn, built from rows of two cards. */
private fun <Item> LazyListScope.cardGrid(
    gridItems: List<Item>,
    key: (Item) -> String,
    card: @Composable (item: Item, cardModifier: Modifier) -> Unit,
) {
    items(gridItems.chunked(2), key = { pair -> key(pair.first()) }) { pair ->
        Row(
            modifier = Modifier.padding(
                horizontal = FlixTheme.spacing.gutter,
                vertical = FlixTheme.spacing.sm,
            ),
            horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
        ) {
            pair.forEach { gridItem -> card(gridItem, Modifier.weight(1f)) }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private const val SONGS_PREVIEW_COUNT = 3
private const val DIMMED_ALPHA = 0.5f
private const val SECTION_TITLE_SKELETON_WIDTH = 0.35f
private val SKELETON_ROW_WIDTHS = listOf(0.7f to 0.45f, 0.6f to 0.4f, 0.75f to 0.35f)
private val TRAILING_ICON_SIZE = 20.dp

private val previewTracks = listOf(
    Track("1", "Fade To Black", listOf(ArtistRef("m", "Metallica")), AlbumRef("rtl", "Ride the Lightning"), 417.seconds, null),
    Track("2", "Fade to Black", listOf(ArtistRef("a", "Apocalyptica")), AlbumRef("pm", "Plays Metallica by Four Cellos"), 281.seconds, null),
    Track("3", "Fade to Black", listOf(ArtistRef("d", "Dire Straits")), AlbumRef("oes", "On Every Street"), 214.seconds, null),
)

private val previewResults = SearchUiState.Results(
    tracks = previewTracks,
    artists = previewTracks.distinctArtists(),
    albums = previewTracks.distinctAlbums(),
)

private val previewRecents = listOf("fade to black", "eminem killshot", "bad bunny")

@Composable
private fun SearchPreviewContent(
    uiState: SearchUiState,
    query: String = "fade to black",
    filter: SearchFilter = SearchFilter.All,
) {
    SearchContent(
        query = query,
        uiState = uiState,
        filter = filter,
        recentSearches = previewRecents,
        actions = SearchActions(),
    )
}








