package eu.kanade.tachiyomi.ui.library

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.rememberAnimeLook
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.anime.AnimeDetailsScreen
import eu.kanade.tachiyomi.ui.browse.anime.AnimeListEntry
import eu.kanade.tachiyomi.ui.browse.anime.AnimeMyList
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

/**
 * Anime tab. Shows My List: anime saved from the anime details screen, newest first. Tapping a
 * title opens its details. The list is empty until something is saved.
 */
data object AnimeTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 6u,
            title = "Anime",
            icon = rememberVectorPainter(Icons.Outlined.Movie),
        )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val look = rememberAnimeLook()
        val entries = AnimeMyList.entries()

        Scaffold(
            topBar = { scrollBehavior ->
                CenterAlignedTopAppBar(
                    title = { Text("My List") },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
                )
            },
        ) { contentPadding ->
            if (entries.isEmpty()) {
                EmptyScreen(
                    message = "Your anime list is empty. Save a title from its details screen.",
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(look.background),
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding() + 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(entries, key = { "${it.sourceId}|${it.url}" }) { entry ->
                        MyListRow(
                            entry = entry,
                            onClick = {
                                navigator.push(
                                    AnimeDetailsScreen(
                                        sourceId = entry.sourceId,
                                        animeUrl = entry.url,
                                        title = entry.title,
                                        thumbnailUrl = entry.thumbnailUrl,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListRow(entry: AnimeListEntry, onClick: () -> Unit) {
    val look = rememberAnimeLook()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(look.cornerRadius))
            .background(look.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = entry.thumbnailUrl,
            contentDescription = entry.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(look.cornerRadius))
                .background(look.surfaceHigh),
        )
        Column {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = look.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
