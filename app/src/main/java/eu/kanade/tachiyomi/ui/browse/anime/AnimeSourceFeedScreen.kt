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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import eu.kanade.presentation.theme.AnimeLookTokens
import eu.kanade.presentation.theme.rememberAnimeLook
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
 * Anime counterpart of the manga source feed. Home opens with a featured title, then Latest and
 * Popular rows and a grid of covers. Popular and Latest switch the feed in place into a paged grid,
 * the way the manga feed does. Filter still opens the browse screen, because the filter sheet lives
 * there. Colours, corners and accents come from the selected anime look.
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
        val look = rememberAnimeLook()

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
                        Column {
                            Text(
                                text = sourceName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Anime source",
                                style = MaterialTheme.typography.labelSmall,
                                color = look.textMuted,
                            )
                        }
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = look.background,
                        titleContentColor = look.textPrimary,
                        navigationIconContentColor = look.textPrimary,
                        actionIconContentColor = look.textPrimary,
                    ),
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(look.background),
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
                        val popular = state.popular?.getOrNull().orEmpty()
                        val featured = popular.firstOrNull()
                        if (featured != null) {
                            item(key = "anime-feed-featured") {
                                FeaturedCard(anime = featured, onClick = { openDetails(featured) })
                            }
                        }
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
                        item(key = "anime-feed-more") {
                            FeedSectionHeader(title = "More to watch")
                        }
                        animeGrid(
                            look = look,
                            animes = if (featured != null) popular.drop(1) else popular,
                        ) { openDetails(it) }
                    }
                    AnimeFeedMode.POPULAR -> animeListing(
                        look = look,
                        title = "Popular",
                        listing = state.popularListing,
                        onLoadMore = { viewModel.loadMore(AnimeFeedMode.POPULAR) },
                        onAnimeClick = { openDetails(it) },
                    )
                    AnimeFeedMode.LATEST -> animeListing(
                        look = look,
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
    val look = rememberAnimeLook()
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = look.surface,
        labelColor = look.textMuted,
        iconColor = look.textMuted,
        selectedContainerColor = look.accent,
        selectedLabelColor = look.onAccent,
        selectedLeadingIconColor = look.onAccent,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = mode == AnimeFeedMode.HOME,
                onClick = onHome,
                label = { Text("Home") },
                leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = chipColors,
            )
        }
        item {
            FilterChip(
                selected = mode == AnimeFeedMode.POPULAR,
                onClick = onPopular,
                label = { Text("Popular") },
                leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = chipColors,
            )
        }
        if (supportsLatest) {
            item {
                FilterChip(
                    selected = mode == AnimeFeedMode.LATEST,
                    onClick = onLatest,
                    label = { Text("Latest") },
                    leadingIcon = { Icon(Icons.Outlined.Update, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = chipColors,
                )
            }
        }
        item {
            FilterChip(
                selected = false,
                onClick = onFilter,
                label = { Text("Filter") },
                leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = chipColors,
            )
        }
    }
}

/** Large first title on Home, with its name over a fade into the page background. */
@Composable
private fun FeaturedCard(anime: SAnime, onClick: () -> Unit) {
    val look = rememberAnimeLook()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = anime.thumbnail_url,
            contentDescription = anime.title,
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
                .padding(16.dp),
        ) {
            Text(
                text = "FEATURED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = look.accent,
            )
            Text(
                text = anime.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = look.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Section title with an accent bar and an optional "See all" action. */
@Composable
private fun FeedSectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
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
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = look.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel, color = look.accent, style = MaterialTheme.typography.labelLarge)
            }
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
    val look = rememberAnimeLook()
    Column {
        FeedSectionHeader(title = title, actionLabel = "See all", onAction = onTitleClick)
        when {
            animes == null -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = look.accent,
                )
            }
            animes.isFailure -> Text(
                text = animes.exceptionOrNull()?.message ?: "Failed to load anime",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            animes.getOrNull().isNullOrEmpty() -> Text(
                text = "No anime found",
                color = look.textMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
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
    look: AnimeLookTokens,
    title: String,
    listing: AnimeFeedListing,
    onLoadMore: () -> Unit,
    onAnimeClick: (SAnime) -> Unit,
) {
    item(key = "anime-listing-title-$title") {
        FeedSectionHeader(title = title)
    }
    animeGrid(look = look, animes = listing.animes, onAnimeClick = onAnimeClick)
    item(key = "anime-listing-footer-$title") {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val error = listing.error
            when {
                listing.loading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = look.accent,
                )
                error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onLoadMore) { Text("Retry", color = look.accent) }
                }
                listing.hasNext -> TextButton(onClick = onLoadMore) { Text("Load more", color = look.accent) }
                listing.animes.isEmpty() -> Text("No anime found", color = look.textMuted)
            }
        }
    }
}

/** Covers in rows of three, each with its title over a fade. */
private fun LazyListScope.animeGrid(
    look: AnimeLookTokens,
    animes: List<SAnime>,
    onAnimeClick: (SAnime) -> Unit,
) {
    items(animes.distinctBy { it.url }.chunked(3), key = { row -> row.first().url }) { row ->
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row.forEach { anime ->
                FeedGridCard(
                    anime = anime,
                    look = look,
                    modifier = Modifier.weight(1f),
                    onClick = { onAnimeClick(anime) },
                )
            }
            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

/** A cover in a rounded frame with its title underneath, in the look's text colour. */
@Composable
private fun FeedCard(anime: SAnime, onClick: () -> Unit) {
    val look = rememberAnimeLook()
    Column(modifier = Modifier.width(128.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = anime.thumbnail_url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(look.cornerRadius))
                .background(look.surface),
        )
        Text(
            text = anime.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = look.textPrimary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** A large cover with the title over a dark fade at the bottom. */
@Composable
private fun FeedGridCard(
    anime: SAnime,
    look: AnimeLookTokens,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
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
