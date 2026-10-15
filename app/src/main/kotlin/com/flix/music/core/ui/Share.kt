package com.flix.music.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.resources.Res
import com.flix.music.resources.share_copied
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * The desktop's "Compartir": the phone opens the share sheet, the PC copies the same
 * text to the clipboard and confirms it with a snackbar.
 */
@Composable
fun rememberShare(): (String) -> Unit {
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val copiedMessage = stringResource(Res.string.share_copied)
    return { text ->
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        coroutineScope.launch { snackbarHostState.showSnackbar(copiedMessage) }
    }
}
