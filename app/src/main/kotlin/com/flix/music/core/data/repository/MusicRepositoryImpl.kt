package com.flix.music.core.data.repository

import com.flix.music.core.data.network.catalogCall
import com.flix.music.core.data.youtube.NewPipeSetup
import com.flix.music.core.data.youtube.YouTubeMusicClient
import com.flix.music.core.data.youtube.upscaledThumbnail
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.SongDetails
import com.flix.music.core.model.Track
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Search through YouTube Music's own API and song details through NewPipeExtractor,
 * both from the phone, so YouTube sees a normal user instead of a server.
 */
class MusicRepositoryImpl(
    private val youTubeMusicClient: YouTubeMusicClient,
    private val newPipeSetup: NewPipeSetup,
) : MusicRepository {

    private val seenTracks = ConcurrentHashMap<String, Track>()

    override suspend fun search(query: String): List<Track> = catalogCall {
        youTubeMusicClient.searchSongs(query)
            .distinctBy { it.id }
            .onEach { track -> seenTracks[track.id] = track }
    }

    override suspend fun searchVideos(query: String): List<Track> = catalogCall {
        youTubeMusicClient.searchVideos(query)
            .distinctBy { it.id }
            .onEach { track -> seenTracks[track.id] = track }
    }

    override fun cachedTrack(trackId: String): Track? = seenTracks[trackId]

    /** Seen songs keep their search metadata (with album); others are read from YouTube. */
    override suspend fun song(trackId: String): SongDetails = catalogCall {
        val track = seenTracks[trackId] ?: trackFromYouTube(trackId).also { track -> seenTracks[trackId] = track }
        SongDetails(track, isPlayable = true)
    }

    override suspend fun radio(track: Track): List<Track> = catalogCall {
        youTubeMusicClient.radio(track.id)
            .filterNot { radioTrack -> radioTrack.id == track.id }
            .distinctBy(Track::id)
            .onEach { radioTrack -> seenTracks[radioTrack.id] = radioTrack }
    }

    private fun trackFromYouTube(trackId: String): Track {
        newPipeSetup.ensureInitialized()
        val info = StreamInfo.getInfo(ServiceList.YouTube, YOUTUBE_WATCH_URL + trackId)
        val artistName = info.uploaderName.orEmpty().removeSuffix(TOPIC_CHANNEL_SUFFIX)
        val channelId = info.uploaderUrl?.substringAfter(CHANNEL_PATH, missingDelimiterValue = "")?.ifEmpty { null }
        return Track(
            id = trackId,
            title = info.name,
            artists = if (artistName.isBlank()) emptyList() else listOf(ArtistRef(channelId, artistName)),
            album = null,
            duration = info.duration.takeIf { seconds -> seconds > 0 }?.seconds,
            artworkUrl = upscaledThumbnail(info.thumbnails.maxByOrNull { image -> image.height }?.url),
        )
    }

    private companion object {
        const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v="
        const val TOPIC_CHANNEL_SUFFIX = " - Topic"
        const val CHANNEL_PATH = "/channel/"
    }
}
