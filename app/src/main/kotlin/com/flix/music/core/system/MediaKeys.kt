package com.flix.music.core.system

import com.flix.music.core.common.Log
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser
import kotlin.concurrent.thread

/** The keyboard's media keys Flix answers to. */
enum class MediaKey(val virtualKey: Int) {
    PlayPause(0xB3),
    Next(0xB0),
    Previous(0xB1),
}

/**
 * Listens to the keyboard's media keys anywhere in Windows, also with Flix in the
 * background, by registering them as hotkeys on a thread with its own message loop.
 * A key another app already took (e.g. another player) is skipped and logged.
 */
class MediaKeys(private val onKey: (MediaKey) -> Unit) {

    fun start() {
        if (!System.getProperty("os.name").startsWith("Windows")) return
        thread(name = "flix-media-keys", isDaemon = true) {
            val user32 = User32.INSTANCE
            MediaKey.entries.forEach { key ->
                if (!user32.RegisterHotKey(null, key.ordinal + 1, MOD_NOREPEAT, key.virtualKey)) {
                    Log.w(TAG, "Media key $key is taken by another app")
                }
            }
            val message = WinUser.MSG()
            while (user32.GetMessage(message, null, 0, 0) > 0) {
                if (message.message == WinUser.WM_HOTKEY) {
                    MediaKey.entries.getOrNull(message.wParam.toInt() - 1)?.let(onKey)
                }
            }
        }
    }

    private companion object {
        const val TAG = "MediaKeys"
        const val MOD_NOREPEAT = 0x4000
    }
}
