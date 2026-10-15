package com.flix.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.FavoriteButton
import com.flix.music.core.designsystem.component.FlixSegmentedControl
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.Segment
import com.flix.music.core.designsystem.component.util.rememberArtworkTint
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.player.PlayerState
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.ic_keyboard_arrow_down
import com.flix.music.resources.lyrics_title
import com.flix.music.resources.player_close
import com.flix.music.resources.player_playing_from
import com.flix.music.resources.queue_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The two panels next to the artwork. */
enum class NowPlayingPanel { Lyrics, Queue }

/**
 * The desktop's full player ("Ahora suena"), over the content area while the player bar
 * stays below with the controls: the artwork-tinted gradient, the artwork big on the
 * left with the song, and Letra or Cola on the right. Esc or the arrow closes it.
 */
@Composable
fun NowPlayingView(
    state: PlayerState,
    queueActions: QueueActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = flixViewModel(),
) {
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    NowPlayingContent(
        state = state,
        isFavorite = isFavorite,
        onToggleFavorite = viewModel::onToggleFavorite,
        onClose = onClose,
        lyrics = { LyricsPanel(position = state.position) },
        queue = { QueuePanel(state = state, actions = queueActions) },
        modifier = modifier,
    )
}

@Composable
fun NowPlayingContent(
    state: PlayerState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    lyrics: @Composable () -> Unit,
    queue: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.current ?: return
    var panel by rememberSaveable { mutableStateOf(NowPlayingPanel.Lyrics) }
    val tint = rememberArtworkTint(track.artworkUrl)
    val background = FlixTheme.colors.background
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .background(Brush.verticalGradient(0f to tint, GRADIENT_END to background))
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                (event.type == KeyEventType.KeyDown && event.key == Key.Escape).also { isEscape -> if (isEscape) onClose() }
            },
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.padding(FlixTheme.spacing.sm),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = stringResource(Res.string.player_close),
                tint = FlixTheme.colors.textPrimary,
                modifier = Modifier.size(CLOSE_ICON_SIZE),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = SIDE_PADDING, end = SIDE_PADDING, top = TOP_PADDING),
            horizontalArrangement = Arrangement.spacedBy(PANEL_GAP),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val artworkSize = minOf(maxWidth, maxHeight - SONG_INFO_HEIGHT).coerceIn(MIN_ARTWORK, MAX_ARTWORK)
                Column(Modifier.widthIn(max = artworkSize)) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(artworkSize)
                            .aspectRatio(1f)
                            .shadow(ARTWORK_ELEVATION, FlixTheme.shapes.lg)
                            .clip(FlixTheme.shapes.lg)
                            .background(FlixTheme.colors.surfaceVariant),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = FlixTheme.spacing.xl),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = FlixTheme.typography.headline,
                                color = FlixTheme.colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artistNames,
                                style = FlixTheme.typography.subtitle,
                                color = FlixTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = FlixTheme.spacing.xs),
                            )
                        }
                        FavoriteButton(isFavorite = isFavorite, onToggle = onToggleFavorite, trackTitle = track.title)
                    }
                    state.source?.let { source ->
                        Text(
                            text = "${stringResource(Res.string.player_playing_from)} · ${playbackSourceLabel(source)}",
                            style = FlixTheme.typography.caption,
                            color = FlixTheme.colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = FlixTheme.spacing.md),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                FlixSegmentedControl(
                    segments = listOf(
                        Segment(NowPlayingPanel.Lyrics, stringResource(Res.string.lyrics_title)),
                        Segment(NowPlayingPanel.Queue, stringResource(Res.string.queue_title)),
                    ),
                    selected = panel,
                    onSelect = { selected -> panel = selected },
                    modifier = Modifier.widthIn(max = TABS_MAX_WIDTH),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = FlixTheme.spacing.md),
                ) {
                    when (panel) {
                        NowPlayingPanel.Lyrics -> lyrics()
                        NowPlayingPanel.Queue -> queue()
                    }
                }
            }
        }
    }
}

private val CLOSE_ICON_SIZE = 28.dp
private val SIDE_PADDING = 56.dp
private val TOP_PADDING = 56.dp
private val PANEL_GAP = 48.dp
private val SONG_INFO_HEIGHT = 140.dp
private val MIN_ARTWORK = 200.dp
private val MAX_ARTWORK = 460.dp
private val ARTWORK_ELEVATION = 24.dp
private val TABS_MAX_WIDTH = 280.dp
private const val GRADIENT_END = 0.85f
