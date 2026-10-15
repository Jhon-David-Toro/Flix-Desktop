package com.flix.music.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AssistChip as MaterialAssistChip
import androidx.compose.material3.AssistChipDefaults as MaterialAssistChipDefaults
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ChipElevation
import androidx.compose.material3.FilledIconButton as MaterialFilledIconButton
import androidx.compose.material3.FilledTonalIconButton as MaterialFilledTonalIconButton
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.FilterChipDefaults as MaterialFilterChipDefaults
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.Switch as MaterialSwitch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * On desktop, everything you can click shows the hand cursor. These are Material's own
 * controls with that one addition and the same parameters, so code shared with the
 * phone only swaps the import.
 */
fun Modifier.handCursor(): Modifier = pointerHoverIcon(PointerIcon.Hand)

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = MaterialIconButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, colors = colors, interactionSource = interactionSource, content = content)

@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = MaterialFilledIconButton(onClick, modifier.handCursor(), enabled, shape, colors, interactionSource, content)

@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = MaterialFilledTonalIconButton(onClick, modifier.handCursor(), enabled, shape, colors, interactionSource, content)

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = MaterialTextButton(onClick, modifier.handCursor(), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = MaterialButton(onClick, modifier.handCursor(), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = MaterialOutlinedButton(onClick, modifier.handCursor(), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)

@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialAssistChipDefaults.shape,
    colors: ChipColors = MaterialAssistChipDefaults.assistChipColors(),
    elevation: ChipElevation? = MaterialAssistChipDefaults.assistChipElevation(),
    border: BorderStroke? = MaterialAssistChipDefaults.assistChipBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
) = MaterialAssistChip(onClick, label, modifier.handCursor(), enabled, leadingIcon, trailingIcon, shape, colors, elevation, border, interactionSource)

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialFilterChipDefaults.shape,
    colors: SelectableChipColors = MaterialFilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = MaterialFilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = MaterialFilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) = MaterialFilterChip(selected, onClick, label, modifier.handCursor(), enabled, leadingIcon, trailingIcon, shape, colors, elevation, border, interactionSource)

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) = MaterialSwitch(checked, onCheckedChange, modifier.handCursor(), thumbContent, enabled, colors, interactionSource)
