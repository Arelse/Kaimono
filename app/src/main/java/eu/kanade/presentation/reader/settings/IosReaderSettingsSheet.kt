package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.Dialog
import eu.kanade.presentation.components.dialogProperties
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.ReaderControlTheme
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.util.collectAsState

/**
 * Translucent, pill-segmented-tab bottom sheet for the reader settings, used only when
 * [ReaderControlTheme.IOS26] is selected. The [ReaderControlTheme.DEFAULT] path in
 * ReaderSettingsDialog.kt is untouched by this file.
 */
@Composable
internal fun IosReaderSettingsSheet(
    tabLabels: List<String>,
    pagerState: PagerState,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.(Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .heightIn(max = maxHeight * 0.85f)
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            ) {
                Column {
                    // Grab handle
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                    ) {}

                    Text(
                        text = "Reader Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 16.dp, bottom = 16.dp),
                    )

                    IosPillTabRow(
                        tabLabels = tabLabels,
                        selectedIndex = pagerState.currentPage,
                        onSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    HorizontalPager(
                        state = pagerState,
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            content(page)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosPillTabRow(
    tabLabels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabLabels.fastForEachIndexed { index, label ->
            IosPillTab(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RowScope.IosPillTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0f),
            )
            .clickableNoRipple(onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.then(
        androidx.compose.foundation.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick,
        ),
    ),
)

/**
 * "Control Theme" settings row + its picker dialog (Default / iOS 26), styled after the app's
 * existing "Nav bar style" picker dialog pattern.
 */
@Composable
internal fun IosControlThemeRow(viewModel: ReaderSettingsViewModel) {
    val controlTheme by viewModel.preferences.controlTheme.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    eu.kanade.presentation.more.settings.widget.TextPreferenceWidget(
        title = "Control Theme",
        subtitle = if (controlTheme == ReaderControlTheme.IOS26) "iOS 26" else "Default",
        onPreferenceClick = { showDialog = true },
    )

    if (showDialog) {
        ControlThemeDialog(
            current = controlTheme,
            onDismissRequest = { showDialog = false },
            onConfirm = {
                viewModel.preferences.controlTheme.set(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun ControlThemeDialog(
    current: ReaderControlTheme,
    onDismissRequest: () -> Unit,
    onConfirm: (ReaderControlTheme) -> Unit,
) {
    var selected by remember { mutableStateOf(current) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Control Theme") },
        text = {
            Column {
                ReaderControlTheme.entries.forEach { theme ->
                    val label = if (theme == ReaderControlTheme.IOS26) "iOS 26" else "Default"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { selected = theme }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == theme, onClick = { selected = theme })
                        Text(text = label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
    )
}
