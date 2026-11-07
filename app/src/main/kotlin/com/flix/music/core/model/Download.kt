package com.flix.music.core.model

/** Where a song's download is (design "Descargas y offline"). */
enum class DownloadState { Queued, Downloading, Downloaded, Failed }

data class SongDownload(val track: Track, val state: DownloadState, val percent: Int = 0)

/** Space taken by downloads, against the phone's storage. */
data class DownloadStorage(val usedBytes: Long, val totalBytes: Long) {

    val usedFraction: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

/** What a "download all" button shows for a list of songs. */
enum class DownloadAllState { None, InProgress, Done }

fun List<Track>.downloadAllState(downloads: List<SongDownload>): DownloadAllState {
    if (isEmpty()) return DownloadAllState.None
    val states = downloads.associate { download -> download.track.id to download.state }
    return when {
        all { track -> states[track.id] == DownloadState.Downloaded } -> DownloadAllState.Done
        any { track -> states[track.id] == DownloadState.Queued || states[track.id] == DownloadState.Downloading } -> DownloadAllState.InProgress
        else -> DownloadAllState.None
    }
}
