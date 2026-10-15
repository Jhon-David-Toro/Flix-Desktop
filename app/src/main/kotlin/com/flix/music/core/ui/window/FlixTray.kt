package com.flix.music.core.ui.window

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

/** One entry of the tray menu. */
class TrayMenuItem(val label: String, val onClick: () -> Unit)

/**
 * Flix's icon in the Windows notification area: a click shows the window, a right
 * click opens [items] in a menu drawn the Windows 11 way (rounded, bordered, with a
 * shadow and the system font) instead of AWT's old-style popup.
 */
@Composable
fun ApplicationScope.FlixTray(icon: Painter, tooltip: String, isDark: Boolean, onShow: () -> Unit, items: List<TrayMenuItem>) {
    if (!SystemTray.isSupported()) return
    var menuAnchor by remember { mutableStateOf<Point?>(null) }
    val currentOnShow by rememberUpdatedState(onShow)
    val image = remember(icon) { icon.toAwtImage(Density(1f), LayoutDirection.Ltr, Size(TRAY_ICON_PX, TRAY_ICON_PX)) }
    val trayIcon = remember {
        TrayIcon(image, tooltip).apply {
            isImageAutoSize = true
            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseReleased(event: MouseEvent) {
                        when {
                            event.isPopupTrigger || event.button == MouseEvent.BUTTON3 -> menuAnchor = event.locationOnScreen
                            event.button == MouseEvent.BUTTON1 -> currentOnShow()
                        }
                    }
                },
            )
        }
    }
    LaunchedEffect(tooltip) { trayIcon.toolTip = tooltip }
    DisposableEffect(trayIcon) {
        SystemTray.getSystemTray().add(trayIcon)
        onDispose { SystemTray.getSystemTray().remove(trayIcon) }
    }

    val anchor = menuAnchor ?: return
    val menuSize = DpSize(MENU_WIDTH, MENU_PADDING * 2 + ITEM_HEIGHT * items.size + SHADOW_ROOM * 2)
    val position = remember(anchor) { menuPosition(anchor, menuSize) }
    Window(
        onCloseRequest = { menuAnchor = null },
        state = rememberWindowState(position = position, size = menuSize),
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        title = "",
    ) {
        DisposableEffect(window) {
            val closeOnFocusLoss = object : WindowFocusListener {
                override fun windowGainedFocus(event: WindowEvent) = Unit
                override fun windowLostFocus(event: WindowEvent) {
                    menuAnchor = null
                }
            }
            window.addWindowFocusListener(closeOnFocusLoss)
            window.toFront()
            window.requestFocus()
            onDispose { window.removeWindowFocusListener(closeOnFocusLoss) }
        }
        TrayMenu(
            isDark = isDark,
            items = items.map { item ->
                TrayMenuItem(item.label) {
                    menuAnchor = null
                    item.onClick()
                }
            },
        )
    }
}

@Composable
internal fun TrayMenu(isDark: Boolean, items: List<TrayMenuItem>) {
    val colors = if (isDark) DarkMenuColors else LightMenuColors
    val shape = RoundedCornerShape(MENU_RADIUS)
    Box(
        Modifier
            .fillMaxSize()
            .padding(SHADOW_ROOM),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .shadow(MENU_ELEVATION, shape)
                .clip(shape)
                .background(colors.background)
                .border(1.dp, colors.border, shape)
                .padding(MENU_PADDING),
        ) {
            items.forEach { item -> TrayMenuRow(item, colors) }
        }
    }
}

@Composable
private fun TrayMenuRow(item: TrayMenuItem, colors: MenuColors) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        Modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT)
            .clip(RoundedCornerShape(ITEM_RADIUS))
            .background(if (hovered) colors.hover else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = item.onClick)
            .padding(horizontal = ITEM_PADDING),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(item.label, style = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, color = colors.text))
    }
}

/** Above and to the left of the pointer, as Windows 11 opens tray menus, kept on screen. */
private fun menuPosition(anchor: Point, size: DpSize): WindowPosition {
    val configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        .map { device -> device.defaultConfiguration }
        .firstOrNull { configuration -> configuration.bounds.contains(anchor) }
        ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
    val screen = configuration.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
    val width = size.width.value.toInt()
    val height = size.height.value.toInt()
    val x = (anchor.x - width).coerceIn(screen.x + insets.left, screen.x + screen.width - insets.right - width)
    val y = (anchor.y - height).coerceIn(screen.y + insets.top, screen.y + screen.height - insets.bottom - height)
    return WindowPosition(x.dp, y.dp)
}

private class MenuColors(val background: Color, val border: Color, val hover: Color, val text: Color)

private val DarkMenuColors = MenuColors(
    background = Color(0xFF2C2C2C),
    border = Color(0x33FFFFFF),
    hover = Color(0x10FFFFFF),
    text = Color(0xFFFFFFFF),
)

private val LightMenuColors = MenuColors(
    background = Color(0xFFF9F9F9),
    border = Color(0x1A000000),
    hover = Color(0x0A000000),
    text = Color(0xE4000000),
)

private const val TRAY_ICON_PX = 32f
private val MENU_WIDTH = 220.dp
private val MENU_RADIUS = 8.dp
private val MENU_PADDING = 4.dp
private val MENU_ELEVATION = 8.dp
private val SHADOW_ROOM = 12.dp
private val ITEM_HEIGHT = 36.dp
private val ITEM_RADIUS = 4.dp
private val ITEM_PADDING = 12.dp
