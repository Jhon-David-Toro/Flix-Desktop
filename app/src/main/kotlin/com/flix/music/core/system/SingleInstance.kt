package com.flix.music.core.system

import com.flix.music.core.common.Log
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock
import kotlin.concurrent.thread

/**
 * Keeps one Flix running. The first instance holds a lock on `instance.lock` and
 * listens on a local port written to `instance.port` (Windows does not let another
 * process read a locked file); a second launch (a double click, or
 * Windows starting it at sign-in while it is open) finds the lock taken, asks the
 * first one to show its window through that port, and exits.
 */
class SingleInstance private constructor(private val lock: FileLock, private val server: ServerSocket) {

    /** Runs [onShowRequest] (on a background thread) whenever another launch asks for the window. */
    fun listen(onShowRequest: () -> Unit) {
        thread(name = "flix-single-instance", isDaemon = true) {
            while (!server.isClosed) {
                runCatching { server.accept().use { onShowRequest() } }
            }
        }
    }

    fun release() {
        runCatching { server.close() }
        runCatching { lock.release() }
    }

    companion object {
        /** This launch's instance, or null after handing over to the one already running. */
        fun acquire(dataDirectory: File): SingleInstance? {
            val lockFile = RandomAccessFile(File(dataDirectory, "instance.lock"), "rw")
            val portFile = File(dataDirectory, "instance.port")
            val lock = runCatching { lockFile.channel.tryLock() }.getOrNull()
            if (lock == null) {
                lockFile.close()
                val port = runCatching { portFile.readText().trim().toInt() }.getOrNull()
                if (port != null) {
                    runCatching { Socket().use { socket -> socket.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), TIMEOUT_MILLIS) } }
                        .onFailure { failure -> Log.w(TAG, "Could not reach the running Flix: ${failure.message}") }
                }
                return null
            }
            val server = ServerSocket(0, BACKLOG, InetAddress.getLoopbackAddress())
            portFile.writeText(server.localPort.toString())
            return SingleInstance(lock, server)
        }

        private const val TAG = "SingleInstance"
        private const val TIMEOUT_MILLIS = 2_000
        private const val BACKLOG = 4
    }
}
