package eu.kanade.presentation.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Schedule-style Updates list: a row of day chips, a month calendar that can be toggled on, and a
 * timeline of the selected day's chapter updates. Colours all come from the theme, and the
 * background is the theme background (pure black only when Pure AMOLED is on, via the theme).
 */
@Composable
internal fun UpdatesScheduleContent(
    items: List<UpdatesItem>,
    contentPadding: PaddingValues,
    onClickUpdate: (UpdatesItem) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    var selectedDay by remember { mutableStateOf(today) }
    var calendarMode by remember { mutableStateOf(false) }
    var shownMonth by remember { mutableStateOf(YearMonth.from(today)) }

    val byDay = remember(items) {
        items.groupBy { Instant.ofEpochMilli(it.update.dateFetch).atZone(zone).toLocalDate() }
    }
    val dayItems = (byDay[selectedDay] ?: emptyList()).sortedByDescending { it.update.dateFetch }
    val accent = MaterialTheme.colorScheme.primary
    val chipDays = remember(today) { (-6..1).map { today.plusDays(it.toLong()) } }
    val chipState = rememberLazyListState(initialFirstVisibleItemIndex = chipDays.size - 3)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item(key = "schedule_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Updates",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { calendarMode = !calendarMode }) {
                    Icon(
                        imageVector = if (calendarMode) Icons.Outlined.ViewList else Icons.Outlined.CalendarMonth,
                        contentDescription = if (calendarMode) "List view" else "Calendar view",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        item(key = "schedule_days") {
            LazyRow(
                state = chipState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(chipDays, key = { it.toEpochDay() }) { day ->
                    DayChip(
                        day = day,
                        today = today,
                        selected = day == selectedDay,
                        count = byDay[day]?.size ?: 0,
                        accent = accent,
                        onClick = {
                            selectedDay = day
                            shownMonth = YearMonth.from(day)
                        },
                    )
                }
            }
        }

        if (calendarMode) {
            item(key = "schedule_calendar") {
                MonthCalendar(
                    month = shownMonth,
                    today = today,
                    selectedDay = selectedDay,
                    countFor = { byDay[it]?.size ?: 0 },
                    accent = accent,
                    onPrevious = { shownMonth = shownMonth.minusMonths(1) },
                    onNext = { shownMonth = shownMonth.plusMonths(1) },
                    onSelect = { day ->
                        selectedDay = day
                        shownMonth = YearMonth.from(day)
                    },
                )
            }
        }

        item(key = "schedule_day_heading") {
            Text(
                text = selectedDay.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
        }

        if (dayItems.isEmpty()) {
            item(key = "schedule_empty") {
                Text(
                    text = "No updates on this day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            items(dayItems, key = { "schedule_${it.update.chapterId}" }) { item ->
                ScheduleUpdateRow(
                    item = item,
                    accent = accent,
                    zone = zone,
                    onClickUpdate = { onClickUpdate(item) },
                    onClickCover = { onClickCover(item) },
                )
            }
        }
    }
}

@Composable
private fun DayChip(
    day: LocalDate,
    today: LocalDate,
    selected: Boolean,
    count: Int,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val relative = when (day) {
        today.minusDays(1) -> "Yesterday"
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(shape)
            .background(if (selected) accent else MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = relative,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
        Text(
            text = day.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.8f),
        )
        if (count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) contentColor else accent,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    today: LocalDate,
    selectedDay: LocalDate,
    countFor: (LocalDate) -> Int,
    accent: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val leading = month.atDay(1).dayOfWeek.value - 1 // Monday = 0
    val cells: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val padded = cells + List((7 - cells.size % 7) % 7) { null }
    val weekdays = DayOfWeek.values().toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }
        padded.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val selected = day != null && day == selectedDay
                    val count = if (day != null) countFor(day) else 0
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) accent else Color.Transparent)
                            .clickable(enabled = day != null) { day?.let(onSelect) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (day != null) {
                            val textColor = when {
                                selected -> MaterialTheme.colorScheme.onPrimary
                                day == today -> accent
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                                color = textColor,
                            )
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleUpdateRow(
    item: UpdatesItem,
    accent: Color,
    zone: ZoneId,
    onClickUpdate: () -> Unit,
    onClickCover: () -> Unit,
) {
    val update = item.update
    val time = Instant.ofEpochMilli(update.dateFetch).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val cardShape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // Timeline: a vertical line with a dot at the top of each entry.
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2f
                    drawLine(
                        color = lineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawCircle(
                        color = accent,
                        radius = 4.dp.toPx(),
                        center = Offset(x, 14.dp.toPx()),
                    )
                },
        )
        Text(
            text = time,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .width(56.dp)
                .padding(start = 8.dp, top = 10.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                .clickable(onClick = onClickUpdate)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = update.coverData,
                contentDescription = update.mangaTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 56.dp, height = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClickCover),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = update.mangaTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = update.chapterName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusPill(read = update.read, accent = accent)
            }
        }
    }
}

@Composable
private fun StatusPill(read: Boolean, accent: Color) {
    val background = if (read) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        accent.copy(alpha = 0.18f)
    }
    Text(
        text = if (read) "READ" else "NEW",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (read) MaterialTheme.colorScheme.onSurfaceVariant else accent,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
