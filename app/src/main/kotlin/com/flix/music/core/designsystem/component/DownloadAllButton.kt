package com.flix.music.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.DownloadAllState
import com.flix.music.resources.Res
import com.flix.music.resources.download_all_done
import com.flix.music.resources.download_all_in_progress
import com.flix.music.resources.ic_download
import com.flix.music.resources.ic_download_done_filled
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * "Descargar todo" for a favorites or playlist header: a download arrow, a spinner while
 * its songs download, and a primary-colored check once they are all on the computer.
 */
@Composable
fun DownloadAllButton(
    state: DownloadAllState,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = state == DownloadAllState.Done
    val description = when (state) {
        DownloadAllState.Done -> stringResource(Res.string.download_all_done)
        DownloadAllState.InProgress -> stringResource(Res.string.download_all_in_progress)
        DownloadAllState.None -> label
    }
    FilledTonalIconButton(
        onClick = onClick,
        enabled = state == DownloadAllState.None,
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = FlixTheme.colors.surfaceVariant,
            contentColor = FlixTheme.colors.textPrimary,
            disabledContainerColor = FlixTheme.colors.surfaceVariant,
            disabledContentColor = if (done) FlixTheme.colors.primary else FlixTheme.colors.textSecondary,
        ),
    ) {
        if (state == DownloadAllState.InProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = FlixTheme.colors.primary,
            )
        } else {
            Icon(
                painter = painterResource(if (done) Res.drawable.ic_download_done_filled else Res.drawable.ic_download),
                contentDescription = description,
            )
        }
    }
}
