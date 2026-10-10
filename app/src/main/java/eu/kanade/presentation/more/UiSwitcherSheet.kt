package eu.kanade.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.UiMode
import tachiyomi.presentation.core.util.collectAsState

/**
 * Translucent "UI Switcher" sheet, opened from the More screen's App section. Picks between the
 * three UiMode values (translucency/card treatment only - see SettingsGroup's cardAlpha param;
 * this does NOT touch the app's actual color scheme, which stays fully governed by the Theme
 * group in Settings > Appearance, same as before this feature existed).
 *
 * Gradient customization used to live here too, but now lives in Settings > Appearance >
 * Flourish, alongside the pre-existing "Accent gradient background" / "Accent color" controls it
 * shares a mechanism with (see getFlourishGroup in SettingsAppearanceScreen.kt and
 * AccentGradientBackground in FlourishOverlay.kt) - this sheet no longer duplicates it.
 */
@Composable
fun UiSwitcherSheet(
    uiPreferences: UiPreferences,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
) {
    val uiMode by uiPreferences.uiMode.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 460.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Palette, contentDescription = null)
                Text("UI Switcher", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = "MODE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
            ) {
                UiMode.entries.forEachIndexed { index, mode ->
                    UiModeRow(
                        mode = mode,
                        selected = mode == uiMode,
                        onClick = { uiPreferences.uiMode.set(mode) },
                    )
                    if (index != UiMode.entries.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }

            Text(
                text = "Looking for the gradient option? It moved to Settings > Appearance > Flourish.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, start = 4.dp),
            )
            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun UiModeRow(mode: UiMode, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(mode.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// Fixed preset palette - keeps the gradient picker self-contained without a color-picker dependency.
private val gradientPresets = listOf(
    Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFF453A),
    Color(0xFFFF9F0A), Color(0xFFBF5AF2), Color(0xFFFFD60A),
    Color(0xFFFFFFFF), Color(0xFF000000),
)

/**
 * A row of preset color swatches with a checkmark on the selected one. Used by the Flourish
 * group's "Custom gradient colors" controls (SettingsAppearanceScreen.kt). Internal rather than
 * private since it's now shared across files in this module.
 */
@Composable
internal fun GradientSwatchPicker(
    label: String,
    selected: Color,
    modifier: Modifier = Modifier,
    onSelect: (Color) -> Unit,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.padding(top = 6.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gradientPresets.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onSelect(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (color == selected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

internal fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)
