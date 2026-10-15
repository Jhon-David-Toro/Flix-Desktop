package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme

private val DarkTopHighlight = Color(0x0AFFFFFF)
private val LightShadow = Color(0x0F101218)
private val LightBorder = Color(0x0A101218)

@Composable
fun FlixCard(
    modifier: Modifier = Modifier,
    shape: Shape = FlixTheme.shapes.lg,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isDark = FlixTheme.isDark
    val surfaceColor = FlixTheme.colors.surface

    val elevationModifier = if (isDark) {
        Modifier
    } else {
        Modifier
            .shadow(elevation = 1.dp, shape = shape, ambientColor = LightShadow, spotColor = LightShadow)
            .border(Dp.Hairline, LightBorder, shape)
    }

    Box(
        modifier = modifier
            .then(elevationModifier)
            .clip(shape)
            .background(surfaceColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawWithContent {
                drawContent()
                if (isDark) {
                    drawLine(
                        color = DarkTopHighlight,
                        start = Offset(0f, 0.5f),
                        end = Offset(size.width, 0.5f),
                        strokeWidth = 1f,
                    )
                }
            },
    ) {
        content()
    }
}
