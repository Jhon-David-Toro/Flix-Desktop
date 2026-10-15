package com.flix.music.ui

import org.junit.Test

class ShellSnapshotTest {

    @Test
    fun `dibuja la ventana principal`() = snapshot("shell") { FlixDesktopApp() }
}
