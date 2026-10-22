package eu.kanade.tachiyomi.ui.browse.anime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.kanade.presentation.util.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.animesource.model.SAnime
import tachiyomi.presentation.core.components.material.Scaffold

/** Browse one anime source: Popular, Latest, or a search. Opens [AnimeDetailsScreen] on tap. */
data class AnimeBrowseScreen(
    val sourceId: Long,
    val startLatest: Boolean = false,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel<AnimeBrowseViewModel>(
            key = "anime-browse-$sourceId-$startLatest",
            factory = viewModelFactory {
                initializer { AnimeBrowseViewModel(sourceId, startLatest) }
            },
        )
        val state by viewModel.state.collectAsState()
        var queryText by remember { mutableStateOf("") }
        val error = state.error

        Scaffold(
            topBar = { scrollBehavior ->
                CenterAlignedTopAppBar(
                    title = { Text(text = viewModel.source?.name ?: "Anime source") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            Column(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search anime") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search(queryText) }),
                )

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.mode == AnimeBrowseViewModel.Mode.POPULAR,
                        onClick = {
                            queryText = ""
                            viewModel.showPopular()
                        },
                        label = { Text("Popular") },
                    )
                    FilterChip(
                        selected = state.mode == AnimeBrowseViewModel.Mode.LATEST,
                        onClick = {
                            queryText = ""
                            viewModel.showLatest()
                        },
                        label = { Text("Latest") },
                    )
                }

                when {
                    error != null && state.animes.isEmpty() -> Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                    state.animes.isEmpty() && state.isLoading -> CircularProgressIndicator(
                        modifier = Modifier
                            .padding(24.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                    state.animes.isEmpty() -> Text(
                        text = "No anime found",
                        modifier = Modifier.padding(16.dp),
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.animes, key = { it.url }) { anime ->
                            AnimeBrowseCard(
                                anime = anime,
                                onClick = {
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
                        if (state.hasNextPage) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                TextButton(
                                    onClick = viewModel::loadNextPage,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (state.isLoading) "Loading…" else "Load more")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnimeBrowseCard(anime: SAnime, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        AsyncImage(
            model = anime.thumbnail_url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            text = anime.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
