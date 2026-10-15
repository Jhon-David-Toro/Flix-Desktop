package com.flix.music.testing

import com.flix.music.core.data.repository.DownloadsRepository
import com.flix.music.core.model.DownloadState
import com.flix.music.core.model.DownloadStorage
import com.flix.music.core.model.SongDownload
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Downloads that finish at once; tests can set any state with [setDownloads]. */
class FakeDownloadsRepository(initial: List<SongDownload> = emptyList()) : DownloadsRepository {

    private val mutableDownloads = MutableStateFlow(initial)

    override val downloads: StateFlow<List<SongDownload>> = mutableDownloads

    val current: List<SongDownload> get() = mutableDownloads.value

    fun setDownloads(downloads: List<SongDownload>) {
        mutableDownloads.value = downloads
    }

    override fun download(tracks: List<Track>) {
        val pending = current.filter { download -> download.state != DownloadState.Failed }.map { download -> download.track.id }
        val added = tracks.distinctBy(Track::id).filterNot { track -> track.id in pending }
        mutableDownloads.value = added.map { track -> SongDownload(track, DownloadState.Downloaded, 100) } +
            current.filterNot { download -> download.track.id in added.map(Track::id) }
    }

    override fun remove(trackId: String) {
        mutableDownloads.value = current.filterNot { download -> download.track.id == trackId }
    }

    override fun storage(): DownloadStorage = DownloadStorage(usedBytes = 0, totalBytes = 0)
}
