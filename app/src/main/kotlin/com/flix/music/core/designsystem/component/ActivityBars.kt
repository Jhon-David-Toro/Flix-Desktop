package com.flix.music.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flix.music.core.designsystem.theme.FlixTheme

/**
 * Vertical bars of listening time (design `bars()`): one slot per day or month, a bar
 * of about half the slot with 4dp radius and at least 4dp tall, the highlighted bar in
 * Lumen and the rest neutral, with small tertiary labels underneath. Screen readers
 * get [description] instead of the drawing.
 */
@Composable
fun ActivityBars(
    values: List<Float>,
    labels: List<String>,
    description: String,
    modifier: Modifier = Modifier,
    highlightedIndex: Int? = null,
    chartHeight: Dp = 70.dp,
) {
    val highlightColor = FlixTheme.colors.primary
    val barColor = FlixTheme.colors.surfaceVariant
    val maxValue = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    Column(modifier.semantics { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            if (values.isEmpty()) return@Canvas
            val slotWidth = size.width / values.size
            val barWidth = slotWidth * BAR_TO_SLOT
            val minHeight = MIN_BAR_HEIGHT.toPx()
            val radius = CornerRadius(BAR_RADIUS.toPx())
            values.forEachIndexed { index, value ->
                val barHeight = maxOf(minHeight, value / maxValue * size.height)
                drawRoundRect(
                    color = if (index == highlightedIndex) highlightColor else barColor,
                    topLeft = Offset(index * slotWidth + (slotWidth - barWidth) / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
        }
        Row(Modifier.padding(top = 4.dp)) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = FlixTheme.typography.caption.copy(fontSize = 9.sp),
                    color = FlixTheme.colors.textTertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private const val BAR_TO_SLOT = 16f / 30f
private val MIN_BAR_HEIGHT = 4.dp
private val BAR_RADIUS = 4.dp
