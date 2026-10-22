package eu.kanade.tachiyomi.ui.browse.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.presentation.core.components.material.Scaffold

/**
 * Anime counterpart of the manga source feed: a Latest row and a Popular row of covers for one
 * source. Tapping a row header opens the full browse list; tapping a cover opens its details.
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
                        IconButton(onClick = { navigator.push(AnimeBrowseScreen(sourceId = sourceId)) }) {
                            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(contentPadding = paddingValues) {
                if (source?.supportsLatest == true) {
                    item(key = "anime-feed-latest") {
                        AnimeFeedSection(
                            title = "Latest",
                            subtitle = sourceName,
                            animes = state.latest,
                            onHeaderClick = {
                                navigator.push(AnimeBrowseScreen(sourceId = sourceId, startLatest = true))
                            },
                            onAnimeClick = { anime ->
                                navigator.push(
                                    AnimeDetailsScreen(
                                        sourceId = sourceId,
                                        animeUrl = anime.url,
                                        title = anime.title,
                                        thumbnailUrl = anime.thumbnail_url,
                                    ),
                                )
                            },
                        )
                    }
                }
                item(key = "anime-feed-popular") {
                    AnimeFeedSection(
                        title = "Popular",
                        subtitle = sourceName,
                        animes = state.popular,
                        onHeaderClick = {
                            navigator.push(AnimeBrowseScreen(sourceId = sourceId))
                        },
                        onAnimeClick = { anime ->
                            navigator.push(
                                AnimeDetailsScreen(
                                    sourceId = sourceId,
                                    animeUrl = anime.url,
                                    title = anime.title,
                                    thumbnailUrl = anime.thumbnail_url,
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
private fun AnimeFeedSection(
    title: String,
    subtitle: String,
    animes: Result<List<SAnime>>?,
    onHeaderClick: () -> Unit,
    onAnimeClick: (SAnime) -> Unit,
) {
    GlobalSearchResultItem(
        title = title,
        subtitle = subtitle,
        onClick = onHeaderClick,
    ) {
        when {
            animes == null -> GlobalSearchLoadingResultItem()
            animes.isFailure -> Text(
                text = animes.exceptionOrNull()?.message ?: "Failed to load anime",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
            else -> {
                val list = animes.getOrNull().orEmpty()
                if (list.isEmpty()) {
                    Text(
                        text = "No anime found",
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(list, key = { it.url }) { anime ->
                            Box(modifier = Modifier.width(120.dp)) {
                                AnimeBrowseCard(anime = anime, onClick = { onAnimeClick(anime) })
                            }
                        }
                    }
                }
            }
        }
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

    private suspend fun fetch(block: suspend () -> List<SAnime>): Result<List<SAnime>> =
        withContext(Dispatchers.IO) { runCatching { block() } }
}
