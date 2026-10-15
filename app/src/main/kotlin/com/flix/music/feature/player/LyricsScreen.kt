package com.flix.music.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.ErrorStateView
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.PlayPauseButton
import com.flix.music.core.designsystem.component.SeekBar
import com.flix.music.core.designsystem.component.SkeletonBox
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.handCursor
import com.flix.music.core.designsystem.component.util.rememberArtworkTint
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Lyrics
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlaybackStatus
import com.flix.music.core.player.PlayerState
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.ic_keyboard_arrow_down
import com.flix.music.resources.ic_lyrics
import com.flix.music.resources.ic_skip_next_filled
import com.flix.music.resources.ic_skip_previous_filled
import com.flix.music.resources.lyrics_close
import com.flix.music.resources.lyrics_unavailable_body
import com.flix.music.resources.lyrics_unavailable_title
import com.flix.music.resources.player_next
import com.flix.music.resources.player_previous
import com.flix.music.resources.player_seek
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Letra (design `lyricsnone` / `lyricssync`) as a panel of the desktop Now Playing view:
 * synced lyrics light up the line being sung, scroll by themselves and seek when a
 * line is clicked; plain lyrics scroll freely. The controls live in the player bar.
 */
@Composable
fun LyricsPanel(position: Duration, modifier: Modifier = Modifier, viewModel: LyricsViewModel = flixViewModel()) {
    val lyricsState by viewModel.uiState.collectAsStateWithLifecycle()
    LyricsPanelContent(
        lyricsState = lyricsState,
        position = position,
        onLineClick = viewModel::onLineClick,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

@Composable
fun LyricsPanelContent(
    lyricsState: LyricsUiState,
    position: Duration,
    onLineClick: (Duration) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (lyricsState) {
            LyricsUiState.Loading -> LyricsSkeleton()
            LyricsUiState.Unavailable -> StateView(
                icon = Res.drawable.ic_lyrics,
                title = stringResource(Res.string.lyrics_unavailable_title),
                body = stringResource(Res.string.lyrics_unavailable_body),
            )
            is LyricsUiState.Failed -> ErrorStateView(error = lyricsState.error, onRetry = onRetry)
            is LyricsUiState.Ready -> LyricsLines(lyricsState.lyrics, position, onLineClick)
        }
    }
}

/**
 * Lyric lines in Bricolage 21sp bold. Synced: the current line in textPrimary, past
 * lines in textTertiary and upcoming ones fading out; the current line is kept about
 * a third from the top.
 */
@Composable
private fun LyricsLines(lyrics: Lyrics, position: Duration, onLineClick: (Duration) -> Unit) {
    val activeIndex = lyrics.activeLineIndex(position)
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex != null) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -listState.layoutInfo.viewportSize.height / 3)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = FlixTheme.spacing.gutter, vertical = 20.dp),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val emphasis = lineEmphasis(index, activeIndex, lyrics.isSynced)
            val color by animateColorAsState(emphasis.color(), label = "lyricColor")
            Text(
                text = line.text,
                style = LYRIC_STYLE,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(emphasis.alpha)
                    .then(
                        if (line.start != null) {
                            Modifier.handCursor().clickable { onLineClick(line.start) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(bottom = 14.dp),
            )
        }
    }
}

/** How a lyric line stands out relative to the one being sung. */
private enum class LineEmphasis(val alpha: Float) {
    Current(1f),
    Past(1f),
    Next(NEXT_LINE_ALPHA),
    Later(LATER_LINES_ALPHA),
    Plain(1f),
}

private fun lineEmphasis(index: Int, activeIndex: Int?, isSynced: Boolean): LineEmphasis = when {
    !isSynced -> LineEmphasis.Plain
    activeIndex == null -> if (index == 0) LineEmphasis.Next else LineEmphasis.Later
    index == activeIndex -> LineEmphasis.Current
    index < activeIndex -> LineEmphasis.Past
    index == activeIndex + 1 -> LineEmphasis.Next
    else -> LineEmphasis.Later
}

@Composable
private fun LineEmphasis.color(): Color = when (this) {
    LineEmphasis.Current, LineEmphasis.Plain -> FlixTheme.colors.textPrimary
    LineEmphasis.Past -> FlixTheme.colors.textTertiary
    LineEmphasis.Next, LineEmphasis.Later -> FlixTheme.colors.textSecondary
}

/** Four placeholder lines at 80 / 60 / 70 / 40 % (design: loading lyrics). */
@Composable
private fun LyricsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FlixTheme.spacing.gutter, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SKELETON_LINE_WIDTHS.forEach { width ->
            SkeletonBox(
                Modifier
                    .fillMaxWidth(width)
                    .height(22.dp),
            )
        }
    }
}

private val LYRIC_STYLE
    @Composable get() = FlixTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp)
private val SKELETON_LINE_WIDTHS = listOf(0.8f, 0.6f, 0.7f, 0.4f)
private const val NEXT_LINE_ALPHA = 0.55f
private const val LATER_LINES_ALPHA = 0.4f
private const val GRADIENT_END = 0.78f
private val CLOSE_DRAG_THRESHOLD = 120.dp


