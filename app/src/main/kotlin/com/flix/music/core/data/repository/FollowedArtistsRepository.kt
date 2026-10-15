package com.flix.music.core.data.repository

import com.flix.music.core.model.Artist
import kotlinx.coroutines.flow.Flow

/** Artists the user follows ("Seguir"), stored on the device; the Library "Artistas" filter. */
interface FollowedArtistsRepository {

    /** Most recently followed first. */
    val artists: Flow<List<Artist>>

    suspend fun follow(artist: Artist)

    suspend fun unfollow(artistId: String)
}
