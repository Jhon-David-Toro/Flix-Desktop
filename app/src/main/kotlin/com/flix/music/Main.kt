package com.flix.music

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.designsystem.theme.ThemeMode
import com.flix.music.core.ui.LocalAppGraph
import com.flix.music.resources.Res
import com.flix.music.resources.flix_icon
import com.flix.music.ui.FlixDesktopApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

/** Opens Flix's window and warms NewPipe up, so the first song does not pay for YouTube's player script. */
fun main() = application {
    val graph = remember { AppGraph() }
    remember { graph.applicationScope.launch(Dispatchers.IO) { graph.newPipeSetup.warmUp() } }
    Window(
        onCloseRequest = {
            graph.close()
            exitApplication()
        },
        title = "Flix",
        icon = painterResource(Res.drawable.flix_icon),
        state = rememberWindowState(size = DpSize(1280.dp, 820.dp)),
    ) {
        window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
        CompositionLocalProvider(LocalAppGraph provides graph) {
            FlixTheme(themeMode = ThemeMode.Dark) {
                FlixDesktopApp()
            }
        }
    }
}

private const val MIN_WIDTH = 960
private const val MIN_HEIGHT = 640
