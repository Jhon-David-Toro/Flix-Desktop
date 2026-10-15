package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.flix.music.resources.Res
import com.flix.music.resources.ic_favorite_filled
import org.jetbrains.compose.resources.painterResource

/**
 * Artwork of the Favorites collection (design `ART.fav`): 150° violet gradient
 * #2E1065 → #7C3AED (65 %) → #C4B5FD with a white filled heart.
 */
@Composable
fun FavoritesArtwork(
    size: Dp,
    iconSize: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(FavoritesGradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_favorite_filled),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

private val FavoritesGradient = Brush.linearGradient(
    0f to Color(0xFF2E1065),
    0.65f to Color(0xFF7C3AED),
    1f to Color(0xFFC4B5FD),
)
