package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.ic_close
import com.flix.music.resources.ic_search
import org.jetbrains.compose.resources.painterResource

/**
 * Search input (design `.field`): one line, [height] tall (44 dp, 38 dp next to a chip),
 * surfaceVariant at rest and surface with a 2 dp textPrimary outline while focused.
 * The placeholder never wraps; it ends with an ellipsis.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = DEFAULT_HEIGHT,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = FlixTheme.colors
    val shape = FlixTheme.shapes.md
    val textStyle = FlixTheme.typography.body.copy(fontSize = 13.5.sp, lineHeight = 18.sp, color = colors.textPrimary)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(if (isFocused) colors.surface else colors.surfaceVariant, shape)
                    .border(FOCUS_OUTLINE, if (isFocused) colors.textPrimary else Color.Transparent, shape)
                    .padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_search),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(ICON_SIZE),
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(CLEAR_BUTTON_SIZE)) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = clearLabel,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(ICON_SIZE),
                        )
                    }
                }
            }
        },
    )
}

/** Height next to a chip, as in Favoritos (design: `.field` at 38 px). */
val SearchFieldCompactHeight = 38.dp

private val DEFAULT_HEIGHT = 44.dp
private val ICON_SIZE = 20.dp
private val CLEAR_BUTTON_SIZE = 32.dp
private val FOCUS_OUTLINE = 2.dp
private const val DISABLED_ALPHA = 0.4f

