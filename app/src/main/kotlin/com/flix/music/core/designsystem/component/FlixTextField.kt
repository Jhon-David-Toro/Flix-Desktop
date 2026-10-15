package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme

/**
 * Form field (design Inputs: `.flabel` + `.tf` + `.fhelp`): label above, 44dp field in
 * surfaceVariant that turns surface with a 2dp textPrimary ring when focused, and an
 * optional helper line such as the "11 / 60" counter. Input is cut at [maxLength].
 */
@Composable
fun FlixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    maxLength: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = FlixTheme.colors

    Column(modifier) {
        Text(
            text = label,
            style = FlixTheme.typography.overline,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = { newValue -> onValueChange(newValue.take(maxLength)) },
            singleLine = singleLine,
            textStyle = FlixTheme.typography.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.primary),
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = FIELD_MIN_HEIGHT)
                        .clip(FlixTheme.shapes.md)
                        .background(if (isFocused) colors.surface else colors.surfaceVariant)
                        .then(
                            if (isFocused) Modifier.border(2.dp, colors.textPrimary, FlixTheme.shapes.md) else Modifier,
                        )
                        .padding(horizontal = FlixTheme.spacing.md, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = FlixTheme.typography.body, color = colors.textTertiary)
                    }
                    innerTextField()
                }
            },
        )
        if (helper != null) {
            Text(
                text = helper,
                style = FlixTheme.typography.overline,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

private val FIELD_MIN_HEIGHT = 44.dp
