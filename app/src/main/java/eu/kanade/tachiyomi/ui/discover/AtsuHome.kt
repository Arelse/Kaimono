package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.rememberAnimeLook

/**
 * The Atsu Home layout: one large feature card, a row of quick pills, a wide Spotlight row,
 * a numbered Ranked list and a three-up Recently released grid. Reads the anime sources the
 * same way the anime hub does, and takes its colours from the chosen anime look.
 */
@Composable
fun AtsuHomeContent(
    onOpen: (AnimeHubItem) -> Unit,
    onSchedule: () -> Unit,
    onBrowse: () -> Unit,
) {
    val look = rememberAnimeLook()
    val hub = rememberAnimeHubData()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(look.background),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { AtsuHeader(onSearch = onBrowse) }

        if (hub.loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = look.accent)
                }
            }
            return@LazyColumn
        }

        val feature = (hub.popular.firstOrNull() ?: hub.latest.firstOrNull())
        if (feature == null) {
            item {
                Text(
                    text = if (hub.sourceCount == 0) {
                        "No anime sources are enabled. Enable one in Browse."
                    } else {
                        "No anime loaded from your sources yet."
                    },
                    color = look.textMuted,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            return@LazyColumn
        }

        item { AtsuFeatureCard(item = feature, onOpen = onOpen) }
        item { AtsuPills(onSchedule = onSchedule, onBrowse = onBrowse) }

        if (hub.popular.isNotEmpty()) {
            item { AtsuSectionTitle("Spotlight") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    hub.popular.take(10).forEach { item ->
                        AtsuWideCard(item = item, onOpen = onOpen)
                    }
                }
            }
        }

        if (hub.popular.isNotEmpty()) {
            item { AtsuSectionTitle("Ranked") }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    hub.popular.take(5).forEachIndexed { index, item ->
                        AtsuRankRow(rank = index + 1, item = item, onOpen = onOpen)
                    }
                }
            }
        }

        if (hub.latest.isNotEmpty()) {
            item { AtsuSectionTitle("Recently released") }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    hub.latest.reversed().take(6).chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { item ->
                                AtsuPoster(item = item, onOpen = onOpen, modifier = Modifier.weight(1f))
                            }
                            // Keeps a short last row at the same column width.
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtsuHeader(onSearch: () -> Unit) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = look.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(look.surface)
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = look.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun AtsuFeatureCard(item: AnimeHubItem, onOpen: (AnimeHubItem) -> Unit) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(380.dp)
            .clip(RoundedCornerShape(look.cornerRadius)),
    ) {
        AsyncImage(
            model = item.anime.thumbnail_url,
            contentDescription = item.anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, look.background.copy(alpha = 0.92f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            Text(
                text = item.anime.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = look.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.anime.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                Text(
                    text = genre.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3).joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = look.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(look.cornerRadius))
                        .background(look.accent)
                        .clickable { onOpen(item) }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = look.onAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = " Play",
                            color = look.onAccent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(look.surfaceHigh)
                        .clickable { onOpen(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Details",
                        tint = look.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AtsuPills(onSchedule: () -> Unit, onBrowse: () -> Unit) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AtsuPill(
            label = "Schedule",
            color = look.surface,
            textColor = look.textPrimary,
            modifier = Modifier.weight(1f),
            onClick = onSchedule,
        )
        AtsuPill(
            label = "Browse sources",
            color = look.accent,
            textColor = look.onAccent,
            modifier = Modifier.weight(1f),
            onClick = onBrowse,
        )
    }
}

@Composable
private fun AtsuPill(
    label: String,
    color: Color,
    textColor: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val look = rememberAnimeLook()
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AtsuSectionTitle(text: String) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(look.accent),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = look.textPrimary,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun AtsuWideCard(item: AnimeHubItem, onOpen: (AnimeHubItem) -> Unit) {
    val look = rememberAnimeLook()
    Column(modifier = Modifier.width(230.dp).clickable { onOpen(item) }) {
        AsyncImage(
            model = item.anime.thumbnail_url,
            contentDescription = item.anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(look.cornerRadius)),
        )
        Text(
            text = item.anime.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = look.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AtsuRankRow(rank: Int, item: AnimeHubItem, onOpen: (AnimeHubItem) -> Unit) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(item) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = look.accent,
            modifier = Modifier.width(36.dp),
        )
        AsyncImage(
            model = item.anime.thumbnail_url,
            contentDescription = item.anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(56.dp)
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(look.cornerRadius / 2)),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = item.anime.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = look.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.anime.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                Text(
                    text = genre.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(2).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = look.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AtsuPoster(item: AnimeHubItem, onOpen: (AnimeHubItem) -> Unit, modifier: Modifier = Modifier) {
    val look = rememberAnimeLook()
    Column(modifier = modifier.clickable { onOpen(item) }) {
        AsyncImage(
            model = item.anime.thumbnail_url,
            contentDescription = item.anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(look.cornerRadius)),
        )
        Text(
            text = item.anime.title,
            style = MaterialTheme.typography.labelLarge,
            color = look.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
