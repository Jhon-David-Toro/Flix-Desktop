package com.flix.music.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.designsystem.theme.ThemeMode
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Renders screens to PNG under `build/snapshots` without opening a window, to review
 * the desktop layout. Fonts load asynchronously, so a few frames are drawn first.
 */
internal fun snapshot(name: String, width: Int = 1280, height: Int = 820, content: @Composable () -> Unit) {
    val scene = ImageComposeScene(width, height, Density(1f)) {
        FlixTheme(themeMode = ThemeMode.Dark) { content() }
    }
    repeat(SETTLE_FRAMES) { frame -> scene.render(frame * FRAME_NANOS) }
    Thread.sleep(FONT_LOAD_MILLIS)
    val image = scene.render(SETTLE_FRAMES * FRAME_NANOS)
    val file = File("build/snapshots/$name.png").apply { parentFile.mkdirs() }
    file.writeBytes(requireNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
    scene.close()
}

private const val SETTLE_FRAMES = 30
private const val FRAME_NANOS = 16_000_000L
private const val FONT_LOAD_MILLIS = 600L
