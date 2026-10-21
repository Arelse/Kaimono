package eu.kanade.tachiyomi.ui.browse.anime

import android.content.Intent
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

/**
 * Details for one anime from an installed source, laid out like the reference: a backdrop hero with
 * a centred title, meta and genre lines, a Share / Watch / My List row, the synopsis, the episode
 * list, and a "You May Also Like" row from the same source.
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
        val anime = state.anime
        val displayTitle = anime?.title ?: title
        val episodeCount = state.episodes.size
        val statusText = statusLabel(anime?.status ?: 0)
        val metaLine = if (episodeCount > 0) "$episodeCount episodes · $statusText" else statusText

        val playbackUnavailable: () -> Unit = {
            Toast.makeText(context, "Episode playback isn't available yet", Toast.LENGTH_SHORT).show()
        }
        val libraryUnavailable: () -> Unit = {
            Toast.makeText(context, "Anime library isn't available yet", Toast.LENGTH_SHORT).show()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                DetailsHero(
                    imageUrl = anime?.thumbnail_url ?: thumbnailUrl,
                    title = displayTitle,
                    metaLine = metaLine,
                    genreLine = anime?.genre?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
                        ?.joinToString(", "),
                    onBack = { navigator.pop() },
                )
            }

            item {
                DetailsActions(
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, displayTitle)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    onWatch = playbackUnavailable,
                    onMyList = libraryUnavailable,
                )
            }

            if (state.isLoading && state.episodes.isEmpty() && state.error == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
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

            if (state.formats.isNotEmpty()) {
                item {
                    AvailableFormatsRow(formats = state.formats)
                }
            }

            item {
                Synopsis(description = anime?.description)
            }

            if (state.episodes.isNotEmpty()) {
                item {
                    SectionTitle(text = "Episodes ($episodeCount)")
                }
                items(state.episodes, key = { it.url }) { episode ->
                    EpisodeRow(
                        episode = episode,
                        onClick = playbackUnavailable,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            if (state.related.isNotEmpty()) {
                item {
                    SectionTitle(text = "You May Also Like")
                }
                item {
                    RelatedRow(
                        items = state.related,
                        onOpen = { related ->
                            navigator.push(
                                AnimeDetailsScreen(
                                    sourceId = sourceId,
                                    animeUrl = related.url,
                                    title = related.title,
                                    thumbnailUrl = related.thumbnail_url,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsHero(
    imageUrl: String?,
    title: String,
    metaLine: String,
    genreLine: String?,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Darkens the top for the back button and fades the bottom into the page background.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                description = "Back",
                onClick = onBack,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            genreLine?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailsActions(
    onShare: () -> Unit,
    onWatch: () -> Unit,
    onMyList: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ActionIconLabel(icon = Icons.Outlined.Share, label = "Share", onClick = onShare)
        Button(
            onClick = onWatch,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .width(200.dp)
                .height(52.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "  Watch",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        ActionIconLabel(icon = Icons.Outlined.FavoriteBorder, label = "My List", onClick = onMyList)
    }
}

@Composable
private fun ActionIconLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun Synopsis(description: String?) {
    var expanded by remember { mutableStateOf(false) }
    val text = description?.takeIf { it.isNotBlank() } ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { expanded = !expanded },
    )
}

@Composable
private fun AvailableFormatsRow(formats: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Also available as",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            formats.forEach { label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private const val MAX_FORMAT_SOURCES = 12
private const val FORMAT_SEARCH_TIMEOUT_MS = 8_000L
private val FORMAT_ORDER = listOf("Manga", "Manhwa", "Manhua", "Novel")

private fun normalizeTitle(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

/** Manhwa is Korean, manhua is Chinese, novels are flagged by the source; everything else is manga. */
private fun formatLabel(source: CatalogueSource): String = when {
    source.isNovelSource() -> "Novel"
    source.lang == "ko" -> "Manhwa"
    source.lang.startsWith("zh") -> "Manhua"
    else -> "Manga"
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun RelatedRow(
    items: List<SAnime>,
    onOpen: (SAnime) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { anime ->
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onOpen(anime) },
            ) {
                AsyncImage(
                    model = anime.thumbnail_url,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: SEpisode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Text(
            text = episode.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Episode ${formatEpisodeNumber(episode.episode_number)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val details = src.getAnimeDetails(seed)
                    val episodes = src.getEpisodeList(details)
                    // Related titles come from the same source's popular list; a failure just hides the row.
                    val related = runCatching {
                        src.getPopularAnime(1).animes.filter { it.url != animeUrl }.take(12)
                    }.getOrDefault(emptyList())
                    Triple(details, episodes, related)
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { (details, episodes, related) ->
                        current.copy(
                            anime = details,
                            episodes = episodes,
                            related = related,
                            isLoading = false,
                        )
                    },
                    onFailure = { e ->
                        current.copy(isLoading = false, error = e.message ?: "Failed to load anime details")
                    },
                )
            }
        }
    }
}
