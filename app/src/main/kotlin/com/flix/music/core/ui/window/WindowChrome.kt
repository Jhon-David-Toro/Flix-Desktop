package com.flix.music.core.ui.window

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.flix.music.core.designsystem.theme.FlixTheme
import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.MouseEvent

/** The undecorated window Flix draws its own frame on, for the drag areas and buttons below. */
class WindowFrame(val window: Window, val state: WindowState, val onMinimize: () -> Unit, val onClose: () -> Unit) {

    val isMaximized: Boolean get() = state.placement == WindowPlacement.Maximized

    /** Maximizes inside the screen's work area, so the taskbar stays visible. */
    fun toggleMaximized() {
        if (isMaximized) {
            state.placement = WindowPlacement.Floating
        } else {
            (window as? java.awt.Frame)?.maximizedBounds = workAreaOf(window)
            state.placement = WindowPlacement.Maximized
        }
    }
}

/** Provided by the window; null in snapshots and tests, where the frame is not drawn. */
val LocalWindowFrame = staticCompositionLocalOf<WindowFrame?> { null }

/**
 * Moves the window when dragged, toggles maximized on double click. Works in screen
 * coordinates, so the window does not jitter as it moves under the pointer.
 */
@Composable
fun WindowDragArea(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val frame = LocalWindowFrame.current
    val dragModifier = if (frame == null) {
        Modifier
    } else {
        Modifier.pointerInput(frame) {
            var pressPoint: Point? = null
            var windowStart: Point? = null
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val awtEvent = event.nativeEvent as? MouseEvent
                    when (event.type) {
                        PointerEventType.Press -> if (awtEvent?.button == MouseEvent.BUTTON1) {
                            if (awtEvent.clickCount == 2) {
                                frame.toggleMaximized()
                                pressPoint = null
                            } else if (!frame.isMaximized) {
                                pressPoint = MouseInfo.getPointerInfo().location
                                windowStart = frame.window.location
                            }
                        }
                        PointerEventType.Move -> {
                            val start = pressPoint ?: continue
                            val origin = windowStart ?: continue
                            val now = MouseInfo.getPointerInfo().location
                            frame.window.setLocation(origin.x + now.x - start.x, origin.y + now.y - start.y)
                        }
                        PointerEventType.Release -> pressPoint = null
                    }
                }
            }
        }
    }
    Box(modifier.then(dragModifier), contentAlignment = contentAlignment, content = content)
}

/** Minimize, maximize/restore and close, drawn in the Windows 11 manner with Flix's colors. */
@Composable
fun WindowButtons(modifier: Modifier = Modifier) {
    val frame = LocalWindowFrame.current ?: return
    Row(modifier.fillMaxHeight()) {
        WindowButton(label = "Minimizar", onClick = frame.onMinimize) { color ->
            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = STROKE_PX)
        }
        WindowButton(label = if (frame.isMaximized) "Restaurar" else "Maximizar", onClick = frame::toggleMaximized) { color ->
            if (frame.isMaximized) {
                val inset = size.width * RESTORE_INSET
                drawRect(color, topLeft = Offset(0f, inset), size = androidx.compose.ui.geometry.Size(size.width - inset, size.height - inset), style = Stroke(STROKE_PX))
                drawLine(color, Offset(inset, 0f), Offset(size.width, 0f), strokeWidth = STROKE_PX)
                drawLine(color, Offset(size.width, 0f), Offset(size.width, size.height - inset), strokeWidth = STROKE_PX)
            } else {
                drawRect(color, style = Stroke(STROKE_PX))
            }
        }
        WindowButton(label = "Cerrar", onClick = frame.onClose, hoverColor = CLOSE_HOVER, hoverContentColor = Color.White) { color ->
            drawLine(color, Offset.Zero, Offset(size.width, size.height), strokeWidth = STROKE_PX)
            drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = STROKE_PX)
        }
    }
}

@Composable
private fun WindowButton(
    label: String,
    onClick: () -> Unit,
    hoverColor: Color = FlixTheme.colors.textPrimary.copy(alpha = HOVER_ALPHA),
    hoverContentColor: Color = FlixTheme.colors.textPrimary,
    glyph: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val contentColor = if (hovered) hoverContentColor else FlixTheme.colors.textSecondary
    Box(
        modifier = Modifier
            .width(BUTTON_WIDTH)
            .fillMaxHeight()
            .background(if (hovered) hoverColor else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(GLYPH_SIZE)) { glyph(contentColor) }
    }
}

/**
 * Invisible borders that resize the undecorated window, with the system's resize
 * cursors; off while maximized. Put it last in a full-window Box so it sits on top.
 */
@Composable
fun BoxScope.WindowResizeEdges() {
    val frame = LocalWindowFrame.current ?: return
    if (frame.isMaximized) return
    ResizeEdge.entries.forEach { edge -> ResizeHandle(frame, edge) }
}

private enum class ResizeEdge(val cursor: Int, val left: Boolean = false, val top: Boolean = false, val right: Boolean = false, val bottom: Boolean = false) {
    Left(Cursor.W_RESIZE_CURSOR, left = true),
    Right(Cursor.E_RESIZE_CURSOR, right = true),
    Top(Cursor.N_RESIZE_CURSOR, top = true),
    Bottom(Cursor.S_RESIZE_CURSOR, bottom = true),
    TopLeft(Cursor.NW_RESIZE_CURSOR, left = true, top = true),
    TopRight(Cursor.NE_RESIZE_CURSOR, right = true, top = true),
    BottomLeft(Cursor.SW_RESIZE_CURSOR, left = true, bottom = true),
    BottomRight(Cursor.SE_RESIZE_CURSOR, right = true, bottom = true),
}

@Composable
private fun BoxScope.ResizeHandle(frame: WindowFrame, edge: ResizeEdge) {
    val horizontal = edge.left || edge.right
    val vertical = edge.top || edge.bottom
    val alignment = when {
        edge.top && edge.left -> Alignment.TopStart
        edge.top && edge.right -> Alignment.TopEnd
        edge.bottom && edge.left -> Alignment.BottomStart
        edge.bottom && edge.right -> Alignment.BottomEnd
        edge.left -> Alignment.CenterStart
        edge.right -> Alignment.CenterEnd
        edge.top -> Alignment.TopCenter
        else -> Alignment.BottomCenter
    }
    val sizeModifier = when {
        horizontal && vertical -> Modifier.size(CORNER_SIZE)
        horizontal -> Modifier.width(EDGE_SIZE).fillMaxHeight()
        else -> Modifier.height(EDGE_SIZE).fillMaxWidth()
    }
    Box(
        sizeModifier
            .align(alignment)
            .pointerHoverIcon(PointerIcon(Cursor(edge.cursor)))
            .pointerInput(frame, edge) {
                var pressPoint: Point? = null
                var startBounds: Rectangle? = null
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                pressPoint = MouseInfo.getPointerInfo().location
                                startBounds = frame.window.bounds
                            }
                            PointerEventType.Move -> {
                                val start = pressPoint ?: continue
                                val bounds = startBounds ?: continue
                                val now = MouseInfo.getPointerInfo().location
                                frame.window.bounds = resized(bounds, edge, now.x - start.x, now.y - start.y, frame.window.minimumSize)
                            }
                            PointerEventType.Release -> pressPoint = null
                        }
                    }
                }
            },
    )
}

private fun resized(bounds: Rectangle, edge: ResizeEdge, deltaX: Int, deltaY: Int, minimum: java.awt.Dimension): Rectangle {
    var left = bounds.x
    var top = bounds.y
    var right = bounds.x + bounds.width
    var bottom = bounds.y + bounds.height
    if (edge.left) left = (left + deltaX).coerceAtMost(right - minimum.width)
    if (edge.right) right = (right + deltaX).coerceAtLeast(left + minimum.width)
    if (edge.top) top = (top + deltaY).coerceAtMost(bottom - minimum.height)
    if (edge.bottom) bottom = (bottom + deltaY).coerceAtLeast(top + minimum.height)
    return Rectangle(left, top, right - left, bottom - top)
}

private fun workAreaOf(window: Window): Rectangle {
    val configuration = window.graphicsConfiguration ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
    val screen = configuration.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
    return Rectangle(
        screen.x + insets.left,
        screen.y + insets.top,
        screen.width - insets.left - insets.right,
        screen.height - insets.top - insets.bottom,
    )
}

/** Fills the window, draws the content, and adds the resize borders on top. */
@Composable
fun WindowSurface(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        WindowResizeEdges()
    }
}

val TITLE_BAR_HEIGHT: Dp = 40.dp
private val BUTTON_WIDTH = 46.dp
private val GLYPH_SIZE = 10.dp
private val EDGE_SIZE = 5.dp
private val CORNER_SIZE = 10.dp
private const val STROKE_PX = 1.4f
private const val RESTORE_INSET = 0.25f
private const val HOVER_ALPHA = 0.08f
private val CLOSE_HOVER = Color(0xFFC42B1C)
