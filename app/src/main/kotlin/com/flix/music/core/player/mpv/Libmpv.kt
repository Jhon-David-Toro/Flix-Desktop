package com.flix.music.core.player.mpv

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File

/** The part of libmpv's C API (`mpv/client.h`) Flix uses, through JNA. */
@Suppress("FunctionName")
internal interface Libmpv : Library {

    fun mpv_create(): Pointer?

    fun mpv_initialize(handle: Pointer): Int

    fun mpv_set_option_string(handle: Pointer, name: String, data: String): Int

    /** [args] must end with null, as C's `const char **`. */
    fun mpv_command(handle: Pointer, args: Array<String?>): Int

    fun mpv_set_property_string(handle: Pointer, name: String, data: String): Int

    fun mpv_observe_property(handle: Pointer, replyUserdata: Long, name: String, format: Int): Int

    fun mpv_wait_event(handle: Pointer, timeout: Double): Pointer

    fun mpv_wakeup(handle: Pointer)

    fun mpv_terminate_destroy(handle: Pointer)

    fun mpv_error_string(error: Int): String

    companion object {
        const val FORMAT_NONE = 0
        const val FORMAT_FLAG = 3
        const val FORMAT_DOUBLE = 5

        const val EVENT_NONE = 0
        const val EVENT_SHUTDOWN = 1
        const val EVENT_START_FILE = 6
        const val EVENT_END_FILE = 7
        const val EVENT_FILE_LOADED = 8
        const val EVENT_PLAYBACK_RESTART = 21
        const val EVENT_PROPERTY_CHANGE = 22

        const val END_FILE_REASON_EOF = 0
        const val END_FILE_REASON_ERROR = 4

        fun load(file: File): Libmpv =
            Native.load(file.absolutePath, Libmpv::class.java, mapOf(Library.OPTION_STRING_ENCODING to "UTF-8"))
    }
}

/** `mpv_event`. */
@Structure.FieldOrder("event_id", "error", "reply_userdata", "data")
internal class MpvEvent(pointer: Pointer) : Structure(pointer) {
    @JvmField var event_id: Int = 0
    @JvmField var error: Int = 0
    @JvmField var reply_userdata: Long = 0
    @JvmField var data: Pointer? = null

    init {
        read()
    }
}

/** `mpv_event_property`. */
@Structure.FieldOrder("name", "format", "data")
internal class MpvEventProperty(pointer: Pointer) : Structure(pointer) {
    @JvmField var name: String? = null
    @JvmField var format: Int = 0
    @JvmField var data: Pointer? = null

    init {
        read()
    }
}

/** `mpv_event_end_file`. */
@Structure.FieldOrder("reason", "error", "playlist_entry_id", "playlist_insert_id", "playlist_insert_num_entries")
internal class MpvEventEndFile(pointer: Pointer) : Structure(pointer) {
    @JvmField var reason: Int = 0
    @JvmField var error: Int = 0
    @JvmField var playlist_entry_id: Long = 0
    @JvmField var playlist_insert_id: Long = 0
    @JvmField var playlist_insert_num_entries: Int = 0

    init {
        read()
    }
}
