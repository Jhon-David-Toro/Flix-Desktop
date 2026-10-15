package com.flix.music.core.designsystem.component

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.flix.music.core.designsystem.theme.FlixTheme

@Composable
fun FlixFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flixColors = FlixTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = FlixTheme.typography.label) },
        modifier = modifier,
        shape = FlixTheme.shapes.pill,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = flixColors.surface,
            labelColor = flixColors.textPrimary,
            selectedContainerColor = flixColors.textPrimary,
            selectedLabelColor = flixColors.background,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = flixColors.divider,
            selectedBorderColor = Color.Transparent,
        ),
    )
}