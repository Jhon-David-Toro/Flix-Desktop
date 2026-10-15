package com.flix.music.testing

import com.flix.music.core.data.repository.FollowedArtistsRepository
import com.flix.music.core.data.repository.SavedAlbumsRepository
import com.flix.music.core.model.Album
import com.flix.music.core.model.Artist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory followed artists, most recent first like the Room query. */
class FakeFollowedArtistsRepository(initial: List<Artist> = emptyList()) : FollowedArtistsRepository {

    private val stored = MutableStateFlow(initial)

    val current: List<Artist> get() = stored.value

    override val artists: Flow<List<Artist>> = stored

    override suspend fun follow(artist: Artist) {
        stored.value = listOf(artist) + stored.value.filterNot { it.id == artist.id }
    }

    override suspend fun unfollow(artistId: String) {
        stored.value = stored.value.filterNot { it.id == artistId }
    }
}

/** In-memory saved albums, most recent first like the Room query. */
class FakeSavedAlbumsRepository(initial: List<Album> = emptyList()) : SavedAlbumsRepository {

    private val stored = MutableStateFlow(initial)

    val current: List<Album> get() = stored.value

    override val albums: Flow<List<Album>> = stored

    override suspend fun save(album: Album) {
        stored.value = listOf(album) + stored.value.filterNot { it.id == album.id }
    }

    override suspend fun remove(albumId: String) {
        stored.value = stored.value.filterNot { it.id == albumId }
    }
}
