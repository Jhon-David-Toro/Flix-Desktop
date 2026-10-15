package com.flix.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.flix.music.core.ui.window.TrayMenu
import com.flix.music.core.ui.window.TrayMenuItem
import org.junit.Test

class TrayMenuSnapshotTest {

    private val items = listOf(TrayMenuItem("Mostrar Flix") {}, TrayMenuItem("Salir de Flix") {})

    @Test
    fun `menú de la bandeja en oscuro y en claro`() {
        listOf(true to Color(0xFF1F1F1F), false to Color(0xFFE6E6E6)).forEach { (isDark, desktop) ->
            snapshot(if (isDark) "tray-dark" else "tray-light", width = 244, height = 104) {
                Box(Modifier.fillMaxSize().background(desktop)) { TrayMenu(isDark = isDark, items = items) }
            }
        }
    }
}
