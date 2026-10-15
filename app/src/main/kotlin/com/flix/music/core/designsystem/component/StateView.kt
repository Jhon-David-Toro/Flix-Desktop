package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.FlixError
import com.flix.music.core.ui.LocalErrorShortcuts
import com.flix.music.core.ui.LocalIsOnline
import com.flix.music.resources.Res
import com.flix.music.resources.action_go_to_library
import com.flix.music.resources.action_retry
import com.flix.music.resources.error_no_connection_body
import com.flix.music.resources.error_no_connection_title
import com.flix.music.resources.error_timeout_body
import com.flix.music.resources.error_timeout_title
import com.flix.music.resources.error_unknown_body
import com.flix.music.resources.error_unknown_title
import com.flix.music.resources.error_upstream_body
import com.flix.music.resources.error_upstream_title
import com.flix.music.resources.ic_cloud_off
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen state (design `empty()`): 72dp icon circle, title, body and up to two
 * buttons, the primary one first and an optional secondary one beside it.
 */
@Composable
fun StateView(
    icon: DrawableResource,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FlixTheme.spacing.x4, vertical = FlixTheme.spacing.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(FlixTheme.colors.surfaceVariant, FlixTheme.shapes.circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = FlixTheme.colors.textSecondary,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            text = title,
            style = FlixTheme.typography.title,
            color = FlixTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = FlixTheme.typography.bodySmall,
            color = FlixTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm)) {
            if (actionLabel != null && onAction != null) {
                FlixPrimaryButton(text = actionLabel, onClick = onAction)
            }
            if (secondaryLabel != null && onSecondary != null) {
                FlixSecondaryButton(text = secondaryLabel, onClick = onSecondary)
            }
        }
    }
}

/**
 * Error with "Reintentar" (design "Errores"). No connection adds "Ir a Biblioteca", and
 * the screen retries by itself when the phone comes back online (design: "se reintenta
 * lo que falló").
 */
@Composable
fun ErrorStateView(
    error: FlixError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    upstreamTitle: StringResource = Res.string.error_upstream_title,
    upstreamBody: StringResource = Res.string.error_upstream_body,
) {
    val (icon, genericTitle, genericBody) = error.toCopy()
    val isUpstream = error == FlixError.Upstream
    val shortcuts = LocalErrorShortcuts.current
    val isOnline = LocalIsOnline.current
    val currentOnRetry by rememberUpdatedState(onRetry)
    var wasOffline by remember { mutableStateOf(!isOnline) }
    LaunchedEffect(isOnline) {
        if (isOnline && wasOffline) currentOnRetry()
        wasOffline = !isOnline
    }
    val secondary: Pair<StringResource, () -> Unit>? = when {
        shortcuts == null -> null
        error == FlixError.NoConnection -> Res.string.action_go_to_library to shortcuts.openLibrary
        else -> null
    }
    StateView(
        icon = icon,
        title = stringResource(if (isUpstream) upstreamTitle else genericTitle),
        body = stringResource(if (isUpstream) upstreamBody else genericBody),
        actionLabel = stringResource(Res.string.action_retry),
        onAction = onRetry,
        secondaryLabel = secondary?.let { (label, _) -> stringResource(label) },
        onSecondary = secondary?.second,
        modifier = modifier,
    )
}

private fun FlixError.toCopy(): Triple<DrawableResource, StringResource, StringResource> = when (this) {
    FlixError.NoConnection ->
        Triple(Res.drawable.ic_cloud_off, Res.string.error_no_connection_title, Res.string.error_no_connection_body)
    FlixError.Timeout ->
        Triple(Res.drawable.ic_cloud_off, Res.string.error_timeout_title, Res.string.error_timeout_body)
    FlixError.Upstream ->
        Triple(Res.drawable.ic_cloud_off, Res.string.error_upstream_title, Res.string.error_upstream_body)
    FlixError.Unknown ->
        Triple(Res.drawable.ic_cloud_off, Res.string.error_unknown_title, Res.string.error_unknown_body)
}