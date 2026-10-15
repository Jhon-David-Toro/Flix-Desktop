package com.flix.music.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Snackbar from the design (`.snack`): inverted neutral surface (light on dark theme,
 * dark on light theme), radius md, and a violet action readable on that surface.
 * [FlixSnackbarVisuals] add the leading icon of confirmations ("Añadida a … · Ver").
 */
@Composable
fun FlixSnackbar(snackbarData: SnackbarData, modifier: Modifier = Modifier) {
    val actionColor = if (FlixTheme.isDark) FlixTheme.colors.primary else ActionOnDarkSnackbar
    val visuals = snackbarData.visuals
    if (visuals !is FlixSnackbarVisuals) {
        Snackbar(
            snackbarData = snackbarData,
            modifier = modifier.padding(horizontal = FlixTheme.spacing.sm),
            shape = FlixTheme.shapes.md,
            containerColor = FlixTheme.colors.textPrimary,
            contentColor = FlixTheme.colors.background,
            actionColor = actionColor,
            actionContentColor = actionColor,
        )
        return
    }
    Snackbar(
        modifier = modifier.padding(horizontal = FlixTheme.spacing.sm),
        shape = FlixTheme.shapes.md,
        containerColor = FlixTheme.colors.textPrimary,
        contentColor = FlixTheme.colors.background,
        action = visuals.actionLabel?.let { label ->
            {
                TextButton(onClick = snackbarData::performAction) {
                    Text(label, style = FlixTheme.typography.label, color = actionColor)
                }
            }
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(visuals.icon),
                contentDescription = null,
                tint = if (visuals.success) SuccessOnSnackbar else FlixTheme.colors.background,
                modifier = Modifier.size(18.dp),
            )
            Text(visuals.message)
        }
    }
}

/** A snackbar with a leading [icon]; [success] tints it green as in "Playlist creada". */
data class FlixSnackbarVisuals(
    override val message: String,
    val icon: DrawableResource,
    override val actionLabel: String? = null,
    val success: Boolean = false,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration =
        if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
}

/** Light violet used for actions on the dark snackbar of the light theme (`.snack .act`). */
private val ActionOnDarkSnackbar = Color(0xFFA78BFA)

/** `.snack .ic-ok`: readable on both inverted surfaces. */
private val SuccessOnSnackbar = Color(0xFF5BC98B)
