package eu.kanade.tachiyomi.ui.browse.anime

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.kanade.presentation.util.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.presentation.core.components.material.Scaffold

/** Details for one anime from an installed source: cover, info, and its episode list. */
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

        Scaffold(
            topBar = { scrollBehavior ->
                CenterAlignedTopAppBar(
                    title = { Text(text = anime?.title ?: title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when {
                anime == null || (state.isLoading && state.episodes.isEmpty() && state.error == null) -> {
                    Column(
                        modifier = Modifier.padding(contentPadding).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.padding(contentPadding).fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        item {
                            anime?.let { AnimeHeader(anime = it) }
                        }
                        state.error?.let { error ->
                            item {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 16.dp),
                                )
                            }
                        }
                        if (state.episodes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Episodes (${state.episodes.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                                )
                            }
                            items(state.episodes, key = { it.url }) { episode ->
                                EpisodeRow(
                                    episode = episode,
                                    onClick = {
                                        // Playback needs a video player, which this build doesn't include yet.
                                        Toast.makeText(
                                            context,
                                            "Episode playback isn't available yet",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
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
private fun AnimeHeader(anime: SAnime) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(
                model = anime.thumbnail_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = anime.title, style = MaterialTheme.typography.titleLarge)
                anime.author?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = statusLabel(anime.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                anime.genre?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        anime.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun EpisodeRow(episode: SEpisode, onClick: () -> Unit) {
    Column(
        modifier = Modifier
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
    SAnime.COMPLETED -> "Completed"
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
        .firstOrNull { it.id == sourceId }

    private val seed: SAnime = SAnime.create().apply {
        url = animeUrl
        this.title = title
        thumbnail_url = thumbnailUrl
    }

    data class State(
        val anime: SAnime? = null,
        val episodes: List<SEpisode> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State(anime = seed))
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
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
                    details to episodes
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { (details, episodes) ->
                        current.copy(anime = details, episodes = episodes, isLoading = false)
                    },
                    onFailure = { e ->
                        current.copy(isLoading = false, error = e.message ?: "Failed to load anime details")
                    },
                )
            }
        }
    }
}
