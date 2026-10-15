package com.flix.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.PlayPauseButton
import com.flix.music.core.designsystem.component.SeekBar
import com.flix.music.core.designsystem.component.util.toClock
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.player.PlaybackStatus
import com.flix.music.core.player.PlayerState
import com.flix.music.core.player.RepeatMode
import com.flix.music.resources.Res
import com.flix.music.resources.ic_repeat
import com.flix.music.resources.ic_repeat_one
import com.flix.music.resources.ic_shuffle
import com.flix.music.resources.ic_skip_next_filled
import com.flix.music.resources.ic_skip_previous_filled
import com.flix.music.resources.ic_volume_up
import com.flix.music.resources.player_next
import com.flix.music.resources.player_previous
import com.flix.music.resources.player_repeat
import com.flix.music.resources.player_seek
import com.flix.music.resources.player_shuffle
import com.flix.music.resources.player_volume
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Duration

/** Every action of the player bar. Defaults do nothing, which keeps snapshots short. */
data class PlayerBarActions(
    val onPlayPause: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onSeek: (Duration) -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onCycleRepeat: () -> Unit = {},
    val onVolumeChange: (Int) -> Unit = {},
    val onOpenPlayer: () -> Unit = {},
)

/**
 * The desktop's player, always at the bottom while something is queued (the phone's
 * mini player and full player controls in one bar): the song on the left, controls and
 * progress in the middle, volume on the right. Lumen marks play and the active modes.
 */
@Composable
fun PlayerBar(state: PlayerState, volume: Int, actions: PlayerBarActions, modifier: Modifier = Modifier) {
    val track = state.current ?: return
    val colors = FlixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .background(colors.surfaceElevated)
            .padding(horizontal = FlixTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(SIDE_WEIGHT)
                .clip(FlixTheme.shapes.md)
                .clickable(onClick = actions.onOpenPlayer)
                .padding(FlixTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ARTWORK_SIZE)
                    .clip(FlixTheme.shapes.sm)
                    .background(colors.surfaceVariant),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = FlixTheme.typography.subtitle,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artistNames,
                    style = FlixTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(CENTER_WEIGHT)
                .padding(top = CONTROLS_TOP_OFFSET),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
            ) {
                ModeButton(
                    icon = Res.drawable.ic_shuffle,
                    label = stringResource(Res.string.player_shuffle),
                    isActive = state.shuffleEnabled,
                    onClick = actions.onToggleShuffle,
                )
                BarIconButton(Res.drawable.ic_skip_previous_filled, stringResource(Res.string.player_previous), actions.onPrevious)
                val playPause = playPauseAction(state.status, isMiniPlayer = false)
                PlayPauseButton(
                    icon = playPause.icon,
                    contentDescription = stringResource(playPause.label),
                    onClick = actions.onPlayPause,
                    size = PLAY_BUTTON_SIZE,
                    iconSize = PLAY_ICON_SIZE,
                    showLoadingRing = state.status == PlaybackStatus.Loading,
                )
                BarIconButton(
                    icon = Res.drawable.ic_skip_next_filled,
                    label = stringResource(Res.string.player_next),
                    onClick = actions.onNext,
                    enabled = state.hasNext,
                )
                ModeButton(
                    icon = if (state.repeatMode == RepeatMode.One) Res.drawable.ic_repeat_one else Res.drawable.ic_repeat,
                    label = "${stringResource(Res.string.player_repeat)}: ${stringResource(state.repeatMode.label())}",
                    isActive = state.repeatMode != RepeatMode.Off,
                    onClick = actions.onCycleRepeat,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
            ) {
                TimeLabel(state.position)
                SeekBar(
                    position = state.position,
                    bufferedPosition = state.bufferedPosition,
                    duration = state.duration,
                    onSeek = actions.onSeek,
                    contentDescription = stringResource(Res.string.player_seek),
                    bufferPulsing = state.status == PlaybackStatus.Buffering,
                    modifier = Modifier
                        .weight(1f)
                        .height(SEEK_ROW_HEIGHT),
                )
                TimeLabel(state.duration ?: Duration.ZERO)
            }
        }

        Row(
            modifier = Modifier.weight(SIDE_WEIGHT),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_volume_up),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(ICON_SIZE),
            )
            VolumeSlider(
                volume = volume,
                onVolumeChange = actions.onVolumeChange,
                contentDescription = stringResource(Res.string.player_volume),
                modifier = Modifier
                    .padding(start = FlixTheme.spacing.sm)
                    .width(VOLUME_WIDTH),
            )
        }
    }
}

/** A thin volume bar in the seek bar's style: textPrimary fill on surfaceVariant, a small round thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSlider(volume: Int, onVolumeChange: (Int) -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val colors = FlixTheme.colors
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.textPrimary,
        activeTrackColor = colors.textPrimary,
        inactiveTrackColor = colors.surfaceVariant,
    )
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = volume.toFloat(),
        onValueChange = { value -> onVolumeChange(value.roundToInt()) },
        valueRange = 0f..MAX_VOLUME,
        interactionSource = interactionSource,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        thumb = {
            Box(
                Modifier
                    .size(VOLUME_THUMB_SIZE)
                    .background(colors.textPrimary, CircleShape),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = sliderColors,
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null,
                modifier = Modifier.height(VOLUME_TRACK_HEIGHT),
            )
        },
    )
}

@Composable
private fun TimeLabel(time: Duration) {
    Text(
        text = time.toClock(),
        style = FlixTheme.typography.caption,
        color = FlixTheme.colors.textTertiary,
        modifier = Modifier.width(TIME_WIDTH),
    )
}

@Composable
private fun BarIconButton(icon: DrawableResource, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (enabled) FlixTheme.colors.textPrimary else FlixTheme.colors.textTertiary,
        )
    }
}

/** Shuffle and repeat: Lumen when on, as the phone's player marks active modes. */
@Composable
private fun ModeButton(icon: DrawableResource, label: String, isActive: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (isActive) FlixTheme.colors.primaryContent else FlixTheme.colors.textSecondary,
            modifier = Modifier.size(ICON_SIZE),
        )
    }
}

private val BAR_HEIGHT = 92.dp
private val SEEK_ROW_HEIGHT = 28.dp

/** The play button's touch area pads it on top and the seek line sits low: this evens both edges. */
private val CONTROLS_TOP_OFFSET = 8.dp
private val ARTWORK_SIZE = 56.dp
private val PLAY_BUTTON_SIZE = 40.dp
private val PLAY_ICON_SIZE = 24.dp
private val ICON_SIZE = 20.dp
private val TIME_WIDTH = 44.dp
private val VOLUME_WIDTH = 120.dp
private val VOLUME_THUMB_SIZE = 12.dp
private val VOLUME_TRACK_HEIGHT = 4.dp
private const val SIDE_WEIGHT = 1f
private const val CENTER_WEIGHT = 1.4f
private const val MAX_VOLUME = 100f
