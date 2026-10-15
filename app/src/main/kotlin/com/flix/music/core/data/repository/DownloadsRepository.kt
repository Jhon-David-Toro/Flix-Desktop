package com.flix.music.core.data.repository

import com.flix.music.core.model.DownloadStorage
import com.flix.music.core.model.SongDownload
import com.flix.music.core.model.Track
import kotlinx.coroutines.flow.Flow

/** Songs saved on the phone, with their lyrics, to play without connection. */
interface DownloadsRepository {

    /** Newest first, with live progress while something downloads. */
    val downloads: Flow<List<SongDownload>>

    /** Queues the [tracks] not downloaded or queued yet; failed ones are retried. */
    fun download(tracks: List<Track>)

    /** Cancels or deletes the download of [trackId]. */
    fun remove(trackId: String)

    fun storage(): DownloadStorage
}
