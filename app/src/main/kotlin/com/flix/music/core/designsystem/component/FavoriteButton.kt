package com.flix.music.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.favorite_add
import com.flix.music.resources.favorite_remove
import com.flix.music.resources.ic_favorite
import com.flix.music.resources.ic_favorite_filled
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Favorite toggle (design Microinteractions → "Favorito"): outline → Lumen fill,
 * scale 1 → 1.2 → 1 with a fast spring, confirm haptic, no particles. Filled vs
 * outline keeps the state readable without color.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    trackTitle: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = FAVORITE_ICON_SIZE,
    inactiveTint: Color = FlixTheme.colors.textSecondary,
) {
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    var previousIsFavorite by remember { mutableStateOf(isFavorite) }

    LaunchedEffect(isFavorite) {
        if (isFavorite && !previousIsFavorite) {
            scale.animateTo(POP_SCALE, spring(stiffness = Spring.StiffnessHigh))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
        }
        previousIsFavorite = isFavorite
    }

    val label = stringResource(
        if (isFavorite) Res.string.favorite_remove else Res.string.favorite_add,
        trackTitle,
    )
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onToggle()
        },
        modifier = modifier.semantics { contentDescription = label },
    ) {
        Icon(
            painter = painterResource(if (isFavorite) Res.drawable.ic_favorite_filled else Res.drawable.ic_favorite),
            contentDescription = null,
            tint = if (isFavorite) FlixTheme.colors.primaryContent else inactiveTint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

private val FAVORITE_ICON_SIZE = 24.dp
private const val POP_SCALE = 1.2f
