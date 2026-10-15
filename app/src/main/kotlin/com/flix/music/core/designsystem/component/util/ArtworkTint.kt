package com.flix.music.core.designsystem.component.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.flix.music.core.designsystem.theme.FlixMotion
import com.flix.music.core.designsystem.theme.FlixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Top color of the Full Player gradient, taken from the artwork (design: "Tinte = color
 * dominante del artwork, limitado en luminancia"). The dominant color is clamped to
 * L* ≤ 35 (dark) / ≥ 85 (light), then mixed with the background at 48 % / 22 %,
 * and animated on track changes (slow). [amount] overrides the mix, e.g. the lighter
 * 18 % of the Cola layer. The phone uses AndroidX Palette; the desktop finds the
 * dominant color itself with [dominantColorOf].
 */
@Composable
fun rememberArtworkTint(artworkUrl: String?, amount: Float? = null): Color {
    val platformContext = LocalPlatformContext.current
    val isDark = FlixTheme.isDark
    val background = FlixTheme.colors.background
    var dominantColor by remember(artworkUrl) { mutableStateOf<Int?>(null) }

    LaunchedEffect(artworkUrl) {
        dominantColor = artworkUrl?.let { url -> loadDominantColor(platformContext, url) }
    }

    val target = dominantColor?.let { color ->
        lerp(background, Color(color.clampLightness(isDark)), amount ?: if (isDark) DARK_TINT_AMOUNT else LIGHT_TINT_AMOUNT)
    } ?: background

    val tint by animateColorAsState(target, tween(FlixMotion.SLOW, easing = FlixMotion.Standard), label = "artworkTint")
    return tint
}

private suspend fun loadDominantColor(platformContext: PlatformContext, artworkUrl: String): Int? {
    val request = ImageRequest.Builder(platformContext)
        .data(artworkUrl)
        .size(SAMPLE_SIZE)
        .build()
    val result = SingletonImageLoader.get(platformContext).execute(request) as? SuccessResult ?: return null
    val bitmap = result.image.toBitmap()
    return withContext(Dispatchers.Default) {
        val pixels = IntArray(bitmap.width * bitmap.height) { index ->
            bitmap.getColor(index % bitmap.width, index / bitmap.width)
        }
        dominantColorOf(pixels)
    }
}

/**
 * The most common color of [pixels] (ARGB): pixels are grouped by their top bits per
 * channel and the busiest group's average is returned, as Palette's dominant swatch.
 * Transparent pixels are ignored; null when nothing is left.
 */
internal fun dominantColorOf(pixels: IntArray): Int? {
    val groups = HashMap<Int, LongArray>()
    for (pixel in pixels) {
        if (pixel ushr ALPHA_SHIFT < OPAQUE_ENOUGH) continue
        val red = pixel shr RED_SHIFT and CHANNEL_MASK
        val green = pixel shr GREEN_SHIFT and CHANNEL_MASK
        val blue = pixel and CHANNEL_MASK
        val group = (red shr QUANTIZE_SHIFT shl (2 * GROUP_BITS)) or (green shr QUANTIZE_SHIFT shl GROUP_BITS) or (blue shr QUANTIZE_SHIFT)
        val sums = groups.getOrPut(group) { LongArray(4) }
        sums[0]++
        sums[1] += red.toLong()
        sums[2] += green.toLong()
        sums[3] += blue.toLong()
    }
    val busiest = groups.values.maxByOrNull { sums -> sums[0] } ?: return null
    val count = busiest[0]
    return OPAQUE or
        ((busiest[1] / count).toInt() shl RED_SHIFT) or
        ((busiest[2] / count).toInt() shl GREEN_SHIFT) or
        (busiest[3] / count).toInt()
}

/** Keeps the hue but limits the CIE L* lightness, so the tint never fights the text. */
internal fun Int.clampLightness(isDark: Boolean): Int {
    val (lightness, a, b) = toLab()
    val clamped = if (isDark) lightness.coerceAtMost(DARK_MAX_LIGHTNESS) else lightness.coerceAtLeast(LIGHT_MIN_LIGHTNESS)
    return labToColor(clamped, a, b)
}

private fun Int.toLab(): Triple<Double, Double, Double> {
    val red = linear(this shr RED_SHIFT and CHANNEL_MASK)
    val green = linear(this shr GREEN_SHIFT and CHANNEL_MASK)
    val blue = linear(this and CHANNEL_MASK)
    val x = labCurve((0.4124 * red + 0.3576 * green + 0.1805 * blue) / WHITE_X)
    val y = labCurve(0.2126 * red + 0.7152 * green + 0.0722 * blue)
    val z = labCurve((0.0193 * red + 0.1192 * green + 0.9505 * blue) / WHITE_Z)
    return Triple(116 * y - 16, 500 * (x - y), 200 * (y - z))
}

private fun labToColor(lightness: Double, a: Double, b: Double): Int {
    val y = (lightness + 16) / 116
    val x = inverseLabCurve(y + a / 500) * WHITE_X
    val z = inverseLabCurve(y - b / 200) * WHITE_Z
    val yLinear = inverseLabCurve(y)
    val red = gamma(3.2406 * x - 1.5372 * yLinear - 0.4986 * z)
    val green = gamma(-0.9689 * x + 1.8758 * yLinear + 0.0415 * z)
    val blue = gamma(0.0557 * x - 0.2040 * yLinear + 1.0570 * z)
    return OPAQUE or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue
}

private fun linear(channel: Int): Double {
    val value = channel / 255.0
    return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}

private fun gamma(linear: Double): Int {
    val value = linear.coerceIn(0.0, 1.0)
    val encoded = if (value <= 0.0031308) value * 12.92 else 1.055 * value.pow(1 / 2.4) - 0.055
    return (encoded * 255).toInt().coerceIn(0, CHANNEL_MASK)
}

private fun labCurve(value: Double): Double = if (value > LAB_EPSILON) cbrt(value) else (LAB_KAPPA * value + 16) / 116

private fun inverseLabCurve(value: Double): Double {
    val cubed = value * value * value
    return if (cubed > LAB_EPSILON) cubed else (116 * value - 16) / LAB_KAPPA
}

private const val DARK_TINT_AMOUNT = 0.48f
private const val LIGHT_TINT_AMOUNT = 0.22f
private const val DARK_MAX_LIGHTNESS = 35.0
private const val LIGHT_MIN_LIGHTNESS = 85.0
private const val SAMPLE_SIZE = 128
private const val GROUP_BITS = 4
private const val QUANTIZE_SHIFT = 8 - GROUP_BITS
private const val CHANNEL_MASK = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val ALPHA_SHIFT = 24
private const val OPAQUE_ENOUGH = 0x80
private const val OPAQUE = 0xFF shl ALPHA_SHIFT
private const val WHITE_X = 0.95047
private const val WHITE_Z = 1.08883
private const val LAB_EPSILON = 216.0 / 24389.0
private const val LAB_KAPPA = 24389.0 / 27.0
