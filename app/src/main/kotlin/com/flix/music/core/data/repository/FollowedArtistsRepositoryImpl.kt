package com.flix.music.core.data.repository

import com.flix.music.core.data.database.FollowedArtistEntity
import com.flix.music.core.data.database.FollowedArtistsDao
import com.flix.music.core.model.Artist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

class FollowedArtistsRepositoryImpl(
    private val followedArtistsDao: FollowedArtistsDao,
    private val clock: Clock,
) : FollowedArtistsRepository {

    override val artists: Flow<List<Artist>> = followedArtistsDao.observeAll().map { entities ->
        entities.map { entity -> Artist(entity.artistId, entity.name, entity.artworkUrl) }
    }

    override suspend fun follow(artist: Artist) {
        followedArtistsDao.upsert(FollowedArtistEntity(artist.id, artist.name, artist.artworkUrl, clock.millis()))
    }

    override suspend fun unfollow(artistId: String) = followedArtistsDao.delete(artistId)
}
