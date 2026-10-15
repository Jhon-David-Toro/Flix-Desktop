package com.flix.music.core.system

import com.flix.music.core.common.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** What Flix remembers about its window and volume between launches. */
@Serializable
data class WindowMemory(
    val x: Int? = null,
    val y: Int? = null,
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT,
    val maximized: Boolean = false,
    val volume: Int = DEFAULT_VOLUME,
) {
    companion object {
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 820
        const val DEFAULT_VOLUME = 100
    }
}

/** Reads and writes [WindowMemory] as `window.json`; a missing or broken file means defaults. */
class WindowMemoryStore(private val file: File, private val json: Json) {

    fun read(): WindowMemory = runCatching { json.decodeFromString<WindowMemory>(file.readText()) }.getOrDefault(WindowMemory())

    fun write(memory: WindowMemory) {
        runCatching { file.writeText(json.encodeToString(memory)) }
            .onFailure { failure -> Log.w("WindowMemory", "Could not save the window: ${failure.message}") }
    }
}
