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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AnimeLook
import eu.kanade.presentation.theme.rememberAnimeLook
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.isNovelSource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** The four categories on Atsu Home. */
enum class AtsuCategory(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Outlined.GridView),
    ANIME("Anime", Icons.Outlined.Movie),
    MANGA("Manga", Icons.Outlined.Book),
    NOVELS("Novels", Icons.AutoMirrored.Outlined.MenuBook),
}

/** What Atsu Home needs from the discover data: the sources and their first popular page. */
data class AtsuMangaData(
    val sources: List<CatalogueSource>,
    val popular: Map<Long, List<Manga>>,
)

/** Actions the header and cards call back into the Home tab. */
data class AtsuActions(
    val onOpenAnime: (AnimeHubItem) -> Unit,
    val onOpenManga: (Manga) -> Unit,
    val onSchedule: () -> Unit,
    val onBrowse: () -> Unit,
    val onSync: () -> Unit,
    val onLeaderboard: () -> Unit,
    val onAccount: () -> Unit,
)

@Composable
fun AtsuHomeContent(
    manga: AtsuMangaData,
    actions: AtsuActions,
) {
    val look = rememberAnimeLook()
    val hub = rememberAnimeHubData()
    var category by remember { mutableStateOf(AtsuCategory.ALL) }
    var showTheme by remember { mutableStateOf(false) }

    if (showTheme) AtsuThemeDialog(onDismiss = { showTheme = false })

    val mangaSources = manga.sources.filter { !it.isNovelSource() }
    val novelSources = manga.sources.filter { it.isNovelSource() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(look.background),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            AtsuHeader(
                onTheme = { showTheme = true },
                onSync = actions.onSync,
                onLeaderboard = actions.onLeaderboard,
                onAccount = actions.onAccount,
            )
        }
        item {
            AtsuCategoryTabs(selected = category, onSelect = { category = it })
        }

        when (category) {
            AtsuCategory.ALL -> {
                item { AtsuAnimeBlock(hub = hub, actions = actions, compact = true) }
                if (mangaSources.isNotEmpty()) {
                    item { AtsuSourceShelf(title = "Manga", sources = mangaSources, popular = manga.popular, onOpen = actions.onOpenManga) }
                }
                if (novelSources.isNotEmpty()) {
                    item { AtsuSourceShelf(title = "Novels", sources = novelSources, popular = manga.popular, onOpen = actions.onOpenManga) }
                }
            }
            AtsuCategory.ANIME -> item { AtsuAnimeBlock(hub = hub, actions = actions, compact = false) }
            AtsuCategory.MANGA -> {
                if (mangaSources.isEmpty()) {
                    item { AtsuEmpty("No manga sources are enabled. Enable one in Browse.") }
                } else {
                    mangaSources.forEach { source ->
                        item(key = "manga-${source.id}") {
                            AtsuSourceShelf(title = source.name, sources = listOf(source), popular = manga.popular, onOpen = actions.onOpenManga)
                        }
                    }
                }
            }
            AtsuCategory.NOVELS -> {
                if (novelSources.isEmpty()) {
                    item { AtsuEmpty("No novel sources are enabled. Enable one in Browse.") }
                } else {
                    novelSources.forEach { source ->
                        item(key = "novel-${source.id}") {
                            AtsuSourceShelf(title = source.name, sources = listOf(source), popular = manga.popular, onOpen = actions.onOpenManga)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtsuHeader(
    onTheme: () -> Unit,
    onSync: () -> Unit,
    onLeaderboard: () -> Unit,
    onAccount: () -> Unit,
) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = look.textPrimary,
            )
            Text(
                text = "Your sources, in one place",
                style = MaterialTheme.typography.bodySmall,
                color = look.textMuted,
            )
        }
        AtsuIconButton(icon = Icons.Outlined.Palette, description = "Theme", onClick = onTheme)
        AtsuIconButton(icon = Icons.Outlined.Refresh, description = "Sync", onClick = onSync)
        AtsuIconButton(icon = Icons.Outlined.EmojiEvents, description = "Leaderboard", onClick = onLeaderboard)
        AtsuIconButton(icon = Icons.Outlined.AccountCircle, description = "Account", onClick = onAccount)
    }
}

@Composable
private fun AtsuIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(look.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = look.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AtsuCategoryTabs(selected: AtsuCategory, onSelect: (AtsuCategory) -> Unit) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AtsuCategory.entries.forEach { category ->
            val active = category == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(look.cornerRadius))
                    .background(if (active) look.accent else Color.Transparent)
                    .clickable { onSelect(category) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = if (active) look.onAccent else look.textMuted,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = " " + category.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) look.onAccent else look.textMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The anime block: a feature card, quick pills, Spotlight, Ranked and Recently released. */
@Composable
private fun AtsuAnimeBlock(hub: AnimeHubData, actions: AtsuActions, compact: Boolean) {
    val look = rememberAnimeLook()
    if (hub.loading) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = look.accent)
        }
        return
    }
    val feature = hub.popular.firstOrNull() ?: hub.latest.firstOrNull()
    if (feature == null) {
        AtsuEmpty(
            if (hub.sourceCount == 0) "No anime sources are enabled. Enable one in Browse." else "No anime loaded from your sources yet.",
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        AtsuFeatureCard(item = feature, onOpen = actions.onOpenAnime)
        AtsuPills(onSchedule = actions.onSchedule, onBrowse = actions.onBrowse)
        if (hub.popular.isNotEmpty()) {
            AtsuSection(title = "Spotlight", subtitle = "Popular this week") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    hub.popular.take(10).forEach { item ->
                        AtsuWideCard(item = item, onOpen = actions.onOpenAnime)
                    }
                }
            }
            AtsuSection(title = "Ranked", subtitle = "Top five from your sources") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    hub.popular.take(5).forEachIndexed { index, item ->
                        AtsuRankRow(rank = index + 1, item = item, onOpen = actions.onOpenAnime)
                    }
                }
            }
        }
        if (!compact && hub.latest.isNotEmpty()) {
            AtsuSection(title = "Recently released", subtitle = "Newest episodes and seasons") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    hub.latest.reversed().take(9).chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { item ->
                                AtsuPoster(item = item, onOpen = actions.onOpenAnime, modifier = Modifier.weight(1f))
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/** A shelf of manga or novel covers for one or more sources, from the discover data. */
@Composable
private fun AtsuSourceShelf(
    title: String,
    sources: List<CatalogueSource>,
    popular: Map<Long, List<Manga>>,
    onOpen: (Manga) -> Unit,
) {
    val look = rememberAnimeLook()
    val mangas = sources.flatMap { popular[it.id].orEmpty() }.distinctBy { it.url }.take(12)
    AtsuSection(title = title, subtitle = sources.joinToString(", ") { it.name }.take(60)) {
        if (mangas.isEmpty()) {
            Text(
                text = "Loading covers from your sources...",
                style = MaterialTheme.typography.bodyMedium,
                color = look.textMuted,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                mangas.forEach { manga ->
                    AtsuMangaCard(manga = manga, onOpen = onOpen)
                }
            }
        }
    }
}

@Composable
private fun AtsuSection(
    title: String,
    subtitle: String?,
    content: @Composable () -> Unit,
) {
    val look = rememberAnimeLook()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(look.accent),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = look.textPrimary,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = look.textMuted,
                    modifier = Modifier.padding(start = 14.dp, top = 2.dp),
                )
            }
        }
        content()
    }
}

@Composable
private fun AtsuEmpty(message: String) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
            .padding(20.dp),
    ) {
        Text(text = message, color = look.textMuted, style = MaterialTheme.typography.bodyMedium)
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
                text = "FEATURED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = look.accent,
            )
            Text(
                text = item.anime.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = look.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
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
                        Text(text = " Play", color = look.onAccent, fontWeight = FontWeight.Bold)
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AtsuPill(label = "Schedule", secondary = true, modifier = Modifier.weight(1f), onClick = onSchedule)
        AtsuPill(label = "Browse sources", secondary = false, modifier = Modifier.weight(1f), onClick = onBrowse)
    }
}

@Composable
private fun AtsuPill(label: String, secondary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val look = rememberAnimeLook()
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(if (secondary) look.surface else look.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (secondary) look.textPrimary else look.onAccent,
            fontWeight = FontWeight.SemiBold,
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

@Composable
private fun AtsuMangaCard(manga: Manga, onOpen: (Manga) -> Unit) {
    val look = rememberAnimeLook()
    Column(modifier = Modifier.width(120.dp).clickable { onOpen(manga) }) {
        AsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(look.cornerRadius)),
        )
        Text(
            text = manga.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = look.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Picks one of the four anime looks. The choice is saved in Settings > UI > Anime look. */
@Composable
private fun AtsuThemeDialog(onDismiss: () -> Unit) {
    val look = rememberAnimeLook()
    val prefs = Injekt.get<UiPreferences>()
    val current by prefs.animeLook.collectPrefAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AnimeLook.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(look.cornerRadius / 2))
                            .clickable { prefs.animeLook.set(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == current, onClick = null)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = option.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
