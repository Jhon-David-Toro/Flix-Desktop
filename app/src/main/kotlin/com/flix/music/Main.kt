package com.flix.music

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.designsystem.theme.ThemeMode
import com.flix.music.resources.Res
import com.flix.music.resources.flix_icon
import com.flix.music.ui.FlixDesktopApp
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Flix",
        icon = painterResource(Res.drawable.flix_icon),
        state = rememberWindowState(size = DpSize(1280.dp, 820.dp)),
    ) {
        window.minimumSize = java.awt.Dimension(960, 640)
        FlixTheme(themeMode = ThemeMode.Dark) {
            FlixDesktopApp()
        }
    }
}
