package com.flix.music.feature.genre

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.designsystem.component.AlbumCarousel
import com.flix.music.core.designsystem.component.ArtistCarousel
import com.flix.music.core.designsystem.component.ErrorStateView
import com.flix.music.core.designsystem.component.FlixPrimaryButton
import com.flix.music.core.designsystem.component.FlixSecondaryButton
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.SectionHeader
import com.flix.music.core.designsystem.component.SkeletonBox
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.TrackRowSkeleton
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.Album
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.Artist
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Genre
import com.flix.music.core.model.Genres
import com.flix.music.core.model.Track
import com.flix.music.core.model.distinctAlbums
import com.flix.music.core.model.distinctArtists
import com.flix.music.core.ui.LocalDetailNavigator
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.action_back
import com.flix.music.resources.genre_albums
import com.flix.music.resources.genre_artists
import com.flix.music.resources.genre_empty_body
import com.flix.music.resources.genre_empty_title
import com.flix.music.resources.genre_overline
import com.flix.music.resources.genre_play
import com.flix.music.resources.genre_popular_songs
import com.flix.music.resources.genre_shuffle
import com.flix.music.resources.ic_arrow_back
import com.flix.music.resources.ic_play_arrow_filled
import com.flix.music.resources.ic_search_off
import com.flix.music.resources.ic_shuffle
import com.flix.music.resources.loading_results
import com.flix.music.resources.see_all
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** Every user action on the Genre screen. Defaults do nothing, which keeps previews short. */
data class GenreActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onPlay: () -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onTrackClick: (Track) -> Unit = {},
    val onArtistClick: (Artist) -> Unit = {},
    val onAlbumClick: (Album) -> Unit = {},
)

@Composable
fun GenreScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GenreViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val detailNavigator = LocalDetailNavigator.current
    val actions = remember(viewModel, onBack, detailNavigator) {
        GenreActions(
            onBack = onBack,
            onRetry = viewModel::onRetry,
            onPlay = viewModel::onPlay,
            onShuffle = viewModel::onShuffle,
            onTrackClick = viewModel::onTrackClick,
            onArtistClick = { artist -> detailNavigator.openArtist(ArtistRef(artist.id, artist.name)) },
            onAlbumClick = { album ->
                detailNavigator.openAlbum(AlbumRef(album.id, album.name), ArtistRef(null, album.artistName))
            },
        )
    }
    GenreContent(
        genre = viewModel.genre,
        uiState = uiState,
        actions = actions,
        nowPlaying = nowPlaying,
        modifier = modifier,
    )
}

@Composable
fun GenreContent(
    genre: Genre,
    uiState: GenreUiState,
    actions: GenreActions,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    var showAllSongs by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl),
    ) {
        item(key = "hero") { GenreHero(genre = genre, onBack = actions.onBack) }
        item(key = "actions") {
            Row(
                modifier = Modifier
                    .padding(horizontal = FlixTheme.spacing.gutter)
                    .padding(top = FlixTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
            ) {
                val hasSongs = uiState is GenreUiState.Content
                FlixPrimaryButton(
                    text = stringResource(Res.string.genre_play),
                    onClick = actions.onPlay,
                    icon = Res.drawable.ic_play_arrow_filled,
                    enabled = hasSongs,
                )
                FlixSecondaryButton(
                    text = stringResource(Res.string.genre_shuffle),
                    onClick = actions.onShuffle,
                    icon = Res.drawable.ic_shuffle,
                    enabled = hasSongs,
                )
            }
        }
        when (uiState) {
            GenreUiState.Loading -> item(key = "loading") { GenreLoading() }
            GenreUiState.Empty -> item(key = "empty") {
                StateView(
                    icon = Res.drawable.ic_search_off,
                    title = stringResource(Res.string.genre_empty_title, genre.name),
                    body = stringResource(Res.string.genre_empty_body),
                )
            }
            is GenreUiState.Error -> item(key = "error") {
                ErrorStateView(error = uiState.error, onRetry = actions.onRetry)
            }
            is GenreUiState.Content -> genreSections(
                content = uiState,
                showAllSongs = showAllSongs,
                onShowAllSongs = { showAllSongs = true },
                nowPlaying = nowPlaying,
                actions = actions,
            )
        }
    }
}

/** Gradient hero with the genre's fixed colors, white overline and display title. */
@Composable
private fun GenreHero(genre: Genre, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HERO_HEIGHT)
            .background(Brush.linearGradient(listOf(Color(genre.gradientStart), Color(genre.gradientEnd)))),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(FlixTheme.spacing.xs),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = stringResource(Res.string.action_back),
                tint = HeroContentColor,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(FlixTheme.spacing.gutter),
        ) {
            Text(
                text = stringResource(Res.string.genre_overline).uppercase(),
                style = FlixTheme.typography.overline,
                color = HeroOverlineColor,
            )
            Text(
                text = genre.name,
                style = FlixTheme.typography.display,
                color = HeroContentColor,
                modifier = Modifier
                    .padding(top = FlixTheme.spacing.sm)
                    .semantics { heading() },
            )
        }
    }
}

@Composable
private fun GenreLoading() {
    val loadingLabel = stringResource(Res.string.loading_results)
    Column(Modifier.clearAndSetSemantics { contentDescription = loadingLabel }) {
        SkeletonBox(
            Modifier
                .padding(horizontal = FlixTheme.spacing.gutter)
                .padding(top = FlixTheme.spacing.sectionGap, bottom = FlixTheme.spacing.md)
                .fillMaxWidth(SECTION_TITLE_SKELETON_WIDTH)
                .height(12.dp)
        )
        repeat(SONGS_PREVIEW_COUNT) { TrackRowSkeleton() }
    }
}

private fun LazyListScope.genreSections(
    content: GenreUiState.Content,
    showAllSongs: Boolean,
    onShowAllSongs: () -> Unit,
    nowPlaying: NowPlayingMarker,
    actions: GenreActions,
) {
    val hasMoreSongs = content.tracks.size > SONGS_PREVIEW_COUNT
    item(key = "songs-header") {
        SectionHeader(
            title = stringResource(Res.string.genre_popular_songs),
            actionLabel = if (hasMoreSongs && !showAllSongs) stringResource(Res.string.see_all) else null,
            onAction = onShowAllSongs,
            modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap),
        )
    }
    val visibleSongs = if (showAllSongs) content.tracks else content.tracks.take(SONGS_PREVIEW_COUNT)
    items(visibleSongs, key = { it.id }) { track ->
        val trackMenu = LocalTrackMenu.current
        TrackRow(
            title = track.title,
            subtitle = track.artistNames,
            artworkUrl = track.artworkUrl,
            onClick = { actions.onTrackClick(track) },
            isCurrent = track.id == nowPlaying.trackId,
            isPlaying = nowPlaying.isPlaying,
            onMoreClick = { trackMenu.open(track) },
        )
    }

    if (content.artists.isNotEmpty()) {
        item(key = "artists-header") {
            SectionHeader(
                title = stringResource(Res.string.genre_artists),
                modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap, bottom = FlixTheme.spacing.md),
            )
        }
        item(key = "artists") {
            ArtistCarousel(artists = content.artists, onArtistClick = actions.onArtistClick)
        }
    }

    if (content.albums.isNotEmpty()) {
        item(key = "albums-header") {
            SectionHeader(
                title = stringResource(Res.string.genre_albums),
                modifier = Modifier.padding(top = FlixTheme.spacing.sectionGap, bottom = FlixTheme.spacing.md),
            )
        }
        item(key = "albums") {
            AlbumCarousel(albums = content.albums, onAlbumClick = actions.onAlbumClick)
        }
    }
}

private const val SONGS_PREVIEW_COUNT = 3
private const val SECTION_TITLE_SKELETON_WIDTH = 0.35f
private val HERO_HEIGHT = 220.dp
private val HeroContentColor = Color.White
private val HeroOverlineColor = Color.White.copy(alpha = 0.75f)

private val previewTracks = listOf(
    Track("1", "Master of Puppets", listOf(ArtistRef("m", "Metallica")), AlbumRef("mop", "Master of Puppets"), 516.seconds, null),
    Track("2", "Symphony of Destruction", listOf(ArtistRef("mg", "Megadeth")), AlbumRef("cte", "Countdown to Extinction"), 242.seconds, null),
    Track("3", "Fade To Black", listOf(ArtistRef("m", "Metallica")), AlbumRef("rtl", "Ride the Lightning"), 417.seconds, null),
    Track("4", "Raining Blood", listOf(ArtistRef("s", "Slayer")), AlbumRef("rib", "Reign in Blood"), 254.seconds, null),
)


