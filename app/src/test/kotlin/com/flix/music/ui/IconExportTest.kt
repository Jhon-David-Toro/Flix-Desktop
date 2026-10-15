package com.flix.music.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.flix.music.resources.Res
import com.flix.music.resources.flix_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes `icons/flix.ico` (the installer's and the exe's icon) from the app's vector
 * icon, one PNG per size Windows asks for. Run once with `-PexportIcon=true` after
 * changing the logo; the .ico is committed.
 */
class IconExportTest {

    @Test
    fun `exporta el icono de Windows`() {
        assumeTrue(System.getProperty("flix.exportIcon") == "true")
        val images = SIZES.map { size -> renderIcon(size) }
        File("icons").mkdirs()
        File("icons/flix.ico").writeBytes(ico(SIZES.zip(images)))
    }

    private fun renderIcon(size: Int): ByteArray {
        val scene = ImageComposeScene(size, size, Density(1f)) {
            Image(painterResource(Res.drawable.flix_icon), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        repeat(SETTLE_FRAMES) { frame -> scene.render(frame * FRAME_NANOS) }
        Thread.sleep(LOAD_MILLIS)
        val image = scene.render(SETTLE_FRAMES * FRAME_NANOS)
        scene.close()
        return requireNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes
    }

    /** An .ico whose entries are PNGs, which Windows Vista and later read at any size. */
    private fun ico(images: List<Pair<Int, ByteArray>>): ByteArray {
        val header = ByteBuffer.allocate(HEADER_BYTES + ENTRY_BYTES * images.size).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(0).putShort(1).putShort(images.size.toShort())
        var offset = HEADER_BYTES + ENTRY_BYTES * images.size
        images.forEach { (size, png) ->
            val side = if (size >= MAX_ICO_SIDE) 0 else size
            header.put(side.toByte()).put(side.toByte()).put(0).put(0)
            header.putShort(1).putShort(BITS_PER_PIXEL)
            header.putInt(png.size).putInt(offset)
            offset += png.size
        }
        return ByteArrayOutputStream().apply {
            write(header.array())
            images.forEach { (_, png) -> write(png) }
        }.toByteArray()
    }

    private companion object {
        val SIZES = listOf(16, 24, 32, 48, 64, 128, 256)
        const val HEADER_BYTES = 6
        const val ENTRY_BYTES = 16
        const val MAX_ICO_SIDE = 256
        const val BITS_PER_PIXEL: Short = 32
        const val SETTLE_FRAMES = 10
        const val FRAME_NANOS = 16_000_000L
        const val LOAD_MILLIS = 400L
    }
}
