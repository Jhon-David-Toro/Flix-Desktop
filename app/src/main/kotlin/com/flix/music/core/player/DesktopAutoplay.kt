package com.flix.music.core.player

import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.model.FlixException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Autoplay on the desktop, as on the phone: once the last song of the queue starts
 * (repeat off), its YouTube Music radio is appended so the music goes on; once per song,
 * and tried again on a later change if the request failed (e.g. offline).
 */
class DesktopAutoplay(
    private val player: DesktopPlayerController,
    private val musicRepository: MusicRepository,
    private val autoplayEnabled: Flow<Boolean>,
    private val scope: CoroutineScope,
) {

    private var seedId: String? = null

    fun start() {
        scope.launch {
            combine(player.state, autoplayEnabled.distinctUntilChanged()) { state, enabled -> state to enabled }
                .collect { (state, enabled) -> continueWithRadio(state, enabled) }
        }
    }

    private fun continueWithRadio(state: PlayerState, enabled: Boolean) {
        val current = state.current ?: return
        if (!enabled || state.hasNext || state.repeatMode != RepeatMode.Off || current.id == seedId) return
        seedId = current.id
        scope.launch {
            val radio = try {
                musicRepository.radio(current)
            } catch (exception: FlixException) {
                seedId = null
                return@launch
            }
            player.appendToQueue(autoplayAdditions(radio, player.queuedTrackIds()))
        }
    }
}
