package com.flix.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.ic_home
import com.flix.music.resources.ic_home_filled
import com.flix.music.resources.ic_library_music
import com.flix.music.resources.ic_library_music_filled
import com.flix.music.resources.ic_search
import com.flix.music.resources.tab_home
import com.flix.music.resources.tab_library
import com.flix.music.resources.tab_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The three sections of the side menu, the same as the phone's tabs. */
enum class Section(val label: StringResource, val icon: DrawableResource, val selectedIcon: DrawableResource) {
    Home(Res.string.tab_home, Res.drawable.ic_home, Res.drawable.ic_home_filled),
    Search(Res.string.tab_search, Res.drawable.ic_search, Res.drawable.ic_search),
    Library(Res.string.tab_library, Res.drawable.ic_library_music, Res.drawable.ic_library_music_filled),
}

/**
 * Desktop layout of Flix: the phone's tabs become a side menu under the wordmark, the
 * screen fills the rest, and the player lives in a bar along the bottom.
 */
@Composable
fun FlixDesktopApp() {
    var section by rememberSaveable { mutableStateOf(Section.Home) }
    Row(
        Modifier
            .fillMaxSize()
            .background(FlixTheme.colors.background),
    ) {
        SideMenu(selected = section, onSelect = { section = it })
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(section.label), style = FlixTheme.typography.headline, color = FlixTheme.colors.textPrimary)
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
            .padding(horizontal = FlixTheme.spacing.md, vertical = FlixTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(FlixTheme.spacing.xs),
    ) {
        Wordmark(Modifier.padding(start = FlixTheme.spacing.md, bottom = FlixTheme.spacing.xxl))
        Section.entries.forEach { entry ->
            SideMenuItem(section = entry, isSelected = entry == selected, onClick = { onSelect(entry) })
        }
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
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
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
private val SIDE_MENU_ITEM_HEIGHT = 44.dp
