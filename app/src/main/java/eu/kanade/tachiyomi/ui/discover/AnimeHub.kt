package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.ui.browse.source.isUsableAnimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val MAX_SOURCES = 6
private const val MAX_ITEMS = 24

/** An anime from one installed source, with the source id needed to open its details. */
data class AnimeHubItem(val sourceId: Long, val anime: SAnime)

data class AnimeHubData(
    val popular: List<AnimeHubItem> = emptyList(),
    val latest: List<AnimeHubItem> = emptyList(),
    val loading: Boolean = true,
    val sourceCount: Int = 0,
)

/**
 * Loads the first page of popular and latest anime from the installed anime sources. Failing
 * sources are skipped so one broken extension doesn't empty the whole Anime home.
 */
@Composable
fun rememberAnimeHubData(): AnimeHubData {
    var data by remember { mutableStateOf(AnimeHubData()) }
    LaunchedEffect(Unit) {
        val sources = withContext(Dispatchers.IO) {
            AnimeExtensionManager.installedSources()
                .filterIsInstance<AnimeCatalogueSource>()
                .filter { isUsableAnimeSource(it) }
        }
        val loaded = withContext(Dispatchers.IO) {
            coroutineScope {
                sources.take(MAX_SOURCES).map { src ->
                    async {
                        val popular = runCatching { src.getPopularAnime(1).animes }.getOrDefault(emptyList())
                        val latest = runCatching { src.getLatestUpdates(1).animes }.getOrDefault(emptyList())
                        popular.map { AnimeHubItem(src.id, it) } to latest.map { AnimeHubItem(src.id, it) }
                    }
                }.awaitAll()
            }
        }
        data = AnimeHubData(
            popular = loaded.flatMap { it.first }.take(MAX_ITEMS),
            latest = loaded.flatMap { it.second }.take(MAX_ITEMS),
            loading = false,
            sourceCount = sources.size,
        )
    }
    return data
}

/**
 * The Anime category body of the home screen: a featured hero carousel with quick tiles, then the
 * Spotlight, Trending and Recently released rows.
 */
fun LazyListScope.animeHubItems(
    hub: AnimeHubData,
    accent: Color,
    onOpen: (AnimeHubItem) -> Unit,
    onSchedule: () -> Unit,
    onGenres: () -> Unit,
) {
    if (hub.loading) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = accent)
            }
        }
        return
    }

    if (hub.popular.isEmpty() && hub.latest.isEmpty()) {
        item {
            Text(
                text = if (hub.sourceCount == 0) {
                    "No anime sources installed. Install an anime extension to see titles here."
                } else {
                    "No anime loaded from your sources yet."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        return
    }

    item {
        AnimeHeroPager(
            items = hub.popular.take(5).ifEmpty { hub.latest.take(5) },
            accent = accent,
            onOpen = onOpen,
        )
    }
    item {
        AnimeQuickTiles(
            backdrop = hub.latest.firstOrNull()?.anime?.thumbnail_url
                ?: hub.popular.firstOrNull()?.anime?.thumbnail_url,
            onSchedule = onSchedule,
            onGenres = onGenres,
        )
    }
    if (hub.popular.isNotEmpty()) {
        item { AnimeSectionHeader("Spotlight") }
        item { AnimeCardRow(items = hub.popular.take(12), onOpen = onOpen) }
    }
    if (hub.latest.isNotEmpty()) {
        item { AnimeSectionHeader("Trending") }
        item { AnimeCardRow(items = hub.latest.take(12), onOpen = onOpen) }
        item { AnimeSectionHeader("Recently released") }
        item { AnimeCardRow(items = hub.latest.reversed().take(12), onOpen = onOpen) }
    }
}

@Composable
private fun AnimeHeroPager(
    items: List<AnimeHubItem>,
    accent: Color,
    onOpen: (AnimeHubItem) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    // Advance every 5 seconds unless the user is dragging the card.
    LaunchedEffect(items.size) {
        if (items.size > 1) {
            while (true) {
                delay(5_000)
                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            AnimeHeroCard(item = items[page], accent = accent, onOpen = onOpen)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            items.indices.forEach { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (selected) 24.dp else 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (selected) accent else Color.White.copy(alpha = 0.25f)),
                )
            }
        }
    }
}

@Composable
private fun AnimeHeroCard(
    item: AnimeHubItem,
    accent: Color,
    onOpen: (AnimeHubItem) -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(shape),
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.anime.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.anime.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                Text(
                    text = genre.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .take(3)
                        .joinToString("  ·  ")
                        .uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onOpen(item) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = "  Play")
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onOpen(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Details",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeQuickTiles(
    backdrop: String?,
    onSchedule: () -> Unit,
    onGenres: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimeQuickTile(
            label = "Schedule & lists",
            imageUrl = backdrop,
            modifier = Modifier.weight(1f),
            onClick = onSchedule,
        )
        AnimeQuickTile(
            label = "Genres",
            imageUrl = backdrop,
            modifier = Modifier.weight(1f),
            onClick = onGenres,
        )
    }
}

@Composable
private fun AnimeQuickTile(
    label: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun AnimeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun AnimeCardRow(
    items: List<AnimeHubItem>,
    onOpen: (AnimeHubItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onOpen(item) },
            ) {
                AsyncImage(
                    model = item.anime.thumbnail_url,
                    contentDescription = item.anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(14.dp)),
                )
                Text(
                    text = item.anime.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
