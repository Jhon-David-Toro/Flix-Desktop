package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flix.music.core.designsystem.theme.FlixTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The app's single bottom sheet (design "Bottom sheet: sistema único"): elevated
 * surface, 24dp top corners and a 32 × 4 handle. Content is built from
 * [SheetHeader], [SheetQuickActions] and [SheetRow]; more than 6 rows means a screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlixBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = FlixTheme.shapes.sheet,
        containerColor = FlixTheme.colors.surfaceElevated,
        contentColor = FlixTheme.colors.textPrimary,
        scrimColor = FlixTheme.colors.overlay,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 10.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .alpha(HANDLE_ALPHA)
                    .clip(FlixTheme.shapes.pill)
                    .background(FlixTheme.colors.textTertiary),
            )
        },
    ) {
        Column(Modifier.padding(bottom = FlixTheme.spacing.md), content = content)
    }
}

/** Identifies what the sheet acts on: 48dp artwork, title and subtitle. */
@Composable
fun SheetHeader(title: String, subtitle: String, artworkUrl: String?) {
    Row(
        modifier = Modifier.padding(start = FlixTheme.spacing.lg, end = FlixTheme.spacing.lg, bottom = FlixTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(FlixTheme.shapes.sm)
                .background(FlixTheme.colors.surfaceVariant),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = FlixTheme.typography.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = subtitle,
                style = FlixTheme.typography.bodySmall,
                color = FlixTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Sheet title without artwork (e.g. "Añadir a playlist"). */
@Composable
fun SheetTitle(text: String) {
    Text(
        text = text,
        style = FlixTheme.typography.subtitle,
        modifier = Modifier
            .padding(start = FlixTheme.spacing.xl, end = FlixTheme.spacing.xl, bottom = 10.dp)
            .semantics { heading() },
    )
}

/** Up to 3 quick actions as cards in one row (`.qa`). */
@Composable
fun SheetQuickActions(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FlixTheme.spacing.lg)
            .padding(top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.sm),
        content = content,
    )
}

@Composable
fun RowScope.SheetQuickAction(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = FlixTheme.colors.textPrimary,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(FlixTheme.shapes.md)
            .background(FlixTheme.colors.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = FlixTheme.spacing.xs, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = iconTint)
        Text(
            text = label,
            style = FlixTheme.typography.overline,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** A 46dp list row (`.srow`): secondary-colored icon and label. */
@Composable
fun SheetRow(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    contentColor: Color = FlixTheme.colors.textPrimary,
    iconTint: Color = FlixTheme.colors.textSecondary,
) {
    SheetRowLayout(onClick = onClick, height = SHEET_ROW_HEIGHT) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = iconTint)
        Text(label, style = FlixTheme.typography.label, color = contentColor)
    }
}

/** Row container shared by sheet lists; [height] is 46dp for actions and 56dp for playlists. */
@Composable
fun SheetRowLayout(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = SHEET_ROW_HEIGHT,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = FlixTheme.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

private val SHEET_ROW_HEIGHT = 46.dp
private const val HANDLE_ALPHA = 0.6f
