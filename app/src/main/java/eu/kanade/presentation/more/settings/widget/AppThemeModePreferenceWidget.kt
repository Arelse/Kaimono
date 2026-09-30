package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.ThemeMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val options = mapOf(
    ThemeMode.SYSTEM to MR.strings.theme_system,
    ThemeMode.LIGHT to MR.strings.theme_light,
    ThemeMode.DARK to MR.strings.theme_dark,
)

@Composable
internal fun AppThemeModePreferenceWidget(
    value: ThemeMode,
    onItemClick: (ThemeMode) -> Unit,
) {
    BasePreferenceWidget(
        subcomponent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PrefsHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                options.forEach { (mode, labelRes) ->
                    ThemeModePreviewItem(
                        modifier = Modifier.weight(1f),
                        mode = mode,
                        label = stringResource(labelRes),
                        selected = mode == value,
                        onClick = { onItemClick(mode) },
                    )
                }
            }
        },
    )
}

@Composable
private fun RowScope.ThemeModePreviewItem(
    modifier: Modifier = Modifier,
    mode: ThemeMode,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(
                    width = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else DividerDefaults.color,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(3.dp)
                .clip(RoundedCornerShape(11.dp))
                .clickable(onClick = onClick),
        ) {
            when (mode) {
                ThemeMode.LIGHT -> ThemeModeCardContent(isDark = false)
                ThemeMode.DARK -> ThemeModeCardContent(isDark = true)
                ThemeMode.SYSTEM -> Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) { ThemeModeCardContent(isDark = false, showSwatches = false) }
                    Box(modifier = Modifier.weight(1f)) { ThemeModeCardContent(isDark = true, showSwatches = false) }
                }
            }
        }
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ThemeModeCardContent(
    isDark: Boolean,
    showSwatches: Boolean = true,
) {
    val cardBg = if (isDark) Color(0xFF17151C) else Color(0xFFF4F2FA)
    val swatchColors = if (isDark) {
        listOf(Color(0xFF9B8AE6), Color(0xFF7A6BC4), Color(0xFFC9BEF2))
    } else {
        listOf(Color(0xFF3B2E7A), Color(0xFF6B5AAE), Color(0xFFCFC6EE))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(6.dp),
    ) {
        if (showSwatches) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                swatchColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(swatchColors.first()),
            )
        }
    }
}
