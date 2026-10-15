package com.flix.music.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flix.music.core.model.SortOrder
import com.flix.music.resources.Res
import com.flix.music.resources.ic_swap_vert
import com.flix.music.resources.sort_alphabetical
import com.flix.music.resources.sort_recent
import org.jetbrains.compose.resources.stringResource

/** "⇅ Recientes / A–Z" chip; each tap switches to the other order. */
@Composable
fun SortOrderChip(
    order: SortOrder,
    onOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (order) {
        SortOrder.Recent -> stringResource(Res.string.sort_recent)
        SortOrder.Alphabetical -> stringResource(Res.string.sort_alphabetical)
    }
    FlixChip(
        label = label,
        onClick = {
            onOrderChange(if (order == SortOrder.Recent) SortOrder.Alphabetical else SortOrder.Recent)
        },
        leadingIcon = Res.drawable.ic_swap_vert,
        modifier = modifier,
    )
}
