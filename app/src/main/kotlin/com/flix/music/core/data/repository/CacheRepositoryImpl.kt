package com.flix.music.core.data.repository

import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The desktop's caches live in [cacheDirectory] (`%APPDATA%\Flix\cache`, where Coil
 * keeps artwork). Clearing also empties Coil's memory cache.
 */
class CacheRepositoryImpl(private val cacheDirectory: File) : CacheRepository {

    override suspend fun sizeInBytes(): Long = withContext(Dispatchers.IO) {
        cacheDirectory.walkBottomUp().filter(File::isFile).sumOf(File::length)
    }

    override suspend fun clear() {
        SingletonImageLoader.get(coil3.PlatformContext.INSTANCE).memoryCache?.clear()
        withContext(Dispatchers.IO) {
            cacheDirectory.listFiles()?.forEach { file -> file.deleteRecursively() }
        }
    }
}
