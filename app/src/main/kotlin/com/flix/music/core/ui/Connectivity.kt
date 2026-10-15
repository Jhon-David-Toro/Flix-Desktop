package com.flix.music.core.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Whether the phone is online; screens in an error state retry by themselves when it returns. */
val LocalIsOnline = compositionLocalOf { true }

/** Where the second button of a full-screen error goes (design "Sin conexión" → Ir a Biblioteca). */
class ErrorShortcuts(
    val openLibrary: () -> Unit,
)

/** Null in previews and tests, where the errors show only "Reintentar". */
val LocalErrorShortcuts = staticCompositionLocalOf<ErrorShortcuts?> { null }
