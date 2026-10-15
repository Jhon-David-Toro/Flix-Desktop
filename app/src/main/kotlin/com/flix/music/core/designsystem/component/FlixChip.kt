package com.flix.music.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flix.music.core.designsystem.theme.FlixTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Action chip from the design (`.chip`): surface background, 1 px divider border,
 * textPrimary label and optional leading icon. Used for moods and suggestions.
 */
@Composable
fun FlixChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: DrawableResource? = null,
) {
    val flixColors = FlixTheme.colors
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = FlixTheme.typography.label) },
        modifier = modifier,
        leadingIcon = leadingIcon?.let { iconRes ->
            {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            }
        },
        shape = FlixTheme.shapes.pill,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = flixColors.surface,
            labelColor = flixColors.textPrimary,
            leadingIconContentColor = flixColors.textPrimary,
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = flixColors.divider),
    )
}
