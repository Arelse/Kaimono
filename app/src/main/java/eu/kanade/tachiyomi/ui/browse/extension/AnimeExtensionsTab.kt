package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.runtime.collectAsState

@Composable
fun animeExtensionsTab(
    viewModel: AnimeExtensionsViewModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    return TabContent(
        titleRes = TDMR.strings.label_anime_extensions,
        searchEnabled = true,
        actions = listOf(
            AppBar.OverflowAction(
                title = stringResource(TDMR.strings.pref_anime_extension_repos),
                onClick = { navigator.push(AnimeExtensionReposScreen()) },
            ),
            AppBar.OverflowAction(
                title = "Refresh",
                onClick = viewModel::refresh,
            ),
        ),
        content = { contentPadding, _ ->
            AnimeExtensionList(
                state = state,
                contentPadding = contentPadding,
                onOpenRepos = { navigator.push(AnimeExtensionReposScreen()) },
                onInstall = viewModel::install,
                onUninstall = viewModel::uninstall,
                onTrust = viewModel::trust,
            )
        },
    )
}

@Composable
private fun AnimeExtensionList(
    state: AnimeExtensionsViewModel.State,
    contentPadding: PaddingValues,
    onOpenRepos: () -> Unit,
    onInstall: (AnimeExtension.Available) -> Unit,
    onUninstall: (AnimeExtension.Installed) -> Unit,
    onTrust: (AnimeExtension.Untrusted) -> Unit,
) {
    val available = state.available.filterNot { it.pkgName in state.installedPkgNames }
    val isEmpty = state.installed.isEmpty() && state.untrusted.isEmpty() && available.isEmpty()

    if (isEmpty) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (state.isLoading) {
                    "Loading anime extensions…"
                } else {
                    "No anime extensions yet. Add a repository to get started."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onOpenRepos) {
                Text(text = "Anime Extension Repos")
            }
        }
        return
    }

    LazyColumn(contentPadding = contentPadding) {
        if (state.installed.isNotEmpty()) {
            item { SectionHeader("Installed") }
            items(state.installed, key = { "installed-${it.pkgName}" }) { ext ->
                ExtensionRow(
                    name = ext.name,
                    subtitle = listOfNotNull(
                        ext.versionName,
                        ext.lang.takeIf { it.isNotBlank() }?.uppercase(),
                        if (ext.isNsfw) "18+" else null,
                        "${ext.sources.size} source(s)",
                    ).joinToString(" · "),
                    actionLabel = "Uninstall",
                    onAction = { onUninstall(ext) },
                )
            }
        }

        if (state.untrusted.isNotEmpty()) {
            item { SectionHeader("Untrusted") }
            items(state.untrusted, key = { "untrusted-${it.pkgName}" }) { ext ->
                ExtensionRow(
                    name = ext.name,
                    subtitle = "${ext.versionName} · Signature not trusted yet",
                    actionLabel = "Trust",
                    onAction = { onTrust(ext) },
                )
            }
        }

        if (available.isNotEmpty()) {
            item { SectionHeader("Available") }
            items(available, key = { "available-${it.pkgName}" }) { ext ->
                ExtensionRow(
                    name = ext.name,
                    subtitle = listOfNotNull(
                        ext.versionName,
                        ext.lang.takeIf { it.isNotBlank() }?.uppercase(),
                        if (ext.isNsfw) "18+" else null,
                        ext.sourceNames.takeIf { it.isNotEmpty() }?.joinToString(", "),
                    ).joinToString(" · "),
                    actionLabel = "Install",
                    onAction = { onInstall(ext) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ExtensionRow(
    name: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onAction) {
            Text(text = actionLabel)
        }
    }
}
