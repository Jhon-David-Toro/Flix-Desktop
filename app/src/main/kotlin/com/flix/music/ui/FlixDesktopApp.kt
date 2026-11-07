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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flix.music.core.designsystem.component.FlixSnackbar
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.handCursor
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.ui.DetailNavigator
import com.flix.music.core.ui.ErrorShortcuts
import com.flix.music.core.ui.LocalAppGraph
import com.flix.music.core.ui.LocalDetailNavigator
import com.flix.music.core.ui.LocalErrorShortcuts
import com.flix.music.core.ui.window.TITLE_BAR_HEIGHT
import com.flix.music.core.ui.window.WindowButtons
import com.flix.music.core.ui.window.WindowDragArea
import com.flix.music.feature.album.navigation.navigateToAlbum
import com.flix.music.feature.artist.navigation.navigateToArtist
import com.flix.music.feature.downloads.navigation.DownloadsRoute
import com.flix.music.feature.downloads.navigation.navigateToDownloads
import com.flix.music.feature.player.NowPlayingView
import com.flix.music.feature.player.PlayerBar
import com.flix.music.feature.player.PlayerBarActions
import com.flix.music.feature.player.QueueActions
import com.flix.music.feature.settings.navigation.SettingsRoute
import com.flix.music.feature.settings.navigation.navigateToSettings
import com.flix.music.feature.song.navigation.navigateToSong
import com.flix.music.feature.trackmenu.TrackMenuHost
import com.flix.music.navigation.FlixNavHost
import com.flix.music.navigation.TopLevelDestination
import com.flix.music.navigation.navigateToTopLevel
import com.flix.music.navigation.openPlaylistInLibrary
import com.flix.music.resources.Res
import com.flix.music.resources.ic_download
import com.flix.music.resources.ic_settings
import com.flix.music.resources.profile_downloads
import com.flix.music.resources.tab_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Where the side menu puts the user: the phone's three tabs, then Descargas and Ajustes at the bottom. */
private sealed interface MenuEntry {
    data class Tab(val destination: TopLevelDestination) : MenuEntry
    data object Downloads : MenuEntry
    data object Settings : MenuEntry
}

/**
 * Desktop layout of Flix: the phone's tabs become a side menu under the wordmark, the
 * screens (the phone's own, with its navigation) fill the rest, the Now Playing view
 * opens over them, and the player lives in a bar along the bottom.
 */
@Composable
fun FlixDesktopApp(volume: Int, onVolumeChange: (Int) -> Unit) {
    val graph = LocalAppGraph.current
    val player = graph.player
    val playerState by player.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    var lastTab by rememberSaveable { mutableStateOf(TopLevelDestination.Home) }
    val selected = currentEntry?.let(::menuEntryOf) ?: MenuEntry.Tab(lastTab)
    SideEffect { (selected as? MenuEntry.Tab)?.let { tab -> lastTab = tab.destination } }
    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    if (playerState.current == null) nowPlayingOpen = false
    val snackbarHostState = remember { SnackbarHostState() }

    val detailNavigator = remember(navController) {
        object : DetailNavigator {
            override fun openPlayer() {
                nowPlayingOpen = true
            }

            override fun openSong(trackId: String) {
                nowPlayingOpen = false
                navController.navigateToSong(trackId)
            }

            override fun openArtist(artist: ArtistRef) {
                nowPlayingOpen = false
                navController.navigateToArtist(artist.id, artist.name)
            }

            override fun openAlbum(album: AlbumRef, artist: ArtistRef?) {
                nowPlayingOpen = false
                navController.navigateToAlbum(album.id, album.name, artist?.name.orEmpty(), artist?.id)
            }
        }
    }
    val errorShortcuts = remember(navController) {
        ErrorShortcuts(openLibrary = { navController.navigateToTopLevel(TopLevelDestination.Library) })
    }
    val playerActions = remember(player, onVolumeChange) {
        PlayerBarActions(
            onPlayPause = player::togglePlayPause,
            onPrevious = player::skipToPrevious,
            onNext = player::skipToNext,
            onSeek = player::seekTo,
            onToggleShuffle = player::toggleShuffle,
            onCycleRepeat = player::cycleRepeatMode,
            onVolumeChange = onVolumeChange,
            onOpenPlayer = { nowPlayingOpen = !nowPlayingOpen },
        )
    }
    val queueActions = remember(player, navController) {
        QueueActions(
            onItemClick = { item -> player.skipToQueueItem(item.index) },
            onRemove = player::removeQueueItem,
            onRestore = player::restoreQueueItem,
            onMove = player::moveQueueItem,
            onClear = player::clearUpNext,
            onBrowseMusic = {
                nowPlayingOpen = false
                navController.navigateToTopLevel(TopLevelDestination.Search)
            },
        )
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalDetailNavigator provides detailNavigator,
        LocalErrorShortcuts provides errorShortcuts,
    ) {
        TrackMenuHost(onOpenPlaylist = navController::openPlaylistInLibrary) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(FlixTheme.colors.background),
            ) {
                Row(Modifier.weight(1f)) {
                    SideMenu(
                        selected = selected,
                        onSelect = { entry ->
                            nowPlayingOpen = false
                            when (entry) {
                                is MenuEntry.Tab -> navController.navigateToTopLevel(entry.destination)
                                MenuEntry.Downloads -> navController.navigateToDownloads()
                                MenuEntry.Settings -> navController.navigateToSettings()
                            }
                        },
                    )
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
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            FlixNavHost(navController = navController, modifier = Modifier.fillMaxSize())
                            if (nowPlayingOpen) {
                                NowPlayingView(
                                    state = playerState,
                                    queueActions = queueActions,
                                    onClose = { nowPlayingOpen = false },
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
}

/** The menu entry that owns [entry]: its tab graph, or Descargas / Ajustes; null for shared screens. */
private fun menuEntryOf(entry: NavBackStackEntry): MenuEntry? {
    val destination = entry.destination
    return when {
        destination.hasRoute(DownloadsRoute::class) -> MenuEntry.Downloads
        destination.hasRoute(SettingsRoute::class) -> MenuEntry.Settings
        else -> TopLevelDestination.entries
            .firstOrNull { tab -> destination.hierarchy.any { ancestor -> ancestor.hasRoute(tab.route::class) } }
            ?.let(MenuEntry::Tab)
    }
}

@Composable
private fun SideMenu(selected: MenuEntry, onSelect: (MenuEntry) -> Unit) {
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
        TopLevelDestination.entries.forEach { destination ->
            val entry = MenuEntry.Tab(destination)
            SideMenuItem(
                label = destination.label,
                icon = destination.icon,
                selectedIcon = destination.selectedIcon,
                isSelected = selected == entry,
                onClick = { onSelect(entry) },
            )
        }
        Spacer(Modifier.weight(1f))
        SideMenuItem(
            label = Res.string.profile_downloads,
            icon = Res.drawable.ic_download,
            selectedIcon = Res.drawable.ic_download,
            isSelected = selected == MenuEntry.Downloads,
            onClick = { onSelect(MenuEntry.Downloads) },
        )
        SideMenuItem(
            label = Res.string.tab_settings,
            icon = Res.drawable.ic_settings,
            selectedIcon = Res.drawable.ic_settings,
            isSelected = selected == MenuEntry.Settings,
            onClick = { onSelect(MenuEntry.Settings) },
        )
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
private fun SideMenuItem(
    label: StringResource,
    icon: DrawableResource,
    selectedIcon: DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = FlixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SIDE_MENU_ITEM_HEIGHT)
            .clip(FlixTheme.shapes.md)
            .background(if (isSelected) colors.surfaceVariant else colors.surface)
            .handCursor()
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = FlixTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.md),
    ) {
        Icon(
            painter = painterResource(if (isSelected) selectedIcon else icon),
            contentDescription = null,
            tint = if (isSelected) colors.textPrimary else colors.textSecondary,
        )
        Text(
            text = stringResource(label),
            style = FlixTheme.typography.label,
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
        )
    }
}

private val SIDE_MENU_WIDTH = 232.dp
private val WORDMARK_AREA_HEIGHT = 88.dp
private val SIDE_MENU_ITEM_HEIGHT = 44.dp
