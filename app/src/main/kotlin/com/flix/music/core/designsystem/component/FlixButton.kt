package com.flix.music.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Primary button (`.btn.b1`): Lumen violet pill. At most one per screen.
 * [compact] is the 32dp `.bsm` size used in top bars ("Guardar").
 */
@Composable
fun FlixPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    FlixButton(
        text = text,
        onClick = onClick,
        containerColor = FlixTheme.colors.primary,
        contentColor = FlixTheme.colors.onPrimary,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        compact = compact,
    )
}

/** Secondary button (`.btn.b2`): surfaceVariant pill, next to a primary action. */
@Composable
fun FlixSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
) {
    FlixButton(
        text = text,
        onClick = onClick,
        containerColor = FlixTheme.colors.surfaceVariant,
        contentColor = FlixTheme.colors.textPrimary,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        compact = false,
    )
}

/**
 * Outlined small button (`.btn.bo.bsm`): transparent with a 1dp textTertiary ring, used
 * for "Seguir / Siguiendo" on artists.
 */
@Composable
fun FlixOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(COMPACT_HEIGHT),
        shape = FlixTheme.shapes.pill,
        border = BorderStroke(1.dp, FlixTheme.colors.textTertiary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlixTheme.colors.textPrimary),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = FlixTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold))
    }
}

/** Disabled buttons fade as a whole to 38 % (design: Buttons → disabled). */
@Composable
private fun FlixButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
    icon: DrawableResource?,
    enabled: Boolean,
    compact: Boolean,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(if (compact) Modifier.height(COMPACT_HEIGHT) else Modifier),
        enabled = enabled,
        shape = FlixTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
        contentPadding = when {
            compact -> PaddingValues(horizontal = 14.dp)
            icon != null -> ButtonDefaults.ButtonWithIconContentPadding
            else -> ButtonDefaults.ContentPadding
        },
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(
            text = text,
            style = if (compact) FlixTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold) else FlixTheme.typography.label,
        )
    }
}

private const val DISABLED_ALPHA = 0.38f
private val COMPACT_HEIGHT = 32.dp
