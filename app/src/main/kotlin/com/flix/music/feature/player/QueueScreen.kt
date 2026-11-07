package com.flix.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.component.IconButton
import com.flix.music.core.designsystem.component.LocalSnackbarHostState
import com.flix.music.core.designsystem.component.NowPlayingEqualizer
import com.flix.music.core.designsystem.component.StateView
import com.flix.music.core.designsystem.component.SwipeToRemove
import com.flix.music.core.designsystem.component.TextButton
import com.flix.music.core.designsystem.component.handCursor
import com.flix.music.core.designsystem.component.showUndo
import com.flix.music.core.designsystem.component.util.draggedRow
import com.flix.music.core.designsystem.component.util.rememberArtworkTint
import com.flix.music.core.designsystem.component.util.rememberReorderState
import com.flix.music.core.designsystem.component.util.reorderHandle
import com.flix.music.core.designsystem.component.util.toClock
import com.flix.music.core.designsystem.component.util.toReadableLength
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import com.flix.music.core.model.totalDuration
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlaybackStatus
import com.flix.music.core.player.PlayerState
import com.flix.music.core.player.QueueItem
import com.flix.music.resources.Res
import com.flix.music.resources.action_browse_music
import com.flix.music.resources.action_move_down
import com.flix.music.resources.action_move_up
import com.flix.music.resources.action_undo
import com.flix.music.resources.ic_drag_indicator
import com.flix.music.resources.ic_keyboard_arrow_down
import com.flix.music.resources.ic_queue_music
import com.flix.music.resources.player_up_next
import com.flix.music.resources.queue_clear
import com.flix.music.resources.queue_close
import com.flix.music.resources.queue_empty_body
import com.flix.music.resources.queue_empty_title
import com.flix.music.resources.queue_now_playing
import com.flix.music.resources.queue_remove
import com.flix.music.resources.queue_title
import com.flix.music.resources.queue_track_removed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** Every user action on the Cola layer. Defaults do nothing, which keeps previews short. */
data class QueueActions(
    val onClose: () -> Unit = {},
    val onItemClick: (QueueItem) -> Unit = {},
    val onRemove: (mediaIndex: Int) -> Unit = {},
    val onRestore: (mediaIndex: Int, track: Track) -> Unit = { _, _ -> },
    val onMove: (fromMediaIndex: Int, toMediaIndex: Int) -> Unit = { _, _ -> },
    val onClear: () -> Unit = {},
    val onBrowseMusic: () -> Unit = {},
)

/**
 * Cola (design `queue`) as a panel of the desktop Now Playing view: "Ahora suena"
 * highlighted, then "A continuación" in play order. Clicking a row jumps to it, the
 * handle reorders (also with shuffle: the desktop queue is kept in play order), and
 * swiping left removes with "Deshacer".
 *
 * Reorders apply to a local copy first so the row stays under the finger while the
 * player catches up; the copy follows the player again when no drag is in progress.
 */
@Composable
fun QueuePanel(state: PlayerState, actions: QueueActions, modifier: Modifier = Modifier) {
    val nowPlaying = state.current ?: return
    val listState = rememberLazyListState()
    val reorder = rememberReorderState(listState) { key -> key is String && key.startsWith(TRACK_KEY_PREFIX) }
    var displayed by remember { mutableStateOf(state.upNext) }
    if (reorder.draggedKey == null && displayed != state.upNext) displayed = state.upNext
    val keys = queueKeys(displayed)
    val firstMediaIndex = state.upNext.minOfOrNull { item -> item.index } ?: 0
    val canReorder = true
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val removedMessage = stringResource(Res.string.queue_track_removed)
    val undoLabel = stringResource(Res.string.action_undo)

    fun mediaIndexAt(position: Int): Int =
        if (canReorder) firstMediaIndex + position else displayed[position].index

    fun move(fromPosition: Int, toPosition: Int) {
        if (fromPosition !in displayed.indices || toPosition !in displayed.indices) return
        val fromMediaIndex = mediaIndexAt(fromPosition)
        val toMediaIndex = mediaIndexAt(toPosition)
        displayed = displayed.toMutableList().apply { add(toPosition, removeAt(fromPosition)) }
        actions.onMove(fromMediaIndex, toMediaIndex)
    }

    fun removeWithUndo(position: Int) {
        val item = displayed.getOrNull(position) ?: return
        val mediaIndex = mediaIndexAt(position)
        displayed = displayed.toMutableList().apply { removeAt(position) }
        actions.onRemove(mediaIndex)
        coroutineScope.launch {
            snackbarHostState.showUndo(removedMessage, undoLabel) { actions.onRestore(mediaIndex, item.track) }
        }
    }

    Column(modifier.fillMaxSize()) {
        QueueHeader(showClear = displayed.isNotEmpty(), onClear = actions.onClear)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = FlixTheme.spacing.xxl),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "now-playing") {
                Column(Modifier.padding(horizontal = FlixTheme.spacing.gutter)) {
                    Overline(
                        text = stringResource(Res.string.queue_now_playing),
                        modifier = Modifier.padding(top = 6.dp, bottom = FlixTheme.spacing.sm),
                    )
                    NowPlayingCard(state, nowPlaying)
                }
            }
            if (displayed.isEmpty()) {
                item(key = "empty") {
                    StateView(
                        icon = Res.drawable.ic_queue_music,
                        title = stringResource(Res.string.queue_empty_title),
                        body = stringResource(Res.string.queue_empty_body),
                        actionLabel = stringResource(Res.string.action_browse_music),
                        onAction = actions.onBrowseMusic,
                    )
                }
                return@LazyColumn
            }
            item(key = "up-next-header") {
                UpNextHeader(source = state.source, tracks = displayed.map { item -> item.track })
            }
            itemsIndexed(displayed, key = { position, _ -> keys[position] }) { position, item ->
                val key = keys[position]
                val isDragged = reorder.draggedKey == key
                SwipeToRemove(
                    onRemove = { removeWithUndo(position) },
                    modifier = if (isDragged) Modifier.draggedRow(reorder, key) else Modifier.animateItem(),
                ) {
                    QueueRow(
                        track = item.track,
                        isDragged = isDragged,
                        onClick = { actions.onItemClick(item) },
                        accessibilityActions = queueAccessibilityActions(
                            position = position,
                            lastPosition = displayed.lastIndex,
                            canReorder = canReorder,
                            onMove = ::move,
                            onRemove = { removeWithUndo(position) },
                        ),
                        dragHandle = if (canReorder) {
                            Modifier.reorderHandle(reorder, key) { fromKey, toKey ->
                                move(keys.indexOf(fromKey), keys.indexOf(toKey))
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/** "Limpiar" above the list; the panel's tab already says Cola. */
@Composable
private fun QueueHeader(showClear: Boolean, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(QUEUE_HEADER_HEIGHT)
            .padding(horizontal = FlixTheme.spacing.sm),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showClear) {
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(Res.string.queue_clear),
                    style = FlixTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = FlixTheme.colors.primaryContent,
                )
            }
        }
    }
}

/** "Ahora suena" (design): surfaceVariant card, title in primaryContent, elapsed / total and the equalizer. */
@Composable
private fun NowPlayingCard(state: PlayerState, nowPlaying: Track) {
    val progress = listOfNotNull(state.position.toClock(), state.duration?.toClock()).joinToString(" / ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CARD_SHAPE)
            .background(FlixTheme.colors.surfaceVariant)
            .padding(start = 10.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
            .height(COMPACT_ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        QueueArtwork(nowPlaying.artworkUrl)
        Column(Modifier.weight(1f)) {
            Text(
                text = nowPlaying.title,
                style = FlixTheme.typography.subtitle,
                color = FlixTheme.colors.primaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${nowPlaying.artistNames} · $progress",
                style = FlixTheme.typography.bodySmall,
                color = FlixTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        NowPlayingEqualizer(
            animated = state.status == PlaybackStatus.Playing,
            modifier = Modifier.padding(end = 10.dp),
        )
    }
}

/** `.sec`: "A continuación · de tu búsqueda" on the left, "5 · 27 min" on the right. */
@Composable
private fun UpNextHeader(source: PlaybackSource?, tracks: List<Track>) {
    val sourceSuffix = source?.let { queueSourceSuffix(it) }
    val title = listOfNotNull(stringResource(Res.string.player_up_next), sourceSuffix).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FlixTheme.spacing.gutter)
            .padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Overline(text = title, modifier = Modifier.weight(1f))
        Text(
            text = "${tracks.size} · ${tracks.totalDuration().toReadableLength()}",
            style = FlixTheme.typography.caption,
            color = FlixTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun QueueRow(
    track: Track,
    isDragged: Boolean,
    onClick: () -> Unit,
    accessibilityActions: List<CustomAccessibilityAction>,
    modifier: Modifier = Modifier,
    dragHandle: Modifier? = null,
) {
    val subtitle = listOfNotNull(track.artistNames, track.duration?.toClock()).joinToString(" · ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(COMPACT_ROW_HEIGHT)
            .then(if (isDragged) Modifier.shadow(6.dp, FlixTheme.shapes.md) else Modifier)
            .clip(FlixTheme.shapes.md)
            .background(if (isDragged) FlixTheme.colors.surfaceElevated else Color.Transparent)
            .handCursor().clickable(onClick = onClick)
            .semantics { customActions = accessibilityActions }
            .padding(start = FlixTheme.spacing.gutter, end = FlixTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        QueueArtwork(track.artworkUrl)
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = FlixTheme.typography.subtitle,
                color = FlixTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = FlixTheme.typography.bodySmall,
                color = FlixTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (dragHandle != null) {
            Box(
                modifier = Modifier
                    .size(FlixTheme.spacing.touchMin)
                    .clearAndSetSemantics {}
                    .then(dragHandle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_drag_indicator),
                    contentDescription = null,
                    tint = if (isDragged) FlixTheme.colors.textPrimary else FlixTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun QueueArtwork(artworkUrl: String?) {
    AsyncImage(
        model = artworkUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(40.dp)
            .clip(ARTWORK_SHAPE)
            .background(FlixTheme.colors.surfaceVariant),
    )
}

@Composable
private fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = FlixTheme.typography.overline,
        color = FlixTheme.colors.textSecondary,
        modifier = modifier,
    )
}

/** Screen-reader alternatives to dragging and swiping (design Accesibilidad). */
@Composable
private fun queueAccessibilityActions(
    position: Int,
    lastPosition: Int,
    canReorder: Boolean,
    onMove: (Int, Int) -> Unit,
    onRemove: () -> Unit,
): List<CustomAccessibilityAction> {
    val moveUpLabel = stringResource(Res.string.action_move_up)
    val moveDownLabel = stringResource(Res.string.action_move_down)
    val removeLabel = stringResource(Res.string.queue_remove)
    return buildList {
        if (canReorder && position > 0) add(CustomAccessibilityAction(moveUpLabel) { onMove(position, position - 1); true })
        if (canReorder && position < lastPosition) {
            add(CustomAccessibilityAction(moveDownLabel) { onMove(position, position + 1); true })
        }
        add(CustomAccessibilityAction(removeLabel) { onRemove(); true })
    }
}

/**
 * Stable row keys: the track id plus its occurrence, so the same song queued twice
 * still gets two distinct rows that keep their identity while reordering.
 */
internal fun queueKeys(items: List<QueueItem>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return items.map { item ->
        val occurrence = seen.merge(item.track.id, 1, Int::plus) ?: 1
        "$TRACK_KEY_PREFIX${item.track.id}#$occurrence"
    }
}

private const val TRACK_KEY_PREFIX = "track-"
private const val QUEUE_TINT_AMOUNT = 0.18f
private const val GRADIENT_END = 0.78f
private val COMPACT_ROW_HEIGHT = 56.dp
private val CARD_SHAPE = RoundedCornerShape(14.dp)
private val ARTWORK_SHAPE = RoundedCornerShape(6.dp)
private val CLOSE_DRAG_THRESHOLD = 120.dp

private fun previewTrack(id: String, title: String, artist: String, seconds: Int) =
    Track(id, title, listOf(ArtistRef(null, artist)), null, seconds.seconds, null)

private val QUEUE_HEADER_HEIGHT = 48.dp
