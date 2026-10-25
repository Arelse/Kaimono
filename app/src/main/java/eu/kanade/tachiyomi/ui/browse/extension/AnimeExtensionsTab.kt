package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.UiMode
import eu.kanade.presentation.browse.components.BaseBrowseItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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
                onUninstallUntrusted = viewModel::uninstallUntrusted,
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
    onUninstallUntrusted: (AnimeExtension.Untrusted) -> Unit,
    onTrust: (AnimeExtension.Untrusted) -> Unit,
) {
    val available = state.available.filterNot { it.pkgName in state.installedPkgNames }
    val isEmpty = state.installed.isEmpty() && state.untrusted.isEmpty() && available.isEmpty()
    var trustTarget by remember { mutableStateOf<AnimeExtension.Untrusted?>(null) }

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

    FastScrollLazyColumn(contentPadding = contentPadding + topSmallPaddingValues) {
        if (state.installed.isNotEmpty()) {
            item(key = "anime-header-installed") { AnimeSectionHeader("Installed") }
            items(state.installed, key = { "installed-${it.pkgName}" }) { ext ->
                AnimeExtensionRow(
                    name = ext.name,
                    details = listOfNotNull(
                        ext.versionName,
                        ext.lang.takeIf { it.isNotBlank() }?.uppercase(),
                        "${ext.sources.size} source(s)",
                    ).joinToString(" · "),
                    warning = if (ext.isNsfw) "18+" else null,
                    installedPkgName = ext.pkgName,
                    action = {
                        IconButton(onClick = { onUninstall(ext) }) {
                            Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Uninstall")
                        }
                    },
                )
            }
        }

        if (state.untrusted.isNotEmpty()) {
            item(key = "anime-header-untrusted") { AnimeSectionHeader("Untrusted") }
            items(state.untrusted, key = { "untrusted-${it.pkgName}" }) { ext ->
                AnimeExtensionRow(
                    name = ext.name,
                    details = "${ext.versionName} · Signature not trusted yet",
                    warning = "UNTRUSTED",
                    installedPkgName = null,
                    onClickItem = { trustTarget = ext },
                    action = {
                        IconButton(onClick = { trustTarget = ext }) {
                            Icon(imageVector = Icons.Outlined.VerifiedUser, contentDescription = "Trust")
                        }
                    },
                )
            }
        }

        // Installed packages the app couldn't load, with the reason, so a missing extension can be diagnosed.
        if (state.loadErrors.isNotEmpty()) {
            item(key = "anime-header-load-errors") { AnimeSectionHeader("Couldn't load") }
            items(state.loadErrors, key = { "load-error-${it.pkgName}" }) { err ->
                Text(
                    text = "${err.pkgName}\n${err.reason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
                )
            }
        }

        if (available.isNotEmpty()) {
            item(key = "anime-header-available") { AnimeSectionHeader("Available") }
            items(available, key = { "available-${it.pkgName}" }) { ext ->
                AnimeExtensionRow(
                    name = ext.name,
                    details = listOfNotNull(
                        ext.versionName,
                        ext.lang.takeIf { it.isNotBlank() }?.uppercase(),
                        ext.sourceNames.takeIf { it.isNotEmpty() }?.joinToString(", "),
                    ).joinToString(" · "),
                    warning = if (ext.isNsfw) "18+" else null,
                    installedPkgName = null,
                    iconUrl = ext.iconUrl,
                    onClickItem = { onInstall(ext) },
                    action = {
                        IconButton(onClick = { onInstall(ext) }) {
                            Icon(imageVector = Icons.Outlined.GetApp, contentDescription = "Install")
                        }
                    },
                )
            }
        }
    }

    trustTarget?.let { ext ->
        AnimeUntrustedDialog(
            onConfirm = {
                onTrust(ext)
                trustTarget = null
            },
            onUninstall = {
                onUninstallUntrusted(ext)
                trustTarget = null
            },
            onDismiss = { trustTarget = null },
        )
    }
}

/**
 * Same wording and buttons as the Manga extensions "Untrusted extension" dialog. Under Atsu UI and
 * Liquid Glass the panel uses a frosted, semi-transparent surface. A Compose dialog cannot blur the
 * content behind it, so this is an approximation of the frosted look.
 */
@Composable
private fun AnimeUntrustedDialog(
    onConfirm: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val uiMode by Injekt.get<UiPreferences>().uiMode.collectPrefAsState()
    val frosted = uiMode == UiMode.GLASSMORPHISM || uiMode == UiMode.LIQUID_GLASS

    AlertDialog(
        containerColor = if (frosted) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        } else {
            AlertDialogDefaults.containerColor
        },
        title = {
            Text(text = stringResource(MR.strings.untrusted_extension))
        },
        text = {
            Text(text = stringResource(MR.strings.untrusted_extension_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(MR.strings.ext_trust))
            }
        },
        dismissButton = {
            TextButton(onClick = onUninstall) {
                Text(text = stringResource(MR.strings.ext_uninstall))
            }
        },
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun AnimeSectionHeader(title: String) {
    Row(
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.header,
        )
    }
}

/**
 * Row styled like the Manga extensions list: app icon on the left, name with a muted detail line,
 * and an action icon on the right. Installed rows use the real app icon, available rows use the
 * icon from the repo ([iconUrl]), and untrusted rows show the warning icon.
 */
@Composable
private fun AnimeExtensionRow(
    name: String,
    details: String,
    warning: String?,
    installedPkgName: String?,
    action: @Composable RowScope.() -> Unit,
    iconUrl: String? = null,
    onClickItem: () -> Unit = {},
) {
    BaseBrowseItem(
        onClickItem = onClickItem,
        icon = {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                AnimeExtensionIcon(
                    installedPkgName = installedPkgName,
                    untrusted = installedPkgName == null && warning == "UNTRUSTED",
                    iconUrl = iconUrl,
                    modifier = Modifier.size(40.dp),
                )
            }
        },
        action = action,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = MaterialTheme.padding.medium),
        ) {
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = details,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (warning != null) {
                    Text(
                        text = if (details.isNotEmpty()) " · $warning" else warning,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeExtensionIcon(
    installedPkgName: String?,
    untrusted: Boolean,
    iconUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appIcon: ImageBitmap? = remember(installedPkgName) {
        if (installedPkgName == null) {
            null
        } else {
            runCatching {
                context.packageManager.getApplicationIcon(installedPkgName).toBitmap().asImageBitmap()
            }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(8.dp)
    when {
        untrusted -> Icon(
            imageVector = Icons.Filled.Dangerous,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        appIcon != null -> Image(
            bitmap = appIcon,
            contentDescription = null,
            modifier = modifier.clip(shape),
        )
        iconUrl != null -> AsyncImage(
            model = iconUrl,
            contentDescription = null,
            placeholder = painterResource(id = R.mipmap.ic_default_source),
            error = painterResource(id = R.mipmap.ic_default_source),
            fallback = painterResource(id = R.mipmap.ic_default_source),
            modifier = modifier.clip(shape),
        )
        else -> Image(
            bitmap = ImageBitmap.imageResource(id = R.mipmap.ic_default_source),
            contentDescription = null,
            modifier = modifier.clip(shape),
        )
    }
}
