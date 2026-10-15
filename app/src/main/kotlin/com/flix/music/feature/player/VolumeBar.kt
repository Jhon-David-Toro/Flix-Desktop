package com.flix.music.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.component.handCursor
import com.flix.music.core.designsystem.theme.FlixTheme
import kotlin.math.roundToInt

/**
 * The player bar's volume: a thin textPrimary bar on surfaceVariant with a round
 * thumb, in the seek bar's style. The bar is inset by the thumb's radius at both ends,
 * so the thumb stays inside at 0 % and 100 %. Click or drag to set it; ← → step 5 %.
 */
@Composable
fun VolumeBar(volume: Int, onVolumeChange: (Int) -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val colors = FlixTheme.colors
    val currentOnVolumeChange by rememberUpdatedState(onVolumeChange)
    val currentVolume by rememberUpdatedState(volume)

    fun volumeAt(x: Float, width: Int, thumbRadius: Float): Int {
        val usable = (width - 2 * thumbRadius).coerceAtLeast(1f)
        return (((x - thumbRadius) / usable) * MAX_VOLUME).roundToInt().coerceIn(0, MAX_VOLUME)
    }

    Canvas(
        modifier = modifier
            .height(TOUCH_HEIGHT)
            .handCursor()
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(currentVolume.toFloat(), 0f..MAX_VOLUME.toFloat(), steps = MAX_VOLUME - 1)
                setProgress { target ->
                    currentOnVolumeChange(target.roundToInt().coerceIn(0, MAX_VOLUME))
                    true
                }
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = when (event.key) {
                    Key.DirectionRight, Key.DirectionUp -> KEY_STEP
                    Key.DirectionLeft, Key.DirectionDown -> -KEY_STEP
                    else -> return@onKeyEvent false
                }
                currentOnVolumeChange((currentVolume + step).coerceIn(0, MAX_VOLUME))
                true
            }
            .focusable()
            .pointerInput(Unit) {
                val thumbRadius = THUMB_RADIUS.toPx()
                detectTapGestures { offset -> currentOnVolumeChange(volumeAt(offset.x, size.width, thumbRadius)) }
            }
            .pointerInput(Unit) {
                val thumbRadius = THUMB_RADIUS.toPx()
                detectHorizontalDragGestures { change, _ ->
                    currentOnVolumeChange(volumeAt(change.position.x, size.width, thumbRadius))
                }
            },
    ) {
        val thumbRadius = THUMB_RADIUS.toPx()
        val trackHeight = TRACK_HEIGHT.toPx()
        val trackStart = thumbRadius
        val trackWidth = size.width - 2 * thumbRadius
        val centerY = size.height / 2
        val trackTop = centerY - trackHeight / 2
        val corner = CornerRadius(trackHeight / 2)
        val thumbX = trackStart + trackWidth * (volume / MAX_VOLUME.toFloat())
        drawRoundRect(colors.surfaceVariant, Offset(trackStart, trackTop), Size(trackWidth, trackHeight), corner)
        drawRoundRect(colors.textPrimary, Offset(trackStart, trackTop), Size(thumbX - trackStart, trackHeight), corner)
        drawCircle(colors.textPrimary, radius = thumbRadius, center = Offset(thumbX, centerY))
    }
}

private val TOUCH_HEIGHT = 24.dp
private val TRACK_HEIGHT = 4.dp
private val THUMB_RADIUS = 6.dp
private const val MAX_VOLUME = 100
private const val KEY_STEP = 5
