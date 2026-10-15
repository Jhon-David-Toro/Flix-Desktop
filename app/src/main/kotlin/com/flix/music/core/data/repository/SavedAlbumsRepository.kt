package com.flix.music.core.data.repository

import com.flix.music.core.model.Album
import kotlinx.coroutines.flow.Flow

/** Albums saved with the album ♥, stored on the device; the Library "Álbumes" filter. */
interface SavedAlbumsRepository {

    /** Most recently saved first. */
    val albums: Flow<List<Album>>

    suspend fun save(album: Album)

    suspend fun remove(albumId: String)
}
