package com.flix.music.feature.artist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.AlbumCarousel
import com.flix.music.core.designsystem.component.ErrorStateView
import com.flix.music.core.designsystem.component.FlixBottomSheet
import com.flix.music.core.designsystem.component.FlixOutlinedButton
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.PlayPauseButton
import com.flix.music.core.designsystem.component.SectionHeader
import com.flix.music.core.designsystem.component.SheetQuickAction
import com.flix.music.core.designsystem.component.SheetQuickActions
import com.flix.music.core.designsystem.component.SheetRow
import com.flix.music.core.designsystem.component.SheetTitle
import com.flix.music.core.designsystem.component.SkeletonBox
import com.flix.music.core.designsystem.component.TextButton
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.TrackRowSkeleton
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.Album
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import com.flix.music.core.ui.LocalDetailNavigator
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.core.ui.rememberShare
import com.flix.music.resources.Res
import com.flix.music.resources.action_back
import com.flix.music.resources.action_more_options
import com.flix.music.resources.action_share
import com.flix.music.resources.artist_albums
import com.flix.music.resources.artist_follow
import com.flix.music.resources.artist_following
import com.flix.music.resources.artist_play
import com.flix.music.resources.artist_popular
import com.flix.music.resources.artist_share_text
import com.flix.music.resources.artist_show_more
import com.flix.music.resources.ic_arrow_back
import com.flix.music.resources.ic_check
import com.flix.music.resources.ic_more_vert
import com.flix.music.resources.ic_person
import com.flix.music.resources.ic_play_arrow_filled
import com.flix.music.resources.ic_share
import com.flix.music.resources.ic_shuffle
import com.flix.music.resources.player_shuffle
import com.flix.music.resources.playlist_play
import com.flix.music.resources.type_artist
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Every user action on the artist screen. Defaults do nothing, which keeps previews short. */
data class ArtistActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onPlay: () -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onToggleFollow: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onTrackClick: (Track) -> Unit = {},
    val onTrackMore: (Track) -> Unit = {},
    val onAlbumClick: (Album) -> Unit = {},
)

@Composable
fun ArtistScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val trackMenu = LocalTrackMenu.current
    val detailNavigator = LocalDetailNavigator.current
    val share = rememberShare()
    val shareTemplate = stringResource(Res.string.artist_share_text)
    ArtistContent(
        uiState = uiState,
        nowPlaying = nowPlaying,
        actions = ArtistActions(
            onBack = onBack,
            onRetry = viewModel::onRetry,
            onPlay = viewModel::onPlay,
            onShuffle = viewModel::onShuffle,
            onToggleFollow = viewModel::onToggleFollow,
            onShare = { share(shareTemplate.format(uiState.name)) },
            onTrackClick = viewModel::onTrackClick,
            onTrackMore = { track -> trackMenu.open(track) },
            onAlbumClick = { album ->
                detailNavigator.openAlbum(AlbumRef(album.id, album.name), ArtistRef(null, album.artistName))
            },
        ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistContent(
    uiState: ArtistUiState,
    actions: ArtistActions,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    val listState = rememberLazyListState()
    val showSolidBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var showAllPopular by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val content = uiState as? ArtistUiState.Content

    Box(modifier.fillMaxSize()) {
        when (uiState) {
            is ArtistUiState.Content -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl),
            ) {
                item(key = "hero") { ArtistHero(uiState, actions) }
                item(key = "popular-header") {
                    SectionHeader(title = stringResource(Res.string.artist_popular), modifier = Modifier.padding(top = 22.dp))
                }
                val visiblePopular = if (showAllPopular) uiState.popular else uiState.popular.take(POPULAR_PREVIEW_COUNT)
                itemsIndexed(visiblePopular, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        title = track.title,
                        subtitle = track.artistNames,
                        artworkUrl = track.artworkUrl,
                        onClick = { actions.onTrackClick(track) },
                        isCurrent = track.id == nowPlaying.trackId,
                        isPlaying = nowPlaying.isPlaying,
                        onMoreClick = { actions.onTrackMore(track) },
                        number = index + 1,
                        modifier = Modifier.animateItem(),
                    )
                }
                if (!showAllPopular && uiState.popular.size > POPULAR_PREVIEW_COUNT) {
                    item(key = "show-more") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = { showAllPopular = true }) {
                                Text(
                                    text = stringResource(Res.string.artist_show_more),
                                    style = FlixTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = FlixTheme.colors.primaryContent,
                                )
                            }
                        }
                    }
                }
                if (uiState.albums.isNotEmpty()) {
                    item(key = "albums") {
                        SectionHeader(title = stringResource(Res.string.artist_albums), modifier = Modifier.padding(top = 22.dp))
                        AlbumCarousel(
                            albums = uiState.albums,
                            onAlbumClick = actions.onAlbumClick,
                            modifier = Modifier.padding(top = FlixTheme.spacing.sm),
                        )
                    }
                }
            }
            is ArtistUiState.Error -> Column {
                Spacer(Modifier.height(TOP_BAR_HEIGHT))
                ErrorStateView(error = uiState.error, onRetry = actions.onRetry)
            }
            is ArtistUiState.Loading -> ArtistSkeleton()
        }
        ArtistTopBar(
            name = uiState.name,
            solid = showSolidBar || content == null,
            onBack = actions.onBack,
            onMore = { menuOpen = true },
            showMore = content != null,
        )
    }

    if (menuOpen && content != null) {
        ArtistMenu(content, actions, onDismiss = { menuOpen = false })
    }
}

/**
 * Full-bleed hero (design `.hero`, 250dp): the cover of the top song under a scrim that
 * fades into the background, then the overline, the name and the actions overlapping it.
 */
@Composable
private fun ArtistHero(content: ArtistUiState.Content, actions: ArtistActions) {
    val background = FlixTheme.colors.background
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT),
        ) {
            AsyncImage(
                model = content.heroArtworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(FlixTheme.colors.surfaceVariant),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = HERO_TOP_SCRIM_ALPHA),
                            0.3f to Color.Transparent,
                            1f to background,
                        ),
                    ),
            )
        }
        Column(
            Modifier
                .padding(top = HERO_HEIGHT - HERO_OVERLAP)
                .padding(horizontal = FlixTheme.spacing.gutter),
        ) {
            Text(
                text = stringResource(Res.string.type_artist).uppercase(),
                style = FlixTheme.typography.overline,
                color = FlixTheme.colors.textSecondary,
            )
            Text(
                text = content.name,
                style = FlixTheme.typography.display.copy(fontSize = 36.sp, lineHeight = 36.sp),
                color = FlixTheme.colors.textPrimary,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .semantics { heading() },
            )
            Row(
                modifier = Modifier.padding(top = FlixTheme.spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
            ) {
                if (content.canFollow) {
                    FlixOutlinedButton(
                        text = stringResource(if (content.isFollowing) Res.string.artist_following else Res.string.artist_follow),
                        onClick = actions.onToggleFollow,
                        icon = if (content.isFollowing) Res.drawable.ic_check else null,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (content.popular.isNotEmpty()) {
                    IconButton(onClick = actions.onShuffle) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_shuffle),
                            contentDescription = stringResource(Res.string.player_shuffle),
                            tint = FlixTheme.colors.textPrimary,
                        )
                    }
                    PlayPauseButton(
                        icon = Res.drawable.ic_play_arrow_filled,
                        contentDescription = stringResource(Res.string.artist_play, content.name),
                        onClick = actions.onPlay,
                        size = 52.dp,
                        iconSize = 28.dp,
                    )
                }
            }
        }
    }
}

/**
 * Transparent with white icons over the hero; once the hero scrolls away it becomes
 * a solid surface bar with the artist's name (design note).
 */
@Composable
private fun ArtistTopBar(name: String, solid: Boolean, onBack: () -> Unit, onMore: () -> Unit, showMore: Boolean) {
    val iconTint = if (solid) FlixTheme.colors.textPrimary else Color.White
    Box(
        Modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT),
    ) {
        AnimatedVisibility(visible = solid, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(FlixTheme.colors.background),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FlixTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(Res.string.action_back),
                    tint = iconTint,
                )
            }
            Text(
                text = if (solid) name else "",
                style = FlixTheme.typography.title,
                color = FlixTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showMore) {
                IconButton(onClick = onMore) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_more_vert),
                        contentDescription = stringResource(Res.string.action_more_options),
                        tint = iconTint,
                    )
                }
            }
        }
    }
}

/** Artist menu (design "Menú: Artista"): Reproducir · Aleatorio · Seguir, then Compartir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistMenu(content: ArtistUiState.Content, actions: ArtistActions, onDismiss: () -> Unit) {
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
        SheetTitle(content.name)
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
            if (content.canFollow) {
                SheetQuickAction(
                    icon = if (content.isFollowing) Res.drawable.ic_check else Res.drawable.ic_person,
                    label = stringResource(if (content.isFollowing) Res.string.artist_following else Res.string.artist_follow),
                    onClick = { closeThen(actions.onToggleFollow) },
                )
            }
        }
        SheetRow(
            icon = Res.drawable.ic_share,
            label = stringResource(Res.string.action_share),
            onClick = { closeThen(actions.onShare) },
        )
    }
}

@Composable
private fun ArtistSkeleton() {
    Column {
        SkeletonBox(
            Modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT),
            shape = FlixTheme.shapes.xs,
        )
        repeat(POPULAR_PREVIEW_COUNT) { TrackRowSkeleton() }
    }
}

private const val POPULAR_PREVIEW_COUNT = 5
private const val HERO_TOP_SCRIM_ALPHA = 0.25f
private val HERO_HEIGHT = 250.dp
private val HERO_OVERLAP = 60.dp
private val TOP_BAR_HEIGHT = 56.dp

