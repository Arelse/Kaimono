package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Reorder and show/hide bottom-nav tabs with up/down arrows and an eye toggle.
 * [lockedKey] can't be hidden (the Settings tab, so the user can always get back here).
 */
@Composable
fun NavTabReorderDialog(
    initialOrder: List<String>,
    initialHidden: Set<String>,
    lockedKey: String,
    labelFor: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (order: List<String>, hidden: Set<String>) -> Unit,
) {
    val order = remember { mutableStateListOf<String>().apply { addAll(initialOrder) } }
    var hidden by remember { mutableStateOf(initialHidden - lockedKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reorder navigation tabs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                order.forEachIndexed { index, key ->
                    val isHidden = key in hidden
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            text = labelFor(key),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            color = if (isHidden) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    order.add(index - 1, order.removeAt(index))
                                }
                            },
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(
                            onClick = {
                                if (index < order.lastIndex) {
                                    order.add(index + 1, order.removeAt(index))
                                }
                            },
                            enabled = index < order.lastIndex,
                        ) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
                        }
                        IconButton(
                            onClick = {
                                hidden = if (isHidden) hidden - key else hidden + key
                            },
                            enabled = key != lockedKey,
                        ) {
                            Icon(
                                imageVector = if (isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (isHidden) "Show tab" else "Hide tab",
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(order.toList(), hidden) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
