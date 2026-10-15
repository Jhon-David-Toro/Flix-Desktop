package com.flix.music

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.data.repository.MusicRepositoryImpl
import com.flix.music.core.data.repository.RecentSearchesRepository
import com.flix.music.core.data.repository.RecentSearchesRepositoryImpl
import com.flix.music.core.data.youtube.NewPipeSetup
import com.flix.music.core.data.youtube.YouTubeMusicClient
import com.flix.music.core.player.DesktopPlayerController
import com.flix.music.core.player.mpv.MpvEngine
import com.flix.music.core.player.stream.AudioServer
import com.flix.music.core.player.stream.NewPipeStreamResolver
import com.flix.music.feature.search.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Every long-lived object of the desktop app, built once at launch: what Hilt's modules
 * provide on the phone. Data lives in `%APPDATA%\Flix` (or `~/.flix` elsewhere).
 */
class AppGraph(val dataDirectory: File = defaultDataDirectory()) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val json = Json { ignoreUnknownKeys = true }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val clock: Clock = Clock.systemDefaultZone()

    private val preferences: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(dataDirectory, "flix_preferences.preferences_pb") },
    )

    val newPipeSetup = NewPipeSetup(okHttpClient)

    private val streamResolver = NewPipeStreamResolver(newPipeSetup, clock)

    private val audioServer = AudioServer(okHttpClient, streamResolver)

    val player = DesktopPlayerController(
        engineFactory = { onSignal -> MpvEngine(libmpvFile(), onSignal) },
        urlOf = audioServer::urlOf,
        streamResolver = streamResolver,
        scope = applicationScope,
    )

    val musicRepository: MusicRepository = MusicRepositoryImpl(YouTubeMusicClient(okHttpClient, json, clock), newPipeSetup)

    val recentSearchesRepository: RecentSearchesRepository = RecentSearchesRepositoryImpl(preferences, json)

    /** Builds the screen ViewModels, as Hilt does on the phone; [savedStateHandle] carries navigation arguments. */
    @Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")
    fun <Model : ViewModel> viewModel(type: KClass<Model>, savedStateHandle: SavedStateHandle): Model = when (type) {
        SearchViewModel::class -> SearchViewModel(musicRepository, recentSearchesRepository, player)
        else -> error("No ViewModel factory for ${type.simpleName}")
    } as Model

    fun close() {
        player.release()
        audioServer.stop()
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val READ_TIMEOUT_SECONDS = 30L

        fun defaultDataDirectory(): File {
            val appData = System.getenv("APPDATA")
            val directory = if (appData != null) File(appData, "Flix") else File(System.getProperty("user.home"), ".flix")
            return directory.apply { mkdirs() }
        }

        /** Packaged next to the app; in development, Gradle points here at `app/resources/windows`. */
        fun libmpvFile(): File {
            val resources = System.getProperty("compose.application.resources.dir")
            return if (resources != null) File(resources, "libmpv-2.dll") else File("resources/windows/libmpv-2.dll")
        }
    }
}
