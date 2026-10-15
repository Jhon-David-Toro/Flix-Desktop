package com.flix.music.core.data.repository

import com.flix.music.core.common.Log
import com.flix.music.core.data.database.StoredTrack
import com.flix.music.core.data.database.toStoredTrack
import com.flix.music.core.data.database.toTrack
import com.flix.music.core.data.lyrics.SavedLyricsStore
import com.flix.music.core.model.DownloadState
import com.flix.music.core.model.DownloadStorage
import com.flix.music.core.model.FlixException
import com.flix.music.core.model.SongDownload
import com.flix.music.core.model.Track
import com.flix.music.core.player.stream.StreamResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** One line of `downloads.json`: enough to list the song offline and resume it. */
@Serializable
private data class StoredDownload(val track: StoredTrack, val state: DownloadState, val addedAtMillis: Long)

/**
 * Downloads on the desktop, where Media3's downloader does not exist: each song is
 * saved as `<id>.m4a` in [directory], two at a time, written to a `.part` file that
 * is renamed only once complete. `downloads.json` keeps the list (newest first), so
 * it renders offline and unfinished downloads resume at the next launch. A URL
 * googlevideo rejects is resolved again once. Lyrics are saved next to each song.
 */
class DesktopDownloadsRepository(
    private val directory: File,
    private val okHttpClient: OkHttpClient,
    private val streamResolver: StreamResolver,
    private val lyricsRepository: LyricsRepository,
    private val savedLyricsStore: SavedLyricsStore,
    private val json: Json,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : DownloadsRepository {

    private val indexFile = File(directory, "downloads.json")
    private val slots = Semaphore(PARALLEL_DOWNLOADS)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val addedAt = ConcurrentHashMap<String, Long>()

    private val _downloads = MutableStateFlow(readIndex())
    override val downloads: StateFlow<List<SongDownload>> = _downloads.asStateFlow()

    init {
        directory.mkdirs()
        _downloads.value
            .filter { song -> song.state == DownloadState.Queued || song.state == DownloadState.Downloading }
            .forEach { song -> start(song.track) }
    }

    /** The finished file of [trackId], which the audio server plays instead of the network. */
    fun downloadedFile(trackId: String): File? =
        audioFile(trackId).takeIf { file -> file.isFile && stateOf(trackId) == DownloadState.Downloaded }

    override fun download(tracks: List<Track>) {
        val pending = _downloads.value.filter { song -> song.state != DownloadState.Failed }.mapTo(HashSet()) { song -> song.track.id }
        val newTracks = tracks.distinctBy(Track::id).filterNot { track -> track.id in pending }
        if (newTracks.isEmpty()) return
        newTracks.forEach { track -> addedAt[track.id] = nowMillis() }
        _downloads.update { current ->
            newTracks.reversed().map { track -> SongDownload(track, DownloadState.Queued) } +
                current.filterNot { song -> newTracks.any { track -> track.id == song.track.id } }
        }
        saveIndex()
        newTracks.forEach(::start)
    }

    override fun remove(trackId: String) {
        jobs.remove(trackId)?.cancel()
        _downloads.update { current -> current.filterNot { song -> song.track.id == trackId } }
        addedAt.remove(trackId)
        saveIndex()
        scope.launch {
            audioFile(trackId).delete()
            partFile(trackId).delete()
            savedLyricsStore.delete(trackId)
        }
    }

    override fun storage(): DownloadStorage = DownloadStorage(
        usedBytes = directory.listFiles()?.filter { file -> file.extension == AUDIO_EXTENSION }?.sumOf(File::length) ?: 0L,
        totalBytes = directory.totalSpace,
    )

    private fun start(track: Track) {
        jobs[track.id] = scope.launch {
            slots.withPermit {
                setState(track.id, DownloadState.Downloading, percent = 0)
                val succeeded = runCatching { fetch(track.id, retryOnRejection = true) }
                    .onFailure { failure -> Log.w(TAG, "Download of ${track.id} failed: ${failure.message}") }
                    .isSuccess
                if (!isActive) return@withPermit
                setState(track.id, if (succeeded) DownloadState.Downloaded else DownloadState.Failed, percent = if (succeeded) FULL else 0)
                saveIndex()
                if (succeeded) saveLyrics(track)
            }
            jobs.remove(track.id)
        }
    }

    private fun fetch(trackId: String, retryOnRejection: Boolean) {
        val url = streamResolver.resolve(trackId) ?: throw IOException("YouTube gave no audio for $trackId")
        val request = Request.Builder().url(url).header("Range", "bytes=0-").build()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == FORBIDDEN && retryOnRejection) {
                streamResolver.forget(trackId)
                return fetch(trackId, retryOnRejection = false)
            }
            if (!response.isSuccessful) throw IOException("googlevideo answered HTTP ${response.code}")
            val total = response.body.contentLength()
            val part = partFile(trackId)
            var written = 0L
            var lastPercent = -1
            part.outputStream().use { output ->
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        val percent = if (total > 0) (written * FULL / total).toInt() else 0
                        if (percent != lastPercent) {
                            lastPercent = percent
                            setState(trackId, DownloadState.Downloading, percent)
                        }
                    }
                }
            }
            if (total > 0 && written != total) throw IOException("Incomplete download: $written of $total bytes")
            val target = audioFile(trackId)
            target.delete()
            if (!part.renameTo(target)) throw IOException("Could not move ${part.name}")
        }
    }

    private suspend fun saveLyrics(track: Track) {
        val lyrics = try {
            lyricsRepository.lyrics(track)
        } catch (exception: FlixException) {
            null
        } ?: return
        savedLyricsStore.write(track.id, lyrics)
    }

    private fun setState(trackId: String, state: DownloadState, percent: Int) {
        _downloads.update { current ->
            current.map { song -> if (song.track.id == trackId) song.copy(state = state, percent = percent) else song }
        }
    }

    private fun stateOf(trackId: String): DownloadState? = _downloads.value.firstOrNull { song -> song.track.id == trackId }?.state

    @Synchronized
    private fun saveIndex() {
        val stored = _downloads.value.map { song ->
            StoredDownload(song.track.toStoredTrack(), song.state, addedAt[song.track.id] ?: nowMillis())
        }
        runCatching {
            val temporary = File(directory, "$INDEX_NAME.tmp")
            temporary.writeText(json.encodeToString(stored))
            indexFile.delete()
            temporary.renameTo(indexFile)
        }.onFailure { failure -> Log.w(TAG, "Could not save the downloads list: ${failure.message}") }
    }

    private fun readIndex(): List<SongDownload> {
        if (!indexFile.isFile) return emptyList()
        return runCatching { json.decodeFromString<List<StoredDownload>>(indexFile.readText()) }
            .getOrElse { failure ->
                Log.w(TAG, "Unreadable downloads list: ${failure.message}")
                emptyList()
            }
            .sortedByDescending(StoredDownload::addedAtMillis)
            .map { stored ->
                addedAt[stored.track.trackId] = stored.addedAtMillis
                val percent = if (stored.state == DownloadState.Downloaded) FULL else 0
                SongDownload(stored.track.toTrack(), stored.state, percent)
            }
    }

    private fun audioFile(trackId: String) = File(directory, "$trackId.$AUDIO_EXTENSION")

    private fun partFile(trackId: String) = File(directory, "$trackId.$AUDIO_EXTENSION.part")

    private companion object {
        const val TAG = "Downloads"
        const val INDEX_NAME = "downloads.json"
        const val AUDIO_EXTENSION = "m4a"
        const val PARALLEL_DOWNLOADS = 2
        const val BUFFER_BYTES = 64 * 1024
        const val FULL = 100
        const val FORBIDDEN = 403
    }
}
