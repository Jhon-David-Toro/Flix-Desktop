package com.flix.music.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Text-only row used for recent searches and typing suggestions
 * (design: `.row` with `min-height: 44px`, weight 500 text, textSecondary icons).
 */
@Composable
fun SuggestionRow(
    text: String,
    leadingIcon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SUGGESTION_ROW_MIN_HEIGHT)
            .handCursor().clickable(onClick = onClick)
            .padding(
                start = FlixTheme.spacing.gutter,
                end = if (trailing != null) FlixTheme.spacing.xs else FlixTheme.spacing.gutter,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        Icon(
            painter = painterResource(leadingIcon),
            contentDescription = null,
            tint = FlixTheme.colors.textSecondary,
        )
        Text(
            text = text,
            style = FlixTheme.typography.body.copy(fontWeight = FontWeight.Medium),
            color = FlixTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

private val SUGGESTION_ROW_MIN_HEIGHT = 56.dp
