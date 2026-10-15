package com.flix.music.core.designsystem.component

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme

/**
 * Three-bar "now playing" indicator (design `.eq`). Bars bounce between 35 % and
 * 100 % of their height; when [animated] is false they freeze (paused).
 */
@Composable
fun NowPlayingEqualizer(
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val barColor = FlixTheme.colors.primaryContent
    val transition = rememberInfiniteTransition(label = "equalizer")
    Row(
        modifier = modifier.size(width = EQUALIZER_WIDTH, height = EQUALIZER_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
        verticalAlignment = Alignment.Bottom,
    ) {
        BARS.forEach { bar ->
            val animatedScale by transition.animateFloat(
                initialValue = MIN_BAR_SCALE,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = HALF_CYCLE_MS, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(bar.phaseMillis, StartOffsetType.FastForward),
                ),
                label = "equalizerBar",
            )
            Box(
                Modifier
                    .width(BAR_WIDTH)
                    .fillMaxHeight(bar.heightFraction)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        scaleY = if (animated) animatedScale else 1f
                    }
                    .background(barColor, RoundedCornerShape(1.dp)),
            )
        }
    }
}

private data class EqualizerBar(val heightFraction: Float, val phaseMillis: Int)

private val BARS = listOf(
    EqualizerBar(heightFraction = 0.6f, phaseMillis = 200),
    EqualizerBar(heightFraction = 1f, phaseMillis = 500),
    EqualizerBar(heightFraction = 0.4f, phaseMillis = 800),
)
private val EQUALIZER_WIDTH = 18.dp
private val EQUALIZER_HEIGHT = 16.dp
private val BAR_WIDTH = 4.dp
private val BAR_GAP = 3.dp
private const val MIN_BAR_SCALE = 0.35f
private const val HALF_CYCLE_MS = 500
