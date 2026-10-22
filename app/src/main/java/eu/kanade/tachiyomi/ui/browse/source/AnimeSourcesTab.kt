package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined._18UpRating
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.BaseBrowseItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.ui.browse.anime.AnimeBrowseScreen
import eu.kanade.tachiyomi.ui.browse.anime.AnimeSourceFeedScreen
import eu.kanade.tachiyomi.ui.browse.extension.AnimeExtensionsViewModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus

/**
 * "Anime Sources" browse tab. Lists every source from installed anime extensions, grouped by
 * language, in the same layout as the Manga Sources tab. Tapping a source opens its popular list;
 * "Latest" opens its latest updates.
 */
@Composable
fun animeSourcesTab(
    animeExtensionsViewModel: AnimeExtensionsViewModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val extState by animeExtensionsViewModel.state.collectAsState()
    val context = LocalContext.current
    var nsfwOnly by remember { mutableStateOf(false) }

    // Every source of every installed extension is listed. Some extensions throw when a field is
    // read, so each field is read on its own and falls back to the extension's own value.
    val rows = extState.installed.flatMap { ext ->
        ext.sources.map { source ->
            AnimeSourceRowData(
                source = source,
                sourceId = runCatching { source.id }.getOrNull(),
                sourceName = runCatching { source.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: ext.name,
                sourceLang = runCatching { source.lang }.getOrNull()?.takeIf { it.isNotBlank() } ?: ext.lang,
                pkgName = ext.pkgName,
                nsfw = ext.isNsfw,
            )
        }
    }
    val groups = rows
        .filter { !nsfwOnly || it.nsfw }
        .groupBy { row ->
            row.sourceLang.takeIf { it.isNotBlank() }
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
                        items(rowsInGroup, key = { "anime-source-${it.pkgName}-${rowsInGroup.indexOf(it)}" }) { row ->
                            AnimeSourceRow(
                                row = row,
                                onOpen = {
                                    row.sourceId?.let { id -> navigator.push(AnimeSourceFeedScreen(sourceId = id)) }
                                },
                                onLatest = {
                                    row.sourceId?.let { id ->
                                        navigator.push(AnimeBrowseScreen(sourceId = id, startLatest = true))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

/**
 * Used by screens that need a single working source (details, player, hub). A source whose id
 * can't be read is skipped there so the screen doesn't crash.
 */
fun isUsableAnimeSource(source: AnimeSource): Boolean =
    runCatching { source.id }.isSuccess

private data class AnimeSourceRowData(
    val source: AnimeSource,
    /** Null when the extension can't provide an id, so the source can't be opened. */
    val sourceId: Long?,
    val sourceName: String,
    val sourceLang: String,
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
    val canOpen = row.sourceId != null && row.source is AnimeCatalogueSource
    val langLabel = row.sourceLang.takeIf { it.isNotBlank() }
        ?.let { LocaleHelper.getSourceDisplayName(it, context) }

    // Same row as the Manga Sources tab: icon, name and language, then a primary "Latest" action.
    BaseBrowseItem(
        onClickItem = { if (canOpen) onOpen() },
        icon = {
            AnimeSourceIcon(pkgName = row.pkgName, modifier = Modifier.size(40.dp))
        },
        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.padding.medium),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.sourceName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.nsfw) {
                        Text(
                            text = "18+",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                if (row.sourceId == null) {
                    Text(
                        text = "Can't be opened: this extension is incompatible with the app",
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (langLabel != null) {
                    Text(
                        text = langLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        action = {
            if (canOpen) {
                TextButton(onClick = onLatest) {
                    Text(
                        text = "Latest",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
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
