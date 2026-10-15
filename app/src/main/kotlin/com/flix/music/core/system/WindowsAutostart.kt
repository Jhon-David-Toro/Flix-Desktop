package com.flix.music.core.system

import com.flix.music.core.common.Log
import java.util.concurrent.TimeUnit

/**
 * "Iniciar Flix con Windows": an entry under the user's `Run` key, so Windows opens Flix
 * minimized at sign-in. Needs no admin rights and shows in Task Manager → Inicio.
 * Only available in the installed app, whose launcher path jpackage exposes as
 * `jpackage.app-path`; a development run has no stable exe to register.
 */
class WindowsAutostart(
    private val appPath: String? = System.getProperty("jpackage.app-path"),
    private val isWindows: Boolean = System.getProperty("os.name").startsWith("Windows"),
) {

    val isAvailable: Boolean get() = isWindows && appPath != null

    fun isEnabled(): Boolean = isAvailable && reg("query", RUN_KEY, "/v", VALUE_NAME) == 0

    fun setEnabled(enabled: Boolean) {
        val path = appPath ?: return
        if (!isWindows) return
        val result = if (enabled) {
            reg("add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", "\"$path\" $MINIMIZED_ARGUMENT", "/f")
        } else {
            reg("delete", RUN_KEY, "/v", VALUE_NAME, "/f")
        }
        if (result != 0) Log.w(TAG, "reg ${if (enabled) "add" else "delete"} exited with $result")
    }

    private fun reg(vararg args: String): Int = runCatching {
        val process = ProcessBuilder("reg", *args).redirectErrorStream(true).start()
        process.inputStream.readAllBytes()
        if (process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) process.exitValue() else -1
    }.getOrElse { failure ->
        Log.w(TAG, "reg failed: ${failure.message}")
        -1
    }

    companion object {
        /** Passed by the Run entry: Flix starts minimized instead of in the user's face. */
        const val MINIMIZED_ARGUMENT = "--minimized"
        private const val TAG = "WindowsAutostart"
        private const val RUN_KEY = """HKCU\Software\Microsoft\Windows\CurrentVersion\Run"""
        private const val VALUE_NAME = "Flix"
        private const val TIMEOUT_SECONDS = 5L
    }
}
