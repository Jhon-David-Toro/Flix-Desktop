package com.flix.music.core.data.youtube

import com.flix.music.core.common.Log
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager

/**
 * Configures NewPipeExtractor once for the whole app (song details and audio
 * streams). Call [ensureInitialized] before any NewPipe call, off the main thread.
 */
class NewPipeSetup(private val okHttpClient: OkHttpClient) {

    private val initialization = lazy {
        NewPipe.init(NewPipeDownloader(okHttpClient), Localization(LANGUAGE, COUNTRY))
    }

    fun ensureInitialized() {
        initialization.value
    }

    /**
     * Downloads and caches YouTube's player script ahead of time; measured to save about
     * a second on the first song. Failures are ignored: the first resolve fetches it then.
     */
    fun warmUp() {
        ensureInitialized()
        runCatching { YoutubeJavaScriptPlayerManager.getSignatureTimestamp(WARM_UP_VIDEO_ID) }
            .onFailure { failure -> Log.w(TAG, "NewPipe warm-up failed: ${failure.message}") }
    }

    private companion object {
        const val TAG = "NewPipeSetup"
        const val LANGUAGE = "en"
        const val COUNTRY = "US"
        const val WARM_UP_VIDEO_ID = "dQw4w9WgXcQ"
    }
}
