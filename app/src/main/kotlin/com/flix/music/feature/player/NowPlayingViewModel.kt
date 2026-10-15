package com.flix.music.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The desktop Now Playing view: whether the song is a favorite and the heart that toggles it. */
class NowPlayingViewModel(
    private val playerController: PlayerController,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val isCurrentFavorite: StateFlow<Boolean> =
        combine(playerController.state, favoritesRepository.favoriteIds) { playerState, favoriteIds ->
            playerState.current?.id in favoriteIds
        }.stateIn(viewModelScope, WhileUiSubscribed, false)

    fun onToggleFavorite() {
        val track = playerController.state.value.current ?: return
        viewModelScope.launch { favoritesRepository.toggle(track) }
    }
}
