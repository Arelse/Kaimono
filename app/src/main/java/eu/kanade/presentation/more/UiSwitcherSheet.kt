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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.UiMode
import tachiyomi.presentation.core.util.collectAsState

/**
 * Translucent "UI Switcher" sheet, opened from the More screen's App section. Picks between the
 * three UiMode values (sharing IosGlassColorScheme - see TachiyomiTheme) and optionally layers a
 * custom two-color gradient on top. Gradient color choice is a small fixed preset swatch palette
 * rather than a full HSV picker, to keep this self-contained without a new dependency.
 */
@Composable
fun UiSwitcherSheet(
    uiPreferences: UiPreferences,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
) {
    val uiMode by uiPreferences.uiMode.collectAsState()
    val gradientEnabled by uiPreferences.gradientThemeEnabled.collectAsState()
    val gradientStart by uiPreferences.gradientColorStart.collectAsState()
    val gradientEnd by uiPreferences.gradientColorEnd.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 620.dp)
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

            Spacer(modifier = Modifier.padding(top = 20.dp))
            Text(
                text = "GRADIENT THEME",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom gradient", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Overlay a gradient background behind glass cards",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = gradientEnabled,
                        onCheckedChange = { uiPreferences.gradientThemeEnabled.set(it) },
                    )
                }

                if (gradientEnabled) {
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        GradientSwatchPicker(
                            label = "Start",
                            selected = Color(gradientStart),
                            modifier = Modifier.weight(1f),
                            onSelect = { uiPreferences.gradientColorStart.set(it.toArgb()) },
                        )
                        GradientSwatchPicker(
                            label = "End",
                            selected = Color(gradientEnd),
                            modifier = Modifier.weight(1f),
                            onSelect = { uiPreferences.gradientColorEnd.set(it.toArgb()) },
                        )
                    }
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(gradientStart), Color(gradientEnd)),
                                ),
                            ),
                    )
                }
            }
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

@Composable
private fun GradientSwatchPicker(
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

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)
