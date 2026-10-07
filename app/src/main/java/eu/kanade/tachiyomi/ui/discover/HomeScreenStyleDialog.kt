package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.HomeScreenStyle

/**
 * The Home Screen Switcher. Lists every [HomeScreenStyle] with a small live swatch (built
 * from that style's own tokens, not a screenshot) so the person can see roughly what they're
 * picking before they commit. Selecting a row writes straight to the preference that
 * [DiscoverTab] and the floating nav bar in HomeScreen both read, so the whole UI - Home tab
 * content and nav buttons alike - updates immediately.
 */
@Composable
fun HomeScreenStyleDialog(
    current: HomeScreenStyle,
    onSelect: (HomeScreenStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Home screen style") },
        text = {
            LazyColumn {
                items(HomeScreenStyle.entries) { style ->
                    HomeScreenStyleRow(
                        style = style,
                        selected = style == current,
                        onClick = { onSelect(style) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun HomeScreenStyleRow(
    style: HomeScreenStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = style.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Live swatch: a little rounded tile painted with the style's own background/accent,
        // so the list doubles as a preview rather than just names.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.background),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (tokens.progressGradient) {
                            Brush.linearGradient(listOf(tokens.accent, tokens.accentSecondary))
                        } else {
                            Brush.linearGradient(listOf(tokens.accent, tokens.accent))
                        },
                    ),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = style.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = style.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
