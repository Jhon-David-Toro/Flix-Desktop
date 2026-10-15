package com.flix.music.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.flix.music.core.designsystem.theme.FlixTheme

/**
 * Confirmation for irreversible actions (design ConfirmDialog): elevated surface,
 * radius xl, and the confirm action in the error color when it destroys data.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FlixTheme.shapes.xl,
        containerColor = FlixTheme.colors.surfaceElevated,
        title = { Text(title, style = FlixTheme.typography.title, color = FlixTheme.colors.textPrimary) },
        text = { Text(message, style = FlixTheme.typography.body, color = FlixTheme.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = FlixTheme.typography.label,
                    color = if (destructive) FlixTheme.colors.error else FlixTheme.colors.primaryContent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, style = FlixTheme.typography.label, color = FlixTheme.colors.textSecondary)
            }
        },
    )
}
