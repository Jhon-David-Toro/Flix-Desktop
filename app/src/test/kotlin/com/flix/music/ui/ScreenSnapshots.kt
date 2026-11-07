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
internal fun snapshot(
    name: String,
    width: Int = 1280,
    height: Int = 820,
    themeMode: ThemeMode = ThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    coil3.SingletonImageLoader.setSafe { ctx -> coil3.ImageLoader.Builder(ctx).components { add(LocalFileFetcher) }.build() }
    val scene = ImageComposeScene(width, height, Density(1f)) {
        FlixTheme(themeMode = themeMode) { content() }
    }
    repeat(SETTLE_FRAMES) { frame -> scene.render(frame * FRAME_NANOS) }
    Thread.sleep(FONT_LOAD_MILLIS)
    repeat(SETTLE_FRAMES) { frame -> Thread.sleep(50); scene.render((SETTLE_FRAMES + frame) * FRAME_NANOS) }
    Thread.sleep(FONT_LOAD_MILLIS)
    val image = scene.render(2 * SETTLE_FRAMES * FRAME_NANOS)
    val file = File("build/snapshots/$name.png").apply { parentFile.mkdirs() }
    file.writeBytes(requireNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
    scene.close()
}

private const val SETTLE_FRAMES = 30
private const val FRAME_NANOS = 16_000_000L
private const val FONT_LOAD_MILLIS = 600L

/** Lets the sample covers, saved as local files, load in the snapshots. */
private object LocalFileFetcher : coil3.fetch.Fetcher.Factory<coil3.Uri> {
    override fun create(data: coil3.Uri, options: coil3.request.Options, imageLoader: coil3.ImageLoader): coil3.fetch.Fetcher? {
        if (data.scheme != "file") return null
        return coil3.fetch.Fetcher {
            val path = okio.Path.Companion.run { File(java.net.URI(data.toString().replaceFirst("file:///", "file:/"))).absolutePath.toPath() }
            coil3.fetch.SourceFetchResult(coil3.decode.ImageSource(path, okio.FileSystem.SYSTEM), "image/png", coil3.decode.DataSource.DISK)
        }
    }
}
