package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined._18UpRating
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.ui.browse.anime.AnimeBrowseScreen
import eu.kanade.tachiyomi.ui.browse.extension.AnimeExtensionsViewModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus

/**
 * "Anime Sources" browse tab. Lists the sources provided by installed anime extensions, grouped
 * by language. Tapping a source opens its popular list; "Latest" opens its latest updates.
 */
@Composable
fun animeSourcesTab(
    animeExtensionsViewModel: AnimeExtensionsViewModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val extState by animeExtensionsViewModel.state.collectAsState()
    val context = LocalContext.current
    var nsfwOnly by remember { mutableStateOf(false) }

    val rows = extState.installed.flatMap { ext ->
        ext.sources.map { AnimeSourceRowData(source = it, pkgName = ext.pkgName, nsfw = ext.isNsfw) }
    }
    val groups = rows
        .filter { !nsfwOnly || it.nsfw }
        .groupBy { row ->
            row.source.lang.takeIf { it.isNotBlank() }
                ?.let { LocaleHelper.getSourceDisplayName(it, context) }
                ?: "Other"
        }
        .toSortedMap()

    return TabContent(
        titleRes = TDMR.strings.label_anime_sources,
        actions = listOf(
            AppBar.Action(
                title = "18+",
                icon = Icons.Outlined._18UpRating,
                iconTint = if (nsfwOnly) MaterialTheme.colorScheme.error else null,
                onClick = { nsfwOnly = !nsfwOnly },
            ),
        ),
        content = { contentPadding, _ ->
            if (groups.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No anime sources yet. Install an anime extension to see its sources here.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                FastScrollLazyColumn(contentPadding = contentPadding + topSmallPaddingValues) {
                    groups.forEach { (label, rowsInGroup) ->
                        item(key = "anime-source-header-$label") {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.header,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(rowsInGroup, key = { "anime-source-${it.source.id}" }) { row ->
                            AnimeSourceRow(
                                row = row,
                                onOpen = {
                                    navigator.push(AnimeBrowseScreen(sourceId = row.source.id))
                                },
                                onLatest = {
                                    navigator.push(AnimeBrowseScreen(sourceId = row.source.id, startLatest = true))
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

private data class AnimeSourceRowData(
    val source: AnimeSource,
    val pkgName: String,
    val nsfw: Boolean,
)

@Composable
private fun AnimeSourceRow(
    row: AnimeSourceRowData,
    onOpen: () -> Unit,
    onLatest: () -> Unit,
) {
    val context = LocalContext.current
    val browsable = row.source is AnimeCatalogueSource
    val langLabel = row.source.lang.takeIf { it.isNotBlank() }
        ?.let { LocaleHelper.getSourceDisplayName(it, context) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = browsable, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimeSourceIcon(pkgName = row.pkgName, modifier = Modifier.size(48.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = row.source.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.nsfw) {
                    Text(
                        text = "18+",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (row.nsfw && langLabel != null) {
                    Text(text = " · ", style = MaterialTheme.typography.bodySmall)
                }
                if (langLabel != null) {
                    Text(
                        text = langLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Text(
            text = "Latest",
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (browsable) 1f else 0.38f),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable(enabled = browsable, onClick = onLatest),
        )
    }
}

@Composable
private fun AnimeSourceIcon(pkgName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appIcon: ImageBitmap? = remember(pkgName) {
        runCatching {
            context.packageManager.getApplicationIcon(pkgName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
    Image(
        bitmap = appIcon ?: ImageBitmap.imageResource(id = R.mipmap.ic_default_source),
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
    )
}
