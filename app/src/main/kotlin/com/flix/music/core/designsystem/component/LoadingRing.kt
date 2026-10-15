package com.flix.music.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme

/**
 * Spinning ring around a play button (design `.ring`): a 2 dp violet quarter arc
 * drawn 4 dp outside the button, one turn per second.
 */
@Composable
fun LoadingRing(modifier: Modifier = Modifier) {
    val rotation by rememberInfiniteTransition(label = "loadingRing").animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN_DEGREES,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TURN_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loadingRingRotation",
    )
    val ringColor = FlixTheme.colors.primary
    Canvas(modifier) {
        val strokeWidth = RING_STROKE.toPx()
        val outset = RING_OUTSET.toPx() - strokeWidth / 2
        drawArc(
            color = ringColor,
            startAngle = rotation,
            sweepAngle = ARC_SWEEP_DEGREES,
            useCenter = false,
            topLeft = Offset(-outset, -outset),
            size = Size(size.width + outset * 2, size.height + outset * 2),
            style = Stroke(width = strokeWidth),
        )
    }
}

private val RING_STROKE = 2.dp
private val RING_OUTSET = 4.dp
private const val TURN_DURATION_MS = 1000
private const val FULL_TURN_DEGREES = 360f
private const val ARC_SWEEP_DEGREES = 90f
