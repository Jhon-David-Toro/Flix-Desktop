package com.flix.music.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.action_back
import com.flix.music.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Detail-screen top bar (design `.topbar.back`): back arrow, Bricolage title and actions. */
@Composable
fun FlixTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TOP_APP_BAR_HEIGHT)
            .padding(horizontal = FlixTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = stringResource(Res.string.action_back),
                tint = FlixTheme.colors.textPrimary,
            )
        }
        Text(
            text = title,
            style = FlixTheme.typography.title,
            color = FlixTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        actions()
    }
}

private val TOP_APP_BAR_HEIGHT = 56.dp
