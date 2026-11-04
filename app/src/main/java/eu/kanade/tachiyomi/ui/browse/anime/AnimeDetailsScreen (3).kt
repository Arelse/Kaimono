package eu.kanade.tachiyomi.ui.browse.anime

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.ui.browse.source.isUsableAnimeSource
import eu.kanade.tachiyomi.ui.player.AnimePlayerScreen
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.isNovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.draw.blur
import eu.kanade.presentation.theme.rememberAnimeLook
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.TextButton

/**
 * Details for one anime from an installed source. The header carries the backdrop, poster, title,
 * meta and genres, with Play and My List under it. Three tabs follow: Overview (synopsis, facts and
 * related titles), Episodes (sortable, paged in ranges, numbered tiles) and Related (a grid).
 */
data class AnimeDetailsScreen(
    val sourceId: Long,
    val animeUrl: String,
    val title: String,
    val thumbnailUrl: String?,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = viewModel<AnimeDetailsViewModel>(
            key = "anime-details-$sourceId-$animeUrl",
            factory = viewModelFactory {
                initializer { AnimeDetailsViewModel(sourceId, animeUrl, title, thumbnailUrl) }
            },
        )
        val state by viewModel.state.collectAsState()
        val look = rememberAnimeLook()
        val anime = state.anime
        val displayTitle = anime?.title ?: title
        val episodeCount = state.episodes.size
        val statusText = statusLabel(anime?.status ?: 0)
        val metaLine = if (episodeCount > 0) "$episodeCount episodes · $statusText" else statusText
        val genres = anime?.genre?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
        var tab by remember { mutableStateOf(DetailsTab.OVERVIEW) }

        val playbackUnavailable: () -> Unit = {
            Toast.makeText(context, "No episodes to play yet", Toast.LENGTH_SHORT).show()
        }
        var saved by remember(sourceId, animeUrl) {
            mutableStateOf(AnimeMyList.contains(sourceId, animeUrl))
        }
        val toggleMyList: () -> Unit = {
            saved = AnimeMyList.toggle(
                sourceId = sourceId,
                url = animeUrl,
                title = displayTitle,
                thumbnailUrl = anime?.thumbnail_url ?: thumbnailUrl,
            )
            Toast.makeText(
                context,
                if (saved) "Added to My List" else "Removed from My List",
                Toast.LENGTH_SHORT,
            ).show()
        }
        val playEpisode: (SEpisode) -> Unit = { episode ->
            navigator.push(
                AnimePlayerScreen(
                    sourceId = sourceId,
                    animeUrl = animeUrl,
                    title = displayTitle,
                    thumbnailUrl = thumbnailUrl,
                    initialEpisodeUrl = episode.url,
                ),
            )
        }
        val openRelated: (SAnime) -> Unit = { related ->
            navigator.push(
                AnimeDetailsScreen(
                    sourceId = sourceId,
                    animeUrl = related.url,
                    title = related.title,
                    thumbnailUrl = related.thumbnail_url,
                ),
            )
        }
        val share: () -> Unit = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, displayTitle)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(look.background),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                DetailsHero(
                    imageUrl = anime?.thumbnail_url ?: thumbnailUrl,
                    title = displayTitle,
                    metaLine = metaLine,
                    genres = genres,
                    onBack = { navigator.pop() },
                    onShare = share,
                )
            }
            item {
                DetailsActionRow(
                    canPlay = state.episodes.isNotEmpty(),
                    onPlay = { state.episodes.firstOrNull()?.let(playEpisode) ?: playbackUnavailable() },
                    saved = saved,
                    onMyList = toggleMyList,
                )
            }
            item {
                DetailsTabs(selected = tab, onSelect = { tab = it })
            }

            if (state.isLoading && state.episodes.isEmpty() && state.error == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = look.accent)
                    }
                }
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            when (tab) {
                DetailsTab.OVERVIEW -> {
                    item {
                        OverviewSection(
                            description = anime?.description,
                            episodeCount = episodeCount,
                            statusText = statusText,
                            formats = state.formats,
                        )
                    }
                    if (state.related.isNotEmpty()) {
                        item { DetailsSectionTitle("You may also like") }
                        item { RelatedStrip(items = state.related, onOpen = openRelated) }
                    }
                }
                DetailsTab.EPISODES -> {
                    if (state.episodes.isEmpty() && !state.isLoading) {
                        item { DetailsEmpty("No episodes listed for this title yet.") }
                    } else {
                        item { EpisodesSection(episodes = state.episodes, onPlay = playEpisode) }
                    }
                }
                DetailsTab.RELATED -> {
                    if (state.related.isEmpty()) {
                        item { DetailsEmpty("No related titles from this source.") }
                    } else {
                        item { RelatedGrid(items = state.related, onOpen = openRelated) }
                    }
                }
            }
        }
    }
}

private enum class DetailsTab(val label: String) {
    OVERVIEW("Overview"),
    EPISODES("Episodes"),
    RELATED("Related"),
}

/** Episodes per range chip, and tiles per row in the episode grid. */
private const val EPISODE_RANGE_SIZE = 24
private const val EPISODE_COLUMNS = 4
private const val RELATED_COLUMNS = 3

/** Formats search: how many sources to ask, the per-source timeout, and the order of labels. */
private const val MAX_FORMAT_SOURCES = 12
private const val FORMAT_SEARCH_TIMEOUT_MS = 8_000L
private val FORMAT_ORDER = listOf("Manga", "Manhwa", "Manhua", "Novel")

@Composable
private fun DetailsHero(
    imageUrl: String?,
    title: String,
    metaLine: String,
    genres: List<String>,
    onBack: () -> Unit,
    onShare: () -> Unit,
) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        // Blurred backdrop: the artwork, softened and fading into the page background.
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            look.background.copy(alpha = 0.35f),
                            look.background.copy(alpha = 0.8f),
                            look.background,
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DetailsIconButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, description = "Back", onClick = onBack)
            DetailsIconButton(icon = Icons.Outlined.Share, description = "Share", onClick = onShare)
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(look.cornerRadius))
                    .background(look.surface),
            )
            Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = look.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = look.textMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (genres.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        genres.take(4).forEach { GenreChip(text = it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChip(text: String) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(look.surfaceHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = look.textMuted)
    }
}

@Composable
private fun DetailsActionRow(
    canPlay: Boolean,
    saved: Boolean,
    onPlay: () -> Unit,
    onMyList: () -> Unit,
) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onPlay,
            enabled = canPlay,
            shape = RoundedCornerShape(look.cornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = look.accent,
                contentColor = look.onAccent,
            ),
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "  Play",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        DetailsIconButton(
            icon = if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            description = if (saved) "Remove from My List" else "Add to My List",
            onClick = onMyList,
            filled = false,
            active = saved,
        )
    }
}

@Composable
private fun DetailsTabs(selected: DetailsTab, onSelect: (DetailsTab) -> Unit) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DetailsTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(look.cornerRadius))
                    .background(if (active) look.accent else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) look.onAccent else look.textMuted,
                )
            }
        }
    }
}

@Composable
private fun OverviewSection(
    description: String?,
    episodeCount: Int,
    statusText: String,
    formats: List<String>,
) {
    val look = rememberAnimeLook()
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailsInfoTile(
                label = "Episodes",
                value = if (episodeCount > 0) episodeCount.toString() else "None yet",
                modifier = Modifier.weight(1f),
            )
            DetailsInfoTile(label = "Status", value = statusText, modifier = Modifier.weight(1f))
            DetailsInfoTile(
                label = "Versions",
                value = formats.joinToString(", ").ifEmpty { "Default" },
                modifier = Modifier.weight(1f),
            )
        }
        val text = description?.trim().orEmpty()
        if (text.isNotEmpty()) {
            DetailsSynopsis(text = text)
        } else {
            Text(
                text = "No synopsis from this source.",
                style = MaterialTheme.typography.bodyMedium,
                color = look.textMuted,
            )
        }
    }
}

@Composable
private fun DetailsInfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    val look = rememberAnimeLook()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
            .padding(12.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = look.textMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = look.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DetailsSynopsis(text: String) {
    val look = rememberAnimeLook()
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = look.textPrimary.copy(alpha = 0.85f),
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }) {
            Text(text = if (expanded) "Show less" else "Read more", color = look.accent)
        }
    }
}

@Composable
private fun DetailsSectionTitle(text: String) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
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
private fun DetailsEmpty(message: String) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
            .padding(20.dp),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = look.textMuted)
    }
}

@Composable
private fun EpisodesSection(episodes: List<SEpisode>, onPlay: (SEpisode) -> Unit) {
    val look = rememberAnimeLook()
    var newestFirst by remember { mutableStateOf(false) }
    var rangeIndex by remember { mutableStateOf(0) }

    val ordered = if (newestFirst) episodes.asReversed() else episodes
    val ranges = ordered.chunked(EPISODE_RANGE_SIZE)
    val current = rangeIndex.coerceIn(0, (ranges.size - 1).coerceAtLeast(0))
    val visible = ranges.getOrNull(current).orEmpty()

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${episodes.size} episodes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = look.textPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { newestFirst = !newestFirst }) {
                Text(text = if (newestFirst) "Newest first" else "Oldest first", color = look.accent)
            }
        }
        if (ranges.size > 1) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ranges.forEachIndexed { index, chunk ->
                    val first = formatEpisodeNumber(chunk.first().episode_number)
                    val last = formatEpisodeNumber(chunk.last().episode_number)
                    DetailsRangeChip(
                        label = "$first-$last",
                        selected = index == current,
                        onClick = { rangeIndex = index },
                    )
                }
            }
        }
        visible.chunked(EPISODE_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { episode ->
                    EpisodeTile(
                        number = formatEpisodeNumber(episode.episode_number),
                        modifier = Modifier.weight(1f),
                        onClick = { onPlay(episode) },
                    )
                }
                repeat(EPISODE_COLUMNS - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DetailsRangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) look.accent else look.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) look.onAccent else look.textPrimary,
        )
    }
}

@Composable
private fun EpisodeTile(number: String, modifier: Modifier, onClick: () -> Unit) {
    val look = rememberAnimeLook()
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = look.textPrimary,
        )
    }
}

@Composable
private fun RelatedStrip(items: List<SAnime>, onOpen: (SAnime) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { anime ->
            RelatedPoster(anime = anime, modifier = Modifier.width(120.dp), onClick = { onOpen(anime) })
        }
    }
}

@Composable
private fun RelatedGrid(items: List<SAnime>, onOpen: (SAnime) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items.chunked(RELATED_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { anime ->
                    RelatedPoster(anime = anime, modifier = Modifier.weight(1f), onClick = { onOpen(anime) })
                }
                repeat(RELATED_COLUMNS - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RelatedPoster(anime: SAnime, modifier: Modifier, onClick: () -> Unit) {
    val look = rememberAnimeLook()
    Column(modifier = modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = anime.thumbnail_url,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(look.cornerRadius)),
        )
        Text(
            text = anime.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = look.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun DetailsIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    filled: Boolean = true,
    active: Boolean = false,
) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (filled) Color.Black.copy(alpha = 0.4f) else look.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = when {
                filled -> Color.White
                active -> look.accent
                else -> look.textPrimary
            },
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun normalizeTitle(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

/** Manhwa is Korean, manhua is Chinese, novels are flagged by the source; everything else is manga. */
private fun formatLabel(source: CatalogueSource): String = when {
    source.isNovelSource() -> "Novel"
    source.lang == "ko" -> "Manhwa"
    source.lang.startsWith("zh") -> "Manhua"
    else -> "Manga"
}

private fun formatEpisodeNumber(number: Float): String =
    if (number % 1f == 0f) number.toInt().toString() else number.toString()

private fun statusLabel(status: Int): String = when (status) {
    SAnime.ONGOING -> "Ongoing"
    SAnime.COMPLETED -> "Finished Airing"
    SAnime.LICENSED -> "Licensed"
    SAnime.PUBLISHING_FINISHED -> "Publishing finished"
    SAnime.CANCELLED -> "Cancelled"
    else -> "Unknown status"
}

class AnimeDetailsViewModel(
    sourceId: Long,
    animeUrl: String,
    title: String,
    thumbnailUrl: String?,
) : ViewModel() {

    private val source: AnimeCatalogueSource? = AnimeExtensionManager.installedSources()
        .filterIsInstance<AnimeCatalogueSource>()
        .firstOrNull { isUsableAnimeSource(it) && it.id == sourceId }

    private val seed: SAnime = SAnime.create().apply {
        url = animeUrl
        this.title = title
        thumbnail_url = thumbnailUrl
    }

    data class State(
        val anime: SAnime? = null,
        val episodes: List<SEpisode> = emptyList(),
        val related: List<SAnime> = emptyList(),
        val formats: List<String> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State(anime = seed))
    val state: StateFlow<State> = _state.asStateFlow()

    /** The first playable stream URL the source offers for this episode, or null if none. */
    suspend fun streamUrlFor(episode: SEpisode): String? {
        val src = source ?: return null
        return withContext(Dispatchers.IO) {
            src.getVideoList(episode).firstOrNull { it.videoUrl.isNotBlank() }?.videoUrl
        }
    }

    init {
        load(animeUrl)
        loadFormats(title)
    }

    /**
     * Looks for the same title in the installed manga and novel sources. A source only counts when
     * one of its results matches the title exactly (ignoring case and punctuation).
     */
    private fun loadFormats(title: String) {
        val wanted = normalizeTitle(title)
        if (wanted.isBlank()) return
        viewModelScope.launch {
            val formats: List<String> = withContext(Dispatchers.IO) {
                val sources = Injekt.get<SourceManager>().getOnlineSources()
                    .filterIsInstance<CatalogueSource>()
                    .take(MAX_FORMAT_SOURCES)
                coroutineScope {
                    sources.map { src ->
                        async {
                            withTimeoutOrNull(FORMAT_SEARCH_TIMEOUT_MS) {
                                runCatching { src.getSearchManga(1, title, src.getFilterList()).mangas }
                                    .getOrDefault(emptyList())
                                    .filter { normalizeTitle(it.title) == wanted }
                                    .map { formatLabel(src) }
                            }.orEmpty()
                        }
                    }.awaitAll().flatten().distinct()
                }
            }
            _state.update { it.copy(formats = FORMAT_ORDER.filter { label -> label in formats }) }
        }
    }

    private fun load(animeUrl: String) {
        val src = source
        if (src == null) {
            _state.update { it.copy(isLoading = false, error = "This source is no longer installed") }
            return
        }
        viewModelScope.launch {
            // Only the details and episodes block the page. Related titles load afterwards.
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    // Some extensions return a new object without its url, which then crashes on
                    // "lateinit property url has not been initialized". Keep the url we opened with.
                    val details = src.getAnimeDetails(seed).apply { url = animeUrl }
                    val episodes = src.getEpisodeList(details)
                    Pair(details, episodes)
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { (details, episodes) ->
                        current.copy(
                            anime = details,
                            episodes = episodes,
                            isLoading = false,
                        )
                    },
                    onFailure = { e ->
                        current.copy(isLoading = false, error = e.message ?: "Failed to load anime details")
                    },
                )
            }

            // Related titles come from the popular list. They give up after 8 seconds and never
            // hold up the page; a failure just hides the row.
            val related = withContext(Dispatchers.IO) {
                kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                    runCatching {
                        src.getPopularAnime(1).animes.filter { it.url != animeUrl }.take(12)
                    }.getOrDefault(emptyList())
                }
            } ?: emptyList()
            _state.update { it.copy(related = related) }
        }
    }
}
