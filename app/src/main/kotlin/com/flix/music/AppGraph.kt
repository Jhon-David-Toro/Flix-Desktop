package com.flix.music

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.flix.music.core.data.database.FlixDatabase
import com.flix.music.core.data.lyrics.LrclibClient
import com.flix.music.core.data.lyrics.SavedLyricsStore
import com.flix.music.core.data.repository.CacheRepository
import com.flix.music.core.data.repository.CacheRepositoryImpl
import com.flix.music.core.data.repository.DesktopDownloadsRepository
import com.flix.music.core.data.repository.DownloadsRepository
import com.flix.music.core.data.repository.FavoritesRepository
import com.flix.music.core.data.repository.FavoritesRepositoryImpl
import com.flix.music.core.data.repository.FollowedArtistsRepository
import com.flix.music.core.data.repository.FollowedArtistsRepositoryImpl
import com.flix.music.core.data.repository.HistoryRepository
import com.flix.music.core.data.repository.HistoryRepositoryImpl
import com.flix.music.core.data.repository.LibraryLayoutRepository
import com.flix.music.core.data.repository.LibraryLayoutRepositoryImpl
import com.flix.music.core.data.repository.LyricsRepository
import com.flix.music.core.data.repository.LyricsRepositoryImpl
import com.flix.music.core.data.repository.MusicRepository
import com.flix.music.core.data.repository.MusicRepositoryImpl
import com.flix.music.core.data.repository.PlaylistsRepository
import com.flix.music.core.data.repository.PlaylistsRepositoryImpl
import com.flix.music.core.data.repository.RecentSearchesRepository
import com.flix.music.core.data.repository.RecentSearchesRepositoryImpl
import com.flix.music.core.data.repository.SavedAlbumsRepository
import com.flix.music.core.data.repository.SavedAlbumsRepositoryImpl
import com.flix.music.core.data.repository.SettingsRepository
import com.flix.music.core.data.repository.SettingsRepositoryImpl
import com.flix.music.core.data.youtube.NewPipeSetup
import com.flix.music.core.data.youtube.YouTubeMusicClient
import com.flix.music.core.player.DesktopAutoplay
import com.flix.music.core.player.DesktopPlayerController
import com.flix.music.core.player.mpv.MpvEngine
import com.flix.music.core.player.stream.AudioServer
import com.flix.music.core.player.stream.NewPipeStreamResolver
import com.flix.music.core.system.WindowsAutostart
import com.flix.music.feature.album.AlbumViewModel
import com.flix.music.feature.artist.ArtistViewModel
import com.flix.music.feature.downloads.DownloadsViewModel
import com.flix.music.feature.favorites.FavoritesViewModel
import com.flix.music.feature.genre.GenreViewModel
import com.flix.music.feature.history.HistoryViewModel
import com.flix.music.feature.home.HomeViewModel
import com.flix.music.feature.library.LibraryViewModel
import com.flix.music.feature.player.LyricsViewModel
import com.flix.music.feature.player.NowPlayingViewModel
import com.flix.music.feature.playlist.PlaylistEditViewModel
import com.flix.music.feature.playlist.PlaylistViewModel
import com.flix.music.feature.profile.ProfileViewModel
import com.flix.music.feature.profile.StatsViewModel
import com.flix.music.feature.search.SearchViewModel
import com.flix.music.feature.settings.SettingsViewModel
import com.flix.music.feature.song.SongViewModel
import com.flix.music.feature.trackmenu.TrackMenuViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    /** Artwork and other caches, emptied from Ajustes. */
    val cacheDirectory = File(dataDirectory, "cache").apply { mkdirs() }

    private val preferences: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(dataDirectory, "flix_preferences.preferences_pb") },
    )

    private val database: FlixDatabase = Room.databaseBuilder<FlixDatabase>(name = File(dataDirectory, "flix.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    val newPipeSetup = NewPipeSetup(okHttpClient)

    private val streamResolver = NewPipeStreamResolver(newPipeSetup, clock)

    val musicRepository: MusicRepository = MusicRepositoryImpl(YouTubeMusicClient(okHttpClient, json, clock), newPipeSetup)
    val recentSearchesRepository: RecentSearchesRepository = RecentSearchesRepositoryImpl(preferences, json)
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(preferences)
    val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(database.favoritesDao())
    val historyRepository: HistoryRepository = HistoryRepositoryImpl(database.historyDao())
    val playlistsRepository: PlaylistsRepository = PlaylistsRepositoryImpl(database.playlistsDao(), clock)
    val followedArtistsRepository: FollowedArtistsRepository = FollowedArtistsRepositoryImpl(database.followedArtistsDao(), clock)
    val savedAlbumsRepository: SavedAlbumsRepository = SavedAlbumsRepositoryImpl(database.savedAlbumsDao(), clock)
    val libraryLayoutRepository: LibraryLayoutRepository = LibraryLayoutRepositoryImpl(preferences)
    val cacheRepository: CacheRepository = CacheRepositoryImpl(cacheDirectory)

    private val savedLyricsStore = SavedLyricsStore(File(dataDirectory, "lyrics"), json)
    val lyricsRepository: LyricsRepository = LyricsRepositoryImpl(LrclibClient(okHttpClient, json), savedLyricsStore)

    private val desktopDownloads = DesktopDownloadsRepository(
        directory = File(dataDirectory, "downloads"),
        okHttpClient = okHttpClient,
        streamResolver = streamResolver,
        lyricsRepository = lyricsRepository,
        savedLyricsStore = savedLyricsStore,
        json = json,
    )
    val downloadsRepository: DownloadsRepository = desktopDownloads

    private val audioServer = AudioServer(okHttpClient, streamResolver, desktopDownloads::downloadedFile)

    val player = DesktopPlayerController(
        engineFactory = { onSignal -> MpvEngine(libmpvFile(), onSignal) },
        urlOf = audioServer::urlOf,
        streamResolver = streamResolver,
        scope = applicationScope,
        onFirstPlay = { track -> applicationScope.launch { runCatching { historyRepository.record(track) } } },
    )

    private val autostart = WindowsAutostart()

    private val autoplay = DesktopAutoplay(
        player = player,
        musicRepository = musicRepository,
        autoplayEnabled = settingsRepository.settings.map { settings -> settings.autoplay },
        scope = applicationScope,
    )

    init {
        autoplay.start()
    }

    /** Builds the screen ViewModels, as Hilt does on the phone; [savedStateHandle] carries navigation arguments. */
    @Suppress("UNCHECKED_CAST")
    fun <Model : ViewModel> viewModel(type: KClass<Model>, savedStateHandle: SavedStateHandle): Model = when (type) {
        HomeViewModel::class -> HomeViewModel(historyRepository, favoritesRepository, playlistsRepository, musicRepository, player, clock)
        SearchViewModel::class -> SearchViewModel(musicRepository, recentSearchesRepository, player)
        GenreViewModel::class -> GenreViewModel(savedStateHandle, musicRepository, player)
        LibraryViewModel::class -> LibraryViewModel(
            favoritesRepository, historyRepository, playlistsRepository, savedAlbumsRepository,
            followedArtistsRepository, libraryLayoutRepository, player, clock,
        )
        FavoritesViewModel::class -> FavoritesViewModel(favoritesRepository, player)
        HistoryViewModel::class -> HistoryViewModel(historyRepository, player, clock)
        PlaylistViewModel::class -> PlaylistViewModel(savedStateHandle, playlistsRepository, player, downloadsRepository)
        PlaylistEditViewModel::class -> PlaylistEditViewModel(savedStateHandle, playlistsRepository)
        AlbumViewModel::class -> AlbumViewModel(savedStateHandle, musicRepository, savedAlbumsRepository, player)
        ArtistViewModel::class -> ArtistViewModel(savedStateHandle, musicRepository, followedArtistsRepository, player)
        SongViewModel::class -> SongViewModel(savedStateHandle, musicRepository, favoritesRepository, followedArtistsRepository, player)
        ProfileViewModel::class -> ProfileViewModel(historyRepository, playlistsRepository, favoritesRepository, followedArtistsRepository, clock)
        StatsViewModel::class -> StatsViewModel(historyRepository, clock)
        DownloadsViewModel::class -> DownloadsViewModel(downloadsRepository, player)
        TrackMenuViewModel::class -> TrackMenuViewModel(favoritesRepository, playlistsRepository, player, downloadsRepository)
        LyricsViewModel::class -> LyricsViewModel(lyricsRepository, player)
        NowPlayingViewModel::class -> NowPlayingViewModel(player, favoritesRepository)
        SettingsViewModel::class -> SettingsViewModel(settingsRepository, autostart, VERSION)
        else -> error("No ViewModel factory for ${type.simpleName}")
    } as Model

    fun close() {
        player.release()
        audioServer.stop()
        database.close()
    }

    companion object {
        /** Keep in step with `packageVersion` in app/build.gradle.kts. */
        const val VERSION = "1.0.0"
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 30L

        private fun defaultDataDirectory(): File {
            val appData = System.getenv("APPDATA")
            val directory = if (appData != null) File(appData, "Flix") else File(System.getProperty("user.home"), ".flix")
            return directory.apply { mkdirs() }
        }

        /** Packaged next to the app; in development, Gradle points here at `app/resources/windows`. */
        private fun libmpvFile(): File {
            val resources = System.getProperty("compose.application.resources.dir")
            return if (resources != null) File(resources, "libmpv-2.dll") else File("resources/windows/libmpv-2.dll")
        }
    }
}
