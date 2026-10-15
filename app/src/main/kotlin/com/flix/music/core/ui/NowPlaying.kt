package com.flix.music.core.ui

import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.player.PlayerController
import com.flix.music.core.player.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The song lists mark, which changes only when the song or play/pause does. Lists read
 * this instead of [PlayerController.state], whose position ticks four times a second
 * and would recompose the whole screen while scrolling.
 */
fun PlayerController.nowPlaying(scope: CoroutineScope): StateFlow<NowPlayingMarker> =
    state
        .map(PlayerState::toNowPlayingMarker)
        .distinctUntilChanged()
        .stateIn(scope, WhileUiSubscribed, state.value.toNowPlayingMarker())

private fun PlayerState.toNowPlayingMarker() = NowPlayingMarker(current?.id, isActive)
