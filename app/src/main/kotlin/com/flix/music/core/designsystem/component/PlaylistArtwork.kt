package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.ic_music_note
import org.jetbrains.compose.resources.painterResource

/**
 * Playlist cover (design "Artwork · Playlist"): the automatic mosaic of up to 4 covers,
 * a single cover, or a `music_note` placeholder when the playlist has no artwork yet.
 * With [stacked], a 1dp `textTertiary` outline peeks 4dp up and right, the "stack"
 * that tells playlists apart from songs and albums (`.art.pl`).
 */
@Composable
fun PlaylistArtwork(
    coverUrls: List<String>,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    stacked: Boolean = true,
    placeholderIconSize: Dp = size / 2,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val outlineColor = FlixTheme.colors.textTertiary
    val backgroundColor = FlixTheme.colors.background
    val stackModifier = if (stacked) {
        Modifier.drawBehind {
            val offset = Offset(STACK_OFFSET.toPx(), -STACK_OFFSET.toPx())
            val radius = CornerRadius(cornerRadius.toPx())
            val outline = 1.dp.toPx()
            drawRoundRect(outlineColor, topLeft = offset, size = this.size, cornerRadius = radius)
            drawRoundRect(
                color = backgroundColor,
                topLeft = offset + Offset(outline, outline),
                size = Size(this.size.width - outline * 2, this.size.height - outline * 2),
                cornerRadius = radius,
            )
        }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .then(stackModifier)
            .clip(shape)
            .background(FlixTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            coverUrls.isEmpty() -> Icon(
                painter = painterResource(Res.drawable.ic_music_note),
                contentDescription = null,
                tint = FlixTheme.colors.textTertiary,
                modifier = Modifier.size(placeholderIconSize),
            )
            coverUrls.size < MOSAIC_SIZE -> Cover(coverUrls.first(), Modifier.fillMaxSize())
            else -> Column {
                coverUrls.take(MOSAIC_SIZE).chunked(2).forEach { rowUrls ->
                    Row(Modifier.weight(1f)) {
                        rowUrls.forEach { url -> Cover(url, Modifier.weight(1f).fillMaxSize()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cover(url: String, modifier: Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

private const val MOSAIC_SIZE = 4
private val STACK_OFFSET = 4.dp
