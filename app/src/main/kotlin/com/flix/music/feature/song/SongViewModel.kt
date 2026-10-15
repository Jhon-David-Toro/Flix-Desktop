package com.flix.music.feature.song

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.data.repository.FollowedArtistsRepository
import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.Artist
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.Track
import com.flix.music.core.model.byArtist
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the song screen shows. [track] arrives first from the search cache (design:
 * "abre con datos del origen, progresivo"); [error] only matters while it is null.
 */
data class SongUiState(
    val track: Track? = null,
    val isPlayable: Boolean = true,
    val error: FlixError? = null,
    val moreLikeThis: List<Track> = emptyList(),
    val isFavorite: Boolean = false,
    val isFollowingArtist: Boolean = false,
) {
    /** The artist shown in the "Artista" section; only artists with an id can be followed. */
    val mainArtist get() = track?.artists?.firstOrNull()
}

/** Song detail (design `song`): play, favorite, add to playlist, album, artist and "Más como esta". */
class SongViewModel(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val followedArtistsRepository: FollowedArtistsRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val trackId: String = requireNotNull(savedStateHandle.get<String>(TRACK_ID_ARGUMENT))

    private val loaded = MutableStateFlow(SongUiState(track = musicRepository.cachedTrack(trackId)))

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    val uiState: StateFlow<SongUiState> = combine(
        loaded,
        favoritesRepository.favoriteIds,
        followedArtistsRepository.artists,
    ) { state, favoriteIds, followedArtists ->
        state.copy(
            isFavorite = trackId in favoriteIds,
            isFollowingArtist = followedArtists.any { artist -> artist.id == state.mainArtist?.id },
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, loaded.value)

    init {
        load()
    }

    fun onRetry() = load()

    /** Plays the song followed by "Más como esta". */
    fun onPlay() {
        val state = loaded.value
        val track = state.track ?: return
        if (!state.isPlayable) return
        playerController.play(listOf(track) + state.moreLikeThis, 0, PlaybackSource.Song(track.title))
    }

    fun onMoreLikeThisClick(track: Track) {
        val tracks = loaded.value.moreLikeThis
        val startIndex = tracks.indexOfFirst { it.id == track.id }
        val song = loaded.value.track ?: return
        if (startIndex >= 0) playerController.play(tracks, startIndex, PlaybackSource.Song(song.title))
    }

    fun onToggleFavorite() {
        val track = loaded.value.track ?: return
        viewModelScope.launch { favoritesRepository.toggle(track) }
    }

    fun onToggleFollow() {
        val state = uiState.value
        val artist = state.mainArtist ?: return
        val artistId = artist.id ?: return
        viewModelScope.launch {
            if (state.isFollowingArtist) {
                followedArtistsRepository.unfollow(artistId)
            } else {
                followedArtistsRepository.follow(Artist(artistId, artist.name, state.track?.artworkUrl))
            }
        }
    }

    private fun load() {
        loaded.update { state -> state.copy(error = null) }
        viewModelScope.launch {
            try {
                val details = musicRepository.song(trackId)
                loaded.update { state -> state.copy(track = details.track, isPlayable = details.isPlayable) }
            } catch (exception: FlixException) {
                if (loaded.value.track == null) loaded.update { state -> state.copy(error = exception.error) }
                return@launch
            }
            loadMoreLikeThis()
        }
    }

    /** Other songs by the same artist (there is no recommendations endpoint). */
    private suspend fun loadMoreLikeThis() {
        val artist = loaded.value.mainArtist ?: return
        val songs = runCatching { musicRepository.search(artist.name) }.getOrDefault(emptyList())
        val moreLikeThis = songs
            .byArtist(artist.id, artist.name)
            .filterNot { it.id == trackId }
            .take(MORE_LIKE_THIS_COUNT)
        loaded.update { state -> state.copy(moreLikeThis = moreLikeThis) }
    }

    internal companion object {
        const val TRACK_ID_ARGUMENT = "trackId"
        private const val MORE_LIKE_THIS_COUNT = 3
    }
}
