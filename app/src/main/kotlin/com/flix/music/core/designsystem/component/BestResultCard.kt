package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.ic_play_arrow_filled
import org.jetbrains.compose.resources.painterResource

val BEST_RESULT_ARTWORK = 72.dp

@Composable
fun BestResultCard(
    overline: String,
    title: String,
    subtitle: String,
    artworkUrl: String?,
    playLabel: String,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlixCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FlixTheme.spacing.gutter),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(FlixTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.lg),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(BEST_RESULT_ARTWORK)
                    .clip(FlixTheme.shapes.md)
                    .background(FlixTheme.colors.surfaceVariant),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = overline.uppercase(),
                    style = FlixTheme.typography.overline,
                    color = FlixTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = FlixTheme.spacing.xs),
                )
                Text(
                    text = title,
                    style = FlixTheme.typography.title,
                    color = FlixTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = FlixTheme.typography.caption,
                    color = FlixTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledIconButton(
                onClick = onPlay,
                modifier = Modifier.size(FlixTheme.spacing.touchMin),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = FlixTheme.colors.primary,
                    contentColor = FlixTheme.colors.onPrimary,
                ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_play_arrow_filled),
                    contentDescription = playLabel,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}