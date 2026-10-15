package com.flix.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.resources.Res
import com.flix.music.resources.ic_person
import org.jetbrains.compose.resources.painterResource

/** Local profile avatar (there is no account yet): a person icon on surfaceVariant. */
@Composable
fun ProfileAvatar(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(FlixTheme.shapes.circle)
            .background(FlixTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_person),
            contentDescription = null,
            tint = FlixTheme.colors.textSecondary,
            modifier = Modifier.size(size / 2),
        )
    }
}
