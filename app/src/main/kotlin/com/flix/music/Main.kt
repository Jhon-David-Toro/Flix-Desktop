package com.flix.music

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.designsystem.theme.ThemeMode
import com.flix.music.core.model.AppTheme
import com.flix.music.core.model.PlaybackModes
import com.flix.music.core.player.RepeatMode
import com.flix.music.core.system.MediaKey
import com.flix.music.core.system.MediaKeys
import com.flix.music.core.system.SingleInstance
import com.flix.music.core.system.WindowMemoryStore
import com.flix.music.core.system.WindowsAutostart
import com.flix.music.core.system.installFileLogging
import com.flix.music.core.ui.LocalAppGraph
import com.flix.music.core.ui.window.FlixTray
import com.flix.music.core.ui.window.LocalWindowFrame
import com.flix.music.core.ui.window.TrayMenuItem
import com.flix.music.core.ui.window.WindowFrame
import com.flix.music.core.ui.window.WindowSurface
import com.flix.music.core.ui.window.roundCornersOnWindows11
import com.flix.music.resources.Res
import com.flix.music.resources.flix_icon
import com.flix.music.resources.tray_quit
import com.flix.music.resources.tray_show
import com.flix.music.ui.FlixDesktopApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension
import java.io.File

/**
 * Starts Flix: the log first, then one instance only (a second launch brings this
 * window forward), the window as it was left, the music where it was, the tray icon
 * and the media keys. Closing the window keeps Flix playing in the tray; "Salir de
 * Flix" there saves the session and quits.
 */
@OptIn(FlowPreview::class)
fun main(args: Array<String>) {
    val dataDirectory = AppGraph.defaultDataDirectory()
    installFileLogging(dataDirectory)
    val instance = SingleInstance.acquire(dataDirectory) ?: return
    val windowMemoryStore = WindowMemoryStore(File(dataDirectory, "window.json"), Json { ignoreUnknownKeys = true })
    val remembered = windowMemoryStore.read()

    application {
        val graph = remember { AppGraph(dataDirectory) }
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .diskCache { DiskCache.Builder().directory(graph.cacheDirectory.resolve("images").toOkioPath()).maxSizeBytes(IMAGE_CACHE_BYTES).build() }
                .build()
        }
        val settings by graph.settingsRepository.settings.collectAsState(initial = null)
        val playerState by graph.player.state.collectAsState()
        var volume by remember { mutableIntStateOf(remembered.volume) }
        var composeWindow by remember { mutableStateOf<ComposeWindow?>(null) }
        var windowVisible by remember { mutableStateOf(true) }
        val windowState = rememberWindowState(
            placement = if (remembered.maximized) WindowPlacement.Maximized else WindowPlacement.Floating,
            position = if (remembered.x != null && remembered.y != null) WindowPosition(remembered.x.dp, remembered.y.dp) else WindowPosition.PlatformDefault,
            size = DpSize(remembered.width.dp, remembered.height.dp),
            isMinimized = WindowsAutostart.MINIMIZED_ARGUMENT in args,
        )
        val showWindow = {
            windowVisible = true
            windowState.isMinimized = false
            composeWindow?.toFront()
            composeWindow?.requestFocus()
            Unit
        }
        val hideToTray = { windowVisible = false }
        val closeApp = {
            graph.sessionStore.write(graph.player.session())
            windowMemoryStore.write(windowMemoryStore.read().copy(volume = volume))
            instance.release()
            graph.close()
            exitApplication()
        }

        LaunchedEffect(graph) {
            launch(Dispatchers.IO) { graph.newPipeSetup.warmUp() }
            graph.player.setVolume(volume)
            graph.sessionStore.read()?.let(graph.player::restore)
            launch {
                while (true) {
                    delay(SESSION_SAVE_MILLIS)
                    graph.player.session()?.let { session -> launch(Dispatchers.IO) { graph.sessionStore.write(session) } }
                }
            }
            instance.listen { graph.applicationScope.launch { showWindow() } }
            MediaKeys { key ->
                graph.applicationScope.launch {
                    when (key) {
                        MediaKey.PlayPause -> graph.player.togglePlayPause()
                        MediaKey.Next -> graph.player.skipToNext()
                        MediaKey.Previous -> graph.player.skipToPrevious()
                    }
                }
            }.start()
            val startSettings = graph.settingsRepository.settings.first()
            val modes = startSettings.playbackModes
            if (startSettings.rememberPlaybackModes && modes != null) {
                graph.player.restoreModes(modes.shuffle, RepeatMode.entries.firstOrNull { mode -> mode.name == modes.repeatMode } ?: RepeatMode.Off)
            }
            graph.player.state
                .map { state -> PlaybackModes(state.shuffleEnabled, state.repeatMode.name) }
                .distinctUntilChanged()
                .collect { current -> runCatching { graph.settingsRepository.savePlaybackModes(current) } }
        }
        LaunchedEffect(windowState) {
            snapshotFlow { Triple(windowState.position, windowState.size, windowState.placement) }
                .debounce(SAVE_DELAY_MILLIS)
                .collect { (position, size, placement) ->
                    val previous = windowMemoryStore.read()
                    val floating = placement == WindowPlacement.Floating && position is WindowPosition.Absolute
                    windowMemoryStore.write(
                        if (floating) {
                            previous.copy(
                                x = position.x.value.toInt(),
                                y = position.y.value.toInt(),
                                width = size.width.value.toInt(),
                                height = size.height.value.toInt(),
                                maximized = false,
                                volume = volume,
                            )
                        } else {
                            previous.copy(maximized = placement == WindowPlacement.Maximized, volume = volume)
                        },
                    )
                }
        }

        val isDark = when (settings?.theme) {
            AppTheme.Dark -> true
            AppTheme.Light -> false
            else -> isSystemInDarkTheme()
        }
        FlixTray(
            icon = painterResource(Res.drawable.flix_icon),
            tooltip = playerState.current?.let { track -> "${track.title} · ${track.artistNames}" } ?: "Flix",
            isDark = isDark,
            onShow = showWindow,
            items = listOf(
                TrayMenuItem(stringResource(Res.string.tray_show), showWindow),
                TrayMenuItem(stringResource(Res.string.tray_quit), closeApp),
            ),
        )

        Window(
            onCloseRequest = hideToTray,
            visible = windowVisible,
            title = playerState.current?.let { track -> "${track.title} · Flix" } ?: "Flix",
            icon = painterResource(Res.drawable.flix_icon),
            state = windowState,
            undecorated = true,
        ) {
            val frame = remember(window) {
                WindowFrame(window, windowState, onMinimize = { windowState.isMinimized = true }, onClose = hideToTray)
            }
            LaunchedEffect(window) {
                composeWindow = window
                window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
                window.roundCornersOnWindows11()
            }
            CompositionLocalProvider(LocalAppGraph provides graph, LocalWindowFrame provides frame) {
                FlixTheme(themeMode = settings?.theme?.toThemeMode() ?: ThemeMode.System) {
                    WindowSurface {
                        FlixDesktopApp(
                            volume = volume,
                            onVolumeChange = { percent ->
                                volume = percent
                                graph.player.setVolume(percent)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun AppTheme.toThemeMode(): ThemeMode = when (this) {
    AppTheme.System -> ThemeMode.System
    AppTheme.Light -> ThemeMode.Light
    AppTheme.Dark -> ThemeMode.Dark
}

private const val IMAGE_CACHE_BYTES = 256L * 1024 * 1024
private const val SAVE_DELAY_MILLIS = 500L
private const val SESSION_SAVE_MILLIS = 15_000L
private const val MIN_WIDTH = 960
private const val MIN_HEIGHT = 640
