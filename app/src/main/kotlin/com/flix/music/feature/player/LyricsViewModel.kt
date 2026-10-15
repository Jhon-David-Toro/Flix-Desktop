package com.flix.music.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.LyricsRepository
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.Lyrics
import com.flix.music.core.model.Track
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
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
import kotlin.time.Duration

sealed interface LyricsUiState {
    data object Loading : LyricsUiState
    data object Unavailable : LyricsUiState
    data class Failed(val error: FlixError) : LyricsUiState
    data class Ready(val lyrics: Lyrics) : LyricsUiState
}

/** Letra (design `lyricsnone` / `lyricssync`): the lyrics of whatever is playing. */
@OptIn(ExperimentalCoroutinesApi::class)
class LyricsViewModel(
    private val lyricsRepository: LyricsRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val retryRequests = MutableStateFlow(0)

    private val currentTrack: Flow<Track?> = playerController.state
        .map { playerState -> playerState.current }
        .distinctUntilChanged { previous, next -> previous?.id == next?.id }

    val uiState: StateFlow<LyricsUiState> = combine(currentTrack, retryRequests) { track, _ -> track }
        .flatMapLatest { track -> if (track == null) flowOf(LyricsUiState.Unavailable) else load(track) }
        .stateIn(viewModelScope, WhileUiSubscribed, LyricsUiState.Loading)

    fun onRetry() {
        retryRequests.value++
    }

    /** Tapping a synced line jumps the song there (design `lyricssync`). */
    fun onLineClick(start: Duration) = playerController.seekTo(start)

    private fun load(track: Track): Flow<LyricsUiState> = flow {
        emit(LyricsUiState.Loading)
        val state = try {
            lyricsRepository.lyrics(track)?.let(LyricsUiState::Ready) ?: LyricsUiState.Unavailable
        } catch (exception: FlixException) {
            LyricsUiState.Failed(exception.error)
        }
        emit(state)
    }
}
