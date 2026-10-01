package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.padding

@Composable
fun BaseBrowseItem(
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onLongClickItem: () -> Unit = {},
    icon: @Composable RowScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f))
            .combinedClickable(
                onClick = onClickItem,
                onLongClick = onLongClickItem,
            )
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        content()
        action()
    }
}
