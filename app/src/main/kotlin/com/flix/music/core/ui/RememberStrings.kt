package com.flix.music.core.ui

import androidx.compose.runtime.Composable
import com.flix.music.core.common.Log
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Texts for code that runs outside composition, such as click handlers, where the
 * desktop's resource reader (a suspend call) cannot be used: [resources] are read once
 * while composing and the returned function looks them up. Templates with `%1$s`
 * arguments are filled with [String.format], as Android's `getString` does.
 */
@Composable
fun rememberStrings(vararg resources: StringResource): (StringResource, Array<out Any>) -> String {
    val values = resources.associateWith { resource -> stringResource(resource) }
    return { resource, arguments ->
        val template = values[resource] ?: "".also { Log.w("RememberStrings", "Not prefetched: ${resource.key}") }
        if (arguments.isEmpty()) template else template.format(*arguments)
    }
}

/** Reads a prefetched text, filling its `%1$s` arguments. */
operator fun ((StringResource, Array<out Any>) -> String).invoke(resource: StringResource, vararg arguments: Any): String =
    this(resource, arguments)
