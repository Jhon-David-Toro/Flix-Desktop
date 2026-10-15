package com.flix.music.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.ic_more_vert
import com.flix.music.resources.now_paused
import com.flix.music.resources.now_playing
import com.flix.music.resources.track_more_options
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Song row. When [isCurrent] it follows the design's "now playing" row (`.row.on`):
 * title in primaryContent and the equalizer in place of the duration, animated only
 * while [isPlaying]. The state is also announced to screen readers.
 * [trailing] replaces the duration/equalizer slot (e.g. the ♥ in Favoritos or the
 * time in Historial).
 * [onMoreClick] adds the design's default ⋯ button (the duration is then left out, as
 * in the mockups) and long press opens the same menu; [onLongClick] only adds the long
 * press, for rows whose right slot is taken.
 * [number] replaces the artwork with the track number (`.row .num`, album tracks and
 * an artist's Populares); the current row then shows the equalizer in its place.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    duration: String? = null,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    number: Int? = null,
    subtitleColor: Color = FlixTheme.colors.textSecondary,
) {
    val haptics = LocalHapticFeedback.current
    val openMenu = onMoreClick ?: onLongClick
    val playbackStateLabel = when {
        !isCurrent -> null
        isPlaying -> stringResource(Res.string.now_playing)
        else -> stringResource(Res.string.now_paused)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FlixTheme.spacing.rowHeight)
            .handCursor().combinedClickable(
                onClick = onClick,
                onLongClick = openMenu?.let { menu ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu()
                    }
                },
            )
            .semantics { if (playbackStateLabel != null) stateDescription = playbackStateLabel }
            .padding(
                start = FlixTheme.spacing.gutter,
                end = if (onMoreClick != null) FlixTheme.spacing.md else FlixTheme.spacing.gutter,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        if (number != null) {
            Box(Modifier.width(NUMBER_WIDTH), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    NowPlayingEqualizer(animated = isPlaying)
                } else {
                    Text(
                        text = number.toString(),
                        style = FlixTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
                        color = FlixTheme.colors.textTertiary,
                    )
                }
            }
        } else {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(FlixTheme.shapes.sm)
                    .background(FlixTheme.colors.surfaceVariant),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = FlixTheme.typography.subtitle,
                color = if (isCurrent) FlixTheme.colors.primaryContent else FlixTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = FlixTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            trailing != null -> trailing()
            isCurrent && number == null -> NowPlayingEqualizer(animated = isPlaying)
            onMoreClick == null && duration != null -> Text(
                text = duration,
                style = FlixTheme.typography.caption,
                color = FlixTheme.colors.textTertiary,
            )
        }
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    painter = painterResource(Res.drawable.ic_more_vert),
                    contentDescription = stringResource(Res.string.track_more_options, title),
                    tint = FlixTheme.colors.textSecondary,
                )
            }
        }
    }
}

private val NUMBER_WIDTH = 20.dp

/** Which track is playing, so lists can mark its row. */
data class NowPlayingMarker(
    val trackId: String? = null,
    val isPlaying: Boolean = false,
)
