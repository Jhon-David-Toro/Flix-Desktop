package com.flix.music.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.FavoritesArtwork
import com.flix.music.core.designsystem.component.FlixCard
import com.flix.music.core.designsystem.component.GenreTile
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.PlayPauseButton
import com.flix.music.core.designsystem.component.PlaylistArtwork
import com.flix.music.core.designsystem.component.ProfileAvatar
import com.flix.music.core.designsystem.component.SectionHeader
import com.flix.music.core.designsystem.component.TextButton
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.TrackRowSkeleton
import com.flix.music.core.designsystem.component.handCursor
import com.flix.music.core.designsystem.component.util.toClock
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Genre
import com.flix.music.core.model.Genres
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.Track
import com.flix.music.core.ui.LocalDetailNavigator
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.action_retry
import com.flix.music.resources.favorites_title
import com.flix.music.resources.history_title
import com.flix.music.resources.home_discover
import com.flix.music.resources.home_explore
import com.flix.music.resources.home_explore_genre
import com.flix.music.resources.home_first_run_body
import com.flix.music.resources.home_first_run_title
import com.flix.music.resources.home_greeting_afternoon
import com.flix.music.resources.home_greeting_morning
import com.flix.music.resources.home_greeting_night
import com.flix.music.resources.home_more_from
import com.flix.music.resources.home_recent
import com.flix.music.resources.home_resume
import com.flix.music.resources.home_resume_play
import com.flix.music.resources.home_section_error
import com.flix.music.resources.home_time_left
import com.flix.music.resources.home_your_playlists
import com.flix.music.resources.ic_graphic_eq
import com.flix.music.resources.ic_history
import com.flix.music.resources.ic_pause_filled
import com.flix.music.resources.ic_play_arrow_filled
import com.flix.music.resources.ic_search
import com.flix.music.resources.player_pause
import com.flix.music.resources.profile_title
import com.flix.music.resources.search_placeholder
import com.flix.music.resources.see_all
import com.flix.music.resources.songs_count
import com.flix.music.resources.tab_library
import com.flix.music.resources.tab_search
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/** Every user action on Home. Defaults do nothing, which keeps previews short. */
data class HomeActions(
    val onOpenProfile: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onOpenSearch: () -> Unit = {},
    val onOpenGenre: (Genre) -> Unit = {},
    val onOpenLibrary: () -> Unit = {},
    val onOpenFavorites: () -> Unit = {},
    val onOpenPlaylist: (Long) -> Unit = {},
    val onOpenArtist: (ArtistRef) -> Unit = {},
    val onResume: () -> Unit = {},
    val onOpenResume: () -> Unit = {},
    val onRecentClick: (Track) -> Unit = {},
    val onMoreFromArtistClick: (Track) -> Unit = {},
    val onTrackMore: (Track) -> Unit = {},
    val onRetryMoreFromArtist: () -> Unit = {},
)

@Composable
fun HomeScreen(
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenGenre: (Genre) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val detailNavigator = LocalDetailNavigator.current
    val trackMenu = LocalTrackMenu.current
    val actions = remember(viewModel, detailNavigator, trackMenu) {
        HomeActions(
            onOpenProfile = onOpenProfile,
            onOpenHistory = onOpenHistory,
            onOpenSearch = onOpenSearch,
            onOpenGenre = onOpenGenre,
            onOpenLibrary = onOpenLibrary,
            onOpenFavorites = onOpenFavorites,
            onOpenPlaylist = onOpenPlaylist,
            onOpenArtist = detailNavigator::openArtist,
            onResume = viewModel::onResume,
            onOpenResume = {
                viewModel.onOpenResume()
                detailNavigator.openPlayer()
            },
            onRecentClick = viewModel::onRecentClick,
            onMoreFromArtistClick = viewModel::onMoreFromArtistClick,
            onTrackMore = { track -> trackMenu.open(track) },
            onRetryMoreFromArtist = viewModel::onRetryMoreFromArtist,
        )
    }
    HomeContent(
        uiState = uiState,
        actions = actions,
        isPlaying = nowPlaying.isPlaying,
        nowPlaying = nowPlaying,
        modifier = modifier,
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl),
    ) {
        item(key = "top-bar") {
            HomeTopBar(
                greeting = uiState.greeting,
                showShortcuts = !uiState.isFirstRun,
                onOpenHistory = actions.onOpenHistory,
                onOpenProfile = actions.onOpenProfile,
            )
        }
        if (uiState.isFirstRun) {
            firstRun(actions)
            return@LazyColumn
        }
        uiState.resume?.let { resume ->
            item(key = "resume") {
                ResumeCard(
                    resume = resume,
                    isPlaying = resume.isInPlayer && isPlaying,
                    onResume = actions.onResume,
                    onOpen = actions.onOpenResume,
                    modifier = Modifier
                        .padding(horizontal = FlixTheme.spacing.gutter)
                        .padding(top = FlixTheme.spacing.xs),
                )
            }
        }
        if (uiState.recent.isNotEmpty()) {
            item(key = "recent") {
                SectionHeader(
                    title = stringResource(Res.string.home_recent),
                    actionLabel = stringResource(Res.string.history_title),
                    onAction = actions.onOpenHistory,
                    modifier = Modifier.padding(top = SECTION_TOP),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = FlixTheme.spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
                ) {
                    items(uiState.recent, key = { it.id }) { track ->
                        HomeCard(
                            title = track.title,
                            subtitle = track.artistNames,
                            onClick = { actions.onRecentClick(track) },
                        ) { artworkModifier -> CoverImage(track.artworkUrl, artworkModifier) }
                    }
                }
            }
        }
        uiState.topArtist?.let { artist -> moreFromArtist(artist, uiState.moreFromArtist, actions, nowPlaying) }
        if (uiState.playlists.isNotEmpty() || uiState.favoriteCount > 0) {
            item(key = "playlists") { YourPlaylists(uiState, actions) }
        }
        if (uiState.discoverGenres.isNotEmpty()) {
            item(key = "discover") {
                SectionHeader(
                    title = stringResource(Res.string.home_discover),
                    actionLabel = stringResource(Res.string.home_explore),
                    onAction = actions.onOpenSearch,
                    modifier = Modifier.padding(top = SECTION_TOP),
                )
                GenreGrid(uiState.discoverGenres, actions.onOpenGenre, Modifier.padding(top = FlixTheme.spacing.sm))
            }
        }
    }
}

/** Title with the greeting (22sp), the History shortcut and the profile avatar. */
@Composable
private fun HomeTopBar(
    greeting: Greeting,
    showShortcuts: Boolean,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val profileLabel = stringResource(Res.string.profile_title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = FlixTheme.spacing.gutter, end = FlixTheme.spacing.xs, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(greeting.label()),
            style = FlixTheme.typography.titleLarge,
            color = FlixTheme.colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (showShortcuts) {
            IconButton(onClick = onOpenHistory) {
                Icon(
                    painter = painterResource(Res.drawable.ic_history),
                    contentDescription = stringResource(Res.string.history_title),
                    tint = FlixTheme.colors.textSecondary,
                )
            }
            IconButton(onClick = onOpenProfile) {
                ProfileAvatar(size = 32.dp, modifier = Modifier.semantics { contentDescription = profileLabel })
            }
        }
    }
}

/**
 * "Seguir escuchando" (design `.card`): overline, 56dp cover, title, "Metallica · quedan
 * 4:16", a small play button and the progress beam when the song is in the player.
 */
@Composable
private fun ResumeCard(
    resume: ResumeItem,
    isPlaying: Boolean,
    onResume: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = listOfNotNull(
        resume.track.artistNames,
        resume.remaining?.let { remaining -> stringResource(Res.string.home_time_left, remaining.toClock()) },
    ).joinToString(" · ")
    FlixCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.padding(FlixTheme.spacing.md)) {
            Text(
                text = stringResource(Res.string.home_resume).uppercase(),
                style = FlixTheme.typography.overline,
                color = FlixTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
            ) {
                CoverImage(
                    artworkUrl = resume.track.artworkUrl,
                    modifier = Modifier.size(56.dp),
                    shape = FlixTheme.shapes.sm,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = resume.track.title,
                        style = FlixTheme.typography.label,
                        color = FlixTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = FlixTheme.typography.caption,
                        color = FlixTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PlayPauseButton(
                    icon = if (isPlaying) Res.drawable.ic_pause_filled else Res.drawable.ic_play_arrow_filled,
                    contentDescription = stringResource(
                        if (isPlaying) Res.string.player_pause else Res.string.home_resume_play,
                        resume.track.title,
                    ),
                    onClick = onResume,
                    size = 36.dp,
                    iconSize = 22.dp,
                )
            }
            resume.progress?.let { progress -> ProgressBeam(progress, Modifier.padding(top = FlixTheme.spacing.md)) }
        }
    }
}

/** `.beam`: 3dp track in surfaceVariant with the played part in primary. */
@Composable
private fun ProgressBeam(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(FlixTheme.shapes.pill)
            .background(FlixTheme.colors.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .clip(FlixTheme.shapes.pill)
                .background(FlixTheme.colors.primary),
        )
    }
}

/** "Más de Metallica": three songs of the most played artist, loaded on its own. */
private fun LazyListScope.moreFromArtist(
    artist: ArtistRef,
    section: RemoteSection<List<Track>>,
    actions: HomeActions,
    nowPlaying: NowPlayingMarker,
) {
    if (section is RemoteSection.Loaded && section.content.isEmpty()) return
    item(key = "more-header") {
        SectionHeader(
            title = stringResource(Res.string.home_more_from, artist.name),
            actionLabel = stringResource(Res.string.see_all).takeIf { section is RemoteSection.Loaded },
            onAction = { actions.onOpenArtist(artist) },
            modifier = Modifier.padding(top = SECTION_TOP),
        )
    }
    when (section) {
        RemoteSection.Loading -> items(SKELETON_WIDTHS.size, key = { index -> "more-skeleton-$index" }) { index ->
            val (titleWidth, subtitleWidth) = SKELETON_WIDTHS[index]
            TrackRowSkeleton(titleWidth = titleWidth, subtitleWidth = subtitleWidth)
        }
        RemoteSection.Failed -> item(key = "more-error") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FlixTheme.spacing.gutter, end = FlixTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.home_section_error),
                    style = FlixTheme.typography.bodySmall,
                    color = FlixTheme.colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onRetryMoreFromArtist) {
                    Text(
                        text = stringResource(Res.string.action_retry),
                        style = FlixTheme.typography.label,
                        color = FlixTheme.colors.primaryContent,
                    )
                }
            }
        }
        is RemoteSection.Loaded -> items(section.content, key = { "more-${it.id}" }) { track ->
            TrackRow(
                title = track.title,
                subtitle = listOfNotNull(track.artistNames, track.album?.name).joinToString(" · "),
                artworkUrl = track.artworkUrl,
                onClick = { actions.onMoreFromArtistClick(track) },
                isCurrent = track.id == nowPlaying.trackId,
                isPlaying = nowPlaying.isPlaying,
                onMoreClick = { actions.onTrackMore(track) },
            )
        }
    }
}

/** "Tus playlists": Favoritos first, then the playlists, as 120dp cards. */
@Composable
private fun YourPlaylists(uiState: HomeUiState, actions: HomeActions) {
    SectionHeader(
        title = stringResource(Res.string.home_your_playlists),
        actionLabel = stringResource(Res.string.tab_library),
        onAction = actions.onOpenLibrary,
        modifier = Modifier.padding(top = SECTION_TOP),
    )
    LazyRow(
        contentPadding = PaddingValues(start = FlixTheme.spacing.gutter, end = FlixTheme.spacing.gutter, top = PLAYLIST_STACK_ROOM),
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        if (uiState.favoriteCount > 0) {
            item(key = "favorites") {
                HomeCard(
                    title = stringResource(Res.string.favorites_title),
                    subtitle = pluralStringResource(Res.plurals.songs_count, uiState.favoriteCount, uiState.favoriteCount),
                    onClick = actions.onOpenFavorites,
                ) { artworkModifier ->
                    FavoritesArtwork(size = CARD_WIDTH, iconSize = 44.dp, shape = FlixTheme.shapes.md, modifier = artworkModifier)
                }
            }
        }
        items(uiState.playlists, key = { playlist -> "playlist-${playlist.id}" }) { playlist ->
            HomeCard(
                title = playlist.name,
                subtitle = pluralStringResource(Res.plurals.songs_count, playlist.tracks.size, playlist.tracks.size),
                onClick = { actions.onOpenPlaylist(playlist.id) },
            ) { artworkModifier ->
                PlaylistArtwork(
                    coverUrls = playlist.coverUrls,
                    size = CARD_WIDTH,
                    cornerRadius = 12.dp,
                    modifier = artworkModifier,
                )
            }
        }
    }
}

/** 120dp card of a carousel (design `card(…, 'a120')`). */
@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    artwork: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(CARD_WIDTH)
            .clip(FlixTheme.shapes.md)
            .handCursor().clickable(onClick = onClick),
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
            modifier = Modifier.padding(top = FlixTheme.spacing.sm),
        )
        Text(
            text = subtitle,
            style = FlixTheme.typography.caption,
            color = FlixTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CoverImage(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = FlixTheme.shapes.md,
) {
    AsyncImage(
        model = artworkUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .background(FlixTheme.colors.surfaceVariant),
    )
}

/** Genre tiles in two columns, 64dp high (design `.g2` + `gtile(…, 64)`). */
@Composable
private fun GenreGrid(genres: List<Genre>, onGenreClick: (Genre) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = FlixTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        genres.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { genre ->
                    GenreTile(
                        name = genre.name,
                        gradientStart = Color(genre.gradientStart),
                        gradientEnd = Color(genre.gradientEnd),
                        onClick = { onGenreClick(genre) },
                        height = GENRE_TILE_HEIGHT,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * First run (design `homefirst`): instead of empty sections, the one useful action —
 * search — plus a way in through genres.
 */
private fun LazyListScope.firstRun(actions: HomeActions) {
    item(key = "first-run") {
        Column(Modifier.padding(horizontal = FlixTheme.spacing.gutter)) {
            Column(Modifier.padding(top = 28.dp, bottom = 18.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(FlixTheme.shapes.circle)
                        .background(FlixTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_graphic_eq),
                        contentDescription = null,
                        tint = FlixTheme.colors.primaryContent,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = stringResource(Res.string.home_first_run_title),
                    style = FlixTheme.typography.titleLarge,
                    color = FlixTheme.colors.textPrimary,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .semantics { heading() },
                )
                Text(
                    text = stringResource(Res.string.home_first_run_body),
                    style = FlixTheme.typography.bodySmall,
                    color = FlixTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            SearchShortcut(onClick = actions.onOpenSearch)
        }
    }
    item(key = "first-run-genres") {
        SectionHeader(title = stringResource(Res.string.home_explore_genre), modifier = Modifier.padding(top = SECTION_TOP))
        GenreGrid(
            genres = Genres.all.take(FIRST_RUN_GENRES),
            onGenreClick = actions.onOpenGenre,
            modifier = Modifier.padding(top = FlixTheme.spacing.sm),
        )
    }
}

/** Looks like the search field (`.field`) and takes you to Buscar. */
@Composable
private fun SearchShortcut(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(FlixTheme.shapes.md)
            .background(FlixTheme.colors.surfaceVariant)
            .handCursor().clickable(onClickLabel = stringResource(Res.string.tab_search), onClick = onClick)
            .padding(horizontal = FlixTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_search),
            contentDescription = null,
            tint = FlixTheme.colors.textSecondary,
        )
        Text(
            text = stringResource(Res.string.search_placeholder),
            style = FlixTheme.typography.body,
            color = FlixTheme.colors.textTertiary,
        )
    }
}

private fun Greeting.label(): StringResource = when (this) {
    Greeting.Morning -> Res.string.home_greeting_morning
    Greeting.Afternoon -> Res.string.home_greeting_afternoon
    Greeting.Night -> Res.string.home_greeting_night
}

private const val FIRST_RUN_GENRES = 6
private val SECTION_TOP = 22.dp
private val CARD_WIDTH: Dp = 120.dp
private val GENRE_TILE_HEIGHT = 64.dp

/** Room above the carousel so the playlist "stack" outline is not clipped. */
private val PLAYLIST_STACK_ROOM = 4.dp
private val SKELETON_WIDTHS = listOf(0.7f to 0.45f, 0.55f to 0.35f, 0.65f to 0.4f)

private val previewTracks = listOf(
    Track("1", "Killshot", listOf(ArtistRef(null, "Eminem")), null, 253.seconds, null),
    Track("2", "Midnight City", listOf(ArtistRef(null, "M83")), null, 243.seconds, null),
)


