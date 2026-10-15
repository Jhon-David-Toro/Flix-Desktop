package com.flix.music.core.designsystem.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The app's single snackbar host (design: one snackbar at a time, above the mini
 * player). Screens read it to show feedback such as "Quitada de favoritos · Deshacer".
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState no provisto: FlixApp debe proveer LocalSnackbarHostState")
}

/** Shows [message] with an undo action and runs [onUndo] if the user taps it. */
suspend fun SnackbarHostState.showUndo(message: String, undoLabel: String, onUndo: suspend () -> Unit) {
    val result = showSnackbar(message = message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
    if (result == SnackbarResult.ActionPerformed) onUndo()
}
