package com.flix.music.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.flix.music.resources.Res
import com.flix.music.resources.bricolage_grotesque_bold
import com.flix.music.resources.bricolage_grotesque_semibold
import com.flix.music.resources.instrument_sans_medium
import com.flix.music.resources.instrument_sans_regular
import com.flix.music.resources.instrument_sans_semibold
import org.jetbrains.compose.resources.Font

@Immutable
data class FlixTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val titleLarge: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
)

/**
 * The app's type scale. On desktop, fonts come from Compose resources, which can only
 * be read inside a composition.
 */
@Composable
fun flixTypography(): FlixTypography {
    val bricolage = FontFamily(
        Font(Res.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
        Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold),
    )
    val instrumentSans = FontFamily(
        Font(Res.font.instrument_sans_regular, FontWeight.Normal),
        Font(Res.font.instrument_sans_medium, FontWeight.Medium),
        Font(Res.font.instrument_sans_semibold, FontWeight.SemiBold),
    )
    return FlixTypography(
        display = TextStyle(
            fontFamily = bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.03).em,
        ),
        headline = TextStyle(
            fontFamily = bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.02).em,
        ),
        titleLarge = TextStyle(
            fontFamily = bricolage,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.01).em,
        ),
        title = TextStyle(
            fontFamily = bricolage,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.01).em,
        ),
        subtitle = TextStyle(
            fontFamily = instrumentSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        ),
        body = TextStyle(
            fontFamily = instrumentSans,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = instrumentSans,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        label = TextStyle(
            fontFamily = instrumentSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.01.em,
        ),
        caption = TextStyle(
            fontFamily = instrumentSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.01.em,
            fontFeatureSettings = "tnum",
        ),
        overline = TextStyle(
            fontFamily = instrumentSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.08.em,
        ),
    )
}

internal fun FlixTypography.toMaterial(): Typography = Typography().copy(
    displayLarge = display,
    headlineMedium = headline,
    titleLarge = titleLarge,
    titleMedium = title,
    titleSmall = subtitle,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = bodySmall,
    labelLarge = label,
    labelMedium = caption,
    labelSmall = overline,
)