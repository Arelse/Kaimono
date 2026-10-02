package eu.kanade.presentation.manga.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DetailsCardShape = RoundedCornerShape(24.dp)

/** Large "Start reading / Continue reading" card shown under the manga header. */
@Composable
fun MangaStartReadingCard(
    isReading: Boolean,
    nextChapterName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(DetailsCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = if (isReading) "Continue reading" else "Start reading",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            if (nextChapterName != null) {
                Text(
                    text = nextChapterName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Pulls "Rank", "Rating", and "Bookmarks/Saves/Follows" out of the source's description text,
 * since some sources (e.g. Asura) put those stats there. Missing values show as a dash.
 */
private fun parseStats(description: String?): Triple<String, String, String> {
    val text = description.orEmpty()
    fun find(pattern: String): String =
        Regex(pattern, RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim() ?: "—"
    return Triple(
        find("""Rank\s*:\s*#?\s*([0-9.,]+)""").let { if (it == "—") it else "#$it" },
        find("""Rating\s*:\s*([0-9.,]+)"""),
        find("""(?:Bookmarks|Saves|Follows)\s*:\s*([0-9.,]+\s*[KkMm]?)"""),
    )
}

@Composable
fun MangaStatsCard(
    description: String?,
    modifier: Modifier = Modifier,
) {
    val (rank, rating, saves) = remember(description) { parseStats(description) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(DetailsCardShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatColumn("RANK", rank, Modifier.weight(1f))
        StatDivider()
        StatColumn("RATING", rating, Modifier.weight(1f))
        StatDivider()
        StatColumn("SAVES", saves, Modifier.weight(1f))
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/** Search field that filters the chapter list by chapter number or name. */
@Composable
fun ChapterQuickJumpField(
    query: String,
    onQueryChange: (String) -> Unit,
    totalChapters: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Quick jump to chapter (e.g. 110)…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "$totalChapters TOTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
