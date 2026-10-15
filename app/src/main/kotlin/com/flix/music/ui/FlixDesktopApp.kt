package com.flix.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.music.core.designsystem.component.FlixSnackbar
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.handCursor
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.ui.LocalAppGraph
import com.flix.music.core.ui.window.TITLE_BAR_HEIGHT
import com.flix.music.core.ui.window.WindowButtons
import com.flix.music.core.ui.window.WindowDragArea
import com.flix.music.feature.player.PlayerBar
import com.flix.music.feature.player.PlayerBarActions
import com.flix.music.feature.search.SearchScreen
import com.flix.music.feature.settings.SettingsScreen
import com.flix.music.resources.Res
import com.flix.music.resources.ic_home
import com.flix.music.resources.ic_home_filled
import com.flix.music.resources.ic_library_music
import com.flix.music.resources.ic_library_music_filled
import com.flix.music.resources.ic_search
import com.flix.music.resources.ic_settings
import com.flix.music.resources.tab_home
import com.flix.music.resources.tab_library
import com.flix.music.resources.tab_search
import com.flix.music.resources.tab_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The three sections of the side menu, the same as the phone's tabs. */
enum class Section(val label: StringResource, val icon: DrawableResource, val selectedIcon: DrawableResource) {
    Home(Res.string.tab_home, Res.drawable.ic_home, Res.drawable.ic_home_filled),
    Search(Res.string.tab_search, Res.drawable.ic_search, Res.drawable.ic_search),
    Library(Res.string.tab_library, Res.drawable.ic_library_music, Res.drawable.ic_library_music_filled),
    Settings(Res.string.tab_settings, Res.drawable.ic_settings, Res.drawable.ic_settings),
}

/**
 * Desktop layout of Flix: the phone's tabs become a side menu under the wordmark, the
 * screen fills the rest, and the player lives in a bar along the bottom.
 */
@Composable
fun FlixDesktopApp() {
    val graph = LocalAppGraph.current
    val player = graph.player
    val playerState by player.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(Section.Home) }
    var volume by rememberSaveable { mutableIntStateOf(DEFAULT_VOLUME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val playerActions = remember(player) {
        PlayerBarActions(
            onPlayPause = player::togglePlayPause,
            onPrevious = player::skipToPrevious,
            onNext = player::skipToNext,
            onSeek = player::seekTo,
            onToggleShuffle = player::toggleShuffle,
            onCycleRepeat = player::cycleRepeatMode,
            onVolumeChange = { percent ->
                volume = percent
                player.setVolume(percent)
            },
        )
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Column(
            Modifier
                .fillMaxSize()
                .background(FlixTheme.colors.background),
        ) {
            Row(Modifier.weight(1f)) {
                SideMenu(selected = section, onSelect = { section = it })
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(TITLE_BAR_HEIGHT),
                    ) {
                        WindowDragArea(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        WindowButtons()
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (section) {
                            Section.Search -> SearchScreen(onGenreClick = {}, modifier = Modifier.fillMaxSize())
                            Section.Settings -> SettingsScreen(modifier = Modifier.fillMaxSize())
                            else -> Text(
                                text = stringResource(section.label),
                                style = FlixTheme.typography.headline,
                                color = FlixTheme.colors.textPrimary,
                            )
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(FlixTheme.spacing.lg),
                        ) { data -> FlixSnackbar(data) }
                    }
                }
            }
            PlayerBar(state = playerState, volume = volume, actions = playerActions)
        }
    }
}

@Composable
private fun SideMenu(selected: Section, onSelect: (Section) -> Unit) {
    Column(
        modifier = Modifier
            .width(SIDE_MENU_WIDTH)
            .fillMaxHeight()
            .background(FlixTheme.colors.surface)
            .padding(horizontal = FlixTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(FlixTheme.spacing.xs),
    ) {
        WindowDragArea(
            modifier = Modifier
                .fillMaxWidth()
                .height(WORDMARK_AREA_HEIGHT)
                .padding(start = FlixTheme.spacing.md, bottom = FlixTheme.spacing.sm),
            contentAlignment = Alignment.BottomStart,
        ) { Wordmark() }
        Section.entries.filter { entry -> entry != Section.Settings }.forEach { entry ->
            SideMenuItem(section = entry, isSelected = entry == selected, onClick = { onSelect(entry) })
        }
        Spacer(Modifier.weight(1f))
        SideMenuItem(section = Section.Settings, isSelected = selected == Section.Settings, onClick = { onSelect(Section.Settings) })
        Spacer(Modifier.height(FlixTheme.spacing.md))
    }
}

/** "flix" in Bricolage with the glowing Lumen dot, as in the design's brand mark. */
@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text("flix", style = FlixTheme.typography.headline, color = FlixTheme.colors.textPrimary)
        Box(
            Modifier
                .padding(start = 2.dp, bottom = 7.dp)
                .size(7.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = FlixTheme.colors.primary, spotColor = FlixTheme.colors.primary)
                .clip(CircleShape)
                .background(FlixTheme.colors.primaryContent),
        )
    }
}

@Composable
private fun SideMenuItem(section: Section, isSelected: Boolean, onClick: () -> Unit) {
    val colors = FlixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SIDE_MENU_ITEM_HEIGHT)
            .clip(FlixTheme.shapes.md)
            .background(if (isSelected) colors.surfaceVariant else colors.surface)
            .handCursor().selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = FlixTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
    ) {
        Icon(
            painter = painterResource(if (isSelected) section.selectedIcon else section.icon),
            contentDescription = null,
            tint = if (isSelected) colors.textPrimary else colors.textSecondary,
        )
        Text(
            text = stringResource(section.label),
            style = FlixTheme.typography.label,
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
        )
    }
}

private val SIDE_MENU_WIDTH = 232.dp
private val WORDMARK_AREA_HEIGHT = 88.dp
private const val DEFAULT_VOLUME = 100
private val SIDE_MENU_ITEM_HEIGHT = 44.dp
