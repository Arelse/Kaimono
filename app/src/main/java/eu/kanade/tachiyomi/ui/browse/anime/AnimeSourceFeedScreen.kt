package eu.kanade.tachiyomi.ui.browse.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.presentation.core.components.material.Scaffold

/** Which part of the feed is showing. Chips and section titles switch between them in place. */
enum class AnimeFeedMode { HOME, POPULAR, LATEST }

/** One paged list (Popular or Latest shown as a full grid). */
data class AnimeFeedListing(
    val animes: List<SAnime> = emptyList(),
    val nextPage: Int = 1,
    val hasNext: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * Anime counterpart of the manga source feed, laid out like it: filter chips, a Latest row, a
 * Popular row with title panels, and a grid of larger covers. Popular and Latest switch the feed
 * in place into a paged grid, the way the manga feed does, instead of opening the browse screen.
 * Filter still opens the browse screen, because the filter sheet lives there.
 */
data class AnimeSourceFeedScreen(
    val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel<AnimeSourceFeedViewModel>(
            key = "anime-feed-$sourceId",
            factory = viewModelFactory {
                initializer { AnimeSourceFeedViewModel(sourceId) }
            },
        )
        val state by viewModel.state.collectAsState()
        val source = viewModel.source
        val sourceName = source?.name ?: "Anime source"
        val supportsLatest = source?.supportsLatest == true

        fun openDetails(anime: SAnime) {
            navigator.push(
                AnimeDetailsScreen(
                    sourceId = sourceId,
                    animeUrl = anime.url,
                    title = anime.title,
                    thumbnailUrl = anime.thumbnail_url,
                ),
            )
        }

        fun openBrowse(startLatest: Boolean = false) {
            navigator.push(AnimeBrowseScreen(sourceId = sourceId, startLatest = startLatest))
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = sourceName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        // Opens the source's website in the WebView. Some sites (Cloudflare) only
                        // serve content after a browser session is established there.
                        val baseUrl = (source as? AnimeHttpSource)?.baseUrl.orEmpty()
                        if (baseUrl.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    navigator.push(
                                        WebViewScreen(
                                            url = baseUrl,
                                            initialTitle = sourceName,
                                            sourceId = sourceId,
                                        ),
                                    )
                                },
                            ) {
                                Icon(imageVector = Icons.Outlined.Public, contentDescription = null)
                            }
                        }
                        IconButton(onClick = { openBrowse() }) {
                            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(contentPadding = paddingValues) {
                item(key = "anime-feed-chips") {
                    FeedChips(
                        mode = state.mode,
                        supportsLatest = supportsLatest,
                        onHome = { viewModel.setMode(AnimeFeedMode.HOME) },
                        onPopular = { viewModel.setMode(AnimeFeedMode.POPULAR) },
                        onLatest = { viewModel.setMode(AnimeFeedMode.LATEST) },
                        onFilter = { openBrowse() },
                    )
                }
                when (state.mode) {
                    AnimeFeedMode.HOME -> {
                        if (supportsLatest) {
                            item(key = "anime-feed-latest") {
                                FeedRow(
                                    title = "Latest",
                                    animes = state.latest,
                                    onTitleClick = { viewModel.setMode(AnimeFeedMode.LATEST) },
                                    onAnimeClick = { openDetails(it) },
                                )
                            }
                        }
                        item(key = "anime-feed-popular") {
                            FeedRow(
                                title = "Popular",
                                animes = state.popular,
                                onTitleClick = { viewModel.setMode(AnimeFeedMode.POPULAR) },
                                onAnimeClick = { openDetails(it) },
                            )
                        }
                        // Grid of larger covers with the title over the image, three per row.
                        animeGrid(state.popular?.getOrNull().orEmpty()) { openDetails(it) }
                    }
                    AnimeFeedMode.POPULAR -> animeListing(
                        title = "Popular",
                        listing = state.popularListing,
                        onLoadMore = { viewModel.loadMore(AnimeFeedMode.POPULAR) },
                        onAnimeClick = { openDetails(it) },
                    )
                    AnimeFeedMode.LATEST -> animeListing(
                        title = "Latest",
                        listing = state.latestListing,
                        onLoadMore = { viewModel.loadMore(AnimeFeedMode.LATEST) },
                        onAnimeClick = { openDetails(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedChips(
    mode: AnimeFeedMode,
    supportsLatest: Boolean,
    onHome: () -> Unit,
    onPopular: () -> Unit,
    onLatest: () -> Unit,
    onFilter: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = mode == AnimeFeedMode.HOME,
                onClick = onHome,
                label = { Text("Home") },
                leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        item {
            FilterChip(
                selected = mode == AnimeFeedMode.POPULAR,
                onClick = onPopular,
                label = { Text("Popular") },
                leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        if (supportsLatest) {
            item {
                FilterChip(
                    selected = mode == AnimeFeedMode.LATEST,
                    onClick = onLatest,
                    label = { Text("Latest") },
                    leadingIcon = { Icon(Icons.Outlined.Update, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
        item {
            FilterChip(
                selected = false,
                onClick = onFilter,
                label = { Text("Filter") },
                leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

@Composable
private fun FeedRow(
    title: String,
    animes: Result<List<SAnime>>?,
    onTitleClick: () -> Unit,
    onAnimeClick: (SAnime) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable(onClick = onTitleClick),
        )
        when {
            animes == null -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            animes.isFailure -> Text(
                text = animes.exceptionOrNull()?.message ?: "Failed to load anime",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
            animes.getOrNull().isNullOrEmpty() -> Text(
                text = "No anime found",
                modifier = Modifier.padding(16.dp),
            )
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(animes.getOrThrow(), key = { it.url }) { anime ->
                    FeedCard(anime = anime, onClick = { onAnimeClick(anime) })
                }
            }
        }
    }
}

/** A paged list shown as a full grid, with a title, a load-more footer and retry on failure. */
private fun LazyListScope.animeListing(
    title: String,
    listing: AnimeFeedListing,
    onLoadMore: () -> Unit,
    onAnimeClick: (SAnime) -> Unit,
) {
    item(key = "anime-listing-title-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
    animeGrid(listing.animes, onAnimeClick)
    item(key = "anime-listing-footer-$title") {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val error = listing.error
            when {
                listing.loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onLoadMore) { Text("Retry") }
                }
                listing.hasNext -> TextButton(onClick = onLoadMore) { Text("Load more") }
                listing.animes.isEmpty() -> Text("No anime found")
            }
        }
    }
}

/** Covers in rows of three, each with its title over a dark fade. */
private fun LazyListScope.animeGrid(animes: List<SAnime>, onAnimeClick: (SAnime) -> Unit) {
    items(animes.distinctBy { it.url }.chunked(3), key = { row -> row.first().url }) { row ->
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row.forEach { anime ->
                FeedGridCard(
                    anime = anime,
                    modifier = Modifier.weight(1f),
                    onClick = { onAnimeClick(anime) },
                )
            }
            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

/** A cover with its title in a rounded panel underneath, as on the manga feed. */
@Composable
private fun FeedCard(anime: SAnime, onClick: () -> Unit) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = anime.thumbnail_url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(14.dp)),
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Text(
                text = anime.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

/** A large cover with the title over a dark fade at the bottom. */
@Composable
private fun FeedGridCard(anime: SAnime, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = anime.thumbnail_url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    ),
                ),
        )
        Text(
            text = anime.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
        )
    }
}

class AnimeSourceFeedViewModel(
    val sourceId: Long,
) : ViewModel() {

    val source: AnimeCatalogueSource? = AnimeExtensionManager.installedSources()
        .filterIsInstance<AnimeCatalogueSource>()
        .firstOrNull { runCatching { it.id }.getOrNull() == sourceId }

    data class State(
        val latest: Result<List<SAnime>>? = null,
        val popular: Result<List<SAnime>>? = null,
        val mode: AnimeFeedMode = AnimeFeedMode.HOME,
        val popularListing: AnimeFeedListing = AnimeFeedListing(),
        val latestListing: AnimeFeedListing = AnimeFeedListing(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val src = source
        if (src == null) {
            val missing = Result.failure<List<SAnime>>(IllegalStateException("This source is no longer installed"))
            _state.update { State(latest = missing, popular = missing) }
            return
        }
        viewModelScope.launch {
            launch {
                val latest = if (src.supportsLatest) {
                    fetch { src.getLatestUpdates(1).animes }
                } else {
                    Result.success(emptyList())
                }
                _state.update { it.copy(latest = latest) }
            }
            launch {
                val popular = fetch { src.getPopularAnime(1).animes }
                _state.update { it.copy(popular = popular) }
            }
        }
    }

    /** Switches the feed in place. Popular and Latest load their first page when first shown. */
    fun setMode(mode: AnimeFeedMode) {
        _state.update { it.copy(mode = mode) }
        if (mode == AnimeFeedMode.HOME) return
        val listing = listingFor(_state.value, mode)
        if (listing.animes.isEmpty() && listing.error == null && !listing.loading) {
            loadMore(mode)
        }
    }

    /** Fetches the next page of Popular or Latest and appends it. */
    fun loadMore(mode: AnimeFeedMode) {
        val current = listingFor(_state.value, mode)
        if (current.loading || !current.hasNext) return
        val src = source
        if (src == null) {
            setListing(mode) { it.copy(error = "This source is no longer installed") }
            return
        }
        val page = current.nextPage
        setListing(mode) { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (mode == AnimeFeedMode.LATEST) src.getLatestUpdates(page) else src.getPopularAnime(page)
                }
            }
            setListing(mode) { listing ->
                result.fold(
                    onSuccess = { pageResult ->
                        listing.copy(
                            animes = (listing.animes + pageResult.animes).distinctBy { it.url },
                            nextPage = page + 1,
                            hasNext = pageResult.hasNextPage,
                            loading = false,
                        )
                    },
                    onFailure = { e ->
                        listing.copy(loading = false, error = e.message ?: "Failed to load anime")
                    },
                )
            }
        }
    }

    private fun listingFor(state: State, mode: AnimeFeedMode): AnimeFeedListing =
        if (mode == AnimeFeedMode.LATEST) state.latestListing else state.popularListing

    private fun setListing(mode: AnimeFeedMode, block: (AnimeFeedListing) -> AnimeFeedListing) {
        _state.update { s ->
            if (mode == AnimeFeedMode.LATEST) {
                s.copy(latestListing = block(s.latestListing))
            } else {
                s.copy(popularListing = block(s.popularListing))
            }
        }
    }

    private suspend fun fetch(block: suspend () -> List<SAnime>): Result<List<SAnime>> =
        withContext(Dispatchers.IO) { runCatching { block() } }
}
