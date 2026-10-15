package com.flix.music.core.designsystem.component

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme

@Composable
fun LoadingHairline(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().height(2.dp)) {
        if (visible) {
            val alpha by rememberInfiniteTransition(label = "hairline").animateFloat(
                initialValue = 0.45f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "hairlineAlpha",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(FlixTheme.colors.primary, RoundedCornerShape(3.dp)),
            )
        }
    }
}
