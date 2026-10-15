package com.flix.music.core.designsystem.component

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme

private const val SHIMMER_DURATION_MS = 1400

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = FlixTheme.shapes.xs,
) {
    val progress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = EaseInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val base = FlixTheme.colors.surfaceVariant
    val highlight = FlixTheme.colors.textPrimary.copy(alpha = 0.07f)

    Box(
        modifier
            .clip(shape)
            .drawBehind {
                drawRect(base)
                val bandStartX = -size.width + progress * 2 * size.width
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        startX = bandStartX,
                        endX = bandStartX + size.width,
                    )
                )
            }
    )
}

@Composable
fun TrackRowSkeleton(
    modifier: Modifier = Modifier,
    titleWidth: Float = 0.7f,
    subtitleWidth: Float = 0.45f,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FlixTheme.spacing.rowHeight)
            .padding(horizontal = FlixTheme.spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        SkeletonBox(Modifier.size(48.dp), shape = FlixTheme.shapes.sm)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
        ) {
            SkeletonBox(Modifier.fillMaxWidth(titleWidth).height(12.dp))
            SkeletonBox(Modifier.fillMaxWidth(subtitleWidth).height(10.dp))
        }
    }
}

@Composable
fun BestResultCardSkeleton(modifier: Modifier = Modifier) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FlixTheme.spacing.gutter)
            .height(FlixTheme.spacing.lg * 2 + BEST_RESULT_ARTWORK),
        shape = FlixTheme.shapes.lg,
    )
}