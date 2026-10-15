package com.flix.music.core.ui.window

import com.flix.music.core.common.Log
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

@Suppress("FunctionName")
private interface Dwmapi : Library {
    fun DwmSetWindowAttribute(window: Pointer, attribute: Int, value: IntByReference, size: Int): Int
}

/**
 * Asks Windows 11 to round the corners of the undecorated window, as it does for
 * framed ones. Older Windows and other systems keep square corners.
 */
fun Window.roundCornersOnWindows11() {
    if (!System.getProperty("os.name").startsWith("Windows")) return
    runCatching {
        val dwmapi = Native.load("dwmapi", Dwmapi::class.java)
        val handle = Native.getWindowPointer(this) ?: return
        dwmapi.DwmSetWindowAttribute(handle, WINDOW_CORNER_PREFERENCE, IntByReference(CORNER_ROUND), Int.SIZE_BYTES)
    }.onFailure { failure -> Log.w("WindowCorners", "Could not round the window: ${failure.message}") }
}

private const val WINDOW_CORNER_PREFERENCE = 33
private const val CORNER_ROUND = 2
