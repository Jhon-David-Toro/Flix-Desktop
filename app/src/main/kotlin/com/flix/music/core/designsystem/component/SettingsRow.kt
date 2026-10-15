package com.flix.music.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.ic_chevron_right
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Settings row (design `.set-r`): at least 50dp, optional leading icon, title with an
 * optional secondary line, and a trailing slot (switch, chevron or "Próximamente").
 * [enabled] false fades it to 45 % (`.set-r.dis`) and ignores taps.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = FlixTheme.colors.textSecondary,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = FlixTheme.spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
    ) {
        icon?.let { drawable ->
            Icon(painter = painterResource(drawable), contentDescription = null, tint = FlixTheme.colors.textPrimary)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = FlixTheme.typography.body, color = FlixTheme.colors.textPrimary)
            subtitle?.let { text -> Text(text = text, style = FlixTheme.typography.caption, color = subtitleColor) }
        }
        trailing()
    }
}

/** Trailing chevron for rows that open another screen. */
@Composable
fun SettingsChevron() {
    Icon(
        painter = painterResource(Res.drawable.ic_chevron_right),
        contentDescription = null,
        tint = FlixTheme.colors.textSecondary,
    )
}

/** Group title (design `.ovl.set-g`): uppercase overline with 18dp above and 6dp below. */
@Composable
fun SettingsGroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = FlixTheme.typography.overline,
        color = FlixTheme.colors.textSecondary,
        modifier = modifier
            .padding(horizontal = FlixTheme.spacing.gutter)
            .padding(top = 18.dp, bottom = 6.dp),
    )
}

private const val DISABLED_ALPHA = 0.45f
