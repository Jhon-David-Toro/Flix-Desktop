package com.flix.music.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.DownloadsRepository
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.model.DownloadState
import com.flix.music.core.model.DownloadStorage
import com.flix.music.core.model.SongDownload
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlayerController
import com.flix.music.core.ui.WhileUiSubscribed
import com.flix.music.core.ui.nowPlaying
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DownloadsUiState {
    data object Loading : DownloadsUiState
    data class Content(val downloads: List<SongDownload>, val storage: DownloadStorage) : DownloadsUiState
}

/** Descargas (design `downloads`): every download with its state, play them offline, cancel, retry or remove. */
class DownloadsViewModel(
    private val downloadsRepository: DownloadsRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlayingMarker> = playerController.nowPlaying(viewModelScope)

    val uiState: StateFlow<DownloadsUiState> = downloadsRepository.downloads
        .map { downloads -> DownloadsUiState.Content(downloads, downloadsRepository.storage()) }
        .stateIn(viewModelScope, WhileUiSubscribed, DownloadsUiState.Loading)

    /** Plays the downloaded songs from [download] onwards; songs still downloading are left out. */
    fun onDownloadClick(download: SongDownload) {
        val downloaded = (uiState.value as? DownloadsUiState.Content)?.downloads.orEmpty()
            .filter { song -> song.state == DownloadState.Downloaded }
            .map(SongDownload::track)
        val startIndex = downloaded.indexOfFirst { track -> track.id == download.track.id }
        if (startIndex >= 0) playerController.play(downloaded, startIndex, PlaybackSource.Downloads)
    }

    fun onRetry(download: SongDownload) = downloadsRepository.download(listOf(download.track))

    /** Cancels a download in flight or deletes a finished one. */
    fun onRemove(download: SongDownload) = downloadsRepository.remove(download.track.id)
}
