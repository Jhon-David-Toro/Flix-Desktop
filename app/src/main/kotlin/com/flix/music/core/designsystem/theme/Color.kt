package com.flix.music.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class FlixColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContent: Color,
    val secondary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val overlay: Color,
)

val DarkFlixColors = FlixColors(
    background = Color(0xFF0C0D11),
    surface = Color(0xFF14161C),
    surfaceElevated = Color(0xFF1B1E26),
    surfaceVariant = Color(0xFF252932),
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContent = Color(0xFFA78BFA),
    secondary = Color(0xFF9FB0D0),
    success = Color(0xFF5BC98B),
    warning = Color(0xFFF0914A),
    error = Color(0xFFF0616A),
    info = Color(0xFF7FA7E8),
    textPrimary = Color(0xFFF4F2EE),
    textSecondary = Color(0xFFA8ACB7),
    textTertiary = Color(0xFF7F8491),
    divider = Color(0x14FFFFFF),
    overlay = Color(0xB806070A)
)

val LightFlixColors = FlixColors(
    background = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EBEF),
    primary = Color(0xFF6D28D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContent = Color(0xFF6D28D9),
    secondary = Color(0xFF4F6187),
    success = Color(0xFF1E8A52),
    warning = Color(0xFFB5561A),
    error = Color(0xFFC8323B),
    info = Color(0xFF2F63C0),
    textPrimary = Color(0xFF15171C),
    textSecondary = Color(0xFF555B67),
    textTertiary = Color(0xFF6F7583),
    divider = Color(0x14101218),
    overlay = Color(0x73101218)
)