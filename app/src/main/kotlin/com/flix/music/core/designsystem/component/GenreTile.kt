package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme

/**
 * Genre tile from the design (`.gtile`): 135° gradient, white Bricolage title and a
 * translucent square rotated 22° peeking from the bottom-right corner.
 */
@Composable
fun GenreTile(
    name: String,
    gradientStart: Color,
    gradientEnd: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = GENRE_TILE_HEIGHT,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(FlixTheme.shapes.md)
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .drawBehind {
                val squareSize = DECORATION_SIZE.toPx()
                val topLeft = Offset(
                    x = size.width - squareSize + DECORATION_OVERFLOW_X.toPx(),
                    y = size.height - squareSize + DECORATION_OVERFLOW_Y.toPx(),
                )
                rotate(
                    degrees = DECORATION_ROTATION,
                    pivot = topLeft + Offset(squareSize / 2, squareSize / 2),
                ) {
                    drawRoundRect(
                        color = DecorationColor,
                        topLeft = topLeft,
                        size = Size(squareSize, squareSize),
                        cornerRadius = CornerRadius(DECORATION_CORNER.toPx()),
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = FlixTheme.spacing.lg, vertical = FlixTheme.spacing.md),
    ) {
        Text(
            text = name,
            style = FlixTheme.typography.title.copy(fontWeight = FontWeight.Bold),
            color = TileContentColor,
        )
    }
}

/** Default tile height (`.gtile`: 78 px in the 280 px-wide mockup). */
val GENRE_TILE_HEIGHT = 100.dp

/** Compact tile height used in the Search grid (`gtile(g, 64)`). */
val GENRE_TILE_COMPACT_HEIGHT = 82.dp

private val TileContentColor = Color.White
private val DecorationColor = Color.White.copy(alpha = 0.14f)
private val DECORATION_SIZE = 90.dp
private val DECORATION_OVERFLOW_X = 18.dp
private val DECORATION_OVERFLOW_Y = 23.dp
private val DECORATION_CORNER = 12.dp
private const val DECORATION_ROTATION = 22f
