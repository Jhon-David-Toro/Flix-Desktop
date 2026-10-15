package com.flix.music.core.designsystem.component.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * True only after [active] has stayed true for [delay]. Implements the design rule
 * "rings appear after 600 ms": if audio arrives sooner, no loader is ever shown.
 */
@Composable
fun rememberDelayedFlag(active: Boolean, delay: Duration = LOADING_INDICATOR_DELAY): Boolean {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (active) {
            delay(delay)
            shown = true
        } else {
            shown = false
        }
    }
    return active && shown
}

val LOADING_INDICATOR_DELAY = 600.milliseconds
