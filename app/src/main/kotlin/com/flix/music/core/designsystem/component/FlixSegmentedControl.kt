package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** One option of a [FlixSegmentedControl]; [icon] is optional (Ajustes → Apariencia uses one). */
data class Segment<Value>(val value: Value, val label: String, val icon: DrawableResource? = null)

/**
 * Segmented control (design `.seg`): surfaceVariant track with 3dp padding; the
 * selected segment is a raised surface pill with textPrimary, the rest textSecondary.
 */
@Composable
fun <Value> FlixSegmentedControl(
    segments: List<Segment<Value>>,
    selected: Value,
    onSelect: (Value) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FlixTheme.shapes.md)
            .background(FlixTheme.colors.surfaceVariant)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        segments.forEach { segment ->
            val isSelected = segment.value == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SEGMENT_MIN_HEIGHT)
                    .then(if (isSelected) Modifier.shadow(1.dp, SEGMENT_SHAPE) else Modifier)
                    .clip(SEGMENT_SHAPE)
                    .background(if (isSelected) FlixTheme.colors.surface else Color.Transparent)
                    .handCursor().selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(segment.value) })
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            ) {
                val contentColor = if (isSelected) FlixTheme.colors.textPrimary else FlixTheme.colors.textSecondary
                segment.icon?.let { icon ->
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = segment.label,
                    style = FlixTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor,
                )
            }
        }
    }
}

private val SEGMENT_SHAPE = RoundedCornerShape(9.dp)
private val SEGMENT_MIN_HEIGHT = 34.dp
