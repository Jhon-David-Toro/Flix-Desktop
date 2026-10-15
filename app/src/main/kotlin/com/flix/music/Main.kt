package com.flix.music

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.designsystem.theme.ThemeMode
import com.flix.music.core.model.AppTheme
import com.flix.music.core.system.WindowsAutostart
import com.flix.music.core.ui.LocalAppGraph
import com.flix.music.core.ui.window.LocalWindowFrame
import com.flix.music.core.ui.window.WindowFrame
import com.flix.music.core.ui.window.WindowSurface
import com.flix.music.core.ui.window.roundCornersOnWindows11
import com.flix.music.resources.Res
import com.flix.music.resources.flix_icon
import com.flix.music.ui.FlixDesktopApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

/** Opens Flix's window and warms NewPipe up, so the first song does not pay for YouTube's player script. */
fun main(args: Array<String>) = application {
    val graph = remember { AppGraph() }
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .diskCache { DiskCache.Builder().directory(graph.cacheDirectory.resolve("images").toOkioPath()).maxSizeBytes(IMAGE_CACHE_BYTES).build() }
            .build()
    }
    remember { graph.applicationScope.launch(Dispatchers.IO) { graph.newPipeSetup.warmUp() } }
    val settings by graph.settingsRepository.settings.collectAsState(initial = null)
    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 820.dp),
        isMinimized = WindowsAutostart.MINIMIZED_ARGUMENT in args,
    )
    val closeApp = {
        graph.close()
        exitApplication()
    }
    Window(
        onCloseRequest = closeApp,
        title = "Flix",
        icon = painterResource(Res.drawable.flix_icon),
        state = windowState,
        undecorated = true,
    ) {
        val frame = remember(window) {
            WindowFrame(window, windowState, onMinimize = { windowState.isMinimized = true }, onClose = closeApp)
        }
        LaunchedEffect(window) {
            window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
            window.roundCornersOnWindows11()
        }
        CompositionLocalProvider(LocalAppGraph provides graph, LocalWindowFrame provides frame) {
            FlixTheme(themeMode = settings?.theme?.toThemeMode() ?: ThemeMode.System) {
                WindowSurface { FlixDesktopApp() }
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
private const val MIN_WIDTH = 960
private const val MIN_HEIGHT = 640
