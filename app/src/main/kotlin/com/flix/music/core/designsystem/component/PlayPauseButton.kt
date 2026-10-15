package com.flix.music.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixMotion
import com.flix.music.core.designsystem.theme.FlixTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Violet play button (design `.play`, 68 dp in the player). The icon morphs with a
 * fade + scale 0.8 → 1 (fast) and the button never changes size. [showLoadingRing]
 * draws the `.ring` around it.
 */
@Composable
fun PlayPauseButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = PLAYER_PLAY_BUTTON_SIZE,
    iconSize: Dp = PLAYER_PLAY_ICON_SIZE,
    showLoadingRing: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (showLoadingRing) {
            LoadingRing(Modifier.matchParentSize())
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(FlixTheme.shapes.circle)
                .background(FlixTheme.colors.primary)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedIcon(icon = icon, tint = FlixTheme.colors.onPrimary, iconSize = iconSize)
        }
    }
}

/** Icon that morphs between drawables like the design's play/pause (fade + scale, fast). */
@Composable
fun AnimatedIcon(
    icon: DrawableResource,
    tint: Color,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = icon,
        transitionSpec = {
            (fadeIn(tween(FlixMotion.FAST)) + scaleIn(tween(FlixMotion.FAST), initialScale = ICON_ENTER_SCALE))
                .togetherWith(fadeOut(tween(FlixMotion.FAST)))
        },
        modifier = modifier,
        label = "animatedIcon",
    ) { iconRes ->
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

val PLAYER_PLAY_BUTTON_SIZE = 68.dp
val PLAYER_PLAY_ICON_SIZE = 36.dp
private const val ICON_ENTER_SCALE = 0.8f
private const val DISABLED_ALPHA = 0.38f
