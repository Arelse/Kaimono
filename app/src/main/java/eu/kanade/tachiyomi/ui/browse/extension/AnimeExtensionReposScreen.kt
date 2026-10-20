package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.api.AnimeExtensionRepos
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Lets the user add, enable/disable and remove anime extension repositories.
 * Layout follows the manga "Extension stores" screen.
 */
class AnimeExtensionReposScreen : Screen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var repos by remember { mutableStateOf(snapshot()) }
        var showAddDialog by remember { mutableStateOf(false) }

        fun reload() {
            repos = snapshot()
            scope.launch { AnimeExtensionManager.refresh(context) }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Anime Extension Repos") },
                    navigationIcon = {
                        IconButton(onClick = navigator::pop) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { reload() }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(text = "Add") },
                )
            },
        ) { padding ->
            if (repos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .padding(24.dp),
                ) {
                    Text(
                        text = "No repositories yet. Tap Add to add a repository URL.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(repos, key = { it.url }) { item ->
                        RepoCard(
                            name = AnimeExtensionRepos.displayName(item.url),
                            enabled = item.enabled,
                            onSetEnabled = { enabled ->
                                AnimeExtensionRepos.setEnabled(item.url, enabled)
                                reload()
                            },
                            onOpen = { context.openInBrowser(item.url) },
                            onCopy = { context.copyToClipboard(item.url, item.url) },
                            onDelete = {
                                AnimeExtensionRepos.removeRepo(item.url)
                                reload()
                            },
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddRepoDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { url ->
                    if (AnimeExtensionRepos.addRepo(url)) {
                        showAddDialog = false
                        reload()
                        true
                    } else {
                        false
                    }
                },
            )
        }
    }
}

private data class RepoItem(val url: String, val enabled: Boolean)

private fun snapshot(): List<RepoItem> =
    AnimeExtensionRepos.getRepos().map { RepoItem(it, AnimeExtensionRepos.isEnabled(it)) }

@Composable
private fun RepoCard(
    name: String,
    enabled: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Label, contentDescription = null)
            Text(
                text = name,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Switch(
                checked = enabled,
                onCheckedChange = onSetEnabled,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = stringResource(MR.strings.action_open_in_browser),
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}

@Composable
private fun AddRepoDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Boolean,
) {
    var text by remember { mutableStateOf("") }
    var isInvalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add repository") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    isInvalid = false
                },
                singleLine = true,
                isError = isInvalid,
                label = { Text(text = "Repository URL") },
                supportingText = {
                    if (isInvalid) {
                        Text(text = "Enter a valid http:// or https:// URL")
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!onAdd(text)) isInvalid = true
                },
            ) {
                Text(text = "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}
