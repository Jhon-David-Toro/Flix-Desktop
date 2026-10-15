package com.flix.music.feature.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.FollowedArtistsRepository
import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.Album
import com.flix.music.core.model.Artist
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.Track
import com.flix.music.core.model.byArtist
import com.flix.music.core.model.distinctAlbums
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ArtistUiState {

    val name: String

    data class Loading(override val name: String) : ArtistUiState

    data class Error(override val name: String, val error: FlixError) : ArtistUiState

    /**
     * [heroArtworkUrl] is the cover of the most popular song, since there are no artist
     * photos. [canFollow] is false when the artist has no channel id.
     */
    data class Content(
        override val name: String,
        val heroArtworkUrl: String?,
        val popular: List<Track>,
        val albums: List<Album>,
        val canFollow: Boolean,
        val isFollowing: Boolean,
    ) : ArtistUiState
}

/**
 * Artist (design `artist`): Populares and Álbumes derived from a search by the
 * artist's name, since there is no artist endpoint. "Relacionados" and "Sencillos"
 * stay hidden until the backend offers them.
 */
class ArtistViewModel(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val followedArtistsRepository: FollowedArtistsRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val artistId: String? = savedStateHandle.get<String>(ARTIST_ID_ARGUMENT)
    private val artistName: String = requireNotNull(savedStateHandle.get<String>(ARTIST_NAME_ARGUMENT))

    private val loaded = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading(artistName))

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    val uiState: StateFlow<ArtistUiState> = combine(loaded, followedArtistsRepository.artists) { state, followed ->
        if (state is ArtistUiState.Content) state.copy(isFollowing = followed.any { it.id == artistId }) else state
    }.stateIn(viewModelScope, WhileUiSubscribed, loaded.value)

    init {
        load()
    }

    fun onRetry() = load()

    fun onPlay() = playPopular(startIndex = 0, shuffle = false)

    fun onShuffle() {
        val popular = popular()
        if (popular.isNotEmpty()) playPopular(startIndex = popular.indices.random(), shuffle = true)
    }

    fun onTrackClick(track: Track) {
        val startIndex = popular().indexOfFirst { it.id == track.id }
        if (startIndex >= 0) playPopular(startIndex, shuffle = false)
    }

    fun onToggleFollow() {
        val content = uiState.value as? ArtistUiState.Content ?: return
        val id = artistId ?: return
        viewModelScope.launch {
            if (content.isFollowing) {
                followedArtistsRepository.unfollow(id)
            } else {
                followedArtistsRepository.follow(Artist(id, artistName, content.heroArtworkUrl))
            }
        }
    }

    private fun load() {
        loaded.value = ArtistUiState.Loading(artistName)
        viewModelScope.launch {
            loaded.value = try {
                val popular = musicRepository.search(artistName).byArtist(artistId, artistName)
                ArtistUiState.Content(
                    name = artistName,
                    heroArtworkUrl = popular.firstOrNull()?.artworkUrl,
                    popular = popular,
                    albums = popular.distinctAlbums(),
                    canFollow = artistId != null,
                    isFollowing = false,
                )
            } catch (exception: FlixException) {
                ArtistUiState.Error(artistName, exception.error)
            }
        }
    }

    private fun popular(): List<Track> = (loaded.value as? ArtistUiState.Content)?.popular.orEmpty()

    private fun playPopular(startIndex: Int, shuffle: Boolean) {
        val popular = popular()
        if (popular.isEmpty()) return
        playerController.play(popular, startIndex, PlaybackSource.Artist(artistName), shuffle)
    }

    internal companion object {
        const val ARTIST_ID_ARGUMENT = "artistId"
        const val ARTIST_NAME_ARGUMENT = "artistName"
    }
}
