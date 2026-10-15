package com.flix.music.core.common

/** The part of Android's `Log` the code shared with the phone app uses, on the JVM logger. */
object Log {

    fun w(tag: String, message: String) {
        System.getLogger(tag).log(System.Logger.Level.WARNING, message)
    }
}
