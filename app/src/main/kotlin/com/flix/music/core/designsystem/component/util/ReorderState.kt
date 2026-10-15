package com.flix.music.core.designsystem.component.util

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex

/**
 * Drag-to-reorder for a [LazyColumn][androidx.compose.foundation.lazy.LazyColumn]
 * (design Gestures: "Arrastrar asa" in Cola and Editar playlist). The dragged row
 * follows the finger through [dragOffset]; when its center crosses another
 * reorderable row both swap, and the offset is corrected so the row stays under the
 * finger. Only items whose key passes [isReorderable] take part.
 */
class ReorderState(
    private val listState: LazyListState,
    private val isReorderable: (key: Any) -> Boolean,
) {

    var draggedKey: Any? by mutableStateOf(null)
        private set

    var dragOffset: Float by mutableFloatStateOf(0f)
        private set

    internal fun start(key: Any) {
        draggedKey = key
        dragOffset = 0f
    }

    internal fun stop() {
        draggedKey = null
        dragOffset = 0f
    }

    /** Returns true when the drag crossed into another row and [onSwap] was called. */
    internal fun drag(deltaY: Float, onSwap: (fromKey: Any, toKey: Any) -> Unit): Boolean {
        val key = draggedKey ?: return false
        dragOffset += deltaY
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val dragged = visibleItems.firstOrNull { item -> item.key == key } ?: return false
        val draggedCenter = (dragged.offset + dragOffset + dragged.size / 2f).toInt()
        val target = visibleItems.firstOrNull { item ->
            item.key != key && isReorderable(item.key) && draggedCenter in item.offset until item.offset + item.size
        } ?: return false
        onSwap(key, target.key)
        dragOffset += dragged.offset - target.offset
        return true
    }
}

@Composable
fun rememberReorderState(listState: LazyListState, isReorderable: (key: Any) -> Boolean): ReorderState =
    remember(listState) { ReorderState(listState, isReorderable) }

/**
 * Lifts the dragged row above the others (design: elevation medium + scale 1.02 while
 * the rest slide with `animateItem()`).
 */
fun Modifier.draggedRow(state: ReorderState, key: Any): Modifier =
    if (state.draggedKey == key) {
        zIndex(1f).graphicsLayer {
            translationY = state.dragOffset
            scaleX = DRAGGED_SCALE
            scaleY = DRAGGED_SCALE
        }
    } else {
        this
    }

/**
 * Makes this element the drag handle of the row with [key]. [onMove] receives the two
 * keys to swap; a haptic tick marks every crossed position.
 */
@Composable
fun Modifier.reorderHandle(state: ReorderState, key: Any, onMove: (fromKey: Any, toKey: Any) -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    val currentOnMove by rememberUpdatedState(onMove)
    return pointerInput(key) {
        detectDragGestures(
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                state.start(key)
            },
            onDragEnd = state::stop,
            onDragCancel = state::stop,
            onDrag = { change, dragAmount ->
                change.consume()
                if (state.drag(dragAmount.y, currentOnMove)) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            },
        )
    }
}

private const val DRAGGED_SCALE = 1.02f
