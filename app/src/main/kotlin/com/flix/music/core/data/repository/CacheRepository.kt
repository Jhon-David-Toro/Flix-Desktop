package com.flix.music.core.data.repository


/** Temporary files the app can rebuild: image cache and audio buffers (Ajustes → Borrar caché). */
interface CacheRepository {

    suspend fun sizeInBytes(): Long

    suspend fun clear()
}
