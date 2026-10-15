package com.flix.music.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.designsystem.component.FlixSegmentedControl
import com.flix.music.core.designsystem.component.Segment
import com.flix.music.core.designsystem.component.SettingsGroupTitle
import com.flix.music.core.designsystem.component.SettingsRow
import com.flix.music.core.designsystem.component.Switch
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.AppTheme
import com.flix.music.core.model.UserSettings
import com.flix.music.core.ui.flixViewModel
import com.flix.music.resources.Res
import com.flix.music.resources.ic_brightness_auto
import com.flix.music.resources.ic_dark_mode
import com.flix.music.resources.ic_light_mode
import com.flix.music.resources.settings_about
import com.flix.music.resources.settings_appearance
import com.flix.music.resources.settings_autoplay
import com.flix.music.resources.settings_autoplay_body
import com.flix.music.resources.settings_desktop
import com.flix.music.resources.settings_playback
import com.flix.music.resources.settings_remember_modes
import com.flix.music.resources.settings_start_with_windows
import com.flix.music.resources.settings_start_with_windows_body
import com.flix.music.resources.settings_start_with_windows_unavailable
import com.flix.music.resources.settings_theme_dark
import com.flix.music.resources.settings_theme_light
import com.flix.music.resources.settings_theme_system
import com.flix.music.resources.settings_title
import com.flix.music.resources.settings_version
import org.jetbrains.compose.resources.stringResource

/** Every user action on Ajustes. Defaults do nothing, which keeps snapshots short. */
data class SettingsActions(
    val onThemeChange: (AppTheme) -> Unit = {},
    val onAutoplayChange: (Boolean) -> Unit = {},
    val onRememberPlaybackModesChange: (Boolean) -> Unit = {},
    val onStartWithWindowsChange: (Boolean) -> Unit = {},
)

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = flixViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsContent(
        settings = settings,
        startsWithWindows = viewModel.startsWithWindows,
        canStartWithWindows = viewModel.canStartWithWindows,
        version = viewModel.version,
        actions = SettingsActions(
            onThemeChange = viewModel::onThemeChange,
            onAutoplayChange = viewModel::onAutoplayChange,
            onRememberPlaybackModesChange = viewModel::onRememberPlaybackModesChange,
            onStartWithWindowsChange = viewModel::onStartWithWindowsChange,
        ),
        modifier = modifier,
    )
}

/**
 * Ajustes (design `settings`), in a column of readable width on wide windows: the
 * phone's appearance and playback groups, plus an Escritorio group for the PC.
 */
@Composable
fun SettingsContent(
    settings: UserSettings,
    startsWithWindows: Boolean,
    canStartWithWindows: Boolean,
    version: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = CONTENT_MAX_WIDTH)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl),
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(Res.string.settings_title),
                    style = FlixTheme.typography.headline,
                    color = FlixTheme.colors.textPrimary,
                    modifier = Modifier.padding(horizontal = FlixTheme.spacing.gutter, vertical = FlixTheme.spacing.md),
                )
            }
            item(key = "appearance") {
                SettingsGroupTitle(stringResource(Res.string.settings_appearance))
                FlixSegmentedControl(
                    segments = listOf(
                        Segment(AppTheme.System, stringResource(Res.string.settings_theme_system), Res.drawable.ic_brightness_auto),
                        Segment(AppTheme.Light, stringResource(Res.string.settings_theme_light), Res.drawable.ic_light_mode),
                        Segment(AppTheme.Dark, stringResource(Res.string.settings_theme_dark), Res.drawable.ic_dark_mode),
                    ),
                    selected = settings.theme,
                    onSelect = actions.onThemeChange,
                    modifier = Modifier.padding(horizontal = FlixTheme.spacing.gutter),
                )
            }
            item(key = "playback") {
                SettingsGroupTitle(stringResource(Res.string.settings_playback))
                SettingsRow(
                    title = stringResource(Res.string.settings_autoplay),
                    subtitle = stringResource(Res.string.settings_autoplay_body),
                    onClick = { actions.onAutoplayChange(!settings.autoplay) },
                ) { FlixSwitch(checked = settings.autoplay, onCheckedChange = actions.onAutoplayChange) }
                SettingsRow(
                    title = stringResource(Res.string.settings_remember_modes),
                    onClick = { actions.onRememberPlaybackModesChange(!settings.rememberPlaybackModes) },
                ) {
                    FlixSwitch(checked = settings.rememberPlaybackModes, onCheckedChange = actions.onRememberPlaybackModesChange)
                }
            }
            item(key = "desktop") {
                SettingsGroupTitle(stringResource(Res.string.settings_desktop))
                SettingsRow(
                    title = stringResource(Res.string.settings_start_with_windows),
                    subtitle = stringResource(
                        if (canStartWithWindows) Res.string.settings_start_with_windows_body else Res.string.settings_start_with_windows_unavailable,
                    ),
                    enabled = canStartWithWindows,
                    onClick = { actions.onStartWithWindowsChange(!startsWithWindows) },
                ) {
                    FlixSwitch(
                        checked = startsWithWindows,
                        onCheckedChange = actions.onStartWithWindowsChange,
                        enabled = canStartWithWindows,
                    )
                }
            }
            item(key = "about") {
                SettingsGroupTitle(stringResource(Res.string.settings_about))
                SettingsRow(title = stringResource(Res.string.settings_version), subtitle = version)
            }
        }
    }
}

/** The design's switch: textPrimary when on, as chips (violet is for music only). */
@Composable
private fun FlixSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = FlixTheme.colors.textPrimary,
            checkedThumbColor = FlixTheme.colors.background,
            checkedBorderColor = FlixTheme.colors.textPrimary,
            uncheckedTrackColor = FlixTheme.colors.surfaceVariant,
            uncheckedThumbColor = FlixTheme.colors.textTertiary,
            uncheckedBorderColor = FlixTheme.colors.textTertiary,
        ),
    )
}

private val CONTENT_MAX_WIDTH = 720.dp
