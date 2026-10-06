package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter

/**
 * Translucent chapter quick-jump sheet, opened by tapping the iOS 26 top bar's title pill.
 * A new feature (not present in the Default theme) rather than a reskin of an existing one, with
 * its own layout (single rounded card list by default, optional grid) rather than copying any
 * other app's exact chapter-list design.
 */
@Composable
fun IosChapterListSheet(
    chapters: List<ReaderChapter>,
    currentChapterId: Long?,
    onChapterClick: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
) {
    var query by remember { mutableStateOf("") }
    var isGridMode by remember { mutableStateOf(false) }

    val filtered = remember(chapters, query) {
        if (query.isBlank()) {
            chapters
        } else {
            chapters.filter { it.chapter.name.contains(query, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 560.dp)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Icon(imageVector = Icons.Outlined.MenuBook, contentDescription = null)
                Text(
                    text = "Chapters",
                    style = MaterialTheme.typography.titleLarge,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text(text = chapters.size.toString(), color = MaterialTheme.colorScheme.primary)
                }
                Box(modifier = Modifier.weight(1f))
                IosSheetIconToggle(
                    icon = if (isGridMode) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                    onClick = { isGridMode = !isGridMode },
                )
            }

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("Search chapters...") },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )

            Box(modifier = Modifier.padding(top = 12.dp)) {
                if (isGridMode) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.chapter.id ?: it.chapter.url.hashCode().toLong() }) { item ->
                            IosChapterGridCard(
                                chapter = item.chapter,
                                selected = item.chapter.id == currentChapterId,
                                onClick = { item.chapter.id?.let(onChapterClick) },
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtered, key = { it.chapter.id ?: it.chapter.url.hashCode().toLong() }) { item ->
                            IosChapterListRow(
                                chapter = item.chapter,
                                selected = item.chapter.id == currentChapterId,
                                onClick = { item.chapter.id?.let(onChapterClick) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatChapterNumber(number: Float): String =
    if (number % 1f == 0f) number.toInt().toString() else number.toString()

@Composable
private fun IosSheetIconToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null)
    }
}

@Composable
private fun IosChapterListRow(chapter: Chapter, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(text = "Ch. ${formatChapterNumber(chapter.chapter_number)}", color = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = chapter.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun IosChapterGridCard(chapter: Chapter, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                },
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ch. ${formatChapterNumber(chapter.chapter_number)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = chapter.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
