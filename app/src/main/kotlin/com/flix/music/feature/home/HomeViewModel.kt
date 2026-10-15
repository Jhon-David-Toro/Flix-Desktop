package com.flix.music.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.data.repository.HistoryRepository
import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.data.repository.PlaylistsRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.Genre
import com.flix.music.core.model.Genres
import com.flix.music.core.model.Playlist
import com.flix.music.core.model.SortOrder
import com.flix.music.core.model.Track
import com.flix.music.core.model.byArtist
import com.flix.music.core.model.sortedPlaylistsFor
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlaybackStatus
import com.flix.music.core.player.PlayerController
import com.flix.music.core.player.PlayerState
import com.flix.music.core.ui.WhileUiSubscribed
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration

/**
 * "Seguir escuchando": the track in the player (with what is left and its progress), or
 * the last played one when the player is empty.
 */
data class ResumeItem(
    val track: Track,
    val isInPlayer: Boolean,
    val remaining: Duration? = null,
    val progress: Float? = null,
)

/** A remote section loads and fails on its own (design: "carga por sección"). */
sealed interface RemoteSection<out Content> {
    data object Loading : RemoteSection<Nothing>
    data object Failed : RemoteSection<Nothing>
    data class Loaded<Content>(val content: Content) : RemoteSection<Content>
}

data class HomeUiState(
    val greeting: Greeting = Greeting.Night,
    val resume: ResumeItem? = null,
    val recent: List<Track> = emptyList(),
    val topArtist: ArtistRef? = null,
    val moreFromArtist: RemoteSection<List<Track>> = RemoteSection.Loading,
    val playlists: List<Playlist> = emptyList(),
    val favoriteCount: Int = 0,
    val discoverGenres: List<Genre> = emptyList(),
) {
    /** Nothing played yet: Home shows the first-run invitation instead of empty sections. */
    val isFirstRun: Boolean get() = resume == null && recent.isEmpty()
}

/**
 * Home (design `home`): everything local (Room, player) appears at once; only
 * "Más de …" comes from the server, with its own skeleton and error.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    historyRepository: HistoryRepository,
    favoritesRepository: FavoritesRepository,
    playlistsRepository: PlaylistsRepository,
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
    private val clock: Clock,
) : ViewModel() {

    private val retryRequests = MutableStateFlow(0)

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    private val history = historyRepository.history

    private val topArtist: Flow<ArtistRef?> = history.map { entries -> entries.mostPlayedArtist() }.distinctUntilChanged()

    private val moreFromArtist: Flow<RemoteSection<List<Track>>> =
        combine(topArtist, retryRequests) { artist, _ -> artist }.flatMapLatest { artist ->
            if (artist == null) flowOf(RemoteSection.Loaded(emptyList())) else loadMoreFrom(artist)
        }

    private val localSections = combine(
        history,
        favoritesRepository.favorites,
        playlistsRepository.playlists,
        playerController.state,
    ) { entries, favorites, playlists, playerState ->
        HomeUiState(
            greeting = greetingAt(LocalTime.now(clock)),
            resume = resumeItem(playerState) ?: entries.firstOrNull()?.let { entry -> ResumeItem(entry.track, isInPlayer = false) },
            recent = entries.recentTracks(),
            playlists = playlists.sortedPlaylistsFor(SortOrder.Recent),
            favoriteCount = favorites.size,
            discoverGenres = Genres.all.forDiscovery(LocalDate.now(clock)),
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(localSections, topArtist, moreFromArtist) { local, artist, more ->
        local.copy(topArtist = artist, moreFromArtist = more)
    }.stateIn(viewModelScope, WhileUiSubscribed, HomeUiState())

    /** Resumes the player, or plays the last song again when the player is empty. */
    fun onResume() {
        val resume = uiState.value.resume ?: return
        if (resume.isInPlayer) {
            playerController.togglePlayPause()
        } else {
            playerController.play(listOf(resume.track), 0, PlaybackSource.History)
        }
    }

    fun onRecentClick(track: Track) {
        val recent = uiState.value.recent
        val startIndex = recent.indexOfFirst { it.id == track.id }
        if (startIndex >= 0) playerController.play(recent, startIndex, PlaybackSource.History)
    }

    fun onMoreFromArtistClick(track: Track) {
        val section = uiState.value.moreFromArtist as? RemoteSection.Loaded ?: return
        val artist = uiState.value.topArtist ?: return
        val startIndex = section.content.indexOfFirst { it.id == track.id }
        if (startIndex >= 0) playerController.play(section.content, startIndex, PlaybackSource.Artist(artist.name))
    }

    fun onRetryMoreFromArtist() {
        retryRequests.value++
    }

    private fun resumeItem(playerState: PlayerState): ResumeItem? {
        val track = playerState.current ?: return null
        if (playerState.status == PlaybackStatus.Idle) return null
        val duration = playerState.duration
        return ResumeItem(
            track = track,
            isInPlayer = true,
            remaining = duration?.let { total -> (total - playerState.position).coerceAtLeast(Duration.ZERO) },
            progress = duration?.takeIf { it > Duration.ZERO }?.let { total ->
                (playerState.position / total).toFloat().coerceIn(0f, 1f)
            },
        )
    }

    private fun loadMoreFrom(artist: ArtistRef): Flow<RemoteSection<List<Track>>> = flow {
        emit(RemoteSection.Loading)
        val section = try {
            RemoteSection.Loaded(musicRepository.search(artist.name).byArtist(artist.id, artist.name).take(MORE_FROM_COUNT))
        } catch (exception: FlixException) {
            RemoteSection.Failed
        }
        emit(section)
    }

    private companion object {
        const val MORE_FROM_COUNT = 3
    }
}
