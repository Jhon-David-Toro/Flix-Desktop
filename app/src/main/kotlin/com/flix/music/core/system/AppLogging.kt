package com.flix.music.core.system

import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

/**
 * Sends the app's log (warnings from `Log.w` and crashes) to `logs/flix.N.log` in
 * [dataDirectory], three files of 1 MB at most, so a problem on another PC can be
 * diagnosed from its files. Uncaught exceptions are logged before the app dies.
 */
fun installFileLogging(dataDirectory: File) {
    val directory = File(dataDirectory, "logs").apply { mkdirs() }
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1\$tF %1\$tT %4\$s %3\$s: %5\$s%6\$s%n")
    LogManager.getLogManager().reset()
    val root = Logger.getLogger("")
    runCatching {
        FileHandler(File(directory, "flix.%g.log").path, MAX_FILE_BYTES, FILE_COUNT, true).apply {
            formatter = SimpleFormatter()
            level = Level.INFO
        }
    }.onSuccess(root::addHandler)
    root.level = Level.INFO
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        root.log(Level.SEVERE, "Uncaught exception in ${thread.name}", throwable)
        previous?.uncaughtException(thread, throwable)
    }
}

private const val MAX_FILE_BYTES = 1_000_000
private const val FILE_COUNT = 3
