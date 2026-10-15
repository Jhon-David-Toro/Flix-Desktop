package com.flix.music.core.designsystem.component

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import kotlin.time.Duration

/**
 * The player's progress "beam" (design `.beam`): 3 dp surfaceVariant track, buffered
 * segment in textTertiary at 45 %, violet fill and a 12 dp knob with a 4 dp violet halo.
 * The buffered segment pulses (0.45 ↔ 0.15, 1.2 s) while [bufferPulsing].
 * Tap or drag to seek; TalkBack users can set the progress directly.
 */
@Composable
fun SeekBar(
    position: Duration,
    bufferedPosition: Duration,
    duration: Duration?,
    onSeek: (Duration) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    bufferPulsing: Boolean = false,
) {
    val enabled = duration != null && duration.isPositive()
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val playedFraction = dragFraction ?: fractionOf(position, duration)
    val bufferedFraction = fractionOf(bufferedPosition, duration)

    val pulsingAlpha by rememberInfiniteTransition(label = "bufferPulse").animateFloat(
        initialValue = BUFFER_ALPHA,
        targetValue = BUFFER_PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(PULSE_HALF_CYCLE_MS, easing = EaseInOut), RepeatMode.Reverse),
        label = "bufferPulseAlpha",
    )
    val bufferAlpha = if (bufferPulsing) pulsingAlpha else BUFFER_ALPHA

    val trackColor = FlixTheme.colors.surfaceVariant
    val bufferColor = FlixTheme.colors.textTertiary
    val fillColor = FlixTheme.colors.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(TOUCH_HEIGHT)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(playedFraction, 0f..1f)
                if (enabled) {
                    setProgress { targetFraction ->
                        onSeek(duration * targetFraction.toDouble())
                        true
                    }
                }
            }
            .pointerInput(enabled, duration) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onSeek(duration * (offset.x / size.width).coerceIn(0f, 1f).toDouble())
                }
            }
            .pointerInput(enabled, duration) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragFraction?.let { fraction -> onSeek(duration * fraction.toDouble()) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                )
            },
    ) {
        val trackHeight = TRACK_HEIGHT.toPx()
        val trackTop = (size.height - trackHeight) / 2
        val corner = CornerRadius(trackHeight / 2)
        fun segment(widthFraction: Float) = Size(size.width * widthFraction, trackHeight)

        drawRoundRect(trackColor, Offset(0f, trackTop), segment(1f), corner)
        drawRoundRect(bufferColor.copy(alpha = bufferAlpha), Offset(0f, trackTop), segment(bufferedFraction), corner)
        drawRoundRect(fillColor, Offset(0f, trackTop), segment(playedFraction), corner)

        if (enabled) {
            val knobCenter = Offset(size.width * playedFraction, size.height / 2)
            drawCircle(fillColor.copy(alpha = KNOB_HALO_ALPHA), KNOB_RADIUS.toPx() + KNOB_HALO.toPx(), knobCenter)
            drawCircle(fillColor, KNOB_RADIUS.toPx(), knobCenter)
        }
    }
}

private fun fractionOf(position: Duration, duration: Duration?): Float =
    if (duration == null || !duration.isPositive()) 0f
    else (position / duration).toFloat().coerceIn(0f, 1f)

/** Minimum touch target from the design's accessibility rules; the track is drawn centered. */
private val TOUCH_HEIGHT = 48.dp
private val TRACK_HEIGHT = 3.dp
private val KNOB_RADIUS = 6.dp
private val KNOB_HALO = 4.dp
private const val KNOB_HALO_ALPHA = 0.22f
private const val BUFFER_ALPHA = 0.45f
private const val BUFFER_PULSE_MIN_ALPHA = 0.15f
private const val PULSE_HALF_CYCLE_MS = 600
