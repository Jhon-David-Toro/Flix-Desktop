package com.flix.music.feature.song

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.ErrorStateView
import com.flix.music.core.designsystem.component.FavoriteButton
import com.flix.music.core.designsystem.component.FlixOutlinedButton
import com.flix.music.core.designsystem.component.FlixPrimaryButton
import com.flix.music.core.designsystem.component.FlixTopAppBar
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.component.SectionHeader
import com.flix.music.core.designsystem.component.SkeletonBox
import com.flix.music.core.designsystem.component.TrackRow
import com.flix.music.core.designsystem.component.handCursor
import com.flix.music.core.designsystem.component.util.toClock
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import com.flix.music.core.ui.LocalDetailNavigator
import com.flix.music.core.ui.LocalTrackMenu
import com.flix.music.core.ui.flixViewModel
import com.flix.music.core.ui.rememberShare
import com.flix.music.resources.Res
import com.flix.music.resources.action_more_options
import com.flix.music.resources.action_share
import com.flix.music.resources.add_to_playlist
import com.flix.music.resources.artist_follow
import com.flix.music.resources.artist_following
import com.flix.music.resources.ic_check
import com.flix.music.resources.ic_chevron_right
import com.flix.music.resources.ic_more_vert
import com.flix.music.resources.ic_play_arrow_filled
import com.flix.music.resources.ic_playlist_add
import com.flix.music.resources.ic_share
import com.flix.music.resources.playlist_play
import com.flix.music.resources.song_album_caption
import com.flix.music.resources.song_from_album
import com.flix.music.resources.song_more_like_this
import com.flix.music.resources.song_unavailable
import com.flix.music.resources.track_share_text
import com.flix.music.resources.type_artist
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** Every user action on the song screen. Defaults do nothing, which keeps previews short. */
data class SongActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onPlay: () -> Unit = {},
    val onToggleFavorite: () -> Unit = {},
    val onAddToPlaylist: (Track) -> Unit = {},
    val onShare: (Track) -> Unit = {},
    val onMore: (Track) -> Unit = {},
    val onOpenAlbum: (AlbumRef, ArtistRef?) -> Unit = { _, _ -> },
    val onOpenArtist: (ArtistRef) -> Unit = {},
    val onToggleFollow: () -> Unit = {},
    val onMoreLikeThisClick: (Track) -> Unit = {},
)

@Composable
fun SongScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SongViewModel = flixViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val trackMenu = LocalTrackMenu.current
    val detailNavigator = LocalDetailNavigator.current
    val share = rememberShare()
    val shareTemplate = stringResource(Res.string.track_share_text)
    SongContent(
        uiState = uiState,
        nowPlaying = nowPlaying,
        actions = SongActions(
            onBack = onBack,
            onRetry = viewModel::onRetry,
            onPlay = viewModel::onPlay,
            onToggleFavorite = viewModel::onToggleFavorite,
            onAddToPlaylist = trackMenu::addToPlaylist,
            onShare = { track -> share(shareTemplate.format(track.title, track.artistNames, track.id)) },
            onMore = { track -> trackMenu.open(track) },
            onOpenAlbum = detailNavigator::openAlbum,
            onOpenArtist = detailNavigator::openArtist,
            onToggleFollow = viewModel::onToggleFollow,
            onMoreLikeThisClick = viewModel::onMoreLikeThisClick,
        ),
        modifier = modifier,
    )
}

@Composable
fun SongContent(
    uiState: SongUiState,
    actions: SongActions,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlayingMarker = NowPlayingMarker(),
) {
    val track = uiState.track
    Column(modifier.fillMaxSize()) {
        FlixTopAppBar(
            title = "",
            onBack = actions.onBack,
            actions = {
                if (track != null) {
                    IconButton(onClick = { actions.onShare(track) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_share),
                            contentDescription = stringResource(Res.string.action_share),
                            tint = FlixTheme.colors.textPrimary,
                        )
                    }
                    IconButton(onClick = { actions.onMore(track) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_more_vert),
                            contentDescription = stringResource(Res.string.action_more_options),
                            tint = FlixTheme.colors.textPrimary,
                        )
                    }
                }
            },
        )
        when {
            track != null -> SongDetails(uiState, track, actions, nowPlaying)
            uiState.error != null -> ErrorStateView(error = uiState.error, onRetry = actions.onRetry)
            else -> SongSkeleton()
        }
    }
}

@Composable
private fun SongDetails(uiState: SongUiState, track: Track, actions: SongActions, nowPlaying: NowPlayingMarker) {
    LazyColumn(contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl)) {
        item(key = "header") { SongHeader(uiState, track, actions) }
        track.album?.let { album ->
            item(key = "album") {
                SectionHeader(
                    title = stringResource(Res.string.song_from_album),
                    modifier = Modifier.padding(top = SECTION_TOP),
                )
                CollectionRow(
                    title = album.name,
                    subtitle = stringResource(Res.string.song_album_caption, track.artistNames),
                    artworkUrl = track.artworkUrl,
                    circular = false,
                    onClick = { actions.onOpenAlbum(album, track.artists.firstOrNull()) },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = FlixTheme.colors.textSecondary,
                    )
                }
            }
        }
        uiState.mainArtist?.let { artist ->
            item(key = "artist") {
                SectionHeader(
                    title = stringResource(Res.string.type_artist),
                    modifier = Modifier.padding(top = SECTION_TOP),
                )
                CollectionRow(
                    title = artist.name,
                    subtitle = stringResource(Res.string.type_artist),
                    artworkUrl = track.artworkUrl,
                    circular = true,
                    onClick = { actions.onOpenArtist(artist) },
                ) {
                    if (artist.id != null) {
                        FollowButton(isFollowing = uiState.isFollowingArtist, onClick = actions.onToggleFollow)
                    }
                }
            }
        }
        if (uiState.moreLikeThis.isNotEmpty()) {
            item(key = "more-header") {
                SectionHeader(
                    title = stringResource(Res.string.song_more_like_this),
                    modifier = Modifier.padding(top = SECTION_TOP),
                )
            }
            items(uiState.moreLikeThis, key = { it.id }) { similar ->
                TrackRow(
                    title = similar.title,
                    subtitle = similar.artistNames,
                    artworkUrl = similar.artworkUrl,
                    onClick = { actions.onMoreLikeThisClick(similar) },
                    isCurrent = similar.id == nowPlaying.trackId,
                    isPlaying = nowPlaying.isPlaying,
                    onMoreClick = { actions.onMore(similar) },
                )
            }
        }
    }
}

/** Cover, title, artist (link), "Album · 6:57" and the actions row. */
@Composable
private fun SongHeader(uiState: SongUiState, track: Track, actions: SongActions) {
    Column(Modifier.padding(horizontal = FlixTheme.spacing.gutter)) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(horizontal = FlixTheme.spacing.xxl)
                .padding(top = FlixTheme.spacing.xs)
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(8.dp, COVER_SHAPE)
                .clip(COVER_SHAPE)
                .background(FlixTheme.colors.surfaceVariant),
        )
        Text(
            text = track.title,
            style = FlixTheme.typography.headline,
            color = FlixTheme.colors.textPrimary,
            modifier = Modifier
                .padding(top = 18.dp)
                .semantics { heading() },
        )
        Text(
            text = track.artistNames,
            style = FlixTheme.typography.label,
            color = FlixTheme.colors.textPrimary,
            modifier = Modifier
                .padding(top = 6.dp)
                .handCursor().clickable(enabled = uiState.mainArtist != null) {
                    uiState.mainArtist?.let(actions.onOpenArtist)
                },
        )
        val details = listOfNotNull(track.album?.name, track.duration?.toClock()).joinToString(" · ")
        if (details.isNotEmpty()) {
            Text(
                text = details,
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Row(
            modifier = Modifier.padding(top = 14.dp, bottom = FlixTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlixPrimaryButton(
                text = stringResource(Res.string.playlist_play),
                onClick = actions.onPlay,
                icon = Res.drawable.ic_play_arrow_filled,
                enabled = uiState.isPlayable,
            )
            FavoriteButton(
                isFavorite = uiState.isFavorite,
                onToggle = actions.onToggleFavorite,
                trackTitle = track.title,
                inactiveTint = FlixTheme.colors.textPrimary,
            )
            IconButton(onClick = { actions.onAddToPlaylist(track) }) {
                Icon(
                    painter = painterResource(Res.drawable.ic_playlist_add),
                    contentDescription = stringResource(Res.string.add_to_playlist),
                    tint = FlixTheme.colors.textPrimary,
                )
            }
        }
        if (!uiState.isPlayable) {
            Text(
                text = stringResource(Res.string.song_unavailable),
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textSecondary,
            )
        }
    }
}

/** A `.row` with a 48dp cover (square or circle), title, subtitle and a trailing slot. */
@Composable
private fun CollectionRow(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    circular: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FlixTheme.spacing.rowHeight)
            .handCursor().clickable(onClick = onClick)
            .padding(start = FlixTheme.spacing.gutter, end = FlixTheme.spacing.md),
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
            Text(subtitle, style = FlixTheme.typography.bodySmall, color = FlixTheme.colors.textSecondary)
        }
        trailing()
    }
}

/** "Seguir" / "✓ Siguiendo" (design `.btn.bo.bsm`). */
@Composable
internal fun FollowButton(isFollowing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FlixOutlinedButton(
        text = stringResource(if (isFollowing) Res.string.artist_following else Res.string.artist_follow),
        onClick = onClick,
        icon = if (isFollowing) Res.drawable.ic_check else null,
        modifier = modifier,
    )
}

@Composable
private fun SongSkeleton() {
    Column(Modifier.padding(horizontal = FlixTheme.spacing.gutter)) {
        SkeletonBox(
            Modifier
                .padding(horizontal = FlixTheme.spacing.xxl)
                .padding(top = FlixTheme.spacing.xs)
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        SkeletonBox(
            Modifier
                .padding(top = 18.dp)
                .fillMaxWidth(0.7f)
                .heightIn(min = 24.dp),
        )
        SkeletonBox(
            Modifier
                .padding(top = FlixTheme.spacing.sm)
                .fillMaxWidth(0.4f)
                .heightIn(min = 14.dp),
        )
    }
}

private val COVER_SHAPE = RoundedCornerShape(16.dp)
private val SECTION_TOP = 22.dp

